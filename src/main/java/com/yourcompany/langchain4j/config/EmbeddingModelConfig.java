package com.yourcompany.langchain4j.config;

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import dev.langchain4j.data.segment.TextSegment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PreDestroy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Embedding 模型和存储配置
 * 优先使用 DashScope text-embedding-v3（支持中英文），API Key 不可用时降级为本地模型
 * 向量存储使用持久化文件，重启不丢失数据
 */
@Slf4j
@Configuration
public class EmbeddingModelConfig {
    
    @Value("${langchain4j.open-ai.chat-model.api-key:}")
    private String apiKey;
    
    @Value("${langchain4j.open-ai.chat-model.base-url:https://dashscope.aliyuncs.com/compatible-mode/v1}")
    private String baseUrl;
    
    @Value("${ai.embedding.filesystem-path:./data/embeddings}")
    private String filesystemPath;
    
    @Value("${ai.embedding.type:filesystem}")
    private String storeType;
    
    private InMemoryEmbeddingStore<TextSegment> persistentStore;
    
    /**
     * 配置向量存储（支持文件持久化）
     * 从文件加载已保存的向量数据，关闭时自动保存
     * 仅当 Milvus 未启用时使用 InMemoryEmbeddingStore
     */
    @Bean
    @ConditionalOnProperty(name = "ai.milvus.enabled", havingValue = "false", matchIfMissing = true)
    public EmbeddingStore<TextSegment> embeddingStore() {
        Path storageDir = Paths.get(filesystemPath);
        Path storeFile = storageDir.resolve("embeddings.json");
        
        try {
            Files.createDirectories(storageDir);
        } catch (Exception e) {
            log.warn("创建向量存储目录失败: {}", e.getMessage());
        }
        
        // 尝试从文件加载已保存的向量数据
        if ("filesystem".equals(storeType) && Files.exists(storeFile)) {
            try {
                String json = Files.readString(storeFile);
                persistentStore = InMemoryEmbeddingStore.fromJson(json);
                log.info("从文件加载向量存储成功: {}", storeFile);
                return persistentStore;
            } catch (Exception e) {
                log.warn("加载向量存储文件失败，将创建新存储: {}", e.getMessage());
            }
        }
        
        persistentStore = new InMemoryEmbeddingStore<>();
        log.info("初始化新的向量存储（In-Memory）");
        return persistentStore;
    }
    
    /**
     * 配置 Embedding 模型
     * 优先使用 DashScope text-embedding-v3（支持中英文），降级到本地模型
     */
    @Bean
    public EmbeddingModel embeddingModel() {
        // 尝试使用 DashScope API（支持中英文混合嵌入）
        if (apiKey != null && !apiKey.isBlank() && !apiKey.startsWith("sk-your")) {
            try {
                var dashscopeModel = dev.langchain4j.model.openai.OpenAiEmbeddingModel.builder()
                    .baseUrl(baseUrl)
                    .apiKey(apiKey)
                    .modelName("text-embedding-v3")
                    .dimensions(1024)
                    .build();
                log.info("Embedding 模型初始化成功: DashScope text-embedding-v3（中英文支持）");
                return dashscopeModel;
            } catch (Exception e) {
                log.warn("DashScope Embedding 模型初始化失败，降级为本地模型: {}", e.getMessage());
            }
        } else {
            log.warn("未配置 DASHSCOPE_API_KEY，将使用本地 Embedding 模型（仅英文）");
        }
        
        // 降级：本地 Embedding 模型（无需 API Key）
        log.info("Embedding 模型初始化: 本地 AllMiniLmL6V2");
        return new dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel();
    }
    
    /**
     * 应用关闭时保存向量数据到磁盘
     */
    @PreDestroy
    public void saveOnShutdown() {
        if (persistentStore != null && "filesystem".equals(storeType)) {
            try {
                Path storeFile = Paths.get(filesystemPath, "embeddings.json");
                // 使用 LangChain4j 内置的 JSON 序列化
                String json = persistentStore.serializeToJson();
                Files.writeString(storeFile, json);
                log.info("向量存储已保存到磁盘: {} ({} 字节)", storeFile, json.length());
            } catch (Exception e) {
                log.error("保存向量存储失败: {}", e.getMessage());
            }
        }
    }
}
