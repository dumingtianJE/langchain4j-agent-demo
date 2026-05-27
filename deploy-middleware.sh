#!/bin/bash

# 中间件部署脚本
# 用法: ./deploy-middleware.sh [deploy|upgrade|delete|status] [all|postgresql|redis|milvus|rabbitmq|elasticsearch]

set -e

NAMESPACE="langchain4j-agent"

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

echo_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

echo_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 部署指定中间件
deploy() {
    local component=$1
    
    case $component in
        postgresql)
            echo_info "部署 PostgreSQL..."
            kubectl apply -f k8s/postgresql.yaml
            echo_info "PostgreSQL 部署完成！"
            ;;
        redis)
            echo_info "部署 Redis Sentinel..."
            kubectl apply -f k8s/redis-sentinel.yaml
            echo_info "Redis Sentinel 部署完成！"
            ;;
        milvus)
            echo_info "部署 Milvus Cluster..."
            echo_warn "注意：Milvus 依赖 etcd 和 MinIO，请先部署这些组件"
            kubectl apply -f k8s/milvus-cluster.yaml
            echo_info "Milvus Cluster 部署完成！"
            ;;
        rabbitmq)
            echo_info "部署 RabbitMQ Cluster..."
            kubectl apply -f k8s/rabbitmq.yaml
            echo_info "RabbitMQ Cluster 部署完成！"
            ;;
        elasticsearch)
            echo_info "部署 Elasticsearch Cluster..."
            kubectl apply -f k8s/elasticsearch.yaml
            echo_info "Elasticsearch Cluster 部署完成！"
            ;;
        all)
            echo_info "部署所有中间件..."
            deploy postgresql
            deploy redis
            deploy milvus
            deploy rabbitmq
            deploy elasticsearch
            echo_info "所有中间件部署完成！"
            ;;
        *)
            echo_error "未知的组件: $component"
            echo "支持的组件: postgresql, redis, milvus, rabbitmq, elasticsearch, all"
            exit 1
            ;;
    esac
}

# 升级中间件
upgrade() {
    local component=$1
    
    case $component in
        postgresql)
            echo_info "升级 PostgreSQL..."
            kubectl rollout restart deployment/postgresql -n $NAMESPACE
            kubectl rollout status deployment/postgresql -n $NAMESPACE
            ;;
        redis)
            echo_info "升级 Redis Sentinel..."
            kubectl rollout restart statefulset/redis -n $NAMESPACE
            kubectl rollout status statefulset/redis -n $NAMESPACE
            ;;
        milvus)
            echo_info "升级 Milvus Cluster..."
            kubectl rollout restart deployment/milvus-proxy -n $NAMESPACE
            kubectl rollout restart deployment/milvus-querynode -n $NAMESPACE
            kubectl rollout restart deployment/milvus-datanode -n $NAMESPACE
            ;;
        rabbitmq)
            echo_info "升级 RabbitMQ Cluster..."
            kubectl rollout restart statefulset/rabbitmq -n $NAMESPACE
            ;;
        elasticsearch)
            echo_info "升级 Elasticsearch Cluster..."
            kubectl rollout restart statefulset/elasticsearch -n $NAMESPACE
            ;;
        all)
            echo_info "升级所有中间件..."
            upgrade postgresql
            upgrade redis
            upgrade milvus
            upgrade rabbitmq
            upgrade elasticsearch
            ;;
        *)
            echo_error "未知的组件: $component"
            exit 1
            ;;
    esac
    
    echo_info "$component 升级完成！"
}

# 删除中间件
delete() {
    local component=$1
    
    echo_warn "即将删除 $component！"
    read -p "确认删除？(y/n): " confirm
    if [ "$confirm" != "y" ]; then
        echo_info "取消删除"
        exit 0
    fi
    
    case $component in
        postgresql)
            kubectl delete -f k8s/postgresql.yaml
            ;;
        redis)
            kubectl delete -f k8s/redis-sentinel.yaml
            ;;
        milvus)
            kubectl delete -f k8s/milvus-cluster.yaml
            ;;
        rabbitmq)
            kubectl delete -f k8s/rabbitmq.yaml
            ;;
        elasticsearch)
            kubectl delete -f k8s/elasticsearch.yaml
            ;;
        all)
            kubectl delete -f k8s/postgresql.yaml
            kubectl delete -f k8s/redis-sentinel.yaml
            kubectl delete -f k8s/milvus-cluster.yaml
            kubectl delete -f k8s/rabbitmq.yaml
            kubectl delete -f k8s/elasticsearch.yaml
            ;;
        *)
            echo_error "未知的组件: $component"
            exit 1
            ;;
    esac
    
    echo_info "$component 删除完成！"
}

# 查看状态
status() {
    local component=$1
    
    case $component in
        postgresql)
            echo_info "PostgreSQL 状态："
            kubectl get pods -n $NAMESPACE -l app=postgresql
            kubectl get svc -n $NAMESPACE -l app=postgresql
            ;;
        redis)
            echo_info "Redis Sentinel 状态："
            kubectl get pods -n $NAMESPACE -l app=redis
            kubectl get pods -n $NAMESPACE -l app=redis-sentinel
            kubectl get svc -n $NAMESPACE -l app=redis
            ;;
        milvus)
            echo_info "Milvus Cluster 状态："
            kubectl get pods -n $NAMESPACE -l app=milvus
            kubectl get svc -n $NAMESPACE -l app=milvus
            ;;
        rabbitmq)
            echo_info "RabbitMQ Cluster 状态："
            kubectl get pods -n $NAMESPACE -l app=rabbitmq
            kubectl get svc -n $NAMESPACE -l app=rabbitmq
            ;;
        elasticsearch)
            echo_info "Elasticsearch Cluster 状态："
            kubectl get pods -n $NAMESPACE -l app=elasticsearch
            kubectl get svc -n $NAMESPACE -l app=elasticsearch
            ;;
        all)
            echo_info "所有中间件状态："
            kubectl get pods -n $NAMESPACE -l app=postgresql
            kubectl get pods -n $NAMESPACE -l app=redis
            kubectl get pods -n $NAMESPACE -l app=redis-sentinel
            kubectl get pods -n $NAMESPACE -l app=milvus
            kubectl get pods -n $NAMESPACE -l app=rabbitmq
            kubectl get pods -n $NAMESPACE -l app=elasticsearch
            ;;
        *)
            echo_error "未知的组件: $component"
            exit 1
            ;;
    esac
}

# 主函数
ACTION=${1:-status}
COMPONENT=${2:-all}

case $ACTION in
    deploy)
        deploy $COMPONENT
        ;;
    upgrade)
        upgrade $COMPONENT
        ;;
    delete)
        delete $COMPONENT
        ;;
    status)
        status $COMPONENT
        ;;
    *)
        echo "用法: $0 [deploy|upgrade|delete|status] [all|postgresql|redis|milvus|rabbitmq|elasticsearch]"
        echo ""
        echo "命令:"
        echo "  deploy    - 部署中间件"
        echo "  upgrade   - 升级中间件"
        echo "  delete    - 删除中间件"
        echo "  status    - 查看状态"
        echo ""
        echo "组件:"
        echo "  all            - 所有中间件"
        echo "  postgresql     - PostgreSQL 数据库"
        echo "  redis          - Redis Sentinel 缓存"
        echo "  milvus         - Milvus 向量数据库"
        echo "  rabbitmq       - RabbitMQ 消息队列"
        echo "  elasticsearch  - Elasticsearch 日志系统"
        exit 1
        ;;
esac
