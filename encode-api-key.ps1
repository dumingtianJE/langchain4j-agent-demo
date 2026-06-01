# Base64 编码工具脚本
param(
    [Parameter(Mandatory=$true)]
    [string]$ApiKey
)

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  API Key Base64 编码工具" -ForegroundColor Cyan
Write-Host "========================================`n" -ForegroundColor Cyan

# 进行 base64 编码
$bytes = [System.Text.Encoding]::UTF8.GetBytes($ApiKey)
$base64 = [Convert]::ToBase64String($bytes)

Write-Host "原始 API Key:" -ForegroundColor Yellow
Write-Host "$ApiKey`n" -ForegroundColor White

Write-Host "Base64 编码后:" -ForegroundColor Yellow
Write-Host "$base64`n" -ForegroundColor Green

Write-Host "复制以下内容到 k8s/secret.yaml:" -ForegroundColor Yellow
Write-Host "DASHSCOPE_API_KEY: $base64`n" -ForegroundColor Cyan

Write-Host "验证解码 (确保编码正确):" -ForegroundColor Yellow
$decodedBytes = [Convert]::FromBase64String($base64)
$decoded = [System.Text.Encoding]::UTF8.GetString($decodedBytes)
if ($decoded -eq $ApiKey) {
    Write-Host "✅ 编码验证成功!" -ForegroundColor Green
} else {
    Write-Host "❌ 编码验证失败!" -ForegroundColor Red
}
Write-Host ""
