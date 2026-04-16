#!/usr/bin/env python3
"""
有效用户迁移脚本（单线程版）
从旧 MySQL 的 tg_valid_users_xx / ws_valid_users_xx（225 个国家分表）
迁移到新 ClickHouse 的 tg_valid_users / ws_valid_users 两张聚合表。

用法:
    python3 migrate_valid_users.py                          # 全量迁移
    python3 migrate_valid_users.py --type tg                # 只迁移 tg
    python3 migrate_valid_users.py --type ws                # 只迁移 ws
    python3 migrate_valid_users.py --country US,CN          # 只迁移指定国家
    python3 migrate_valid_users.py --reset                  # 忽略进度，全部重跑
"""
import sys
import time
import json
import argparse
import logging
from pathlib import Path

import pymysql
from clickhouse_driver import Client

from config import OLD_MYSQL, NEW_CLICKHOUSE

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s",
    handlers=[
        logging.StreamHandler(sys.stdout),
        logging.FileHandler("migrate_valid_users.log", encoding="utf-8"),
    ],
)
log = logging.getLogger(__name__)

BATCH_SIZE = 100000
PROGRESS_FILE = "valid_users_progress.json"


def get_mysql_conn(streaming=False):
    cfg = dict(OLD_MYSQL)
    cfg["connect_timeout"] = 30
    cfg["read_timeout"] = 3600
    cfg["write_timeout"] = 60
    if streaming:
        cfg["cursorclass"] = pymysql.cursors.SSCursor
    else:
        cfg["cursorclass"] = pymysql.cursors.DictCursor
    return pymysql.connect(**cfg)


def get_new_ck_client():
    return Client(
        host=NEW_CLICKHOUSE["host"],
        port=NEW_CLICKHOUSE["port"],
        user=NEW_CLICKHOUSE["user"],
        password=NEW_CLICKHOUSE["password"],
        database=NEW_CLICKHOUSE["database"],
        connect_timeout=30,
        send_receive_timeout=600,
    )


def load_progress():
    p = Path(PROGRESS_FILE)
    if p.exists():
        with open(p, "r", encoding="utf-8") as f:
            return json.load(f)
    return {"tg": [], "ws": []}


def save_progress(progress):
    with open(PROGRESS_FILE, "w", encoding="utf-8") as f:
        json.dump(progress, f, ensure_ascii=False, indent=2)


def fetch_country_codes(mysql_conn):
    with mysql_conn.cursor() as cursor:
        cursor.execute("SELECT code FROM ts_country ORDER BY code")
        return [row["code"] for row in cursor.fetchall()]


def table_exists(mysql_conn, table_name):
    with mysql_conn.cursor() as cursor:
        cursor.execute(
            "SELECT COUNT(*) AS cnt FROM information_schema.tables "
            "WHERE table_schema = %s AND table_name = %s",
            (OLD_MYSQL["database"], table_name),
        )
        return cursor.fetchone()["cnt"] > 0


def get_table_count(mysql_conn, table_name):
    with mysql_conn.cursor() as cursor:
        cursor.execute(f"SELECT COUNT(*) AS cnt FROM `{table_name}`")
        return cursor.fetchone()["cnt"]


def migrate_one_table(country_code, prefix, ck_table, meta_conn, ck):
    """迁移单个国家的一张表，复用连接"""
    mysql_table = f"{prefix}_{country_code}"

    if not table_exists(meta_conn, mysql_table):
        return 0, "skip"

    total = get_table_count(meta_conn, mysql_table)
    if total == 0:
        return 0, "empty"

    log.info("  [%s] %s -> %s, 共 %d 行", country_code, mysql_table, ck_table, total)

    stream_conn = get_mysql_conn(streaming=True)
    try:
        migrated = 0
        batch = []

        cursor = stream_conn.cursor()
        cursor.execute(f"SELECT phone FROM `{mysql_table}`")

        for row in cursor:
            phone = str(row[0]).strip() if row[0] else ""
            if not phone or not phone.isdigit():
                continue

            batch.append({"phone": phone, "country_code": country_code})

            if len(batch) >= BATCH_SIZE:
                ck.execute(
                    f"INSERT INTO {ck_table} (phone, country_code) VALUES",
                    batch,
                )
                migrated += len(batch)
                batch = []
                log.info("    [%s] 进度: %d / %d", country_code, migrated, total)

        if batch:
            ck.execute(
                f"INSERT INTO {ck_table} (phone, country_code) VALUES",
                batch,
            )
            migrated += len(batch)

        cursor.close()
        return migrated, "ok"

    except Exception as e:
        log.error("  [%s] %s 迁移失败: %s", country_code, mysql_table, str(e)[:300])
        return 0, f"error: {str(e)[:200]}"
    finally:
        stream_conn.close()


def format_duration(seconds):
    if seconds < 60:
        return f"{seconds:.1f}s"
    elif seconds < 3600:
        return f"{int(seconds // 60)}m {int(seconds % 60)}s"
    else:
        return f"{int(seconds // 3600)}h {int((seconds % 3600) // 60)}m"


def main():
    parser = argparse.ArgumentParser(description="有效用户迁移脚本")
    parser.add_argument("--type", choices=["tg", "ws"], default=None, help="只迁移 tg 或 ws（默认都迁）")
    parser.add_argument("--country", type=str, default=None, help="只迁移指定国家，逗号分隔，如 US,CN")
    parser.add_argument("--reset", action="store_true", help="忽略进度文件，全部重新迁移")
    args = parser.parse_args()

    log.info("=" * 60)
    log.info("有效用户迁移开始（单线程）")
    log.info("  批次大小: %d", BATCH_SIZE)
    log.info("  类型: %s", args.type or "全部（tg + ws）")
    log.info("  国家: %s", args.country or "全部")
    log.info("=" * 60)

    if args.reset:
        progress_data = {"tg": [], "ws": []}
        save_progress(progress_data)
        log.info("已重置进度文件")
    else:
        progress_data = load_progress()

    meta_conn = get_mysql_conn(streaming=False)
    ck = get_new_ck_client()

    if args.country:
        country_codes = [c.strip() for c in args.country.split(",")]
        log.info("指定国家: %s", country_codes)
    else:
        country_codes = fetch_country_codes(meta_conn)
        log.info("从 ts_country 获取到 %d 个国家", len(country_codes))

    type_map = {
        "tg": ("tg_valid_users", "tg_valid_users"),
        "ws": ("ws_valid_users", "ws_valid_users"),
    }
    types_to_run = [args.type] if args.type else ["tg", "ws"]

    tasks = []
    for t in types_to_run:
        prefix, ck_table = type_map[t]
        done_set = set(progress_data.get(t, []))
        for code in country_codes:
            if code in done_set:
                continue
            tasks.append((code, prefix, ck_table, t))

    skipped_count = len(country_codes) * len(types_to_run) - len(tasks)
    log.info("待迁移: %d 个表, 已跳过(之前完成): %d", len(tasks), skipped_count)

    if not tasks:
        log.info("没有需要迁移的表，退出")
        meta_conn.close()
        return

    start_time = time.time()
    success = 0
    skip = 0
    fail = 0
    total_rows = 0

    for i, (code, prefix, ck_table, ptype) in enumerate(tasks, 1):
        rows, status = migrate_one_table(code, prefix, ck_table, meta_conn, ck)
        total_rows += rows

        if status == "ok" or status == "empty":
            success += 1
            progress_data[ptype].append(code)
            save_progress(progress_data)
        elif status == "skip":
            skip += 1
        else:
            fail += 1
            progress_data[ptype].append(code)
            save_progress(progress_data)

        elapsed = time.time() - start_time
        if status == "ok":
            log.info(
                "[%d/%d] %s_%s 完成, %d 行 | 累计: %d 行 | 耗时: %s",
                i, len(tasks), prefix, code, rows, total_rows, format_duration(elapsed),
            )
        elif status != "skip":
            log.info("[%d/%d] %s_%s: %s", i, len(tasks), prefix, code, status)

    meta_conn.close()
    elapsed = time.time() - start_time

    log.info("")
    log.info("=" * 60)
    log.info("迁移结果汇总")
    log.info("=" * 60)
    log.info("  成功:   %d 个表", success)
    log.info("  跳过:   %d 个表（表不存在）", skip)
    log.info("  失败:   %d 个表", fail)
    log.info("  总行数: %d 行", total_rows)
    log.info("  总耗时: %s", format_duration(elapsed))
    if total_rows > 0 and elapsed > 0:
        log.info("  吞吐:   %.0f 行/秒", total_rows / elapsed)
    log.info("=" * 60)


if __name__ == "__main__":
    main()
