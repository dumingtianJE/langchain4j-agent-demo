package com.yourcompany.langchain4j.knowledge;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 云原生和容器化架构模范代码
 * 补充 Kubernetes、Docker、服务网格等云原生技术示例
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CloudNativePatternsKnowledge implements CommandLineRunner {
    
    private final KnowledgeBaseManager knowledgeBaseManager;
    
    @Override
    public void run(String... args) {
        loadCloudNativePatterns();
    }
    
    private void loadCloudNativePatterns() {
        // 1. Kubernetes 部署配置
        knowledgeBaseManager.addDocument(new KnowledgeDocument(
            "kubernetes-deployment-patterns",
            "Kubernetes：生产级部署配置示例",
            """
            【Kubernetes 生产级部署配置】
            
            ## 1. Deployment 配置
            
            \`\`\`yaml
            apiVersion: apps/v1
            kind: Deployment
            metadata:
              name: order-service
              namespace: production
              labels:
                app: order-service
                version: v1.2.0
            spec:
              replicas: 3
              strategy:
                type: RollingUpdate
                rollingUpdate:
                  maxSurge: 1        # 最多超出期望1个Pod
                  maxUnavailable: 0  # 不允许不可用（零停机）
              selector:
                matchLabels:
                  app: order-service
              template:
                metadata:
                  labels:
                    app: order-service
                    version: v1.2.0
                spec:
                  # 容器配置
                  containers:
                  - name: order-service
                    image: registry.example.com/order-service:v1.2.0
                    ports:
                    - containerPort: 8080
                      name: http
                      protocol: TCP
                    
                    # 环境变量
                    env:
                    - name: SPRING_PROFILES_ACTIVE
                      value: "production"
                    - name: JAVA_OPTS
                      value: "-Xms512m -Xmx1024m"
                    envFrom:
                    - secretRef:
                        name: order-service-secrets
                    - configMapRef:
                        name: order-service-config
                    
                    # 资源限制
                    resources:
                      requests:
                        cpu: 250m
                        memory: 512Mi
                      limits:
                        cpu: 1000m
                        memory: 1024Mi
                    
                    # 健康检查
                    livenessProbe:
                      httpGet:
                        path: /actuator/health/liveness
                        port: 8080
                      initialDelaySeconds: 60
                      periodSeconds: 10
                      timeoutSeconds: 5
                      failureThreshold: 3
                    
                    readinessProbe:
                      httpGet:
                        path: /actuator/health/readiness
                        port: 8080
                      initialDelaySeconds: 30
                      periodSeconds: 5
                      timeoutSeconds: 3
                      failureThreshold: 3
                    
                    # 生命周期钩子
                    lifecycle:
                      preStop:
                        exec:
                          command: ["/bin/sh", "-c", "sleep 10"]
                  
                  # 镜像拉取策略
                  imagePullSecrets:
                  - name: registry-secret
                  
                  # 优雅终止
                  terminationGracePeriodSeconds: 30
            \`\`\`
            
            ## 2. Service 配置
            
            \`\`\`yaml
            apiVersion: v1
            kind: Service
            metadata:
              name: order-service
              namespace: production
            spec:
              type: ClusterIP
              selector:
                app: order-service
              ports:
              - name: http
                port: 80
                targetPort: 8080
                protocol: TCP
              sessionAffinity: None
            \`\`\`
            
            ## 3. Ingress 配置
            
            \`\`\`yaml
            apiVersion: networking.k8s.io/v1
            kind: Ingress
            metadata:
              name: order-service-ingress
              namespace: production
              annotations:
                nginx.ingress.kubernetes.io/rewrite-target: /
                nginx.ingress.kubernetes.io/ssl-redirect: "true"
                nginx.ingress.kubernetes.io/limit-rps: "100"
                nginx.ingress.kubernetes.io/proxy-body-size: "10m"
            spec:
              tls:
              - hosts:
                - api.example.com
                secretName: example-tls
              rules:
              - host: api.example.com
                http:
                  paths:
                  - path: /orders
                    pathType: Prefix
                    backend:
                      service:
                        name: order-service
                        port:
                          number: 80
            \`\`\`
            
            ## 4. Horizontal Pod Autoscaler
            
            \`\`\`yaml
            apiVersion: autoscaling/v2
            kind: HorizontalPodAutoscaler
            metadata:
              name: order-service-hpa
              namespace: production
            spec:
              scaleTargetRef:
                apiVersion: apps/v1
                kind: Deployment
                name: order-service
              minReplicas: 3
              maxReplicas: 20
              metrics:
              - type: Resource
                resource:
                  name: cpu
                  target:
                    type: Utilization
                    averageUtilization: 70
              - type: Resource
                resource:
                  name: memory
                  target:
                    type: Utilization
                    averageUtilization: 80
              - type: Pods
                pods:
                  metric:
                    name: http_requests_per_second
                  target:
                    type: AverageValue
                    averageValue: 1000
              behavior:
                scaleUp:
                  stabilizationWindowSeconds: 60
                  policies:
                  - type: Pods
                    value: 2
                    periodSeconds: 60
                scaleDown:
                  stabilizationWindowSeconds: 300
                  policies:
                  - type: Pods
                    value: 1
                    periodSeconds: 120
            \`\`\`
            
            ## 5. ConfigMap 和 Secret
            
            \`\`\`yaml
            # ConfigMap
            apiVersion: v1
            kind: ConfigMap
            metadata:
              name: order-service-config
              namespace: production
            data:
              application.yml: |
                server:
                  port: 8080
                spring:
                  datasource:
                    url: jdbc:mysql://mysql-service:3306/order_db
                    username: ${DB_USERNAME}
                    password: ${DB_PASSWORD}
                  redis:
                    host: redis-service
                    port: 6379
                rabbitmq:
                  host: rabbitmq-service
                  port: 5672
                logging:
                  level:
                    com.yourcompany: INFO
            
            # Secret
            apiVersion: v1
            kind: Secret
            metadata:
              name: order-service-secrets
              namespace: production
            type: Opaque
            data:
              DB_USERNAME: YWRtaW4=  # base64 encoded
              DB_PASSWORD: cGFzc3dvcmQxMjM=
              REDIS_PASSWORD: cmVkaXMxMjM=
            \`\`\`
            
            ## 关键要点
            
            1. **零停机部署**：RollingUpdate + readinessProbe
            2. **自动扩缩容**：HPA 基于 CPU/内存/自定义指标
            3. **资源管理**：requests 和 limits 合理配置
            4. **配置分离**：ConfigMap + Secret 管理配置
            5. **健康检查**：liveness + readiness 双重探针
            """,
            "配置示例",
            new String[]{"kubernetes", "deployment", "hpa", "ingress", "cloud-native"},
            "system",
            null,
            java.time.LocalDateTime.now(),
            null,
            0,
            null
        ));
        
        // 2. Docker 多阶段构建
        knowledgeBaseManager.addDocument(new KnowledgeDocument(
            "docker-multistage-build",
            "Docker：多阶段构建优化实践",
            """
            【Docker 多阶段构建优化】
            
            ## 1. Spring Boot 多阶段构建
            
            \`\`\`dockerfile
            # 第一阶段：构建
            FROM maven:3.9-eclipse-temurin-21 AS builder
            WORKDIR /app
            
            # 复制依赖文件（利用 Docker 缓存）
            COPY pom.xml .
            RUN mvn dependency:go-offline -B
            
            # 复制源代码
            COPY src ./src
            RUN mvn package -DskipTests
            
            # 第二阶段：运行
            FROM eclipse-temurin:21-jre-alpine
            WORKDIR /app
            
            # 创建非 root 用户
            RUN addgroup -S spring && adduser -S spring -G spring
            USER spring:spring
            
            # 复制构建产物
            COPY --from=builder /app/target/*.jar app.jar
            
            # JVM 参数
            ENV JAVA_OPTS="-Xms512m -Xmx1024m \
                          -XX:+UseG1GC \
                          -XX:MaxGCPauseMillis=200 \
                          -XX:+UseContainerSupport \
                          -XX:MaxRAMPercentage=75.0"
            
            # 健康检查
            HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \\
              CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1
            
            # 暴露端口
            EXPOSE 8080
            
            # 启动命令
            ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
            \`\`\`
            
            ## 2. 前端项目多阶段构建
            
            \`\`\`dockerfile
            # 第一阶段：构建
            FROM node:20-alpine AS builder
            WORKDIR /app
            
            # 复制依赖文件
            COPY package*.json ./
            RUN npm ci --only=production
            
            # 复制源代码
            COPY . .
            RUN npm run build
            
            # 第二阶段：Nginx 托管
            FROM nginx:alpine
            
            # 复制构建产物
            COPY --from=builder /app/dist /usr/share/nginx/html
            
            # 复制 Nginx 配置
            COPY nginx.conf /etc/nginx/conf.d/default.conf
            
            EXPOSE 80
            
            CMD ["nginx", "-g", "daemon off;"]
            \`\`\`
            
            ## 3. Docker Compose 开发环境
            
            \`\`\`yaml
            version: '3.8'
            
            services:
              # 应用服务
              order-service:
                build:
                  context: .
                  dockerfile: Dockerfile
                ports:
                  - "8080:8080"
                environment:
                  - SPRING_PROFILES_ACTIVE=dev
                  - SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/order_db
                  - SPRING_REDIS_HOST=redis
                  - SPRING_RABBITMQ_HOST=rabbitmq
                depends_on:
                  mysql:
                    condition: service_healthy
                  redis:
                    condition: service_healthy
                  rabbitmq:
                    condition: service_healthy
                volumes:
                  - ./logs:/app/logs
                networks:
                  - app-network
            
              # MySQL
              mysql:
                image: mysql:8.0
                environment:
                  MYSQL_ROOT_PASSWORD: root
                  MYSQL_DATABASE: order_db
                  MYSQL_USER: app
                  MYSQL_PASSWORD: app123
                ports:
                  - "3306:3306"
                volumes:
                  - mysql-data:/var/lib/mysql
                  - ./init.sql:/docker-entrypoint-initdb.d/init.sql
                healthcheck:
                  test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
                  interval: 10s
                  timeout: 5s
                  retries: 5
                networks:
                  - app-network
            
              # Redis
              redis:
                image: redis:7-alpine
                ports:
                  - "6379:6379"
                volumes:
                  - redis-data:/data
                command: redis-server --appendonly yes
                healthcheck:
                  test: ["CMD", "redis-cli", "ping"]
                  interval: 10s
                  timeout: 5s
                  retries: 5
                networks:
                  - app-network
            
              # RabbitMQ
              rabbitmq:
                image: rabbitmq:3-management
                ports:
                  - "5672:5672"
                  - "15672:15672"
                environment:
                  RABBITMQ_DEFAULT_USER: guest
                  RABBITMQ_DEFAULT_PASS: guest
                volumes:
                  - rabbitmq-data:/var/lib/rabbitmq
                healthcheck:
                  test: ["CMD", "rabbitmq-diagnostics", "check_port_connectivity"]
                  interval: 10s
                  timeout: 5s
                  retries: 5
                networks:
                  - app-network
            
            volumes:
              mysql-data:
              redis-data:
              rabbitmq-data:
            
            networks:
              app-network:
                driver: bridge
            \`\`\`
            
            ## 4. 镜像优化技巧
            
            \`\`\`dockerfile
            # 1. 使用轻量级基础镜像
            FROM eclipse-temurin:21-jre-alpine  # ~100MB
            # 而不是
            FROM eclipse-temurin:21             # ~400MB
            
            # 2. 多阶段构建减少镜像大小
            # 构建阶段使用完整 JDK，运行阶段只使用 JRE
            
            # 3. 合并 RUN 指令减少层数
            RUN apt-get update && \\
                apt-get install -y --no-install-recommends \\
                curl \\
                wget && \\
                rm -rf /var/lib/apt/lists/*
            
            # 4. 使用 .dockerignore
            # .git
            # .idea
            # *.md
            # target/
            # .mvn/
            
            # 5. 利用构建缓存
            # 先复制变化少的文件（pom.xml）
            COPY pom.xml .
            RUN mvn dependency:go-offline
            
            # 再复制变化多的文件（源代码）
            COPY src ./src
            RUN mvn package
            \`\`\`
            
            ## 关键要点
            
            1. **多阶段构建**：减小最终镜像体积
            2. **非 root 用户**：提升安全性
            3. **健康检查**：确保容器正常运行
            4. **利用缓存**：优化构建速度
            5. **资源限制**：Docker run 时限制 CPU/内存
            """,
            "配置示例",
            new String[]{"docker", "multistage-build", "container", "optimization", "devops"},
            "system",
            null,
            java.time.LocalDateTime.now(),
            null,
            0,
            null
        ));
        
        // 3. 可观测性 - 日志、监控、追踪
        knowledgeBaseManager.addDocument(new KnowledgeDocument(
            "observability-patterns",
            "可观测性：日志、监控、分布式追踪",
            """
            【可观测性三支柱】
            
            ## 1. 结构化日志（ELK Stack）
            
            \`\`\`java
            // Logback 配置（logback-spring.xml）
            <configuration>
                <include resource="org/springframework/boot/logging/logback/defaults.xml"/>
                
                <appender name="JSON" class="ch.qos.logback.core.ConsoleAppender">
                    <encoder class="net.logstash.logback.encoder.LogstashEncoder">
                        <customFields>{"service":"order-service","env":"${SPRING_PROFILES_ACTIVE}"}</customFields>
                    </encoder>
                </appender>
                
                <root level="INFO">
                    <appender-ref ref="JSON"/>
                </root>
            </configuration>
            
            // 使用示例
            @Service
            @Slf4j
            public class OrderService {
                public OrderDTO createOrder(CreateOrderRequest request) {
                    // MDC 添加追踪 ID
                    MDC.put("traceId", UUID.randomUUID().toString());
                    MDC.put("userId", request.getUserId().toString());
                    
                    log.info("开始创建订单");
                    
                    try {
                        Order order = orderRepository.save(request.toEntity());
                        log.info("订单创建成功: orderId={}", order.getId());
                        return OrderDTO.fromEntity(order);
                    } catch (Exception e) {
                        log.error("订单创建失败: userId={}", request.getUserId(), e);
                        throw new BusinessException("订单创建失败", e);
                    } finally {
                        MDC.clear();
                    }
                }
            }
            \`\`\`
            
            ## 2. Prometheus + Grafana 监控
            
            \`\`\`java
            // Spring Boot Actuator + Micrometer
            \`\`\`yaml
            management:
              endpoints:
                web:
                  exposure:
                    include: health,prometheus,metrics,info
              metrics:
                tags:
                  application: ${spring.application.name}
            \`\`\`
            
            // 自定义指标
            @Service
            @RequiredArgsConstructor
            public class OrderMetricsService {
                private final MeterRegistry meterRegistry;
                private final Counter orderCreateCounter;
                private final Timer orderProcessTimer;
                private final Gauge activeOrdersGauge;
                
                @PostConstruct
                public void init() {
                    orderCreateCounter = Counter.builder("orders.created")
                        .description("Total orders created")
                        .tag("service", "order-service")
                        .register(meterRegistry);
                    
                    orderProcessTimer = Timer.builder("orders.process.duration")
                        .description("Order processing duration")
                        .register(meterRegistry);
                }
                
                public OrderDTO processOrder(CreateOrderRequest request) {
                    return orderProcessTimer.record(() -> {
                        OrderDTO order = createOrder(request);
                        orderCreateCounter.increment();
                        return order;
                    });
                }
            }
            \`\`\`
            
            ## 3. 分布式追踪（SkyWalking/Jaeger）
            
            \`\`\`java
            // SkyWalking Agent 配置
            \`\`\`yaml
            # Dockerfile 添加
            ENV SKYWALKING_AGENT_VERSION=9.0.0
            RUN wget https://archive.apache.org/dist/skywalking/${SKYWALKING_AGENT_VERSION}/apache-skywalking-java-agent-${SKYWALKING_AGENT_VERSION}.tgz
            RUN tar -xzf apache-skywalking-java-agent-${SKYWALKING_AGENT_VERSION}.tgz -C /opt/
            
            # JVM 参数
            ENV JAVA_OPTS="-javaagent:/opt/skywalking-agent/skywalking-agent.jar \
                          -Dskywalking.agent.service_name=order-service \
                          -Dskywalking.collector.backend_service=oap:11800"
            \`\`\`
            
            // 代码中手动创建 Span
            @Service
            public class OrderService {
                @Trace(operationName = "processPayment")
                @Tags({
                    @Tag(key = "orderId", value = "arg[0]"),
                    @Tag(key = "amount", value = "arg[1]")
                })
                public PaymentResult processPayment(String orderId, BigDecimal amount) {
                    // 自动追踪
                    return paymentGateway.pay(orderId, amount);
                }
            }
            \`\`\`
            
            ## 4. 健康检查端点
            
            \`\`\`java
            @Component
            public class DatabaseHealthIndicator implements HealthIndicator {
                private final DataSource dataSource;
                
                @Override
                public Health health() {
                    try (Connection conn = dataSource.getConnection()) {
                        if (conn.isValid(1)) {
                            return Health.up()
                                .withDetail("database", "MySQL")
                                .withDetail("version", conn.getMetaData().getDatabaseProductVersion())
                                .build();
                        }
                    } catch (SQLException e) {
                        return Health.down()
                            .withDetail("database", "MySQL")
                            .withException(e)
                            .build();
                    }
                    return Health.down().build();
                }
            }
            
            // 自定义健康检查
            @Component
            public class RabbitMQHealthIndicator implements HealthIndicator {
                private final RabbitTemplate rabbitTemplate;
                
                @Override
                public Health health() {
                    try {
                        rabbitTemplate.execute(channel -> {
                            channel.queueDeclarePassive("health-check");
                            return null;
                        });
                        return Health.up().withDetail("rabbitmq", "connected").build();
                    } catch (Exception e) {
                        return Health.down().withException(e).build();
                    }
                }
            }
            \`\`\`
            
            ## 5. Grafana Dashboard 配置
            
            \`\`\`json
            {
              "dashboard": {
                "panels": [
                  {
                    "title": "QPS (每秒请求数)",
                    "type": "graph",
                    "targets": [
                      {
                        "expr": "rate(http_server_requests_seconds_count[1m])",
                        "legendFormat": "{{uri}}"
                      }
                    ]
                  },
                  {
                    "title": "响应时间 P99",
                    "type": "graph",
                    "targets": [
                      {
                        "expr": "histogram_quantile(0.99, rate(http_server_requests_seconds_bucket[5m]))",
                        "legendFormat": "{{uri}}"
                      }
                    ]
                  },
                  {
                    "title": "错误率",
                    "type": "graph",
                    "targets": [
                      {
                        "expr": "rate(http_server_requests_seconds_count{status=~"5.."}[1m]) / rate(http_server_requests_seconds_count[1m])",
                        "legendFormat": "{{uri}}"
                      }
                    ]
                  }
                ]
              }
            }
            \`\`\`
            
            ## 关键要点
            
            1. **结构化日志**：JSON 格式，便于 ELK 解析
            2. **链路追踪**：TraceId 贯穿整个请求链路
            3. **业务指标**：自定义 Counter/Timer/Gauge
            4. **健康检查**：数据库、缓存、MQ 全面监控
            5. **告警规则**：Prometheus AlertManager 配置
            """,
            "配置示例",
            new String[]{"observability", "logging", "monitoring", "tracing", "prometheus"},
            "system",
            null,
            java.time.LocalDateTime.now(),
            null,
            0,
            null
        ));
        
        // 4. 安全架构 - Spring Security 高级配置
        knowledgeBaseManager.addDocument(new KnowledgeDocument(
            "security-architecture-patterns",
            "安全架构：Spring Security 生产级配置",
            """
            【Spring Security 生产级配置】
            
            ## 1. JWT 认证完整实现
            
            \`\`\`java
            // JWT Token 提供者
            @Component
            public class JwtTokenProvider {
                @Value("${jwt.secret}")
                private String secretKey;
                
                @Value("${jwt.expiration}")
                private long expiration;
                
                public String createToken(Authentication authentication) {
                    UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
                    
                    Date now = new Date();
                    Date expiryDate = new Date(now.getTime() + expiration);
                    
                    return Jwts.builder()
                        .setSubject(userPrincipal.getId().toString())
                        .claim("username", userPrincipal.getUsername())
                        .claim("roles", userPrincipal.getAuthorities())
                        .setIssuedAt(now)
                        .setExpiration(expiryDate)
                        .signWith(Keys.hmacShaKeyFor(secretKey.getBytes()), SignatureAlgorithm.HS512)
                        .compact();
                }
                
                public boolean validateToken(String token) {
                    try {
                        Jwts.parserBuilder()
                            .setSigningKey(Keys.hmacShaKeyFor(secretKey.getBytes()))
                            .build()
                            .parseClaimsJws(token);
                        return true;
                    } catch (Exception e) {
                        return false;
                    }
                }
                
                public Long getUserIdFromToken(String token) {
                    Claims claims = Jwts.parserBuilder()
                        .setSigningKey(Keys.hmacShaKeyFor(secretKey.getBytes()))
                        .build()
                        .parseClaimsJws(token)
                        .getBody();
                    
                    return Long.parseLong(claims.getSubject());
                }
            }
            
            // JWT 认证过滤器
            @Component
            public class JwtAuthenticationFilter extends OncePerRequestFilter {
                private final JwtTokenProvider tokenProvider;
                private final CustomUserDetailsService userDetailsService;
                
                @Override
                protected void doFilterInternal(HttpServletRequest request,
                                                HttpServletResponse response,
                                                FilterChain filterChain) throws ServletException, IOException {
                    String token = extractToken(request);
                    
                    if (token != null && tokenProvider.validateToken(token)) {
                        Long userId = tokenProvider.getUserIdFromToken(token);
                        
                        UserDetails userDetails = userDetailsService.loadUserById(userId);
                        UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities()
                            );
                        authentication.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request)
                        );
                        
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                    
                    filterChain.doFilter(request, response);
                }
                
                private String extractToken(HttpServletRequest request) {
                    String bearerToken = request.getHeader("Authorization");
                    if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
                        return bearerToken.substring(7);
                    }
                    return null;
                }
            }
            \`\`\`
            
            ## 2. 方法级权限控制
            
            \`\`\`java
            @Configuration
            @EnableMethodSecurity
            public class MethodSecurityConfig {
            }
            
            // 使用示例
            @RestController
            @RequestMapping("/api/orders")
            @RequiredArgsConstructor
            public class OrderController {
                private final OrderService orderService;
                
                @GetMapping("/{id}")
                @PreAuthorize("hasRole('USER')")
                public OrderDTO getOrder(@PathVariable Long id) {
                    return orderService.getOrder(id);
                }
                
                @PostMapping
                @PreAuthorize("hasRole('USER')")
                public OrderDTO createOrder(@RequestBody CreateOrderRequest request) {
                    return orderService.createOrder(request);
                }
                
                @PutMapping("/{id}/status")
                @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR')")
                public OrderDTO updateOrderStatus(
                        @PathVariable Long id,
                        @RequestBody UpdateStatusRequest request) {
                    return orderService.updateStatus(id, request);
                }
                
                @DeleteMapping("/{id}")
                @PreAuthorize("hasRole('ADMIN')")
                public void deleteOrder(@PathVariable Long id) {
                    orderService.deleteOrder(id);
                }
                
                // 自定义权限检查
                @GetMapping("/{id}")
                @PreAuthorize("@orderSecurity.isOwner(#id, authentication.principal.id)")
                public OrderDTO getOwnOrder(@PathVariable Long id) {
                    return orderService.getOrder(id);
                }
            }
            
            // 自定义安全表达式
            @Component("orderSecurity")
            public class OrderSecurity {
                private final OrderRepository orderRepository;
                
                public boolean isOwner(Long orderId, Long userId) {
                    return orderRepository.findById(orderId)
                        .map(order -> order.getUserId().equals(userId))
                        .orElse(false);
                }
            }
            \`\`\`
            
            ## 3. 安全防护配置
            
            \`\`\`java
            @Configuration
            @EnableWebSecurity
            @RequiredArgsConstructor
            public class SecurityConfig {
                private final JwtAuthenticationFilter jwtFilter;
                
                @Bean
                public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
                    http
                        .csrf(csrf -> csrf.disable())
                        .sessionManagement(session -> 
                            session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                        .authorizeHttpRequests(auth -> auth
                            .requestMatchers("/api/auth/**").permitAll()
                            .requestMatchers("/actuator/health/**").permitAll()
                            .requestMatchers("/api/admin/**").hasRole("ADMIN")
                            .anyRequest().authenticated()
                        )
                        .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                        .exceptionHandling(ex -> ex
                            .authenticationEntryPoint(new JwtAuthenticationEntryPoint())
                            .accessDeniedHandler(new JwtAccessDeniedHandler())
                        );
                    
                    return http.build();
                }
                
                // 密码加密
                @Bean
                public PasswordEncoder passwordEncoder() {
                    return new BCryptPasswordEncoder();
                }
            }
            \`\`\`
            
            ## 4. 接口限流
            
            \`\`\`java
            @Component
            public class RateLimitFilter extends OncePerRequestFilter {
                private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
                
                @Value("${ratelimit.requests-per-minute:100}")
                private int requestsPerMinute;
                
                @Override
                protected void doFilterInternal(HttpServletRequest request,
                                                HttpServletResponse response,
                                                FilterChain filterChain) throws ServletException, IOException {
                    String ip = getClientIp(request);
                    Bucket bucket = buckets.computeIfAbsent(ip, this::createBucket);
                    
                    if (bucket.tryConsume(1)) {
                        filterChain.doFilter(request, response);
                    } else {
                        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                        response.getWriter().write("Rate limit exceeded");
                    }
                }
                
                private Bucket createBucket(String ip) {
                    return Bucket.builder()
                        .addLimit(Bandwidth.classic(
                            requestsPerMinute,
                            Refill.greedy(requestsPerMinute, Duration.ofMinutes(1))
                        ))
                        .build();
                }
            }
            \`\`\`
            
            ## 关键要点
            
            1. **无状态认证**：JWT + 无 Session
            2. **细粒度权限**：@PreAuthorize 方法级控制
            3. **自定义权限**：业务逻辑权限检查
            4. **接口限流**：防止恶意请求
            5. **安全响应**：统一异常处理
            """,
            "代码示例",
            new String[]{"security", "jwt", "spring-security", "authentication", "authorization"},
            "system",
            null,
            java.time.LocalDateTime.now(),
            null,
            0,
            null
        ));
        
        // 5. 数据库架构 - 分库分表、读写分离
        knowledgeBaseManager.addDocument(new KnowledgeDocument(
            "database-architecture-patterns",
            "数据库架构：分库分表、读写分离、多数据源",
            """
            【数据库高级架构模式】
            
            ## 1. 多数据源配置（读写分离）
            
            \`\`\`java
            @Configuration
            public class DataSourceConfig {
                
                @Bean
                @ConfigurationProperties("spring.datasource.write")
                public DataSource writeDataSource() {
                    return DataSourceBuilder.create().build();
                }
                
                @Bean
                @ConfigurationProperties("spring.datasource.read")
                public DataSource readDataSource() {
                    return DataSourceBuilder.create().build();
                }
                
                @Bean
                public DataSource routingDataSource() {
                    Map<Object, Object> targetDataSources = new HashMap<>();
                    targetDataSources.put("WRITE", writeDataSource());
                    targetDataSources.put("READ", readDataSource());
                    
                    RoutingDataSource routingDataSource = new RoutingDataSource();
                    routingDataSource.setDefaultTargetDataSource(writeDataSource());
                    routingDataSource.setTargetDataSources(targetDataSources);
                    return routingDataSource;
                }
            }
            
            // 路由数据源
            public class RoutingDataSource extends AbstractRoutingDataSource {
                @Override
                protected Object determineCurrentLookupKey() {
                    return DataSourceContextHolder.getDataSourceType();
                }
            }
            
            // 数据源上下文
            public class DataSourceContextHolder {
                private static final ThreadLocal<String> CONTEXT = new ThreadLocal<>();
                
                public static void setDataSourceType(String type) {
                    CONTEXT.set(type);
                }
                
                public static String getDataSourceType() {
                    return CONTEXT.get();
                }
                
                public static void clear() {
                    CONTEXT.remove();
                }
            }
            
            // 读写路由切面
            @Aspect
            @Component
            public class DataSourceAspect {
                @Before("execution(* com.yourcompany.repository..*.find*(..)) || " +
                       "execution(* com.yourcompany.repository..*.get*(..)) || " +
                       "execution(* com.yourcompany.repository..*.query*(..))")
                public void setReadDataSource() {
                    DataSourceContextHolder.setDataSourceType("READ");
                }
                
                @Before("execution(* com.yourcompany.repository..*.save*(..)) || " +
                       "execution(* com.yourcompany.repository..*.update*(..)) || " +
                       "execution(* com.yourcompany.repository..*.delete*(..))")
                public void setWriteDataSource() {
                    DataSourceContextHolder.setDataSourceType("WRITE");
                }
                
                @After("execution(* com.yourcompany.repository..*(..))")
                public void clearDataSource() {
                    DataSourceContextHolder.clear();
                }
            }
            \`\`\`
            
            ## 2. ShardingSphere 分库分表
            
            \`\`\`yaml
            # application.yml
            spring:
              shardingsphere:
                datasource:
                  names: ds0,ds1
                  ds0:
                    type: com.zaxxer.hikari.HikariDataSource
                    jdbc-url: jdbc:mysql://localhost:3306/order_db_0
                    username: root
                    password: root
                  ds1:
                    type: com.zaxxer.hikari.HikariDataSource
                    jdbc-url: jdbc:mysql://localhost:3306/order_db_1
                    username: root
                    password: root
                
                rules:
                  sharding:
                    tables:
                      t_order:
                        actual-data-nodes: ds$->{0..1}.t_order_$->{0..3}
                        table-strategy:
                          standard:
                            sharding-column: user_id
                            sharding-algorithm-name: table-inline
                        database-strategy:
                          standard:
                            sharding-column: user_id
                            sharding-algorithm-name: database-inline
                    
                    sharding-algorithms:
                      database-inline:
                        type: INLINE
                        props:
                          algorithm-expression: ds$->{user_id % 2}
                      table-inline:
                        type: INLINE
                        props:
                          algorithm-expression: t_order_$->{user_id % 4}
            \`\`\`
            
            ## 3. 数据库连接池优化
            
            \`\`\`yaml
            spring:
              datasource:
                hikari:
                  maximum-pool-size: 20           # 最大连接数
                  minimum-idle: 5                 # 最小空闲连接
                  connection-timeout: 30000       # 连接超时 30s
                  idle-timeout: 600000            # 空闲超时 10min
                  max-lifetime: 1800000           # 最大生命周期 30min
                  validation-timeout: 5000        # 验证超时 5s
                  leak-detection-threshold: 60000 # 连接泄漏检测 60s
                  connection-test-query: SELECT 1 # 连接测试查询
                  pool-name: OrderServiceHikariPool
            \`\`\`
            
            ## 4. 批量操作优化
            
            \`\`\`java
            @Service
            @RequiredArgsConstructor
            public class BatchOperationService {
                private final JdbcTemplate jdbcTemplate;
                
                // 批量插入
                public void batchInsert(List<Order> orders) {
                    String sql = "INSERT INTO t_order (user_id, amount, status) VALUES (?, ?, ?)";
                    
                    jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
                        @Override
                        public void setValues(PreparedStatement ps, int i) throws SQLException {
                            Order order = orders.get(i);
                            ps.setLong(1, order.getUserId());
                            ps.setBigDecimal(2, order.getAmount());
                            ps.setString(3, order.getStatus());
                        }
                        
                        @Override
                        public int getBatchSize() {
                            return orders.size();
                        }
                    });
                }
                
                // 分批次处理（避免 OOM）
                public void batchProcess(List<Long> orderIds) {
                    int batchSize = 1000;
                    for (int i = 0; i < orderIds.size(); i += batchSize) {
                        List<Long> batch = orderIds.subList(
                            i, 
                            Math.min(i + batchSize, orderIds.size())
                        );
                        processBatch(batch);
                    }
                }
            }
            \`\`\`
            
            ## 5. 数据库迁移（Flyway）
            
            \`\`\`yaml
            spring:
              flyway:
                enabled: true
                locations: classpath:db/migration
                baseline-on-migrate: true
                validate-on-migrate: true
            \`\`\`
            
            \`\`\`sql
            -- V1__create_order_table.sql
            CREATE TABLE t_order (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                user_id BIGINT NOT NULL,
                order_no VARCHAR(64) NOT NULL UNIQUE,
                amount DECIMAL(10, 2) NOT NULL,
                status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                INDEX idx_user_id (user_id),
                INDEX idx_order_no (order_no),
                INDEX idx_status (status)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
            
            -- V2__add_payment_info.sql
            ALTER TABLE t_order 
            ADD COLUMN payment_method VARCHAR(50),
            ADD COLUMN payment_time TIMESTAMP,
            ADD INDEX idx_payment_time (payment_time);
            \`\`\`
            
            ## 关键要点
            
            1. **读写分离**：读操作走从库，写操作走主库
            2. **分库分表**：水平拆分提升性能
            3. **连接池**：合理配置连接数，避免泄漏
            4. **批量操作**：减少数据库往返次数
            5. **数据库迁移**：版本化管理数据库结构
            """,
            "配置示例",
            new String[]{"database", "sharding", "read-write-splitting", "connection-pool", "flyway"},
            "system",
            null,
            java.time.LocalDateTime.now(),
            null,
            0,
            null
        ));
        
        log.info("云原生架构模范代码知识库加载完成");
    }
}
