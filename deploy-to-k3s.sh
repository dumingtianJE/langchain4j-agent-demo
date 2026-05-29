#!/bin/bash
# ============================================
# k3s 完整部署脚本
# ============================================

set -e

echo "========================================"
echo "  k3s 完整部署脚本"
echo "========================================"
echo ""

# 颜色定义
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
CYAN='\033[0;36m'
NC='\033[0m'

NAMESPACE="ai-agent"
DEPLOYMENT_FILE="k8s/deployment-k3s.yaml"

# 检查 kubectl
echo -e "${YELLOW}[1/8] 检查 kubectl 连接...${NC}"
if ! kubectl cluster-info > /dev/null 2>&1; then
    echo -e "${RED}  ✗ 错误: 无法连接到 k3s 集群${NC}"
    echo -e "${YELLOW}  请确保 k3s 已安装并且 kubectl 已配置${NC}"
    exit 1
fi
echo -e "${GREEN}  ✓ 已连接到 k3s 集群${NC}"
kubectl cluster-info | head -1

echo ""
echo -e "${YELLOW}[2/8] 创建命名空间...${NC}"
kubectl create namespace $NAMESPACE --dry-run=client -o yaml | kubectl apply -f -
echo -e "${GREEN}  ✓ 命名空间 $NAMESPACE 已创建${NC}"

echo ""
echo -e "${YELLOW}[3/8] 检查 Docker 镜像...${NC}"
IMAGE_NAME="langchain4j-agent-demo:v1.0.0"
if sudo k3s ctr images ls | grep -q "$IMAGE_NAME"; then
    echo -e "${GREEN}  ✓ 镜像 $IMAGE_NAME 已存在${NC}"
else
    echo -e "${RED}  ✗ 镜像 $IMAGE_NAME 不存在${NC}"
    echo -e "${YELLOW}  请先运行构建脚本: ./build-k3s-image.sh${NC}"
    exit 1
fi

echo ""
echo -e "${YELLOW}[4/8] 部署 ConfigMap 和 Secret...${NC}"
kubectl apply -f k8s/configmap.yaml -n $NAMESPACE
kubectl apply -f k8s/secret.yaml -n $NAMESPACE
echo -e "${GREEN}  ✓ ConfigMap 和 Secret 已部署${NC}"

echo ""
echo -e "${YELLOW}[5/8] 部署 PVC...${NC}"
kubectl apply -f k8s/pvc.yaml -n $NAMESPACE
echo -e "${GREEN}  ✓ PVC 已部署${NC}"

echo ""
echo -e "${YELLOW}[6/8] 部署中间件（可选）...${NC}"
echo -e "${YELLOW}  是否部署 Redis? (y/n)${NC}"
read -r deploy_redis
if [[ "$deploy_redis" =~ ^[Yy]$ ]]; then
    kubectl apply -f k8s/redis.yaml -n $NAMESPACE
    echo -e "${GREEN}  ✓ Redis 已部署${NC}"
fi

echo -e "${YELLOW}  是否部署 Milvus? (y/n)${NC}"
read -r deploy_milvus
if [[ "$deploy_milvus" =~ ^[Yy]$ ]]; then
    kubectl apply -f k8s/milvus.yaml -n $NAMESPACE
    echo -e "${GREEN}  ✓ Milvus 已部署${NC}"
    echo -e "${YELLOW}  等待 Milvus 启动...${NC}"
    sleep 30
fi

echo ""
echo -e "${YELLOW}[7/8] 部署应用...${NC}"
kubectl apply -f $DEPLOYMENT_FILE -n $NAMESPACE
kubectl apply -f k8s/service.yaml -n $NAMESPACE
echo -e "${GREEN}  ✓ 应用已部署${NC}"

echo ""
echo -e "${YELLOW}[8/8] 部署 Ingress（可选）...${NC}"
echo -e "${YELLOW}  是否部署 Ingress? (y/n)${NC}"
read -r deploy_ingress
if [[ "$deploy_ingress" =~ ^[Yy]$ ]]; then
    kubectl apply -f k8s/ingress-traefik.yaml -n $NAMESPACE
    echo -e "${GREEN}  ✓ Ingress 已部署${NC}"
fi

echo ""
echo "========================================"
echo -e "${GREEN}  部署完成！${NC}"
echo "========================================"
echo ""

# 显示部署状态
echo -e "${CYAN}部署状态:${NC}"
echo ""
echo -e "${YELLOW}Pod 状态:${NC}"
kubectl get pods -n $NAMESPACE

echo ""
echo -e "${YELLOW}Service 状态:${NC}"
kubectl get svc -n $NAMESPACE

echo ""
echo -e "${YELLOW}查看 Pod 日志:${NC}"
echo -e "  kubectl logs -n $NAMESPACE -l app=langchain4j-agent -f"

echo ""
echo -e "${YELLOW}访问应用:${NC}"
echo -e "  # 端口转发到本地"
echo -e "  kubectl port-forward -n $NAMESPACE svc/langchain4j-agent 8080:8080"
echo -e "  # 然后访问 http://localhost:8080"

echo ""
echo -e "${YELLOW}健康检查:${NC}"
echo -e "  kubectl exec -n $NAMESPACE \$(kubectl get pod -n $NAMESPACE -l app=langchain4j-agent -o jsonpath='{.items[0].metadata.name}') -- wget -qO- http://localhost:8080/actuator/health"

echo ""
