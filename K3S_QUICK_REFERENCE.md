# k3s 部署快速参考

## 文件说明

### Docker 相关文件
- `Dockerfile.k3s` - 针对 k3s 优化的 Dockerfile（Java 17）
- `.dockerignore` - Docker 构建排除文件
- `build-k3s-image.ps1` - Windows 构建脚本（PowerShell）
- `build-k3s-image.sh` - Linux/Mac 构建脚本（Bash）

### Kubernetes 配置文件
- `k8s/deployment-k3s.yaml` - k3s 专用部署配置
- `k8s/namespace.yaml` - 命名空间配置
- `k8s/configmap.yaml` - 应用配置
- `k8s/secret.yaml` - 敏感信息配置
- `k8s/service.yaml` - 服务配置
- `k8s/pvc.yaml` - 持久化存储配置
- `k8s/ingress-traefik.yaml` - Traefik Ingress 配置
- `k8s/ingress.yaml` - 标准 Ingress 配置
- `k8s/hpa.yaml` - 水平自动伸缩配置
- `k8s/pdb.yaml` - Pod 中断预算配置

### 中间件配置
- `k8s/redis.yaml` - Redis 单节点
- `k8s/redis-sentinel.yaml` - Redis 哨兵模式
- `k8s/milvus.yaml` - Milvus 单节点
- `k8s/milvus-cluster.yaml` - Milvus 集群
- `k8s/postgresql.yaml` - PostgreSQL
- `k8s/elasticsearch.yaml` - Elasticsearch
- `k8s/rabbitmq.yaml` - RabbitMQ

### 部署脚本
- `deploy-to-k3s.sh` - 完整部署脚本
- `deploy-middleware.sh` - 中间件部署脚本
- `deploy-traefik.sh` - Traefik 部署脚本
- `deploy-full.sh` - 完整环境部署脚本

### 文档
- `K3S_DEPLOYMENT.md` - 详细部署文档
- `K3S_QUICK_REFERENCE.md` - 本文件

## 快速开始

### 1. 构建镜像

**Windows:**
```powershell
.\build-k3s-image.ps1
```

**Linux/Mac:**
```bash
chmod +x build-k3s-image.sh
./build-k3s-image.sh
```

### 2. 导入镜像到 k3s

```bash
# 方式 1: 使用 k3s ctr
sudo k3s ctr images import langchain4j-agent-demo-v1.0.0.tar

# 方式 2: 使用 docker load
sudo docker load -i langchain4j-agent-demo-v1.0.0.tar
sudo k3s ctr images tag docker.io/library/langchain4j-agent-demo:v1.0.0 docker.io/library/langchain4j-agent-demo:v1.0.0

# 验证镜像
sudo k3s ctr images ls | grep langchain4j-agent-demo
```

### 3. 部署应用

**自动部署:**
```bash
chmod +x deploy-to-k3s.sh
./deploy-to-k3s.sh
```

**手动部署:**
```bash
# 创建命名空间
kubectl apply -f k8s/namespace.yaml

# 创建配置
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/secret.yaml

# 创建 PVC
kubectl apply -f k8s/pvc.yaml

# 部署应用
kubectl apply -f k8s/deployment-k3s.yaml
kubectl apply -f k8s/service.yaml

# 部署 Ingress（可选）
kubectl apply -f k8s/ingress-traefik.yaml
```

## 常用命令

### 查看状态

```bash
# 查看 Pod
kubectl get pods -n ai-agent

# 查看 Pod 详情
kubectl describe pod <pod-name> -n ai-agent

# 查看日志
kubectl logs -n ai-agent -l app=langchain4j-agent
kubectl logs -n ai-agent -l app=langchain4j-agent -f  # 实时

# 查看 Service
kubectl get svc -n ai-agent

# 查看 Ingress
kubectl get ingress -n ai-agent

# 查看 PVC
kubectl get pvc -n ai-agent
```

### 访问应用

```bash
# 端口转发
kubectl port-forward -n ai-agent svc/langchain4j-agent 8080:8080

# 浏览器访问: http://localhost:8080

# 或使用 Ingress 域名访问
```

### 健康检查

```bash
# 检查健康状态
kubectl exec -n ai-agent <pod-name> -- wget -qO- http://localhost:8080/actuator/health

# 检查就绪状态
kubectl get pods -n ai-agent -l app=langchain4j-agent -o jsonpath='{.items[*].status.conditions[?(@.type=="Ready")].status}'
```

### 更新应用

```bash
# 方式 1: 重新构建并导入镜像
./build-k3s-image.sh
sudo k3s ctr images import langchain4j-agent-demo-v1.0.1.tar

# 重启 Pod 使用新镜像
kubectl rollout restart deployment/langchain4j-agent -n ai-agent

# 方式 2: 直接更新镜像版本
kubectl set image deployment/langchain4j-agent \
  agent=langchain4j-agent-demo:v1.0.1 -n ai-agent

# 查看更新状态
kubectl rollout status deployment/langchain4j-agent -n ai-agent

# 回滚
kubectl rollout undo deployment/langchain4j-agent -n ai-agent
```

### 扩缩容

```bash
# 手动扩缩容
kubectl scale deployment/langchain4j-agent --replicas=3 -n ai-agent

# 启用 HPA 自动伸缩
kubectl apply -f k8s/hpa.yaml

# 查看 HPA 状态
kubectl get hpa -n ai-agent
```

### 调试

```bash
# 进入 Pod
kubectl exec -it <pod-name> -n ai-agent -- sh

# 查看 Pod 事件
kubectl get events -n ai-agent --sort-by='.lastTimestamp'

# 查看资源使用情况
kubectl top pods -n ai-agent
kubectl top nodes

# 查看配置
kubectl get configmap agent-config -n ai-agent -o yaml
kubectl get secret agent-secret -n ai-agent -o yaml
```

### 清理资源

```bash
# 删除所有资源
kubectl delete namespace ai-agent

# 或删除特定资源
kubectl delete -f k8s/deployment-k3s.yaml -n ai-agent
kubectl delete -f k8s/service.yaml -n ai-agent
```

## 环境变量配置

### ConfigMap (k8s/configmap.yaml)
非敏感配置，例如：
- SPRING_PROFILES_ACTIVE
- 数据库连接 URL
- Redis 地址
- Milvus 地址

### Secret (k8s/secret.yaml)
敏感配置，例如：
- DASHSCOPE_API_KEY
- JWT_SECRET
- REDIS_PASSWORD
- 数据库密码

**修改 Secret:**
```bash
# 编辑 Secret
kubectl edit secret agent-secret -n ai-agent

# 或删除并重新创建
kubectl delete secret agent-secret -n ai-agent
kubectl apply -f k8s/secret.yaml
```

## 存储配置

### PVC (k8s/pvc.yaml)
持久化存储卷：
- `agent-data-pvc` - 应用数据
- `agent-logs-pvc` - 应用日志

### 查看存储

```bash
# 查看 PVC
kubectl get pvc -n ai-agent

# 查看 PV
kubectl get pv

# 查看存储类
kubectl get storageclass
```

## 监控和日志

### Prometheus 指标

```bash
# 访问 Prometheus 指标
kubectl port-forward -n ai-agent svc/langchain4j-agent 8080:8080
curl http://localhost:8080/actuator/prometheus
```

### Grafana 面板

```bash
# 部署 Grafana
kubectl apply -f k8s/

# 访问 Grafana
kubectl port-forward -n ai-agent svc/grafana 3000:3000
# 浏览器访问: http://localhost:3000
# 用户名/密码: admin/admin123
```

## 故障排查

### Pod 无法启动

```bash
# 查看 Pod 状态
kubectl get pods -n ai-agent

# 查看 Pod 详情
kubectl describe pod <pod-name> -n ai-agent

# 查看日志
kubectl logs <pod-name> -n ai-agent
kubectl logs <pod-name> -n ai-agent --previous  # 上一个实例的日志
```

### 镜像拉取失败

```bash
# 检查镜像
sudo k3s ctr images ls | grep langchain4j-agent-demo

# 检查 imagePullPolicy
kubectl get deployment langchain4j-agent -n ai-agent -o yaml | grep imagePullPolicy

# 应该是 IfNotPresent（本地镜像）
```

### 健康检查失败

```bash
# 查看应用是否启动
kubectl logs <pod-name> -n ai-agent | grep "Started"

# 手动检查健康端点
kubectl exec -it <pod-name> -n ai-agent -- wget -qO- http://localhost:8080/actuator/health

# 增加健康检查延迟
kubectl edit deployment langchain4j-agent -n ai-agent
# 修改 initialDelaySeconds
```

### 数据库连接失败

```bash
# 检查数据库 Pod
kubectl get pods -n ai-agent | grep postgresql

# 检查 Service
kubectl get svc -n ai-agent

# 测试连接
kubectl exec -it <pod-name> -n ai-agent -- nc -zv postgresql 5432
```

### Redis 连接失败

```bash
# 检查 Redis Pod
kubectl get pods -n ai-agent | grep redis

# 测试连接
kubectl exec -it <pod-name> -n ai-agent -- redis-cli -h redis -a <password> ping
```

## 性能优化

### JVM 参数调整

编辑 `k8s/deployment-k3s.yaml`:
```yaml
env:
- name: JAVA_OPTS
  value: >-
    -Xms512m          # 初始堆大小
    -Xmx1024m         # 最大堆大小
    -XX:+UseG1GC      # 使用 G1 垃圾回收器
    -XX:MaxGCPauseMillis=200
    -XX:+UseContainerSupport
    -XX:MaxRAMPercentage=75.0
```

### 资源限制调整

```yaml
resources:
  requests:
    cpu: "500m"      # 请求 CPU
    memory: "512Mi"  # 请求内存
  limits:
    cpu: "2000m"     # 限制 CPU
    memory: "2Gi"    # 限制内存
```

### 副本数调整

```bash
# 修改副本数
kubectl scale deployment/langchain4j-agent --replicas=3 -n ai-agent
```

## 备份和恢复

### 备份配置

```bash
# 导出所有配置
kubectl get configmap -n ai-agent -o yaml > backup-configmaps.yaml
kubectl get secret -n ai-agent -o yaml > backup-secrets.yaml

# 导出数据（如果有 PV）
kubectl exec -it <pod-name> -n ai-agent -- tar czf /tmp/data-backup.tar.gz /app/data
kubectl cp <pod-name>:/tmp/data-backup.tar.gz ./data-backup.tar.gz -n ai-agent
```

### 恢复配置

```bash
# 导入配置
kubectl apply -f backup-configmaps.yaml
kubectl apply -f backup-secrets.yaml

# 恢复数据
kubectl cp ./data-backup.tar.gz <pod-name>:/tmp/data-backup.tar.gz -n ai-agent
kubectl exec -it <pod-name> -n ai-agent -- tar xzf /tmp/data-backup.tar.gz -C /app/data
```

## 生产环境检查清单

- [ ] 修改所有默认密码
- [ ] 配置 HTTPS（Ingress TLS）
- [ ] 启用资源监控和告警
- [ ] 配置日志集中收集
- [ ] 定期备份数据
- [ ] 配置自动伸缩（HPA）
- [ ] 配置 Pod 中断预算（PDB）
- [ ] 启用网络策略
- [ ] 测试灾难恢复
- [ ] 配置镜像拉取策略（使用私有仓库）

## 相关文档

- [详细部署文档](K3S_DEPLOYMENT.md)
- [K8s 部署指南](K8S_DEPLOYMENT.md)
- [K8s 中间件配置](K8S_MIDDLEWARE.md)
- [Traefik 部署指南](DEPLOY_WITH_TRAEFIK.md)
- [生产环境检查清单](PRODUCTION_CHECKLIST.md)
