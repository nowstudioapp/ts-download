-- TG 有效用户表（从旧 MySQL tg_valid_users_xx 合并而来）
CREATE TABLE IF NOT EXISTS ts_backup.tg_valid_users
(
    phone        String,
    country_code String,
    create_time  DateTime DEFAULT now()
)
ENGINE = ReplacingMergeTree(create_time)
PARTITION BY country_code
ORDER BY (country_code, phone)
SETTINGS index_granularity = 8192;

-- WS 有效用户表（从旧 MySQL ws_valid_users_xx 合并而来）
CREATE TABLE IF NOT EXISTS ts_backup.ws_valid_users
(
    phone        String,
    country_code String,
    create_time  DateTime DEFAULT now()
)
ENGINE = ReplacingMergeTree(create_time)
PARTITION BY country_code
ORDER BY (country_code, phone)
SETTINGS index_granularity = 8192;
