package com.yourcompany.langchain4j.monitor;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Prometheus 指标收集器
 * 监控 AI Agent 系统的关键指标
 */
@Slf4j
@Component
public class PrometheusMetricsCollector {
    
    private final MeterRegistry meterRegistry;
    
    // 计数器
    private Counter totalRequestsCounter;
    private Counter successfulRequestsCounter;
    private Counter failedRequestsCounter;
    private Counter tokensUsedCounter;
    private Counter knowledgeSearchCounter;
    private Counter alertTriggeredCounter;
    
    // 计时器
    private Timer requestTimer;
    private Timer knowledgeSearchTimer;
    
    // 仪表盘
    private AtomicInteger concurrentRequestsGauge;
    private AtomicLong hourlyTokenUsageGauge;
    private AtomicLong dailyTokenUsageGauge;
    private AtomicInteger knowledgeBaseSizeGauge;
    private AtomicInteger skillCountGauge;
    private AtomicInteger learningExperienceCountGauge;
    
    public PrometheusMetricsCollector(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }
    
    @PostConstruct
    public void init() {
        log.info("初始化 Prometheus 指标收集器");
        
        // 注册计数器
        totalRequestsCounter = Counter.builder("ai_requests_total")
            .description("Total number of AI requests")
            .tag("system", "ai-programming-agent")
            .register(meterRegistry);
        
        successfulRequestsCounter = Counter.builder("ai_requests_successful_total")
            .description("Total number of successful AI requests")
            .tag("system", "ai-programming-agent")
            .register(meterRegistry);
        
        failedRequestsCounter = Counter.builder("ai_requests_failed_total")
            .description("Total number of failed AI requests")
            .tag("system", "ai-programming-agent")
            .register(meterRegistry);
        
        tokensUsedCounter = Counter.builder("ai_tokens_used_total")
            .description("Total number of tokens used")
            .tag("system", "ai-programming-agent")
            .register(meterRegistry);
        
        knowledgeSearchCounter = Counter.builder("ai_knowledge_search_total")
            .description("Total number of knowledge searches")
            .tag("system", "ai-programming-agent")
            .register(meterRegistry);
        
        alertTriggeredCounter = Counter.builder("ai_alerts_triggered_total")
            .description("Total number of alerts triggered")
            .tag("system", "ai-programming-agent")
            .register(meterRegistry);
        
        // 注册计时器
        requestTimer = Timer.builder("ai_request_duration")
            .description("AI request duration")
            .tag("system", "ai-programming-agent")
            .register(meterRegistry);
        
        knowledgeSearchTimer = Timer.builder("ai_knowledge_search_duration")
            .description("Knowledge search duration")
            .tag("system", "ai-programming-agent")
            .register(meterRegistry);
        
        // 注册仪表盘
        concurrentRequestsGauge = new AtomicInteger(0);
        Gauge.builder("ai_concurrent_requests", concurrentRequestsGauge, AtomicInteger::get)
            .description("Current number of concurrent requests")
            .tag("system", "ai-programming-agent")
            .register(meterRegistry);
        
        hourlyTokenUsageGauge = new AtomicLong(0);
        Gauge.builder("ai_hourly_token_usage", hourlyTokenUsageGauge, AtomicLong::get)
            .description("Current hourly token usage")
            .tag("system", "ai-programming-agent")
            .register(meterRegistry);
        
        dailyTokenUsageGauge = new AtomicLong(0);
        Gauge.builder("ai_daily_token_usage", dailyTokenUsageGauge, AtomicLong::get)
            .description("Current daily token usage")
            .tag("system", "ai-programming-agent")
            .register(meterRegistry);
        
        knowledgeBaseSizeGauge = new AtomicInteger(0);
        Gauge.builder("ai_knowledge_base_size", knowledgeBaseSizeGauge, AtomicInteger::get)
            .description("Number of documents in knowledge base")
            .tag("system", "ai-programming-agent")
            .register(meterRegistry);
        
        skillCountGauge = new AtomicInteger(0);
        Gauge.builder("ai_skill_count", skillCountGauge, AtomicInteger::get)
            .description("Number of registered skills")
            .tag("system", "ai-programming-agent")
            .register(meterRegistry);
        
        learningExperienceCountGauge = new AtomicInteger(0);
        Gauge.builder("ai_learning_experiences", learningExperienceCountGauge, AtomicInteger::get)
            .description("Number of learning experiences")
            .tag("system", "ai-programming-agent")
            .register(meterRegistry);
        
        log.info("Prometheus 指标收集器初始化完成");
    }
    
    /**
     * 记录请求
     */
    public void recordRequest() {
        totalRequestsCounter.increment();
    }
    
    /**
     * 记录成功请求
     */
    public void recordSuccessfulRequest() {
        successfulRequestsCounter.increment();
    }
    
    /**
     * 记录失败请求
     */
    public void recordFailedRequest() {
        failedRequestsCounter.increment();
    }
    
    /**
     * 记录 Token 使用
     */
    public void recordTokensUsed(long tokens) {
        tokensUsedCounter.increment(tokens);
    }
    
    /**
     * 记录请求耗时
     */
    public void recordRequestDuration(long durationMs) {
        requestTimer.record(durationMs, java.util.concurrent.TimeUnit.MILLISECONDS);
    }
    
    /**
     * 记录知识检索
     */
    public void recordKnowledgeSearch() {
        knowledgeSearchCounter.increment();
    }
    
    /**
     * 记录知识检索耗时
     */
    public void recordKnowledgeSearchDuration(long durationMs) {
        knowledgeSearchTimer.record(durationMs, java.util.concurrent.TimeUnit.MILLISECONDS);
    }
    
    /**
     * 记录警报
     */
    public void recordAlert() {
        alertTriggeredCounter.increment();
    }
    
    /**
     * 更新并发请求数
     */
    public void updateConcurrentRequests(int count) {
        concurrentRequestsGauge.set(count);
    }
    
    /**
     * 更新每小时 Token 使用
     */
    public void updateHourlyTokenUsage(long count) {
        hourlyTokenUsageGauge.set(count);
    }
    
    /**
     * 更新每日 Token 使用
     */
    public void updateDailyTokenUsage(long count) {
        dailyTokenUsageGauge.set(count);
    }
    
    /**
     * 更新知识库大小
     */
    public void updateKnowledgeBaseSize(int size) {
        knowledgeBaseSizeGauge.set(size);
    }
    
    /**
     * 更新技能数量
     */
    public void updateSkillCount(int count) {
        skillCountGauge.set(count);
    }
    
    /**
     * 更新学习经验数量
     */
    public void updateLearningExperienceCount(int count) {
        learningExperienceCountGauge.set(count);
    }
}
