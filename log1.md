
========================================
开始完整部署 langchain4j-agent
========================================

[INFO] 验证 Traefik...
[INFO] ✅ Traefik 运行正常


========================================
部署中间件（按依赖顺序）
========================================

[INFO] 1. 创建命名空间...
namespace/langchain4j-agent unchanged
[INFO] ✅ 命名空间已创建

[INFO] 2. 配置 Secret...
[WARN] 请确保已编辑 k8s/secret.yaml，填入实际的密钥
secret/app-secret unchanged
[INFO] ✅ Secret 已配置

[INFO] 3. 配置 PVC...
persistentvolumeclaim/app-data-pvc created
persistentvolumeclaim/app-logs-pvc created
[INFO] ✅ PVC 已配置

[INFO] 4. 部署 Redis...
deployment.apps/redis created
service/redis-service unchanged
persistentvolumeclaim/redis-pvc created
[INFO] ⏳ 等待 Redis 启动...
[WARN] ⚠️  Redis 可能还在启动中，继续部署...

[INFO] ⏭️  跳过 Milvus（低内存环境，需要时手动启用）
[INFO] 6. 部署 PostgreSQL（可选）...
[INFO] ⏭️  跳过 PostgreSQL（设置 DEPLOY_POSTGRESQL=yes 启用）

[INFO] 7. 部署 RabbitMQ（可选）...
[INFO] ⏭️  跳过 RabbitMQ（设置 DEPLOY_RABBITMQ=yes 启用）

[INFO] 8. 部署 Elasticsearch（可选）...
[INFO] ⏭️  跳过 Elasticsearch（设置 DEPLOY_ELASTICSEARCH=yes 启用）


========================================
中间件部署完成
========================================


========================================
部署应用
========================================

[INFO] 1. 配置 ConfigMap...
configmap/app-config unchanged
[INFO] ✅ ConfigMap 已配置

[INFO] 2. 部署 langchain4j-agent...
deployment.apps/langchain4j-agent created
service/langchain4j-agent-service unchanged
[INFO] ✅ 应用已部署

[INFO] 3. 配置 Ingress（Traefik）...
ingress.networking.k8s.io/langchain4j-agent-ingress unchanged
[INFO] ✅ Ingress 已配置

[INFO] 4. 配置 HPA（可选）...
[INFO] ⏭️  跳过 HPA（设置 DEPLOY_HPA=yes 启用）

[INFO] 5. 等待 Pod 启动...

========================================
应用部署完成
========================================


========================================
验证部署
========================================

[INFO] 1. 检查 Pod 状态...

NAME                                 READY   STATUS              RESTARTS   AGE
redis-55dc64946b-6f9p9               0/1     ContainerCreating   0          46s
langchain4j-agent-6899d77667-7bn6l   0/1     ContainerCreating   0          31s

[INFO] 2. 检查 Service...

NAME                        TYPE        CLUSTER-IP      EXTERNAL-IP   PORT(S)              AGE
redis-service               ClusterIP   10.43.118.214   <none>        6379/TCP             3h9m
milvus-service              ClusterIP   10.43.101.135   <none>        19530/TCP,9091/TCP   3h9m
langchain4j-agent-service   ClusterIP   10.43.207.248   <none>        8080/TCP             3h8m

[INFO] 3. 检查 Ingress...

NAME                        CLASS     HOSTS   ADDRESS   PORTS   AGE
langchain4j-agent-ingress   traefik   *                 80      3h8m

[INFO] 4. 检查中间件状态...

[INFO] Redis:
NAME                     READY   STATUS              RESTARTS   AGE
redis-55dc64946b-6f9p9   0/1     ContainerCreating   0          47s

[INFO] 5. 测试应用健康检查...

[INFO] 访问地址: http://192.168.0.2:31992

[WARN] ️  健康检查返回: 404
[WARN]    请查看 Pod 日志: sudo kubectl logs -n langchain4j-agent -l app=langchain4j-agent

=========================================
部署完成！
=========================================

访问信息：
节点 IP: 192.168.0.2
HTTP 端口: 31992

访问地址：
🌐 主页面:    http://192.168.0.2:31992
🔗 API 接口:  http://192.168.0.2:31992/api
❤️  健康检查: http://192.168.0.2:31992/actuator/health
Prometheus: http://192.168.0.2:31992/actuator/prometheus

中间件访问：
Redis:     redis://redis-service:6379

=========================================

[INFO] 运维命令：
[INFO]   查看日志:   sudo kubectl logs -n langchain4j-agent -l app=langchain4j-agent -f
[INFO]   查看状态:   sudo ./deploy-full.sh status
[INFO]   删除部署:   sudo ./deploy-full.sh delete
