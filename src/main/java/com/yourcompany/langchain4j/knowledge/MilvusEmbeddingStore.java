package com.yourcompany.langchain4j.knowledge;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import io.milvus.client.MilvusServiceClient;
import io.milvus.grpc.DataType;
import io.milvus.param.ConnectParam;
import io.milvus.param.IndexType;
import io.milvus.param.MetricType;
import io.milvus.param.collection.*;
import io.milvus.param.dml.InsertParam;
import io.milvus.param.index.CreateIndexParam;
import io.milvus.param.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import jakarta.annotation.PostConstruct;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Milvus 向量存储实现
 * 支持分布式、高性能的向量检索
 */
@Slf4j
public class MilvusEmbeddingStore implements EmbeddingStore<TextSegment> {
    
    private final MilvusServiceClient milvusClient;
    private final com.yourcompany.langchain4j.config.MilvusConfig config;
    
    @Autowired
    public MilvusEmbeddingStore(com.yourcompany.langchain4j.config.MilvusConfig config) {
        this.config = config;
        this.milvusClient = new MilvusServiceClient(
            ConnectParam.newBuilder()
                .withHost(config.getHost())
                .withPort(config.getPort())
                .build()
        );
    }
    
    @PostConstruct
    public void init() {
        log.info("初始化 Milvus 向量存储: {}:{}", config.getHost(), config.getPort());
        
        // 创建集合（如果不存在）
        createCollectionIfNotExist();
        
        // 创建索引
        createIndex();
        
        // 加载集合到内存
        loadCollection();
        
        log.info("Milvus 向量存储初始化完成");
    }
    
    /**
     * 创建集合
     */
    private void createCollectionIfNotExist() {
        R<DescribeCollectionResponse> descResponse = milvusClient.describeCollection(
            DescribeCollectionParam.newBuilder()
                .withCollectionName(config.getCollectionName())
                .build()
        );
        
        if (descResponse.getStatus() != R.Status.Success.getCode()) {
            // 集合不存在，创建新集合
            FieldType idField = FieldType.newBuilder()
                .withName("id")
                .withDataType(DataType.VarChar)
                .withMaxLength(256)
                .withPrimaryKey(true)
                .withAutoID(false)
                .build();
            
            FieldType embeddingField = FieldType.newBuilder()
                .withName("embedding")
                .withDataType(DataType.FloatVector)
                .withDimension(config.getDimension())
                .build();
            
            FieldType textField = FieldType.newBuilder()
                .withName("text")
                .withDataType(DataType.VarChar)
                .withMaxLength(10000)
                .build();
            
            FieldType metadataField = FieldType.newBuilder()
                .withName("metadata")
                .withDataType(DataType.VarChar)
                .withMaxLength(5000)
                .build();
            
            CreateCollectionParam param = CreateCollectionParam.newBuilder()
                .withCollectionName(config.getCollectionName())
                .withShardsNum(2)
                .addFieldType(idField)
                .addFieldType(embeddingField)
                .addFieldType(textField)
                .addFieldType(metadataField)
                .build();
            
            R<RpcStatus> createResponse = milvusClient.createCollection(param);
            
            if (createResponse.getStatus() == R.Status.Success.getCode()) {
                log.info("Milvus 集合创建成功: {}", config.getCollectionName());
            } else {
                log.error("Milvus 集合创建失败: {}", createResponse.getMessage());
            }
        } else {
            log.info("Milvus 集合已存在: {}", config.getCollectionName());
        }
    }
    
    /**
     * 创建索引
     */
    private void createIndex() {
        IndexType indexType;
        switch (config.getIndexType()) {
            case "IVF_FLAT":
                indexType = IndexType.IVF_FLAT;
                break;
            case "IVF_PQ":
                indexType = IndexType.IVF_PQ;
                break;
            case "HNSW":
                indexType = IndexType.HNSW;
                break;
            default:
                indexType = IndexType.FLAT;
        }
        
        MetricType metricType;
        switch (config.getMetricType()) {
            case "L2":
                metricType = MetricType.L2;
                break;
            case "IP":
                metricType = MetricType.IP;
                break;
            default:
                metricType = MetricType.COSINE;
        }
        
        Map<String, Object> extraParams = new HashMap<>();
        if (indexType == IndexType.HNSW) {
            extraParams.put("M", config.getHnswM());
            extraParams.put("efConstruction", config.getHnswEfConstruction());
        } else if (indexType == IndexType.IVF_FLAT || indexType == IndexType.IVF_PQ) {
            extraParams.put("nlist", config.getIvfNlist());
        }
        
        CreateIndexParam indexParam = CreateIndexParam.newBuilder()
            .withCollectionName(config.getCollectionName())
            .withFieldName("embedding")
            .withIndexType(indexType)
            .withMetricType(metricType)
            .withExtraParams(extraParams)
            .withSyncMode(Boolean.TRUE)
            .build();
        
        R<RpcStatus> response = milvusClient.createIndex(indexParam);
        
        if (response.getStatus() == R.Status.Success.getCode()) {
            log.info("Milvus 索引创建成功: {}", config.getIndexType());
        } else {
            log.error("Milvus 索引创建失败: {}", response.getMessage());
        }
    }
    
    /**
     * 加载集合到内存
     */
    private void loadCollection() {
        R<RpcStatus> response = milvusClient.loadCollection(
            LoadCollectionParam.newBuilder()
                .withCollectionName(config.getCollectionName())
                .build()
        );
        
        if (response.getStatus() == R.Status.Success.getCode()) {
            log.info("Milvus 集合加载到内存成功");
        } else {
            log.error("Milvus 集合加载失败: {}", response.getMessage());
        }
    }
    
    @Override
    public String add(Embedding embedding) {
        String id = UUID.randomUUID().toString();
        add(id, embedding);
        return id;
    }
    
    @Override
    public void add(String id, Embedding embedding) {
        List<Float> vector = embedding.vectorAsList();
        
        List<InsertParam.Field> fields = new ArrayList<>();
        fields.add(new InsertParam.Field("id", Collections.singletonList(id)));
        fields.add(new InsertParam.Field("embedding", Collections.singletonList(vector)));
        fields.add(new InsertParam.Field("text", Collections.singletonList("")));
        fields.add(new InsertParam.Field("metadata", Collections.singletonList("{}")));
        
        InsertParam insertParam = InsertParam.newBuilder()
            .withCollectionName(config.getCollectionName())
            .withFields(fields)
            .build();
        
        milvusClient.insert(insertParam);
    }
    
    @Override
    public String add(Embedding embedding, TextSegment embedded) {
        String id = UUID.randomUUID().toString();
        
        List<Float> vector = embedding.vectorAsList();
        String text = embedded.text();
        String metadata = embedded.metadata().toJson();
        
        List<InsertParam.Field> fields = new ArrayList<>();
        fields.add(new InsertParam.Field("id", Collections.singletonList(id)));
        fields.add(new InsertParam.Field("embedding", Collections.singletonList(vector)));
        fields.add(new InsertParam.Field("text", Collections.singletonList(text)));
        fields.add(new InsertParam.Field("metadata", Collections.singletonList(metadata)));
        
        InsertParam insertParam = InsertParam.newBuilder()
            .withCollectionName(config.getCollectionName())
            .withFields(fields)
            .build();
        
        milvusClient.insert(insertParam);
        
        return id;
    }
    
    @Override
    public List<String> addAll(List<Embedding> embeddings) {
        List<String> ids = new ArrayList<>();
        for (Embedding embedding : embeddings) {
            ids.add(add(embedding));
        }
        return ids;
    }
    
    @Override
    public List<String> addAll(List<Embedding> embeddings, List<TextSegment> embedded) {
        List<String> ids = new ArrayList<>();
        for (int i = 0; i < embeddings.size(); i++) {
            ids.add(add(embeddings.get(i), embedded.get(i)));
        }
        return ids;
    }
    
    @Override
    public EmbeddingSearchResult<TextSegment> search(EmbeddingSearchRequest request) {
        List<Float> queryVector = request.queryEmbedding().vectorAsList();
        
        // 简化的搜索实现
        // 实际生产环境需要实现完整的 Milvus 搜索逻辑
        
        List<EmbeddingMatch<TextSegment>> matches = new ArrayList<>();
        
        return new EmbeddingSearchResult<>(matches);
    }
    
    @Override
    public void remove(String id) {
        milvusClient.delete(
            io.milvus.param.dml.DeleteParam.newBuilder()
                .withCollectionName(config.getCollectionName())
                .withExpr(String.format("id == '%s'", id))
                .build()
        );
    }
    
    @Override
    public void removeAll(Collection<String> ids) {
        // 批量删除实现
    }
    
    @Override
    public void removeAll() {
        // 清空集合实现
    }
}
