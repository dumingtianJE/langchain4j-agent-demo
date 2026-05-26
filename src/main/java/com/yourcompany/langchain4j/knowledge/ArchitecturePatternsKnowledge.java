package com.yourcompany.langchain4j.knowledge;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * 技术架构模范代码知识库
 * 补充微服务、分布式系统、云原生等架构模式的高质量代码示例
 * 
 * 注意：文档内容中不使用 Markdown 代码块标记（```），避免 Java 编译错误
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ArchitecturePatternsKnowledge implements CommandLineRunner {
    
    private final KnowledgeBaseManager knowledgeBaseManager;
    
    @Override
    public void run(String... args) {
        loadArchitecturePatterns();
    }
    
    private void loadArchitecturePatterns() {
        // 1. 微服务架构 - 服务间通信
        knowledgeBaseManager.addDocument(new KnowledgeDocument(
            "microservice-communication-patterns",
            "微服务架构：服务间通信完整示例（包含 REST/Feign、RabbitMQ、Resilience4j）",
            """
            【微服务通信模式完整示例】
            
            【1. 同步通信 - REST API + Feign Client】
            
            // Feign Client 定义
            @FeignClient(name = "user-service", url = "${services.user-service.url}")
            public interface UserClient {
                @GetMapping("/api/users/{id}")
                UserDTO getUserById(@PathVariable("id") Long id);
                
                @PostMapping("/api/users/batch")
                List<UserDTO> batchGetUsers(@RequestBody List<Long> ids);
            }
            
            // 使用示例
            @Service
            @RequiredArgsConstructor
            public class OrderService {
                private final UserClient userClient;
                
                public OrderDTO createOrder(CreateOrderRequest request) {
                    // 同步调用用户服务
                    UserDTO user = userClient.getUserById(request.getUserId());
                    if (user == null) {
                        throw new BusinessException("用户不存在");
                    }
                    
                    // 创建订单逻辑
                    Order order = Order.builder()
                        .userId(user.getId())
                        .items(request.getItems())
                        .totalAmount(calculateTotal(request.getItems()))
                        .status(OrderStatus.PENDING)
                        .createdAt(LocalDateTime.now())
                        .build();
                    
                    return OrderDTO.fromEntity(orderRepository.save(order));
                }
            }
            
            【2. 异步通信 - Spring Events + RabbitMQ】
            
            // 事件定义
            @Data
            @Builder
            public class OrderCreatedEvent {
                private String orderId;
                private Long userId;
                private BigDecimal totalAmount;
                private LocalDateTime createdAt;
            }
            
            // 事件发布
            @Service
            @RequiredArgsConstructor
            public class OrderService {
                private final ApplicationEventPublisher eventPublisher;
                private final RabbitTemplate rabbitTemplate;
                
                @Transactional
                public OrderDTO createOrder(CreateOrderRequest request) {
                    Order order = orderRepository.save(request.toEntity());
                    
                    // 发布本地事件
                    eventPublisher.publishEvent(new OrderCreatedEvent(
                        order.getId(), order.getUserId(),
                        order.getTotalAmount(), LocalDateTime.now()
                    ));
                    
                    // 发送消息到 MQ（用于跨服务通信）
                    OrderMessage message = OrderMessage.builder()
                        .orderId(order.getId())
                        .action("CREATE")
                        .timestamp(System.currentTimeMillis())
                        .build();
                    
                    rabbitTemplate.convertAndSend(
                        "order.exchange", "order.created", message
                    );
                    
                    return OrderDTO.fromEntity(order);
                }
            }
            
            // 事件监听 - 库存服务
            @Component
            @RequiredArgsConstructor
            public class InventoryListener {
                private final InventoryService inventoryService;
                
                @RabbitListener(queues = "${rabbitmq.queues.order-created}")
                public void handleOrderCreated(OrderMessage message) {
                    log.info("收到订单创建消息: {}", message.getOrderId());
                    // 扣减库存
                    inventoryService.deductStock(message.getOrderId());
                }
            }
            
            【3. 服务熔断 - Resilience4j】
            
            @Service
            @RequiredArgsConstructor
            public class ProductService {
                private final ProductClient productClient;
                
                @CircuitBreaker(name = "productService", fallbackMethod = "getProductFallback")
                @Retry(name = "productService")
                @TimeLimiter(name = "productService")
                public CompletableFuture<ProductDTO> getProductAsync(Long productId) {
                    return CompletableFuture.supplyAsync(() -> 
                        productClient.getProduct(productId)
                    );
                }
                
                // 降级方法
                public CompletableFuture<ProductDTO> getProductFallback(
                        Long productId, Throwable t) {
                    log.error("获取产品信息失败，使用降级数据", t);
                    return CompletableFuture.completedFuture(
                        ProductDTO.builder()
                            .id(productId)
                            .name("产品信息暂时不可用")
                            .price(BigDecimal.ZERO)
                            .isFallback(true)
                            .build()
                    );
                }
            }
            
            【配置示例 application.yml】
            resilience4j:
              circuitbreaker:
                instances:
                  productService:
                    slidingWindowSize: 10
                    failureRateThreshold: 50
                    waitDurationInOpenState: 10000
                    permittedNumberOfCallsInHalfOpenState: 3
              retry:
                instances:
                  productService:
                    maxAttempts: 3
                    waitDuration: 100ms
                    retryExceptions:
                      - org.springframework.web.client.ResourceAccessException
              timelimiter:
                instances:
                  productService:
                    timeoutDuration: 2s
            
            【关键要点】
            1. 同步通信：适用于强一致性场景（查询、验证）
            2. 异步通信：适用于最终一致性场景（通知、日志、统计）
            3. 熔断降级：防止级联故障，提升系统可用性
            4. 超时控制：避免线程阻塞，保护系统资源
            5. 幂等性：消息处理必须支持重试
            """,
            "代码示例",
            new String[]{"microservice", "communication", "feign", "rabbitmq", "resilience4j"},
            "system",
            null,
            java.time.LocalDateTime.now(),
            null,
            0,
            null
        ));
        
        // 2. 分布式缓存 - Redis 高级应用
        knowledgeBaseManager.addDocument(new KnowledgeDocument(
            "distributed-cache-redis-patterns",
            "分布式缓存：Redis 高级应用模式（分布式锁、布隆过滤器、限流器）",
            """
            【Redis 高级应用模式完整示例】
            
            【1. 分布式锁】
            
            @Service
            @RequiredArgsConstructor
            public class DistributedLockService {
                private final StringRedisTemplate redisTemplate;
                
                /**
                 * 获取分布式锁
                 */
                public boolean tryLock(String lockKey, String requestId, long expireSeconds) {
                    Boolean success = redisTemplate.opsForValue()
                        .setIfAbsent(lockKey, requestId, expireSeconds, TimeUnit.SECONDS);
                    return Boolean.TRUE.equals(success);
                }
                
                /**
                 * 释放分布式锁（Lua 脚本保证原子性）
                 */
                public boolean releaseLock(String lockKey, String requestId) {
                    String script = "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                        "return redis.call('del', KEYS[1]) " +
                        "else " +
                        "return 0 " +
                        "end";
                    
                    Long result = redisTemplate.execute(
                        new DefaultRedisScript<>(script, Long.class),
                        Collections.singletonList(lockKey),
                        requestId
                    );
                    
                    return result == 1;
                }
                
                /**
                 * 使用示例
                 */
                public void processOrder(String orderId) {
                    String lockKey = "lock:order:" + orderId;
                    String requestId = UUID.randomUUID().toString();
                    
                    try {
                        if (!tryLock(lockKey, requestId, 10)) {
                            throw new BusinessException("订单处理中，请勿重复提交");
                        }
                        
                        // 业务逻辑
                        processOrderLogic(orderId);
                        
                    } finally {
                        releaseLock(lockKey, requestId);
                    }
                }
            }
            
            【2. 缓存穿透保护（布隆过滤器）】
            
            @Service
            @RequiredArgsConstructor
            public class BloomFilterService {
                private final RedissonClient redissonClient;
                private RBloomFilter<String> bloomFilter;
                
                @PostConstruct
                public void init() {
                    bloomFilter = redissonClient.getBloomFilter("product:bloom");
                    bloomFilter.tryInit(1000000L, 0.01); // 预期100万元素，1%误判率
                    loadExistingData();
                }
                
                public boolean mightExist(String productId) {
                    return bloomFilter.contains(productId);
                }
                
                public void add(String productId) {
                    bloomFilter.add(productId);
                }
            }
            
            // 使用示例
            @Service
            @RequiredArgsConstructor
            public class ProductQueryService {
                private final BloomFilterService bloomFilter;
                private final RedisTemplate<String, Object> redisTemplate;
                private final ProductRepository productRepository;
                
                public ProductDTO getProduct(String productId) {
                    // 1. 布隆过滤器检查
                    if (!bloomFilter.mightExist(productId)) {
                        return null; // 一定不存在，直接返回
                    }
                    
                    // 2. 查询缓存
                    Object cached = redisTemplate.opsForValue().get("product:" + productId);
                    if (cached != null) {
                        return (ProductDTO) cached;
                    }
                    
                    // 3. 查询数据库
                    Product product = productRepository.findById(productId).orElse(null);
                    if (product == null) {
                        // 缓存空值，防止穿透
                        redisTemplate.opsForValue().set(
                            "product:" + productId, null, 5, TimeUnit.MINUTES
                        );
                        return null;
                    }
                    
                    // 4. 写入缓存
                    ProductDTO dto = ProductDTO.fromEntity(product);
                    redisTemplate.opsForValue().set(
                        "product:" + productId, dto, 30, TimeUnit.MINUTES
                    );
                    
                    return dto;
                }
            }
            
            【3. 限流器（令牌桶算法）】
            
            @Service
            @RequiredArgsConstructor
            public class RateLimiterService {
                private final RedisTemplate<String, String> redisTemplate;
                
                /**
                 * 令牌桶限流
                 */
                public boolean tryAcquire(String key, int maxTokens, int refillRate) {
                    String script = "local key = KEYS[1] " +
                        "local maxTokens = tonumber(ARGV[1]) " +
                        "local refillRate = tonumber(ARGV[2]) " +
                        "local now = tonumber(redis.call('TIME')[1]) " +
                        "local bucket = redis.call('HMGET', key, 'tokens', 'lastRefill') " +
                        "local tokens = tonumber(bucket[1]) or maxTokens " +
                        "local lastRefill = tonumber(bucket[2]) or now " +
                        "local elapsed = now - lastRefill " +
                        "local newTokens = math.floor(elapsed * refillRate) " +
                        "tokens = math.min(maxTokens, tokens + newTokens) " +
                        "if tokens > 0 then " +
                        "tokens = tokens - 1 " +
                        "redis.call('HMSET', key, 'tokens', tokens, 'lastRefill', now) " +
                        "redis.call('EXPIRE', key, 60) " +
                        "return 1 " +
                        "else " +
                        "return 0 " +
                        "end";
                    
                    Long result = redisTemplate.execute(
                        new DefaultRedisScript<>(script, Long.class),
                        Collections.singletonList("ratelimit:" + key),
                        String.valueOf(maxTokens),
                        String.valueOf(refillRate)
                    );
                    
                    return result == 1;
                }
            }
            
            【4. 分布式计数器】
            
            @Service
            @RequiredArgsConstructor
            public class DistributedCounterService {
                private final StringRedisTemplate redisTemplate;
                
                public Long increment(String key, long delta) {
                    return redisTemplate.opsForValue().increment(key, delta);
                }
                
                public Long incrementWithExpire(String key, long delta, long seconds) {
                    Long value = redisTemplate.opsForValue().increment(key, delta);
                    if (value != null && value == delta) {
                        redisTemplate.expire(key, seconds, TimeUnit.SECONDS);
                    }
                    return value;
                }
                
                // 使用示例：接口调用次数统计
                public boolean checkApiLimit(String userId, int limit) {
                    String key = "api:limit:" + userId + ":" + LocalDate.now();
                    Long count = incrementWithExpire(key, 1, 86400);
                    
                    if (count != null && count > limit) {
                        throw new BusinessException("今日 API 调用次数已达上限");
                    }
                    return true;
                }
            }
            
            【关键要点】
            1. 分布式锁：使用 Lua 脚本保证释放锁的原子性
            2. 缓存穿透：布隆过滤器 + 空值缓存
            3. 缓存雪崩：随机过期时间 + 互斥锁
            4. 缓存击穿：逻辑过期 + 异步刷新
            5. 限流保护：令牌桶/漏桶算法
            """,
            "代码示例",
            new String[]{"redis", "distributed-cache", "distributed-lock", "bloom-filter", "rate-limiter"},
            "system",
            null,
            java.time.LocalDateTime.now(),
            null,
            0,
            null
        ));
        
        // 3. 消息队列 - 高级应用模式
        knowledgeBaseManager.addDocument(new KnowledgeDocument(
            "message-queue-advanced-patterns",
            "消息队列：高级应用模式（Kafka/RabbitMQ 可靠性、延迟队列、顺序消息）",
            """
            【消息队列高级应用模式完整示例】
            
            【1. 消息可靠性保证】
            
            // 生产者配置 application.yml
            spring:
              rabbitmq:
                publisher-confirm-type: correlated  # 开启确认模式
                publisher-returns: true             # 开启返回模式
                template:
                  mandatory: true                   # 强制返回
            
            // 生产者确认
            @Component
            @RequiredArgsConstructor
            public class ReliableMessageSender {
                private final RabbitTemplate rabbitTemplate;
                
                @PostConstruct
                public void init() {
                    // 确认回调
                    rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
                        if (!ack) {
                            log.error("消息发送失败: {}", cause);
                            // 重试或记录失败消息
                        } else {
                            log.info("消息发送成功: {}", correlationData);
                        }
                    });
                    
                    // 返回回调（消息未路由到队列）
                    rabbitTemplate.setReturnsCallback(returned -> {
                        log.error("消息未路由: {}", returned);
                        // 处理未路由消息
                    });
                }
                
                public void send(String exchange, String routingKey, Object message) {
                    CorrelationData correlationData = new CorrelationData(
                        UUID.randomUUID().toString()
                    );
                    rabbitTemplate.convertAndSend(exchange, routingKey, message, correlationData);
                }
            }
            
            【2. 消费者手动确认】
            
            // 消费者配置 application.yml
            spring:
              rabbitmq:
                listener:
                  simple:
                    acknowledge-mode: manual  # 手动确认
                    prefetch: 10              # 预取数量
            
            // 消费者实现
            @Component
            @RequiredArgsConstructor
            public class OrderMessageConsumer {
                private final OrderService orderService;
                
                @RabbitListener(queues = "${rabbitmq.queues.order-created}")
                public void handleMessage(Message message, Channel channel) throws IOException {
                    long deliveryTag = message.getMessageProperties().getDeliveryTag();
                    
                    try {
                        // 反序列化消息
                        OrderMessage orderMsg = JsonUtils.parse(
                            new String(message.getBody()), OrderMessage.class
                        );
                        
                        // 处理消息（幂等性检查）
                        if (orderService.isProcessed(orderMsg.getOrderId())) {
                            log.info("消息已处理，跳过: {}", orderMsg.getOrderId());
                            channel.basicAck(deliveryTag, false);
                            return;
                        }
                        
                        // 业务处理
                        orderService.processOrder(orderMsg);
                        
                        // 手动确认
                        channel.basicAck(deliveryTag, false);
                        
                    } catch (Exception e) {
                        log.error("消息处理失败", e);
                        
                        // 判断是否重试
                        Integer retryCount = getRetryCount(message);
                        if (retryCount < 3) {
                            // 重新入队
                            channel.basicNack(deliveryTag, false, true);
                        } else {
                            // 发送到死信队列
                            channel.basicNack(deliveryTag, false, false);
                            sendToDeadLetter(message, e);
                        }
                    }
                }
            }
            
            【3. 延迟队列】
            
            // 使用死信队列实现延迟
            @Configuration
            public class DelayQueueConfig {
                
                // 普通队列（设置 TTL 和死信交换机）
                @Bean
                public Queue delayQueue() {
                    return QueueBuilder.durable("order.delay.queue")
                        .withArgument("x-dead-letter-exchange", "order.dlx.exchange")
                        .withArgument("x-dead-letter-routing-key", "order.timeout")
                        .withArgument("x-message-ttl", 1800000) // 30分钟
                        .build();
                }
                
                // 死信队列
                @Bean
                public Queue deadLetterQueue() {
                    return QueueBuilder.durable("order.timeout.queue").build();
                }
            }
            
            // 发送延迟消息
            public void sendDelayMessage(OrderMessage message) {
                rabbitTemplate.convertAndSend(
                    "order.delay.exchange", "order.delay", message
                    // 消息会自动在 30 分钟后转发到死信队列
                );
            }
            
            // 监听超时订单
            @RabbitListener(queues = "order.timeout.queue")
            public void handleTimeoutOrder(OrderMessage message) {
                if (orderService.isPending(message.getOrderId())) {
                    orderService.cancelOrder(message.getOrderId());
                }
            }
            
            【4. 消息顺序性保证】
            
            // Kafka 顺序消息
            @Service
            public class OrderedMessageProducer {
                private final KafkaTemplate<String, OrderEvent> kafkaTemplate;
                
                public void sendOrderEvent(OrderEvent event) {
                    // 使用订单ID作为 key，保证同一订单的消息顺序
                    ProducerRecord<String, OrderEvent> record = new ProducerRecord<>(
                        "order-events",
                        event.getOrderId(), // key - 保证分区一致
                        event
                    );
                    kafkaTemplate.send(record);
                }
            }
            
            // 顺序消费
            @KafkaListener(topics = "order-events", concurrency = "1")
            public void consumeOrderEvent(OrderEvent event) {
                // 按顺序处理订单事件
                orderEventProcessor.process(event);
            }
            
            【关键要点】
            1. 可靠性：生产者确认 + 消费者手动确认 + 持久化
            2. 幂等性：唯一消息 ID + 去重表
            3. 顺序性：相同 key 路由到同一分区 + 单线程消费
            4. 延迟处理：死信队列 / 延迟插件
            5. 死信处理：失败消息转移到死信队列人工处理
            """,
            "代码示例",
            new String[]{"message-queue", "kafka", "rabbitmq", "reliability", "delay-queue"},
            "system",
            null,
            java.time.LocalDateTime.now(),
            null,
            0,
            null
        ));
        
        log.info("技术架构模范代码知识库加载完成");
    }
}
