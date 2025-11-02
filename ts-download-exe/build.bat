@echo off
REM 构建 TS-Merge 应用

echo.
echo ========================================
echo TS-Merge 构建脚本
echo ========================================
echo.

REM 检查 Wails 是否安装
wails --version >nul 2>&1
if errorlevel 1 (
    echo 错误: 未找到 Wails，请先安装
    echo 安装命令: go install github.com/wailsapp/wails/v2/cmd/wails@latest
    exit /b 1
)

echo [1/3] 检查前端依赖...
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

echo [2/3] 构建前端...
cd frontend
call npm run build
if errorlevel 1 (
    echo 错误: 前端构建失败
    exit /b 1
)
cd ..

echo [3/3] 构建应用...
call wails build -nsis

if errorlevel 1 (
    echo 错误: 应用构建失败
    exit /b 1
)

echo.
echo ========================================
echo 构建完成！
echo 输出文件位置: build\bin\
echo ========================================
echo.
