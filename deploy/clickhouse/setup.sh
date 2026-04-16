#!/bin/bash
set -e

# ============================================================
# ClickHouse Docker 部署 + 建表初始化脚本
# 使用方式: chmod +x setup.sh && ./setup.sh
# ============================================================

CK_CONTAINER_NAME="clickhouse-backup"
CK_IMAGE="clickhouse/clickhouse-server:23.8-alpine"
CK_HTTP_PORT=8123
CK_TCP_PORT=9000
CK_USER="ts_backup"
CK_PASSWORD="ts_backup_2026"
CK_DATABASE="ts_backup"
CK_DATA_DIR="/data/clickhouse"

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
INIT_SQL="${SCRIPT_DIR}/init-tables.sql"

echo "=========================================="
echo " ClickHouse 部署脚本"
echo "=========================================="

# 1. 检查 Docker
if ! command -v docker &> /dev/null; then
    echo "[1/5] Docker 未安装，开始安装..."
    curl -fsSL https://get.docker.com | sh
    systemctl enable docker
    systemctl start docker
    echo "Docker 安装完成"
else
    echo "[1/5] Docker 已安装: $(docker --version)"
fi

# 2. 检查并停止旧容器
if docker ps -a --format '{{.Names}}' | grep -q "^${CK_CONTAINER_NAME}$"; then
    echo "[2/5] 发现旧容器 ${CK_CONTAINER_NAME}，停止并删除..."
    docker stop ${CK_CONTAINER_NAME} 2>/dev/null || true
    docker rm ${CK_CONTAINER_NAME} 2>/dev/null || true
else
    echo "[2/5] 无旧容器"
fi

# 3. 创建数据目录
echo "[3/5] 创建数据目录: ${CK_DATA_DIR}"
mkdir -p ${CK_DATA_DIR}/data
mkdir -p ${CK_DATA_DIR}/logs

# 4. 启动 ClickHouse 容器
echo "[4/5] 启动 ClickHouse 容器..."
docker run -d \
    --name ${CK_CONTAINER_NAME} \
    --restart always \
    --ulimit nofile=262144:262144 \
    -p ${CK_HTTP_PORT}:8123 \
    -p ${CK_TCP_PORT}:9000 \
    -v ${CK_DATA_DIR}/data:/var/lib/clickhouse \
    -v ${CK_DATA_DIR}/logs:/var/log/clickhouse-server \
    -e CLICKHOUSE_USER=${CK_USER} \
    -e CLICKHOUSE_PASSWORD=${CK_PASSWORD} \
    -e CLICKHOUSE_DEFAULT_ACCESS_MANAGEMENT=1 \
    ${CK_IMAGE}

echo "等待 ClickHouse 启动..."
for i in $(seq 1 30); do
    if docker exec ${CK_CONTAINER_NAME} clickhouse-client \
        --user ${CK_USER} --password ${CK_PASSWORD} \
        -q "SELECT 1" &>/dev/null; then
        echo "ClickHouse 已就绪"
        break
    fi
    if [ $i -eq 30 ]; then
        echo "ERROR: ClickHouse 启动超时，请检查日志: docker logs ${CK_CONTAINER_NAME}"
        exit 1
    fi
    sleep 1
done

# 5. 初始化表结构
echo "[5/5] 初始化数据库和表结构..."
docker exec -i ${CK_CONTAINER_NAME} clickhouse-client \
    --user ${CK_USER} \
    --password ${CK_PASSWORD} \
    --multiquery < "${INIT_SQL}"

echo ""
echo "=========================================="
echo " 部署完成"
echo "=========================================="
echo ""
echo " 容器名称:  ${CK_CONTAINER_NAME}"
echo " HTTP 端口: ${CK_HTTP_PORT}"
echo " TCP  端口: ${CK_TCP_PORT}"
echo " 用户名:    ${CK_USER}"
echo " 密码:      ${CK_PASSWORD}"
echo " 数据库:    ${CK_DATABASE}"
echo " 数据目录:  ${CK_DATA_DIR}"
echo ""
echo " 验证命令:"
echo "   docker exec -it ${CK_CONTAINER_NAME} clickhouse-client --user ${CK_USER} --password ${CK_PASSWORD} -d ${CK_DATABASE} -q 'SHOW TABLES'"
echo ""
