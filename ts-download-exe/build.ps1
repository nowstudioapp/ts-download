# PowerShell 构建脚本

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "TS-Merge 构建脚本" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# 检查 Wails 是否安装
$wailsCheck = wails --version 2>$null
if ($LASTEXITCODE -ne 0) {
    Write-Host "错误: 未找到 Wails，请先安装" -ForegroundColor Red
    Write-Host "安装命令: go install github.com/wailsapp/wails/v2/cmd/wails@latest" -ForegroundColor Yellow
    exit 1
}

Write-Host "[1/3] 检查前端依赖..." -ForegroundColor Green
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

Write-Host "[2/3] 构建前端..." -ForegroundColor Green
Push-Location frontend
npm run build
if ($LASTEXITCODE -ne 0) {
    Write-Host "错误: 前端构建失败" -ForegroundColor Red
    exit 1
}
Pop-Location

Write-Host "[3/3] 构建应用..." -ForegroundColor Green
wails build -nsis

if ($LASTEXITCODE -ne 0) {
    Write-Host "错误: 应用构建失败" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Green
Write-Host "构建完成！" -ForegroundColor Green
Write-Host "输出文件位置: build\bin\" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host ""
