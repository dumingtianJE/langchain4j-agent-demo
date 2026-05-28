#!/bin/bash
# 导入所有中间件镜像到 k3s

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

echo "========================================="
echo "  导入中间件镜像到 k3s"
echo "========================================="
echo ""

# 检查 k3s 是否运行
if ! systemctl is-active --quiet k3s; then
    error "k3s 未运行！"
    exit 1
fi
info "✅ k3s 运行正常"
echo ""

# 导入 Redis
info "1. 导入 Redis 镜像..."
if [ -f /root/redis-7-alpine.tar ]; then
    sudo k3s ctr images import /root/redis-7-alpine.tar
    info "✅ Redis 镜像已导入"
else
    warn "⚠️  未找到 redis-7-alpine.tar，跳过"
fi
echo ""

# 导入 Milvus
info "2. 导入 Milvus 镜像..."
if [ -f /root/milvus-v2.3.0.tar ]; then
    sudo k3s ctr images import /root/milvus-v2.3.0.tar
    info "✅ Milvus 镜像已导入"
else
    warn "⚠️  未找到 milvus-v2.3.0.tar，跳过"
fi
echo ""

# 导入 PostgreSQL
info "3. 导入 PostgreSQL 镜像..."
if [ -f /root/postgres-15-alpine.tar ]; then
    sudo k3s ctr images import /root/postgres-15-alpine.tar
    info "✅ PostgreSQL 镜像已导入"
else
    warn "⏭️  未找到 postgres-15-alpine.tar（可选）"
fi
echo ""

# 导入 RabbitMQ
info "4. 导入 RabbitMQ 镜像..."
if [ -f /root/rabbitmq-3-management-alpine.tar ]; then
    sudo k3s ctr images import /root/rabbitmq-3-management-alpine.tar
    info "✅ RabbitMQ 镜像已导入"
else
    warn "⏭️  未找到 rabbitmq-3-management-alpine.tar（可选）"
fi
echo ""

# 导入 Elasticsearch
info "5. 导入 Elasticsearch 镜像..."
if [ -f /root/elasticsearch-8.11.0.tar ]; then
    sudo k3s ctr images import /root/elasticsearch-8.11.0.tar
    info "✅ Elasticsearch 镜像已导入"
else
    warn "⏭️  未找到 elasticsearch-8.11.0.tar（可选）"
fi
echo ""

# 验证所有镜像
info "6. 验证导入的镜像..."
echo ""
echo "已导入的中间件镜像："
sudo k3s crictl images | grep -E "redis|milvus|postgres|rabbitmq|elasticsearch" || echo "未找到镜像"
echo ""

# 统计
TOTAL=$(sudo k3s crictl images | grep -E "redis|milvus|postgres|rabbitmq|elasticsearch" | wc -l)
info "成功导入 $TOTAL 个中间件镜像"
echo ""

echo "========================================="
echo "  镜像导入完成！"
echo "========================================="
echo ""
info "现在可以运行部署脚本："
info "  sudo ./deploy-full.sh deploy"
echo ""
