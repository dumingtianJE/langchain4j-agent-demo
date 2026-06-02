package com.yourcompany.langchain4j;

import com.yourcompany.langchain4j.agent.CustomerSupportAgent;
import com.yourcompany.langchain4j.agent.OrderManagementAgent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * 应用启动时执行的示例代码
 * 展示如何使用 Agent
 */
@Component
@Profile("!production")
public class AgentDemoRunner implements CommandLineRunner {

    @Autowired
    private CustomerSupportAgent customerSupportAgent;

    @Autowired
    private OrderManagementAgent orderManagementAgent;

    @Override
    public void run(String... args) throws Exception {
        System.out.println("\n========== LangChain4j Agent Demo ==========\n");

        // 示例 1: 基础对话
        System.out.println("【示例 1】智能客服对话:");
        String response1 = customerSupportAgent.chat("你好,我想了解一下你们的产品");
        System.out.println("用户: 你好,我想了解一下你们的产品");
        System.out.println("AI: " + response1);
        System.out.println();

        // 示例 2: 订单管理
        System.out.println("【示例 2】订单管理:");
        String response2 = orderManagementAgent.chat("帮我查询订单 ORD-123456 的状态");
        System.out.println("用户: 帮我查询订单 ORD-123456 的状态");
        System.out.println("AI: " + response2);
        System.out.println();

        // 示例 3: 创建订单
        System.out.println("【示例 3】创建订单:");
        String response3 = orderManagementAgent.chat("我想下一个新订单,购买商品A和商品B");
        System.out.println("用户: 我想下一个新订单,购买商品A和商品B");
        System.out.println("AI: " + response3);
        System.out.println();

        System.out.println("========== Demo 执行完成 ==========\n");
    }
}
