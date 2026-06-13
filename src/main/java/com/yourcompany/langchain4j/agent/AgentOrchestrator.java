package com.yourcompany.langchain4j.agent;

import com.yourcompany.langchain4j.tool.CodeQualityTool;
import com.yourcompany.langchain4j.service.ProjectContextCache;
import com.yourcompany.langchain4j.learning.SelfLearningManager;
import com.yourcompany.langchain4j.knowledge.KnowledgeBaseManager;
import com.yourcompany.langchain4j.knowledge.KnowledgeDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.PreDestroy;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.regex.Pattern;

/**
 * AI 编程 Agent 编排器（增强版 + Token 优化）
 *
 * 核心能力：
 * 1. 增强意图分类 — 正则 + 中英文关键词 + 置信度评分，降低误分类
 * 2. 质量门禁 — 代码生成后自动调用 CodeQualityTool 做客观验证（安全/规范/复杂度）
 * 3. 自动修正循环 — 质量不达标时定向修复，最多 2 轮
 * 4. 智能上下文传递 — 从上游输出中提取代码块，避免截断关键代码
 * 5. 超时保护与降级 — 单步骤超时 + 连续失败熔断 + 降级策略
 * 6. 按意图动态路由 Agent — 分析/问答/文档用轻量 Agent，代码生成/审查用完整 Agent
 * 7. Few-Shot 注入（log.md #8）— 代码生成步骤从知识库检索相似示例注入 Prompt
 * 8. 历史教训注入（O6）— SelfLearningManager 改进模式注入，避免重蹈覆辙
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentOrchestrator {

    private final AiProgrammingAgent agent;
    private final LightweightQaAgent lightweightQaAgent;
    private final CodeQualityTool codeQualityTool;
    private final ProjectContextCache projectContextCache;
    private final SelfLearningManager selfLearningManager;
    private final KnowledgeBaseManager knowledgeBaseManager;

    /** 共享线程池（避免每次步骤创建/销毁线程池，减少线程创建开销） */
    private final ExecutorService sharedExecutor = Executors.newFixedThreadPool(
            Math.min(4, Runtime.getRuntime().availableProcessors()),
            r -> {
                Thread t = new Thread(r, "orchestrator-worker");
                t.setDaemon(true);
                return t;
            });

    @PreDestroy
    public void shutdown() {
        sharedExecutor.shutdownNow();
        log.info("[Orchestrator] 共享线程池已关闭");
    }

    // ============================================================
    // 配置常量
    // ============================================================
    /** 单步骤最大执行时间（毫秒）— 工具调用密集型任务需要更多时间 */
    private static final long STEP_TIMEOUT_MS = 300_000;  // 5 分钟
    /** 质量门禁及格线 */
    private static final int QUALITY_PASS_SCORE = 70;
    /** 最大自动修正轮次 */
    private static final int MAX_FIX_ROUNDS = 2;
    /** 连续失败熔断阈值：连续 N 步失败则中止后续步骤 */
    private static final int FAILURE_CIRCUIT_BREAKER = 1;  // 1 步失败即熔断（避免无谓等待）

    // ============================================================
    // 意图关键词集合（中英文 + 正则模式）
    // ============================================================
    private static final Set<String> CODE_GEN_KEYWORDS = Set.of(
            "写代码", "编写代码", "生成代码", "实现一个", "帮我写",
            "创建一个", "开发一个", "编写一个", "new class", "new method",
            "实现功能", "添加功能", "新增接口", "写个工具", "写方法",
            "重构", "修复bug", "修 bug", "fix bug", "refactor",
            "代码生成", "自动生成", "补全代码", "代码实现",
            // 英文关键词（P1 增强）
            "write code", "generate code", "implement", "create class",
            "create function", "write function", "write a", "build a",
            "develop", "code a", "add feature", "write method"
    );

    private static final Set<String> REVIEW_KEYWORDS = Set.of(
            "代码审查", "code review", "审查代码", "检查代码",
            "review", "看看代码", "代码质量", "有没有问题", "安全问题",
            "漏洞", "代码规范", "规范性", "质量检查", "优化代码",
            // 英文
            "review code", "check code", "code quality", "find bugs",
            "security issue", "code smell"
    );

    private static final Set<String> ANALYSIS_KEYWORDS = Set.of(
            "分析", "解释", "解读", "理解", "为什么", "怎么回事",
            "是什么", "什么意思", "技术栈", "架构", "业务逻辑",
            "流程", "设计思路", "原理", "区别", "对比",
            "如何工作", "实现原理", "调用链路", "依赖关系", "模块划分",
            // 英文
            "explain", "analyze", "how does", "what is", "why",
            "architecture", "workflow", "compare", "difference"
    );

    private static final Set<String> DOC_KEYWORDS = Set.of(
            "文档", "readme", "api文档", "接口文档", "说明文档",
            "架构文档", "注释", "javadoc", "changelog", "wiki",
            "使用指南", "开发文档", "技术文档", "设计文档",
            // 英文
            "documentation", "generate doc", "write doc", "api doc"
    );

    /** 正则模式（P1 增强：捕捉关键词集合难以覆盖的结构化模式） */
    private static final Pattern CODE_GEN_PATTERN = Pattern.compile(
            "(?i)(write|generate|create|implement|build|develop)\\s+(a|an|the)?\\s*(java|python|javascript|go|rust|typescript|c\\+\\+|spring|react|vue)\\s+",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern REVIEW_PATTERN = Pattern.compile(
            "(?i)(review|audit|check|inspect|analyze)\\s+(this|the|my)?\\s*(code|function|class|method|module)"
    );

    // ============================================================
    // 公共 API
    // ============================================================

    /**
     * 智能编排入口
     * 自动分类意图 → 选择流水线 → 执行 → 质量门禁 → 聚合结果
     */
    public OrchestrationResult orchestrate(String userMessage, String codeContext) {
        long start = System.currentTimeMillis();
        log.info("[Orchestrator] 收到编排请求，消息长度: {}", userMessage.length());

        try {
            // Step 1：增强意图分类（含置信度）
            IntentResult intentResult = classifyIntentEnhanced(userMessage, codeContext);
            String intent = intentResult.intent;
            double confidence = intentResult.confidence;
            String intentLabel = getIntentLabel(intent);
            log.info("[Orchestrator] 意图: {} ({})，置信度: {}", intent, intentLabel, String.format("%.2f", confidence));

            // Step 2：获取流水线
            List<String> pipeline = getPipeline(intent);

            // Step 3：执行流水线（含超时保护 + 熔断降级）
            List<OrchestrationResult.StepResult> stepResults = executePipelineWithGuard(
                    intent, userMessage, codeContext, pipeline);

            // Step 4：质量门禁（仅代码生成类任务）
            int qualityScore = -1;
            int fixRounds = 0;
            if ("code_generation".equals(intent) || "complex_task".equals(intent)) {
                QualityGateResult qgResult = runQualityGate(
                        userMessage, codeContext, stepResults, intent);
                qualityScore = qgResult.score;
                fixRounds = qgResult.fixRounds;
                if (qgResult.additionalSteps != null) {
                    stepResults.addAll(qgResult.additionalSteps);
                }
            }

            // Step 5：提取最终结果
            String finalResult = stepResults.stream()
                    .filter(OrchestrationResult.StepResult::isSuccess)
                    .reduce((a, b) -> b)
                    .map(OrchestrationResult.StepResult::getResult)
                    .orElse("⚠️ 所有步骤均未成功执行，请重新描述您的请求。");

            long duration = System.currentTimeMillis() - start;
            log.info("[Orchestrator] 编排完成，意图={}，步骤数={}，质量分={}，修正轮次={}，总耗时={}ms",
                    intent, stepResults.size(), qualityScore, fixRounds, duration);

            boolean anySuccess = stepResults.stream().anyMatch(OrchestrationResult.StepResult::isSuccess);
            
            // 构建详细错误信息（当所有步骤都失败时）
            String errorMessage = null;
            if (!anySuccess) {
                StringBuilder errorDetail = new StringBuilder();
                for (OrchestrationResult.StepResult step : stepResults) {
                    if (!step.isSuccess() && step.getError() != null) {
                        errorDetail.append(step.getStepName()).append(": ")
                                   .append(step.getError()).append("; ");
                    }
                }
                errorMessage = errorDetail.length() > 0 
                    ? errorDetail.toString() 
                    : "所有步骤均未成功执行，请重新描述您的请求。";
            }

            return OrchestrationResult.builder()
                    .intent(intent)
                    .intentLabel(intentLabel)
                    .confidence(confidence)
                    .pipeline(pipeline)
                    .steps(stepResults)
                    .finalResult(finalResult)
                    .qualityScore(qualityScore)
                    .fixRounds(fixRounds)
                    .totalDurationMs(duration)
                    .success(anySuccess)
                    .errorMessage(errorMessage)
                    .build();

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - start;
            log.error("[Orchestrator] 编排执行失败", e);
            return OrchestrationResult.builder()
                    .intent("error").intentLabel("执行异常")
                    .confidence(0).pipeline(List.of()).steps(List.of())
                    .finalResult(null).qualityScore(-1).fixRounds(0)
                    .totalDurationMs(duration)
                    .success(false).errorMessage(e.getMessage())
                    .build();
        }
    }

    // ============================================================
    // P1: 增强意图分类（正则 + 关键词 + 置信度）
    // ============================================================

    record IntentResult(String intent, double confidence) {}

    /**
     * 关键词匹配（带词边界保护，避免子串误匹配）
     * 短关键词（<=5字符）使用正则词边界匹配，长关键词使用 contains
     */
    private long countKeywordHits(String text, Set<String> keywords) {
        long hits = 0;
        for (String kw : keywords) {
            if (kw.length() <= 5) {
                // 短关键词用词边界匹配，避免 "how" 匹配 "show"、"a" 匹配 "java"
                if (Pattern.compile("(?i)\\b" + Pattern.quote(kw) + "\\b").matcher(text).find()) {
                    hits++;
                }
            } else {
                if (text.contains(kw)) hits++;
            }
        }
        return hits;
    }

    IntentResult classifyIntentEnhanced(String message, String codeContext) {
        String lower = message.toLowerCase();

        // 关键词匹配（带词边界保护）
        long codeGenHits  = countKeywordHits(lower, CODE_GEN_KEYWORDS);
        long reviewHits   = countKeywordHits(lower, REVIEW_KEYWORDS);
        long analysisHits = countKeywordHits(lower, ANALYSIS_KEYWORDS);
        long docHits      = countKeywordHits(lower, DOC_KEYWORDS);

        // 正则加分（命中 +2）
        if (CODE_GEN_PATTERN.matcher(lower).find())  codeGenHits  += 2;
        if (REVIEW_PATTERN.matcher(lower).find())     reviewHits   += 2;

        // 计算各意图得分（命中数 + 正则加分）
        double codeGenScore  = Math.min(codeGenHits  * 0.3, 1.0);
        double reviewScore   = Math.min(reviewHits   * 0.35, 1.0);
        double analysisScore = Math.min(analysisHits * 0.25, 1.0);
        double docScore      = Math.min(docHits      * 0.35, 1.0);

        // 统计命中了几类意图
        int intentTypes = 0;
        if (codeGenScore > 0)  intentTypes++;
        if (reviewScore > 0)   intentTypes++;
        if (analysisScore > 0) intentTypes++;
        if (docScore > 0)      intentTypes++;

        // 两个及以上意图叠加 → complex_task
        if (intentTypes >= 2) {
            double conf = Math.min((codeGenScore + reviewScore + analysisScore + docScore) / 2.0, 1.0);
            return new IntentResult("complex_task", conf);
        }

        // 单意图（按优先级 + 得分排序）
        if (codeGenScore > 0 && codeGenScore >= reviewScore && codeGenScore >= analysisScore) {
            return new IntentResult("code_generation", Math.max(codeGenScore, 0.6));
        }
        if (reviewScore > 0) {
            return new IntentResult("code_review", Math.max(reviewScore, 0.6));
        }
        if (docScore > 0) {
            return new IntentResult("documentation", Math.max(docScore, 0.6));
        }
        if (analysisScore > 0) {
            return new IntentResult("analysis", Math.max(analysisScore, 0.5));
        }

        // 有代码上下文但没有明确意图 → 默认走代码审查
        if (codeContext != null && !codeContext.isBlank()) {
            return new IntentResult("code_review", 0.4);
        }

        return new IntentResult("general", 0.3);
    }

    String getIntentLabel(String intent) {
        return switch (intent) {
            case "code_generation" -> "💻 代码生成与审查";
            case "code_review"     -> "🔍 代码审查";
            case "analysis"        -> "📊 技术分析与问答";
            case "documentation"   -> "📝 文档生成";
            case "complex_task"    -> "🚀 综合任务编排";
            case "general"         -> "💬 通用问答";
            default                -> "❓ 未知意图";
        };
    }

    // ============================================================
    // 流水线定义
    // ============================================================

    List<String> getPipeline(String intent) {
        return switch (intent) {
            case "code_generation" -> List.of("代码生成", "代码审查");
            case "code_review"     -> List.of("代码审查");
            case "analysis"        -> List.of("技术分析");
            case "documentation"   -> List.of("文档生成");
            case "complex_task"    -> List.of("项目分析", "代码实现", "代码审查", "文档生成");
            default                -> List.of("智能问答");
        };
    }

    // ============================================================
    // P6: 带超时保护和熔断降级的流水线执行
    // ============================================================

    private List<OrchestrationResult.StepResult> executePipelineWithGuard(
            String intent, String message, String codeContext, List<String> pipeline) {

        List<OrchestrationResult.StepResult> results = new ArrayList<>();
        String previousOutput = null;
        int consecutiveFailures = 0;

        for (int i = 0; i < pipeline.size(); i++) {
            String stepName = pipeline.get(i);
            int stepNum = i + 1;

            // P6 熔断检查：连续失败超过阈值，中止后续步骤
            if (consecutiveFailures >= FAILURE_CIRCUIT_BREAKER) {
                log.warn("[Orchestrator] 连续 {} 步失败，触发熔断，跳过剩余步骤", consecutiveFailures);
                results.add(OrchestrationResult.StepResult.builder()
                        .stepNumber(stepNum).stepName(stepName)
                        .agentMethod("—")
                        .result(null).durationMs(0)
                        .success(false)
                        .error("⚡ 熔断降级：前序步骤连续失败，已跳过此步骤")
                        .build());
                continue;
            }

            log.info("[Orchestrator] 执行步骤 {}/{}: {}", stepNum, pipeline.size(), stepName);
            long stepStart = System.currentTimeMillis();

            try {
                String stepInput = buildStepInputSmart(message, codeContext, previousOutput, stepName);

                // P6: 使用 CompletableFuture 实现单步骤超时
                String output = executeWithTimeout(stepName, stepInput, codeContext);
                long stepDuration = System.currentTimeMillis() - stepStart;

                results.add(OrchestrationResult.StepResult.builder()
                        .stepNumber(stepNum).stepName(stepName)
                        .agentMethod(getAgentMethodName(stepName))
                        .result(output).durationMs(stepDuration)
                        .success(true).isFixRound(false)
                        .build());

                previousOutput = output;
                consecutiveFailures = 0;  // 成功则重置
                log.info("[Orchestrator] 步骤 {} 完成，耗时={}ms", stepName, stepDuration);

            } catch (TimeoutException e) {
                long stepDuration = System.currentTimeMillis() - stepStart;
                log.error("[Orchestrator] 步骤 {} 超时 ({}ms)", stepName, stepDuration);
                results.add(OrchestrationResult.StepResult.builder()
                        .stepNumber(stepNum).stepName(stepName)
                        .agentMethod(getAgentMethodName(stepName))
                        .result(null).durationMs(stepDuration)
                        .success(false).isFixRound(false)
                        .error("⏰ 步骤超时（" + STEP_TIMEOUT_MS / 1000 + "s），已跳过")
                        .build());
                consecutiveFailures++;

            } catch (Exception e) {
                long stepDuration = System.currentTimeMillis() - stepStart;
                log.error("[Orchestrator] 步骤 {} 执行失败", stepName, e);
                results.add(OrchestrationResult.StepResult.builder()
                        .stepNumber(stepNum).stepName(stepName)
                        .agentMethod(getAgentMethodName(stepName))
                        .result(null).durationMs(stepDuration)
                        .success(false).isFixRound(false)
                        .error(e.getMessage())
                        .build());
                consecutiveFailures++;
            }
        }
        return results;
    }

    /** P6: 带超时的步骤执行（使用共享线程池，避免频繁创建/销毁） */
    private String executeWithTimeout(String stepName, String input, String codeContext)
            throws TimeoutException, InterruptedException {
        try {
            CompletableFuture<String> future = CompletableFuture.supplyAsync(
                    () -> routeToSubAgent(stepName, input, codeContext), sharedExecutor);
            return future.get(STEP_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (ExecutionException e) {
            throw new RuntimeException(e.getCause());
        }
    }

    // ============================================================
    // P2 + P3: 质量门禁 + 自动修正循环
    // ============================================================

    record QualityGateResult(int score, int fixRounds, List<OrchestrationResult.StepResult> additionalSteps) {}

    private QualityGateResult runQualityGate(
            String message, String codeContext,
            List<OrchestrationResult.StepResult> existingSteps, String intent) {

        // 找到最后一步代码生成/实现的结果
        String generatedCode = extractLatestCodeOutput(existingSteps);
        if (generatedCode == null || generatedCode.isBlank()) {
            return new QualityGateResult(-1, 0, null);
        }

        List<OrchestrationResult.StepResult> additionalSteps = new ArrayList<>();
        String language = detectLanguage(generatedCode);
        int currentScore = -1;
        int fixRounds = 0;

        for (int round = 0; round <= MAX_FIX_ROUNDS; round++) {
            // 运行质量验证
            long qgStart = System.currentTimeMillis();
            String qualityReport = codeQualityTool.validateCodeQuality(generatedCode, language);
            String securityReport = codeQualityTool.checkSecurityIssues(generatedCode);
            String complexityReport = codeQualityTool.analyzeComplexity(generatedCode);
            long qgDuration = System.currentTimeMillis() - qgStart;

            // 解析评分
            currentScore = parseQualityScore(qualityReport);
            log.info("[Orchestrator] 质量门禁第 {} 轮，评分: {}，耗时: {}ms", round, currentScore, qgDuration);

            // 记录质量门禁步骤
            String combinedReport = qualityReport + "\n\n" + securityReport + "\n\n" + complexityReport;
            additionalSteps.add(OrchestrationResult.StepResult.builder()
                    .stepNumber(existingSteps.size() + additionalSteps.size() + 1)
                    .stepName(round == 0 ? "质量门禁" : "质量门禁（第 " + round + " 轮修正后）")
                    .agentMethod("CodeQualityTool (validate + security + complexity)")
                    .result(combinedReport)
                    .durationMs(qgDuration)
                    .success(true)
                    .isFixRound(round > 0)
                    .build());

            // 通过 → 退出
            if (currentScore >= QUALITY_PASS_SCORE) {
                if (round > 0) {
                    log.info("[Orchestrator] 经过 {} 轮修正，质量评分达到 {}（及格线 {}）",
                            round, currentScore, QUALITY_PASS_SCORE);
                }
                break;
            }

            // P3: 未通过 → 触发自动修正
            if (round < MAX_FIX_ROUNDS) {
                fixRounds++;
                log.info("[Orchestrator] 评分 {} < {}，触发第 {} 轮自动修正",
                        currentScore, QUALITY_PASS_SCORE, round + 1);

                long fixStart = System.currentTimeMillis();
                String fixPrompt = buildFixPrompt(generatedCode, qualityReport, securityReport, language);
                try {
                    String fixedCode = agent.executeTask(fixPrompt, generatedCode);
                    long fixDuration = System.currentTimeMillis() - fixStart;

                    additionalSteps.add(OrchestrationResult.StepResult.builder()
                            .stepNumber(existingSteps.size() + additionalSteps.size() + 1)
                            .stepName("自动修正（第 " + (round + 1) + " 轮）")
                            .agentMethod("executeTask() — fix")
                            .result(fixedCode)
                            .durationMs(fixDuration)
                            .success(true)
                            .isFixRound(true)
                            .build());

                    // 用修正后的代码更新，供下一轮验证
                    generatedCode = extractCodeFromInput(fixedCode, null);
                } catch (Exception e) {
                    log.error("[Orchestrator] 自动修正失败", e);
                    additionalSteps.add(OrchestrationResult.StepResult.builder()
                            .stepNumber(existingSteps.size() + additionalSteps.size() + 1)
                            .stepName("自动修正（第 " + (round + 1) + " 轮）")
                            .agentMethod("executeTask() — fix")
                            .result(null)
                            .durationMs(System.currentTimeMillis() - fixStart)
                            .success(false).isFixRound(true)
                            .error("自动修正失败: " + e.getMessage())
                            .build());
                    break;
                }
            }
        }

        return new QualityGateResult(currentScore, fixRounds, additionalSteps);
    }

    /** 构建自动修正提示词（精简版：只发送问题描述 + 受影响代码段，而非完整代码+完整报告） */
    private String buildFixPrompt(String code, String qualityReport, String securityReport, String language) {
        // 从质量报告中提取具体问题行（❌ 和 🔴 标记的问题）
        String problemSummary = extractProblemsFromReport(qualityReport + "\n" + securityReport);

        // 截断代码：如果代码太长，只保留前 150 行
        String codeSnippet = code;
        String[] codeLines = code.split("\n");
        if (codeLines.length > 150) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 150; i++) sb.append(codeLines[i]).append("\n");
            codeSnippet = sb.toString() + "\n// ... 共 " + codeLines.length + " 行，已截取前 150 行";
        }

        return String.format("""
                请根据以下代码质量问题，定向修复代码中的具体问题。
                
                【语言】: %s
                
                【发现的问题】:
                %s
                
                【当前代码】:
                ```
                %s
                ```
                
                【修复要求】:
                1. 只修复上面列出的具体问题，不要改变业务逻辑或重写无关代码
                2. 修复后必须包含完整的 import 语句
                3. 确保修复后的代码可编译、可运行
                """, language, problemSummary, codeSnippet);
    }

    /** 从质量/安全报告中提取具体问题（❌、🔴、🟡 标记的行），控制总长度 */
    private String extractProblemsFromReport(String report) {
        if (report == null) return "无具体问题描述";
        StringBuilder problems = new StringBuilder();
        for (String line : report.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("❌") || trimmed.startsWith("🔴") || trimmed.startsWith("🟡")
                    || trimmed.startsWith("- ") || trimmed.contains("问题") || trimmed.contains("风险")) {
                problems.append(trimmed).append("\n");
                if (problems.length() > 1500) break; // 限制问题描述长度
            }
        }
        return problems.length() > 0 ? problems.toString() : report.substring(0, Math.min(report.length(), 1500));
    }

    /** 从质量报告中解析评分 */
    private int parseQualityScore(String report) {
        if (report == null) return -1;
        // 匹配 "质量评分: 85/100" 或 "质量评分: 85" 格式
        var matcher = Pattern.compile("质量评分[：:]\\s*(\\d+)").matcher(report);
        if (matcher.find()) {
            try { return Integer.parseInt(matcher.group(1)); } catch (NumberFormatException ignored) {}
        }
        return -1;
    }

    /** 从步骤结果中提取最新的代码输出 */
    private String extractLatestCodeOutput(List<OrchestrationResult.StepResult> steps) {
        for (int i = steps.size() - 1; i >= 0; i--) {
            OrchestrationResult.StepResult step = steps.get(i);
            if (step.isSuccess() && step.getResult() != null &&
                    ("代码生成".equals(step.getStepName()) || "代码实现".equals(step.getStepName()))) {
                return extractCodeFromInput(step.getResult(), null);
            }
        }
        return null;
    }

    // ============================================================
    // P5: 智能上下文传递（代码块提取而非粗暴截断）
    // ============================================================

    private String buildStepInputSmart(String message, String codeContext,
                                        String previousOutput, String currentStep) {
        StringBuilder input = new StringBuilder(message);

        boolean isCodeGenStep = "代码生成".equals(currentStep) || "代码实现".equals(currentStep);

        // 1. 注入缓存的项目上下文摘要（减少重复分析，节省 Token）
        if (isCodeGenStep) {
            String cachedSummary = projectContextCache.getCachedSummary();
            if (cachedSummary != null && !cachedSummary.isBlank()) {
                input.append("\n\n").append(cachedSummary);
                log.debug("[Orchestrator] 已注入缓存的项目摘要到步骤: {}", currentStep);
            }
        }

        // 2. Few-Shot 注入（log.md #8）：从知识库检索 1-2 个与任务相关的代码/知识片段作为参考示例
        //    仅在代码生成步骤注入，可显著提高代码风格一致性
        if (isCodeGenStep) {
            String fewShotContext = buildFewShotContext(message);
            if (!fewShotContext.isBlank()) {
                input.append("\n\n").append(fewShotContext);
                log.debug("[Orchestrator] Few-Shot 已注入到步骤: {}", currentStep);
            }
        }

        // 3. 历史教训注入（O6 补全）：从 SelfLearningManager 检索改进模式，避免重蹈覆辙
        try {
            String learningContext = selfLearningManager.buildLearningContext(message, 3);
            if (learningContext != null && !learningContext.isBlank()) {
                input.append("\n\n").append(learningContext);
                log.debug("[Orchestrator] 学习上下文已注入到步骤: {}", currentStep);
            }
        } catch (Exception e) {
            log.warn("[Orchestrator] 注入学习上下文失败（非关键）: {}", e.getMessage());
        }

        // 4. 相关代码上下文
        if (codeContext != null && !codeContext.isBlank()) {
            input.append("\n\n【相关代码】:\n").append(codeContext);
        }

        // 5. 上一步骤结果（智能提取代码块，避免截断关键代码）
        if (previousOutput != null && !previousOutput.isBlank()) {
            String extracted = extractCodeBlockOrSummarize(previousOutput);
            input.append("\n\n【上一步骤结果（").append(currentStep).append("的参考输入）】:\n")
                 .append(extracted);
        }

        return input.toString();
    }

    /**
     * Few-Shot 上下文构建（log.md #8）
     * 从知识库中检索与用户任务语义最相关的 1-2 个知识片段，
     * 以【参考示例】的形式注入 Prompt，让 AI 在生成代码时参考项目已有的风格和模式。
     * 
     * 检索条件：minScore=0.65（较高阈值，确保相关性）
     * 返回长度上限：2000 字符（避免挤占生成空间）
     */
    private String buildFewShotContext(String userMessage) {
        try {
            List<KnowledgeDocument> examples = knowledgeBaseManager.searchRelevantDocuments(
                    userMessage, 2, 0.65);
            if (examples.isEmpty()) return "";

            StringBuilder sb = new StringBuilder();
            sb.append("【参考示例 — 以下是项目中已有的相关知识/代码风格，请参考】\n");
            int totalLen = 0;
            for (KnowledgeDocument doc : examples) {
                String content = doc.getContent();
                // 截取每个示例最多 1000 字符
                if (content.length() > 1000) {
                    content = content.substring(0, 1000) + "\n... (示例截取)";
                }
                // 总长度不超过 2000 字符
                if (totalLen + content.length() > 2000) break;
                sb.append("---\n").append(content).append("\n");
                totalLen += content.length();
            }
            return sb.toString();
        } catch (Exception e) {
            log.warn("[Orchestrator] Few-Shot 知识库检索失败（非关键）: {}", e.getMessage());
            return "";
        }
    }

    /**
     * P5: 从 LLM 输出中智能提取代码块
     * 如果有完整的 ``` 代码块，提取所有代码块；否则按段落截取并保留完整性
     */
    private String extractCodeBlockOrSummarize(String text) {
        // 尝试提取所有 Markdown 代码块
        StringBuilder codeBlocks = new StringBuilder();
        int searchFrom = 0;
        while (true) {
            int start = text.indexOf("```", searchFrom);
            if (start < 0) break;
            int langEnd = text.indexOf('\n', start);
            if (langEnd < 0) break;
            int end = text.indexOf("```", langEnd + 1);
            if (end < 0) break;
            codeBlocks.append(text, start, end + 3).append("\n\n");
            searchFrom = end + 3;
        }

        if (codeBlocks.length() > 0) {
            // 代码块总长度不超过 6000 字符
            String result = codeBlocks.toString();
            return result.length() > 6000 ? result.substring(0, 6000) + "\n... (代码过长已截取)" : result;
        }

        // 没有代码块 → 智能截取（按段落边界截取，不超过 4000 字符）
        if (text.length() <= 4000) return text;
        int cutPoint = text.lastIndexOf('\n', 4000);
        if (cutPoint < 2000) cutPoint = 4000;  // 找不到好的断点就直接截
        return text.substring(0, cutPoint) + "\n... (内容过长已截取)";
    }

    // ============================================================
    // 路由 + 辅助方法
    // ============================================================

    /**
     * 路由到子 Agent（按步骤类型动态选择完整/轻量 Agent）
     * - 代码生成/实现/审查 → 完整 Agent（需要全部工具）
     * - 技术分析/项目分析/智能问答/文档生成 → 轻量 Agent（仅需文件+知识库）
     */
    private String routeToSubAgent(String stepName, String input, String codeContext) {
        return switch (stepName) {
            case "代码生成", "代码实现" ->
                    agent.executeTask(input, codeContext);
            case "代码审查" -> {
                String codeToReview = extractCodeFromInput(input, codeContext);
                String language = detectLanguage(codeToReview);
                yield agent.reviewCode(codeToReview, language);
            }
            case "技术分析", "项目分析", "智能问答" ->
                    lightweightQaAgent.answerTechnicalQuestion(input);
            case "文档生成" -> {
                String docType = inferDocType(input);
                yield lightweightQaAgent.generateDocumentation(input, docType);
            }
            default ->
                    lightweightQaAgent.answerTechnicalQuestion(input);
        };
    }

    private String extractCodeFromInput(String input, String codeContext) {
        if (codeContext != null && !codeContext.isBlank()) return codeContext;
        // 提取第一个完整的 Markdown 代码块
        int start = input.indexOf("```");
        if (start >= 0) {
            int langEnd = input.indexOf('\n', start);
            if (langEnd > 0) {
                int end = input.indexOf("```", langEnd + 1);
                if (end > langEnd) return input.substring(langEnd + 1, end).trim();
            }
        }
        return input;
    }

    private String detectLanguage(String code) {
        if (code == null) return "java";
        String lower = code.toLowerCase();
        if (lower.contains("package ") || lower.contains("import java.") || lower.contains("public class ")) return "java";
        if (lower.contains("def ") && lower.contains(":") && !lower.contains("function")) return "python";
        if (lower.contains("function ") || lower.contains("const ") || lower.contains("=> ")) return "javascript";
        if (lower.contains("func ") && lower.contains("package ")) return "go";
        if (lower.contains("fn ") && lower.contains("let mut")) return "rust";
        if (lower.contains("import ") && lower.contains("type ") && lower.contains("interface ")) return "typescript";
        return "java";
    }

    private String inferDocType(String message) {
        String lower = message.toLowerCase();
        if (lower.contains("readme"))    return "README";
        if (lower.contains("api"))       return "API文档";
        if (lower.contains("架构"))      return "架构文档";
        if (lower.contains("使用指南") || lower.contains("使用说明")) return "使用指南";
        if (lower.contains("changelog")) return "Changelog";
        return "技术文档";
    }

    private String getAgentMethodName(String stepName) {
        return switch (stepName) {
            case "代码生成", "代码实现" -> "executeTask()";
            case "代码审查"             -> "reviewCode()";
            case "技术分析", "项目分析", "智能问答" -> "answerTechnicalQuestion()";
            case "文档生成"             -> "generateDocumentation()";
            default                    -> "answerTechnicalQuestion()";
        };
    }
}
