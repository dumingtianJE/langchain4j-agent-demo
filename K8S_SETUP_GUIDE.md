# Kubernetes 部署准备工作指南

## 📋 目录

- [概述](#概述)
- [前置条件检查](#前置条件检查)
- [Kubernetes 集群准备](#kubernetes-集群准备)
- [必需组件安装](#必需组件安装)
- [配置修改](#配置修改)
- [镜像构建与推送](#镜像构建与推送)
- [TLS 证书配置](#tls-证书配置)
- [部署应用](#部署应用)
- [监控与日志（可选）](#监控与日志可选)
- [部署检查清单](#部署检查清单)
- [快速开始脚本](#快速开始脚本)
- [常见问题](#常见问题)

---

## 概述

本文档记录了在 Kubernetes 环境中部署 LangChain4j Agent 应用需要完成的所有准备工作。按照本文档的步骤操作，可以确保部署过程顺利进行。

### 准备工作流程图

```
┌─────────────────┐
│ 1. K8s 集群准备 │
└────────┬────────┘
         ↓
┌─────────────────┐
│ 2. 安装必需组件 │
└────────┬────────┘
         ↓
┌─────────────────┐
│ 3. 修改配置     │
└────────┬────────┘
         ↓
┌─────────────────┐
│ 4. 构建镜像     │
└────────┬────────┘
         ↓
┌─────────────────┐
│ 5. 配置 TLS     │
└────────┬────────┘
         ↓
┌─────────────────┐
│ 6. 部署应用     │
└────────┬────────┘
         ↓
┌─────────────────┐
│ 7. 验证部署     │
└─────────────────┘
```

**预计完成时间**：30-60 分钟

---

## 前置条件检查

### 必需工具

在执行部署之前，请确保以下工具已安装：

```bash
# 检查 kubectl
kubectl version --client

# 检查 Docker
docker --version

# 检查 Helm（推荐）
helm version

# 检查 Git
git --version
```

### 安装工具

如果某些工具未安装，请参考以下安装指南：

#### kubectl

```bash
# Linux
curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"
chmod +x kubectl
sudo mv kubectl /usr/local/bin/

# macOS
brew install kubectl

# Windows
choco install kubernetes-cli
```

#### Helm

```bash
# Linux/macOS
curl https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3 | bash

# Windows
choco install kubernetes-helm
```

---

## Kubernetes 集群准备

### 方案选择

#### 方案 A：云服务商托管 K8s（生产环境推荐）

**阿里云 ACK**：

```bash
# 1. 登录阿里云控制台
# 2. 进入容器服务 Kubernetes 版
# 3. 创建集群（推荐配置）：
#    - Worker 节点：3 个
#    - 节点规格：4 核 8GB 或更高
#    - 系统盘：100GB SSD
#    - 网络：VPC + 交换机

# 4. 配置 kubectl 访问凭证
# 在集群详情页面下载 kubeconfig 文件
# 或执行：
aliyun cs DescribeClusterUserKubeconfig --cluster-id <cluster-id>
```

**AWS EKS**：

```bash
# 1. 安装 eksctl
curl --silent --location "https://github.com/weaveworks/eksctl/releases/latest/download/eksctl_$(uname -s)_amd64.tar.gz" | tar xz -C /tmp
sudo mv /tmp/eksctl /usr/local/bin

# 2. 创建集群
eksctl create cluster \
  --name langchain4j-cluster \
  --region us-east-1 \
  --nodegroup-name standard-workers \
  --node-type t3.xlarge \
  --nodes 3 \
  --nodes-min 2 \
  --nodes-max 5

# 3. 自动配置 kubectl
aws eks update-kubeconfig --region us-east-1 --name langchain4j-cluster
```

**腾讯云 TKE**：

```bash
# 1. 登录腾讯云控制台
# 2. 进入容器服务
# 3. 创建集群
# 4. 下载 kubeconfig 配置文件
```

#### 方案 B：本地开发环境

**Minikube（单节点）**：

```bash
# 安装 Minikube
curl -LO https://storage.googleapis.com/minikube/releases/latest/minikube-linux-amd64
sudo install minikube-linux-amd64 /usr/local/bin/minikube

# 启动集群（推荐配置）
minikube start \
  --cpus=4 \
  --memory=8192 \
  --disk-size=50g \
  --driver=docker

# 验证
kubectl get nodes
```

**Kind（多节点）**：

```bash
# 安装 Kind
go install sigs.k8s.io/kind@latest

# 创建多节点集群配置文件
cat <<EOF > kind-config.yaml
kind: Cluster
apiVersion: kind.x-k8s.io/v1alpha4
nodes:
- role: control-plane
- role: worker
- role: worker
EOF

# 创建集群
kind create cluster --config kind-config.yaml

# 验证
kubectl get nodes
```

**K3s（轻量级）**：

```bash
# 安装 K3s
curl -sfL https://get.k3s.io | sh -

# 验证
sudo k3s kubectl get nodes

# 配置 kubectl
sudo cp /etc/rancher/k3s/k3s.yaml ~/.kube/config
sudo chown $USER ~/.kube/config
```

### 验证集群状态

```bash
# 检查当前上下文
kubectl config current-context

# 查看所有上下文
kubectl config get-contexts

# 查看节点状态
kubectl get nodes

# 预期输出：
# NAME           STATUS   ROLES    AGE   VERSION
# node-1         Ready    master   10m   v1.28.0
# node-2         Ready    <none>   10m   v1.28.0
# node-3         Ready    <none>   10m   v1.28.0

# 查看集群信息
kubectl cluster-info

# 检查核心组件
kubectl get pods -n kube-system
```

---

## 必需组件安装

### 1. Nginx Ingress Controller

Ingress Controller 是外部访问集群内部服务的入口。

```bash
# 添加 Helm 仓库
helm repo add ingress-nginx https://kubernetes.github.io/ingress-nginx
helm repo update

# 安装 Ingress Controller
helm install ingress-nginx ingress-nginx/ingress-nginx \
  --namespace ingress-nginx \
  --create-namespace \
  --set controller.replicaCount=2 \
  --set controller.nodeSelector."kubernetes\.io/os"=linux \
  --set defaultBackend.nodeSelector."kubernetes\.io/os"=linux

# 验证安装
kubectl get pods -n ingress-nginx
kubectl get svc -n ingress-nginx

# 等待 External IP 分配（云环境）
kubectl get svc -n ingress-nginx -w
```

**验证 Ingress 是否可用**：

```bash
# 创建测试应用
kubectl create deployment nginx-test --image=nginx
kubectl expose deployment nginx-test --port=80

# 创建测试 Ingress
cat <<EOF | kubectl apply -f -
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: test-ingress
spec:
  rules:
  - http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: nginx-test
            port:
              number: 80
EOF

# 测试访问
curl http://<ingress-external-ip>
```

### 2. Metrics Server

Metrics Server 提供资源使用指标，HPA 自动扩缩容必需。

```bash
# 安装 Metrics Server
kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/latest/download/components.yaml

# 如果使用 Minikube 或 Kind，需要添加额外参数
kubectl patch deployment metrics-server \
  -n kube-system \
  --type='json' \
  -p='[{"op": "add", "path": "/spec/template/spec/containers/0/args/-", "value": "--kubelet-insecure-tls"}]'

# 验证安装
kubectl get apiservice v1beta1.metrics.k8s.io

# 等待 API 服务可用
kubectl get apiservice v1beta1.metrics.k8s.io -w

# 测试指标
kubectl top nodes
kubectl top pods -A
```

### 3. StorageClass

StorageClass 用于动态提供持久化存储（PVC）。

```bash
# 查看现有 StorageClass
kubectl get storageclass

# 预期输出（如果有默认 StorageClass）：
# NAME                 PROVISIONER          RECLAIMPOLICY   VOLUMEBINDINGMODE   ALLOWVOLUMEEXPANSION   AGE
# standard (default)   kubernetes.io/aws-ebs   Delete          WaitForFirstConsumer   false                  10m
```

#### 如果没有 StorageClass，根据环境选择安装：

**阿里云**：

```bash
# 安装阿里云 CSI 插件
kubectl apply -f https://raw.githubusercontent.com/kubernetes-sigs/alibaba-cloud-csi-driver/master/deploy/setup/alicloud-disk-controller.yaml
```

**AWS**：

```bash
# 安装 AWS EBS CSI 驱动
kubectl apply -f https://raw.githubusercontent.com/kubernetes-sigs/aws-ebs-csi-driver/master/deploy/kubernetes/overlays/stable/ecr/kustomization.yaml
```

**本地环境（Minikube/Kind）**：

```bash
# 使用 hostPath 创建 StorageClass
cat <<EOF | kubectl apply -f -
apiVersion: storage.k8s.io/v1
kind: StorageClass
metadata:
  name: standard
  annotations:
    storageclass.kubernetes.io/is-default-class: "true"
provisioner: kubernetes.io/no-provisioner
volumeBindingMode: WaitForFirstConsumer
EOF
```

**验证 StorageClass**：

```bash
# 查看 StorageClass
kubectl get storageclass

# 测试创建 PVC
cat <<EOF | kubectl apply -f -
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: test-pvc
spec:
  accessModes:
    - ReadWriteOnce
  storageClassName: standard
  resources:
    requests:
      storage: 1Gi
EOF

# 查看 PVC 状态
kubectl get pvc test-pvc

# 清理测试
kubectl delete pvc test-pvc
```

---

## 配置修改

### 1. 修改 Secret（必须）

Secret 用于存储敏感信息，如 API Key、密码等。

#### 生成 base64 编码

```bash
# Linux/macOS
echo -n 'your-dashscope-api-key' | base64
# 输出示例：eW91ci1kYXNoc2NvcGUtYXBpLWtleQ==

echo -n 'your-redis-password' | base64
# 输出示例：eW91ci1yZWRpcy1wYXNzd29yZA==

echo -n 'your-jwt-secret-key' | base64
# 输出示例：eW91ci1qd3Qtc2VjcmV0LWtleQ==

echo -n 'admin' | base64
# 输出示例：YWRtaW4=

echo -n 'your-postgres-password' | base64
# 输出示例：eW91ci1wb3N0Z3Jlcy1wYXNzd29yZA==

echo -n 'your-erlang-cookie' | base64
# 输出示例：eW91ci1lcmxhbmctY29va2ll
```

```bash
# Windows PowerShell
[System.Convert]::ToBase64String([System.Text.Encoding]::UTF8.GetBytes('your-dashscope-api-key'))
```

#### 编辑 Secret 文件

```bash
vi k8s/secret.yaml
```

**替换以下值**：

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: app-secret
  namespace: langchain4j-agent
type: Opaque
data:
  # 通义千问 API Key（必填）
  DASHSCOPE_API_KEY: <your-base64-encoded-api-key>
  
  # Redis 密码（必填）
  REDIS_PASSWORD: <your-base64-encoded-password>
  
  # JWT Secret（必填）
  JWT_SECRET: <your-base64-encoded-secret>
  
  # PostgreSQL 用户名
  POSTGRES_USER: YWRtaW4=  # admin
  
  # PostgreSQL 密码（如果使用 PostgreSQL）
  POSTGRES_PASSWORD: <your-base64-encoded-postgres-password>
  
  # RabbitMQ Erlang Cookie（如果使用 RabbitMQ）
  RABBITMQ_ERLANG_COOKIE: <your-base64-encoded-cookie>
```

#### 验证 Secret

```bash
# 应用 Secret
kubectl apply -f k8s/secret.yaml

# 查看 Secret（值已加密）
kubectl get secret app-secret -n langchain4j-agent -o yaml

# 解码验证
kubectl get secret app-secret -n langchain4j-agent -o jsonpath='{.data.DASHSCOPE_API_KEY}' | base64 --decode
```

### 2. 修改 Ingress 域名（如果使用 Ingress）

```bash
vi k8s/ingress.yaml
```

**替换域名**：

```yaml
spec:
  rules:
    - host: langchain4j-agent.your-domain.com  # 替换为您的域名
```

**如果没有域名，可以**：
- 使用 IP + nip.io 服务：`http://<ingress-ip>.nip.io`
- 使用 NodePort 直接访问
- 使用端口转发测试

### 3. 选择部署方案

根据业务需求选择部署方案：

#### 方案 A：基础方案（开发/测试环境）

- 应用 + Redis（单机）
- 资源需求：1.75 核 / 2.75Gi / 22Gi

```bash
# 不需要额外配置，使用默认 ConfigMap 即可
```

#### 方案 B：标准方案（推荐）

- 应用 + PostgreSQL + Redis Sentinel + Milvus
- 资源需求：10 核 / 17Gi / 190Gi

```bash
# 修改 ConfigMap 启用 PostgreSQL
vi k8s/configmap.yaml
```

**取消注释并修改**：

```yaml
# PostgreSQL 配置
SPRING_DATASOURCE_URL: "jdbc:postgresql://postgresql-service:5432/agent_db"
# SPRING_DATASOURCE_USERNAME: "admin"  # 从 Secret 读取
# SPRING_DATASOURCE_PASSWORD: "your-password"  # 从 Secret 读取

# Redis Sentinel 配置
SPRING_REDIS_SENTINEL_MASTER: "mymaster"
SPRING_REDIS_SENTINEL_NODES: "redis-sentinel-0.redis-sentinel-service:26379,redis-sentinel-1.redis-sentinel-service:26379,redis-sentinel-2.redis-sentinel-service:26379"
```

**注释 H2 配置**：

```yaml
# 数据库配置（H2 - 开发环境）
# SPRING_DATASOURCE_URL: "jdbc:h2:file:/app/data/ai-agent-db"
```

#### 方案 C：完整方案（生产环境）

- 所有中间件 + ELK 日志
- 资源需求：18 核 / 30Gi / 510Gi

```bash
# 在标准方案基础上，额外启用 RabbitMQ 和 Elasticsearch
vi k8s/configmap.yaml
```

**取消注释**：

```yaml
# RabbitMQ 配置
SPRING_RABBITMQ_HOST: "rabbitmq-service"
SPRING_RABBITMQ_PORT: "5672"
# SPRING_RABBITMQ_USERNAME: "guest"  # 从 Secret 读取
# SPRING_RABBITMQ_PASSWORD: "guest"  # 从 Secret 读取

# Elasticsearch 配置
SPRING_ELASTICSEARCH_URIS: "http://elasticsearch:9200"
```

---

## 镜像构建与推送

### 1. 构建 Docker 镜像

```bash
# 进入项目目录
cd /path/to/langchain4j-agent-demo

# 构建镜像
docker build -t langchain4j-agent:latest .

# 查看镜像
docker images | grep langchain4j-agent

# 测试本地运行
docker run --rm -p 8080:8080 \
  -e LANGCHAIN4J_OPEN_AI_CHAT_MODEL_API_KEY=your-api-key \
  langchain4j-agent:latest

# 在另一个终端测试
curl http://localhost:8080/actuator/health
```

### 2. 推送到镜像仓库

#### 阿里云容器镜像服务（ACR）

```bash
# 1. 登录 ACR
docker login --username=<your-username> registry.cn-hangzhou.aliyuncs.com
# 输入密码（阿里云账号密码或独立镜像仓库密码）

# 2. 打标签
docker tag langchain4j-agent:latest \
  registry.cn-hangzhou.aliyuncs.com/<your-namespace>/langchain4j-agent:latest

# 3. 推送
docker push registry.cn-hangzhou.aliyuncs.com/<your-namespace>/langchain4j-agent:latest

# 4. 验证
# 在阿里云控制台查看镜像是否推送成功
```

#### Docker Hub

```bash
# 1. 登录 Docker Hub
docker login
# 输入 Docker Hub 用户名和密码

# 2. 打标签
docker tag langchain4j-agent:latest \
  yourusername/langchain4j-agent:latest

# 3. 推送
docker push yourusername/langchain4j-agent:latest

# 4. 验证
# 访问 https://hub.docker.com 查看镜像
```

#### 其他镜像仓库

```bash
# Harbor
docker login harbor.your-domain.com
docker tag langchain4j-agent:latest harbor.your-domain.com/project/langchain4j-agent:latest
docker push harbor.your-domain.com/project/langchain4j-agent:latest

# AWS ECR
aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin <account-id>.dkr.ecr.us-east-1.amazonaws.com
docker tag langchain4j-agent:latest <account-id>.dkr.ecr.us-east-1.amazonaws.com/langchain4j-agent:latest
docker push <account-id>.dkr.ecr.us-east-1.amazonaws.com/langchain4j-agent:latest
```

### 3. 更新 Deployment 镜像地址

```bash
vi k8s/deployment.yaml
```

**替换镜像地址**：

```yaml
spec:
  containers:
    - name: app
      # 阿里云 ACR
      image: registry.cn-hangzhou.aliyuncs.com/<your-namespace>/langchain4j-agent:latest
      
      # 或 Docker Hub
      # image: yourusername/langchain4j-agent:latest
      
      # 或 Harbor
      # image: harbor.your-domain.com/project/langchain4j-agent:latest
      
      imagePullPolicy: Always
```

**如果使用私有镜像仓库，还需要创建 ImagePullSecret**：

```bash
# 创建 Secret
kubectl create secret docker-registry registry-secret \
  --docker-server=<registry-url> \
  --docker-username=<username> \
  --docker-password=<password> \
  --docker-email=<email> \
  -n langchain4j-agent

# 在 Deployment 中引用
# 添加 imagePullSecrets:
spec:
  template:
    spec:
      imagePullSecrets:
        - name: registry-secret
```

---

## TLS 证书配置

### 方式 1：手动配置（适合已有证书）

```bash
# 创建 TLS Secret
kubectl create secret tls langchain4j-agent-tls \
  --cert=/path/to/tls.crt \
  --key=/path/to/tls.key \
  -n langchain4j-agent

# 验证
kubectl get secret langchain4j-agent-tls -n langchain4j-agent
```

### 方式 2：使用 Let's Encrypt（自动，推荐）

#### 安装 cert-manager

```bash
# 安装 cert-manager
kubectl apply -f https://github.com/cert-manager/cert-manager/releases/download/v1.13.0/cert-manager.yaml

# 验证安装
kubectl get pods -n cert-manager

# 等待 cert-manager 就绪
kubectl wait --for=condition=available \
  deployment/cert-manager \
  -n cert-manager \
  --timeout=120s
```

#### 创建 ClusterIssuer

```bash
cat <<EOF | kubectl apply -f -
apiVersion: cert-manager.io/v1
kind: ClusterIssuer
metadata:
  name: letsencrypt-prod
spec:
  acme:
    server: https://acme-v02.api.letsencrypt.org/directory
    email: your-email@example.com  # 替换为您的邮箱
    privateKeySecretRef:
      name: letsencrypt-prod
    solvers:
      - http01:
          ingress:
            class: nginx
EOF

# 验证
kubectl get clusterissuer
```

#### 修改 Ingress 使用自动证书

```bash
vi k8s/ingress.yaml
```

**添加注解和证书配置**：

```yaml
metadata:
  annotations:
    cert-manager.io/cluster-issuer: "letsencrypt-prod"
    # 其他注解...

spec:
  tls:
    - hosts:
        - langchain4j-agent.your-domain.com
      secretName: langchain4j-agent-tls  # cert-manager 会自动创建
```

**应用并等待证书签发**：

```bash
kubectl apply -f k8s/ingress.yaml

# 查看证书状态
kubectl get certificate -n langchain4j-agent

# 等待证书就绪
kubectl get certificate langchain4j-agent-tls -n langchain4j-agent -w
```

### 方式 3：使用云服务商证书

**阿里云**：

```bash
# 在阿里云 SSL 证书服务购买和下载证书
# 然后手动创建 Secret
kubectl create secret tls langchain4j-agent-tls \
  --cert=path/to/cert.pem \
  --key=path/to/key.pem \
  -n langchain4j-agent
```

---

## 部署应用

### 1. 一键部署

#### 部署基础环境（应用 + Redis）

```bash
# 赋予执行权限
chmod +x deploy.sh

# 部署
./deploy.sh deploy

# 查看状态
kubectl get pods -n langchain4j-agent
kubectl get svc -n langchain4j-agent
```

#### 部署中间件

```bash
# 赋予执行权限
chmod +x deploy-middleware.sh

# 部署所有中间件
./deploy-middleware.sh deploy all

# 或单独部署
./deploy-middleware.sh deploy postgresql
./deploy-middleware.sh deploy redis
./deploy-middleware.sh deploy milvus
./deploy-middleware.sh deploy rabbitmq
./deploy-middleware.sh deploy elasticsearch
```

### 2. 验证部署

```bash
# 查看所有资源
kubectl get all -n langchain4j-agent

# 查看 Pod 状态
kubectl get pods -n langchain4j-agent

# 预期输出：
# NAME                                 READY   STATUS    RESTARTS   AGE
# langchain4j-agent-5d8f9c7b6-abc12   1/1     Running   0          2m
# langchain4j-agent-5d8f9c7b6-def34   1/1     Running   0          2m
# redis-0                              1/1     Running   0          2m

# 查看 Service
kubectl get svc -n langchain4j-agent

# 查看 Ingress
kubectl get ingress -n langchain4j-agent

# 查看 HPA
kubectl get hpa -n langchain4j-agent

# 查看 PVC
kubectl get pvc -n langchain4j-agent

# 查看 Pod 详情
kubectl describe pod <pod-name> -n langchain4j-agent

# 查看日志
kubectl logs -f <pod-name> -n langchain4j-agent
```

### 3. 测试应用

```bash
# 端口转发
kubectl port-forward -n langchain4j-agent svc/langchain4j-agent-service 8080:8080

# 测试健康检查
curl http://localhost:8080/actuator/health

# 预期输出：
# {"status":"UP"}

# 测试 AI 接口
curl -X POST http://localhost:8080/api/ai/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "你好，请介绍一下自己"}'

# 预期输出：
# {"reply": "你好！我是 AI 编程助手..."}
```

### 4. 访问应用

#### 通过 Ingress（需要域名和 TLS）

```bash
# 访问应用
curl https://langchain4j-agent.your-domain.com/api/ai/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "你好"}'
```

#### 通过 NodePort

```bash
# 修改 Service 类型为 NodePort
kubectl patch svc langchain4j-agent-service -n langchain4j-agent -p '{"spec":{"type":"NodePort"}}'

# 查看端口
kubectl get svc langchain4j-agent-service -n langchain4j-agent

# 访问
curl http://<node-ip>:<node-port>/actuator/health
```

#### 通过 LoadBalancer（云环境）

```bash
# 修改 Service 类型为 LoadBalancer
kubectl patch svc langchain4j-agent-service -n langchain4j-agent -p '{"spec":{"type":"LoadBalancer"}}'

# 查看 External IP
kubectl get svc langchain4j-agent-service -n langchain4j-agent -w

# 访问
curl http://<external-ip>:8080/actuator/health
```

---

## 监控与日志（可选）

### 1. 安装 Prometheus + Grafana

```bash
# 添加 Helm 仓库
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo update

# 安装 Prometheus Stack
helm install kube-prometheus-stack prometheus-community/kube-prometheus-stack \
  --namespace monitoring \
  --create-namespace \
  --set grafana.adminPassword=admin123

# 验证安装
kubectl get pods -n monitoring

# 访问 Grafana
kubectl port-forward -n monitoring svc/kube-prometheus-stack-grafana 3000:80

# 浏览器访问 http://localhost:3000
# 默认账号：admin
# 默认密码：admin123
```

**配置 Grafana Dashboard**：

1. 登录 Grafana
2. 导入 Spring Boot Dashboard（ID: 11378）
3. 导入 JVM Dashboard（ID: 4701）
4. 配置 Prometheus 数据源（已自动配置）

### 2. 安装 EFK 日志系统

```bash
# 添加 Elastic Helm 仓库
helm repo add elastic https://helm.elastic.co
helm repo update

# 安装 Elasticsearch
helm install elasticsearch elastic/elasticsearch \
  --namespace logging \
  --create-namespace \
  --set replicas=3 \
  --set minimumMasterNodes=2

# 安装 Fluentd
helm install fluentd fluent/fluentd \
  --namespace logging

# 安装 Kibana
helm install kibana elastic/kibana \
  --namespace logging

# 验证安装
kubectl get pods -n logging

# 访问 Kibana
kubectl port-forward -n logging svc/kibana-kibana 5601:5601

# 浏览器访问 http://localhost:5601
```

**配置日志索引**：

1. 登录 Kibana
2. 创建索引模式：`logstash-*` 或 `filebeat-*`
3. 查看日志

---

## 部署检查清单

### 前置条件

- [ ] Kubernetes 集群已就绪（v1.24+）
- [ ] kubectl 已配置并可以访问集群
- [ ] Helm 已安装（v3.x）
- [ ] Docker 已安装
- [ ] Git 已安装

### 必需组件

- [ ] Nginx Ingress Controller 已安装
- [ ] Metrics Server 已安装
- [ ] StorageClass 已配置
- [ ] 命名空间 `langchain4j-agent` 已创建

### 配置修改

- [ ] k8s/secret.yaml 已更新
  - [ ] DASHSCOPE_API_KEY 已替换
  - [ ] REDIS_PASSWORD 已替换
  - [ ] JWT_SECRET 已替换
  - [ ] POSTGRES_PASSWORD 已替换（如使用）
  - [ ] RABBITMQ_ERLANG_COOKIE 已替换（如使用）

- [ ] k8s/ingress.yaml 已更新
  - [ ] 域名已替换为实际域名
  - [ ] TLS Secret 名称正确

- [ ] k8s/configmap.yaml 已更新
  - [ ] 选择的部署方案配置已启用
  - [ ] 不需要的配置已注释

- [ ] k8s/deployment.yaml 已更新
  - [ ] 镜像地址已替换
  - [ ] imagePullSecrets 已配置（如使用私有仓库）

### 镜像

- [ ] Docker 镜像已构建
- [ ] 镜像已推送到仓库
- [ ] 镜像版本标签正确
- [ ] Deployment 中镜像地址已更新

### TLS 证书

- [ ] TLS 证书已配置
  - [ ] 手动方式：Secret 已创建
  - [ ] 自动方式：cert-manager 已安装，ClusterIssuer 已创建

### 部署验证

- [ ] 所有 Pod 运行正常（STATUS=Running）
- [ ] 所有 Pod 就绪（READY=X/X）
- [ ] 健康检查通过（/actuator/health 返回 UP）
- [ ] AI 接口正常工作
- [ ] 中间件连接正常
- [ ] Ingress 可访问（如配置）
- [ ] HPA 正常工作（如配置）

### 监控与日志（可选）

- [ ] Prometheus 已安装并运行
- [ ] Grafana 可访问，Dashboard 已导入
- [ ] Elasticsearch 已安装并运行
- [ ] Kibana 可访问，索引模式已创建
- [ ] 应用日志正常输出

---

## 快速开始脚本

创建一个一键初始化脚本，自动化完成所有准备工作：

```bash
cat <<'EOF' > init-k8s.sh
#!/bin/bash
# K8s 环境初始化脚本
# 用法: ./init-k8s.sh

set -e

echo "========================================="
echo "  Kubernetes 环境初始化"
echo "========================================="
echo ""

# 颜色定义
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 1. 检查必需工具
info "1. 检查必需工具..."
command -v kubectl >/dev/null 2>&1 || { error "kubectl 未安装，请先安装"; exit 1; }
command -v docker >/dev/null 2>&1 || { error "docker 未安装，请先安装"; exit 1; }
command -v helm >/dev/null 2>&1 || { warn "helm 未安装，部分功能将不可用"; }

info "kubectl 版本: $(kubectl version --client --short 2>/dev/null || kubectl version --client)"
info "docker 版本: $(docker --version)"
info ""

# 2. 检查集群连接
info "2. 检查 Kubernetes 集群..."
kubectl cluster-info >/dev/null 2>&1 || { error "无法连接到 Kubernetes 集群"; exit 1; }
info "集群信息:"
kubectl cluster-info | head -n 2
info ""

# 3. 检查节点状态
info "3. 检查节点状态..."
kubectl get nodes
info ""

# 4. 安装 Nginx Ingress Controller
info "4. 安装 Nginx Ingress Controller..."
if kubectl get deployment ingress-nginx-controller -n ingress-nginx >/dev/null 2>&1; then
    warn "Ingress Controller 已安装，跳过"
else
    helm repo add ingress-nginx https://kubernetes.github.io/ingress-nginx >/dev/null 2>&1
    helm repo update >/dev/null 2>&1
    helm install ingress-nginx ingress-nginx/ingress-nginx \
        --namespace ingress-nginx \
        --create-namespace \
        --set controller.replicaCount=2
    info "Ingress Controller 安装完成"
fi
info ""

# 5. 安装 Metrics Server
info "5. 安装 Metrics Server..."
if kubectl get deployment metrics-server -n kube-system >/dev/null 2>&1; then
    warn "Metrics Server 已安装，跳过"
else
    kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/latest/download/components.yaml
    info "Metrics Server 安装完成"
fi
info ""

# 6. 检查 StorageClass
info "6. 检查 StorageClass..."
STORAGE_CLASS=$(kubectl get storageclass 2>/dev/null | grep -c "(default)" || true)
if [ "$STORAGE_CLASS" -eq 0 ]; then
    warn "未找到默认 StorageClass，请手动配置"
else
    kubectl get storageclass
fi
info ""

# 7. 创建命名空间
info "7. 创建命名空间..."
if kubectl get namespace langchain4j-agent >/dev/null 2>&1; then
    warn "命名空间 langchain4j-agent 已存在"
else
    kubectl apply -f k8s/namespace.yaml
    info "命名空间创建完成"
fi
info ""

echo "========================================="
echo "  初始化完成！"
echo "========================================="
echo ""
echo "请继续执行以下步骤："
echo ""
echo "1. 修改敏感配置："
echo "   vi k8s/secret.yaml"
echo "   vi k8s/ingress.yaml"
echo "   vi k8s/configmap.yaml"
echo ""
echo "2. 构建并推送 Docker 镜像："
echo "   docker build -t langchain4j-agent:latest ."
echo "   docker push <your-registry>/langchain4j-agent:latest"
echo ""
echo "3. 更新 Deployment 镜像地址："
echo "   vi k8s/deployment.yaml"
echo ""
echo "4. 部署应用："
echo "   chmod +x deploy.sh"
echo "   ./deploy.sh deploy"
echo ""
echo "5. 验证部署："
echo "   kubectl get pods -n langchain4j-agent"
echo "   kubectl port-forward -n langchain4j-agent svc/langchain4j-agent-service 8080:8080"
echo "   curl http://localhost:8080/actuator/health"
echo ""
EOF

# 赋予执行权限
chmod +x init-k8s.sh
```

**使用方法**：

```bash
# 运行初始化脚本
./init-k8s.sh

# 按照提示完成后续步骤
```

---

## 常见问题

### Q1: kubectl 无法连接到集群

**问题**：

```bash
Unable to connect to the server: dial tcp: lookup ...
```

**解决方案**：

```bash
# 检查 kubeconfig 文件
cat ~/.kube/config

# 如果文件不存在或配置错误
# 1. 从云服务商下载最新的 kubeconfig
# 2. 复制到 ~/.kube/config

# 测试连接
kubectl cluster-info
```

### Q2: Pod 一直处于 Pending 状态

**问题**：

```bash
kubectl get pods
NAME                                READY   STATUS    RESTARTS   AGE
langchain4j-agent-xxx              0/1     Pending   0          5m
```

**解决方案**：

```bash
# 查看 Pod 详情
kubectl describe pod <pod-name> -n langchain4j-agent

# 常见原因：
# 1. 资源不足：检查节点资源
kubectl top nodes

# 2. PVC 无法绑定：检查 StorageClass
kubectl get storageclass
kubectl get pvc

# 3. 镜像拉取失败：检查镜像地址和 ImagePullSecret
kubectl describe pod <pod-name> | grep -A 5 Events
```

### Q3: 镜像拉取失败

**问题**：

```bash
Failed to pull image "xxx": rpc error: code = Unknown desc = Error response from daemon: pull access denied
```

**解决方案**：

```bash
# 1. 检查镜像地址是否正确
# 2. 创建 ImagePullSecret
kubectl create secret docker-registry registry-secret \
  --docker-server=<registry-url> \
  --docker-username=<username> \
  --docker-password=<password> \
  -n langchain4j-agent

# 3. 在 Deployment 中引用
# 添加 imagePullSecrets:
spec:
  template:
    spec:
      imagePullSecrets:
        - name: registry-secret
```

### Q4: Ingress 无法访问

**问题**：

```bash
curl http://langchain4j-agent.your-domain.com
curl: (7) Failed to connect to ...
```

**解决方案**：

```bash
# 1. 检查 Ingress 状态
kubectl get ingress -n langchain4j-agent

# 2. 检查 Ingress Controller
kubectl get pods -n ingress-nginx

# 3. 检查域名解析
nslookup langchain4j-agent.your-domain.com

# 4. 临时使用 IP 访问
kubectl get svc -n ingress-nginx
curl http://<ingress-external-ip>

# 5. 检查 Ingress 规则
kubectl describe ingress langchain4j-agent-ingress -n langchain4j-agent
```

### Q5: HPA 不工作

**问题**：

```bash
kubectl get hpa
NAME                    REFERENCE                     TARGETS   MINPODS   MAXPODS   REPLICAS
langchain4j-agent-hpa   Deployment/langchain4j-agent  <unknown>/70%   2         10        2
```

**解决方案**：

```bash
# 1. 检查 Metrics Server
kubectl get apiservice v1beta1.metrics.k8s.io

# 2. 检查 Pod 资源限制
kubectl describe pod <pod-name> -n langchain4j-agent | grep -A 10 Limits

# 确保设置了 resources.requests
resources:
  requests:
    cpu: "1"
    memory: 1Gi

# 3. 查看 HPA 详情
kubectl describe hpa langchain4j-agent-hpa -n langchain4j-agent
```

### Q6: PVC 一直处于 Pending 状态

**问题**：

```bash
kubectl get pvc
NAME             STATUS    VOLUME   CAPACITY   ACCESS MODES
app-data-pvc     Pending
```

**解决方案**：

```bash
# 1. 检查 StorageClass
kubectl get storageclass

# 2. 如果没有默认 StorageClass，创建一个
kubectl apply -f - <<EOF
apiVersion: storage.k8s.io/v1
kind: StorageClass
metadata:
  name: standard
  annotations:
    storageclass.kubernetes.io/is-default-class: "true"
provisioner: kubernetes.io/no-provisioner
volumeBindingMode: WaitForFirstConsumer
EOF

# 3. 查看 PVC 事件
kubectl describe pvc app-data-pvc -n langchain4j-agent
```

---

## 相关文档

- [K8S_DEPLOYMENT.md](./K8S_DEPLOYMENT.md) - 完整 K8s 部署指南
- [K8S_MIDDLEWARE.md](./K8S_MIDDLEWARE.md) - 中间件部署指南
- [DEPLOYMENT.md](./DEPLOYMENT.md) - Docker 部署指南
- [PRODUCTION_CHECKLIST.md](./PRODUCTION_CHECKLIST.md) - 生产环境检查清单

---

## 下一步

完成所有准备工作后，您可以：

1. **部署应用**：按照 [K8S_DEPLOYMENT.md](./K8S_DEPLOYMENT.md) 进行部署
2. **部署中间件**：按照 [K8S_MIDDLEWARE.md](./K8S_MIDDLEWARE.md) 部署所需中间件
3. **配置监控**：安装 Prometheus + Grafana
4. **配置日志**：安装 EFK 日志系统
5. **配置 CI/CD**：集成 GitHub Actions 或 GitLab CI

---

**文档版本**: v1.0  
**最后更新**: 2026-05-26  
**维护者**: AI Agent 开发团队
