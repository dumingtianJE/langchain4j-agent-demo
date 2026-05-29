# ============================================
# k3s Docker 镜像构建脚本
# ============================================

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  k3s Docker 镜像构建脚本" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# 配置变量
$IMAGE_NAME = "langchain4j-agent-demo"
$IMAGE_TAG = "v1.0.0"
$FULL_IMAGE_NAME = "${IMAGE_NAME}:${IMAGE_TAG}"
$DOCKERFILE = "Dockerfile.k3s"

Write-Host "[1/4] 检查 Docker 环境..." -ForegroundColor Yellow
try {
    $dockerVersion = docker version --format '{{.Server.Version}}' 2>&1
    if ($LASTEXITCODE -ne 0) {
        throw "Docker daemon is not running"
    }
    Write-Host "  ✓ Docker 版本: $dockerVersion" -ForegroundColor Green
} catch {
    Write-Host "  ✗ 错误: Docker 守护进程未运行" -ForegroundColor Red
    Write-Host "  请先启动 Docker Desktop 或 Docker 服务" -ForegroundColor Yellow
    exit 1
}

Write-Host ""
Write-Host "[2/4] 清理旧的构建产物..." -ForegroundColor Yellow
if (Test-Path "target") {
    Remove-Item -Recurse -Force "target"
    Write-Host "  ✓ 已清理 target 目录" -ForegroundColor Green
} else {
    Write-Host "  - target 目录不存在，跳过清理" -ForegroundColor Gray
}

Write-Host ""
Write-Host "[3/4] 构建 JAR 包..." -ForegroundColor Yellow
mvn clean package -DskipTests -B
if ($LASTEXITCODE -ne 0) {
    Write-Host "  ✗ Maven 构建失败" -ForegroundColor Red
    exit 1
}
Write-Host "  ✓ JAR 包构建成功" -ForegroundColor Green

Write-Host ""
Write-Host "[4/4] 构建 Docker 镜像 ($FULL_IMAGE_NAME)..." -ForegroundColor Yellow
docker build -f $DOCKERFILE -t $FULL_IMAGE_NAME .

if ($LASTEXITCODE -ne 0) {
    Write-Host "  ✗ Docker 镜像构建失败" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  构建完成！" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "镜像信息:" -ForegroundColor Yellow
Write-Host "  镜像名称: $FULL_IMAGE_NAME" -ForegroundColor White
docker images $IMAGE_NAME -f "reference=$FULL_IMAGE_NAME" --format "  镜像ID: {{.ID}}`n  大小: {{.Size}}`n  创建时间: {{.CreatedAt}}"
Write-Host ""

# 询问是否保存镜像为 tar 文件
Write-Host "是否保存镜像为 tar 文件以便导入到 k3s 节点? (Y/N)" -ForegroundColor Yellow
$saveResponse = Read-Host

if ($saveResponse -eq "Y" -or $saveResponse -eq "y") {
    $tarFileName = "${IMAGE_NAME}-${IMAGE_TAG}.tar"
    Write-Host ""
    Write-Host "正在保存镜像到 $tarFileName ..." -ForegroundColor Yellow
    docker save -o $tarFileName $FULL_IMAGE_NAME
    
    if ($LASTEXITCODE -eq 0) {
        $fileSize = (Get-Item $tarFileName).Length / 1MB
        Write-Host "  ✓ 镜像已保存: $tarFileName ($([math]::Round($fileSize, 2)) MB)" -ForegroundColor Green
        Write-Host ""
        Write-Host "在 k3s 节点上导入镜像的命令:" -ForegroundColor Cyan
        Write-Host "  sudo k3s ctr images import $tarFileName" -ForegroundColor White
        Write-Host "  或" -ForegroundColor White
        Write-Host "  sudo docker load -i $tarFileName" -ForegroundColor White
        Write-Host "  sudo k3s ctr images tag docker.io/library/$FULL_IMAGE_NAME docker.io/library/$FULL_IMAGE_NAME" -ForegroundColor White
    } else {
        Write-Host "  ✗ 保存镜像失败" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "下一步操作:" -ForegroundColor Cyan
Write-Host "  1. 推送镜像到仓库（可选）:" -ForegroundColor White
Write-Host "     docker tag $FULL_IMAGE_NAME your-registry/$FULL_IMAGE_NAME" -ForegroundColor Gray
Write-Host "     docker push your-registry/$FULL_IMAGE_NAME" -ForegroundColor Gray
Write-Host ""
Write-Host "  2. 在 k3s 中部署:" -ForegroundColor White
Write-Host "     kubectl apply -f k8s/" -ForegroundColor Gray
Write-Host ""
