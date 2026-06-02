# 生产环境部署配置指南

## 一、系统架构总览

```
                    ┌─────────────┐
                    │   Nginx     │  端口: 80/443
                    │  (反向代理)  │  CPU: 0.5核 | 内存: 128MB
                    └──────┬──────┘
                           │
            ┌──────────────┼──────────────┐
            ▼              ▼              ▼
    ┌──────────────┐ ┌──────────┐ ┌──────────────┐
    │ Spring Boot  │ │  Redis   │ │   Grafana    │
    │   Agent 应用  │ │  (缓存)   │ │  (监控面板)  │
    │  端口: 8080  │ │ 端口:6379│ │  端口: 3000  │
    └──────┬───────┘ └──────────┘ └──────┬───────┘
           │                             │
    ┌──────┼──────────┐           ┌──────┴───────┐
    ▼      ▼          ▼           ▼              ▼
┌──────┐ ┌──────┐ ┌────────┐ ┌──────────┐ ┌──────────┐
│  H2  │ │Milvus│ │Prometheus│ │ 应用日志  │ │Prometheus│
│(数据库)│ │(向量库)│ │ (监控)   │ │  文件存储 │ │ 数据持久化│
│      │ │19530 │ │ 端口:9090│ │          │ │          │
└──────┘ └──────┘ └──────────┘ └──────────┘ └──────────┘
```

---

## 二、硬件设施要求

### 2.1 服务器整体规格

| 配置级别 | CPU | 内存 | 系统盘 | 数据盘 | 适用场景 |
|---------|-----|------|-------|-------|---------|
| **最低配置** | 4 核 | 8 GB | 50 GB SSD | 20 GB | 开发测试、内部小团队使用（≤10人） |
| **标准配置（推荐）** | 8 核 | 16 GB | 100 GB SSD | 50 GB SSD | 中小规模生产（≤100并发用户） |
| **高配** | 16 核 | 32 GB | 200 GB SSD | 100 GB SSD | 大规模生产（≤500并发用户） |
| **高可用集群** | 3×8核 | 3×16GB | 3×100GB SSD | NAS/分布式存储 | K8s 多副本、高可用部署 |

> **说明**：Milvus 向量数据库是内存密集型服务，占用约 50% 的总内存，是主要资源消耗者。

---

### 2.2 各服务组件资源分配

#### Docker Compose 部署（单机）

| 服务 | CPU 请求 | CPU 上限 | 内存请求 | 内存上限 | 磁盘 | 说明 |
|------|---------|---------|---------|---------|------|------|
| **langchain4j-agent** (应用) | 1 核 | 2 核 | 1 GB | 2 GB | 5 GB | Spring Boot + JVM |
| **Milvus** (向量数据库) | 1 核 | 2 核 | 2 GB | 4 GB | 20 GB | 内存 + 索引持久化 |
| **Redis** (缓存/限流) | 0.5 核 | 1 核 | 512 MB | 1 GB | 5 GB | 512MB maxmemory |
| **Prometheus** (监控) | 0.25 核 | 0.5 核 | 256 MB | 512 MB | 10 GB | 保留 30 天指标 |
| **Grafana** (面板) | 0.25 核 | 0.5 核 | 128 MB | 256 MB | 1 GB | 仪表盘数据 |
| **Nginx** (反向代理) | 0.25 核 | 0.5 核 | 64 MB | 128 MB | 1 GB | 仅生产启用 |
| **合计** | **3.25 核** | **6.5 核** | **4 GB** | **8 GB** | **42 GB** | — |

#### Kubernetes 部署（多副本）

| 服务 | 副本数 | 每副本 CPU | 每副本内存 | PVC 存储 | HPA 范围 |
|------|-------|-----------|-----------|---------|---------|
| **langchain4j-agent** | 1~3 | 250m~500m | 384~768 Mi | 5 Gi + 2 Gi(日志) | 1~3 副本 |
| **Milvus Standalone** | 1 | 500m~1000m | 1~2 Gi | 20 Gi | 固定 1 副本 |
| **Milvus Cluster** | 3 | 1~2 核 | 4~8 Gi | 50 Gi | 3 副本（高可用） |
| **Redis** | 1~3 | 250m~500m | 128~256 Mi | 5 Gi | Sentinel 3节点 |
| **Prometheus** | 1 | 250m | 256 Mi | 10 Gi | 固定 1 副本 |
| **Grafana** | 1 | 100m~250m | 128 Mi | 1 Gi | 固定 1 副本 |

---

### 2.3 JVM 内存参数说明

```bash
# 容器内 JVM 参数（Dockerfile 内置，可通过 JAVA_OPTS 覆盖）
-Xms512m                              # 初始堆大小
-Xmx1024m                             # 最大堆大小
-XX:+UseG1GC                          # G1 垃圾收集器（低延迟）
-XX:MaxGCPauseMillis=200              # GC 最大暂停时间目标
-XX:+UseContainerSupport              # 启用容器内存感知
-XX:MaxRAMPercentage=75.0             # 堆占容器内存的 75%
-Djava.security.egd=file:/dev/./urandom  # 安全随机数源
```

| 容器内存限制 | 实际可用堆 (-Xmx) | 非堆占用 | 说明 |
|------------|-----------------|---------|------|
| 1 GB | ~768 MB | ~256 MB | 最低可用 |
| 2 GB (默认) | ~1.5 GB | ~512 MB | 推荐配置 |
| 4 GB | ~3 GB | ~1 GB | 高并发场景 |

---

### 2.4 磁盘 I/O 与存储要求

| 存储类型 | 最小 IOPS | 推荐 IOPS | 用途 |
|---------|----------|----------|------|
| **SSD（系统盘）** | 3,000 | 5,000+ | 操作系统 + Docker 镜像 |
| **SSD（数据盘）** | 5,000 | 10,000+ | Milvus 向量索引、H2/MySQL 数据 |
| **HDD（日志盘）** | 500 | 1,000 | 应用日志、监控数据（可选） |

**磁盘空间分配明细**：

| 目录/卷 | 大小 | 说明 |
|---------|------|------|
| `/app/data` | 5 GB | H2 数据库 + 向量嵌入缓存 |
| `/app/data/embeddings` | 2 GB | 本地向量嵌入存储（Milvus 未启用时使用） |
| `/app/logs` | 2 GB | 应用日志（自动轮转，保留 30 天） |
| `/var/lib/milvus` | 20 GB | Milvus 向量数据 + etcd + MinIO |
| `/var/lib/redis` | 5 GB | Redis RDB + AOF 持久化 |
| `/prometheus` | 10 GB | Prometheus TSDB（保留 30 天） |
| `/var/lib/grafana` | 1 GB | Grafana 仪表盘与配置 |
| Docker 镜像缓存 | 10 GB | 所有服务镜像总计 |

---

### 2.5 网络要求

| 组件 | 入站端口 | 出站 | 协议 | 说明 |
|------|---------|------|------|------|
| **Nginx** | 80, 443 | → 8080 | HTTP/HTTPS | 外部访问入口 |
| **Spring Boot App** | 8080 | → 6379, 19530, 443 | TCP | Redis + Milvus + DashScope API |
| **Redis** | 6379 | — | TCP | 仅内网访问 |
| **Milvus** | 19530, 9091 | — | gRPC/HTTP | 仅内网访问 |
| **Prometheus** | 9090 | → 8080 | HTTP | 抓取应用指标 |
| **Grafana** | 3000 | → 9090 | HTTP | 读取 Prometheus 数据 |
| **DashScope API** | — | → 443 | HTTPS | 外网访问阿里云 |

**带宽要求**：
- **最低**：5 Mbps（文本对话为主）
- **推荐**：10 Mbps+（含 SSE 流式响应 + 监控数据上报）

---

## 三、软件环境要求

### 3.1 宿主机基础软件

| 软件 | 最低版本 | 推荐版本 | 用途 | 必须 |
|------|---------|---------|------|------|
| **操作系统** | Ubuntu 20.04 / CentOS 7 | Ubuntu 22.04 LTS / CentOS 8 | 服务器 OS | 是 |
| **Docker Engine** | 20.10+ | 24.x+ | 容器运行时 | 是 |
| **Docker Compose** | 2.0+ (V2) | 2.23+ | 容器编排 | 是 |
| **Kubernetes (可选)** | 1.26+ | 1.28+ | K8s 部署 | 可选 |
| **Git** | 2.30+ | 2.40+ | 拉取源码 | 是 |

---

### 3.2 后端运行环境

#### Java（构建时需要，运行时由容器内置 JDK 21）

| 版本 | 说明 |
|------|------|
| **JDK 17**（最低） | pom.xml 配置 `source/target=17`，Spring Boot 3.4.2 要求最低 Java 17 |
| **JDK 21**（推荐） | Docker 多阶段构建使用 `eclipse-temurin:21`，性能更优 |

```bash
# Ubuntu 安装 JDK 17
sudo apt update
sudo apt install -y openjdk-17-jdk
java -version

# 或使用 SDKMAN（推荐）
curl -s "https://get.sdkman.io" | bash
sdk install java 21.0.2-tem
```

#### Maven（构建工具）

| 版本 | 说明 |
|------|------|
| **Maven 3.8+**（最低） | Spring Boot 3.x 构建支持 |
| **Maven 3.9+**（推荐） | 更好的依赖解析和性能 |

```bash
# Ubuntu 安装
sudo apt install -y maven
mvn -version

# 或手动安装 3.9.x
cd /opt
wget https://dlcdn.apache.org/maven/maven-3/3.9.6/binaries/apache-maven-3.9.6-bin.tar.gz
tar xzf apache-maven-3.9.6-bin.tar.gz
ln -s /opt/apache-maven-3.9.6/bin/mvn /usr/local/bin/mvn
```

> **注意**：Docker 多阶段构建模式下，宿主机**不需要**安装 Java 和 Maven，Dockerfile 内置了 `maven:3.9-eclipse-temurin-21` 构建镜像。仅在本地编译调试时需要。

---

### 3.3 数据库环境

项目支持两种数据库方案，按生产规模选择：

#### 方案 A：H2 嵌入式数据库（小规模 / 快速部署）

| 项目 | 说明 |
|------|------|
| **H2 版本** | 2.2.224（Spring Boot 3.4.2 内置） |
| **安装** | 无需额外安装，应用启动自动创建 `data/ai-agent-db.mv.db` |
| **适用** | 单机部署、≤10 并发用户、开发测试 |
| **限制** | 不支持并发写入，不适合高并发生产 |

```yaml
# application.yml 中 H2 配置（默认已启用）
spring:
  datasource:
    url: jdbc:h2:file:./data/ai-agent-db
    driver-class-name: org.h2.Driver
```

#### 方案 B：MySQL 8.x（推荐生产）

| 项目 | 说明 |
|------|------|
| **MySQL 版本** | 8.0+ 或 8.4 LTS |
| **安装方式** | Docker 容器 或 物理机安装 |
| **字符集** | `utf8mb4`，排序规则 `utf8mb4_unicode_ci` |
| **存储引擎** | InnoDB |
| **推荐配置** | `innodb_buffer_pool_size` ≥ 1GB |

```bash
# Docker 启动 MySQL
docker run -d --name mysql \
  -e MYSQL_ROOT_PASSWORD=CHANGE_ME_root_password \
  -e MYSQL_DATABASE=langchain4j_agent \
  -p 3306:3306 \
  mysql:8.0 --character-set-server=utf8mb4 --collation-server=utf8mb4_unicode_ci

# 创建应用用户
mysql -u root -p -e "
  CREATE DATABASE IF NOT EXISTS langchain4j_agent
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;
  CREATE USER 'agent_user'@'%' IDENTIFIED BY 'CHANGE_ME_strong_password';
  GRANT ALL PRIVILEGES ON langchain4j_agent.* TO 'agent_user'@'%';
  FLUSH PRIVILEGES;
"
```

```yaml
# application-prod.yml 中 MySQL 配置
spring:
  datasource:
    url: jdbc:mysql://mysql-host:3306/langchain4j_agent?useSSL=true&serverTimezone=Asia/Shanghai
    username: agent_user
    password: CHANGE_ME_strong_password
    driver-class-name: com.mysql.cj.jdbc.Driver
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
```

#### 方案 C：PostgreSQL 15+（K8s 部署推荐）

```bash
# Docker 启动 PostgreSQL
docker run -d --name postgres \
  -e POSTGRES_DB=langchain4j_agent \
  -e POSTGRES_USER=agent_user \
  -e POSTGRES_PASSWORD=CHANGE_ME \
  -p 5432:5432 \
  postgres:15-alpine
```

> **K8s 部署**：项目已提供 `k8s/postgresql.yaml`，包含 StatefulSet + PVC 持久化配置。

---

### 3.4 向量数据库 —— Milvus

| 项目 | 说明 |
|------|------|
| **版本** | Milvus v2.3.x（SDK 2.3.0 兼容） |
| **部署模式** | Standalone（单机）或 Cluster（集群） |
| **内部依赖** | etcd + MinIO（Milvus 镜像内置） |
| **最低资源** | 2 核 CPU / 4 GB 内存 / 20 GB SSD |
| **MetricType** | IP（内积距离，项目配置） |
| **向量维度** | 1024（DashScope text-embedding-v3） |

```bash
# 方式1：Docker Compose（已在 docker-compose.yml 中配置）
docker compose up -d milvus

# 方式2：独立 Docker 启动
docker run -d --name milvus \
  -p 19530:19530 \
  -p 9091:9091 \
  -v milvus_data:/var/lib/milvus \
  milvusdb/milvus:v2.3.0

# 验证 Milvus 状态
curl http://localhost:9091/healthz
```

```yaml
# application.yml 中 Milvus 配置
milvus:
  enabled: true
  host: milvus
  port: 19530
  metric-type: IP
  index-type: IVF_FLAT
  collection-name: code_embeddings
```

> **K8s 部署**：项目已提供 `k8s/milvus.yaml`（Standalone）和 `k8s/milvus-cluster.yaml`（3 节点集群）。

---

### 3.5 缓存 —— Redis

| 项目 | 说明 |
|------|------|
| **版本** | Redis 7.x（alpine 镜像） |
| **用途** | 会话缓存、API 限流计数、对话记忆 |
| **持久化** | AOF + RDB 双模式 |
| **内存策略** | `maxmemory 512mb` + `allkeys-lru` |

```bash
# Docker 启动 Redis
docker run -d --name redis \
  -p 6379:6379 \
  -v redis_data:/data \
  redis:7-alpine \
  redis-server --requirepass CHANGE_ME_password \
    --maxmemory 512mb \
    --maxmemory-policy allkeys-lru \
    --appendonly yes
```

```yaml
# application.yml 中 Redis 配置
spring:
  redis:
    host: redis
    port: 6379
    password: CHANGE_ME_password
    timeout: 3000ms
```

> **K8s 部署**：项目已提供 `k8s/redis.yaml`（Deployment + Service + PVC 5Gi）和 `k8s/redis-sentinel.yaml`（高可用 Sentinel 3 节点）。

---

### 3.6 前端环境 —— Node.js + Vue

| 项目 | 说明 |
|------|------|
| **Node.js 版本** | 18+（最低）/ 20 LTS（推荐） |
| **npm 版本** | 9+（随 Node.js 安装） |
| **框架** | Vue 3.5 + Vite 5.4 |
| **UI 组件库** | Element Plus 2.14 |
| **HTTP 客户端** | Axios |
| **路由** | Vue Router 4.6 |
| **构建产物** | `frontend/dist/` 静态文件，由 Nginx 托管 |

```bash
# 安装 Node.js 20 LTS（Ubuntu）
curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash -
sudo apt install -y nodejs
node -v && npm -v

# 或使用 nvm（推荐版本管理）
curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.39.7/install.sh | bash
nvm install 20
nvm use 20

# 安装依赖并构建
cd frontend
npm install
npm run build    # 产物输出到 frontend/dist/
```

> **生产部署**：前端构建产物为纯静态文件，放入 Nginx 容器或挂载到 Nginx 的 `/usr/share/nginx/html` 即可，**运行时不需要 Node.js**。

---

### 3.7 反向代理 —— Nginx

| 项目 | 说明 |
|------|------|
| **版本** | Nginx 1.25+（alpine 镜像） |
| **用途** | 反向代理、SSL 终止、静态文件托管、负载均衡 |
| **配置** | 项目已提供 `nginx/nginx.conf` |

```nginx
# nginx.conf 核心配置（项目已提供）
upstream backend {
    server app:8080;
}

server {
    listen 80;
    # 生产环境建议配置 SSL
    # listen 443 ssl;
    # ssl_certificate     /etc/nginx/certs/server.crt;
    # ssl_certificate_key /etc/nginx/certs/server.key;

    location /api/ {
        proxy_pass http://backend;
        proxy_set_header Host $host;
        # SSE 流式响应支持
        proxy_buffering off;
        proxy_cache off;
    }

    location / {
        root /usr/share/nginx/html;  # 前端静态文件
        try_files $uri $uri/ /index.html;
    }
}
```

> **K8s 部署**：项目提供 `k8s/ingress.yaml`（Nginx Ingress）和 `k8s/ingress-traefik.yaml`（Traefik Ingress）两套配置。

---

### 3.8 监控环境 —— Prometheus + Grafana

| 组件 | 版本 | 端口 | 用途 |
|------|------|------|------|
| **Prometheus** | 2.x (latest) | 9090 | 指标采集与存储（保留 30 天） |
| **Grafana** | 10.x (latest) | 3000 | 可视化监控面板 |

```bash
# Prometheus 配置（项目已提供 monitoring/prometheus.yml）
docker run -d --name prometheus \
  -p 9090:9090 \
  -v ./monitoring/prometheus.yml:/etc/prometheus/prometheus.yml \
  -v prometheus_data:/prometheus \
  prom/prometheus:latest

# Grafana 启动
docker run -d --name grafana \
  -p 3000:3000 \
  -v grafana_data:/var/lib/grafana \
  -e GF_SECURITY_ADMIN_PASSWORD=CHANGE_ME \
  grafana/grafana:latest
```

> **应用已内置** Spring Boot Actuator + Micrometer，`/actuator/prometheus` 端点自动暴露指标，Prometheus 自动抓取。

---

### 3.9 AI 模型服务（外部依赖，无需安装）

| 服务 | 提供方 | 说明 |
|------|--------|------|
| **通义千问 qwen-max** | 阿里云 DashScope | LLM 推理（AI 编程 Agent 核心） |
| **text-embedding-v3** | 阿里云 DashScope | 文本向量化（1024 维） |

```yaml
# application.yml 中 AI 模型配置
langchain4j:
  open-ai:
    chat-model:
      base-url: https://dashscope.aliyuncs.com/compatible-mode/v1
      api-key: ${DASHSCOPE_API_KEY}
      model-name: qwen-max
    embedding-model:
      base-url: https://dashscope.aliyuncs.com/compatible-mode/v1
      api-key: ${DASHSCOPE_API_KEY}
      model-name: text-embedding-v3
```

> **网络要求**：服务器必须能访问 `https://dashscope.aliyuncs.com`（出站 443 端口）。

---

### 3.10 环境安装总结

#### 最小安装清单（Docker Compose 单机部署）

```
宿主机安装：
  ✅ Docker Engine 24.x+
  ✅ Docker Compose V2 2.23+
  ✅ Git 2.40+

容器内自动部署（无需手动安装）：
  📦 eclipse-temurin:21-jre  — Java 运行时
  📦 redis:7-alpine          — 缓存服务
  📦 milvusdb/milvus:v2.3.0  — 向量数据库
  📦 prom/prometheus          — 监控采集
  📦 grafana/grafana          — 监控面板
  📦 nginx:alpine            — 反向代理

外部依赖（无需安装）：
  ☁️ DashScope API           — AI 模型服务
```

#### 本地开发安装清单（IDEA 开发调试）

```
开发机安装：
  ✅ JDK 17 或 21
  ✅ Maven 3.9+
  ✅ Node.js 20 LTS
  ✅ npm 9+
  ✅ Git 2.40+
  ✅ IntelliJ IDEA 2023.2+
  ✅ Redis 7.x（本地或 Docker）
  ✅ Milvus v2.3.x（Docker）
  ✅ DashScope API Key
```

#### 完整生产安装清单（K8s 集群部署）

```
集群节点安装：
  ✅ Kubernetes 1.28+（k3s 或 kubeadm）
  ✅ Docker/containerd 运行时
  ✅ Helm 3.x（可选，用于中间件 Chart）
  ✅ kubectl 1.28+

容器内自动部署：
  📦 应用 Pod（JDK 21 + Spring Boot）× 1~3 副本
  📦 Redis（Deployment + Sentinel 3 节点）
  📦 Milvus Cluster（3 节点 StatefulSet）
  📦 PostgreSQL 15（StatefulSet + PVC）
  📦 Prometheus（Deployment + PVC 10Gi）
  📦 Grafana（Deployment + PVC 1Gi）
  📦 Traefik / Nginx Ingress Controller

外部依赖：
  ☁️ DashScope API
  📜 SSL 证书（Let's Encrypt 或商业证书）
```

---

### 3.11 Docker 镜像清单

| 镜像 | 标签 | 大小(约) | 用途 | 部署方式 |
|------|------|---------|------|----------|
| `langchain4j-agent` | `latest` | ~200 MB | 应用（多阶段构建产物） | 本地构建 |
| `milvusdb/milvus` | `v2.3.0` | ~1.2 GB | 向量数据库 | 自动拉取 |
| `redis` | `7-alpine` | ~30 MB | 缓存 | 自动拉取 |
| `postgres` | `15-alpine` | ~230 MB | 数据库（可选） | 自动拉取 |
| `prom/prometheus` | `latest` | ~220 MB | 监控 | 自动拉取 |
| `grafana/grafana` | `latest` | ~370 MB | 可视化面板 | 自动拉取 |
| `nginx` | `alpine` | ~40 MB | 反向代理 | 自动拉取 |
| `eclipse-temurin` | `21-jre-jammy` | ~200 MB | 应用运行时基础镜像 | 构建时拉取 |
| `maven` | `3.9-eclipse-temurin-21` | ~500 MB | 应用构建阶段 | 构建时拉取 |

**离线部署需预下载镜像总大小**：约 2.5 GB

---

## 四、环境变量快速参考

完整配置模板见 `.env.production` 文件。以下为必须修改的核心变量：

| 变量 | 必填 | 说明 | 示例值 |
|------|------|------|--------|
| `DASHSCOPE_API_KEY` | **是** | 阿里云通义千问 API Key | `sk-xxxxxxxxxxxxxxxx` |
| `REDIS_PASSWORD` | **是** | Redis 访问密码 | 至少 16 字符随机串 |
| `JWT_SECRET` | **是** | JWT 签名密钥 | 至少 32 字符随机串 |
| `MINIO_ACCESS_KEY` | **是** | Milvus 内部存储凭证 | 自定义 |
| `MINIO_SECRET_KEY` | **是** | Milvus 内部存储凭证 | 自定义 |
| `GRAFANA_ADMIN_PASSWORD` | 建议 | Grafana 管理员密码 | 至少 12 字符 |
| `SPRING_DATASOURCE_*` | 可选 | MySQL 连接信息（如不用 H2） | — |

**生成安全随机密钥**：
```bash
# 生成 JWT Secret（64字符）
openssl rand -hex 32

# 生成 Redis 密码（32字符）
openssl rand -base64 24

# 生成 MinIO 密钥
openssl rand -hex 16
```

---

## 五、快速部署步骤

### 5.1 Docker Compose 部署（推荐入门）

```bash
# 1. 准备配置文件
cp .env.production .env
vim .env  # 修改所有 CHANGE_ME 标记

# 2. 构建应用镜像
docker build -t langchain4j-agent:latest .

# 3. 启动全部服务（不含 Nginx）
docker compose up -d

# 4. 启动含 Nginx 的生产配置
docker compose --profile production up -d

# 5. 验证服务状态
docker compose ps
curl http://localhost:8080/actuator/health
```

### 5.2 Kubernetes 部署

```bash
# 1. 创建命名空间
kubectl apply -f k8s/namespace.yaml

# 2. 创建配置（修改 secret.yaml 中的 base64 值）
kubectl apply -f k8s/secret.yaml
kubectl apply -f k8s/configmap.yaml

# 3. 部署中间件
kubectl apply -f k8s/redis.yaml
kubectl apply -f k8s/milvus.yaml

# 4. 部署应用
kubectl apply -f k8s/pvc.yaml
kubectl apply -f k8s/service.yaml
kubectl apply -f k8s/deployment.yaml

# 5. 配置 Ingress
kubectl apply -f k8s/ingress.yaml

# 6. 验证
kubectl -n langchain4j-agent get pods
kubectl -n langchain4j-agent get svc
```

---

## 六、健康检查与监控端点

| 端点 | 方法 | 说明 |
|------|------|------|
| `/actuator/health` | GET | 应用健康状态 |
| `/actuator/health/liveness` | GET | 存活探针（K8s） |
| `/actuator/health/readiness` | GET | 就绪探针（K8s） |
| `/actuator/prometheus` | GET | Prometheus 指标 |
| `/actuator/info` | GET | 应用信息 |
| `/actuator/metrics` | GET | 所有度量指标 |
| `http://redis:6379` | PING | Redis 连通性 |
| `http://milvus:9091/healthz` | GET | Milvus 健康状态 |

---

## 七、容量规划参考

### 7.1 并发用户与资源关系

| 并发用户数 | 应用副本 | JVM 堆 | Milvus 内存 | Redis 内存 | 总 CPU | 总内存 |
|-----------|---------|--------|------------|-----------|--------|-------|
| 1~10 | 1 | 1 GB | 2 GB | 256 MB | 4 核 | 8 GB |
| 10~50 | 2 | 1.5 GB | 4 GB | 512 MB | 8 核 | 16 GB |
| 50~200 | 3 | 2 GB | 8 GB | 1 GB | 16 核 | 32 GB |
| 200~500 | 5+ | 3 GB | 16 GB | 2 GB | 32 核 | 64 GB |

### 7.2 向量存储容量

| 文档数量 | 向量维度 | 索引类型 | 预估磁盘 | Milvus 内存 |
|---------|---------|---------|---------|------------|
| 1 万 | 1024 | HNSW | ~200 MB | ~500 MB |
| 10 万 | 1024 | HNSW | ~2 GB | ~4 GB |
| 100 万 | 1024 | HNSW | ~20 GB | ~16 GB |
| 1000 万 | 1024 | IVF_FLAT | ~200 GB | ~32 GB |

---

## 八、生产安全检查清单

- [ ] 所有 `CHANGE_ME` 值已替换为安全随机值
- [ ] JWT_SECRET 至少 32 字符且不可猜测
- [ ] Redis 密码已设置且非默认
- [ ] Milvus MinIO 凭证已修改
- [ ] Grafana 管理员密码已更改
- [ ] Nginx SSL 证书已配置（HTTPS）
- [ ] H2 控制台已禁用（`h2.console.enabled=false`）
- [ ] 日志级别为 INFO 或 WARN（非 DEBUG）
- [ ] API 限流已启用
- [ ] 数据库已切换为 MySQL/PostgreSQL（非 H2）
- [ ] Docker 镜像使用固定版本号（非 latest 浮动标签）
- [ ] 定期备份策略已配置（数据库 + Milvus + Redis）
