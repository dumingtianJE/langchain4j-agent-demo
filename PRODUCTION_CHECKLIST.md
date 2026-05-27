# Docker 生产环境配置清单

## ✅ 完整的服务组件

### 核心服务（必须）

| 服务 | 端口 | 用途 | 状态 |
|------|------|------|------|
| **app** | 8080 | LangChain4j Agent 应用 | ✅ 已配置 |
| **redis** | 6379 | 缓存 + API 限流 | ✅ 已配置 |
| **milvus** | 19530, 9091 | 向量数据库（RAG） | ✅ 已配置 |

### 监控服务（推荐）

| 服务 | 端口 | 用途 | 状态 |
|------|------|------|------|
| **prometheus** | 9090 | 指标收集 | ✅ 已配置 |
| **grafana** | 3000 | 可视化监控面板 | ✅ 已配置 |

### 基础设施（可选）

| 服务 | 端口 | 用途 | 状态 |
|------|------|------|------|
| **nginx** | 80, 443 | 反向代理 + SSL | ✅ 已配置 |

---

## 📋 配置项检查清单

### 1. 环境变量配置

#### ✅ 已配置的环境变量

```yaml
# AI 模型配置
- LANGCHAIN4J_OPEN_AI_CHAT_MODEL_API_KEY=${DASHSCOPE_API_KEY}
- LANGCHAIN4J_OPEN_AI_CHAT_MODEL_BASE_URL=https://dashscope.aliyuncs.com/compatible-mode/v1
- LANGCHAIN4J_OPEN_AI_CHAT_MODEL_MODEL_NAME=qwen-max

# 数据库配置
- SPRING_DATASOURCE_URL=jdbc:h2:file:/app/data/ai-agent-db;DB_CLOSE_ON_EXIT=FALSE
- SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE=20
- SPRING_DATASOURCE_HIKARI_MINIMUM_IDLE=5

# Redis 配置
- SPRING_REDIS_HOST=redis
- SPRING_REDIS_PORT=6379
- SPRING_REDIS_PASSWORD=${REDIS_PASSWORD:-redis123}
- SPRING_REDIS_TIMEOUT=5000

# Milvus 配置
- AI_EMBEDDING_TYPE=milvus
- AI_MILVUS_HOST=milvus
- AI_MILVUS_PORT=19530

# JWT 配置
- JWT_SECRET=${JWT_SECRET:-mySecretKeyForJWTTokenGenerationAndValidation123456789}
- JWT_EXPIRATION=86400

# API 限流
- AI_RATELIMIT_ENABLED=true
- AI_RATELIMIT_REQUESTS_PER_MINUTE=100
- AI_RATELIMIT_BURST_CAPACITY=150

# 日志配置
- LOGGING_LEVEL_COM_YOURCOMPANY=INFO
- LOGGING_LEVEL_DEV_LANGCHAIN4J=INFO
- LOGGING_FILE_NAME=/app/logs/application.log
```

#### ⚠️ 需要您配置的环境变量

在 `.env` 文件中必须设置：

```bash
# 1. AI API Key（必填）
DASHSCOPE_API_KEY=sk-your-actual-api-key

# 2. Redis 密码（必填）
REDIS_PASSWORD=YourStrongPassword123!

# 3. JWT 密钥（强烈建议修改）
JWT_SECRET=your-random-secret-key-change-this

# 4. Grafana 密码（可选）
GRAFANA_ADMIN_PASSWORD=your-grafana-password
```

---

## 🔐 安全配置检查

### ✅ 已配置的安全措施

- [x] 非 root 用户运行（Dockerfile）
- [x] Redis 密码保护
- [x] JWT 认证机制
- [x] API 限流（100 请求/分钟）
- [x] 日志轮转（10MB × 3 文件）
- [x] 资源限制（CPU + 内存）
- [x] 健康检查（自动重启）
- [x] 网络隔离（app-network）
- [x] Nginx 限流保护
- [x] Actuator 端点访问控制

### ⚠️ 生产环境必须完成

- [ ] 修改所有默认密码（.env 文件）
- [ ] 配置 HTTPS（Nginx SSL）
- [ ] 配置防火墙规则
- [ ] 定期更新镜像
- [ ] 配置日志备份
- [ ] 配置数据库备份

---

## 💾 数据持久化检查

### 已配置的数据卷

| 数据卷 | 用途 | 大小限制 |
|--------|------|----------|
| `app-data` | H2 数据库文件 | 无限制 |
| `app-logs` | 应用日志 | 30MB（3 × 10MB） |
| `redis-data` | Redis 持久化 | 无限制 |
| `milvus-data` | Milvus 向量数据 | 无限制 |
| `prometheus-data` | Prometheus 指标 | 30 天保留 |
| `grafana-data` | Grafana 配置 | 无限制 |
| `nginx-logs` | Nginx 访问日志 | 无限制 |

### 备份建议

```bash
# 备份所有数据卷
docker-compose exec redis redis-cli -a $REDIS_PASSWORD BGSAVE

# 备份 H2 数据库
docker cp langchain4j-agent:/app/data/ai-agent-db.mv.db ./backup/

# 备份 Milvus 数据
docker cp milvus-vector-db:/var/lib/milvus/ ./backup/milvus/
```

---

## 📊 监控配置检查

### Prometheus 配置

- ✅ 采集间隔：15 秒
- ✅ 数据保留：30 天
- ✅ 监控目标：Spring Boot App + Prometheus 自身

### Grafana 配置

- ✅ 默认数据源：Prometheus
- ✅ 自动配置数据源
- ️ 需要手动导入 Dashboard

### 访问地址

| 服务 | URL | 默认账号 | 默认密码 |
|------|-----|----------|----------|
| 应用 | http://localhost:8080 | - | - |
| Redis | localhost:6379 | - | 您配置的密码 |
| Milvus | localhost:19530 | - | - |
| Prometheus | http://localhost:9090 | - | - |
| Grafana | http://localhost:3000 | admin | 您配置的密码 |
| Nginx | http://localhost:80 | - | - |

---

## 🚀 启动服务

### 完整启动（所有服务）

```bash
# 1. 配置环境变量
cp .env.example .env
vim .env  # 修改所有配置项

# 2. 一键启动
docker-compose up -d

# 3. 验证服务
docker-compose ps
curl http://localhost:8080/actuator/health
```

### 仅启动核心服务（推荐开发环境）

```bash
# 启动 app + redis + milvus（不含监控）
docker-compose up -d app redis milvus
```

### 启动监控服务

```bash
# 启动 Prometheus + Grafana
docker-compose up -d prometheus grafana
```

### 启动 Nginx 反向代理

```bash
# 使用 production profile 启动 Nginx
docker-compose --profile production up -d nginx
```

---

## 🔧 配置文件清单

### 必须存在的文件

```
langchain4j-agent-demo/
── Dockerfile                          ✅ 已创建
├── docker-compose.yml                  ✅ 已更新（完整版）
├── .dockerignore                       ✅ 已创建
├── .env.example                        ✅ 已更新（完整版）
── .env                                ⚠️ 需要您创建
│
├── monitoring/
│   ├── prometheus.yml                  ✅ 已创建
│   └── grafana-dashboards/
│       └── datasources.yml             ✅ 已创建
│
└── nginx/
    └── nginx.conf                      ✅ 已创建
```

### 需要您创建的文件

```bash
# 1. 环境变量文件（必须）
cp .env.example .env
vim .env

# 2. SSL 证书（如果需要 HTTPS）
mkdir -p nginx/ssl
# 放置 cert.pem 和 key.pem

# 3. Grafana Dashboards（可选）
mkdir -p monitoring/grafana-dashboards
# 添加自定义 dashboard JSON 文件
```

---

## ️ 生产环境注意事项

### 必须修改的配置

1. **API 密钥**
   ```bash
   DASHSCOPE_API_KEY=sk-your-real-api-key
   ```

2. **所有密码**
   ```bash
   REDIS_PASSWORD=YourStrongPassword123!
   JWT_SECRET=your-random-secret-key
   GRAFANA_ADMIN_PASSWORD=your-grafana-password
   ```

3. **域名配置**（Nginx）
   ```nginx
   server_name your-domain.com;
   ```

### 建议优化的配置

1. **资源限制**（根据服务器配置调整）
   ```yaml
   app:
     deploy:
       resources:
         limits:
           cpus: '4'
           memory: 4G
   ```

2. **日志保留策略**
   ```yaml
   logging:
     options:
       max-size: "50m"
       max-file: "10"
   ```

3. **数据库切换**（H2 → PostgreSQL）
   ```yaml
   # 使用 PostgreSQL 替代 H2
   - SPRING_DATASOURCE_URL=jdbc:postgresql://postgresql:5432/agent_db
   ```

---

## 📈 性能调优建议

### JVM 参数优化

```yaml
environment:
  - JAVA_OPTS=-Xms2g -Xmx4g -XX:+UseG1GC -XX:MaxGCPauseMillis=200
```

### Redis 优化

```yaml
command: >
  redis-server
  --maxmemory 2gb
  --maxmemory-policy allkeys-lru
  --appendonly yes
```

### Milvus 优化

```yaml
deploy:
  resources:
    limits:
      cpus: '4'
      memory: 8G
```

---

##  故障排查

### 服务启动失败

```bash
# 查看详细日志
docker-compose logs app

# 检查配置
docker-compose config

# 测试单个服务
docker-compose up -d redis
docker-compose logs redis
```

### Milvus 启动慢

Milvus 需要较长的启动时间（90 秒）：

```bash
# 查看启动日志
docker-compose logs -f milvus

# 等待健康检查通过
docker-compose ps milvus
```

### 监控服务无法访问

```bash
# 检查 Prometheus
curl http://localhost:9090/-/healthy

# 检查 Grafana
curl http://localhost:3000/api/health

# 查看日志
docker-compose logs prometheus
docker-compose logs grafana
```

---

## ✅ 最终检查清单

部署前请确认：

- [x] Dockerfile 已创建
- [x] docker-compose.yml 已更新（完整配置）
- [x] .env.example 已更新
- [x] monitoring/ 目录已创建
- [x] nginx/ 目录已创建
- [ ] .env 文件已创建并配置
- [ ] 所有默认密码已修改
- [ ] API Key 已配置
- [ ] 磁盘空间充足（至少 10GB）
- [ ] 端口未被占用（8080, 6379, 19530, 9090, 3000, 80）
- [ ] 防火墙已配置
- [ ] 备份策略已制定

---

**配置完整！现在您可以开始部署了！** 🚀
