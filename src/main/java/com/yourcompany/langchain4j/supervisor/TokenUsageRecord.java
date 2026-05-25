package com.yourcompany.langchain4j.supervisor;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Token 使用记录
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TokenUsageRecord {
    
    /**
     * 记录 ID
     */
    private String id;
    
    /**
     * Agent 名称
     */
    private String agentName;
    
    /**
     * 用户 ID
     */
    private String userId;
    
    /**
     * 请求类型（chat、code-review、question 等）
     */
    private String requestType;
    
    /**
     * 输入 token 数
     */
    private Integer inputTokens;
    
    /**
     * 输出 token 数
     */
    private Integer outputTokens;
    
    /**
     * 总 token 数
     */
    private Integer totalTokens;
    
    /**
     * 请求耗时（毫秒）
     */
    private Long durationMs;
    
    /**
     * 是否触发警报
     */
    private Boolean alertTriggered;
    
    /**
     * 警报原因
     */
    private String alertReason;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
