package com.yourcompany.langchain4j.config;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.message.AiMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 上下文优化器
 * 解决上下文超过最大限度的问题
 * 
 * 优化策略：
 * 1. 智能摘要压缩
 * 2. 优先级保留
 * 3. 分层上下文
 * 4. 滑动窗口
 * 5. 关键信息提取
 */
@Slf4j
@Component
public class ContextOptimizer {
    
    /**
     * 默认最大 Token 数（qwen-max 限制）
     */
    private static final int DEFAULT_MAX_TOKENS = 8000;
    
    /**
     * 系统提示词固定占用
     */
    private static final int SYSTEM_PROMPT_TOKENS = 500;
    
    /**
     * 每条消息平均 Token 估算
     */
    private static final int AVG_TOKENS_PER_MESSAGE = 150;
    
    /**
     * 优化上下文
     * 
     * @param messages 原始消息列表
     * @param maxTokens 最大 Token 限制
     * @return 优化后的消息列表
     */
    public List<ChatMessage> optimizeContext(List<ChatMessage> messages, int maxTokens) {
        if (messages == null || messages.isEmpty()) {
            return messages;
        }
        
        // 估算当前 Token 数
        int currentTokens = estimateTokens(messages);
        
        if (currentTokens <= maxTokens) {
            return messages;  // 未超限，直接返回
        }
        
        log.warn("上下文超限: {} / {}，开始优化", currentTokens, maxTokens);
        
        // 策略1: 保留系统消息
        List<ChatMessage> systemMessages = messages.stream()
            .filter(m -> m instanceof SystemMessage)
            .collect(Collectors.toList());
        
        // 策略2: 保留最近的对话（滑动窗口）
        List<ChatMessage> recentMessages = extractRecentMessages(messages, maxTokens);
        
        // 策略3: 提取关键信息
        List<ChatMessage> keyMessages = extractKeyInformation(messages, maxTokens);
        
        // 合并结果
        List<ChatMessage> optimized = new ArrayList<>();
        optimized.addAll(systemMessages);
        optimized.addAll(keyMessages);
        optimized.addAll(recentMessages);
        
        int optimizedTokens = estimateTokens(optimized);
        log.info("上下文优化完成: {} -> {} Tokens (减少 {}%)",
            currentTokens, optimizedTokens,
            (currentTokens - optimizedTokens) * 100 / currentTokens);
        
        return optimized;
    }
    
    /**
     * 策略1: 滑动窗口 - 保留最近 N 条消息
     */
    private List<ChatMessage> extractRecentMessages(List<ChatMessage> messages, int maxTokens) {
        int availableTokens = maxTokens - SYSTEM_PROMPT_TOKENS;
        int maxMessages = availableTokens / AVG_TOKENS_PER_MESSAGE;
        
        // 从后往前取最近的消息
        int startIndex = Math.max(0, messages.size() - maxMessages);
        List<ChatMessage> recentMessages = messages.subList(startIndex, messages.size());
        
        log.debug("滑动窗口策略: 保留最近 {} 条消息", recentMessages.size());
        
        return recentMessages;
    }
    
    /**
     * 策略2: 提取关键信息 - 识别并保留重要对话
     */
    private List<ChatMessage> extractKeyInformation(List<ChatMessage> messages, int maxTokens) {
        List<ChatMessage> keyMessages = new ArrayList<>();
        int usedTokens = SYSTEM_PROMPT_TOKENS;
        int availableTokens = maxTokens - usedTokens;
        
        // 关键信息标记
        Set<String> keyPhrases = Set.of(
            "重要", "关键", "必须", "注意", "警告", "错误",
            "important", "critical", "warning", "error",
            "requirement", "specification", "api", "database"
        );
        
        // 按重要性排序
        List<ScoredMessage> scoredMessages = messages.stream()
            .filter(m -> !(m instanceof SystemMessage))
            .map(m -> new ScoredMessage(m, calculateImportance(m, keyPhrases)))
            .sorted(Comparator.comparingDouble(ScoredMessage::getScore).reversed())
            .collect(Collectors.toList());
        
        // 选取高重要性消息
        for (ScoredMessage scored : scoredMessages) {
            int messageTokens = estimateTokens(scored.getMessage());
            
            if (usedTokens + messageTokens <= availableTokens && scored.getScore() > 0.7) {
                keyMessages.add(scored.getMessage());
                usedTokens += messageTokens;
            }
        }
        
        log.debug("关键信息提取: 保留 {} 条高重要性消息", keyMessages.size());
        
        return keyMessages;
    }
    
    /**
     * 计算消息重要性分数
     */
    private double calculateImportance(ChatMessage message, Set<String> keyPhrases) {
        String content = getMessageContent(message).toLowerCase();
        double score = 0.0;
        
        // 包含关键词
        for (String phrase : keyPhrases) {
            if (content.contains(phrase)) {
                score += 0.2;
            }
        }
        
        // 代码片段（通常更重要）
        if (content.contains("```") || content.contains("class ") || content.contains("function ")) {
            score += 0.3;
        }
        
        // 用户问题（比 AI 回复更重要）
        if (message instanceof UserMessage) {
            score += 0.2;
        }
        
        // 最新消息更重要
        int index = -1;  // 简化处理
        score += 0.1;
        
        return Math.min(score, 1.0);
    }
    
    /**
     * 策略3: 智能摘要 - 压缩长消息
     */
    public String summarizeMessage(String content, int maxTokens) {
        if (estimateTokens(content) <= maxTokens) {
            return content;
        }
        
        // 简单摘要策略：保留首尾，中间省略
        String[] lines = content.split("\n");
        if (lines.length <= 5) {
            return content;  // 短内容不压缩
        }
        
        StringBuilder summarized = new StringBuilder();
        summarized.append("【摘要】\n");
        
        // 保留前 2 行
        for (int i = 0; i < Math.min(2, lines.length); i++) {
            summarized.append(lines[i]).append("\n");
        }
        
        summarized.append("...\n");
        
        // 保留后 2 行
        for (int i = Math.max(lines.length - 2, 3); i < lines.length; i++) {
            summarized.append(lines[i]).append("\n");
        }
        
        log.debug("消息摘要: {} -> {} 行", lines.length, 5);
        
        return summarized.toString();
    }
    
    /**
     * 策略4: 分层上下文管理
     * 
     * L1: 核心上下文（必须在对话中）
     * L2: 辅助上下文（按需加载）
     * L3: 历史上下文（存入向量数据库）
     */
    public ContextLayers createLayeredContext(List<ChatMessage> messages) {
        ContextLayers layers = new ContextLayers();
        
        int totalTokens = 0;
        int l1Limit = DEFAULT_MAX_TOKENS / 2;  // 50% 用于核心上下文
        
        // 从新到旧遍历
        for (int i = messages.size() - 1; i >= 0; i--) {
            ChatMessage msg = messages.get(i);
            int msgTokens = estimateTokens(msg);
            
            if (totalTokens + msgTokens <= l1Limit) {
                layers.getL1CoreContext().add(0, msg);  // 插入到开头保持顺序
                totalTokens += msgTokens;
            } else if (totalTokens + msgTokens <= l1Limit * 1.5) {
                layers.getL2AuxiliaryContext().add(0, msg);
                totalTokens += msgTokens;
            } else {
                layers.getL3HistoryContext().add(0, msg);
            }
        }
        
        log.info("分层上下文: L1={}条, L2={}条, L3={}条",
            layers.getL1CoreContext().size(),
            layers.getL2AuxiliaryContext().size(),
            layers.getL3HistoryContext().size());
        
        return layers;
    }
    
    /**
     * 动态调整上下文窗口
     */
    public List<ChatMessage> dynamicWindowSize(List<ChatMessage> messages, 
                                               int currentTokens, 
                                               int maxTokens) {
        double usageRatio = (double) currentTokens / maxTokens;
        
        if (usageRatio > 0.9) {
            // 紧急模式：大幅压缩
            log.warn("上下文使用率 {:.1f}%，启用紧急压缩", usageRatio * 100);
            return optimizeContext(messages, (int)(maxTokens * 0.6));
        } else if (usageRatio > 0.7) {
            // 警告模式：适度压缩
            log.info("上下文使用率 {:.1f}%，启用适度压缩", usageRatio * 100);
            return optimizeContext(messages, (int)(maxTokens * 0.8));
        } else {
            // 正常模式：不压缩
            return messages;
        }
    }
    
    /**
     * 估算 Token 数量
     */
    private int estimateTokens(List<ChatMessage> messages) {
        return messages.stream()
            .mapToInt(this::estimateTokens)
            .sum();
    }
    
    /**
     * 估算单条消息 Token 数
     */
    private int estimateTokens(ChatMessage message) {
        String content = getMessageContent(message);
        // 简单估算：中文 1.5 字符/token，英文 4 字符/token
        return (int)(content.length() * 0.5);
    }
    
    /**
     * 估算字符串 Token 数
     */
    private int estimateTokens(String content) {
        return (int)(content.length() * 0.5);
    }
    
    /**
     * 获取消息内容
     */
    private String getMessageContent(ChatMessage message) {
        if (message instanceof SystemMessage) {
            return ((SystemMessage) message).text();
        } else if (message instanceof UserMessage) {
            return ((UserMessage) message).text();
        } else if (message instanceof AiMessage) {
            return ((AiMessage) message).text();
        }
        return "";
    }
    
    /**
     * 打分消息
     */
    private static class ScoredMessage {
        private final ChatMessage message;
        private final double score;
        
        public ScoredMessage(ChatMessage message, double score) {
            this.message = message;
            this.score = score;
        }
        
        public ChatMessage getMessage() { return message; }
        public double getScore() { return score; }
    }
    
    /**
     * 分层上下文
     */
    public static class ContextLayers {
        private List<ChatMessage> l1CoreContext = new ArrayList<>();
        private List<ChatMessage> l2AuxiliaryContext = new ArrayList<>();
        private List<ChatMessage> l3HistoryContext = new ArrayList<>();
        
        public List<ChatMessage> getL1CoreContext() { return l1CoreContext; }
        public List<ChatMessage> getL2AuxiliaryContext() { return l2AuxiliaryContext; }
        public List<ChatMessage> getL3HistoryContext() { return l3HistoryContext; }
        
        /**
         * 获取可用于对话的上下文（L1 + 部分 L2）
         */
        public List<ChatMessage> getActiveContext() {
            List<ChatMessage> active = new ArrayList<>();
            active.addAll(l1CoreContext);
            active.addAll(l2AuxiliaryContext);
            return active;
        }
    }
}
