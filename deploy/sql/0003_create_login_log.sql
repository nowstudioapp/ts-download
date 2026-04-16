-- 登录日志表
CREATE TABLE IF NOT EXISTS ts_backup.login_log
(
    id          UInt64,
    user_id     UInt64,
    username    String              DEFAULT '',
    ip          String              DEFAULT '',
    status      UInt8               DEFAULT 1,
    message     String              DEFAULT '',
    create_time DateTime            DEFAULT now()
)
ENGINE = MergeTree()
ORDER BY (create_time, user_id)
PARTITION BY toYYYYMM(create_time)
SETTINGS index_granularity = 8192;
