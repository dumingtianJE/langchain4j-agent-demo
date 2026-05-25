package com.yourcompany.langchain4j.config;

import com.yourcompany.langchain4j.agent.AiProgrammingAgent;
import com.yourcompany.langchain4j.supervisor.AiSupervisor;
import com.yourcompany.langchain4j.tool.CodeAnalysisTool;
import com.yourcompany.langchain4j.tool.CodeFileTool;
import com.yourcompany.langchain4j.tool.KnowledgeBaseTool;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.TokenWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
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
    
    private final ChatLanguageModel chatLanguageModel;
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
     * 注意：需要在 application.yml 中配置相应的 API 密钥
     */
    @Bean
    public EmbeddingModel embeddingModel() {
        // 这里使用 OpenAI 兼容的 embedding 模型
        // 生产环境可以替换为其他 embedding 服务
        return new dev.langchain4j.model.openai.OpenAiEmbeddingModel.builder()
            .baseUrl("https://dashscope.aliyuncs.com/compatible-mode/v1")
            .apiKey(System.getenv("DASHSCOPE_API_KEY"))
            .modelName("text-embedding-v3")
            .build();
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
            .chatLanguageModel(chatLanguageModel)
            .chatMemory(chatMemory)
            .tools(codeFileTool, codeAnalysisTool, knowledgeBaseTool)
            .build();
    }
}
