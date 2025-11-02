#!/bin/bash

# 开发模式启动脚本

echo ""
echo "========================================"
echo "TS-Merge 开发模式"
echo "========================================"
echo ""

# 检查 Wails 是否安装
if ! command -v wails &> /dev/null; then
    echo "错误: 未找到 Wails，请先安装"
    echo "安装命令: go install github.com/wailsapp/wails/v2/cmd/wails@latest"
    exit 1
fi

echo "[1/2] 检查前端依赖..."
cd frontend
if [ ! -d "node_modules" ]; then
    echo "安装前端依赖..."
    npm install
    if [ $? -ne 0 ]; then
        echo "错误: 前端依赖安装失败"
        exit 1
    fi
fi
cd ..

echo "[2/2] 启动开发服务器..."
echo ""
echo "应用将在 http://localhost:3000 打开"
echo "按 Ctrl+C 停止开发服务器"
echo ""

wails dev

echo ""
echo "开发服务器已停止"
echo ""
