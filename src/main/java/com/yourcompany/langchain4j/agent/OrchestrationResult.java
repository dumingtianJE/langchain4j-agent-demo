package com.yourcompany.langchain4j.agent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * AgentOrchestrator 编排结果
 * 记录完整的编排执行过程：意图分类 → 流水线 → 质量门禁 → 各步骤结果 → 最终汇总
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrchestrationResult {

    /** 分类意图：code_generation / code_review / analysis / documentation / complex_task / general */
    private String intent;

    /** 意图的中文描述 */
    private String intentLabel;

    /** 意图分类置信度（0.0 ~ 1.0） */
    private double confidence;

    /** 执行的流水线步骤名称列表，如 ["代码生成", "质量门禁", "代码审查"] */
    private List<String> pipeline;

    /** 每个步骤的详细执行结果 */
    private List<StepResult> steps;

    /** 最终汇总结果（最后一步的输出，或所有步骤的合并摘要） */
    private String finalResult;

    /** 质量门禁评分（0~100，仅代码生成类任务），-1 表示未执行 */
    private int qualityScore;

    /** 自动修正轮次（0 表示无修正） */
    private int fixRounds;

    /** 总耗时（毫秒） */
    private long totalDurationMs;

    /** 是否执行成功 */
    private boolean success;

    /** 错误信息（如果失败） */
    private String errorMessage;

    /**
     * 单步骤执行结果
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StepResult {
        /** 步骤序号（从 1 开始） */
        private int stepNumber;
        /** 步骤名称，如 "代码生成"、"质量门禁"、"代码审查" */
        private String stepName;
        /** 该步骤使用的 Agent 方法 */
        private String agentMethod;
        /** 该步骤的输出结果 */
        private String result;
        /** 该步骤耗时（毫秒） */
        private long durationMs;
        /** 是否成功 */
        private boolean success;
        /** 错误信息 */
        private String error;
        /** 是否为自动修正轮次 */
        private boolean isFixRound;
    }
}
