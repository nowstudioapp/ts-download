# ============================================================
# 连接配置 - 请根据实际环境修改 IP 和密码
# ============================================================

# 旧 MySQL（生产服务器）
OLD_MYSQL = {
    "host": "YOUR_OLD_MYSQL_HOST",  # 改成旧服务器的外网 IP
    "port": 3306,
    "user": "ry",
    "password": "rCX74MxW6Ks457zF",
    "database": "ry",
    "charset": "utf8mb4",
}

# 旧 ClickHouse（生产服务器）
OLD_CLICKHOUSE = {
    "host": "YOUR_OLD_CK_HOST",  # 改成旧服务器的外网 IP
    "port": 9000,
    "user": "ts",
    "password": "6b52a53a13ab190c",
    "database": "ts",
}

# 新 ClickHouse（底裤服务器，脚本本地运行）
NEW_CLICKHOUSE = {
    "host": "127.0.0.1",
    "port": 9000,
    "user": "ts_backup",
    "password": "ts_backup_2026",
    "database": "ts_backup",
}

# 批量处理配置
BATCH_SIZE = 50000
