# Docker 生产环境部署指南

## 快速开始

### 1. 准备工作

```bash
# 复制环境变量文件
cp .env.example .env

# 编辑 .env 文件，填入您的 API 密钥
# DASHSCOPE_API_KEY=sk-your-actual-api-key
```

### 2. 构建并启动

```bash
# 方式 1：使用 docker-compose（推荐）
docker-compose up -d

# 方式 2：单独构建镜像
docker build -t langchain4j-agent:latest .

# 启动容器
docker run -d \
  --name langchain4j-agent \
  -p 8080:8080 \
  -e LANGCHAIN4J_OPEN_AI_CHAT_MODEL_API_KEY=your-api-key \
  langchain4j-agent:latest
```

### 3. 验证部署

```bash
# 查看容器状态
docker-compose ps

# 查看应用日志
docker-compose logs -f app

# 健康检查
curl http://localhost:8080/actuator/health

# 测试 AI 接口
curl -X POST http://localhost:8080/api/ai/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "你好，请介绍一下自己"}'
```

### 4. 停止服务

```bash
# 停止并删除容器
docker-compose down

# 停止但保留数据卷
docker-compose stop

# 完全清理（包括数据卷）
docker-compose down -v
```

## 配置说明

### 环境变量

| 变量名 | 说明 | 默认值 |
|--------|------|--------|
| `SPRING_PROFILES_ACTIVE` | Spring 配置文件 | `production` |
| `LANGCHAIN4J_OPEN_AI_CHAT_MODEL_API_KEY` | 通义千问 API Key | 必填 |
| `SPRING_DATASOURCE_URL` | 数据库连接 | `jdbc:h2:file:/app/data/ai-agent-db` |
| `SPRING_REDIS_HOST` | Redis 主机 | `redis` |
| `REDIS_PASSWORD` | Redis 密码 | `redis123` |

### JVM 参数

Dockerfile 中已优化 JVM 参数：

```bash
-Xms512m                  # 初始堆内存
-Xmx1024m                 # 最大堆内存
-XX:+UseG1GC              # G1 垃圾收集器
-XX:MaxGCPauseMillis=200  # 最大 GC 暂停时间
-XX:+UseContainerSupport  # 容器感知
-XX:MaxRAMPercentage=75.0 # 最大 RAM 使用率
```

### 资源限制

docker-compose.yml 中配置了资源限制：

```yaml
deploy:
  resources:
    limits:
      cpus: '2'          # CPU 上限
      memory: 2G         # 内存上限
    reservations:
      cpus: '1'          # CPU 预留
      memory: 1G         # 内存预留
```

## 生产环境优化

### 1. 日志管理

```bash
# 查看实时日志
docker-compose logs -f app

# 查看最近 100 行日志
docker-compose logs --tail=100 app

# 日志轮转（防止日志过大）
docker run --log-opt max-size=10m --log-opt max-file=3 ...
```

### 2. 数据持久化

应用数据存储在 Docker volumes 中：

- `app-data` - H2 数据库文件
- `app-logs` - 应用日志
- `redis-data` - Redis 持久化数据

备份数据：

```bash
# 备份数据卷
docker run --rm \
  -v app-data:/data \
  -v $(pwd)/backup:/backup \
  alpine tar czf /backup/app-data.tar.gz -C /data .

# 恢复数据卷
docker run --rm \
  -v app-data:/data \
  -v $(pwd)/backup:/backup \
  alpine tar xzf /backup/app-data.tar.gz -C /data
```

### 3. 网络配置

应用和 Redis 在同一 Docker 网络中：

```bash
# 查看网络
docker network ls

# 查看网络详情
docker network inspect langchain4j-agent-demo_app-network
```

### 4. 安全加固

```bash
# 使用非 root 用户运行
# Dockerfile 中已配置：USER appuser

# 只暴露必要端口
# ports:
#   - "8080:8080"  # 应用端口

# 使用只读文件系统（可选）
# read_only: true
```

## 监控与维护

### 健康检查

Dockerfile 中已配置健康检查：

```yaml
HEALTHCHECK --interval=30s \
            --timeout=3s \
            --start-period=60s \
            --retries=3
```

查看健康状态：

```bash
docker inspect --format='{{.State.Health.Status}}' langchain4j-agent
```

### 资源监控

```bash
# 查看容器资源使用
docker stats langchain4j-agent

# 查看容器详情
docker inspect langchain4j-agent
```

### 更新部署

```bash
# 1. 拉取最新代码
git pull

# 2. 重新构建镜像
docker-compose build --no-cache

# 3. 重启服务
docker-compose up -d

# 4. 清理旧镜像
docker image prune -f
```

## 故障排查

### 容器无法启动

```bash
# 查看详细日志
docker-compose logs app

# 检查配置文件
docker-compose config

# 测试构建
docker build -t test .
```

### 数据库问题

```bash
# 进入容器
docker exec -it langchain4j-agent sh

# 检查数据目录
ls -la /app/data

# 查看 H2 数据库文件
file /app/data/ai-agent-db.mv.db
```

### Redis 连接问题

```bash
# 检查 Redis 状态
docker-compose ps redis

# 测试 Redis 连接
docker exec -it redis-cache redis-cli -a redis123 ping

# 查看 Redis 日志
docker-compose logs redis
```

### 内存不足

```bash
# 增加 JVM 内存限制
# 修改 docker-compose.yml 中的 environment：
environment:
  - JAVA_OPTS=-Xms1g -Xmx2g

# 增加容器内存限制
deploy:
  resources:
    limits:
      memory: 4G
```

## 性能调优

### 1. 数据库优化

使用生产级数据库（替代 H2）：

```yaml
# docker-compose.yml 中添加 PostgreSQL
postgresql:
  image: postgres:15-alpine
  environment:
    POSTGRES_DB: agent_db
    POSTGRES_USER: admin
    POSTGRES_PASSWORD: admin123
  volumes:
    - postgres-data:/var/lib/postgresql/data

# app 服务中添加依赖
depends_on:
  - postgresql

# app 环境变量
environment:
  - SPRING_DATASOURCE_URL=jdbc:postgresql://postgresql:5432/agent_db
```

### 2. 缓存优化

调整 Redis 配置：

```yaml
redis:
  command: >
    redis-server
    --maxmemory 512mb
    --maxmemory-policy allkeys-lru
    --appendonly yes
```

### 3. 应用优化

```yaml
# docker-compose.yml
app:
  environment:
    # 增加线程池大小
    - SERVER_TOMCAT_MAX_THREADS=200
    # 调整连接池
    - SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE=20
```

## CI/CD 集成

### GitHub Actions 示例

```yaml
# .github/workflows/docker.yml
name: Docker Build and Push

on:
  push:
    branches: [ main ]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Build Docker image
        run: docker build -t langchain4j-agent:${{ github.sha }} .
      
      - name: Run tests
        run: docker run --rm langchain4j-agent:${{ github.sha }} mvn test
      
      - name: Push to registry
        run: |
          docker tag langchain4j-agent:${{ github.sha }} registry.example.com/langchain4j-agent:latest
          docker push registry.example.com/langchain4j-agent:latest
```

## 常见问题

### Q: 如何修改 API 端口？

A: 修改 docker-compose.yml：

```yaml
ports:
  - "9090:8080"  # 宿主机 9090 映射到容器 8080
```

### Q: 如何启用调试模式？

A: 添加环境变量：

```yaml
environment:
  - JAVA_OPTS=-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=*:5005
  - SPRING_PROFILES_ACTIVE=dev
ports:
  - "8080:8080"
  - "5005:5005"  # 调试端口
```

### Q: 如何备份和恢复数据？

A: 使用 docker volume 命令：

```bash
# 备份
docker run --rm -v app-data:/data -v $(pwd):/backup alpine tar czf /backup/data.tar.gz -C /data .

# 恢复
docker run --rm -v app-data:/data -v $(pwd):/backup alpine tar xzf /backup/data.tar.gz -C /data
```

## 许可证

本项目采用 MIT 许可证。

## 联系方式

如有问题，请提交 Issue 或联系开发团队。
