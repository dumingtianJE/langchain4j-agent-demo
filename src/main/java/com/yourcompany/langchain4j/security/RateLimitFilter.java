package com.yourcompany.langchain4j.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * API 限流过滤器
 * 使用 Bucket4j 实现令牌桶算法
 */
@Slf4j
@Component
public class RateLimitFilter extends OncePerRequestFilter {
    
    // 每个 IP 地址的限流桶
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    FilterChain filterChain) throws ServletException, IOException {
        String clientIP = getClientIP(request);
        
        Bucket bucket = buckets.computeIfAbsent(clientIP, this::createNewBucket);
        
        if (bucket.tryConsume(1)) {
            // 允许请求
            filterChain.doFilter(request, response);
        } else {
            // 拒绝请求 - 429 Too Many Requests
            response.setStatus(429);
            response.setContentType("application/json");
            response.getWriter().write(
                "{\"error\":\"请求频率过高，请稍后重试\",\"limit\":\"100 requests per minute\"}"
            );
            log.warn("IP {} 触发限流", clientIP);
        }
    }
    
    /**
     * 创建新的限流桶
     * 默认：100 请求/分钟
     */
    private Bucket createNewBucket(String key) {
        Bandwidth limit = Bandwidth.classic(
            100,  // 初始容量
            Refill.greedy(100, Duration.ofMinutes(1))  // 每分钟补充 100 个令牌
        );
        
        log.info("为 IP {} 创建限流桶: 100 requests/minute", key);
        
        return Bucket4j.builder()
            .addLimit(limit)
            .build();
    }
    
    /**
     * 获取客户端真实 IP
     */
    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isEmpty()) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0].trim();
    }
    
    /**
     * 清理过期桶（可选，防止内存泄漏）
     */
    public void cleanup() {
        // 实际生产环境应该使用定时任务清理
        buckets.clear();
    }
}
