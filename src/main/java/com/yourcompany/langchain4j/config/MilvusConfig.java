package com.yourcompany.langchain4j.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Milvus 向量数据库配置
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "ai.milvus")
public class MilvusConfig {
    
    /**
     * Milvus 服务器地址
     */
    private String host = "localhost";
    
    /**
     * Milvus 端口
     */
    private int port = 19530;
    
    /**
     * 集合名称
     */
    private String collectionName = "ai_knowledge_embeddings";
    
    /**
     * 向量维度（根据 embedding 模型确定）
     */
    private int dimension = 1024;
    
    /**
     * 索引类型：IVF_FLAT, IVF_PQ, HNSW, FLAT
     */
    private String indexType = "HNSW";
    
    /**
     * 度量类型：L2, IP, COSINE
     */
    private String metricType = "COSINE";
    
    /**
     * HNSW 参数 - M
     */
    private int hnswM = 16;
    
    /**
     * HNSW 参数 - efConstruction
     */
    private int hnswEfConstruction = 200;
    
    /**
     * IVF 参数 - nlist
     */
    private int ivfNlist = 1024;
    
    /**
     * 搜索参数 - nprobe (IVF)
     */
    private int ivfNprobe = 16;
    
    /**
     * 搜索参数 - ef (HNSW)
     */
    private int hnswEf = 64;
}
