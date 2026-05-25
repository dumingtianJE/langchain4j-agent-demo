package com.yourcompany.langchain4j.config;

import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.TokenWindowChatMemory;
import dev.langchain4j.model.Tokenizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * LangChain4j 配置类
 * 配置 ChatMemory、Tokenizer 等组件
 */
@Configuration
public class LangChain4jConfig {

    /**
     * 配置聊天记忆提供者
     * 使用 TokenWindowChatMemory 基于 Token 数量管理记忆窗口
     * 
     * @param tokenizer Token 计算器
     * @return ChatMemoryProvider
     */
    @Bean
    ChatMemoryProvider chatMemoryProvider(Tokenizer tokenizer) {
        return chatId -> TokenWindowChatMemory.withMaxTokens(1000, tokenizer);
    }
}
