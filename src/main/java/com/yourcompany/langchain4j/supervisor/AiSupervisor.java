package com.yourcompany.langchain4j.supervisor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * AI 监管器
 * 负责监控 Token 使用、防止异常消耗、成本控制等
 */
@Slf4j
@Service
public class AiSupervisor {
    
    @Value("${ai.supervisor.max-tokens-per-request:10000}")
    private int maxTokensPerRequest;
    
    @Value("${ai.supervisor.max-tokens-per-hour:100000}")
    private int maxTokensPerHour;
    
    @Value("${ai.supervisor.max-tokens-per-day:1000000}")
    private int maxTokensPerDay;
    
    @Value("${ai.supervisor.max-concurrent-requests:10}")
    private int maxConcurrentRequests;
    
    @Value("${ai.supervisor.alert-threshold:0.8}")
    private double alertThreshold;
    
    // Token 使用记录存储
    private final Map<String, TokenUsageRecord> usageRecords = new ConcurrentHashMap<>();
    
    // 每小时 Token 使用统计
    private final Map<String, AtomicInteger> hourlyTokenUsage = new ConcurrentHashMap<>();
    
    // 每天 Token 使用统计
    private final Map<String, AtomicInteger> dailyTokenUsage = new ConcurrentHashMap<>();
    
    // 当前并发请求数
    private final AtomicInteger concurrentRequests = new AtomicInteger(0);
    
    // 警报日志
    private final List<String> alertLogs = Collections.synchronizedList(new ArrayList<>());
    
    @PostConstruct
    public void initializeSupervisor() {
        log.info("AI 监管系统初始化完成");
        log.info("配置: 单次请求上限={} tokens, 每小时上限={} tokens, 每日上限={} tokens",
            maxTokensPerRequest, maxTokensPerHour, maxTokensPerDay);
    }
    
    /**
     * 记录 Token 使用
     */
    public void recordTokenUsage(TokenUsageRecord record) {
        if (record.getId() == null) {
            record.setId(UUID.randomUUID().toString());
        }
        record.setCreatedAt(LocalDateTime.now());
        
        usageRecords.put(record.getId(), record);
        
        // 更新统计
        String hourKey = getHourKey();
        String dayKey = getDayKey();
        
        hourlyTokenUsage.computeIfAbsent(hourKey, k -> new AtomicInteger(0))
            .addAndGet(record.getTotalTokens());
        
        dailyTokenUsage.computeIfAbsent(dayKey, k -> new AtomicInteger(0))
            .addAndGet(record.getTotalTokens());
        
        // 检查是否触发警报
        checkAlerts(record);
        
        log.debug("Token 使用已记录: {} - 总计: {} tokens", 
            record.getId(), record.getTotalTokens());
    }
    
    /**
     * 检查是否超过限制
     */
    public boolean checkLimits(String userId, int requestedTokens) {
        // 检查单次请求限制
        if (requestedTokens > maxTokensPerRequest) {
            String alert = String.format("❌ 拒绝请求: 超过单次请求限制 (%d > %d)", 
                requestedTokens, maxTokensPerRequest);
            log.warn(alert);
            alertLogs.add(alert);
            return false;
        }
        
        // 检查并发请求限制
        if (concurrentRequests.get() >= maxConcurrentRequests) {
            String alert = String.format("❌ 拒绝请求: 超过并发请求限制 (%d >= %d)", 
                concurrentRequests.get(), maxConcurrentRequests);
            log.warn(alert);
            alertLogs.add(alert);
            return false;
        }
        
        // 检查每小时限制
        String hourKey = getHourKey();
        int currentHourlyUsage = hourlyTokenUsage.getOrDefault(hourKey, new AtomicInteger(0)).get();
        if (currentHourlyUsage + requestedTokens > maxTokensPerHour) {
            String alert = String.format("❌ 拒绝请求: 超过每小时限制 (%d + %d > %d)", 
                currentHourlyUsage, requestedTokens, maxTokensPerHour);
            log.warn(alert);
            alertLogs.add(alert);
            return false;
        }
        
        // 检查每日限制
        String dayKey = getDayKey();
        int currentDailyUsage = dailyTokenUsage.getOrDefault(dayKey, new AtomicInteger(0)).get();
        if (currentDailyUsage + requestedTokens > maxTokensPerDay) {
            String alert = String.format("❌ 拒绝请求: 超过每日限制 (%d + %d > %d)", 
                currentDailyUsage, requestedTokens, maxTokensPerDay);
            log.warn(alert);
            alertLogs.add(alert);
            return false;
        }
        
        return true;
    }
    
    /**
     * 增加并发请求计数
     */
    public void incrementConcurrentRequests() {
        concurrentRequests.incrementAndGet();
    }
    
    /**
     * 减少并发请求计数
     */
    public void decrementConcurrentRequests() {
        concurrentRequests.decrementAndGet();
    }
    
    /**
     * 检查并触发警报
     */
    private void checkAlerts(TokenUsageRecord record) {
        String hourKey = getHourKey();
        String dayKey = getDayKey();
        
        int currentHourlyUsage = hourlyTokenUsage.getOrDefault(hourKey, new AtomicInteger(0)).get();
        int currentDailyUsage = dailyTokenUsage.getOrDefault(dayKey, new AtomicInteger(0)).get();
        
        // 检查每小时使用率
        double hourlyUsageRate = (double) currentHourlyUsage / maxTokensPerHour;
        if (hourlyUsageRate >= alertThreshold) {
            String alert = String.format("⚠️ 警报: 每小时 Token 使用率达到 %.1f%% (%d/%d)",
                hourlyUsageRate * 100, currentHourlyUsage, maxTokensPerHour);
            log.warn(alert);
            alertLogs.add(alert);
            record.setAlertTriggered(true);
            record.setAlertReason("每小时使用率过高");
        }
        
        // 检查每日使用率
        double dailyUsageRate = (double) currentDailyUsage / maxTokensPerDay;
        if (dailyUsageRate >= alertThreshold) {
            String alert = String.format("⚠️ 警报: 每日 Token 使用率达到 %.1f%% (%d/%d)",
                dailyUsageRate * 100, currentDailyUsage, maxTokensPerDay);
            log.warn(alert);
            alertLogs.add(alert);
            record.setAlertTriggered(true);
            record.setAlertReason("每日使用率过高");
        }
        
        // 检查单次请求 token 数异常
        if (record.getTotalTokens() > maxTokensPerRequest * 0.8) {
            String alert = String.format("⚠️ 警报: 单次请求 Token 数异常高 (%d)",
                record.getTotalTokens());
            log.warn(alert);
            alertLogs.add(alert);
            record.setAlertTriggered(true);
            if (record.getAlertReason() == null) {
                record.setAlertReason("单次请求 token 数过高");
            }
        }
    }
    
    /**
     * 获取 Token 使用统计
     */
    public Map<String, Object> getTokenUsageStats() {
        Map<String, Object> stats = new HashMap<>();
        
        // 总记录数
        stats.put("totalRecords", usageRecords.size());
        
        // 总 Token 使用
        int totalTokens = usageRecords.values().stream()
            .mapToInt(TokenUsageRecord::getTotalTokens)
            .sum();
        stats.put("totalTokensUsed", totalTokens);
        
        // 当前小时使用
        String hourKey = getHourKey();
        stats.put("currentHourUsage", 
            hourlyTokenUsage.getOrDefault(hourKey, new AtomicInteger(0)).get());
        stats.put("hourlyLimit", maxTokensPerHour);
        
        // 当前日使用
        String dayKey = getDayKey();
        stats.put("currentDayUsage", 
            dailyTokenUsage.getOrDefault(dayKey, new AtomicInteger(0)).get());
        stats.put("dailyLimit", maxTokensPerDay);
        
        // 平均每次请求 Token 数
        if (!usageRecords.isEmpty()) {
            stats.put("averageTokensPerRequest", 
                totalTokens / usageRecords.size());
        }
        
        // 并发请求数
        stats.put("currentConcurrentRequests", concurrentRequests.get());
        stats.put("maxConcurrentRequests", maxConcurrentRequests);
        
        // 警报数量
        long alertCount = usageRecords.values().stream()
            .filter(r -> Boolean.TRUE.equals(r.getAlertTriggered()))
            .count();
        stats.put("alertCount", alertCount);
        
        return stats;
    }
    
    /**
     * 获取最近的 Token 使用记录
     */
    public List<TokenUsageRecord> getRecentUsageRecords(int limit) {
        return usageRecords.values().stream()
            .sorted(Comparator.comparing(TokenUsageRecord::getCreatedAt).reversed())
            .limit(limit)
            .collect(Collectors.toList());
    }
    
    /**
     * 获取警报日志
     */
    public List<String> getAlertLogs(int limit) {
        List<String> recentAlerts = alertLogs.stream()
            .sorted(Comparator.reverseOrder())
            .limit(limit)
            .collect(Collectors.toList());
        return recentAlerts;
    }
    
    /**
     * 生成监管报告
     */
    public String generateSupervisionReport() {
        Map<String, Object> stats = getTokenUsageStats();
        
        StringBuilder report = new StringBuilder();
        report.append("=== AI 监管报告 ===\n\n");
        report.append(String.format("总请求数: %s\n", stats.get("totalRecords")));
        report.append(String.format("总 Token 使用: %s\n", stats.get("totalTokensUsed")));
        report.append(String.format("当前小时使用: %s / %s (%.1f%%)\n", 
            stats.get("currentHourUsage"), 
            stats.get("hourlyLimit"),
            (int)stats.get("currentHourUsage") * 100.0 / (int)stats.get("hourlyLimit")));
        report.append(String.format("当前日使用: %s / %s (%.1f%%)\n", 
            stats.get("currentDayUsage"), 
            stats.get("dailyLimit"),
            (int)stats.get("currentDayUsage") * 100.0 / (int)stats.get("dailyLimit")));
        report.append(String.format("平均每次请求: %s tokens\n", stats.get("averageTokensPerRequest")));
        report.append(String.format("当前并发请求: %s / %s\n", 
            stats.get("currentConcurrentRequests"), 
            stats.get("maxConcurrentRequests")));
        report.append(String.format("警报数量: %s\n\n", stats.get("alertCount")));
        
        if (!alertLogs.isEmpty()) {
            report.append("最近警报:\n");
            alertLogs.stream()
                .skip(Math.max(0, alertLogs.size() - 10))
                .forEach(alert -> report.append("- ").append(alert).append("\n"));
        }
        
        return report.toString();
    }
    
    private String getHourKey() {
        return "hour_" + LocalDateTime.now().getHour() + "_" + 
               LocalDateTime.now().toLocalDate();
    }
    
    private String getDayKey() {
        return "day_" + LocalDateTime.now().toLocalDate();
    }
    
    // ================================================================
    // Token 估算工具（中文感知）
    // ================================================================
    
    /**
     * 估算文本的 Token 数量（中文感知版）
     * 
     * 传统估算使用 length/4 对英文有效，但中文每个字符约 1-2 tokens。
     * 本方法按字符 Unicode 范围分类统计，提高混合语言的估算精度：
     * - ASCII / 拉丁字符：约 4 字符/token
     * - CJK（中日韩）字符：约 1.5 字符/token
     * - 代码特殊字符（{} () [] = + - 等）：约 2 字符/token
     * 
     * @param text 待估算文本
     * @return 预估 Token 数量
     */
    public static int estimateTokens(String text) {
        if (text == null || text.isEmpty()) return 0;
        
        int asciiChars = 0;
        int cjkChars = 0;
        int otherChars = 0;
        
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c < 0x80) {
                asciiChars++;
            } else if (Character.isIdeographic(c) || 
                       (c >= 0x3400 && c <= 0x9FFF) ||  // CJK Unified
                       (c >= 0xF900 && c <= 0xFAFF) ||  // CJK Compatibility
                       (c >= 0x3040 && c <= 0x30FF) ||  // Hiragana/Katakana
                       (c >= 0xAC00 && c <= 0xD7AF)) {  // Korean Hangul
                cjkChars++;
            } else {
                otherChars++;
            }
        }
        
        // 各类字符的 token 估算系数
        double tokens = (asciiChars / 4.0)    // ASCII: ~4 chars/token
                      + (cjkChars / 1.5)      // CJK: ~1.5 chars/token
                      + (otherChars / 2.5);   // 其他: ~2.5 chars/token
        
        return Math.max(1, (int) Math.ceil(tokens));
    }
}
