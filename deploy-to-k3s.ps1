# K3s 云服务器部署脚本
param(
    [string]$ServerIP = "106.12.23.114",
    [string]$ServerPort = "22",
    [string]$User = "root"
)

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  K3s 云服务器部署工具" -ForegroundColor Cyan
Write-Host "========================================`n" -ForegroundColor Cyan

$server = "${User}@${ServerIP}"
$sshPort = "-p ${ServerPort}"

# 1. 检查 SSH 连接
Write-Host "[1/7] 测试 SSH 连接..." -ForegroundColor Yellow
try {
    ssh ${sshPort} $server "echo 'Connection OK'" 2>&1 | Out-Null
    Write-Host "✅ SSH 连接成功" -ForegroundColor Green
} catch {
    Write-Host "❌ SSH 连接失败,请检查:" -ForegroundColor Red
    Write-Host "   - 服务器地址: ${ServerIP}" -ForegroundColor White
    Write-Host "   - SSH 端口: ${ServerPort}" -ForegroundColor White
    Write-Host "   - 用户: ${User}" -ForegroundColor White
    Write-Host "   - SSH 密钥是否配置" -ForegroundColor White
    exit 1
}
Write-Host ""

# 2. 检查 k3s 状态
Write-Host "[2/7] 检查 k3s 状态..." -ForegroundColor Yellow
$k3sStatus = ssh ${sshPort} $server "k3s kubectl cluster-info" 2>&1
if ($LASTEXITCODE -eq 0) {
    Write-Host "✅ k3s 集群正常运行" -ForegroundColor Green
} else {
    Write-Host "❌ k3s 集群异常" -ForegroundColor Red
    Write-Host $k3sStatus -ForegroundColor Red
    exit 1
}
Write-Host ""

# 3. 清理构建失败的镜像
Write-Host "[3/7] 清理构建失败的镜像..." -ForegroundColor Yellow
$failedImages = ssh ${sshPort} $server "docker images --filter 'dangling=true' --format '{{.ID}} {{.Size}}'" 2>&1
if ($failedImages) {
    Write-Host "发现以下悬空镜像:" -ForegroundColor Yellow
    Write-Host $failedImages -ForegroundColor White
    ssh ${sshPort} $server "docker image prune -f" 2>&1 | Out-Null
    Write-Host "✅ 已清理悬空镜像" -ForegroundColor Green
} else {
    Write-Host "✅ 无需清理的镜像" -ForegroundColor Green
}
Write-Host ""

# 4. 检查当前部署状态
Write-Host "[4/7] 检查当前部署状态..." -ForegroundColor Yellow
ssh ${sshPort} $server "k3s kubectl get pods -n langchain4j-agent" 2>&1
Write-Host ""

# 5. 创建命名空间
Write-Host "[5/7] 创建命名空间..." -ForegroundColor Yellow
ssh ${sshPort} $server "k3s kubectl create namespace langchain4j-agent --dry-run=client -o yaml | k3s kubectl apply -f -" 2>&1
Write-Host "✅ 命名空间已准备" -ForegroundColor Green
Write-Host ""

# 6. 部署应用配置
Write-Host "[6/7] 部署应用配置..." -ForegroundColor Yellow

$configs = @(
    "namespace.yaml",
    "secret.yaml",
    "configmap.yaml",
    "pvc.yaml",
    "service.yaml",
    "deployment.yaml"
)

foreach ($config in $configs) {
    Write-Host "   部署: $config" -ForegroundColor Cyan
    scp ${sshPort} "k8s\${config}" "${server}:/tmp/${config}" 2>&1 | Out-Null
    ssh ${sshPort} $server "k3s kubectl apply -f /tmp/${config} -n langchain4j-agent" 2>&1
}

Write-Host "✅ 应用配置已部署" -ForegroundColor Green
Write-Host ""

# 7. 检查部署状态
Write-Host "[7/7] 检查部署状态..." -ForegroundColor Yellow
Start-Sleep -Seconds 5

ssh ${sshPort} $server "k3s kubectl get all -n langchain4j-agent" 2>&1
Write-Host ""

ssh ${sshPort} $server "k3s kubectl get pods -n langchain4j-agent" 2>&1
Write-Host ""

# 等待 Pod 启动
Write-Host "等待 Pod 启动..." -ForegroundColor Cyan
Start-Sleep -Seconds 10

$podStatus = ssh ${sshPort} $server "k3s kubectl get pods -n langchain4j-agent -o jsonpath='{.items[0].status.phase}'" 2>&1
if ($podStatus -eq "Running") {
    Write-Host "✅ 应用部署成功!" -ForegroundColor Green
} else {
    Write-Host "⚠️  Pod 状态: $podStatus" -ForegroundColor Yellow
    Write-Host "   查看日志: ssh ${sshPort} ${server} 'k3s kubectl logs -n langchain4j-agent <pod-name>'" -ForegroundColor White
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  部署完成!" -ForegroundColor Cyan
Write-Host "========================================`n" -ForegroundColor Cyan

Write-Host " 常用命令:" -ForegroundColor Yellow
Write-Host "   查看 Pod 状态: ssh ${sshPort} ${server} 'k3s kubectl get pods -n langchain4j-agent'" -ForegroundColor White
Write-Host "   查看日志: ssh ${sshPort} ${server} 'k3s kubectl logs -n langchain4j-agent -l app=langchain4j-agent'" -ForegroundColor White
Write-Host "   查看服务: ssh ${sshPort} ${server} 'k3s kubectl get svc -n langchain4j-agent'" -ForegroundColor White
Write-Host ""
