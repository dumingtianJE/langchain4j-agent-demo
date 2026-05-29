package com.yourcompany.langchain4j.knowledge;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * 代码生成最佳实践知识库增强
 * 在系统启动时加载高质量的代码示例和最佳实践
 */
@Slf4j
@Component
public class CodeGenerationBestPractices implements CommandLineRunner {
    
    private final KnowledgeBaseManager knowledgeBaseManager;
    
    public CodeGenerationBestPractices(KnowledgeBaseManager knowledgeBaseManager) {
        this.knowledgeBaseManager = knowledgeBaseManager;
    }
    
    @Override
    public void run(String... args) {
        loadCodeGenerationPractices();
    }
    
    private void loadCodeGenerationPractices() {
        // 1. Spring Boot REST API 最佳实践
        knowledgeBaseManager.addDocument(new KnowledgeDocument(
            "springboot-rest-api-best-practices",
            "Spring Boot REST API 完整示例与最佳实践",
            """
            【REST API 设计最佳实践】
            
            ## 1. Controller 层标准示例
            
            ```java
            @RestController
            @RequestMapping("/api/users")
            @RequiredArgsConstructor
            @Validated
            public class UserController {
                
                private final UserService userService;
                
                @GetMapping("/{id}")
                public ResponseEntity<UserDTO> getUserById(
                        @PathVariable @Positive Long id) {
                    return userService.findById(id)
                        .map(ResponseEntity::ok)
                        .orElse(ResponseEntity.notFound().build());
                }
                
                @PostMapping
                @ResponseStatus(HttpStatus.CREATED)
                public UserDTO createUser(@Valid @RequestBody CreateUserRequest request) {
                    return userService.create(request);
                }
                
                @PutMapping("/{id}")
                public UserDTO updateUser(
                        @PathVariable @Positive Long id,
                        @Valid @RequestBody UpdateUserRequest request) {
                    return userService.update(id, request);
                }
                
                @DeleteMapping("/{id}")
                @ResponseStatus(HttpStatus.NO_CONTENT)
                public void deleteUser(@PathVariable @Positive Long id) {
                    userService.delete(id);
                }
            }
            ```
            
            ## 2. Service 层事务管理
            
            ```java
            @Service
            @RequiredArgsConstructor
            @Transactional(readOnly = true)
            public class UserService {
                
                private final UserRepository userRepository;
                
                @Transactional
                public UserDTO create(CreateUserRequest request) {
                    // 1. 业务校验
                    if (userRepository.existsByEmail(request.email())) {
                        throw new BusinessRuleException("邮箱已存在");
                    }
                    
                    // 2. 实体转换
                    User user = User.builder()
                        .email(request.email())
                        .name(request.name())
                        .createdAt(LocalDateTime.now())
                        .build();
                    
                    // 3. 持久化
                    User savedUser = userRepository.save(user);
                    
                    // 4. 返回 DTO
                    return UserDTO.fromEntity(savedUser);
                }
            }
            ```
            
            ## 3. 统一异常处理
            
            ```java
            @RestControllerAdvice
            public class GlobalExceptionHandler {
                
                @ExceptionHandler(ResourceNotFoundException.class)
                public ResponseEntity<ErrorResponse> handleNotFound(
                        ResourceNotFoundException ex) {
                    ErrorResponse error = ErrorResponse.builder()
                        .timestamp(LocalDateTime.now())
                        .status(HttpStatus.NOT_FOUND.value())
                        .error("Not Found")
                        .message(ex.getMessage())
                        .build();
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
                }
                
                @ExceptionHandler(MethodArgumentNotValidException.class)
                public ResponseEntity<ErrorResponse> handleValidation(
                        MethodArgumentNotValidException ex) {
                    String errors = ex.getBindingResult()
                        .getFieldErrors()
                        .stream()
                        .map(FieldError::getDefaultMessage)
                        .collect(Collectors.joining(", "));
                    
                    ErrorResponse error = ErrorResponse.builder()
                        .timestamp(LocalDateTime.now())
                        .status(HttpStatus.BAD_REQUEST.value())
                        .error("Validation Failed")
                        .message(errors)
                        .build();
                    return ResponseEntity.badRequest().body(error);
                }
            }
            ```
            
            ## 4. 关键要点
            
            1. **使用 DTO 模式**：不要直接暴露实体类
            2. **参数校验**：使用 @Valid 和 Bean Validation
            3. **统一响应格式**：使用 ResponseEntity 包装
            4. **异常处理**：使用 @RestControllerAdvice 统一处理
            5. **事务管理**：在 Service 层使用 @Transactional
            6. **日志记录**：关键操作记录日志
            7. **分页查询**：使用 Spring Data 的 Pageable
            """,
            "代码示例",
            new String[]{"spring-boot", "rest-api", "best-practices", "controller", "service"},
            "system",
            null,
            java.time.LocalDateTime.now(),
            null,
            0,
            null
        ));
        
        // 2. 设计模式应用示例
        knowledgeBaseManager.addDocument(new KnowledgeDocument(
            "design-patterns-java-examples",
            "Java 常用设计模式完整示例",
            """
            【常用设计模式 Java 实现】
            
            ## 1. 策略模式（Strategy Pattern）
            
            ```java
            // 策略接口
            public interface PaymentStrategy {
                void pay(BigDecimal amount);
            }
            
            // 具体策略
            @Component
            public class CreditCardPayment implements PaymentStrategy {
                @Override
                public void pay(BigDecimal amount) {
                    // 信用卡支付逻辑
                }
            }
            
            @Component
            public class AlipayPayment implements PaymentStrategy {
                @Override
                public void pay(BigDecimal amount) {
                    // 支付宝支付逻辑
                }
            }
            
            // 上下文
            @Service
            @RequiredArgsConstructor
            public class PaymentService {
                private final Map<String, PaymentStrategy> strategies;
                
                public void processPayment(String type, BigDecimal amount) {
                    PaymentStrategy strategy = strategies.get(type);
                    if (strategy == null) {
                        throw new IllegalArgumentException("不支持的支付方式: " + type);
                    }
                    strategy.pay(amount);
                }
            }
            ```
            
            ## 2. 建造者模式（Builder Pattern）
            
            ```java
            @Builder
            public class User {
                private final Long id;
                private final String email;
                private final String name;
                private final LocalDateTime createdAt;
                
                public static class UserBuilder {
                    public UserBuilder email(String email) {
                        if (!email.contains("@")) {
                            throw new IllegalArgumentException("无效邮箱");
                        }
                        this.email = email;
                        return this;
                    }
                }
            }
            
            // 使用
            User user = User.builder()
                .email("user@example.com")
                .name("张三")
                .createdAt(LocalDateTime.now())
                .build();
            ```
            
            ## 3. 观察者模式（Observer Pattern）
            
            ```java
            // 使用 Spring Events
            @Component
            @RequiredArgsConstructor
            public class OrderService {
                private final ApplicationEventPublisher eventPublisher;
                
                @Transactional
                public Order createOrder(CreateOrderRequest request) {
                    Order order = orderRepository.save(request.toEntity());
                    
                    // 发布事件
                    eventPublisher.publishEvent(new OrderCreatedEvent(this, order));
                    
                    return order;
                }
            }
            
            // 事件监听器
            @Component
            public class OrderEventListener {
                
                @Async
                @EventListener
                public void handleOrderCreated(OrderCreatedEvent event) {
                    // 发送通知邮件
                    // 更新库存
                    // 发送短信通知
                }
            }
            ```
            
            ## 使用场景
            
            - **策略模式**：多种算法可互换（支付方式、排序算法）
            - **建造者模式**：对象构建复杂，参数多（配置对象、DTO）
            - **观察者模式**：事件驱动架构（订单创建、用户注册）
            """,
            "代码示例",
            new String[]{"design-patterns", "strategy", "builder", "observer", "java"},
            "system",
            null,
            java.time.LocalDateTime.now(),
            null,
            0,
            null
        ));
        
        // 3. 数据库操作最佳实践
        knowledgeBaseManager.addDocument(new KnowledgeDocument(
            "database-operations-best-practices",
            "数据库操作最佳实践与性能优化",
            """
            【数据库操作最佳实践】
            
            ## 1. Repository 层设计
            
            ```java
            public interface UserRepository extends JpaRepository<User, Long>, 
                                                  JpaSpecificationExecutor<User> {
                
                // 方法命名查询
                Optional<User> findByEmail(String email);
                List<User> findByStatusAndCreatedAtBetween(
                    UserStatus status, LocalDateTime start, LocalDateTime end);
                
                // JPQL 查询
                @Query("SELECT u FROM User u WHERE u.status = :status ORDER BY u.createdAt DESC")
                Page<User> findActiveUsers(@Param("status") UserStatus status, Pageable pageable);
                
                // 原生 SQL
                @Query(value = "SELECT * FROM users WHERE email LIKE %:keyword%", 
                       nativeQuery = true)
                List<User> searchByEmail(@Param("keyword") String keyword);
                
                // 更新操作
                @Modifying
                @Transactional
                @Query("UPDATE User u SET u.status = :status WHERE u.id = :id")
                int updateStatus(@Param("id") Long id, @Param("status") UserStatus status);
            }
            ```
            
            ## 2. 性能优化技巧
            
            ### 避免 N+1 查询问题
            ```java
            // ❌ 错误示例：导致 N+1 查询
            List<Order> orders = orderRepository.findAll();
            for (Order order : orders) {
                User user = order.getUser(); // 每次都查询数据库
            }
            
            // ✅ 正确示例：使用 JOIN FETCH
            @Query("SELECT o FROM Order o JOIN FETCH o.user")
            List<Order> findAllWithUser();
            ```
            
            ### 使用投影减少数据传输
            ```java
            // 接口投影
            public interface UserSummary {
                Long getId();
                String getName();
                String getEmail();
            }
            
            @Query("SELECT u.id as id, u.name as name, u.email as email FROM User u")
            List<UserSummary> findAllSummary();
            ```
            
            ## 3. 批量操作
            
            ```java
            @Service
            @RequiredArgsConstructor
            public class BatchService {
                
                private final EntityManager entityManager;
                
                @Transactional
                public void batchInsert(List<User> users) {
                    int batchSize = 50;
                    for (int i = 0; i < users.size(); i++) {
                        entityManager.persist(users.get(i));
                        
                        if (i % batchSize == 0 && i > 0) {
                            entityManager.flush();
                            entityManager.clear();
                        }
                    }
                }
            }
            ```
            
            ## 4. 配置优化
            
            application.yml:
            ```yaml
            spring:
              jpa:
                properties:
                  hibernate:
                    jdbc:
                      batch_size: 50
                    order_inserts: true
                    order_updates: true
                    generate_statistics: false
                show-sql: false
            ```
            """,
            "代码示例",
            new String[]{"database", "jpa", "performance", "optimization", "spring-data"},
            "system",
            null,
            java.time.LocalDateTime.now(),
            null,
            0,
            null
        ));
        
        log.info("代码生成最佳实践知识库加载完成");
    }
}
