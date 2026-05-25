package com.yourcompany.langchain4j.config;

import com.yourcompany.langchain4j.agent.AiProgrammingAgent;
import com.yourcompany.langchain4j.supervisor.AiSupervisor;
import com.yourcompany.langchain4j.tool.CodeAnalysisTool;
import com.yourcompany.langchain4j.tool.CodeFileTool;
import com.yourcompany.langchain4j.tool.KnowledgeBaseTool;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.TokenWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AI 编程 Agent 配置
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class AiProgrammingAgentConfig {
    
    private final ChatModel chatModel;
    private final CodeFileTool codeFileTool;
    private final CodeAnalysisTool codeAnalysisTool;
    private final KnowledgeBaseTool knowledgeBaseTool;
    private final AiSupervisor aiSupervisor;
    
    /**
     * 配置内存向量存储（生产环境建议使用持久化方案如 Milvus、Pinecone 等）
     */
    @Bean
    public EmbeddingStore<dev.langchain4j.data.segment.TextSegment> embeddingStore() {
        log.info("初始化向量存储（In-Memory）");
        return new InMemoryEmbeddingStore<>();
    }
    
    /**
     * 配置 Embedding 模型
     * 使用通义千问的 Embedding 服务（OpenAI 兼容接口）
     */
    @Bean
    public EmbeddingModel embeddingModel() {
        log.info("初始化 Embedding 模型 (text-embedding-v3)");
        
        // 使用 OpenAI 兼容接口调用通义千问 Embedding
        return new dev.langchain4j.model.openai.OpenAiEmbeddingModel(
            "sk-b1095efa41cf4fee82ef4e13ae6a6c9f",  // API Key
            null,  // organization ID
            null,  // user
            "https://dashscope.aliyuncs.com/compatible-mode/v1",  // Base URL
            "text-embedding-v3",  // Model name
            null,  // dimensions
            null,  // timeout
            0,     // max retries
            null,  // log requests
            null   // log responses
        );
    }
    
    /**
     * 配置 Chat Memory（对话记忆）
     * 使用 Token 窗口记忆，保留最近的对话上下文
     */
    @Bean
    public ChatMemory chatMemory() {
        return TokenWindowChatMemory.withMaxTokens(8000);
    }
    
    /**
     * 配置 AI 编程 Agent
     * 集成所有工具和能力
     */
    @Bean
    public AiProgrammingAgent aiProgrammingAgent(ChatMemory chatMemory) {
        log.info("初始化 AI 编程 Agent");
        
        return dev.langchain4j.service.AiServices.builder(AiProgrammingAgent.class)
            .chatModel(chatModel)
            .chatMemory(chatMemory)
            .tools(codeFileTool, codeAnalysisTool, knowledgeBaseTool)
            .build();
    }
}
