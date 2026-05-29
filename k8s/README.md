# k3s 部署指南

本项目支持在 k3s 集群中部署 LangChain4j AI Agent 应用。

## 📁 文件结构

```
├── Dockerfile.k3s                  # k3s 优化的 Dockerfile
├── build-k3s-image.ps1             # Windows 构建脚本
├── build-k3s-image.sh              # Linux/Mac 构建脚本
├── deploy-to-k3s.sh                # k3s 部署脚本
├── K3S_DEPLOYMENT.md               # 详细部署文档
├── K3S_QUICK_REFERENCE.md          # 快速参考
└── k8s/
    ├── deployment-k3s.yaml         # k3s 专用部署配置
    ├── namespace.yaml              # 命名空间
    ├── configmap.yaml              # 配置
    ├── secret.yaml                 # 密钥
    ├── service.yaml                # 服务
    ├── pvc.yaml                    # 持久化存储
    ├── ingress-traefik.yaml        # Traefik Ingress
    ├── ingress.yaml                # 标准 Ingress
    ├── hpa.yaml                    # 自动伸缩
    ├── pdb.yaml                    # Pod 中断预算
    ├── redis.yaml                  # Redis
    ├── milvus.yaml                 # Milvus
    └── ...                         # 其他中间件
```

## 🚀 快速开始

### 前置条件

1. ✅ k3s 集群已安装并运行
2. ✅ Docker 已安装
3. ✅ kubectl 已配置并连接到 k3s
4. ✅ Maven 已安装（Java 17+）

### 步骤 1: 构建 Docker 镜像

**Windows (PowerShell):**
```powershell
.\build-k3s-image.ps1
```

**Linux/Mac (Bash):**
```bash
chmod +x build-k3s-image.sh
./build-k3s-image.sh
```

构建脚本会：
- 检查 Docker 环境
- 清理旧的构建产物
- 使用 Maven 构建 JAR 包
- 构建 Docker 镜像（`langchain4j-agent-demo:v1.0.0`）
- 询问是否保存为 tar 文件

### 步骤 2: 导入镜像到 k3s

```bash
# 使用 k3s ctr 导入
sudo k3s ctr images import langchain4j-agent-demo-v1.0.0.tar

# 验证镜像
sudo k3s ctr images ls | grep langchain4j-agent-demo
```

### 步骤 3: 配置 Secret

编辑 `k8s/secret.yaml`，修改以下敏感信息：

```yaml
data:
  DASHSCOPE_API_KEY: <你的通义千问 API Key (Base64)>
  JWT_SECRET: <你的 JWT 密钥 (Base64)>
  REDIS_PASSWORD: <你的 Redis 密码 (Base64)>
```

**生成 Base64:**
```bash
echo -n "your-api-key" | base64
```

### 步骤 4: 部署应用

**自动部署（推荐）:**
```bash
chmod +x deploy-to-k3s.sh
./deploy-to-k3s.sh
```

**手动部署:**
```bash
# 1. 创建命名空间
kubectl apply -f k8s/namespace.yaml

# 2. 创建配置
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/secret.yaml

# 3. 创建 PVC
kubectl apply -f k8s/pvc.yaml

# 4. 部署中间件（可选）
kubectl apply -f k8s/redis.yaml
kubectl apply -f k8s/milvus.yaml

# 5. 部署应用
kubectl apply -f k8s/deployment-k3s.yaml
kubectl apply -f k8s/service.yaml

# 6. 部署 Ingress（可选）
kubectl apply -f k8s/ingress-traefik.yaml
```

## ✅ 验证部署

### 检查 Pod 状态

```bash
kubectl get pods -n ai-agent
kubectl get pods -n ai-agent -w  # 实时监控
```

### 查看日志

```bash
kubectl logs -n ai-agent -l app=langchain4j-agent -f
```

### 访问应用

**方式 1: 端口转发**
```bash
kubectl port-forward -n ai-agent svc/langchain4j-agent 8080:8080
# 浏览器访问: http://localhost:8080
```

**方式 2: Ingress（如果已配置）**
```bash
kubectl get ingress -n ai-agent
# 使用显示的域名访问
```

### 健康检查

```bash
kubectl exec -n ai-agent <pod-name> -- wget -qO- http://localhost:8080/actuator/health
```

## 📊 架构说明

### k3s 优化特性

1. **镜像拉取策略**: `imagePullPolicy: IfNotPresent`（使用本地镜像）
2. **Java 版本**: 使用 Java 17 匹配项目要求
3. **资源优化**: 针对 k3s 轻量级特性优化资源限制
4. **健康检查**: 配置 liveness 和 readiness probe
5. **容器支持**: 启用 `-XX:+UseContainerSupport`

### 资源配置

| 资源类型 | Requests | Limits |
|---------|----------|--------|
| CPU | 500m | 2000m |
| Memory | 512Mi | 2Gi |

### 依赖服务

应用依赖以下中间件（可选部署）：

- **Redis**: 缓存和会话存储
- **Milvus**: 向量数据库（用于 RAG）
- **PostgreSQL**: 生产数据库（可选）

## 🔧 常用操作

### 更新应用

```bash
# 1. 构建新镜像
./build-k3s-image.sh

# 2. 导入新镜像
sudo k3s ctr images import langchain4j-agent-demo-v1.0.1.tar

# 3. 重启 Pod
kubectl rollout restart deployment/langchain4j-agent -n ai-agent

# 4. 查看更新状态
kubectl rollout status deployment/langchain4j-agent -n ai-agent
```

### 扩缩容

```bash
# 手动扩容到 3 个副本
kubectl scale deployment/langchain4j-agent --replicas=3 -n ai-agent

# 启用自动伸缩
kubectl apply -f k8s/hpa.yaml
```

### 查看资源使用

```bash
kubectl top pods -n ai-agent
kubectl top nodes
```

### 调试

```bash
# 进入 Pod
kubectl exec -it <pod-name> -n ai-agent -- sh

# 查看 Pod 详情
kubectl describe pod <pod-name> -n ai-agent

# 查看事件
kubectl get events -n ai-agent --sort-by='.lastTimestamp'
```

## 🐛 故障排查

### Pod 无法启动

```bash
# 查看 Pod 状态
kubectl get pods -n ai-agent

# 查看详细信息
kubectl describe pod <pod-name> -n ai-agent

# 查看日志
kubectl logs <pod-name> -n ai-agent
```

### 镜像拉取失败

```bash
# 检查镜像是否存在
sudo k3s ctr images ls | grep langchain4j-agent-demo

# 检查 imagePullPolicy
kubectl get deployment langchain4j-agent -n ai-agent -o yaml | grep imagePullPolicy
# 应该是 IfNotPresent
```

### 健康检查失败

```bash
# 查看应用启动日志
kubectl logs <pod-name> -n ai-agent | grep "Started"

# 手动检查健康端点
kubectl exec -it <pod-name> -n ai-agent -- wget -qO- http://localhost:8080/actuator/health
```

更多故障排查方法请参考 [K3S_QUICK_REFERENCE.md](K3S_QUICK_REFERENCE.md)

## 📚 更多文档

- [K3S_DEPLOYMENT.md](K3S_DEPLOYMENT.md) - 详细部署文档
- [K3S_QUICK_REFERENCE.md](K3S_QUICK_REFERENCE.md) - 快速参考手册
- [PRODUCTION_CHECKLIST.md](PRODUCTION_CHECKLIST.md) - 生产环境检查清单

## ⚙️ 环境变量

### ConfigMap

非敏感配置通过 ConfigMap 注入：
- Spring Profile
- 数据库连接
- Redis/Milvus 地址
- 日志级别

### Secret

敏感信息通过 Secret 注入：
- API Keys
- 密码
- JWT 密钥

## 🔒 安全建议

1. **修改默认密码**: 所有默认密码必须修改
2. **使用 Secret**: 敏感信息不要硬编码
3. **HTTPS**: 生产环境启用 TLS
4. **网络策略**: 配置 NetworkPolicy 限制访问
5. **RBAC**: 配置合适的权限控制

## 📈 监控

### Prometheus 指标

```bash
kubectl port-forward -n ai-agent svc/langchain4j-agent 8080:8080
curl http://localhost:8080/actuator/prometheus
```

### Grafana 面板

部署 Grafana 后访问监控面板：
```bash
kubectl port-forward -n ai-agent svc/grafana 3000:3000
# http://localhost:3000
# 用户名/密码: admin/admin123
```

## 🗑️ 清理

```bash
# 删除命名空间（会删除所有资源）
kubectl delete namespace ai-agent

# 或删除特定资源
kubectl delete -f k8s/deployment-k3s.yaml -n ai-agent
```

## 💡 提示

1. **本地开发**: 可以使用 `docker-compose.yml` 进行本地开发
2. **生产环境**: 建议使用私有镜像仓库存储镜像
3. **CI/CD**: 可以集成到 GitLab CI、GitHub Actions 等
4. **备份**: 定期备份 PVC 数据和配置

## 🤝 贡献

如有问题或建议，请提 Issue 或 PR。

## 📄 许可证

见项目根目录 LICENSE 文件。
