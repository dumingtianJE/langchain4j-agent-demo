package com.yourcompany.langchain4j.controller;

import com.yourcompany.langchain4j.agent.CustomerSupportAgent;
import com.yourcompany.langchain4j.agent.OrderManagementAgent;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * AI Agent REST API 控制器
 * 提供 HTTP 接口供前端或其他系统调用
 */
@RestController
@RequestMapping("/api/agent")
@RequiredArgsConstructor
public class AgentController {

    private final CustomerSupportAgent customerSupportAgent;
    private final OrderManagementAgent orderManagementAgent;

    /**
     * 智能客服对话接口
     * 
     * @param message 用户消息
     * @return AI 回复
     */
    @PostMapping("/support/chat")
    public ApiResponse<String> chat(@RequestParam String message) {
        String response = customerSupportAgent.chat(message);
        return ApiResponse.success(response);
    }

    /**
     * 订单管理对话接口
     * 
     * @param message 用户消息
     * @return AI 回复
     */
    @PostMapping("/order/chat")
    public ApiResponse<String> orderChat(@RequestParam String message) {
        String response = orderManagementAgent.chat(message);
        return ApiResponse.success(response);
    }

    /**
     * 健康检查接口
     */
    @GetMapping("/health")
    public ApiResponse<String> health() {
        return ApiResponse.success("Agent Service is running!");
    }

    /**
     * 统一响应格式
     */
    public record ApiResponse<T>(
        int code,
        String message,
        T data
    ) {
        public static <T> ApiResponse<T> success(T data) {
            return new ApiResponse<>(200, "success", data);
        }

        public static <T> ApiResponse<T> error(String message) {
            return new ApiResponse<>(500, message, null);
        }
    }
}
