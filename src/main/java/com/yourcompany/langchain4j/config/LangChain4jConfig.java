package com.yourcompany.langchain4j.config;

import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * LangChain4j 配置类
 * 配置 ChatMemory 等组件
 */
@Configuration
public class LangChain4jConfig {

    /**
     * 配置聊天记忆提供者
     * 使用 MessageWindowChatMemory 基于消息数量管理记忆窗口
     * 
     * @return ChatMemoryProvider
     */
    @Bean
    ChatMemoryProvider chatMemoryProvider() {
        return chatId -> MessageWindowChatMemory.withMaxMessages(10);
    }
}
