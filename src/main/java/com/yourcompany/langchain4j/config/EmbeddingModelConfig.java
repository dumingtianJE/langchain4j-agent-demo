package com.yourcompany.langchain4j.config;

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Embedding 模型和存储配置
 * 独立配置类，避免与 AiProgrammingAgentConfig 产生循环依赖
 */
@Slf4j
@Configuration
public class EmbeddingModelConfig {
    
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
     * 使用本地嵌入模型（无需 API Key）
     */
    @Bean
    public EmbeddingModel embeddingModel() {
        log.info("初始化本地 Embedding 模型 (all-minilm-l6-v2)");
        
        // 使用本地嵌入模型，不需要 API Key
        return new dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel();
    }
}
