package com.yourcompany.langchain4j.security;

// import io.github.bucket4j.Bandwidth;  // 暂时禁用，Bucket4j API 变更
// import io.github.bucket4j.Bucket;
// import io.github.bucket4j.Bucket4j;
// import io.github.bucket4j.Refill;
// import io.github.bucket4j.Configuration;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * API 限流过滤器
 * 注意：暂时禁用，Bucket4j 8.7.0 API 已变更，需要升级到最新版本
 */
@Slf4j
@Component
public class RateLimitFilter extends OncePerRequestFilter {
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    FilterChain filterChain) throws ServletException, IOException {
        // 暂时不实施限流，直接放行
        filterChain.doFilter(request, response);
    }
}
