package com.yourcompany.langchain4j.controller;

import com.yourcompany.langchain4j.agent.AiProgrammingAgent;
import com.yourcompany.langchain4j.security.PromptInjectionGuard;
import com.yourcompany.langchain4j.service.CodeFileService;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 统一 AI 聊天控制器
 * 提供同步 AI 对话接口和 SSE 流式输出接口
 */
@Slf4j
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiChatController {
    
    private final AiProgrammingAgent aiProgrammingAgent;
    private final StreamingChatModel streamingChatModel;
    private final PromptInjectionGuard promptInjectionGuard;
    private final CodeFileService codeFileService;
    
    // 异步执行线程池（用于流式响应）
    private final ExecutorService sseExecutor = Executors.newCachedThreadPool();
    
    /**
     * 统一 AI 对话接口（同步）
     */
    @PostMapping("/chat")
    public ResponseEntity<Map<String, Object>> chat(@RequestBody ChatRequest request) {
        log.info("收到 AI 对话请求: {}", request.getMessage());
        
        // Prompt 注入防护
        String sanitized = promptInjectionGuard.sanitizeInput(request.getMessage());
        if (sanitized == null) {
            log.warn("拒绝请求: 检测到 Prompt 注入攻击");
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "reply", "❗ 检测到异常输入，请求被拒绝。",
                "timestamp", System.currentTimeMillis()
            ));
        }
        request.setMessage(sanitized);
        
        try {
            String response;
            
            // 构建用户消息（自动注入项目工作区路径）
            String userContent = request.getMessage();
            String projectContext = buildProjectContextHint(userContent);
            if (!projectContext.isEmpty()) {
                userContent = projectContext + "\n\n" + userContent;
                log.info("已注入项目上下文提示，工作区: {}", codeFileService.getCurrentProjectRoot());
            }
            
            if (request.getCodeContext() != null && !request.getCodeContext().isEmpty()) {
                userContent = userContent + "\n\n相关代码:\n" + request.getCodeContext();
                response = aiProgrammingAgent.answerTechnicalQuestion(userContent);
            } else {
                response = aiProgrammingAgent.answerTechnicalQuestion(userContent);
            }
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "reply", response,
                "timestamp", System.currentTimeMillis()
            ));
            
        } catch (Exception e) {
            log.error("AI 对话处理失败", e);
            
            String errorMessage = e.getMessage();
            String reply;
            
            // 检测工具调用序列错误
            if (errorMessage != null && errorMessage.contains("tool_calls")) {
                log.warn("检测到工具调用序列错误，可能是对话历史不一致导致");
                reply = "❗ 对话上下文异常，请清除对话历史后重试。\n\n" +
                        "建议操作：\n" +
                        "1. 点击右侧 AI 助手面板右上角的垃圾桶图标清除对话\n" +
                        "2. 或刷新页面后重新发送消息";
            } else if (errorMessage != null && errorMessage.contains("400")) {
                // 其他 400 错误（如 API 参数错误）
                reply = "❗ API 请求参数错误：" + errorMessage;
            } else {
                reply = "❗ 抱歉，处理您的请求时出现错误：" + errorMessage;
            }
            
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "reply", reply,
                "timestamp", System.currentTimeMillis()
            ));
        }
    }
    
    /**
     * SSE 流式 AI 对话接口
     * 支持逐 Token 推送，提升用户体验
     * 
     * 前端使用示例：
     * const eventSource = new EventSource('/api/ai/chat/stream?message=你的问题')
     * eventSource.onmessage = (event) => console.log(event.data)
     */
    @GetMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@RequestParam String message,
                                  @RequestParam(required = false) String codeContext) {
        log.info("收到 SSE 流式对话请求: {}", message);
        
        // 超时设置 5 分钟，支持复杂任务
        SseEmitter emitter = new SseEmitter(300_000L);
        
        sseExecutor.execute(() -> {
            try {
                // 发送开始信号
                emitter.send(SseEmitter.event()
                    .name("start")
                    .data("{\"status\":\"thinking\",\"message\":\"AI 正在思考中...\"}"));
                
                // 构建消息列表
                String userContent = message;
                if (codeContext != null && !codeContext.isBlank()) {
                    userContent = message + "\n\n相关代码:\n" + codeContext;
                }
                
                List<ChatMessage> messages = List.of(
                    SystemMessage.from("你是一个专业的 AI 编程助手，请简洁、准确地回答用户的技术问题。"),
                    UserMessage.from(userContent)
                );
                
                StringBuilder fullResponse = new StringBuilder();
                
                // 流式调用 LLM
                streamingChatModel.chat(messages, new StreamingChatResponseHandler() {
                    @Override
                    public void onPartialResponse(String partialResponse) {
                        try {
                            fullResponse.append(partialResponse);
                            emitter.send(SseEmitter.event()
                                .name("token")
                                .data(partialResponse));
                        } catch (Exception e) {
                            log.warn("发送 SSE Token 失败: {}", e.getMessage());
                        }
                    }
                    
                    @Override
                    public void onCompleteResponse(ChatResponse chatResponse) {
                        try {
                            AiMessage aiMessage = chatResponse.aiMessage();
                            if (aiMessage != null && fullResponse.length() == 0) {
                                emitter.send(SseEmitter.event()
                                    .name("token")
                                    .data(aiMessage.text()));
                            }
                            emitter.send(SseEmitter.event()
                                .name("done")
                                .data("{\"status\":\"complete\"}"));
                            emitter.complete();
                            log.info("SSE 流式对话完成");
                        } catch (Exception e) {
                            log.warn("发送 SSE 完成信号失败: {}", e.getMessage());
                            emitter.complete();
                        }
                    }
                    
                    @Override
                    public void onError(Throwable error) {
                        try {
                            log.error("SSE 流式对话错误", error);
                            emitter.send(SseEmitter.event()
                                .name("error")
                                .data("{\"error\":\"" + error.getMessage() + "\"}"));
                        } catch (Exception e) {
                            log.warn("发送 SSE 错误信号失败", e);
                        }
                        emitter.completeWithError(error);
                    }
                });
                
            } catch (Exception e) {
                log.error("SSE 流式对话处理失败", e);
                try {
                    emitter.send(SseEmitter.event()
                        .name("error")
                        .data("{\"error\":\"处理失败: " + e.getMessage() + "\"}"));
                } catch (Exception ex) {
                    log.warn("发送 SSE 错误信号失败", ex);
                }
                emitter.completeWithError(e);
            }
        });
        
        // 连接超时或断开时清理
        emitter.onTimeout(() -> log.warn("SSE 连接超时"));
        emitter.onCompletion(() -> log.debug("SSE 连接已关闭"));
        
        return emitter;
    }
    
    /**
     * 健康检查
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
            "success", true,
            "status", "AI Chat Service is running",
            "timestamp", System.currentTimeMillis()
        ));
    }
    
    /**
     * 获取项目上下文摘要（结构化项目分析，供前端 AI 助手使用）
     * 整合 CodeFileService 和 ProjectContextTool 的能力
     */
    @GetMapping("/project-context")
    public ResponseEntity<Map<String, Object>> projectContext() {
        try {
            String projectRoot = codeFileService.getCurrentProjectRoot();
            String workspaceRoot = codeFileService.getWorkspaceRoot();
            
            // 获取目录树（浅层，最多3层，用于摘要）
            Map<String, Object> treeData = codeFileService.buildDirectoryTree(".");
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "projectRoot", projectRoot,
                "workspaceRoot", workspaceRoot,
                "treeSummary", summarizeTree(treeData),
                "timestamp", System.currentTimeMillis()
            ));
        } catch (Exception e) {
            log.warn("获取项目上下文失败: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", "获取项目上下文失败: " + e.getMessage()
            ));
        }
    }
    
    /** 将目录树压缩为文本摘要（最多展示前50个条目） */
    private String summarizeTree(Map<String, Object> treeData) {
        Object tree = treeData.get("tree");
        if (tree == null) return "无法获取目录树";
        StringBuilder sb = new StringBuilder();
        flattenTree(tree, "", 0, sb, new int[]{0}, 50);
        return sb.toString();
    }
    
    @SuppressWarnings("unchecked")
    private void flattenTree(Object node, String indent, int depth, StringBuilder sb, int[] counter, int max) {
        if (node == null || counter[0] >= max) return;
        if (!(node instanceof Map)) return;
        Map<String, Object> map = (Map<String, Object>) node;
        String name = String.valueOf(map.getOrDefault("name", ""));
        String type = String.valueOf(map.getOrDefault("type", ""));
        counter[0]++;
        sb.append(indent).append("directory".equals(type) ? "📁 " : "📄 ").append(name).append("\n");
        if ("directory".equals(type) && depth < 3) {
            Object children = map.get("children");
            if (children instanceof List) {
                for (Object child : (List<?>) children) {
                    if (counter[0] >= max) break;
                    flattenTree(child, indent + "  ", depth + 1, sb, counter, max);
                }
            }
        }
    }
    
    /**
     * 聊天请求 DTO
     */
    @Data
    public static class ChatRequest {
        private String message;
        private String codeContext;
        private String userId;
    }
    
    /**
     * 检测消息是否涉及项目分析，自动注入工作区路径提示
     * 触发关键词：分析/架构/业务/项目结构/代码结构/模块/技术栈/依赖
     */
    private String buildProjectContextHint(String message) {
        if (message == null || message.isBlank()) return "";
        String lower = message.toLowerCase();
        boolean projectRelated = lower.contains("分析") || lower.contains("架构") 
                || lower.contains("业务") || lower.contains("项目结构")
                || lower.contains("代码结构") || lower.contains("模块")
                || lower.contains("技术栈") || lower.contains("依赖")
                || lower.contains("解析") || lower.contains("全量")
                || lower.contains("业务逻辑") || lower.contains("业务架构");
        if (!projectRelated) return "";
        String projectRoot = codeFileService.getCurrentProjectRoot();
        return "【系统上下文】当前项目工作区路径为: " + projectRoot 
                + "\n请使用工具主动读取和分析该项目，不要说无法访问项目。";
    }
}
