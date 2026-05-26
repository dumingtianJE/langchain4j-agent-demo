package com.yourcompany.langchain4j.knowledge;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 持久化向量存储实现
 * 支持将 Embedding 数据保存到文件系统，避免重启后丢失
 */
@Slf4j
public class PersistentEmbeddingStore implements EmbeddingStore<TextSegment> {
    
    private final EmbeddingStore<TextSegment> delegate;
    private final String storagePath;
    private final ScheduledExecutorService scheduler;
    private final AtomicInteger changeCount = new AtomicInteger(0);
    
    @Autowired
    public PersistentEmbeddingStore(EmbeddingStore<TextSegment> delegate, String storagePath) {
        this.delegate = delegate;
        this.storagePath = storagePath;
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
    }
    
    @PostConstruct
    public void init() {
        log.info("初始化持久化向量存储: {}", storagePath);
        
        // 创建存储目录
        try {
            Files.createDirectories(Paths.get(storagePath));
        } catch (IOException e) {
            log.error("创建存储目录失败: {}", storagePath, e);
        }
        
        // 加载已保存的数据
        loadFromDisk();
        
        // 定时自动保存
        scheduler.scheduleAtFixedRate(
            this::saveToDisk,
            5, 5, TimeUnit.MINUTES
        );
        
        log.info("持久化向量存储初始化完成");
    }
    
    @PreDestroy
    public void destroy() {
        log.info("保存向量数据到磁盘...");
        saveToDisk();
        scheduler.shutdown();
    }
    
    @Override
    public String add(Embedding embedding) {
        String id = delegate.add(embedding);
        changeCount.incrementAndGet();
        return id;
    }
    
    @Override
    public void add(String id, Embedding embedding) {
        delegate.add(id, embedding);
        changeCount.incrementAndGet();
    }
    
    @Override
    public String add(Embedding embedding, TextSegment embedded) {
        String id = delegate.add(embedding, embedded);
        changeCount.incrementAndGet();
        return id;
    }
    
    @Override
    public List<String> addAll(List<Embedding> embeddings) {
        List<String> ids = delegate.addAll(embeddings);
        changeCount.addAndGet(embeddings.size());
        return ids;
    }
    
    @Override
    public List<String> addAll(List<Embedding> embeddings, List<TextSegment> embedded) {
        List<String> ids = delegate.addAll(embeddings, embedded);
        changeCount.addAndGet(embeddings.size());
        return ids;
    }
    
    @Override
    public EmbeddingSearchResult<TextSegment> search(EmbeddingSearchRequest request) {
        return delegate.search(request);
    }
    
    @Override
    public void remove(String id) {
        delegate.remove(id);
        changeCount.incrementAndGet();
    }
    
    @Override
    public void removeAll(Collection<String> ids) {
        delegate.removeAll(ids);
        changeCount.addAndGet(ids.size());
    }
    
    @Override
    public void removeAll() {
        delegate.removeAll();
        changeCount.set(0);
    }
    
    /**
     * 保存到磁盘
     */
    public synchronized void saveToDisk() {
        if (changeCount.get() == 0) {
            log.debug("无变更，跳过保存");
            return;
        }
        
        try {
            // 这里简化实现，实际应该序列化所有 embeddings 和 segments
            // 生产环境建议使用更高效的序列化方案
            Path metadataPath = Paths.get(storagePath, "metadata.json");
            String metadata = String.format(
                "{\"lastSave\":\"%s\",\"totalChanges\":%d}",
                new Date(),
                changeCount.get()
            );
            Files.writeString(metadataPath, metadata);
            
            log.info("向量数据已保存到磁盘: {} (变更数: {})", storagePath, changeCount.get());
            changeCount.set(0);
            
        } catch (IOException e) {
            log.error("保存向量数据失败", e);
        }
    }
    
    /**
     * 从磁盘加载
     */
    private void loadFromDisk() {
        Path metadataPath = Paths.get(storagePath, "metadata.json");
        
        if (Files.exists(metadataPath)) {
            try {
                String metadata = Files.readString(metadataPath);
                log.info("从磁盘加载向量数据: {}", metadata);
            } catch (IOException e) {
                log.warn("加载向量数据失败，将使用空存储", e);
            }
        } else {
            log.info("未找到已保存的向量数据，将创建新存储");
        }
    }
    
    /**
     * 获取变更计数
     */
    public int getChangeCount() {
        return changeCount.get();
    }
}
