#!/bin/bash

# Kubernetes 部署脚本
# 用法: ./deploy.sh [deploy|upgrade|rollback|status|logs|delete]

set -e

NAMESPACE="langchain4j-agent"
APP_NAME="langchain4j-agent"

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

echo_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

echo_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 部署所有资源
deploy() {
    echo_info "开始部署 $APP_NAME 到 Kubernetes..."
    
    # 创建命名空间
    echo_info "1. 创建命名空间..."
    kubectl apply -f k8s/namespace.yaml
    
    # 创建 Secret（需要先修改）
    echo_warn "2. 检查 Secret 配置..."
    echo_warn "请先修改 k8s/secret.yaml 中的敏感信息！"
    read -p "是否已修改 Secret 配置？(y/n): " confirm
    if [ "$confirm" != "y" ]; then
        echo_error "请先修改 k8s/secret.yaml"
        exit 1
    fi
    
    # 按顺序应用资源配置
    echo_info "3. 应用 ConfigMap..."
    kubectl apply -f k8s/configmap.yaml
    
    echo_info "4. 应用 Secret..."
    kubectl apply -f k8s/secret.yaml
    
    echo_info "5. 应用 PVC..."
    kubectl apply -f k8s/pvc.yaml
    
    echo_info "6. 部署 Redis..."
    kubectl apply -f k8s/redis.yaml
    
    echo_info "7. 部署 Milvus..."
    kubectl apply -f k8s/milvus.yaml
    
    echo_info "8. 部署应用..."
    kubectl apply -f k8s/deployment.yaml
    
    echo_info "9. 创建 Service..."
    kubectl apply -f k8s/service.yaml
    
    echo_info "10. 配置 HPA..."
    kubectl apply -f k8s/hpa.yaml
    
    echo_info "11. 配置 PDB..."
    kubectl apply -f k8s/pdb.yaml
    
    echo_info "12. 配置 Ingress..."
    kubectl apply -f k8s/ingress.yaml
    
    echo_info "等待 Pod 启动..."
    sleep 10
    
    # 检查部署状态
    kubectl get pods -n $NAMESPACE
    kubectl get svc -n $NAMESPACE
    
    echo_info "部署完成！"
    echo_info "查看服务状态: kubectl get pods -n $NAMESPACE"
    echo_info "查看服务日志: kubectl logs -f -n $NAMESPACE -l app=$APP_NAME"
}

# 升级部署
upgrade() {
    echo_info "升级 $APP_NAME..."
    
    # 构建新镜像
    echo_info "构建新 Docker 镜像..."
    docker build -t $APP_NAME:latest .
    
    # 推送到镜像仓库（如果需要）
    # docker push your-registry/$APP_NAME:latest
    
    # 滚动更新
    echo_info "执行滚动更新..."
    kubectl rollout restart deployment/$APP_NAME -n $NAMESPACE
    
    echo_info "监控更新进度..."
    kubectl rollout status deployment/$APP_NAME -n $NAMESPACE
    
    echo_info "升级完成！"
}

# 回滚
rollback() {
    echo_info "回滚 $APP_NAME..."
    
    # 显示历史版本
    echo_info "部署历史："
    kubectl rollout history deployment/$APP_NAME -n $NAMESPACE
    
    read -p "回滚到哪个版本？（留空回滚到上一个版本）: " revision
    
    if [ -z "$revision" ]; then
        kubectl rollout undo deployment/$APP_NAME -n $NAMESPACE
    else
        kubectl rollout undo deployment/$APP_NAME -n $NAMESPACE --to-revision=$revision
    fi
    
    echo_info "回滚完成！"
    kubectl rollout status deployment/$APP_NAME -n $NAMESPACE
}

# 查看状态
status() {
    echo_info "服务状态："
    kubectl get pods -n $NAMESPACE
    echo ""
    
    echo_info "服务信息："
    kubectl get svc -n $NAMESPACE
    echo ""
    
    echo_info "HPA 状态："
    kubectl get hpa -n $NAMESPACE
    echo ""
    
    echo_info "PVC 状态："
    kubectl get pvc -n $NAMESPACE
    echo ""
    
    echo_info "Ingress 状态："
    kubectl get ingress -n $NAMESPACE
}

# 查看日志
logs() {
    echo_info "查看应用日志..."
    kubectl logs -f -n $NAMESPACE -l app=$APP_NAME
}

# 删除所有资源
delete() {
    echo_warn "即将删除所有资源！"
    read -p "确认删除？(y/n): " confirm
    if [ "$confirm" != "y" ]; then
        echo_info "取消删除"
        exit 0
    fi
    
    echo_info "删除所有资源..."
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
    
    echo_info "删除完成！"
}

# 主函数
case "${1:-deploy}" in
    deploy)
        deploy
        ;;
    upgrade)
        upgrade
        ;;
    rollback)
        rollback
        ;;
    status)
        status
        ;;
    logs)
        logs
        ;;
    delete)
        delete
        ;;
    *)
        echo "用法: $0 [deploy|upgrade|rollback|status|logs|delete]"
        echo ""
        echo "命令:"
        echo "  deploy    - 部署所有资源"
        echo "  upgrade   - 升级部署"
        echo "  rollback  - 回滚部署"
        echo "  status    - 查看状态"
        echo "  logs      - 查看日志"
        echo "  delete    - 删除所有资源"
        exit 1
        ;;
esac
