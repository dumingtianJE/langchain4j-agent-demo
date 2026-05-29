#!/bin/bash
# ============================================
# k3s Docker 镜像构建脚本 (Linux/Mac)
# ============================================

set -e

echo "========================================"
echo "  k3s Docker 镜像构建脚本"
echo "========================================"
echo ""

# 配置变量
IMAGE_NAME="langchain4j-agent-demo"
IMAGE_TAG="v1.0.0"
FULL_IMAGE_NAME="${IMAGE_NAME}:${IMAGE_TAG}"
DOCKERFILE="Dockerfile.k3s"

# 颜色定义
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

echo -e "${YELLOW}[1/4] 检查 Docker 环境...${NC}"
if ! docker version > /dev/null 2>&1; then
    echo -e "${RED}  ✗ 错误: Docker 守护进程未运行${NC}"
    echo -e "${YELLOW}  请先启动 Docker 服务${NC}"
    exit 1
fi
DOCKER_VERSION=$(docker version --format '{{.Server.Version}}')
echo -e "${GREEN}  ✓ Docker 版本: $DOCKER_VERSION${NC}"

echo ""
echo -e "${YELLOW}[2/4] 清理旧的构建产物...${NC}"
if [ -d "target" ]; then
    rm -rf target
    echo -e "${GREEN}  ✓ 已清理 target 目录${NC}"
else
    echo -e "  - target 目录不存在，跳过清理"
fi

echo ""
echo -e "${YELLOW}[3/4] 构建 JAR 包...${NC}"
mvn clean package -DskipTests -B
if [ $? -ne 0 ]; then
    echo -e "${RED}  ✗ Maven 构建失败${NC}"
    exit 1
fi
echo -e "${GREEN}  ✓ JAR 包构建成功${NC}"

echo ""
echo -e "${YELLOW}[4/4] 构建 Docker 镜像 ($FULL_IMAGE_NAME)...${NC}"
docker build -f $DOCKERFILE -t $FULL_IMAGE_NAME .

if [ $? -ne 0 ]; then
    echo -e "${RED}  ✗ Docker 镜像构建失败${NC}"
    exit 1
fi

echo ""
echo "========================================"
echo -e "${GREEN}  构建完成！${NC}"
echo "========================================"
echo ""
echo -e "${YELLOW}镜像信息:${NC}"
echo -e "  镜像名称: $FULL_IMAGE_NAME"
docker images $IMAGE_NAME --filter "reference=$FULL_IMAGE_NAME" --format "  镜像ID: {{.ID}}\n  大小: {{.Size}}\n  创建时间: {{.CreatedAt}}"
echo ""

# 询问是否保存镜像为 tar 文件
echo -e "${YELLOW}是否保存镜像为 tar 文件以便导入到 k3s 节点? (y/n)${NC}"
read -r save_response

if [[ "$save_response" =~ ^[Yy]$ ]]; then
    TAR_FILE="${IMAGE_NAME}-${IMAGE_TAG}.tar"
    echo ""
    echo -e "${YELLOW}正在保存镜像到 $TAR_FILE ...${NC}"
    docker save -o $TAR_FILE $FULL_IMAGE_NAME
    
    if [ $? -eq 0 ]; then
        FILE_SIZE=$(du -h $TAR_FILE | cut -f1)
        echo -e "${GREEN}  ✓ 镜像已保存: $TAR_FILE ($FILE_SIZE)${NC}"
        echo ""
        echo -e "${CYAN}在 k3s 节点上导入镜像的命令:${NC}"
        echo -e "  sudo k3s ctr images import $TAR_FILE"
        echo -e "  或"
        echo -e "  sudo docker load -i $TAR_FILE"
        echo -e "  sudo k3s ctr images tag docker.io/library/$FULL_IMAGE_NAME docker.io/library/$FULL_IMAGE_NAME"
    else
        echo -e "${RED}  ✗ 保存镜像失败${NC}"
    fi
fi

echo ""
echo -e "${CYAN}下一步操作:${NC}"
echo -e "  1. 推送镜像到仓库（可选）:"
echo -e "     docker tag $FULL_IMAGE_NAME your-registry/$FULL_IMAGE_NAME"
echo -e "     docker push your-registry/$FULL_IMAGE_NAME"
echo ""
echo -e "  2. 在 k3s 中部署:"
echo -e "     kubectl apply -f k8s/"
echo ""
