#!/bin/bash

# 构建脚本

echo ""
echo "========================================"
echo "TS-Merge 构建脚本"
echo "========================================"
echo ""

# 检查 Wails 是否安装
if ! command -v wails &> /dev/null; then
    echo "错误: 未找到 Wails，请先安装"
    echo "安装命令: go install github.com/wailsapp/wails/v2/cmd/wails@latest"
    exit 1
fi

echo "[1/3] 检查前端依赖..."
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

echo "[2/3] 构建前端..."
cd frontend
npm run build
if [ $? -ne 0 ]; then
    echo "错误: 前端构建失败"
    exit 1
fi
cd ..

echo "[3/3] 构建应用..."
wails build

if [ $? -ne 0 ]; then
    echo "错误: 应用构建失败"
    exit 1
fi

echo ""
echo "========================================"
echo "构建完成！"
echo "输出文件位置: build/bin/"
echo "========================================"
echo ""
