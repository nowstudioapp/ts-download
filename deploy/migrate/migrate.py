#!/usr/bin/env python3
"""
ClickHouse 数据迁移脚本（多线程版）
从旧 MySQL 读取任务列表，从旧 ClickHouse 读取明细，写入新 ClickHouse（底裤）。
wsExist 任务类型纠正归属到 WS 表。

用法:
    python3 migrate.py                        # 全量迁移，默认 8 线程
    python3 migrate.py --limit 2              # 只跑 2 个任务（测试用）
    python3 migrate.py --workers 16           # 16 线程全量跑
    python3 migrate.py --limit 10 --workers 4 # 4 线程跑 10 个任务
"""
import sys
import time
import argparse
import logging
import threading
from datetime import datetime
from concurrent.futures import ThreadPoolExecutor, as_completed

import pymysql
from clickhouse_driver import Client

from config import OLD_MYSQL, OLD_CLICKHOUSE, NEW_CLICKHOUSE, BATCH_SIZE

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] [%(threadName)s] %(message)s",
    handlers=[
        logging.StreamHandler(sys.stdout),
        logging.FileHandler("migrate.log", encoding="utf-8"),
    ],
)
log = logging.getLogger(__name__)

# ============================================================
# 任务类型 -> 表名映射
# ============================================================

WS_TASK_TYPES = {"gender", "whatsappExist", "wsValid", "wsExist"}
TG_TASK_TYPES = {"sieveLive", "sieveAvatar", "tgEffective"}

OLD_WS_TASK_TYPES = {"gender", "whatsappExist", "wsValid"}
OLD_TG_TASK_TYPES = {"sieveLive", "sieveAvatar", "tgEffective"}


def get_old_table_name(task_type, country_code):
    if task_type in OLD_WS_TASK_TYPES:
        return f"ts_ws_task_record_{country_code}"
    elif task_type in OLD_TG_TASK_TYPES:
        return f"ts_tg_task_record_{country_code}"
    else:
        return f"ts_other_task_record_{country_code}"


def get_new_table_name(task_type):
    if task_type in WS_TASK_TYPES:
        return "ts_ws_task_record"
    elif task_type in TG_TASK_TYPES:
        return "ts_tg_task_record"
    else:
        return "ts_other_task_record"


# ============================================================
# 数据库连接（每个线程独立连接）
# ============================================================

def get_mysql_conn():
    return pymysql.connect(**OLD_MYSQL, cursorclass=pymysql.cursors.DictCursor)


def get_old_ck_client():
    return Client(
        host=OLD_CLICKHOUSE["host"],
        port=OLD_CLICKHOUSE["port"],
        user=OLD_CLICKHOUSE["user"],
        password=OLD_CLICKHOUSE["password"],
        database=OLD_CLICKHOUSE["database"],
        connect_timeout=30,
        send_receive_timeout=600,
    )


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


# ============================================================
# 迁移日志（断点续传）- 线程安全写入
# ============================================================

log_lock = threading.Lock()


def load_migrated_tasks(new_ck):
    rows = new_ck.execute(
        "SELECT task_id FROM migrate_log FINAL WHERE status = 'success'"
    )
    return {row[0] for row in rows}


def write_migrate_log(new_ck, task_id, task_type, country_code, target_table, row_count, status, error_msg=""):
    with log_lock:
        new_ck.execute(
            "INSERT INTO migrate_log (task_id, task_type, country_code, target_table, row_count, status, error_msg, migrate_time) VALUES",
            [{
                "task_id": task_id,
                "task_type": task_type,
                "country_code": country_code,
                "target_table": target_table,
                "row_count": row_count,
                "status": status,
                "error_msg": error_msg,
                "migrate_time": datetime.now(),
            }],
        )


# ============================================================
# 核心迁移逻辑
# ============================================================

COLUMNS = [
    "id", "phone", "sex", "age", "uid", "user_name",
    "last_online_time", "active_day", "business_number", "member",
    "multiple_avatars", "pic", "status", "skin", "hair_color",
    "ethnicity", "first_name", "last_name", "task_type", "country_code",
    "task_id", "create_time",
]


def migrate_task(old_ck, new_ck, task_id, task_type, country_code):
    old_table = get_old_table_name(task_type, country_code)
    new_table = get_new_table_name(task_type)

    count_result = old_ck.execute(
        f"SELECT count() FROM {old_table} WHERE task_id = %(task_id)s",
        {"task_id": task_id},
    )
    total_rows = count_result[0][0]

    if total_rows == 0:
        log.info("  任务 %s 在旧表 %s 中无数据，跳过", task_id, old_table)
        write_migrate_log(new_ck, task_id, task_type, country_code, new_table, 0, "success", "no data in old table")
        return 0

    log.info("  旧表: %s -> 新表: %s, 共 %d 行", old_table, new_table, total_rows)

    migrated = 0
    offset = 0

    while offset < total_rows:
        rows = old_ck.execute(
            f"SELECT {', '.join(COLUMNS)} FROM {old_table} "
            f"WHERE task_id = %(task_id)s "
            f"LIMIT {BATCH_SIZE} OFFSET {offset}",
            {"task_id": task_id},
        )

        if not rows:
            break

        batch = []
        skipped = 0
        for row in rows:
            record = dict(zip(COLUMNS, row))
            phone = str(record.get("phone", ""))
            if not phone.isdigit():
                skipped += 1
                continue
            for uint_col in ("age", "active_day", "business_number"):
                try:
                    val = int(record.get(uint_col, 0))
                except (ValueError, TypeError):
                    val = 0
                if val < 0 or val > 2147483647:
                    val = 0
                record[uint_col] = val
            batch.append(record)

        if skipped > 0:
            log.info("    跳过 %d 条无效 phone 记录", skipped)

        if batch:
            new_ck.execute(
                f"INSERT INTO {new_table} ({', '.join(COLUMNS)}) VALUES",
                batch,
            )

        migrated += len(batch)
        offset += BATCH_SIZE
        log.info("    已写入 %d / %d (跳过无效: %d)", migrated, total_rows, skipped)

    return migrated


def fetch_tasks(mysql_conn):
    with mysql_conn.cursor() as cursor:
        cursor.execute(
            "SELECT task_id, task_type, country_code "
            "FROM ts_task "
            "WHERE task_status = 1 "
            "ORDER BY create_time ASC"
        )
        return cursor.fetchall()


# ============================================================
# 线程安全的进度计数器
# ============================================================

class ProgressCounter:
    def __init__(self, total):
        self.total = total
        self.success = 0
        self.fail = 0
        self.skip = 0
        self.total_rows = 0
        self.processed = 0
        self.lock = threading.Lock()
        self.start_time = time.time()

    def add_success(self, row_count):
        with self.lock:
            self.success += 1
            self.total_rows += row_count
            self.processed += 1

    def add_fail(self):
        with self.lock:
            self.fail += 1
            self.processed += 1

    def fix_retry(self, row_count):
        with self.lock:
            self.success += 1
            self.fail -= 1
            self.total_rows += row_count

    def get_progress(self):
        with self.lock:
            elapsed = time.time() - self.start_time
            done = self.processed
            if done > 0:
                avg = elapsed / done
                eta = avg * (self.total - done)
            else:
                eta = 0
            return self.processed, self.total, self.success, self.fail, self.total_rows, eta


# ============================================================
# 单个任务的工作函数（每个线程调用）
# ============================================================

def worker(task, progress):
    task_id = task["task_id"]
    task_type = task["task_type"]
    country_code = task["country_code"]
    new_table = get_new_table_name(task_type)

    old_ck = get_old_ck_client()
    new_ck = get_new_ck_client()

    processed, total, succ, fail, rows, eta = progress.get_progress()
    eta_str = format_duration(eta) if processed > 0 else "计算中..."

    log.info(
        "[%d/%d] 任务: %s | 类型: %-20s | 国家: %s | 目标: %s | 预计剩余: %s",
        processed + 1, total, task_id, task_type, country_code, new_table, eta_str,
    )

    try:
        task_start = time.time()
        row_count = migrate_task(old_ck, new_ck, task_id, task_type, country_code)
        task_elapsed = time.time() - task_start
        write_migrate_log(new_ck, task_id, task_type, country_code, new_table, row_count, "success")
        progress.add_success(row_count)

        _, _, succ, fail, total_rows, _ = progress.get_progress()
        log.info(
            "  -> 成功 | %d 行 | 耗时 %s | 累计: 成功 %d / 失败 %d / 总行数 %d",
            row_count, format_duration(task_elapsed), succ, fail, total_rows,
        )
        return True
    except Exception as e:
        error_msg = str(e)[:500]
        log.error("  -> 失败: %s", error_msg)
        write_migrate_log(new_ck, task_id, task_type, country_code, new_table, 0, "failed", error_msg)
        progress.add_fail()

        try:
            log.info("  -> 等待 2s 后重试...")
            time.sleep(2)
            old_ck = get_old_ck_client()
            new_ck = get_new_ck_client()
            row_count = migrate_task(old_ck, new_ck, task_id, task_type, country_code)
            write_migrate_log(new_ck, task_id, task_type, country_code, new_table, row_count, "success")
            progress.fix_retry(row_count)
            log.info("  -> 重试成功 | %d 行", row_count)
            return True
        except Exception as retry_e:
            log.error("  -> 重试失败: %s", str(retry_e)[:500])
            return False


def format_duration(seconds):
    if seconds < 60:
        return f"{seconds:.1f}s"
    elif seconds < 3600:
        return f"{int(seconds // 60)}m {int(seconds % 60)}s"
    else:
        return f"{int(seconds // 3600)}h {int((seconds % 3600) // 60)}m"


# ============================================================
# 主流程
# ============================================================

def main():
    parser = argparse.ArgumentParser(description="ClickHouse 数据迁移脚本（多线程版）")
    parser.add_argument("--limit", type=int, default=0, help="只迁移前 N 个任务（测试用，0=全部）")
    parser.add_argument("--workers", type=int, default=8, help="并行线程数（默认 8）")
    args = parser.parse_args()

    log.info("=" * 60)
    log.info("ClickHouse 数据迁移开始")
    log.info("  模式: %s", f"测试（限制 {args.limit} 个任务）" if args.limit > 0 else "全量")
    log.info("  线程数: %d", args.workers)
    log.info("  批次大小: %d", BATCH_SIZE)
    log.info("=" * 60)

    start_time = time.time()

    mysql_conn = get_mysql_conn()
    new_ck_main = get_new_ck_client()

    log.info("加载已迁移任务列表...")
    migrated_set = load_migrated_tasks(new_ck_main)
    log.info("已迁移任务数: %d", len(migrated_set))

    log.info("从 MySQL 读取任务列表...")
    tasks = fetch_tasks(mysql_conn)
    mysql_conn.close()
    log.info("MySQL 中总任务数: %d", len(tasks))

    pending_tasks = [t for t in tasks if t["task_id"] not in migrated_set]
    skip_count = len(tasks) - len(pending_tasks)
    log.info("待迁移: %d, 已跳过(之前已完成): %d", len(pending_tasks), skip_count)

    if args.limit > 0:
        pending_tasks = pending_tasks[:args.limit]
        log.info("测试模式：本次只处理 %d 个任务", len(pending_tasks))

    if not pending_tasks:
        log.info("没有需要迁移的任务，退出")
        return

    progress = ProgressCounter(len(pending_tasks))

    with ThreadPoolExecutor(max_workers=args.workers, thread_name_prefix="migrate") as executor:
        futures = {
            executor.submit(worker, task, progress): task
            for task in pending_tasks
        }

        for future in as_completed(futures):
            try:
                future.result()
            except Exception as e:
                log.error("线程异常: %s", str(e)[:500])

    total_elapsed = time.time() - start_time
    _, _, succ, fail, total_rows, _ = progress.get_progress()

    log.info("")
    log.info("=" * 60)
    log.info(" 迁移结果汇总")
    log.info("=" * 60)
    log.info("  线程数: %d", args.workers)
    log.info("  批次大小: %d", BATCH_SIZE)
    log.info("  成功:   %d 个任务", succ)
    log.info("  失败:   %d 个任务", fail)
    log.info("  跳过:   %d 个任务（之前已迁移）", skip_count)
    log.info("  总行数: %d 行", total_rows)
    log.info("  总耗时: %s", format_duration(total_elapsed))
    if succ > 0:
        log.info("  吞吐:   %.0f 行/秒", total_rows / total_elapsed)
    log.info("=" * 60)


if __name__ == "__main__":
    main()
