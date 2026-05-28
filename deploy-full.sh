#!/bin/bash
# langchain4j-agent 完整部署脚本（按依赖顺序）

set -e

# 颜色定义
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
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

step() {
    echo ""
    echo -e "${BLUE}========================================${NC}"
    echo -e "${BLUE}  $1${NC}"
    echo -e "${BLUE}========================================${NC}"
    echo ""
}

# 部署中间件
deploy_middleware() {
    step "部署中间件（按依赖顺序）"

    # 1. 创建命名空间
    info "1. 创建命名空间..."
    sudo kubectl apply -f k8s/namespace.yaml
    sleep 2
    info "✅ 命名空间已创建"
    echo ""

    # 2. 配置 Secret
    info "2. 配置 Secret..."
    warn "请确保已编辑 k8s/secret.yaml，填入实际的密钥"
    sudo kubectl apply -f k8s/secret.yaml
    info "✅ Secret 已配置"
    echo ""

    # 3. 配置 PVC（持久化存储）
    info "3. 配置 PVC..."
    sudo kubectl apply -f k8s/pvc.yaml
    info "✅ PVC 已配置"
    echo ""

    # 4. 部署 Redis（基础缓存）
    info "4. 部署 Redis..."
    sudo kubectl apply -f k8s/redis.yaml
    info "⏳ 等待 Redis 启动..."
    sleep 15
    
    # 验证 Redis
    if sudo kubectl get pods -n langchain4j-agent -l app=redis | grep -q "Running"; then
        info "✅ Redis 运行正常"
    else
        warn "⚠️  Redis 可能还在启动中，继续部署..."
    fi
    echo ""

    # 5. 部署 Milvus（向量数据库）
    info "5. 部署 Milvus（向量数据库）..."
    sudo kubectl apply -f k8s/milvus.yaml
    info "⏳ 等待 Milvus 启动..."
    sleep 20
    
    # 验证 Milvus
    if sudo kubectl get pods -n langchain4j-agent -l app=milvus | grep -q "Running"; then
        info "✅ Milvus 运行正常"
    else
        warn "⚠️  Milvus 可能还在启动中，继续部署..."
    fi
    echo ""

    # 6. 部署 PostgreSQL（关系型数据库，可选）
    info "6. 部署 PostgreSQL（可选）..."
    if [ "${DEPLOY_POSTGRESQL:-no}" == "yes" ]; then
        sudo kubectl apply -f k8s/postgresql.yaml
        info "⏳ 等待 PostgreSQL 启动..."
        sleep 20
        
        if sudo kubectl get pods -n langchain4j-agent -l app=postgresql | grep -q "Running"; then
            info "✅ PostgreSQL 运行正常"
        fi
    else
        info "⏭️  跳过 PostgreSQL（设置 DEPLOY_POSTGRESQL=yes 启用）"
    fi
    echo ""

    # 7. 部署 RabbitMQ（消息队列，可选）
    info "7. 部署 RabbitMQ（可选）..."
    if [ "${DEPLOY_RABBITMQ:-no}" == "yes" ]; then
        sudo kubectl apply -f k8s/rabbitmq.yaml
        info " 等待 RabbitMQ 启动..."
        sleep 15
        
        if sudo kubectl get pods -n langchain4j-agent -l app=rabbitmq | grep -q "Running"; then
            info "✅ RabbitMQ 运行正常"
        fi
    else
        info "⏭️  跳过 RabbitMQ（设置 DEPLOY_RABBITMQ=yes 启用）"
    fi
    echo ""

    # 8. 部署 Elasticsearch（日志搜索，可选）
    info "8. 部署 Elasticsearch（可选）..."
    if [ "${DEPLOY_ELASTICSEARCH:-no}" == "yes" ]; then
        sudo kubectl apply -f k8s/elasticsearch.yaml
        info " 等待 Elasticsearch 启动..."
        sleep 20
        
        if sudo kubectl get pods -n langchain4j-agent -l app=elasticsearch | grep -q "Running"; then
            info "✅ Elasticsearch 运行正常"
        fi
    else
        info "⏭️  跳过 Elasticsearch（设置 DEPLOY_ELASTICSEARCH=yes 启用）"
    fi
    echo ""

    step "中间件部署完成"
}

# 部署应用
deploy_app() {
    step "部署应用"

    # 1. 配置 ConfigMap
    info "1. 配置 ConfigMap..."
    sudo kubectl apply -f k8s/configmap.yaml
    info "✅ ConfigMap 已配置"
    echo ""

    # 2. 部署应用
    info "2. 部署 langchain4j-agent..."
    sudo kubectl apply -f k8s/deployment.yaml
    sudo kubectl apply -f k8s/service.yaml
    info "✅ 应用已部署"
    echo ""

    # 3. 配置 Ingress（使用 Traefik）
    info "3. 配置 Ingress（Traefik）..."
    sudo kubectl apply -f k8s/ingress-traefik.yaml
    info "✅ Ingress 已配置"
    echo ""

    # 4. 配置 HPA（自动扩缩容，可选）
    info "4. 配置 HPA（可选）..."
    if [ "${DEPLOY_HPA:-no}" == "yes" ]; then
        sudo kubectl apply -f k8s/hpa.yaml
        sudo kubectl apply -f k8s/pdb.yaml
        info "✅ HPA 已配置"
    else
        info "⏭️  跳过 HPA（设置 DEPLOY_HPA=yes 启用）"
    fi
    echo ""

    # 5. 等待 Pod 启动
    info "5. 等待 Pod 启动..."
    sleep 30

    step "应用部署完成"
}

# 验证部署
verify_deployment() {
    step "验证部署"

    # 1. 检查所有 Pod
    info "1. 检查 Pod 状态..."
    echo ""
    sudo kubectl get pods -n langchain4j-agent
    echo ""

    # 2. 检查 Service
    info "2. 检查 Service..."
    echo ""
    sudo kubectl get svc -n langchain4j-agent
    echo ""

    # 3. 检查 Ingress
    info "3. 检查 Ingress..."
    echo ""
    sudo kubectl get ingress -n langchain4j-agent
    echo ""

    # 4. 检查中间件
    info "4. 检查中间件状态..."
    echo ""
    info "Redis:"
    sudo kubectl get pods -n langchain4j-agent -l app=redis
    echo ""
    
    info "Milvus:"
    sudo kubectl get pods -n langchain4j-agent -l app=milvus
    echo ""

    # 5. 测试应用健康检查
    info "5. 测试应用健康检查..."
    NODE_IP=$(sudo kubectl get nodes -o jsonpath='{.items[0].status.addresses[?(@.type=="InternalIP")].address}')
    HTTP_PORT=$(sudo kubectl get svc -n kube-system traefik -o jsonpath='{.spec.ports[?(@.name=="web")].nodePort}')

    echo ""
    info "访问地址: http://$NODE_IP:$HTTP_PORT"
    echo ""

    HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" http://$NODE_IP:$HTTP_PORT/actuator/health 2>/dev/null || echo "000")
    if [ "$HTTP_CODE" == "200" ]; then
        info "✅ 应用运行正常！HTTP 状态码: $HTTP_CODE"
    else
        warn "️  健康检查返回: $HTTP_CODE"
        warn "   请查看 Pod 日志: sudo kubectl logs -n langchain4j-agent -l app=langchain4j-agent"
    fi
    echo ""

    # 6. 显示访问信息
    echo "========================================="
    echo "  部署完成！"
    echo "========================================="
    echo ""
    echo " 访问信息："
    echo "   节点 IP: $NODE_IP"
    echo "   HTTP 端口: $HTTP_PORT"
    echo ""
    echo " 访问地址："
    echo "   🌐 主页面:    http://$NODE_IP:$HTTP_PORT"
    echo "   🔗 API 接口:  http://$NODE_IP:$HTTP_PORT/api"
    echo "   ❤️  健康检查: http://$NODE_IP:$HTTP_PORT/actuator/health"
    echo "    Prometheus: http://$NODE_IP:$HTTP_PORT/actuator/prometheus"
    echo ""
    echo " 中间件访问："
    echo "   Redis:     redis://redis-service:6379"
    echo "   Milvus:    http://milvus-service:19530"
    if [ "${DEPLOY_POSTGRESQL:-no}" == "yes" ]; then
        echo "   PostgreSQL: postgresql-service:5432"
    fi
    if [ "${DEPLOY_RABBITMQ:-no}" == "yes" ]; then
        echo "   RabbitMQ:  rabbitmq-service:5672"
    fi
    if [ "${DEPLOY_ELASTICSEARCH:-no}" == "yes" ]; then
        echo "   Elasticsearch: http://elasticsearch:9200"
    fi
    echo ""
    echo "========================================="
    echo ""

    # 7. 运维提示
    info "运维命令："
    info "  查看日志:   sudo kubectl logs -n langchain4j-agent -l app=langchain4j-agent -f"
    info "  查看状态:   sudo ./deploy-full.sh status"
    info "  删除部署:   sudo ./deploy-full.sh delete"
    echo ""
}

# 完整部署
deploy_all() {
    step "开始完整部署 langchain4j-agent"

    # 验证 Traefik
    info "验证 Traefik..."
    if ! sudo kubectl get pods -n kube-system | grep -q traefik; then
        error "Traefik 未运行！请先确保 k3s 正常运行。"
        exit 1
    fi
    info "✅ Traefik 运行正常"
    echo ""

    # 部署中间件
    deploy_middleware

    # 部署应用
    deploy_app

    # 验证部署
    verify_deployment
}

# 状态检查
status() {
    step "langchain4j-agent 部署状态"

    info "Pod 状态："
    sudo kubectl get pods -n langchain4j-agent
    echo ""

    info "Service 状态："
    sudo kubectl get svc -n langchain4j-agent
    echo ""

    info "Ingress 状态："
    sudo kubectl get ingress -n langchain4j-agent
    echo ""

    info "PVC 状态："
    sudo kubectl get pvc -n langchain4j-agent
    echo ""

    NODE_IP=$(sudo kubectl get nodes -o jsonpath='{.items[0].status.addresses[?(@.type=="InternalIP")].address}')
    HTTP_PORT=$(sudo kubectl get svc -n kube-system traefik -o jsonpath='{.spec.ports[?(@.name=="web")].nodePort}')

    info "访问地址: http://$NODE_IP:$HTTP_PORT"
}

# 日志查看
logs() {
    SERVICE=${1:-app}
    info "查看 $SERVICE 日志..."
    
    case $SERVICE in
        app)
            sudo kubectl logs -n langchain4j-agent -l app=langchain4j-agent -f
            ;;
        redis)
            sudo kubectl logs -n langchain4j-agent -l app=redis -f
            ;;
        milvus)
            sudo kubectl logs -n langchain4j-agent -l app=milvus -f
            ;;
        postgresql)
            sudo kubectl logs -n langchain4j-agent -l app=postgresql -f
            ;;
        rabbitmq)
            sudo kubectl logs -n langchain4j-agent -l app=rabbitmq -f
            ;;
        elasticsearch)
            sudo kubectl logs -n langchain4j-agent -l app=elasticsearch -f
            ;;
        *)
            error "未知的服务: $SERVICE"
            info "可用的服务: app, redis, milvus, postgresql, rabbitmq, elasticsearch"
            exit 1
            ;;
    esac
}

# 删除部署
delete_all() {
    warn "========================================="
    warn "  删除所有部署"
    warn "========================================="
    echo ""

    read -p "确认删除所有资源？(y/n): " confirm
    if [ "$confirm" != "y" ]; then
        info "取消删除"
        exit 0
    fi

    info "删除资源（反向顺序）..."
    
    sudo kubectl delete -f k8s/ingress-traefik.yaml 2>/dev/null
    sudo kubectl delete -f k8s/hpa.yaml 2>/dev/null
    sudo kubectl delete -f k8s/pdb.yaml 2>/dev/null
    sudo kubectl delete -f k8s/service.yaml 2>/dev/null
    sudo kubectl delete -f k8s/deployment.yaml 2>/dev/null
    sudo kubectl delete -f k8s/configmap.yaml 2>/dev/null
    sudo kubectl delete -f k8s/elasticsearch.yaml 2>/dev/null
    sudo kubectl delete -f k8s/rabbitmq.yaml 2>/dev/null
    sudo kubectl delete -f k8s/postgresql.yaml 2>/dev/null
    sudo kubectl delete -f k8s/milvus.yaml 2>/dev/null
    sudo kubectl delete -f k8s/redis.yaml 2>/dev/null
    sudo kubectl delete -f k8s/pvc.yaml 2>/dev/null
    sudo kubectl delete -f k8s/secret.yaml 2>/dev/null
    sudo kubectl delete -f k8s/namespace.yaml 2>/dev/null

    info "✅ 所有资源已删除"
}

# 主函数
case "${1:-deploy}" in
    deploy)
        deploy_all
        ;;
    status)
        status
        ;;
    logs)
        logs "${2:-app}"
        ;;
    delete)
        delete_all
        ;;
    *)
        echo "用法: $0 {deploy|status|logs|delete}"
        echo ""
        echo "命令说明："
        echo "  deploy              - 完整部署（默认）"
        echo "  status              - 查看部署状态"
        echo "  logs <service>      - 查看服务日志"
        echo "                        可用服务: app, redis, milvus, postgresql, rabbitmq, elasticsearch"
        echo "  delete              - 删除所有资源"
        echo ""
        echo "环境变量："
        echo "  DEPLOY_POSTGRESQL=yes      - 部署 PostgreSQL"
        echo "  DEPLOY_RABBITMQ=yes        - 部署 RabbitMQ"
        echo "  DEPLOY_ELASTICSEARCH=yes   - 部署 Elasticsearch"
        echo "  DEPLOY_HPA=yes             - 启用自动扩缩容"
        echo ""
        echo "示例："
        echo "  sudo ./deploy-full.sh deploy"
        echo "  sudo DEPLOY_POSTGRESQL=yes DEPLOY_RABBITMQ=yes ./deploy-full.sh deploy"
        echo "  sudo ./deploy-full.sh logs app"
        echo "  sudo ./deploy-full.sh logs redis"
        echo "  sudo ./deploy-full.sh status"
        echo "  sudo ./deploy-full.sh delete"
        exit 1
        ;;
esac
