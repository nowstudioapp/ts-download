@echo off
REM 开发模式启动脚本

echo.
echo ========================================
echo TS-Merge 开发模式
echo ========================================
echo.

REM 检查 Wails 是否安装
wails --version >nul 2>&1
if errorlevel 1 (
    echo 错误: 未找到 Wails，请先安装
    echo 安装命令: go install github.com/wailsapp/wails/v2/cmd/wails@latest
    exit /b 1
)

echo [1/2] 检查前端依赖...
cd frontend
if not exist node_modules (
    echo 安装前端依赖...
    call npm install
    if errorlevel 1 (
        echo 错误: 前端依赖安装失败
        exit /b 1
    )
)
cd ..

echo [2/2] 启动开发服务器...
echo.
echo 应用将在 http://localhost:3000 打开
echo 按 Ctrl+C 停止开发服务器
echo.

call wails dev

echo.
echo 开发服务器已停止
echo.
