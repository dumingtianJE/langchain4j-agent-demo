package com.yourcompany.langchain4j.controller;

import com.yourcompany.langchain4j.agent.AiProgrammingAgent;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 统一 AI 聊天控制器
 * 为前端提供简洁的 AI 对话接口
 */
@Slf4j
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiChatController {
    
    private final AiProgrammingAgent aiProgrammingAgent;
    
    /**
     * 统一 AI 对话接口
     * 支持编程问答、代码生成、技术咨询等
     */
    @PostMapping("/chat")
    public ResponseEntity<Map<String, Object>> chat(@RequestBody ChatRequest request) {
        log.info("收到 AI 对话请求: {}", request.getMessage());
        
        try {
            // 使用 AI 编程 Agent 回答
            String response;
            
            if (request.getCodeContext() != null && !request.getCodeContext().isEmpty()) {
                // 如果有代码上下文，进行代码相关的回答
                response = aiProgrammingAgent.answerTechnicalQuestion(
                    request.getMessage() + "\n\n相关代码:\n" + request.getCodeContext()
                );
            } else {
                // 普通技术问题
                response = aiProgrammingAgent.answerTechnicalQuestion(request.getMessage());
            }
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "reply", response,
                "timestamp", System.currentTimeMillis()
            ));
            
        } catch (Exception e) {
            log.error("AI 对话处理失败", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "reply", "❌ 抱歉，处理您的请求时出现错误：" + e.getMessage(),
                "timestamp", System.currentTimeMillis()
            ));
        }
    }
    
    /**
     * 健康检查
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
            "success", true,
            "status", "AI Chat Service is running",
            "timestamp", System.currentTimeMillis()
        ));
    }
    
    /**
     * 聊天请求 DTO
     */
    @Data
    public static class ChatRequest {
        private String message;        // 用户消息
        private String codeContext;    // 可选：代码上下文
        private String userId;         // 可选：用户ID
    }
}
