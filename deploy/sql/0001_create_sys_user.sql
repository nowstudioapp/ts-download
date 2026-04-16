-- 用户表
CREATE TABLE IF NOT EXISTS ts_backup.sys_user
(
    id          UInt64,
    username    String,
    password    String,
    nickname    String              DEFAULT '',
    role        String              DEFAULT 'user',
    status      UInt8               DEFAULT 1,
    deleted     UInt8               DEFAULT 0,
    create_time DateTime            DEFAULT now(),
    update_time DateTime            DEFAULT now(),
    version     UInt64              DEFAULT 1
)
ENGINE = ReplacingMergeTree(version)
ORDER BY id
SETTINGS index_granularity = 8192;

-- 默认管理员（密码 admin123 的 BCrypt 值）
INSERT INTO ts_backup.sys_user (id, username, password, nickname, role, status, deleted, create_time, update_time, version)
VALUES (1, 'admin', '$2a$12$u3OjzOgfV0jNfxNxmyqA8.WbPwXojPB/vm8//0Nem7JUBXOxWKZCS', '管理员', 'admin', 1, 0, now(), now(), 1);
