package com.yourcompany.langchain4j.tool;

import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 订单操作工具
 * 提供订单相关的业务操作能力
 */
@Slf4j
@Component
public class OrderTool {

    /**
     * 查询订单详情
     * 
     * @param orderId 订单ID
     * @return 订单详情信息
     */
    @Tool("查询订单详细信息,包括订单状态、商品列表、金额等")
    public String getOrderDetails(String orderId) {
        log.info("查询订单: {}", orderId);
        
        // TODO: 替换为实际的数据库查询或服务调用
        return String.format("""
            订单详情:
            - 订单号: %s
            - 状态: 已发货
            - 商品: 商品A x1, 商品B x2
            - 金额: ¥299.00
            - 创建时间: 2024-01-15 10:30:00
            - 预计送达: 2024-01-18
            """, orderId);
    }

    /**
     * 创建新订单
     * 
     * @param userId 用户ID
     * @param items 商品信息
     * @return 订单创建结果
     */
    @Tool("创建新的订单")
    public String createOrder(String userId, String items) {
        log.info("为用户 {} 创建订单,商品: {}", userId, items);
        
        // TODO: 替换为实际的订单创建逻辑
        String orderId = "ORD-" + System.currentTimeMillis();
        
        return String.format("""
            订单创建成功:
            - 订单号: %s
            - 用户ID: %s
            - 商品: %s
            - 总金额: ¥299.00
            - 状态: 待支付
            
            请引导用户完成支付流程。
            """, orderId, userId, items);
    }

    /**
     * 取消订单
     * 
     * @param orderId 订单ID
     * @param reason 取消原因
     * @return 取消结果
     */
    @Tool("取消指定订单,需要提供订单号和取消原因")
    public String cancelOrder(String orderId, String reason) {
        log.info("取消订单: {}, 原因: {}", orderId, reason);
        
        // TODO: 替换为实际的订单取消逻辑
        return String.format("""
            订单取消成功:
            - 订单号: %s
            - 取消原因: %s
            - 退款金额: ¥299.00
            - 退款状态: 已发起退款,1-3个工作日内到账
            
            如有问题,请联系客服。
            """, orderId, reason);
    }

    /**
     * 查询订单状态
     * 
     * @param orderId 订单ID
     * @return 订单当前状态
     */
    @Tool("查询订单的当前状态")
    public String getOrderStatus(String orderId) {
        log.info("查询订单状态: {}", orderId);
        
        // TODO: 替换为实际的状态查询逻辑
        return String.format("订单 %s 的当前状态是: 已发货", orderId);
    }
}
