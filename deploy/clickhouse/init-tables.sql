CREATE DATABASE IF NOT EXISTS ts_backup;

CREATE TABLE IF NOT EXISTS ts_backup.ts_ws_task_record
(
    id               UInt32,
    phone            String              DEFAULT '',
    sex              String              DEFAULT '',
    age              UInt32              DEFAULT 0,
    uid              String              DEFAULT '',
    user_name        String              DEFAULT '',
    last_online_time DateTime,
    active_day       UInt32              DEFAULT 0,
    business_number  UInt32              DEFAULT 0,
    member           String              DEFAULT '',
    multiple_avatars String              DEFAULT '',
    pic              String              DEFAULT '',
    status           String              DEFAULT '',
    skin             String              DEFAULT '',
    hair_color       String              DEFAULT '',
    ethnicity        String              DEFAULT '',
    first_name       String              DEFAULT '',
    last_name        String              DEFAULT '',
    task_type        LowCardinality(String),
    country_code     LowCardinality(String),
    task_id          String              DEFAULT '',
    create_time      DateTime            DEFAULT now()
)
ENGINE = ReplacingMergeTree(create_time)
PARTITION BY country_code
ORDER BY (country_code, task_type, phone)
SETTINGS index_granularity = 8192;

CREATE TABLE IF NOT EXISTS ts_backup.ts_tg_task_record
(
    id               UInt32,
    phone            String              DEFAULT '',
    sex              String              DEFAULT '',
    age              UInt32              DEFAULT 0,
    uid              String              DEFAULT '',
    user_name        String              DEFAULT '',
    last_online_time DateTime,
    active_day       UInt32              DEFAULT 0,
    business_number  UInt32              DEFAULT 0,
    member           String              DEFAULT '',
    multiple_avatars String              DEFAULT '',
    pic              String              DEFAULT '',
    status           String              DEFAULT '',
    skin             String              DEFAULT '',
    hair_color       String              DEFAULT '',
    ethnicity        String              DEFAULT '',
    first_name       String              DEFAULT '',
    last_name        String              DEFAULT '',
    task_type        LowCardinality(String),
    country_code     LowCardinality(String),
    task_id          String              DEFAULT '',
    create_time      DateTime            DEFAULT now()
)
ENGINE = ReplacingMergeTree(create_time)
PARTITION BY country_code
ORDER BY (country_code, task_type, phone)
SETTINGS index_granularity = 8192;

CREATE TABLE IF NOT EXISTS ts_backup.ts_other_task_record
(
    id               UInt32,
    phone            String              DEFAULT '',
    sex              String              DEFAULT '',
    age              UInt32              DEFAULT 0,
    uid              String              DEFAULT '',
    user_name        String              DEFAULT '',
    last_online_time DateTime,
    active_day       UInt32              DEFAULT 0,
    business_number  UInt32              DEFAULT 0,
    member           String              DEFAULT '',
    multiple_avatars String              DEFAULT '',
    pic              String              DEFAULT '',
    status           String              DEFAULT '',
    skin             String              DEFAULT '',
    hair_color       String              DEFAULT '',
    ethnicity        String              DEFAULT '',
    first_name       String              DEFAULT '',
    last_name        String              DEFAULT '',
    task_type        LowCardinality(String),
    country_code     LowCardinality(String),
    task_id          String              DEFAULT '',
    create_time      DateTime            DEFAULT now()
)
ENGINE = ReplacingMergeTree(create_time)
PARTITION BY country_code
ORDER BY (country_code, task_type, phone)
SETTINGS index_granularity = 8192;

CREATE TABLE IF NOT EXISTS ts_backup.migrate_log
(
    task_id      String,
    task_type    String,
    country_code String,
    target_table String,
    row_count    UInt32,
    status       String,
    error_msg    String              DEFAULT '',
    migrate_time DateTime            DEFAULT now()
)
ENGINE = ReplacingMergeTree(migrate_time)
ORDER BY (task_id)
SETTINGS index_granularity = 8192;
