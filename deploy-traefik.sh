#!/bin/bash
# langchain4j-agent K8s 部署脚本（使用 Traefik）

set -e

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

# 部署函数
deploy() {
    info "========================================="
    info "  部署 langchain4j-agent（Traefik）"
    info "========================================="
    echo ""

    # 1. 验证 Traefik
    info "1. 验证 Traefik..."
    if ! sudo kubectl get pods -n kube-system | grep -q traefik; then
        error "Traefik 未运行！"
        exit 1
    fi
    info "✅ Traefik 运行正常"
    echo ""

    # 2. 创建命名空间
    info "2. 创建命名空间..."
    sudo kubectl apply -f k8s/namespace.yaml
    info "✅ 命名空间已创建"
    echo ""

    # 3. 配置 Secret
    info "3. 配置 Secret..."
    warn "请确保已编辑 k8s/secret.yaml，填入实际的 API Key 和密码"
    sudo kubectl apply -f k8s/secret.yaml
    info "✅ Secret 已配置"
    echo ""

    # 4. 配置 ConfigMap
    info "4. 配置 ConfigMap..."
    sudo kubectl apply -f k8s/configmap.yaml
    info "✅ ConfigMap 已配置"
    echo ""

    # 5. 部署应用
    info "5. 部署应用..."
    sudo kubectl apply -f k8s/deployment.yaml
    sudo kubectl apply -f k8s/service.yaml
    info "✅ 应用已部署"
    echo ""

    # 6. 配置 Ingress（使用 Traefik）
    info "6. 配置 Ingress（Traefik）..."
    sudo kubectl apply -f k8s/ingress-traefik.yaml
    info "✅ Ingress 已配置"
    echo ""

    # 7. 等待 Pod 启动
    info "7. 等待 Pod 启动..."
    sleep 20

    # 8. 检查状态
    info "8. 检查部署状态..."
    echo ""
    info "Pod 状态："
    sudo kubectl get pods -n langchain4j-agent
    echo ""

    info "Service 状态："
    sudo kubectl get svc -n langchain4j-agent
    echo ""

    info "Ingress 状态："
    sudo kubectl get ingress -n langchain4j-agent
    echo ""

    # 9. 获取访问信息
    info "9. 获取访问信息..."
    NODE_IP=$(sudo kubectl get nodes -o jsonpath='{.items[0].status.addresses[?(@.type=="InternalIP")].address}')
    HTTP_PORT=$(sudo kubectl get svc -n kube-system traefik -o jsonpath='{.spec.ports[?(@.name=="web")].nodePort}')
    HTTPS_PORT=$(sudo kubectl get svc -n kube-system traefik -o jsonpath='{.spec.ports[?(@.name=="websecure")].nodePort}')

    echo ""
    echo "========================================="
    echo "   部署完成！"
    echo "========================================="
    echo ""
    echo " 访问信息："
    echo "   节点 IP: $NODE_IP"
    echo "   HTTP 端口: $HTTP_PORT"
    if [ -n "$HTTPS_PORT" ]; then
        echo "   HTTPS 端口: $HTTPS_PORT"
    fi
    echo ""
    echo "🌐 访问地址："
    echo "   HTTP:  http://$NODE_IP:$HTTP_PORT"
    if [ -n "$HTTPS_PORT" ]; then
        echo "   HTTPS: https://$NODE_IP:$HTTPS_PORT"
    fi
    echo ""
    echo "🔗 API 接口："
    echo "   健康检查: http://$NODE_IP:$HTTP_PORT/actuator/health"
    echo "   AI 对话:  http://$NODE_IP:$HTTP_PORT/api/ai/chat"
    echo ""
    echo "📊 监控端点："
    echo "   Prometheus: http://$NODE_IP:$HTTP_PORT/actuator/prometheus"
    echo "   详细信息:   http://$NODE_IP:$HTTP_PORT/actuator/info"
    echo ""
    echo "========================================="
    echo ""

    # 10. 测试健康检查
    info "10. 测试健康检查..."
    HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" http://$NODE_IP:$HTTP_PORT/actuator/health || echo "000")
    if [ "$HTTP_CODE" == "200" ]; then
        info "✅ 应用运行正常！HTTP 状态码: $HTTP_CODE"
    else
        warn "⚠️  健康检查返回: $HTTP_CODE"
        warn "   请查看 Pod 日志: sudo kubectl logs -n langchain4j-agent -l app=langchain4j-agent"
    fi
    echo ""

    info "========================================="
    info "  提示"
    info "========================================="
    info "查看 Pod 日志:   sudo kubectl logs -n langchain4j-agent -l app=langchain4j-agent -f"
    info "查看 Pod 详情:   sudo kubectl describe pod -n langchain4j-agent -l app=langchain4j-agent"
    info "删除部署:       sudo ./deploy-traefik.sh delete"
    info "查看状态:       sudo ./deploy-traefik.sh status"
}

# 升级函数
upgrade() {
    info "========================================="
    info "  升级 langchain4j-agent"
    info "========================================="
    echo ""

    info "1. 更新应用..."
    sudo kubectl apply -f k8s/deployment.yaml
    info "✅ 应用已更新"
    echo ""

    info "2. 等待新 Pod 启动..."
    sleep 15

    info "3. 检查状态..."
    sudo kubectl get pods -n langchain4j-agent
    echo ""

    info "✅ 升级完成！"
}

# 回滚函数
rollback() {
    info "========================================="
    info "  回滚 langchain4j-agent"
    info "========================================="
    echo ""

    # 获取上一个版本
    REVISION=$(sudo kubectl rollout history deployment/langchain4j-agent -n langchain4j-agent | tail -2 | head -1 | awk '{print $1}')

    if [ -z "$REVISION" ]; then
        error "没有可回滚的版本"
        exit 1
    fi

    info "回滚到版本: $REVISION"
    sudo kubectl rollout undo deployment/langchain4j-agent -n langchain4j-agent
    info "✅ 回滚完成！"

    info "等待 Pod 重启..."
    sleep 15

    info "检查状态..."
    sudo kubectl get pods -n langchain4j-agent
}

# 状态函数
status() {
    info "========================================="
    info "  langchain4j-agent 状态"
    info "========================================="
    echo ""

    info "Pod 状态："
    sudo kubectl get pods -n langchain4j-agent
    echo ""

    info "Service 状态："
    sudo kubectl get svc -n langchain4j-agent
    echo ""

    info "Ingress 状态："
    sudo kubectl get ingress -n langchain4j-agent
    echo ""

    info "Traefik 状态："
    sudo kubectl get pods -n kube-system | grep traefik
    echo ""

    NODE_IP=$(sudo kubectl get nodes -o jsonpath='{.items[0].status.addresses[?(@.type=="InternalIP")].address}')
    HTTP_PORT=$(sudo kubectl get svc -n kube-system traefik -o jsonpath='{.spec.ports[?(@.name=="web")].nodePort}')

    info "访问地址: http://$NODE_IP:$HTTP_PORT"
}

# 日志函数
logs() {
    info "查看 Pod 日志..."
    sudo kubectl logs -n langchain4j-agent -l app=langchain4j-agent -f
}

# 删除函数
delete() {
    warn "========================================="
    warn "  删除 langchain4j-agent"
    warn "========================================="
    echo ""

    read -p "确认删除？(y/n): " confirm
    if [ "$confirm" != "y" ]; then
        info "取消删除"
        exit 0
    fi

    info "删除资源..."
    sudo kubectl delete -f k8s/ingress-traefik.yaml 2>/dev/null
    sudo kubectl delete -f k8s/service.yaml 2>/dev/null
    sudo kubectl delete -f k8s/deployment.yaml 2>/dev/null
    sudo kubectl delete -f k8s/configmap.yaml 2>/dev/null
    sudo kubectl delete -f k8s/secret.yaml 2>/dev/null
    sudo kubectl delete -f k8s/namespace.yaml 2>/dev/null

    info "✅ 删除完成！"
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
        echo "用法: $0 {deploy|upgrade|rollback|status|logs|delete}"
        echo ""
        echo "命令说明："
        echo "  deploy   - 部署应用（默认）"
        echo "  upgrade  - 升级应用"
        echo "  rollback - 回滚到上一个版本"
        echo "  status   - 查看部署状态"
        echo "  logs     - 查看 Pod 日志"
        echo "  delete   - 删除应用"
        exit 1
        ;;
esac
