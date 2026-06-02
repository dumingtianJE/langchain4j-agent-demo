package com.yourcompany.langchain4j.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * API 限流过滤器
 * 基于 Bucket4j 令牌桶算法，按 IP 地址限制请求速率
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "ai.ratelimit.enabled", havingValue = "true")
public class RateLimitFilter extends OncePerRequestFilter {
    
    @Value("${ai.ratelimit.requests-per-minute:100}")
    private int requestsPerMinute;
    
    @Value("${ai.ratelimit.burst-capacity:150}")
    private int burstCapacity;
    
    // IP 地址到限流桶的映射
    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    FilterChain filterChain) throws ServletException, IOException {
        String clientIp = getClientIp(request);
        Bucket bucket = getOrCreateBucket(clientIp);
        
        if (bucket.tryConsume(1)) {
            // 允许请求通过
            filterChain.doFilter(request, response);
        } else {
            // 超出限流阈值，拒绝请求
            log.warn("API 限流触发: IP={}, 限制={}次/分钟", clientIp, requestsPerMinute);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(String.format(
                "{\"success\":false,\"error\":\"请求过于频繁，请稍后再试（限制: %d次/分钟）\"}",
                requestsPerMinute
            ));
        }
    }
    
    /**
     * 获取或创建指定 IP 的限流桶
     */
    private Bucket getOrCreateBucket(String ip) {
        return buckets.computeIfAbsent(ip, k -> {
            Bandwidth limit = Bandwidth.classic(
                burstCapacity,
                Refill.greedy(requestsPerMinute, Duration.ofMinutes(1))
            );
            return Bucket.builder().addLimit(limit).build();
        });
    }
    
    /**
     * 获取客户端真实 IP（支持反向代理）
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 取第一个 IP（多级代理时）
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
