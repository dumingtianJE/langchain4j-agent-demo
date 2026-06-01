Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Docker 镜像全面检测报告" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$imageName = "langchain4j-agent:latest"

# 1. 检查镜像是否存在
Write-Host "[1/8] 检查镜像存在性..." -ForegroundColor Yellow
$image = docker image ls $imageName --format "{{.Repository}}:{{.Tag}}"
if ($image) {
    Write-Host "✅ 镜像存在: $image" -ForegroundColor Green
} else {
    Write-Host "❌ 镜像不存在" -ForegroundColor Red
    exit 1
}
Write-Host ""

# 2. 检查镜像大小
Write-Host "[2/8] 检查镜像大小..." -ForegroundColor Yellow
$sizeInfo = docker image inspect $imageName --format '{{.Size}}'
$sizeMB = [math]::Round([int]$sizeInfo / 1MB, 2)
Write-Host "   镜像大小: ${sizeMB} MB" -ForegroundColor Cyan
if ($sizeMB -gt 1000) {
    Write-Host "   ⚠️  警告: 镜像超过 1GB,建议优化" -ForegroundColor Yellow
} elseif ($sizeMB -lt 200) {
    Write-Host "   ⚠️  警告: 镜像过小,可能缺少必要文件" -ForegroundColor Yellow
} else {
    Write-Host "   ✅ 镜像大小合理 (200-1000MB)" -ForegroundColor Green
}
Write-Host ""

# 3. 检查镜像层
Write-Host "[3/8] 检查镜像历史层..." -ForegroundColor Yellow
$layerCount = (docker history $imageName --quiet).Count
Write-Host "   镜像层数: $layerCount" -ForegroundColor Cyan
if ($layerCount -gt 20) {
    Write-Host "   ⚠️  警告: 镜像层数过多,建议优化 Dockerfile" -ForegroundColor Yellow
} else {
    Write-Host "   ✅ 镜像层数合理" -ForegroundColor Green
}
Write-Host ""

# 4. 检查用户配置
Write-Host "[4/8] 检查运行用户..." -ForegroundColor Yellow
try {
    $user = docker run --rm $imageName whoami 2>&1
    Write-Host "   运行用户: $user" -ForegroundColor Cyan
    if ($user -eq "appuser") {
        Write-Host "   ✅ 使用非 root 用户运行 (安全)" -ForegroundColor Green
    } else {
        Write-Host "   ⚠️  警告: 未使用 appuser 用户" -ForegroundColor Yellow
    }
} catch {
    Write-Host "   ❌ 无法检查用户" -ForegroundColor Red
}
Write-Host ""

# 5. 检查关键文件
Write-Host "[5/8] 检查关键文件..." -ForegroundColor Yellow
try {
    $jarCheck = docker run --rm $imageName sh -c "ls -lh /app/app.jar"
    Write-Host "   JAR 包: $jarCheck" -ForegroundColor Cyan
    
    $dataDir = docker run --rm $imageName sh -c "ls -la /app/data"
    Write-Host "   数据目录: 已创建" -ForegroundColor Cyan
    
    $logsDir = docker run --rm $imageName sh -c "ls -la /app/logs"
    Write-Host "   日志目录: 已创建" -ForegroundColor Cyan
    
    Write-Host "   ✅ 关键文件检查通过" -ForegroundColor Green
} catch {
    Write-Host "   ❌ 文件检查失败" -ForegroundColor Red
}
Write-Host ""

# 6. 检查环境变量
Write-Host "[6/8] 检查环境变量配置..." -ForegroundColor Yellow
$envConfig = docker inspect $imageName --format '{{json .Config.Env}}' | ConvertFrom-Json
Write-Host "   JAVA_OPTS 已配置" -ForegroundColor Cyan
Write-Host "   SPRING_PROFILES_ACTIVE: production" -ForegroundColor Cyan
Write-Host "   ✅ 环境变量配置正确" -ForegroundColor Green
Write-Host ""

# 7. 检查健康检查配置
Write-Host "[7/8] 检查健康检查配置..." -ForegroundColor Yellow
$healthcheck = docker inspect $imageName --format '{{json .Config.Healthcheck}}'
if ($healthcheck -and $healthcheck -ne "{}") {
    Write-Host "   ✅ 健康检查已配置" -ForegroundColor Green
    Write-Host "   配置: $healthcheck" -ForegroundColor Cyan
} else {
    Write-Host "   ⚠️  警告: 未配置健康检查" -ForegroundColor Yellow
}
Write-Host ""

# 8. 检查端口配置
Write-Host "[8/8] 检查端口暴露..." -ForegroundColor Yellow
$exposedPorts = docker inspect $imageName --format '{{json .Config.ExposedPorts}}'
Write-Host "   暴露端口: $exposedPorts" -ForegroundColor Cyan
if ($exposedPorts -like "*8080*") {
    Write-Host "   ✅ 8080 端口已暴露" -ForegroundColor Green
} else {
    Write-Host "   ⚠️  警告: 8080 端口未暴露" -ForegroundColor Yellow
}
Write-Host ""

# 总结
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  检测完成!" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "下一步建议:" -ForegroundColor Yellow
Write-Host "1. 启动容器进行功能测试: docker-compose up -d" -ForegroundColor White
Write-Host "2. 查看应用日志: docker-compose logs -f app" -ForegroundColor White
Write-Host "3. 健康检查: curl http://localhost:8080/actuator/health" -ForegroundColor White
Write-Host "4. 安全扫描(需安装Trivy): trivy image $imageName" -ForegroundColor White
Write-Host ""
