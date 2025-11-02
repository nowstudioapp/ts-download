# PowerShell 修复脚本 - 解决常见问题

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "TS-Merge 问题修复脚本" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# 选择修复选项
Write-Host "请选择要执行的操作:" -ForegroundColor Yellow
Write-Host "1. 清除 npm 缓存并重新安装依赖 (推荐)" -ForegroundColor Green
Write-Host "2. 删除 node_modules 和 package-lock.json" -ForegroundColor Green
Write-Host "3. 清除所有构建缓存" -ForegroundColor Green
Write-Host "4. 检查环境要求" -ForegroundColor Green
Write-Host "5. 允许 PowerShell 脚本执行" -ForegroundColor Green
Write-Host "6. 执行所有修复" -ForegroundColor Green
Write-Host ""

$choice = Read-Host "请输入选项 (1-6)"

function CleanNpmCache {
    Write-Host ""
    Write-Host "[1/3] 清除 npm 缓存..." -ForegroundColor Green
    npm cache clean --force
    
    Write-Host "[2/3] 删除 node_modules..." -ForegroundColor Green
    Push-Location frontend
    if (Test-Path "node_modules") {
        Remove-Item -Recurse -Force node_modules
        Write-Host "✓ node_modules 已删除" -ForegroundColor Green
    }
    
    if (Test-Path "package-lock.json") {
        Remove-Item package-lock.json
        Write-Host "✓ package-lock.json 已删除" -ForegroundColor Green
    }
    Pop-Location
    
    Write-Host "[3/3] 重新安装依赖..." -ForegroundColor Green
    Push-Location frontend
    npm install
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✓ 依赖安装成功" -ForegroundColor Green
    } else {
        Write-Host "✗ 依赖安装失败" -ForegroundColor Red
    }
    Pop-Location
}

function DeleteNodeModules {
    Write-Host ""
    Write-Host "删除 node_modules 和 package-lock.json..." -ForegroundColor Green
    Push-Location frontend
    
    if (Test-Path "node_modules") {
        Remove-Item -Recurse -Force node_modules
        Write-Host "✓ node_modules 已删除" -ForegroundColor Green
    }
    
    if (Test-Path "package-lock.json") {
        Remove-Item package-lock.json
        Write-Host "✓ package-lock.json 已删除" -ForegroundColor Green
    }
    
    Pop-Location
    Write-Host ""
    Write-Host "现在运行: npm install" -ForegroundColor Yellow
}

function ClearBuildCache {
    Write-Host ""
    Write-Host "[1/3] 删除 dist 目录..." -ForegroundColor Green
    if (Test-Path "frontend/dist") {
        Remove-Item -Recurse -Force frontend/dist
        Write-Host "✓ dist 已删除" -ForegroundColor Green
    }
    
    Write-Host "[2/3] 删除 build 目录..." -ForegroundColor Green
    if (Test-Path "build") {
        Remove-Item -Recurse -Force build
        Write-Host "✓ build 已删除" -ForegroundColor Green
    }
    
    Write-Host "[3/3] 删除 node_modules..." -ForegroundColor Green
    Push-Location frontend
    if (Test-Path "node_modules") {
        Remove-Item -Recurse -Force node_modules
        Write-Host "✓ node_modules 已删除" -ForegroundColor Green
    }
    Pop-Location
    
    Write-Host ""
    Write-Host "现在运行: .\dev.ps1" -ForegroundColor Yellow
}

function CheckEnvironment {
    Write-Host ""
    Write-Host "检查环境要求..." -ForegroundColor Green
    Write-Host ""
    
    # 检查 Go
    Write-Host "1. 检查 Go..." -ForegroundColor Cyan
    $goVersion = go version 2>$null
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✓ $goVersion" -ForegroundColor Green
    } else {
        Write-Host "✗ Go 未安装或未在 PATH 中" -ForegroundColor Red
        Write-Host "  请访问: https://golang.org/dl/" -ForegroundColor Yellow
    }
    
    # 检查 Node.js
    Write-Host ""
    Write-Host "2. 检查 Node.js..." -ForegroundColor Cyan
    $nodeVersion = node --version 2>$null
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✓ Node.js $nodeVersion" -ForegroundColor Green
    } else {
        Write-Host "✗ Node.js 未安装或未在 PATH 中" -ForegroundColor Red
        Write-Host "  请访问: https://nodejs.org/" -ForegroundColor Yellow
    }
    
    # 检查 npm
    Write-Host ""
    Write-Host "3. 检查 npm..." -ForegroundColor Cyan
    $npmVersion = npm --version 2>$null
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✓ npm $npmVersion" -ForegroundColor Green
    } else {
        Write-Host "✗ npm 未安装" -ForegroundColor Red
    }
    
    # 检查 Wails
    Write-Host ""
    Write-Host "4. 检查 Wails..." -ForegroundColor Cyan
    $wailsVersion = wails --version 2>$null
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✓ $wailsVersion" -ForegroundColor Green
    } else {
        Write-Host "✗ Wails 未安装" -ForegroundColor Red
        Write-Host "  安装命令: go install github.com/wailsapp/wails/v2/cmd/wails@latest" -ForegroundColor Yellow
    }
    
    Write-Host ""
}

function AllowScriptExecution {
    Write-Host ""
    Write-Host "允许 PowerShell 脚本执行..." -ForegroundColor Green
    
    try {
        Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser -Force
        Write-Host "✓ 执行策略已更新" -ForegroundColor Green
        Write-Host "  现在可以运行: .\dev.ps1" -ForegroundColor Yellow
    } catch {
        Write-Host "✗ 更新执行策略失败" -ForegroundColor Red
        Write-Host "  请以管理员身份运行 PowerShell 后重试" -ForegroundColor Yellow
    }
    
    Write-Host ""
}

function ExecuteAllFixes {
    Write-Host ""
    Write-Host "执行所有修复..." -ForegroundColor Green
    Write-Host ""
    
    CheckEnvironment
    AllowScriptExecution
    ClearBuildCache
    CleanNpmCache
    
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Green
    Write-Host "所有修复已完成！" -ForegroundColor Green
    Write-Host "========================================" -ForegroundColor Green
    Write-Host ""
    Write-Host "现在可以运行: .\dev.ps1" -ForegroundColor Yellow
    Write-Host ""
}

# 执行选择的操作
switch ($choice) {
    "1" { CleanNpmCache }
    "2" { DeleteNodeModules }
    "3" { ClearBuildCache }
    "4" { CheckEnvironment }
    "5" { AllowScriptExecution }
    "6" { ExecuteAllFixes }
    default {
        Write-Host "无效的选项" -ForegroundColor Red
        exit 1
    }
}

Write-Host ""
Write-Host "修复脚本执行完成" -ForegroundColor Green
Write-Host ""
