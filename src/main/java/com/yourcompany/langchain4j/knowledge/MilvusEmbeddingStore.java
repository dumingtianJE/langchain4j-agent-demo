package com.yourcompany.langchain4j.knowledge;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import io.milvus.client.MilvusServiceClient;
import io.milvus.grpc.DataType;
import io.milvus.grpc.DescribeCollectionResponse;
import io.milvus.grpc.MutationResult;
import io.milvus.grpc.SearchResults;
import io.milvus.param.ConnectParam;
import io.milvus.param.IndexType;
import io.milvus.param.MetricType;
import io.milvus.param.R;
import io.milvus.param.RpcStatus;
import io.milvus.param.collection.*;
import io.milvus.param.dml.DeleteParam;
import io.milvus.param.dml.InsertParam;
import io.milvus.param.dml.SearchParam;
import io.milvus.param.index.CreateIndexParam;
import io.milvus.response.QueryResultsWrapper;
import io.milvus.response.SearchResultsWrapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Milvus 向量存储实现
 * 支持分布式、高性能的向量检索
 * 仅在 ai.milvus.enabled=true 时激活
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "ai.milvus.enabled", havingValue = "true")
public class MilvusEmbeddingStore implements EmbeddingStore<TextSegment> {

    private final MilvusServiceClient milvusClient;
    private final com.yourcompany.langchain4j.config.MilvusConfig config;
    private final ObjectMapper objectMapper = new ObjectMapper();

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

    @PreDestroy
    public void destroy() {
        if (milvusClient != null) {
            milvusClient.close();
            log.info("Milvus 客户端已关闭");
        }
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
                // Milvus 2.3.0 不支持 COSINE，归一化向量下 IP 等价于余弦相似度
                metricType = MetricType.IP;
        }

        // 构建索引额外参数（JSON 格式）
        String extraParamsJson = buildIndexExtraParamsJson(indexType);

        CreateIndexParam.Builder builder = CreateIndexParam.newBuilder()
            .withCollectionName(config.getCollectionName())
            .withFieldName("embedding")
            .withIndexType(indexType)
            .withMetricType(metricType)
            .withSyncMode(Boolean.TRUE);

        if (!extraParamsJson.isEmpty()) {
            builder.withExtraParam(extraParamsJson);
        }

        CreateIndexParam indexParam = builder.build();

        R<RpcStatus> response = milvusClient.createIndex(indexParam);

        if (response.getStatus() == R.Status.Success.getCode()) {
            log.info("Milvus 索引创建成功: {}", config.getIndexType());
        } else {
            log.error("Milvus 索引创建失败: {}", response.getMessage());
        }
    }

    /**
     * 构建索引额外参数 JSON
     */
    private String buildIndexExtraParamsJson(IndexType indexType) {
        Map<String, Object> params = new HashMap<>();
        if (indexType == IndexType.HNSW) {
            params.put("M", config.getHnswM());
            params.put("efConstruction", config.getHnswEfConstruction());
        } else if (indexType == IndexType.IVF_FLAT || indexType == IndexType.IVF_PQ) {
            params.put("nlist", config.getIvfNlist());
        }
        if (params.isEmpty()) return "";
        try {
            return objectMapper.writeValueAsString(params);
        } catch (Exception e) {
            log.warn("序列化索引参数失败", e);
            return "";
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
        String metadata = "{}";
        if (embedded.metadata() != null) {
            try {
                metadata = objectMapper.writeValueAsString(embedded.metadata().toMap());
            } catch (Exception e) {
                log.warn("序列化元数据失败: {}", e.getMessage());
            }
        }

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

    /**
     * 向量相似度搜索 - 完整的 Milvus Search API 实现
     */
    @Override
    public EmbeddingSearchResult<TextSegment> search(EmbeddingSearchRequest request) {
        List<Float> queryVector = request.queryEmbedding().vectorAsList();
        int topK = request.maxResults();

        // 构建搜索参数（JSON 格式）
        String searchParamsJson = buildSearchParamsJson();

        List<String> outputFields = Arrays.asList("text", "metadata");

        MetricType searchMetricType = resolveMetricType(config.getMetricType());

        SearchParam searchParam = SearchParam.newBuilder()
            .withCollectionName(config.getCollectionName())
            .withMetricType(searchMetricType)
            .withOutFields(outputFields)
            .withTopK(topK)
            .withVectors(Collections.singletonList(queryVector))
            .withVectorFieldName("embedding")
            .withParams(searchParamsJson)
            .build();

        try {
            R<SearchResults> searchResult = milvusClient.search(searchParam);

            if (searchResult.getStatus() != R.Status.Success.getCode()) {
                log.error("Milvus 搜索失败: {}", searchResult.getMessage());
                return new EmbeddingSearchResult<>(Collections.emptyList());
            }

            SearchResultsWrapper wrapper = new SearchResultsWrapper(searchResult.getData().getResults());
            List<EmbeddingMatch<TextSegment>> matches = new ArrayList<>();

            List<QueryResultsWrapper.RowRecord> rows = wrapper.getRowRecords();
            List<SearchResultsWrapper.IDScore> scores = wrapper.getIDScore(0);

            for (int i = 0; i < rows.size(); i++) {
                SearchResultsWrapper.IDScore idScore = scores.get(i);
                QueryResultsWrapper.RowRecord row = rows.get(i);

                // 获取文本
                String text = row.get("text") != null ? row.get("text").toString() : null;

                // 解析元数据并构建 TextSegment
                TextSegment segment = null;
                if (text != null && !text.isEmpty()) {
                    Object metadataObj = row.get("metadata");
                    String metadataJson = metadataObj != null ? metadataObj.toString() : null;
                    dev.langchain4j.data.document.Metadata metadata = parseMetadata(metadataJson);
                    segment = TextSegment.from(text, metadata);
                }

                String resultId = idScore.getStrID() != null
                    ? idScore.getStrID() : String.valueOf(idScore.getLongID());
                EmbeddingMatch<TextSegment> match = new EmbeddingMatch<>(
                    (double) idScore.getScore(),
                    resultId,
                    null,
                    segment
                );
                matches.add(match);
            }

            // 按 minScore 过滤
            double minScore = request.minScore();
            matches = matches.stream()
                .filter(m -> m.score() >= minScore)
                .collect(Collectors.toList());

            log.debug("Milvus 搜索完成，返回 {} 个结果（minScore={}）", matches.size(), minScore);
            return new EmbeddingSearchResult<>(matches);

        } catch (Exception e) {
            log.error("Milvus 搜索异常", e);
            return new EmbeddingSearchResult<>(Collections.emptyList());
        }
    }

    /**
     * 解析配置中的 MetricType 字符串为枚举值
     */
    private MetricType resolveMetricType(String metricTypeStr) {
        switch (metricTypeStr) {
            case "L2": return MetricType.L2;
            case "IP": return MetricType.IP;
            default: return MetricType.IP; // COSINE 在 2.3.0 中不可用，用 IP 替代
        }
    }

    /**
     * 构建搜索参数 JSON（根据索引类型）
     */
    private String buildSearchParamsJson() {
        try {
            Map<String, Object> params = new HashMap<>();
            String indexType = config.getIndexType();
            if ("HNSW".equals(indexType)) {
                params.put("ef", config.getHnswEf());
            } else if ("IVF_FLAT".equals(indexType) || "IVF_PQ".equals(indexType)) {
                params.put("nprobe", config.getIvfNprobe());
            }
            return objectMapper.writeValueAsString(params);
        } catch (Exception e) {
            log.warn("构建搜索参数失败: {}", e.getMessage());
            return "{}";
        }
    }

    /**
     * 解析元数据 JSON 为 LangChain4j Metadata 对象
     */
    private dev.langchain4j.data.document.Metadata parseMetadata(String metadataJson) {
        if (metadataJson == null || metadataJson.isBlank() || "{}".equals(metadataJson)) {
            return new dev.langchain4j.data.document.Metadata();
        }
        try {
            Map<String, Object> metadataMap = objectMapper.readValue(
                metadataJson, new TypeReference<Map<String, Object>>() {});
            return new dev.langchain4j.data.document.Metadata(metadataMap);
        } catch (Exception e) {
            log.warn("解析元数据失败: {}", e.getMessage());
            return new dev.langchain4j.data.document.Metadata();
        }
    }

    @Override
    public void remove(String id) {
        milvusClient.delete(
            DeleteParam.newBuilder()
                .withCollectionName(config.getCollectionName())
                .withExpr(String.format("id == '%s'", id))
                .build()
        );
    }

    @Override
    public void removeAll(Collection<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        String expr = ids.stream()
            .map(id -> String.format("id == '%s'", id))
            .collect(Collectors.joining(" || "));

        R<MutationResult> response = milvusClient.delete(
            DeleteParam.newBuilder()
                .withCollectionName(config.getCollectionName())
                .withExpr(expr)
                .build()
        );

        if (response.getStatus() == R.Status.Success.getCode()) {
            log.info("Milvus 批量删除成功，共 {} 条", ids.size());
        } else {
            log.error("Milvus 批量删除失败: {}", response.getMessage());
        }
    }

    @Override
    public void removeAll() {
        // 先释放集合，再重新创建以清空所有数据
        milvusClient.dropCollection(
            DropCollectionParam.newBuilder()
                .withCollectionName(config.getCollectionName())
                .build()
        );
        createCollectionIfNotExist();
        createIndex();
        loadCollection();
        log.info("Milvus 集合已清空并重建: {}", config.getCollectionName());
    }
}
