# Kubernetes 一键部署脚本（Windows PowerShell 版本）
# 用法: .\deploy.ps1 [-Action deploy|upgrade|rollback|status|logs|delete]

param(
    [Parameter(Mandatory=$false)]
    [ValidateSet("deploy", "upgrade", "rollback", "status", "logs", "delete")]
    [string]$Action = "deploy"
)

$namespace = "langchain4j-agent"
$appName = "langchain4j-agent"

function Write-Info {
    param([string]$Message)
    Write-Host "[INFO] $Message" -ForegroundColor Green
}

function Write-Warn {
    param([string]$Message)
    Write-Host "[WARN] $Message" -ForegroundColor Yellow
}

function Write-Error {
    param([string]$Message)
    Write-Host "[ERROR] $Message" -ForegroundColor Red
}

function Deploy {
    Write-Info "开始部署 $appName 到 Kubernetes..."
    
    # 创建命名空间
    Write-Info "1. 创建命名空间..."
    kubectl apply -f k8s/namespace.yaml
    
    # 检查 Secret 配置
    Write-Warn "2. 检查 Secret 配置..."
    Write-Warn "请先修改 k8s/secret.yaml 中的敏感信息！"
    $confirm = Read-Host "是否已修改 Secret 配置？(y/n)"
    if ($confirm -ne "y") {
        Write-Error "请先修改 k8s/secret.yaml"
        exit 1
    }
    
    # 按顺序应用资源配置
    Write-Info "3. 应用 ConfigMap..."
    kubectl apply -f k8s/configmap.yaml
    
    Write-Info "4. 应用 Secret..."
    kubectl apply -f k8s/secret.yaml
    
    Write-Info "5. 应用 PVC..."
    kubectl apply -f k8s/pvc.yaml
    
    Write-Info "6. 部署 Redis..."
    kubectl apply -f k8s/redis.yaml
    
    Write-Info "7. 部署 Milvus..."
    kubectl apply -f k8s/milvus.yaml
    
    Write-Info "8. 部署应用..."
    kubectl apply -f k8s/deployment.yaml
    
    Write-Info "9. 创建 Service..."
    kubectl apply -f k8s/service.yaml
    
    Write-Info "10. 配置 HPA..."
    kubectl apply -f k8s/hpa.yaml
    
    Write-Info "11. 配置 PDB..."
    kubectl apply -f k8s/pdb.yaml
    
    Write-Info "12. 配置 Ingress..."
    kubectl apply -f k8s/ingress.yaml
    
    Write-Info "等待 Pod 启动..."
    Start-Sleep -Seconds 10
    
    # 检查部署状态
    kubectl get pods -n $namespace
    kubectl get svc -n $namespace
    
    Write-Info "部署完成！"
    Write-Info "查看服务状态: kubectl get pods -n $namespace"
    Write-Info "查看服务日志: kubectl logs -f -n $namespace -l app=$appName"
}

function Upgrade {
    Write-Info "升级 $appName..."
    
    # 构建新镜像
    Write-Info "构建新 Docker 镜像..."
    docker build -t "$appName`:latest" .
    
    # 滚动更新
    Write-Info "执行滚动更新..."
    kubectl rollout restart "deployment/$appName" -n $namespace
    
    Write-Info "监控更新进度..."
    kubectl rollout status "deployment/$appName" -n $namespace
    
    Write-Info "升级完成！"
}

function Rollback {
    Write-Info "回滚 $appName..."
    
    # 显示历史版本
    Write-Info "部署历史："
    kubectl rollout history "deployment/$appName" -n $namespace
    
    $revision = Read-Host "回滚到哪个版本？（留空回滚到上一个版本）"
    
    if ([string]::IsNullOrEmpty($revision)) {
        kubectl rollout undo "deployment/$appName" -n $namespace
    } else {
        kubectl rollout undo "deployment/$appName" -n $namespace --to-revision=$revision
    }
    
    Write-Info "回滚完成！"
    kubectl rollout status "deployment/$appName" -n $namespace
}

function Status {
    Write-Info "服务状态："
    kubectl get pods -n $namespace
    Write-Host ""
    
    Write-Info "服务信息："
    kubectl get svc -n $namespace
    Write-Host ""
    
    Write-Info "HPA 状态："
    kubectl get hpa -n $namespace
    Write-Host ""
    
    Write-Info "PVC 状态："
    kubectl get pvc -n $namespace
    Write-Host ""
    
    Write-Info "Ingress 状态："
    kubectl get ingress -n $namespace
}

function Logs {
    Write-Info "查看应用日志..."
    kubectl logs -f -n $namespace -l app=$appName
}

function Delete {
    Write-Warn "即将删除所有资源！"
    $confirm = Read-Host "确认删除？(y/n)"
    if ($confirm -ne "y") {
        Write-Info "取消删除"
        exit 0
    }
    
    Write-Info "删除所有资源..."
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
    
    Write-Info "删除完成！"
}

# 主函数
switch ($Action) {
    "deploy" { Deploy }
    "upgrade" { Upgrade }
    "rollback" { Rollback }
    "status" { Status }
    "logs" { Logs }
    "delete" { Delete }
    default {
        Write-Host "用法: .\deploy.ps1 [-Action deploy|upgrade|rollback|status|logs|delete]"
        Write-Host ""
        Write-Host "命令:"
        Write-Host "  deploy    - 部署所有资源"
        Write-Host "  upgrade   - 升级部署"
        Write-Host "  rollback  - 回滚部署"
        Write-Host "  status    - 查看状态"
        Write-Host "  logs      - 查看日志"
        Write-Host "  delete    - 删除所有资源"
        exit 1
    }
}
