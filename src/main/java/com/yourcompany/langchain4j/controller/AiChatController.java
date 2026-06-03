package com.yourcompany.langchain4j.controller;

import com.yourcompany.langchain4j.agent.AiProgrammingAgent;
import com.yourcompany.langchain4j.security.PromptInjectionGuard;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 统一 AI 聊天控制器
 * 提供同步 AI 对话接口和 SSE 流式输出接口
 */
@Slf4j
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiChatController {
    
    private final AiProgrammingAgent aiProgrammingAgent;
    private final StreamingChatModel streamingChatModel;
    private final PromptInjectionGuard promptInjectionGuard;
    
    // 异步执行线程池（用于流式响应）
    private final ExecutorService sseExecutor = Executors.newCachedThreadPool();
    
    /**
     * 统一 AI 对话接口（同步）
     */
    @PostMapping("/chat")
    public ResponseEntity<Map<String, Object>> chat(@RequestBody ChatRequest request) {
        log.info("收到 AI 对话请求: {}", request.getMessage());
        
        // Prompt 注入防护
        String sanitized = promptInjectionGuard.sanitizeInput(request.getMessage());
        if (sanitized == null) {
            log.warn("拒绝请求: 检测到 Prompt 注入攻击");
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "reply", "❗ 检测到异常输入，请求被拒绝。",
                "timestamp", System.currentTimeMillis()
            ));
        }
        request.setMessage(sanitized);
        
        try {
            String response;
            
            if (request.getCodeContext() != null && !request.getCodeContext().isEmpty()) {
                response = aiProgrammingAgent.answerTechnicalQuestion(
                    request.getMessage() + "\n\n相关代码:\n" + request.getCodeContext()
                );
            } else {
                response = aiProgrammingAgent.answerTechnicalQuestion(request.getMessage());
            }
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "reply", response,
                "timestamp", System.currentTimeMillis()
            ));
            
        } catch (Exception e) {
            log.error("AI 对话处理失败", e);
            
            String errorMessage = e.getMessage();
            String reply;
            
            // 检测工具调用序列错误
            if (errorMessage != null && errorMessage.contains("tool_calls")) {
                log.warn("检测到工具调用序列错误，可能是对话历史不一致导致");
                reply = "❗ 对话上下文异常，请清除对话历史后重试。\n\n" +
                        "建议操作：\n" +
                        "1. 点击右侧 AI 助手面板右上角的垃圾桶图标清除对话\n" +
                        "2. 或刷新页面后重新发送消息";
            } else if (errorMessage != null && errorMessage.contains("400")) {
                // 其他 400 错误（如 API 参数错误）
                reply = "❗ API 请求参数错误：" + errorMessage;
            } else {
                reply = "❗ 抱歉，处理您的请求时出现错误：" + errorMessage;
            }
            
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "reply", reply,
                "timestamp", System.currentTimeMillis()
            ));
        }
    }
    
    /**
     * SSE 流式 AI 对话接口
     * 支持逐 Token 推送，提升用户体验
     * 
     * 前端使用示例：
     * const eventSource = new EventSource('/api/ai/chat/stream?message=你的问题')
     * eventSource.onmessage = (event) => console.log(event.data)
     */
    @GetMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@RequestParam String message,
                                  @RequestParam(required = false) String codeContext) {
        log.info("收到 SSE 流式对话请求: {}", message);
        
        // 超时设置 5 分钟，支持复杂任务
        SseEmitter emitter = new SseEmitter(300_000L);
        
        sseExecutor.execute(() -> {
            try {
                // 发送开始信号
                emitter.send(SseEmitter.event()
                    .name("start")
                    .data("{\"status\":\"thinking\",\"message\":\"AI 正在思考中...\"}"));
                
                // 构建消息列表
                String userContent = message;
                if (codeContext != null && !codeContext.isBlank()) {
                    userContent = message + "\n\n相关代码:\n" + codeContext;
                }
                
                List<ChatMessage> messages = List.of(
                    SystemMessage.from("你是一个专业的 AI 编程助手，请简洁、准确地回答用户的技术问题。"),
                    UserMessage.from(userContent)
                );
                
                StringBuilder fullResponse = new StringBuilder();
                
                // 流式调用 LLM
                streamingChatModel.chat(messages, new StreamingChatResponseHandler() {
                    @Override
                    public void onPartialResponse(String partialResponse) {
                        try {
                            fullResponse.append(partialResponse);
                            emitter.send(SseEmitter.event()
                                .name("token")
                                .data(partialResponse));
                        } catch (Exception e) {
                            log.warn("发送 SSE Token 失败: {}", e.getMessage());
                        }
                    }
                    
                    @Override
                    public void onCompleteResponse(ChatResponse chatResponse) {
                        try {
                            AiMessage aiMessage = chatResponse.aiMessage();
                            if (aiMessage != null && fullResponse.length() == 0) {
                                emitter.send(SseEmitter.event()
                                    .name("token")
                                    .data(aiMessage.text()));
                            }
                            emitter.send(SseEmitter.event()
                                .name("done")
                                .data("{\"status\":\"complete\"}"));
                            emitter.complete();
                            log.info("SSE 流式对话完成");
                        } catch (Exception e) {
                            log.warn("发送 SSE 完成信号失败: {}", e.getMessage());
                            emitter.complete();
                        }
                    }
                    
                    @Override
                    public void onError(Throwable error) {
                        try {
                            log.error("SSE 流式对话错误", error);
                            emitter.send(SseEmitter.event()
                                .name("error")
                                .data("{\"error\":\"" + error.getMessage() + "\"}"));
                        } catch (Exception e) {
                            log.warn("发送 SSE 错误信号失败", e);
                        }
                        emitter.completeWithError(error);
                    }
                });
                
            } catch (Exception e) {
                log.error("SSE 流式对话处理失败", e);
                try {
                    emitter.send(SseEmitter.event()
                        .name("error")
                        .data("{\"error\":\"处理失败: " + e.getMessage() + "\"}"));
                } catch (Exception ex) {
                    log.warn("发送 SSE 错误信号失败", ex);
                }
                emitter.completeWithError(e);
            }
        });
        
        // 连接超时或断开时清理
        emitter.onTimeout(() -> log.warn("SSE 连接超时"));
        emitter.onCompletion(() -> log.debug("SSE 连接已关闭"));
        
        return emitter;
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
        private String message;
        private String codeContext;
        private String userId;
    }
}
