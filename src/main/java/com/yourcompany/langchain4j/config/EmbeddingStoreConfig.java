package com.yourcompany.langchain4j.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 向量存储配置
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "ai.embedding")
public class EmbeddingStoreConfig {
    
    /**
     * 存储类型：memory（内存）、filesystem（文件系统）
     */
    private String type = "filesystem";
    
    /**
     * 文件系统存储路径
     */
    private String filesystemPath = "./data/embeddings";
    
    /**
     * 自动保存间隔（秒）
     */
    private int autoSaveInterval = 300;
}
