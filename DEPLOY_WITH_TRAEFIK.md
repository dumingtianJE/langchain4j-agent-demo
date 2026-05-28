# langchain4j-agent Traefik 部署指南

## 概述

本指南介绍如何使用 k3s 自带的 Traefik Ingress Controller 部署 langchain4j-agent 应用。

**优势**：
- ✅ 无需额外安装 Ingress Controller
- ✅ 不需要拉取外部镜像
- ✅ k3s 官方支持，稳定可靠
- ✅ 功能完整，满足所有需求

---

## 前置条件

1. ✅ k3s 已安装并运行
2. ✅ Traefik Pod 正常运行
3. ✅ 已构建 Docker 镜像
4. ✅ 已配置 API Key 和密码

---

## 部署步骤

### 1. 验证 Traefik 状态

```bash
# SSH 登录云服务器
ssh root@106.12.23.114

# 检查 Traefik 是否运行
sudo kubectl get pods -n kube-system | grep traefik

# 预期输出：
# traefik-xxxxxxxxxx-xxxxx   1/1   Running   0   24m

# 查看 Traefik Service
sudo kubectl get svc -n kube-system traefik

# 预期输出：
# NAME      TYPE           CLUSTER-IP    EXTERNAL-IP   PORT(S)                      AGE
# traefik   LoadBalancer   10.43.x.x     192.168.0.2   80:31992/TCP,443:30558/TCP   24m
```

### 2. 准备配置文件

#### 2.1 修改 Secret

编辑 `k8s/secret.yaml`，填入实际的密钥：

```bash
# 本地电脑操作
vim k8s/secret.yaml

# 需要修改的内容：
# 1. DASHSCOPE_API_KEY: 您的阿里云 API Key（Base64 编码）
# 2. REDIS_PASSWORD: Redis 密码（Base64 编码）
```

**Base64 编码方法**：

```bash
# Linux/Mac
echo -n "your-api-key" | base64

# Windows PowerShell
[System.Convert]::ToBase64String([System.Text.Encoding]::UTF8.GetBytes("your-api-key"))
```

#### 2.2 修改镜像地址

编辑 `k8s/deployment.yaml`，设置正确的镜像地址：

```yaml
spec:
  containers:
    - name: app
      image: langchain4j-agent:latest  # 修改为实际镜像地址
```

如果使用阿里云镜像仓库：

```yaml
image: registry.cn-hangzhou.aliyuncs.com/your-namespace/langchain4j-agent:latest
```

### 3. 上传配置文件到服务器

```bash
# 本地电脑操作

# 方式 1：使用 scp
scp -r k8s/ root@106.12.23.114:/root/langchain4j-agent/

# 方式 2：使用 WinSCP / FileZilla
# 上传整个 k8s 目录到服务器的 /root/langchain4j-agent/
```

### 4. 部署应用

```bash
# SSH 登录服务器
ssh root@106.12.23.114

# 进入项目目录
cd /root/langchain4j-agent

# 赋予脚本执行权限
chmod +x deploy-traefik.sh

# 部署应用
sudo ./deploy-traefik.sh
```

### 5. 验证部署

```bash
# 查看 Pod 状态
sudo kubectl get pods -n langchain4j-agent

# 预期输出：
# NAME                                  READY   STATUS    RESTARTS   AGE
# langchain4j-agent-xxxxxxxxxx-xxxxx   1/1     Running   0          2m

# 查看 Service
sudo kubectl get svc -n langchain4j-agent

# 查看 Ingress
sudo kubectl get ingress -n langchain4j-agent

# 测试健康检查
NODE_IP=$(sudo kubectl get nodes -o jsonpath='{.items[0].status.addresses[?(@.type=="InternalIP")].address}')
HTTP_PORT=$(sudo kubectl get svc -n kube-system traefik -o jsonpath='{.spec.ports[?(@.name=="web")].nodePort}')

curl http://$NODE_IP:$HTTP_PORT/actuator/health

# 预期输出：
# {"status":"UP"}
```

---

## 访问应用

### 获取访问地址

```bash
# 获取节点 IP
NODE_IP=$(sudo kubectl get nodes -o jsonpath='{.items[0].status.addresses[?(@.type=="InternalIP")].address}')

# 获取 Traefik HTTP 端口
HTTP_PORT=$(sudo kubectl get svc -n kube-system traefik -o jsonpath='{.spec.ports[?(@.name=="web")].nodePort}')

echo "访问地址: http://$NODE_IP:$HTTP_PORT"
```

### 测试 API 接口

```bash
# 健康检查
curl http://$NODE_IP:$HTTP_PORT/actuator/health

# AI 对话
curl -X POST http://$NODE_IP:$HTTP_PORT/api/ai/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "你好"}'

# 代码生成
curl -X POST http://$NODE_IP:$HTTP_PORT/api/ai/code/generate \
  -H "Content-Type: application/json" \
  -d '{
    "prompt": "创建一个 Spring Boot REST API",
    "language": "java"
  }'

# 知识库查询
curl -X POST http://$NODE_IP:$HTTP_PORT/api/ai/knowledge/query \
  -H "Content-Type: application/json" \
  -d '{"query": "如何部署应用到 K8s"}'
```

---

## 运维命令

### 查看日志

```bash
# 查看应用日志
sudo kubectl logs -n langchain4j-agent -l app=langchain4j-agent -f

# 查看最近 100 行日志
sudo kubectl logs -n langchain4j-agent -l app=langchain4j-agent --tail=100

# 查看 Traefik 日志
sudo kubectl logs -n kube-system -l app.kubernetes.io/name=traefik -f
```

### 查看资源详情

```bash
# 查看 Pod 详情
sudo kubectl describe pod -n langchain4j-agent -l app=langchain4j-agent

# 查看 Service 详情
sudo kubectl describe svc -n langchain4j-agent langchain4j-agent-service

# 查看 Ingress 详情
sudo kubectl describe ingress -n langchain4j-agent langchain4j-agent-ingress
```

### 重启应用

```bash
# 重启 Deployment
sudo kubectl rollout restart deployment/langchain4j-agent -n langchain4j-agent

# 等待重启完成
sudo kubectl rollout status deployment/langchain4j-agent -n langchain4j-agent
```

### 扩缩容

```bash
# 扩容到 3 个副本
sudo kubectl scale deployment/langchain4j-agent --replicas=3 -n langchain4j-agent

# 缩容到 1 个副本
sudo kubectl scale deployment/langchain4j-agent --replicas=1 -n langchain4j-agent
```

### 升级应用

```bash
# 更新镜像
sudo kubectl set image deployment/langchain4j-agent \
  app=langchain4j-agent:v2.0 -n langchain4j-agent

# 或使用脚本
sudo ./deploy-traefik.sh upgrade
```

### 回滚

```bash
# 查看历史版本
sudo kubectl rollout history deployment/langchain4j-agent -n langchain4j-agent

# 回滚到上一个版本
sudo kubectl rollout undo deployment/langchain4j-agent -n langchain4j-agent

# 或使用脚本
sudo ./deploy-traefik.sh rollback
```

### 删除应用

```bash
# 使用脚本删除
sudo ./deploy-traefik.sh delete

# 或手动删除
sudo kubectl delete -f k8s/ingress-traefik.yaml
sudo kubectl delete -f k8s/service.yaml
sudo kubectl delete -f k8s/deployment.yaml
sudo kubectl delete -f k8s/configmap.yaml
sudo kubectl delete -f k8s/secret.yaml
sudo kubectl delete -f k8s/namespace.yaml
```

---

## 故障排查

### Pod 无法启动

```bash
# 查看 Pod 状态
sudo kubectl get pods -n langchain4j-agent

# 查看 Pod 事件
sudo kubectl describe pod -n langchain4j-agent <pod-name>

# 查看日志
sudo kubectl logs -n langchain4j-agent <pod-name>

# 常见原因：
# 1. 镜像拉取失败 → 检查镜像地址
# 2. 资源不足 → 检查节点资源
# 3. 配置错误 → 检查 ConfigMap/Secret
```

### 无法访问应用

```bash
# 1. 检查 Traefik 状态
sudo kubectl get pods -n kube-system | grep traefik

# 2. 检查 Service Endpoints
sudo kubectl get endpoints -n langchain4j-agent

# 3. 检查 Ingress 配置
sudo kubectl describe ingress -n langchain4j-agent

# 4. 测试 Service
CLUSTER_IP=$(sudo kubectl get svc -n langchain4j-agent langchain4j-agent-service -o jsonpath='{.spec.clusterIP}')
sudo kubectl run test-client --image=busybox --rm -it -- wget -qO- http://$CLUSTER_IP:8080/actuator/health
```

### 磁盘空间不足

```bash
# 查看磁盘使用
df -h

# 清理未使用的镜像
sudo k3s crictl rmi --prune

# 清理日志
sudo journalctl --vacuum-size=100M
```

---

## Traefik 配置说明

### 入口点（Entrypoints）

Traefik 定义了两个入口点：

- `web`：HTTP（端口 80）
- `websecure`：HTTPS（端口 443）

在 Ingress 注解中指定：

```yaml
annotations:
  traefik.ingress.kubernetes.io/router.entrypoints: web
```

### WebSocket 支持

Traefik **自动支持** WebSocket，无需额外配置。

### 限流配置

如需启用限流，需要创建 Middleware：

```yaml
# 创建限流中间件
cat <<EOF | sudo kubectl apply -f -
apiVersion: traefik.io/v1alpha1
kind: Middleware
metadata:
  name: ratelimit
  namespace: langchain4j-agent
spec:
  rateLimit:
    average: 10
    burst: 20
EOF

# 在 Ingress 中使用
annotations:
  traefik.ingress.kubernetes.io/router.middlewares: langchain4j-agent-ratelimit@kubernetescrd
```

### TLS/SSL 配置

如需启用 HTTPS，需要准备证书：

```yaml
# 创建 TLS Secret
sudo kubectl create secret tls langchain4j-agent-tls \
  --cert=path/to/tls.crt \
  --key=path/to/tls.key \
  -n langchain4j-agent

# 在 Ingress 中启用 TLS
spec:
  tls:
    - hosts:
        - langchain4j-agent.your-domain.com
      secretName: langchain4j-agent-tls
```

---

## 性能优化

### 1. 资源配置

编辑 `k8s/deployment.yaml`，调整资源限制：

```yaml
resources:
  requests:
    cpu: "1"
    memory: 1Gi
  limits:
    cpu: "2"
    memory: 2Gi
```

### 2. 健康检查

调整探针参数：

```yaml
livenessProbe:
  initialDelaySeconds: 60  # 启动后 60 秒开始检查
  periodSeconds: 10        # 每 10 秒检查一次
  failureThreshold: 3      # 失败 3 次后重启

readinessProbe:
  initialDelaySeconds: 30  # 启动后 30 秒开始检查
  periodSeconds: 5         # 每 5 秒检查一次
  failureThreshold: 2      # 失败 2 次后标记为未就绪
```

### 3. JVM 参数

在 `k8s/deployment.yaml` 中配置 JVM 参数：

```yaml
env:
  - name: JAVA_OPTS
    value: "-Xms512m -Xmx1024m -XX:+UseG1GC"
```

---

## 监控与日志

### Prometheus 监控

```bash
# 访问 Prometheus 指标
curl http://$NODE_IP:$HTTP_PORT/actuator/prometheus

# 在 Prometheus 配置中添加抓取目标
# 参见 k8s/monitoring/ 目录
```

### Grafana 仪表板

导入以下仪表板：
- JVM 监控：ID 4701
- Spring Boot：ID 11378

### 日志收集

```bash
# 查看应用日志
sudo kubectl logs -n langchain4j-agent -l app=langchain4j-agent

# 查看 Traefik 访问日志
sudo kubectl logs -n kube-system -l app.kubernetes.io/name=traefik
```

---

## 安全加固

### 1. 网络策略

限制 Ingress 访问：

```yaml
# 只允许特定 IP 访问 actuator
annotations:
  traefik.ingress.kubernetes.io/router.middlewares: langchain4j-agent-ipwhitelist@kubernetescrd
```

### 2. 认证

启用基本认证：

```yaml
# 创建认证 Secret
sudo kubectl create secret generic basic-auth \
  --from-literal=username=admin \
  --from-literal=password=yourpassword \
  -n langchain4j-agent

# 创建认证 Middleware
cat <<EOF | sudo kubectl apply -f -
apiVersion: traefik.io/v1alpha1
kind: Middleware
metadata:
  name: basic-auth
  namespace: langchain4j-agent
spec:
  basicAuth:
    secret: basic-auth
EOF
```

---

## 常见问题 FAQ

### Q: Traefik 和 Nginx Ingress 有什么区别？

**A**: Traefik 是 k3s 自带的 Ingress Controller，功能完整且无需额外安装。两者性能相当，Traefik 配置更简单。

### Q: 如何配置域名？

**A**: 在 `k8s/ingress-traefik.yaml` 中取消注释 `host` 配置：

```yaml
rules:
  - host: langchain4j-agent.your-domain.com
    http:
      paths:
        - path: /
          pathType: Prefix
          backend:
            service:
              name: langchain4j-agent-service
              port:
                number: 8080
```

### Q: 如何启用 HTTPS？

**A**: 需要 SSL 证书，可以使用 Let's Encrypt（免费）：

```bash
# 安装 cert-manager
kubectl apply -f https://github.com/cert-manager/cert-manager/releases/download/v1.13.0/cert-manager.yaml

# 创建 ClusterIssuer
# 配置 Ingress TLS
```

### Q: Pod 一直处于 Pending 状态怎么办？

**A**: 检查节点资源：

```bash
sudo kubectl describe node
sudo kubectl top nodes
```

### Q: 如何查看 Traefik 的路由配置？

**A**: 

```bash
# 访问 Traefik Dashboard（如果启用）
# 或查看 Ingress 配置
sudo kubectl describe ingress -n langchain4j-agent
```

---

## 下一步

1. **配置域名和 HTTPS**：提高安全性
2. **设置监控告警**：Prometheus + Grafana
3. **配置日志收集**：EFK 或 Loki
4. **设置自动扩缩容**：HPA
5. **配置备份策略**：定期备份数据库

---

## 参考文档

- [Traefik 官方文档](https://doc.traefik.io/traefik/)
- [Kubernetes Ingress](https://kubernetes.io/docs/concepts/services-networking/ingress/)
- [k3s 文档](https://docs.k3s.io/)
- [Spring Boot Actuator](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)

---

**祝您部署顺利！** 🚀
