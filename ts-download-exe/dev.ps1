# PowerShell 开发启动脚本

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "TS-Merge 开发模式" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# 检查 Wails 是否安装
$wailsCheck = wails --version 2>$null
if ($LASTEXITCODE -ne 0) {
    Write-Host "错误: 未找到 Wails，请先安装" -ForegroundColor Red
    Write-Host "安装命令: go install github.com/wailsapp/wails/v2/cmd/wails@latest" -ForegroundColor Yellow
    exit 1
}

Write-Host "[1/2] 检查前端依赖..." -ForegroundColor Green
Push-Location frontend
if (-not (Test-Path "node_modules")) {
    Write-Host "安装前端依赖..." -ForegroundColor Yellow
    npm install
    if ($LASTEXITCODE -ne 0) {
        Write-Host "错误: 前端依赖安装失败" -ForegroundColor Red
        exit 1
    }
}
Pop-Location

Write-Host "[2/2] 启动开发服务器..." -ForegroundColor Green
Write-Host ""
Write-Host "应用将在 http://localhost:3000 打开" -ForegroundColor Cyan
Write-Host "按 Ctrl+C 停止开发服务器" -ForegroundColor Cyan
Write-Host ""

wails dev

Write-Host ""
Write-Host "开发服务器已停止" -ForegroundColor Yellow
Write-Host ""
