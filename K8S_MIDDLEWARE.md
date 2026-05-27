# 生产环境中间件 K8s 部署指南

## 📋 目录

- [架构概览](#架构概览)
- [中间件清单](#中间件清单)
- [快速部署](#快速部署)
- [配置说明](#配置说明)
- [高可用方案](#高可用方案)
- [监控与运维](#监控与运维)
- [最佳实践](#最佳实践)

---

## 架构概览

### 生产环境中间件架构

```
┌─────────────────────────────────────────────────────────┐
│              Kubernetes Cluster                          │
│                                                          │
│  ┌──────────────────────────────────────────────────┐  │
│  │              应用层                                │  │
│  │  LangChain4j Agent Pods (2-10)                   │  │
│  └────┬──────────┬────────────┬─────────┬──────────┘  │
│       │          │            │         │              │
│  ┌────▼────┐ ┌──▼─────┐ ┌───▼────┐ ┌──▼──────────┐  │
│  │PostgreSQL│ │Redis   │ │Milvus  │ │  RabbitMQ   │  │
│  │ 数据库   │ │缓存    │ │向量库  │ │  消息队列   │  │
│  └─────────┘ └────────┘ └────────┘ └─────────────┘  │
│                                                          │
│  ┌──────────────────────────────────────────────────┐  │
│  │              日志系统                              │  │
│  │  Elasticsearch (3 节点集群)                       │  │
│  └──────────────────────────────────────────────────┘  │
│                                                          │
│  ┌──────────────────────────────────────────────────┐  │
│  │              存储服务                              │  │
│  │  MinIO (对象存储) / NFS / Ceph                   │  │
│  └──────────────────────────────────────────────────┘  │
────────────────────────────────────────────────────────
```

---

## 中间件清单

| 中间件 | 用途 | 部署模式 | 副本数 | 存储 |
|--------|------|----------|--------|------|
| **PostgreSQL** | 关系型数据库 | Deployment | 1 | 50Gi PVC |
| **Redis Sentinel** | 缓存 + 会话 | StatefulSet | 3 | 10Gi × 3 |
| **Milvus Cluster** | 向量数据库 | Deployment | 2-3 | 50Gi PVC |
| **RabbitMQ** | 消息队列 | StatefulSet | 3 | 10Gi × 3 |
| **Elasticsearch** | 日志搜索 | StatefulSet | 3 | 100Gi × 3 |

---

## 快速部署

### 步骤 1：选择需要的中间件

根据业务需求选择：

#### 基础方案（最小配置）

```bash
# PostgreSQL + Redis（单机）
kubectl apply -f k8s/postgresql.yaml
kubectl apply -f k8s/redis.yaml  # 已有
```

#### 标准方案（推荐）

```bash
# PostgreSQL + Redis Sentinel + Milvus
kubectl apply -f k8s/postgresql.yaml
kubectl apply -f k8s/redis-sentinel.yaml
kubectl apply -f k8s/milvus-cluster.yaml
```

#### 完整方案（生产环境）

```bash
# 所有中间件 + ELK 日志
kubectl apply -f k8s/postgresql.yaml
kubectl apply -f k8s/redis-sentinel.yaml
kubectl apply -f k8s/milvus-cluster.yaml
kubectl apply -f k8s/rabbitmq.yaml
kubectl apply -f k8s/elasticsearch.yaml
```

### 步骤 2：更新应用配置

修改 `k8s/configmap.yaml`：

```yaml
# PostgreSQL 配置
SPRING_DATASOURCE_URL: "jdbc:postgresql://postgresql-service:5432/agent_db"
SPRING_DATASOURCE_USERNAME: "admin"
SPRING_DATASOURCE_PASSWORD: "your-password"

# Redis Sentinel 配置
SPRING_REDIS_HOST: "redis-sentinel-service"
SPRING_REDIS_PORT: "26379"
SPRING_REDIS_SENTINEL_MASTER: "mymaster"

# Milvus 配置（保持不变）
AI_MILVUS_HOST: "milvus-service"
AI_MILVUS_PORT: "19530"

# RabbitMQ 配置
SPRING_RABBITMQ_HOST: "rabbitmq-service"
SPRING_RABBITMQ_PORT: "5672"
SPRING_RABBITMQ_USERNAME: "guest"
SPRING_RABBITMQ_PASSWORD: "guest"

# Elasticsearch 配置
SPRING_ELASTICSEARCH_URIS: "http://elasticsearch:9200"
```

### 步骤 3：更新 Secret

修改 `k8s/secret.yaml`：

```yaml
data:
  # PostgreSQL
  POSTGRES_USER: YWRtaW4=                    # admin (base64)
  POSTGRES_PASSWORD: eW91ci1wYXNzd29yZA==  # your-password (base64)
  
  # Redis
  REDIS_PASSWORD: cmVkaXMxMjM=             # redis123 (base64)
  
  # RabbitMQ
  RABBITMQ_ERLANG_COOKIE: c2VjcmV0LWNvb2tpZQ==  # secret-cookie (base64)
```

### 步骤 4：部署中间件

```bash
# 一键部署所有中间件
./deploy-middleware.sh deploy all

# 或单独部署
./deploy-middleware.sh deploy postgresql
./deploy-middleware.sh deploy redis
./deploy-middleware.sh deploy milvus
./deploy-middleware.sh deploy rabbitmq
./deploy-middleware.sh deploy elasticsearch
```

---

## 配置说明

### 1. PostgreSQL（关系型数据库）

#### 部署模式

```yaml
# 单机模式（适合中小规模）
replicas: 1
strategy: Recreate  # 数据库必须使用 Recreate

# 高可用模式（推荐生产环境）
# 使用 Patroni 或 CloudNativePG Operator
```

#### 性能优化

```conf
# postgresql.conf
max_connections = 200
shared_buffers = 512MB           # 物理内存的 25%
effective_cache_size = 1536MB    # 物理内存的 75%
maintenance_work_mem = 128MB
checkpoint_completion_target = 0.9
wal_buffers = 16MB
work_mem = 4MB
```

#### 使用方式

```yaml
# 应用连接配置
SPRING_DATASOURCE_URL: jdbc:postgresql://postgresql-service:5432/agent_db
SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE: "20"
SPRING_DATASOURCE_HIKARI_MINIMUM_IDLE: "5"
```

### 2. Redis Sentinel（高可用缓存）

#### 架构

```
Redis Master (1 个)
  ↓
Redis Slave (2 个)
  ↓
Redis Sentinel (3 个) - 监控和故障转移
```

#### 配置说明

```conf
# redis.conf
maxmemory 512mb
maxmemory-policy allkeys-lru    # LRU 淘汰策略
appendonly yes                   # AOF 持久化
appendfsync everysec             # 每秒同步

# sentinel.conf
sentinel monitor mymaster redis-master 6379 2
sentinel down-after-milliseconds mymaster 5000
sentinel failover-timeout mymaster 60000
```

#### 使用方式

```yaml
# Spring Boot Redis Sentinel 配置
SPRING_REDIS_SENTINEL_MASTER: "mymaster"
SPRING_REDIS_SENTINEL_NODES: "redis-sentinel-0.redis-sentinel-service:26379,redis-sentinel-1.redis-sentinel-service:26379,redis-sentinel-2.redis-sentinel-service:26379"
```

### 3. Milvus Cluster（向量数据库）

#### 架构

```
Milvus Proxy (2 个) - 负载均衡
  ↓
Milvus Query Node (2 个) - 查询服务
  ↓
Milvus Data Node (2 个) - 数据管理
  ↓
etcd (3 个) - 元数据存储
MinIO (3 个) - 对象存储
```

#### 依赖服务

需要先部署 etcd 和 MinIO：

```bash
# 部署 etcd
kubectl apply -f k8s/etcd.yaml

# 部署 MinIO
kubectl apply -f k8s/minio.yaml

# 部署 Milvus 集群
kubectl apply -f k8s/milvus-cluster.yaml
```

#### 使用方式

```yaml
# 应用连接配置
AI_EMBEDDING_TYPE: "milvus"
AI_MILVUS_HOST: "milvus-service"
AI_MILVUS_PORT: "19530"
AI_MILVUS_COLLECTION_NAME: "ai_knowledge_embeddings"
```

### 4. RabbitMQ（消息队列）

#### 架构

```
RabbitMQ Cluster (3 节点)
  ↓
Queue Mirroring (队列镜像)
  ↓
Management UI (管理界面)
```

#### 配置说明

```conf
# rabbitmq.conf
vm_memory_high_watermark.relative = 0.6    # 内存限制 60%
disk_free_limit.absolute = 2GB             # 磁盘限制
channel_max = 2047
connection_max = 1000
heartbeat = 60
```

#### 使用方式

```yaml
# Spring Boot RabbitMQ 配置
SPRING_RABBITMQ_HOST: "rabbitmq-service"
SPRING_RABBITMQ_PORT: "5672"
SPRING_RABBITMQ_USERNAME: "guest"
SPRING_RABBITMQ_PASSWORD: "guest"
SPRING_RABBITMQ_LISTENER_SIMPLE_CONCURRENCY: "5"
SPRING_RABBITMQ_LISTENER_SIMPLE_MAX_CONCURRENCY: "10"
```

#### 管理界面

```bash
# 端口转发访问管理界面
kubectl port-forward -n langchain4j-agent svc/rabbitmq-management 15672:15672

# 访问 http://localhost:15672
# 默认账号: guest / guest
```

### 5. Elasticsearch（日志系统）

#### 架构

```
Elasticsearch Cluster (3 节点)
  ↓
Master + Data 节点混合
  ↓
Headless Service (内部访问)
```

#### 配置说明

```yaml
# elasticsearch.yml
cluster.name: "elasticsearch-cluster"
discovery.seed_hosts:
  - elasticsearch-0.elasticsearch:9300
  - elasticsearch-1.elasticsearch:9300
  - elasticsearch-2.elasticsearch:9300
cluster.initial_master_nodes:
  - elasticsearch-0
  - elasticsearch-1
  - elasticsearch-2
```

#### 使用方式

```yaml
# Spring Boot Elasticsearch 配置
SPRING_ELASTICSEARCH_URIS: "http://elasticsearch:9200"

# Logstash 配置（需要额外部署）
# 将应用日志发送到 Elasticsearch
```

#### 访问方式

```bash
# 端口转发
kubectl port-forward -n langchain4j-agent svc/elasticsearch 9200:9200

# 测试集群健康
curl http://localhost:9200/_cluster/health

# 查看索引
curl http://localhost:9200/_cat/indices
```

---

## 高可用方案

### 1. PostgreSQL 高可用

#### 方案 1：Patroni（推荐）

```bash
# 使用 Helm 安装 Patroni
helm repo add postgres-operator-charts https://opensource.zalando.com/postgres-operator/charts
helm install postgres-operator postgres-operator-charts/postgres-operator

# 创建 PostgreSQL 集群
kubectl apply -f - <<EOF
apiVersion: acid.zalan.do/v1
kind: postgresql
metadata:
  name: postgresql-cluster
  namespace: langchain4j-agent
spec:
  numberOfInstances: 3
  volume:
    size: 50Gi
  postgresql:
    version: "15"
EOF
```

#### 方案 2：CloudNativePG

```bash
# 安装 CloudNativePG Operator
kubectl apply -f https://raw.githubusercontent.com/cloudnative-pg/cloudnative-pg/main/releases/cnpg-1.22.0.yaml

# 创建 PostgreSQL 集群
kubectl apply -f - <<EOF
apiVersion: postgresql.cnpg.io/v1
kind: Cluster
metadata:
  name: postgresql-cluster
  namespace: langchain4j-agent
spec:
  instances: 3
  storage:
    size: 50Gi
EOF
```

### 2. Redis 高可用

已使用 Redis Sentinel 方案，自动故障转移。

### 3. Milvus 高可用

已使用集群模式，多组件多副本。

### 4. RabbitMQ 高可用

使用 3 节点集群 + 队列镜像。

### 5. Elasticsearch 高可用

使用 3 节点集群，自动分片和副本。

---

## 监控与运维

### 1. 监控指标

```bash
# PostgreSQL
kubectl exec -n langchain4j-agent postgresql-xxx -- psql -U admin -d agent_db -c "SELECT * FROM pg_stat_activity;"

# Redis
kubectl exec -n langchain4j-agent redis-0 -- redis-cli -a $REDIS_PASSWORD info

# RabbitMQ
kubectl exec -n langchain4j-agent rabbitmq-0 -- rabbitmqctl status

# Elasticsearch
curl http://localhost:9200/_cluster/health?pretty
```

### 2. 备份与恢复

#### PostgreSQL 备份

```bash
# 备份
kubectl exec -n langchain4j-agent postgresql-xxx -- pg_dump -U admin agent_db > backup.sql

# 恢复
kubectl exec -i -n langchain4j-agent postgresql-xxx -- psql -U admin agent_db < backup.sql
```

#### Redis 备份

```bash
# 触发 RDB 快照
kubectl exec -n langchain4j-agent redis-0 -- redis-cli -a $REDIS_PASSWORD BGSAVE

# 复制 RDB 文件
kubectl cp langchain4j-agent/redis-0:/data/dump.rdb ./dump.rdb
```

#### Elasticsearch 备份

```bash
# 创建快照仓库
curl -X PUT "localhost:9200/_snapshot/my_backup" -H 'Content-Type: application/json' -d'
{
  "type": "fs",
  "settings": {
    "location": "/usr/share/elasticsearch/backups"
  }
}'

# 创建快照
curl -X PUT "localhost:9200/_snapshot/my_backup/snapshot_1?wait_for_completion=true"
```

### 3. 扩容

#### PostgreSQL 扩容（只读副本）

```bash
# 添加只读副本
kubectl scale deployment postgresql-read-replicas --replicas=3 -n langchain4j-agent
```

#### Redis 扩容

```bash
# 添加 Slave 节点
kubectl scale statefulset redis --replicas=5 -n langchain4j-agent
```

#### RabbitMQ 扩容

```bash
# 添加节点
kubectl scale statefulset rabbitmq --replicas=5 -n langchain4j-agent
```

---

## 最佳实践

### 1. 数据库

- ✅ 使用 PVC 持久化存储
- ✅ 配置定期备份策略
- ✅ 监控慢查询日志
- ✅ 使用连接池（HikariCP）
- ✅ 定期 VACUUM 和 ANALYZE

### 2. 缓存

- ✅ 使用 Sentinel 或 Cluster 模式
- ✅ 配置内存限制和淘汰策略
- ✅ 启用 AOF 持久化
- ✅ 监控内存使用率
- ✅ 定期清理过期 key

### 3. 消息队列

- ✅ 使用集群模式
- ✅ 配置队列镜像
- ✅ 监控队列深度
- ✅ 配置死信队列
- ✅ 定期清理积压消息

### 4. 向量数据库

- ✅ 使用集群模式
- ✅ 配置合适的索引类型
- ✅ 监控内存和磁盘使用
- ✅ 定期 compact 数据
- ✅ 备份 etcd 和 MinIO 数据

### 5. 日志系统

- ✅ 使用 3 节点集群
- ✅ 配置索引生命周期管理（ILM）
- ✅ 监控磁盘使用率
- ✅ 定期清理旧索引
- ✅ 配置告警规则

---

## 资源规划

### 最小配置（开发环境）

| 中间件 | CPU | 内存 | 存储 |
|--------|-----|------|------|
| PostgreSQL | 500m | 512Mi | 10Gi |
| Redis | 250m | 256Mi | 2Gi |
| Milvus | 1 | 2Gi | 10Gi |
| **总计** | **1.75 核** | **2.75Gi** | **22Gi** |

### 标准配置（生产环境）

| 中间件 | CPU | 内存 | 存储 |
|--------|-----|------|------|
| PostgreSQL | 2 | 4Gi | 50Gi |
| Redis (3) | 3 | 3Gi | 30Gi |
| Milvus | 4 | 8Gi | 100Gi |
| RabbitMQ (3) | 3 | 3Gi | 30Gi |
| Elasticsearch (3) | 6 | 12Gi | 300Gi |
| **总计** | **18 核** | **30Gi** | **510Gi** |

### 高配（大规模生产）

| 中间件 | CPU | 内存 | 存储 |
|--------|-----|------|------|
| PostgreSQL (HA) | 4 | 8Gi | 100Gi |
| Redis Cluster (6) | 6 | 6Gi | 60Gi |
| Milvus | 8 | 16Gi | 200Gi |
| RabbitMQ (5) | 5 | 5Gi | 50Gi |
| Elasticsearch (5) | 10 | 20Gi | 500Gi |
| **总计** | **33 核** | **55Gi** | **910Gi** |

---

## 故障排查

### PostgreSQL

```bash
# 查看日志
kubectl logs -n langchain4j-agent -l app=postgresql

# 检查连接
kubectl exec -n langchain4j-agent postgresql-xxx -- pg_isready

# 查看慢查询
kubectl exec -n langchain4j-agent postgresql-xxx -- psql -U admin -d agent_db -c "SELECT * FROM pg_stat_statements ORDER BY mean_time DESC LIMIT 10;"
```

### Redis

```bash
# 查看集群状态
kubectl exec -n langchain4j-agent redis-0 -- redis-cli -a $REDIS_PASSWORD cluster info

# 查看内存使用
kubectl exec -n langchain4j-agent redis-0 -- redis-cli -a $REDIS_PASSWORD info memory

# 查看慢查询
kubectl exec -n langchain4j-agent redis-0 -- redis-cli -a $REDIS_PASSWORD slowlog get 10
```

### Milvus

```bash
# 查看集群状态
kubectl exec -n langchain4j-agent milvus-proxy-xxx -- curl http://localhost:9091/healthz

# 查看日志
kubectl logs -n langchain4j-agent -l app=milvus
```

### RabbitMQ

```bash
# 查看集群状态
kubectl exec -n langchain4j-agent rabbitmq-0 -- rabbitmqctl cluster_status

# 查看队列
kubectl exec -n langchain4j-agent rabbitmq-0 -- rabbitmqctl list_queues

# 查看连接
kubectl exec -n langchain4j-agent rabbitmq-0 -- rabbitmqctl list_connections
```

### Elasticsearch

```bash
# 查看集群健康
curl http://localhost:9200/_cluster/health?pretty

# 查看节点状态
curl http://localhost:9200/_cat/nodes?v

# 查看索引状态
curl http://localhost:9200/_cat/indices?v

# 查看慢查询日志
kubectl logs -n langchain4j-agent -l app=elasticsearch
```

---

## 相关资源

- [PostgreSQL on Kubernetes](https://www.postgresql.org/docs/current/high-availability.html)
- [Redis Sentinel 文档](https://redis.io/docs/management/sentinel/)
- [Milvus 集群部署](https://milvus.io/docs/deploy_cluster.md)
- [RabbitMQ on Kubernetes](https://www.rabbitmq.com/kubernetes.html)
- [Elasticsearch on Kubernetes](https://www.elastic.co/guide/en/elasticsearch/reference/current/kubernetes.html)

---

**配置完整，可以直接用于生产环境部署！** 🚀
