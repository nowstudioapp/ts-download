-- 下载日志表
CREATE TABLE IF NOT EXISTS ts_backup.download_log
(
    id               UInt64,
    user_id          UInt64,
    username         String              DEFAULT '',
    ip               String              DEFAULT '',
    country_code     String              DEFAULT '',
    first_task_type  String              DEFAULT '',
    second_task_type String              DEFAULT '',
    download_params  String              DEFAULT '',
    file_url         String              DEFAULT '',
    create_time      DateTime            DEFAULT now()
)
ENGINE = MergeTree()
ORDER BY (create_time, user_id)
PARTITION BY toYYYYMM(create_time)
SETTINGS index_granularity = 8192;
