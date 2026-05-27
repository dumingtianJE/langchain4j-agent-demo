# Kubernetes 生产环境部署指南

## 📋 目录

- [架构概览](#架构概览)
- [前置条件](#前置条件)
- [快速部署](#快速部署)
- [配置说明](#配置说明)
- [高级配置](#高级配置)
- [监控与运维](#监控与运维)
- [故障排查](#故障排查)
- [最佳实践](#最佳实践)

---

## 架构概览

### K8s 资源清单

```
k8s/
├── namespace.yaml          # 命名空间
├── configmap.yaml          # 应用配置
├── secret.yaml             # 敏感信息
├── pvc.yaml                # 持久化存储
├── redis.yaml              # Redis 缓存服务
├── milvus.yaml             # Milvus 向量数据库
├── deployment.yaml         # 应用部署
├── service.yaml            # 服务暴露
├── ingress.yaml            #  ingress 路由
├── hpa.yaml                # 水平自动扩缩容
└── pdb.yaml                # Pod 中断预算
```

### 服务架构

```
┌─────────────────────────────────────────────────┐
│              Kubernetes Cluster                  │
│                                                   │
│  ┌──────────────────────────────────────────┐  │
│  │         Ingress (Nginx)                   │  │
│  │    langchain4j-agent.your-domain.com      │  │
│  └──────────────┬───────────────────────────┘  │
│                 │                                │
│  ┌──────────────▼───────────────────────────┐  │
│  │     langchain4j-agent-service (8080)      │  │
│  └──────────────┬───────────────────────────┘  │
│                 │                                │
│  ┌──────────────▼───────────────────────────┐  │
│  │     LangChain4j Agent Pods (2-10)         │  │
│  │  - JVM 优化                              │  │
│  │  - 健康检查                              │  │
│  │  - 资源限制                              │  │
│  └──┬──────────┬──────────────┬───────────┘  │
│     │          │              │               │
│  ┌──▼──┐  ┌───▼──┐  ┌──────▼───────┐       │
│  │Redis│  │Milvus│  │  PVC Storage │       │
│  └─────┘  └──────┘  └──────────────┘       │
│                                               │
│  ┌───────────────────────────────────────┐  │
│  │  HPA (自动扩缩容 2-10 Pods)           │  │
│  │  - CPU > 70% 扩容                     │  │
│  │  - Memory > 80% 扩容                  │  │
│  │  - QPS > 1000 扩容                    │  │
│  └───────────────────────────────────────┘  │
│                                               │
│  ┌───────────────────────────────────────┐  │
│  │  PDB (Pod 中断预算)                   │  │
│  │  - 最小可用 Pod: 1                    │  │
│  └───────────────────────────────────────┘  │
─────────────────────────────────────────────┘
```

---

## 前置条件

### 必需工具

- ✅ Kubernetes 集群（v1.24+）
- ✅ kubectl 命令行工具
- ✅ Docker（构建镜像）
- ✅ Helm（可选，用于安装 Nginx Ingress）

### 推荐的 K8s 集群

| 组件 | 配置 | 说明 |
|------|------|------|
| Master 节点 | 2 核 4GB | 控制平面 |
| Worker 节点 | 4 核 8GB × 3 | 工作节点 |
| 存储 | 100GB+ | PVC 持久化存储 |
| 网络 | CNI 插件 | Flannel/Calico |

### 安装 Nginx Ingress Controller

```bash
# 使用 Helm 安装
helm repo add ingress-nginx https://kubernetes.github.io/ingress-nginx
helm repo update
helm install ingress-nginx ingress-nginx/ingress-nginx \
  --namespace ingress-nginx \
  --create-namespace
```

---

## 快速部署

### 步骤 1：准备配置

#### 1.1 修改 Secret

```bash
# 编辑 Secret 文件
vi k8s/secret.yaml

# 替换以下值为 base64 编码后的值：
# - DASHSCOPE_API_KEY
# - REDIS_PASSWORD
# - JWT_SECRET
```

**如何生成 base64 编码**：

```bash
# Linux/Mac
echo -n 'your-api-key' | base64

# Windows PowerShell
[System.Convert]::ToBase64String([System.Text.Encoding]::UTF8.GetBytes('your-api-key'))
```

#### 1.2 修改 Ingress 域名

```bash
# 编辑 Ingress 文件
vi k8s/ingress.yaml

# 替换域名
langchain4j-agent.your-domain.com
```

### 步骤 2：构建镜像

```bash
# 构建 Docker 镜像
docker build -t langchain4j-agent:latest .

# 推送到镜像仓库（生产环境推荐）
docker tag langchain4j-agent:latest your-registry/langchain4j-agent:latest
docker push your-registry/langchain4j-agent:latest
```

### 步骤 3：一键部署

#### Linux/Mac

```bash
# 赋予执行权限
chmod +x deploy.sh

# 部署
./deploy.sh deploy
```

#### Windows

```powershell
# 部署
.\deploy.ps1 -Action deploy
```

### 步骤 4：验证部署

```bash
# 查看 Pod 状态
kubectl get pods -n langchain4j-agent

# 查看 Service
kubectl get svc -n langchain4j-agent

# 查看 Ingress
kubectl get ingress -n langchain4j-agent

# 查看 HPA
kubectl get hpa -n langchain4j-agent

# 查看日志
kubectl logs -f -n langchain4j-agent -l app=langchain4j-agent
```

### 步骤 5：访问应用

```bash
# 通过 Ingress 访问
curl https://langchain4j-agent.your-domain.com/api/ai/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "你好"}'

# 或者通过 Service 端口转发
kubectl port-forward -n langchain4j-agent svc/langchain4j-agent-service 8080:8080
curl http://localhost:8080/actuator/health
```

---

## 配置说明

### 资源配置

| 资源 | 类型 | 名称 | 说明 |
|------|------|------|------|
| Namespace | 命名空间 | langchain4j-agent | 资源隔离 |
| ConfigMap | 配置 | app-config | 非敏感配置 |
| Secret | 密钥 | app-secret | 敏感信息 |
| Deployment | 部署 | langchain4j-agent | 应用部署 |
| Service | 服务 | langchain4j-agent-service | 内部访问 |
| Ingress | 路由 | langchain4j-agent-ingress | 外部访问 |
| HPA | 扩缩容 | langchain4j-agent-hpa | 自动扩缩容 |
| PDB | 中断预算 | langchain4j-agent-pdb | 高可用保障 |
| PVC | 存储 | app-data-pvc, app-logs-pvc | 持久化存储 |

### 环境变量

#### ConfigMap (k8s/configmap.yaml)

```yaml
SPRING_PROFILES_ACTIVE: "production"
SPRING_DATASOURCE_URL: "jdbc:h2:file:/app/data/ai-agent-db"
SPRING_REDIS_HOST: "redis-service"
AI_MILVUS_HOST: "milvus-service"
JAVA_OPTS: "-Xms512m -Xmx1024m ..."
```

#### Secret (k8s/secret.yaml)

```yaml
DASHSCOPE_API_KEY: <base64>
REDIS_PASSWORD: <base64>
JWT_SECRET: <base64>
```

### 健康检查

```yaml
# 存活探针（Liveness Probe）
livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8080
  initialDelaySeconds: 60
  periodSeconds: 10

# 就绪探针（Readiness Probe）
readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 8080
  initialDelaySeconds: 30
  periodSeconds: 10

# 启动探针（Startup Probe）
startupProbe:
  httpGet:
    path: /actuator/health
    port: 8080
  initialDelaySeconds: 30
  periodSeconds: 10
  failureThreshold: 30  # 最多等待 5 分钟
```

### 资源限制

```yaml
resources:
  requests:
    cpu: "1"
    memory: 1Gi
  limits:
    cpu: "2"
    memory: 2Gi
```

### HPA 自动扩缩容

```yaml
minReplicas: 2          # 最小 2 个 Pod
maxReplicas: 10         # 最大 10 个 Pod
metrics:
  - CPU > 70%           # 触发扩容
  - Memory > 80%        # 触发扩容
  - QPS > 1000          # 触发扩容
```

---

## 高级配置

### 1. 滚动更新策略

```yaml
strategy:
  type: RollingUpdate
  rollingUpdate:
    maxSurge: 1           # 最多额外 1 个 Pod
    maxUnavailable: 0     # 不允许不可用 Pod
```

### 2. Pod 反亲和性

```yaml
affinity:
  podAntiAffinity:
    preferredDuringSchedulingIgnoredDuringExecution:
      - weight: 100
        podAffinityTerm:
          labelSelector:
            matchExpressions:
              - key: app
                operator: In
                values:
                  - langchain4j-agent
          topologyKey: kubernetes.io/hostname
```

### 3. TLS 证书配置

#### 方式 1：手动配置

```bash
# 创建 TLS Secret
kubectl create secret tls langchain4j-agent-tls \
  --cert=path/to/tls.crt \
  --key=path/to/tls.key \
  -n langchain4j-agent
```

#### 方式 2：使用 cert-manager（自动）

```bash
# 安装 cert-manager
kubectl apply -f https://github.com/cert-manager/cert-manager/releases/download/v1.13.0/cert-manager.yaml

# 创建 ClusterIssuer
cat <<EOF | kubectl apply -f -
apiVersion: cert-manager.io/v1
kind: ClusterIssuer
metadata:
  name: letsencrypt-prod
spec:
  acme:
    server: https://acme-v02.api.letsencrypt.org/directory
    email: your-email@example.com
    privateKeySecretRef:
      name: letsencrypt-prod
    solvers:
      - http01:
          ingress:
            class: nginx
EOF

# 修改 Ingress 注解
# cert-manager.io/cluster-issuer: "letsencrypt-prod"
```

### 4. 使用外部数据库

#### PostgreSQL

```yaml
# 修改 ConfigMap
SPRING_DATASOURCE_URL: "jdbc:postgresql://postgresql:5432/agent_db"
SPRING_DATASOURCE_USERNAME: "admin"
SPRING_DATASOURCE_PASSWORD: "your-password"

# 添加 PostgreSQL Deployment（或使用云服务）
```

### 5. 使用外部 Redis

```yaml
# 修改 ConfigMap
SPRING_REDIS_HOST: "redis.your-domain.com"
SPRING_REDIS_PASSWORD: "your-password"
```

### 6. 日志聚合

#### 使用 EFK Stack

```bash
# 安装 Elasticsearch
helm install elasticsearch elastic/elasticsearch \
  --namespace logging --create-namespace

# 安装 Fluentd
helm install fluentd fluent/fluentd \
  --namespace logging

# 安装 Kibana
helm install kibana elastic/kibana \
  --namespace logging
```

---

## 监控与运维

### 1. 常用命令

```bash
# 查看所有资源
kubectl get all -n langchain4j-agent

# 查看 Pod 详情
kubectl describe pod <pod-name> -n langchain4j-agent

# 查看日志
kubectl logs -f <pod-name> -n langchain4j-agent

# 进入 Pod
kubectl exec -it <pod-name> -n langchain4j-agent -- /bin/sh

# 端口转发
kubectl port-forward -n langchain4j-agent svc/langchain4j-agent-service 8080:8080
```

### 2. 监控指标

```bash
# 查看 HPA 状态
kubectl get hpa -n langchain4j-agent -w

# 查看 Pod 资源使用
kubectl top pods -n langchain4j-agent

# 查看节点资源使用
kubectl top nodes
```

### 3. 扩缩容

#### 手动扩缩容

```bash
# 扩容到 5 个 Pod
kubectl scale deployment langchain4j-agent \
  --replicas=5 \
  -n langchain4j-agent
```

#### 自动扩缩容（HPA）

HPA 会根据配置自动扩缩容，无需手动干预。

### 4. 滚动更新

```bash
# 更新镜像
kubectl set image deployment/langchain4j-agent \
  app=langchain4j-agent:v2 \
  -n langchain4j-agent

# 监控更新进度
kubectl rollout status deployment/langchain4j-agent -n langchain4j-agent

# 查看更新历史
kubectl rollout history deployment/langchain4j-agent -n langchain4j-agent

# 回滚到上一个版本
kubectl rollout undo deployment/langchain4j-agent -n langchain4j-agent

# 回滚到指定版本
kubectl rollout undo deployment/langchain4j-agent \
  -n langchain4j-agent \
  --to-revision=2
```

### 5. 备份与恢复

```bash
# 备份 PVC 数据
kubectl run backup-pod --rm -it \
  --image=busybox \
  --namespace=langchain4j-agent \
  --command -- sh

# 在 Pod 内执行
tar czf /tmp/backup.tar.gz /app/data
kubectl cp langchain4j-agent/backup-pod:/tmp/backup.tar.gz ./backup.tar.gz

# 恢复数据
kubectl cp ./backup.tar.gz langchain4j-agent/<pod-name>:/tmp/backup.tar.gz
kubectl exec -it <pod-name> -n langchain4j-agent -- \
  tar xzf /tmp/backup.tar.gz -C /app/data
```

---

## 故障排查

### 1. Pod 无法启动

```bash
# 查看 Pod 状态
kubectl get pods -n langchain4j-agent

# 查看 Pod 事件
kubectl describe pod <pod-name> -n langchain4j-agent

# 查看日志
kubectl logs <pod-name> -n langchain4j-agent

# 常见问题：
# - ImagePullBackOff: 镜像拉取失败
# - CrashLoopBackOff: 应用启动失败
# - Pending: 资源不足
```

### 2. 健康检查失败

```bash
# 查看健康检查配置
kubectl describe pod <pod-name> -n langchain4j-agent | grep -A 10 Liveness

# 手动测试健康检查
kubectl exec -it <pod-name> -n langchain4j-agent -- \
  wget -qO- http://localhost:8080/actuator/health
```

### 3. 服务无法访问

```bash
# 检查 Service
kubectl get svc -n langchain4j-agent

# 检查 Endpoints
kubectl get endpoints -n langchain4j-agent

# 检查 Ingress
kubectl get ingress -n langchain4j-agent

# 测试 Service
kubectl run test-pod --rm -it \
  --image=curlimages/curl \
  --namespace=langchain4j-agent \
  --command -- curl -s http://langchain4j-agent-service:8080/actuator/health
```

### 4. 资源不足

```bash
# 查看节点资源
kubectl top nodes

# 查看 Pod 资源
kubectl top pods -n langchain4j-agent

# 调整资源限制
# 编辑 deployment.yaml，修改 resources 字段
kubectl apply -f k8s/deployment.yaml
```

### 5. PVC 问题

```bash
# 查看 PVC 状态
kubectl get pvc -n langchain4j-agent

# 查看 PV 状态
kubectl get pv

# 查看 StorageClass
kubectl get storageclass
```

---

## 最佳实践

### 1. 安全加固

- ✅ 使用 Secret 管理敏感信息
- ✅ 配置 NetworkPolicy 限制网络访问
- ✅ 使用 RBAC 控制权限
- ✅ 启用 PodSecurityPolicy
- ✅ 定期更新镜像

### 2. 高可用

- ✅ 至少 2 个 Pod 副本
- ✅ 配置 Pod 反亲和性
- ✅ 配置 PDB
- ✅ 多可用区部署
- ✅ 定期备份数据

### 3. 性能优化

- ✅ 配置资源请求和限制
- ✅ 使用 HPA 自动扩缩容
- ✅ 优化 JVM 参数
- ✅ 使用本地存储
- ✅ 配置连接池

### 4. 监控告警

- ✅ 配置 Prometheus 监控
- ✅ 配置 Grafana 面板
- ✅ 配置告警规则
- ✅ 配置日志聚合
- ✅ 配置分布式追踪

### 5. CI/CD 集成

```yaml
# GitHub Actions 示例
name: Deploy to Kubernetes

on:
  push:
    branches: [main]

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Build Docker image
        run: docker build -t langchain4j-agent:${{ github.sha }} .
      
      - name: Push to registry
        run: |
          docker tag langchain4j-agent:${{ github.sha }} your-registry/langchain4j-agent:${{ github.sha }}
          docker push your-registry/langchain4j-agent:${{ github.sha }}
      
      - name: Deploy to K8s
        run: |
          kubectl set image deployment/langchain4j-agent \
            app=your-registry/langchain4j-agent:${{ github.sha }} \
            -n langchain4j-agent
```

---

## 清理资源

```bash
# 删除所有资源
./deploy.sh delete

# 或者手动删除
kubectl delete -f k8s/ingress.yaml
kubectl delete -f k8s/pdb.yaml
kubectl delete -f k8s/hpa.yaml
kubectl delete -f k8s/service.yaml
kubectl delete -f k8s/deployment.yaml
kubectl delete -f k8s/milvus.yaml
kubectl delete -f k8s/redis.yaml
kubectl delete -f k8s/pvc.yaml
kubectl delete -f k8s/secret.yaml
kubectl delete -f k8s/configmap.yaml
kubectl delete -f k8s/namespace.yaml
```

---

## 常见问题 FAQ

### Q1: 如何修改 Pod 副本数？

A: 编辑 `k8s/deployment.yaml`，修改 `replicas` 字段，或使用命令：

```bash
kubectl scale deployment langchain4j-agent --replicas=3 -n langchain4j-agent
```

### Q2: 如何配置外部数据库？

A: 修改 `k8s/configmap.yaml` 中的 `SPRING_DATASOURCE_URL`。

### Q3: 如何禁用 HPA？

A: 删除 HPA 资源：

```bash
kubectl delete hpa langchain4j-agent-hpa -n langchain4j-agent
```

### Q4: 如何查看 H2 数据库文件？

A: 通过端口转发访问：

```bash
kubectl port-forward -n langchain4j-agent <pod-name> 8082:8082
# 访问 http://localhost:8082/h2-console
```

### Q5: 如何配置多个环境？

A: 使用 Kustomize 或 Helm：

```bash
# Kustomize 目录结构
k8s/
├── base/
│   ├── deployment.yaml
│   └── service.yaml
├── overlays/
│   ├── dev/
│   │   └── kustomization.yaml
│   └── prod/
│       └── kustomization.yaml
```

---

## 相关文档

- [Kubernetes 官方文档](https://kubernetes.io/docs/)
- [Spring Boot on Kubernetes](https://spring.io/guides/topicals/spring-boot-docker/)
- [Nginx Ingress Controller](https://kubernetes.github.io/ingress-nginx/)
- [HPA 文档](https://kubernetes.io/docs/tasks/run-application/horizontal-pod-autoscale/)

---

**配置完整，可以直接用于生产环境部署！** 🚀
