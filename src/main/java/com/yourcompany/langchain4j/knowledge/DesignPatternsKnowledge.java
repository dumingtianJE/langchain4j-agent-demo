package com.yourcompany.langchain4j.knowledge;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * 设计模式模范代码
 * 补充 23 种经典设计模式的实战应用示例
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DesignPatternsKnowledge implements CommandLineRunner {
    
    private final KnowledgeBaseManager knowledgeBaseManager;
    
    @Override
    public void run(String... args) {
        loadDesignPatterns();
    }
    
    private void loadDesignPatterns() {
        // 1. 策略模式 - 支付方式
        knowledgeBaseManager.addDocument(new KnowledgeDocument(
            "strategy-pattern-payment",
            "策略模式：支付方式灵活切换",
            """
            【策略模式 - 支付场景】
            
            ## 1. 策略接口
            
            \`\`\`java
            public interface PaymentStrategy {
                /**
                 * 执行支付
                 */
                PaymentResult pay(PaymentRequest request);
                
                /**
                 * 获取支付方式
                 */
                PaymentType getPaymentType();
            }
            \`\`\`
            
            ## 2. 具体策略实现
            
            \`\`\`java
            // 支付宝支付
            @Component
            public class AlipayStrategy implements PaymentStrategy {
                private final AlipayClient alipayClient;
                
                @Override
                public PaymentResult pay(PaymentRequest request) {
                    AlipayTradePayModel model = new AlipayTradePayModel();
                    model.setOutTradeNo(request.getOrderNo());
                    model.setTotalAmount(request.getAmount().toString());
                    model.setSubject(request.getSubject());
                    
                    AlipayTradePayResponse response = alipayClient.execute(
                        new AlipayTradePayRequest(), model
                    );
                    
                    if (response.isSuccess()) {
                        return PaymentResult.success(response.getTradeNo());
                    } else {
                        return PaymentResult.failure(response.getSubMsg());
                    }
                }
                
                @Override
                public PaymentType getPaymentType() {
                    return PaymentType.ALIPAY;
                }
            }
            
            // 微信支付
            @Component
            public class WechatPayStrategy implements PaymentStrategy {
                private final WxPayService wxPayService;
                
                @Override
                public PaymentResult pay(PaymentRequest request) {
                    WxPayUnifiedOrderRequest orderRequest = new WxPayUnifiedOrderRequest();
                    orderRequest.setOutTradeNo(request.getOrderNo());
                    orderRequest.setTotalFee(request.getAmount().multiply(new BigDecimal("100")).intValue());
                    orderRequest.setBody(request.getSubject());
                    
                    try {
                        WxPayUnifiedOrderResult result = wxPayService.unifiedOrder(orderRequest);
                        return PaymentResult.success(result.getPrepayId());
                    } catch (WxPayException e) {
                        return PaymentResult.failure(e.getMessage());
                    }
                }
                
                @Override
                public PaymentType getPaymentType() {
                    return PaymentType.WECHAT;
                }
            }
            
            // 银行卡支付
            @Component
            public class BankCardStrategy implements PaymentStrategy {
                private final BankCardPaymentGateway bankCardGateway;
                
                @Override
                public PaymentResult pay(PaymentRequest request) {
                    return bankCardGateway.executePayment(request);
                }
                
                @Override
                public PaymentType getPaymentType() {
                    return PaymentType.BANK_CARD;
                }
            }
            \`\`\`
            
            ## 3. 策略工厂
            
            \`\`\`java
            @Component
            public class PaymentStrategyFactory {
                private final Map<PaymentType, PaymentStrategy> strategyMap = new ConcurrentHashMap<>();
                
                // 自动注入所有策略
                public PaymentStrategyFactory(List<PaymentStrategy> strategies) {
                    for (PaymentStrategy strategy : strategies) {
                        strategyMap.put(strategy.getPaymentType(), strategy);
                    }
                }
                
                public PaymentStrategy getStrategy(PaymentType paymentType) {
                    PaymentStrategy strategy = strategyMap.get(paymentType);
                    if (strategy == null) {
                        throw new UnsupportedOperationException("不支持的支付方式: " + paymentType);
                    }
                    return strategy;
                }
            }
            \`\`\`
            
            ## 4. 使用示例
            
            \`\`\`java
            @Service
            @RequiredArgsConstructor
            public class PaymentService {
                private final PaymentStrategyFactory strategyFactory;
                private final OrderRepository orderRepository;
                
                @Transactional
                public PaymentResult processPayment(Long orderId, PaymentType paymentType, 
                                                   PaymentRequest request) {
                    // 获取订单
                    Order order = orderRepository.findById(orderId)
                        .orElseThrow(() -> new ResourceNotFoundException("订单不存在"));
                    
                    // 获取对应策略
                    PaymentStrategy strategy = strategyFactory.getStrategy(paymentType);
                    
                    // 执行支付
                    PaymentResult result = strategy.pay(request);
                    
                    // 更新订单状态
                    if (result.isSuccess()) {
                        order.setStatus(OrderStatus.PAID);
                        order.setPaymentType(paymentType);
                        order.setPaymentTime(LocalDateTime.now());
                        orderRepository.save(order);
                    }
                    
                    return result;
                }
            }
            
            // Controller 使用
            @PostMapping("/orders/{id}/pay")
            public PaymentResult payOrder(
                    @PathVariable Long id,
                    @RequestParam PaymentType paymentType,
                    @RequestBody PaymentRequest request) {
                return paymentService.processPayment(id, paymentType, request);
            }
            \`\`\`
            
            ## 关键要点
            
            1. **开闭原则**：新增支付方式无需修改现有代码
            2. **策略工厂**：统一管理策略实例
            3. **自动注入**：Spring 自动发现所有策略实现
            4. **运行时切换**：根据参数动态选择策略
            5. **避免 if-else**：消除条件判断
            """,
            "代码示例",
            new String[]{"strategy-pattern", "design-pattern", "payment", "spring"},
            "system",
            null,
            java.time.LocalDateTime.now(),
            null,
            0,
            null
        ));
        
        // 2. 观察者模式 - 事件驱动
        knowledgeBaseManager.addDocument(new KnowledgeDocument(
            "observer-pattern-events",
            "观察者模式：Spring Events 事件驱动",
            """
            【观察者模式 - Spring Events】
            
            ## 1. 事件定义
            
            \`\`\`java
            // 订单创建事件
            @Getter
            public class OrderCreatedEvent extends ApplicationEvent {
                private final Long orderId;
                private final Long userId;
                private final BigDecimal amount;
                
                public OrderCreatedEvent(Object source, Long orderId, Long userId, BigDecimal amount) {
                    super(source);
                    this.orderId = orderId;
                    this.userId = userId;
                    this.amount = amount;
                }
            }
            
            // 订单支付事件
            @Getter
            public class OrderPaidEvent extends ApplicationEvent {
                private final Long orderId;
                private final String paymentNo;
                private final LocalDateTime paidAt;
                
                public OrderPaidEvent(Object source, Long orderId, String paymentNo, LocalDateTime paidAt) {
                    super(source);
                    this.orderId = orderId;
                    this.paymentNo = paymentNo;
                    this.paidAt = paidAt;
                }
            }
            \`\`\`
            
            ## 2. 事件发布
            
            \`\`\`java
            @Service
            @RequiredArgsConstructor
            public class OrderService {
                private final OrderRepository orderRepository;
                private final ApplicationEventPublisher eventPublisher;
                
                @Transactional
                public OrderDTO createOrder(CreateOrderRequest request) {
                    // 保存订单
                    Order order = orderRepository.save(request.toEntity());
                    
                    // 发布事件
                    eventPublisher.publishEvent(new OrderCreatedEvent(
                        this,
                        order.getId(),
                        order.getUserId(),
                        order.getTotalAmount()
                    ));
                    
                    return OrderDTO.fromEntity(order);
                }
                
                @Transactional
                public void payOrder(Long orderId, String paymentNo) {
                    Order order = orderRepository.findById(orderId)
                        .orElseThrow(() -> new ResourceNotFoundException("订单不存在"));
                    
                    order.setStatus(OrderStatus.PAID);
                    orderRepository.save(order);
                    
                    // 发布支付事件
                    eventPublisher.publishEvent(new OrderPaidEvent(
                        this,
                        orderId,
                        paymentNo,
                        LocalDateTime.now()
                    ));
                }
            }
            \`\`\`
            
            ## 3. 事件监听器
            
            \`\`\`java
            // 库存监听器
            @Component
            @RequiredArgsConstructor
            public class InventoryEventListener {
                private final InventoryService inventoryService;
                private final MessageService messageService;
                
                @Async
                @EventListener
                public void handleOrderCreated(OrderCreatedEvent event) {
                    // 预扣库存
                    inventoryService.deductStock(event.getOrderId());
                    
                    // 发送通知
                    messageService.sendOrderCreatedNotification(event.getUserId(), event.getOrderId());
                }
                
                @Async
                @EventListener
                public void handleOrderPaid(OrderPaidEvent event) {
                    // 扣减实际库存
                    inventoryService.reduceStock(event.getOrderId());
                    
                    // 通知仓库发货
                    messageService.notifyWarehouse(event.getOrderId());
                }
            }
            
            // 积分监听器
            @Component
            @RequiredArgsConstructor
            public class PointsEventListener {
                private final PointsService pointsService;
                private final AnalyticsService analyticsService;
                
                @Async
                @EventListener
                public void handleOrderPaid(OrderPaidEvent event) {
                    // 增加用户积分
                    pointsService.addPoints(event.getOrderId());
                    
                    // 记录分析数据
                    analyticsService.trackPurchase(event);
                }
                
                @Async
                @EventListener(OrderCancelledEvent.class)
                public void handleOrderCancelled(OrderCancelledEvent event) {
                    // 扣减用户积分
                    pointsService.deductPoints(event.getOrderId());
                }
            }
            
            // 日志监听器
            @Component
            @Slf4j
            public class OrderLogEventListener {
                @EventListener
                public void logOrderEvents(ApplicationEvent event) {
                    if (event instanceof OrderCreatedEvent) {
                        log.info("订单创建: orderId={}", ((OrderCreatedEvent) event).getOrderId());
                    } else if (event instanceof OrderPaidEvent) {
                        log.info("订单支付: orderId={}, paymentNo={}", 
                            ((OrderPaidEvent) event).getOrderId(),
                            ((OrderPaidEvent) event).getPaymentNo());
                    }
                }
            }
            \`\`\`
            
            ## 4. 事务性事件监听器
            
            \`\`\`java
            @Component
            public class TransactionalEventListenerExample {
                
                // 事务提交前执行
                @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
                public void handleBeforeCommit(OrderCreatedEvent event) {
                    // 在事务提交前执行
                }
                
                // 事务提交后执行
                @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
                public void handleAfterCommit(OrderCreatedEvent event) {
                    // 在事务提交后执行（推荐）
                    sendEmail(event.getUserId());
                }
                
                // 事务回滚后执行
                @TransactionalEventListener(phase = TransactionPhase.AFTER_ROLLBACK)
                public void handleAfterRollback(OrderCreatedEvent event) {
                    // 在事务回滚后执行
                    log.error("订单创建失败，回滚: orderId={}", event.getOrderId());
                }
            }
            \`\`\`
            
            ## 关键要点
            
            1. **解耦**：发布者和订阅者完全解耦
            2. **异步处理**：@Async 提升性能
            3. **事务控制**：@TransactionalEventListener
            4. **多监听器**：一个事件可被多个监听器处理
            5. **避免循环依赖**：注意事件链设计
            """,
            "代码示例",
            new String[]{"observer-pattern", "spring-events", "event-driven", "design-pattern"},
            "system",
            null,
            java.time.LocalDateTime.now(),
            null,
            0,
            null
        ));
        
        // 3. 工厂模式 - 订单类型
        knowledgeBaseManager.addDocument(new KnowledgeDocument(
            "factory-pattern-order",
            "工厂模式：订单类型创建",
            """
            【工厂模式 - 订单处理】
            
            ## 1. 抽象产品
            
            \`\`\`java
            public interface OrderHandler {
                /**
                 * 创建订单
                 */
                Order createOrder(OrderRequest request);
                
                /**
                 * 验证订单
                 */
                void validateOrder(OrderRequest request);
                
                /**
                 * 获取订单类型
                 */
                OrderType getOrderType();
            }
            \`\`\`
            
            ## 2. 具体产品
            
            \`\`\`java
            // 普通订单
            @Component
            public class NormalOrderHandler implements OrderHandler {
                private final OrderRepository orderRepository;
                private final PricingService pricingService;
                
                @Override
                public Order createOrder(OrderRequest request) {
                    validateOrder(request);
                    
                    BigDecimal finalPrice = pricingService.calculatePrice(
                        request.getItems(), 
                        PricingStrategy.NORMAL
                    );
                    
                    return Order.builder()
                        .orderNo(generateOrderNo())
                        .userId(request.getUserId())
                        .items(request.getItems())
                        .totalAmount(finalPrice)
                        .orderType(OrderType.NORMAL)
                        .status(OrderStatus.PENDING)
                        .build();
                }
                
                @Override
                public void validateOrder(OrderRequest request) {
                    if (request.getItems().isEmpty()) {
                        throw new BusinessException("订单商品不能为空");
                    }
                }
                
                @Override
                public OrderType getOrderType() {
                    return OrderType.NORMAL;
                }
            }
            
            // 团购订单
            @Component
            public class GroupBuyOrderHandler implements OrderHandler {
                private final GroupBuyService groupBuyService;
                
                @Override
                public Order createOrder(OrderRequest request) {
                    validateOrder(request);
                    
                    // 验证团购活动
                    GroupBuyActivity activity = groupBuyService.validateActivity(
                        request.getGroupBuyId()
                    );
                    
                    // 检查成团人数
                    if (activity.getCurrentParticipants() >= activity.getMaxParticipants()) {
                        throw new BusinessException("团购人数已满");
                    }
                    
                    BigDecimal groupPrice = activity.getGroupPrice();
                    
                    return Order.builder()
                        .orderNo(generateOrderNo())
                        .userId(request.getUserId())
                        .groupBuyId(request.getGroupBuyId())
                        .totalAmount(groupPrice.multiply(new BigDecimal(request.getItems().size())))
                        .orderType(OrderType.GROUP_BUY)
                        .status(OrderStatus.PENDING)
                        .build();
                }
                
                @Override
                public void validateOrder(OrderRequest request) {
                    if (request.getGroupBuyId() == null) {
                        throw new BusinessException("团购活动 ID 不能为空");
                    }
                }
                
                @Override
                public OrderType getOrderType() {
                    return OrderType.GROUP_BUY;
                }
            }
            
            // 秒杀订单
            @Component
            public class FlashSaleOrderHandler implements OrderHandler {
                private final RedisTemplate<String, String> redisTemplate;
                
                @Override
                public Order createOrder(OrderRequest request) {
                    validateOrder(request);
                    
                    // 检查库存（Redis 原子操作）
                    String stockKey = "flash_sale:stock:" + request.getFlashSaleId();
                    Long stock = redisTemplate.opsForValue().decrement(stockKey);
                    
                    if (stock == null || stock < 0) {
                        throw new BusinessException("秒杀商品已售罄");
                    }
                    
                    // 限制购买数量
                    String userKey = "flash_sale:user:" + request.getFlashSaleId() + ":" + request.getUserId();
                    if (Boolean.TRUE.equals(redisTemplate.hasKey(userKey))) {
                        throw new BusinessException("您已购买过该商品");
                    }
                    redisTemplate.opsForValue().set(userKey, "1", 1, TimeUnit.HOURS);
                    
                    FlashSaleItem item = getFlashSaleItem(request.getFlashSaleId());
                    
                    return Order.builder()
                        .orderNo(generateOrderNo())
                        .userId(request.getUserId())
                        .flashSaleId(request.getFlashSaleId())
                        .totalAmount(item.getFlashPrice())
                        .orderType(OrderType.FLASH_SALE)
                        .status(OrderStatus.PENDING)
                        .expireTime(LocalDateTime.now().plusMinutes(15))
                        .build();
                }
                
                @Override
                public void validateOrder(OrderRequest request) {
                    if (request.getFlashSaleId() == null) {
                        throw new BusinessException("秒杀活动 ID 不能为空");
                    }
                }
                
                @Override
                public OrderType getOrderType() {
                    return OrderType.FLASH_SALE;
                }
            }
            \`\`\`
            
            ## 3. 工厂类
            
            \`\`\`java
            @Component
            public class OrderHandlerFactory {
                private final Map<OrderType, OrderHandler> handlerMap = new ConcurrentHashMap<>();
                
                // 自动注入所有处理器
                public OrderHandlerFactory(List<OrderHandler> handlers) {
                    for (OrderHandler handler : handlers) {
                        handlerMap.put(handler.getOrderType(), handler);
                    }
                }
                
                public OrderHandler getHandler(OrderType orderType) {
                    OrderHandler handler = handlerMap.get(orderType);
                    if (handler == null) {
                        throw new UnsupportedOperationException("不支持的订单类型: " + orderType);
                    }
                    return handler;
                }
            }
            \`\`\`
            
            ## 4. 使用示例
            
            \`\`\`java
            @Service
            @RequiredArgsConstructor
            public class OrderCreateService {
                private final OrderHandlerFactory handlerFactory;
                private final OrderRepository orderRepository;
                
                @Transactional
                public OrderDTO createOrder(CreateOrderRequest request) {
                    // 获取对应订单处理器
                    OrderHandler handler = handlerFactory.getHandler(request.getOrderType());
                    
                    // 创建订单
                    Order order = handler.createOrder(request);
                    
                    // 保存订单
                    Order savedOrder = orderRepository.save(order);
                    
                    return OrderDTO.fromEntity(savedOrder);
                }
            }
            \`\`\`
            
            ## 关键要点
            
            1. **简单工厂**：工厂类负责创建对象
            2. **工厂方法**：子类决定实例化哪个类
            3. **抽象工厂**：创建一组相关对象
            4. **Spring 注入**：自动发现所有实现
            5. **策略 + 工厂**：常结合使用
            """,
            "代码示例",
            new String[]{"factory-pattern", "design-pattern", "order", "spring"},
            "system",
            null,
            java.time.LocalDateTime.now(),
            null,
            0,
            null
        ));
        
        // 4. 责任链模式 - 订单审核
        knowledgeBaseManager.addDocument(new KnowledgeDocument(
            "chain-of-responsibility-approval",
            "责任链模式：订单审核流程",
            """
            【责任链模式 - 审批流程】
            
            ## 1. 处理器接口
            
            \`\`\`java
            public abstract class ApprovalHandler {
                protected ApprovalHandler nextHandler;
                
                /**
                 * 设置下一个处理器
                 */
                public void setNextHandler(ApprovalHandler handler) {
                    this.nextHandler = handler;
                }
                
                /**
                 * 处理审批
                 */
                public abstract ApprovalResult handle(Order order);
            }
            \`\`\`
            
            ## 2. 具体处理器
            
            \`\`\`java
            // 风控审核
            @Component
            public class RiskControlHandler extends ApprovalHandler {
                private final RiskControlService riskControlService;
                
                @Override
                public ApprovalResult handle(Order order) {
                    log.info("风控审核: orderId={}", order.getId());
                    
                    // 风控检查
                    RiskAssessment assessment = riskControlService.assessRisk(order);
                    
                    if (assessment.getRiskLevel() == RiskLevel.HIGH) {
                        return ApprovalResult.reject("订单存在高风险");
                    }
                    
                    // 传递给下一个处理器
                    if (nextHandler != null) {
                        return nextHandler.handle(order);
                    }
                    
                    return ApprovalResult.pass();
                }
            }
            
            // 库存审核
            @Component
            public class InventoryHandler extends ApprovalHandler {
                private final InventoryService inventoryService;
                
                @Override
                public ApprovalResult handle(Order order) {
                    log.info("库存审核: orderId={}", order.getId());
                    
                    // 检查库存
                    boolean hasStock = inventoryService.checkStock(order.getItems());
                    
                    if (!hasStock) {
                        return ApprovalResult.reject("商品库存不足");
                    }
                    
                    // 预扣库存
                    inventoryService.preDeductStock(order.getId());
                    
                    // 传递给下一个处理器
                    if (nextHandler != null) {
                        return nextHandler.handle(order);
                    }
                    
                    return ApprovalResult.pass();
                }
            }
            
            // 财务审核
            @Component
            public class FinanceHandler extends ApprovalHandler {
                private final FinanceService financeService;
                
                @Override
                public ApprovalResult handle(Order order) {
                    log.info("财务审核: orderId={}", order.getId());
                    
                    // 大额订单需要财务审核
                    if (order.getTotalAmount().compareTo(new BigDecimal("10000")) > 0) {
                        log.info("大额订单，需要财务审核");
                        return ApprovalResult.pending("等待财务审核");
                    }
                    
                    // 传递给下一个处理器
                    if (nextHandler != null) {
                        return nextHandler.handle(order);
                    }
                    
                    return ApprovalResult.pass();
                }
            }
            \`\`\`
            
            ## 3. 责任链构建器
            
            \`\`\`java
            @Component
            public class ApprovalChainBuilder {
                private final RiskControlHandler riskControlHandler;
                private final InventoryHandler inventoryHandler;
                private final FinanceHandler financeHandler;
                
                public ApprovalChainBuilder(RiskControlHandler riskControlHandler,
                                          InventoryHandler inventoryHandler,
                                          FinanceHandler financeHandler) {
                    this.riskControlHandler = riskControlHandler;
                    this.inventoryHandler = inventoryHandler;
                    this.financeHandler = financeHandler;
                }
                
                public ApprovalHandler buildChain() {
                    // 构建责任链：风控 -> 库存 -> 财务
                    riskControlHandler.setNextHandler(inventoryHandler);
                    inventoryHandler.setNextHandler(financeHandler);
                    
                    return riskControlHandler;
                }
            }
            \`\`\`
            
            ## 4. 使用示例
            
            \`\`\`java
            @Service
            @RequiredArgsConstructor
            public class OrderApprovalService {
                private final ApprovalChainBuilder chainBuilder;
                private final OrderRepository orderRepository;
                
                @Transactional
                public ApprovalResult approveOrder(Long orderId) {
                    Order order = orderRepository.findById(orderId)
                        .orElseThrow(() -> new ResourceNotFoundException("订单不存在"));
                    
                    // 构建责任链
                    ApprovalHandler chain = chainBuilder.buildChain();
                    
                    // 执行审批
                    ApprovalResult result = chain.handle(order);
                    
                    // 更新订单状态
                    order.setApprovalStatus(result.getStatus());
                    order.setApprovalMessage(result.getMessage());
                    orderRepository.save(order);
                    
                    return result;
                }
            }
            \`\`\`
            
            ## 关键要点
            
            1. **解耦**：请求发送者和接收者解耦
            2. **动态组合**：运行时动态构建责任链
            3. **单一职责**：每个处理器只负责一个环节
            4. **灵活扩展**：新增处理器无需修改现有代码
            5. **注意性能**：链路过长影响性能
            """,
            "代码示例",
            new String[]{"chain-of-responsibility", "design-pattern", "approval", "workflow"},
            "system",
            null,
            java.time.LocalDateTime.now(),
            null,
            0,
            null
        ));
        
        // 5. 模板方法模式 - 数据导入
        knowledgeBaseManager.addDocument(new KnowledgeDocument(
            "template-method-import",
            "模板方法模式：数据导入导出",
            """
            【模板方法模式 - 数据处理】
            
            ## 1. 抽象模板类
            
            \`\`\`java
            public abstract class AbstractDataImporter<T> {
                
                /**
                 * 模板方法 - 定义算法骨架
                 */
                public final ImportResult importData(MultipartFile file) {
                    try {
                        // 1. 验证文件
                        validateFile(file);
                        
                        // 2. 解析数据（子类实现）
                        List<T> dataList = parseData(file);
                        
                        // 3. 数据校验（子类实现）
                        ValidationResult validation = validateData(dataList);
                        if (!validation.isValid()) {
                            return ImportResult.failure(validation.getErrors());
                        }
                        
                        // 4. 数据转换（子类实现）
                        List<T> convertedData = convertData(dataList);
                        
                        // 5. 批量保存
                        saveData(convertedData);
                        
                        // 6. 后处理（可选）
                        afterImport(convertedData);
                        
                        return ImportResult.success(convertedData.size());
                        
                    } catch (Exception e) {
                        log.error("数据导入失败", e);
                        return ImportResult.failure(e.getMessage());
                    }
                }
                
                /**
                 * 验证文件格式
                 */
                protected void validateFile(MultipartFile file) {
                    if (file.isEmpty()) {
                        throw new BusinessException("文件不能为空");
                    }
                    
                    String contentType = file.getContentType();
                    if (!isSupportedContentType(contentType)) {
                        throw new BusinessException("不支持的文件格式");
                    }
                }
                
                /**
                 * 解析数据（子类实现）
                 */
                protected abstract List<T> parseData(MultipartFile file);
                
                /**
                 * 数据校验（子类实现）
                 */
                protected abstract ValidationResult validateData(List<T> dataList);
                
                /**
                 * 数据转换（子类实现）
                 */
                protected abstract List<T> convertData(List<T> dataList);
                
                /**
                 * 保存数据
                 */
                protected abstract void saveData(List<T> dataList);
                
                /**
                 * 后处理（可选）
                 */
                protected void afterImport(List<T> dataList) {
                    // 默认不执行任何操作
                }
                
                private boolean isSupportedContentType(String contentType) {
                    return contentType != null && (
                        contentType.equals("text/csv") ||
                        contentType.equals("application/vnd.ms-excel") ||
                        contentType.equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                    );
                }
            }
            \`\`\`
            
            ## 2. 具体实现
            
            \`\`\`java
            // 用户数据导入
            @Service
            public class UserImporter extends AbstractDataImporter<UserImportDTO> {
                private final UserRepository userRepository;
                private final PasswordEncoder passwordEncoder;
                
                @Override
                protected List<UserImportDTO> parseData(MultipartFile file) {
                    // 使用 EasyExcel 解析
                    return EasyExcel.read(file.getInputStream())
                        .head(UserImportDTO.class)
                        .sheet()
                        .doReadSync();
                }
                
                @Override
                protected ValidationResult validateData(List<UserImportDTO> dataList) {
                    List<String> errors = new ArrayList<>();
                    
                    for (int i = 0; i < dataList.size(); i++) {
                        UserImportDTO dto = dataList.get(i);
                        int rowNum = i + 2; // Excel 行号
                        
                        if (StringUtils.isBlank(dto.getUsername())) {
                            errors.add("第" + rowNum + "行：用户名不能为空");
                        }
                        
                        if (StringUtils.isBlank(dto.getEmail())) {
                            errors.add("第" + rowNum + "行：邮箱不能为空");
                        } else if (!isValidEmail(dto.getEmail())) {
                            errors.add("第" + rowNum + "行：邮箱格式不正确");
                        }
                    }
                    
                    return errors.isEmpty() 
                        ? ValidationResult.valid() 
                        : ValidationResult.invalid(errors);
                }
                
                @Override
                protected List<UserImportDTO> convertData(List<UserImportDTO> dataList) {
                    return dataList.stream()
                        .map(dto -> {
                            dto.setPassword(passwordEncoder.encode("123456")); // 默认密码
                            return dto;
                        })
                        .collect(Collectors.toList());
                }
                
                @Override
                protected void saveData(List<UserImportDTO> dataList) {
                    List<User> users = dataList.stream()
                        .map(UserImportDTO::toEntity)
                        .collect(Collectors.toList());
                    
                    userRepository.saveAll(users);
                }
                
                @Override
                protected void afterImport(List<UserImportDTO> dataList) {
                    // 发送欢迎邮件
                    dataList.forEach(dto -> 
                        emailService.sendWelcomeEmail(dto.getEmail(), dto.getUsername())
                    );
                }
                
                private boolean isValidEmail(String email) {
                    return email.matches("^[A-Za-z0-9+_.-]+@(.+)$");
                }
            }
            \`\`\`
            
            ## 3. 使用示例
            
            \`\`\`java
            @RestController
            @RequestMapping("/api/import")
            @RequiredArgsConstructor
            public class DataImportController {
                private final UserImporter userImporter;
                private final ProductImporter productImporter;
                
                @PostMapping("/users")
                public ImportResult importUsers(@RequestParam("file") MultipartFile file) {
                    return userImporter.importData(file);
                }
                
                @PostMapping("/products")
                public ImportResult importProducts(@RequestParam("file") MultipartFile file) {
                    return productImporter.importData(file);
                }
            }
            \`\`\`
            
            ## 关键要点
            
            1. **算法骨架**：父类定义算法流程
            2. **延迟实现**：子类实现具体步骤
            3. **钩子方法**：可选的扩展点
            4. **代码复用**：避免重复代码
            5. **好莱坞原则**：别调用我们，我们会调用你
            """,
            "代码示例",
            new String[]{"template-method", "design-pattern", "import-export", "data-processing"},
            "system",
            null,
            java.time.LocalDateTime.now(),
            null,
            0,
            null
        ));
        
        log.info("设计模式模范代码知识库加载完成");
    }
}
