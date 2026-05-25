package com.yourcompany.langchain4j.agent;

import com.yourcompany.langchain4j.tool.OrderTool;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;

/**
 * 订单管理 Agent
 * 处理订单查询、创建、取消等操作
 */
@AiService
public interface OrderManagementAgent {

    @SystemMessage("""
        你是一个订单管理专家助手。
        
        你可以执行的操作:
        1. 查询订单状态和详情
        2. 创建新订单
        3. 取消订单
        4. 修改订单信息
        
        处理流程:
        1. 首先确认用户身份和订单号
        2. 根据用户需求调用相应工具
        3. 向用户反馈操作结果
        4. 如有异常,提供解决建议
        
        注意事项:
        - 涉及敏感操作(如取消订单)需要用户二次确认
        - 所有操作都要记录日志
        - 失败时要提供明确的错误信息和解决方案
        """)
    String chat(@UserMessage String message);
}
