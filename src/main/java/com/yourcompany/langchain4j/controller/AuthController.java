package com.yourcompany.langchain4j.controller;

import com.yourcompany.langchain4j.security.JwtUtil;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 认证控制器
 * 提供登录和 Token 管理功能
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    
    /**
     * 用户登录
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody LoginRequest request) {
        log.info("用户登录请求: {}", request.getUsername());
        
        // TODO: 实际项目中应该从数据库查询用户并验证密码
        // 这里简化处理，接受任何用户名密码
        String token = jwtUtil.generateToken(request.getUsername());
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "token", token,
            "username", request.getUsername(),
            "expiresIn", jwtUtil.getClaimFromToken(token, claims -> 
                claims.getExpiration().getTime() - System.currentTimeMillis()
            )
        ));
    }
    
    /**
     * 刷新 Token
     */
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refreshToken(@RequestBody RefreshRequest request) {
        try {
            String newToken = jwtUtil.refreshToken(request.getToken());
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "token", newToken
            ));
        } catch (Exception e) {
            log.error("Token 刷新失败", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", "Token 刷新失败: " + e.getMessage()
            ));
        }
    }
    
    /**
     * 验证 Token
     */
    @PostMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateToken(@RequestBody ValidateRequest request) {
        try {
            String username = jwtUtil.getUsernameFromToken(request.getToken());
            boolean isValid = jwtUtil.validateToken(request.getToken(), username);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "valid", isValid,
                "username", username
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                "success", true,
                "valid", false,
                "error", e.getMessage()
            ));
        }
    }
    
    @Data
    public static class LoginRequest {
        private String username;
        private String password;
    }
    
    @Data
    public static class RefreshRequest {
        private String token;
    }
    
    @Data
    public static class ValidateRequest {
        private String token;
    }
}
