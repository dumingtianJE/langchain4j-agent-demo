package com.yourcompany.langchain4j.knowledge;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * 测试架构模范代码
 * 补充单元测试、集成测试、性能测试、E2E 测试示例
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TestArchitectureKnowledge implements CommandLineRunner {
    
    private final KnowledgeBaseManager knowledgeBaseManager;
    
    @Override
    public void run(String... args) {
        loadTestArchitecturePatterns();
    }
    
    private void loadTestArchitecturePatterns() {
        // 1. 单元测试完整示例
        knowledgeBaseManager.addDocument(new KnowledgeDocument(
            "unit-testing-patterns",
            "单元测试：JUnit 5 + Mockito 完整示例",
            """
            【单元测试最佳实践】
            
            ## 1. Service 层单元测试
            
            \`\`\`java
            @ExtendWith(MockitoExtension.class)
            class OrderServiceTest {
                
                @Mock
                private OrderRepository orderRepository;
                
                @Mock
                private PaymentService paymentService;
                
                @Mock
                private InventoryService inventoryService;
                
                @InjectMocks
                private OrderService orderService;
                
                @Test
                @DisplayName("创建订单 - 成功场景")
                void createOrder_Success() {
                    // Given
                    CreateOrderRequest request = CreateOrderRequest.builder()
                        .userId(1L)
                        .productId(100L)
                        .quantity(2)
                        .build();
                    
                    Product product = Product.builder()
                        .id(100L)
                        .name("Test Product")
                        .price(new BigDecimal("99.99"))
                        .stock(10)
                        .build();
                    
                    when(inventoryService.checkStock(100L, 2)).thenReturn(true);
                    when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
                        Order order = invocation.getArgument(0);
                        order.setId(1L);
                        return order;
                    });
                    
                    // When
                    OrderDTO result = orderService.createOrder(request);
                    
                    // Then
                    assertThat(result).isNotNull();
                    assertThat(result.getId()).isEqualTo(1L);
                    assertThat(result.getUserId()).isEqualTo(1L);
                    assertThat(result.getTotalAmount()).isEqualTo(new BigDecimal("199.98"));
                    assertThat(result.getStatus()).isEqualTo(OrderStatus.PENDING);
                    
                    verify(orderRepository).save(any(Order.class));
                    verify(inventoryService).checkStock(100L, 2);
                }
                
                @Test
                @DisplayName("创建订单 - 库存不足")
                void createOrder_InsufficientStock() {
                    // Given
                    CreateOrderRequest request = CreateOrderRequest.builder()
                        .userId(1L)
                        .productId(100L)
                        .quantity(100)
                        .build();
                    
                    when(inventoryService.checkStock(100L, 100)).thenReturn(false);
                    
                    // When & Then
                    assertThatThrownBy(() -> orderService.createOrder(request))
                        .isInstanceOf(BusinessException.class)
                        .hasMessageContaining("库存不足");
                    
                    verify(orderRepository, never()).save(any());
                }
                
                @Test
                @DisplayName("批量查询订单 - 分页")
                void getOrders_Pagination() {
                    // Given
                    Long userId = 1L;
                    Pageable pageable = PageRequest.of(0, 10);
                    
                    List<Order> orders = IntStream.range(1, 11)
                        .mapToObj(i -> Order.builder()
                            .id((long) i)
                            .userId(userId)
                            .build())
                        .collect(Collectors.toList());
                    
                    Page<Order> orderPage = new PageImpl<>(orders, pageable, 50);
                    when(orderRepository.findByUserId(eq(userId), any(Pageable.class)))
                        .thenReturn(orderPage);
                    
                    // When
                    Page<OrderDTO> result = orderService.getOrders(userId, pageable);
                    
                    // Then
                    assertThat(result).isNotNull();
                    assertThat(result.getTotalElements()).isEqualTo(50);
                    assertThat(result.getContent()).hasSize(10);
                    assertThat(result.getContent().get(0).getUserId()).isEqualTo(userId);
                }
                
                @Test
                @DisplayName("取消订单 - 已支付订单需要退款")
                void cancelOrder_WithRefund() {
                    // Given
                    Long orderId = 1L;
                    Order order = Order.builder()
                        .id(orderId)
                        .status(OrderStatus.PAID)
                        .totalAmount(new BigDecimal("199.98"))
                        .build();
                    
                    when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
                    when(paymentService.refund(any())).thenReturn(true);
                    
                    // When
                    orderService.cancelOrder(orderId);
                    
                    // Then
                    assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
                    verify(paymentService).refund(any(RefundRequest.class));
                    verify(orderRepository).save(order);
                }
            }
            \`\`\`
            
            ## 2. Controller 层单元测试
            
            \`\`\`java
            @WebMvcTest(OrderController.class)
            class OrderControllerTest {
                
                @Autowired
                private MockMvc mockMvc;
                
                @MockBean
                private OrderService orderService;
                
                @Autowired
                private ObjectMapper objectMapper;
                
                @Test
                @DisplayName("GET /api/orders/{id} - 成功")
                void getOrder_Success() throws Exception {
                    // Given
                    Long orderId = 1L;
                    OrderDTO orderDTO = OrderDTO.builder()
                        .id(orderId)
                        .userId(1L)
                        .totalAmount(new BigDecimal("199.98"))
                        .status(OrderStatus.PENDING)
                        .build();
                    
                    when(orderService.getOrder(orderId)).thenReturn(orderDTO);
                    
                    // When & Then
                    mockMvc.perform(get("/api/orders/{id}", orderId))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.id").value(orderId))
                        .andExpect(jsonPath("$.userId").value(1L))
                        .andExpect(jsonPath("$.totalAmount").value(199.98))
                        .andExpect(jsonPath("$.status").value("PENDING"));
                }
                
                @Test
                @DisplayName("POST /api/orders - 参数校验失败")
                void createOrder_ValidationFailed() throws Exception {
                    // Given
                    CreateOrderRequest request = CreateOrderRequest.builder()
                        .userId(null)  // 必填字段为空
                        .productId(null)
                        .quantity(0)  // 数量必须大于 0
                        .build();
                    
                    // When & Then
                    mockMvc.perform(post("/api/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.errors").isArray())
                        .andExpect(jsonPath("$.errors.length()").value(3));
                }
                
                @Test
                @DisplayName("PUT /api/orders/{id}/status - 权限不足")
                void updateOrderStatus_Forbidden() throws Exception {
                    // Given
                    Long orderId = 1L;
                    UpdateStatusRequest request = new UpdateStatusRequest("SHIPPED");
                    
                    when(orderService.updateStatus(eq(orderId), any()))
                        .thenThrow(new AccessDeniedException("权限不足"));
                    
                    // When & Then
                    mockMvc.perform(put("/api/orders/{id}/status", orderId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isForbidden());
                }
            }
            \`\`\`
            
            ## 3. Repository 层测试
            
            \`\`\`java
            @DataJpaTest
            @AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
            class OrderRepositoryTest {
                
                @Autowired
                private TestEntityManager entityManager;
                
                @Autowired
                private OrderRepository orderRepository;
                
                @Test
                @DisplayName("根据用户 ID 查询订单")
                void findByUserId() {
                    // Given
                    User user = new User();
                    user.setUsername("testuser");
                    entityManager.persist(user);
                    
                    Order order1 = new Order();
                    order1.setUser(user);
                    order1.setTotalAmount(new BigDecimal("100.00"));
                    entityManager.persist(order1);
                    
                    Order order2 = new Order();
                    order2.setUser(user);
                    order2.setTotalAmount(new BigDecimal("200.00"));
                    entityManager.persist(order2);
                    
                    entityManager.flush();
                    
                    // When
                    List<Order> orders = orderRepository.findByUserId(user.getId());
                    
                    // Then
                    assertThat(orders).hasSize(2);
                    assertThat(orders).extracting("totalAmount")
                        .containsExactlyInAnyOrder(
                            new BigDecimal("100.00"),
                            new BigDecimal("200.00")
                        );
                }
                
                @Test
                @DisplayName("分页查询订单")
                void findByUserIdWithPagination() {
                    // Given
                    User user = new User();
                    entityManager.persist(user);
                    
                    for (int i = 0; i < 50; i++) {
                        Order order = new Order();
                        order.setUser(user);
                        entityManager.persist(order);
                    }
                    entityManager.flush();
                    
                    // When
                    Pageable pageable = PageRequest.of(0, 10);
                    Page<Order> page = orderRepository.findByUserId(user.getId(), pageable);
                    
                    // Then
                    assertThat(page.getTotalElements()).isEqualTo(50);
                    assertThat(page.getTotalPages()).isEqualTo(5);
                    assertThat(page.getContent()).hasSize(10);
                }
            }
            \`\`\`
            
            ## 关键要点
            
            1. **AAA 模式**：Arrange-Act-Assert
            2. **Mock 外部依赖**：Repository、Service
            3. **边界测试**：正常、异常、边界值
            4. **参数校验**：验证请求参数
            5. **隔离测试**：不依赖外部环境
            """,
            "代码示例",
            new String[]{"unit-test", "junit5", "mockito", "testing", "tdd"},
            "system",
            null,
            java.time.LocalDateTime.now(),
            null,
            0,
            null
        ));
        
        // 2. 集成测试
        knowledgeBaseManager.addDocument(new KnowledgeDocument(
            "integration-testing-patterns",
            "集成测试：Testcontainers + Spring Boot",
            """
            【集成测试最佳实践】
            
            ## 1. Testcontainers 配置
            
            \`\`\`java
            @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
            @TestPropertySource(properties = {
                "spring.datasource.url=jdbc:tc:mysql:8.0:///testdb",
                "spring.redis.host=localhost",
                "spring.rabbitmq.host=localhost"
            })
            @TestMethodInstance(Lifecycle.PER_CLASS)
            class OrderIntegrationTest {
                
                @LocalServerPort
                private int port;
                
                @Autowired
                private TestRestTemplate restTemplate;
                
                @Autowired
                private OrderRepository orderRepository;
                
                @BeforeEach
                void setUp() {
                    orderRepository.deleteAll();
                }
                
                @Test
                @DisplayName("完整订单流程：创建 -> 支付 -> 发货")
                void completeOrderFlow() {
                    // 1. 创建订单
                    CreateOrderRequest createRequest = new CreateOrderRequest(1L, 100L, 2);
                    ResponseEntity<OrderDTO> createResponse = restTemplate.postForEntity(
                        "http://localhost:" + port + "/api/orders",
                        createRequest,
                        OrderDTO.class
                    );
                    
                    assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
                    OrderDTO order = createResponse.getBody();
                    assertThat(order).isNotNull();
                    assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
                    
                    // 2. 支付订单
                    PaymentRequest paymentRequest = new PaymentRequest("CREDIT_CARD");
                    ResponseEntity<PaymentResult> paymentResponse = restTemplate.postForEntity(
                        "http://localhost:" + port + "/api/orders/" + order.getId() + "/pay",
                        paymentRequest,
                        PaymentResult.class
                    );
                    
                    assertThat(paymentResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
                    
                    // 3. 验证订单状态
                    ResponseEntity<OrderDTO> orderResponse = restTemplate.getForEntity(
                        "http://localhost:" + port + "/api/orders/" + order.getId(),
                        OrderDTO.class
                    );
                    
                    assertThat(orderResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
                    assertThat(orderResponse.getBody().getStatus()).isEqualTo(OrderStatus.PAID);
                    
                    // 4. 发货
                    ResponseEntity<Void> shipResponse = restTemplate.exchange(
                        "http://localhost:" + port + "/api/orders/" + order.getId() + "/ship",
                        HttpMethod.PUT,
                        null,
                        Void.class
                    );
                    
                    assertThat(shipResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
                    
                    // 5. 最终验证
                    OrderDTO finalOrder = restTemplate.getForObject(
                        "http://localhost:" + port + "/api/orders/" + order.getId(),
                        OrderDTO.class
                    );
                    
                    assertThat(finalOrder.getStatus()).isEqualTo(OrderStatus.SHIPPED);
                }
            }
            \`\`\`
            
            ## 2. 数据库集成测试
            
            \`\`\`java
            @Testcontainers
            @SpringBootTest
            class DatabaseIntegrationTest {
                
                @Container
                static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
                    .withDatabaseName("testdb")
                    .withUsername("test")
                    .withPassword("test");
                
                @DynamicPropertySource
                static void postgresProperties(DynamicPropertyRegistry registry) {
                    registry.add("spring.datasource.url", postgres::getJdbcUrl);
                    registry.add("spring.datasource.username", postgres::getUsername);
                    registry.add("spring.datasource.password", postgres::getPassword);
                }
                
                @Autowired
                private OrderService orderService;
                
                @Test
                @DisplayName("订单 CRUD 操作")
                void orderCRUDOperations() {
                    // Create
                    OrderDTO created = orderService.createOrder(new CreateOrderRequest(1L, 100L, 1));
                    assertThat(created.getId()).isNotNull();
                    
                    // Read
                    OrderDTO found = orderService.getOrder(created.getId());
                    assertThat(found).isNotNull();
                    assertThat(found.getId()).isEqualTo(created.getId());
                    
                    // Update
                    orderService.updateStatus(created.getId(), OrderStatus.PAID);
                    OrderDTO updated = orderService.getOrder(created.getId());
                    assertThat(updated.getStatus()).isEqualTo(OrderStatus.PAID);
                    
                    // Delete
                    orderService.deleteOrder(created.getId());
                    assertThatThrownBy(() -> orderService.getOrder(created.getId()))
                        .isInstanceOf(ResourceNotFoundException.class);
                }
            }
            \`\`\`
            
            ## 3. Redis 集成测试
            
            \`\`\`java
            @Testcontainers
            @SpringBootTest
            class RedisIntegrationTest {
                
                @Container
                static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
                    .withExposedPorts(6379);
                
                @DynamicPropertySource
                static void redisProperties(DynamicPropertyRegistry registry) {
                    registry.add("spring.redis.host", redis::getHost);
                    registry.add("spring.redis.port", () -> redis.getMappedPort(6379));
                }
                
                @Autowired
                private StringRedisTemplate redisTemplate;
                
                @Test
                @DisplayName("缓存操作测试")
                void cacheOperations() {
                    // Set
                    redisTemplate.opsForValue().set("test:key", "test:value", 10, TimeUnit.SECONDS);
                    
                    // Get
                    String value = redisTemplate.opsForValue().get("test:key");
                    assertThat(value).isEqualTo("test:value");
                    
                    // Delete
                    redisTemplate.delete("test:key");
                    assertThat(redisTemplate.hasKey("test:key")).isFalse();
                }
            }
            \`\`\`
            
            ## 4. 性能测试
            
            \`\`\`java
            @SpringBootTest
            class PerformanceTest {
                
                @Autowired
                private OrderService orderService;
                
                @Test
                @DisplayName("批量创建订单性能测试")
                void batchCreateOrderPerformance() {
                    int totalRequests = 1000;
                    int concurrentThreads = 10;
                    
                    ExecutorService executor = Executors.newFixedThreadPool(concurrentThreads);
                    CountDownLatch latch = new CountDownLatch(totalRequests);
                    
                    long startTime = System.currentTimeMillis();
                    
                    for (int i = 0; i < totalRequests; i++) {
                        executor.submit(() -> {
                            try {
                                CreateOrderRequest request = new CreateOrderRequest(
                                    ThreadLocalRandom.current().nextLong(1, 1000),
                                    100L,
                                    1
                                );
                                orderService.createOrder(request);
                            } finally {
                                latch.countDown();
                            }
                        });
                    }
                    
                    latch.await();
                    long endTime = System.currentTimeMillis();
                    
                    long duration = endTime - startTime;
                    double tps = (double) totalRequests / (duration / 1000.0);
                    
                    System.out.println("Total Requests: " + totalRequests);
                    System.out.println("Duration: " + duration + "ms");
                    System.out.println("TPS: " + tps);
                    
                    assertThat(duration).isLessThan(10000); // 10秒内完成
                    assertThat(tps).isGreaterThan(100);     // TPS > 100
                    
                    executor.shutdown();
                }
            }
            \`\`\`
            
            ## 关键要点
            
            1. **Testcontainers**：真实依赖环境
            2. **隔离测试**：每个测试独立
            3. **性能指标**：TPS、响应时间
            4. **并发测试**：多线程场景
            5. **清理数据**：测试前后清理
            """,
            "代码示例",
            new String[]{"integration-test", "testcontainers", "performance-test", "testing", "e2e"},
            "system",
            null,
            java.time.LocalDateTime.now(),
            null,
            0,
            null
        ));
        
        log.info("测试架构模范代码知识库加载完成");
    }
}
