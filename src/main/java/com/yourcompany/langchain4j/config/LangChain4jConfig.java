package com.yourcompany.langchain4j.config;

import com.yourcompany.langchain4j.supervisor.AiSupervisor;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.memory.chat.TokenWindowChatMemory;
import dev.langchain4j.model.TokenCountEstimator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * LangChain4j 配置类
 * 
 * 优化（log.md #3）：接入 ContextOptimizer — TokenWindowChatMemory 替代 MessageWindowChatMemory
 * 记忆管理从"数量驱动"变为"预算驱动"，精确控制上下文 Token 总量
 * 配合 ContextOptimizer 的 estimateTokens 实现中文感知 Token 计数
 */
@Configuration
public class LangChain4jConfig {

    @Value("${ai.chat.memory.provider-max-messages:20}")
    private int maxMessages;

    /**
     * Token 预算上限（qwen-max 上下文窗口 8K 以内安全区域）
     */
    @Value("${ai.chat.memory.max-tokens:8000}")
    private int maxTokens;

    /**
     * TokenCountEstimator：基于 AiSupervisor 的中文感知 Token 估算器
     * 中文 1.5 tok/字、ASCII 4 tok/字，比 LangChain4j 默认估算更准确
     * 注：LangChain4j 1.x 中 Tokenizer 已重命名为 TokenCountEstimator，且不再是函数式接口
     */
    @Bean
    TokenCountEstimator tokenCountEstimator() {
        return new TokenCountEstimator() {
            @Override
            public int estimateTokenCountInText(String text) {
                return AiSupervisor.estimateTokens(text);
            }

            @Override
            public int estimateTokenCountInMessage(ChatMessage message) {
                return AiSupervisor.estimateTokens(message.toString());
            }

            @Override
            public int estimateTokenCountInMessages(Iterable<ChatMessage> messages) {
                int total = 0;
                for (ChatMessage msg : messages) {
                    total += estimateTokenCountInMessage(msg);
                }
                return total;
            }
        };
    }

    /**
     * 默认 ChatMemory（供完整 Agent 使用）
     * 优化（#3）：使用 TokenWindowChatMemory 替代 MessageWindowChatMemory
     * 预算驱动：基于 Token 总量淘汰旧消息，而非固定条数
     * 
     * 对比：
     * - MessageWindowChatMemory(20条)：20条消息可能 2000 token 也可能 20000 token
     * - TokenWindowChatMemory(8000tok)：精确控制上下文不超预算
     */
    @Bean
    ChatMemory chatMemory(TokenCountEstimator tokenCountEstimator) {
        return TokenWindowChatMemory.builder()
                .maxTokens(maxTokens, tokenCountEstimator)
                .build();
    }

    /**
     * ChatMemoryProvider（供多会话场景使用）
     * 每个 chatId 维护独立的 Token 预算记忆
     */
    @Bean
    ChatMemoryProvider chatMemoryProvider(TokenCountEstimator tokenCountEstimator) {
        return chatId -> TokenWindowChatMemory.builder()
                .maxTokens(maxTokens, tokenCountEstimator)
                .build();
    }
}
