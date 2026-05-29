# k3s 快速部署指南

## 前置条件

1. k3s 集群已安装并运行
2. Docker 已安装（用于构建镜像）
3. kubectl 已配置并连接到 k3s 集群

## 步骤 1: 构建 Docker 镜像

### 在 Windows 上构建

```powershell
# 运行构建脚本
.\build-k3s-image.ps1
```

### 在 Linux/Mac 上构建

```bash
# 构建镜像
docker build -f Dockerfile.k3s -t langchain4j-agent-demo:v1.0.0 .

# 保存镜像为 tar 文件
docker save -o langchain4j-agent-demo-v1.0.0.tar langchain4j-agent-demo:v1.0.0
```

## 步骤 2: 将镜像导入 k3s

### 方法 1: 使用 k3s ctr（推荐）

```bash
# 在 k3s 服务器上执行
sudo k3s ctr images import langchain4j-agent-demo-v1.0.0.tar

# 验证镜像已导入
sudo k3s ctr images ls | grep langchain4j-agent-demo
```

### 方法 2: 使用 Docker 加载

```bash
# 在 k3s 服务器上执行
sudo docker load -i langchain4j-agent-demo-v1.0.0.tar

# 标记镜像供 k3s 使用
sudo k3s ctr images tag docker.io/library/langchain4j-agent-demo:v1.0.0 docker.io/library/langchain4j-agent-demo:v1.0.0
```

### 方法 3: 推送到镜像仓库（生产环境推荐）

```bash
# 登录到镜像仓库
docker login your-registry.com

# 标记镜像
docker tag langchain4j-agent-demo:v1.0.0 your-registry.com/ai-agent/langchain4j-agent-demo:v1.0.0

# 推送镜像
docker push your-registry.com/ai-agent/langchain4j-agent-demo:v1.0.0
```

## 步骤 3: 配置 Kubernetes 资源

### 创建命名空间

```bash
kubectl apply -f k8s/namespace.yaml
```

### 创建 ConfigMap 和 Secret

```bash
# 创建配置文件
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/secret.yaml
```

**重要**: 修改 `k8s/secret.yaml` 中的敏感信息：
- DASHSCOPE_API_KEY: 你的通义千问 API Key
- JWT_SECRET: JWT 签名密钥
- REDIS_PASSWORD: Redis 密码

### 创建 PVC（持久化存储）

```bash
kubectl apply -f k8s/pvc.yaml
```

### 部署应用

```bash
# 使用 k3s 专用部署文件
kubectl apply -f k8s/deployment-k3s.yaml

# 创建 Service
kubectl apply -f k8s/service.yaml
```

### 配置 Ingress（可选）

```bash
# 使用 Traefik（k3s 默认内置）
kubectl apply -f k8s/ingress-traefik.yaml

# 或使用标准 Ingress
kubectl apply -f k8s/ingress.yaml
```

## 步骤 4: 验证部署

### 检查 Pod 状态

```bash
kubectl get pods -n ai-agent
kubectl get pods -n ai-agent -w  # 实时监控
```

### 查看 Pod 日志

```bash
# 查看所有 Pod 日志
kubectl logs -n ai-agent -l app=langchain4j-agent

# 查看特定 Pod 日志
kubectl logs -n ai-agent <pod-name>

# 实时查看日志
kubectl logs -n ai-agent -l app=langchain4j-agent -f
```

### 检查 Service

```bash
kubectl get svc -n ai-agent
kubectl describe svc langchain4j-agent -n ai-agent
```

### 测试应用

```bash
# 获取 Service 的 ClusterIP
kubectl get svc langchain4j-agent -n ai-agent

# 从集群内测试
kubectl run -it --rm test-pod --image=curlimages/curl --restart=Never -- \
  curl http://langchain4j-agent.ai-agent.svc.cluster.local:8080/actuator/health
```

## 步骤 5: 配置中间件（可选）

### 部署 Redis

```bash
kubectl apply -f k8s/redis.yaml
```

### 部署 Milvus

```bash
# 单节点版本
kubectl apply -f k8s/milvus.yaml

# 或集群版本
kubectl apply -f k8s/milvus-cluster.yaml
```

### 部署 PostgreSQL（生产环境）

```bash
kubectl apply -f k8s/postgresql.yaml
```

## 步骤 6: 监控和日志（可选）

### 部署 Prometheus 和 Grafana

```bash
kubectl apply -f k8s/
```

### 访问 Grafana

```bash
# 端口转发
kubectl port-forward -n ai-agent svc/grafana 3000:3000

# 浏览器访问 http://localhost:3000
# 默认用户名/密码: admin/admin123
```

## 故障排查

### Pod 无法启动

```bash
# 查看 Pod 详情
kubectl describe pod <pod-name> -n ai-agent

# 查看事件
kubectl get events -n ai-agent --sort-by='.lastTimestamp'
```

### 镜像拉取失败

```bash
# 检查镜像是否存在
sudo k3s ctr images ls | grep langchain4j-agent-demo

# 检查 imagePullPolicy
kubectl get deployment langchain4j-agent -n ai-agent -o yaml | grep imagePullPolicy
```

### 健康检查失败

```bash
# 查看健康检查端点
kubectl exec -it <pod-name> -n ai-agent -- wget -qO- http://localhost:8080/actuator/health

# 查看应用日志
kubectl logs <pod-name> -n ai-agent --tail=100
```

## 更新部署

### 更新镜像

```bash
# 方式 1: 重新构建并导入镜像
.\build-k3s-image.ps1
sudo k3s ctr images import langchain4j-agent-demo-v1.0.1.tar

# 方式 2: 更新 deployment
kubectl set image deployment/langchain4j-agent \
  agent=langchain4j-agent-demo:v1.0.1 -n ai-agent

# 方式 3: 编辑 deployment
kubectl edit deployment/langchain4j-agent -n ai-agent
```

### 滚动更新

```bash
# 查看更新状态
kubectl rollout status deployment/langchain4j-agent -n ai-agent

# 查看更新历史
kubectl rollout history deployment/langchain4j-agent -n ai-agent

# 回滚到上一个版本
kubectl rollout undo deployment/langchain4j-agent -n ai-agent
```

## 清理资源

```bash
# 删除所有资源
kubectl delete namespace ai-agent

# 或删除特定资源
kubectl delete -f k8s/deployment-k3s.yaml
kubectl delete -f k8s/service.yaml
kubectl delete -f k8s/configmap.yaml
kubectl delete -f k8s/secret.yaml
```

## 性能优化建议

1. **JVM 参数**: 根据节点资源调整 JAVA_OPTS
2. **资源限制**: 合理设置 requests 和 limits
3. **HPA**: 启用水平自动伸缩（见 k8s/hpa.yaml）
4. **PDB**: 配置 Pod 中断预算（见 k8s/pdb.yaml）
5. **镜像拉取策略**: 本地镜像使用 IfNotPresent，远程仓库使用 Always

## 生产环境检查清单

- [ ] 修改所有默认密码
- [ ] 配置 HTTPS（Ingress TLS）
- [ ] 启用网络策略
- [ ] 配置资源监控和告警
- [ ] 配置日志集中收集
- [ ] 定期备份数据
- [ ] 配置自动伸缩
- [ ] 测试灾难恢复
