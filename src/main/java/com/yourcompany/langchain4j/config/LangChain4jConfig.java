package com.yourcompany.langchain4j.config;

import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * LangChain4j 配置类
 * 配置 ChatMemory 等组件
 */
@Configuration
public class LangChain4jConfig {

    @Value("${ai.chat.memory.provider-max-messages:30}")
    private int maxMessages;

    /**
     * 默认 ChatMemory（供 @AiService 接口使用）
     */
    @Bean
    ChatMemory chatMemory() {
        return MessageWindowChatMemory.withMaxMessages(maxMessages);
    }

    /**
     * 配置聊天记忆提供者
     * 使用 MessageWindowChatMemory 基于消息数量管理记忆窗口
     * 每个会话（chatId）维护独立的记忆，最多保留 30 条消息
     * 
     * @return ChatMemoryProvider
     */
    @Bean
    ChatMemoryProvider chatMemoryProvider() {
        return chatId -> MessageWindowChatMemory.withMaxMessages(30);
    }
}
