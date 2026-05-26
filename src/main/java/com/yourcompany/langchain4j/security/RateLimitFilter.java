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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * API 限流过滤器
 * 使用 Bucket4j 实现令牌桶算法进行限流
 */
@Slf4j
@Component
public class RateLimitFilter extends OncePerRequestFilter {
    
    @Value("${ai.ratelimit.requests-per-minute:100}")
    private int requestsPerMinute;
    
    @Value("${ai.ratelimit.burst-capacity:150}")
    private int burstCapacity;
    
    @Value("${ai.ratelimit.enabled:true}")
    private boolean enabled;
    
    // IP 地址到限流桶的映射
    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!enabled) {
            // 限流未启用，直接放行
            filterChain.doFilter(request, response);
            return;
        }
        
        String clientIp = getClientIp(request);
        Bucket bucket = buckets.computeIfAbsent(clientIp, this::createNewBucket);
        
        if (bucket.tryConsume(1)) {
            // 有令牌，允许请求
            filterChain.doFilter(request, response);
        } else {
            // 没有令牌，拒绝请求
            log.warn("API 限流：IP {} 超过请求限制", clientIp);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Too many requests. Please try again later.\"}");
        }
    }
    
    /**
     * 创建新的限流桶
     */
    private Bucket createNewBucket(String ip) {
        // 使用 Bucket4j 8.x 的新 API
        Bandwidth limit = Bandwidth.classic(
            burstCapacity,
            Refill.greedy(requestsPerMinute, Duration.ofMinutes(1))
        );
        
        return Bucket4j.builder()
            .addLimit(limit)
            .build();
    }
    
    /**
     * 获取客户端真实 IP
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}
