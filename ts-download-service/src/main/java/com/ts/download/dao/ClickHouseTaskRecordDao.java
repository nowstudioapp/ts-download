package com.ts.download.dao;

import com.ts.download.domain.entity.TsWsTaskRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ClickHouse 任务记录 DAO
 * 
 * @author TS Team
 */
@Repository
@Slf4j
public class ClickHouseTaskRecordDao {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final Map<String, String> TASK_TYPE_TO_TABLE;

    static {
        TASK_TYPE_TO_TABLE = new HashMap<>();
        TASK_TYPE_TO_TABLE.put("gender", "ts_ws_task_record");
        TASK_TYPE_TO_TABLE.put("whatsappExist", "ts_ws_task_record");
        TASK_TYPE_TO_TABLE.put("wsValid", "ts_ws_task_record");
        TASK_TYPE_TO_TABLE.put("sieveLive", "ts_tg_task_record");
        TASK_TYPE_TO_TABLE.put("sieveAvatar", "ts_tg_task_record");
        TASK_TYPE_TO_TABLE.put("tgEffective", "ts_tg_task_record");
    }

    /**
     * 根据任务类型和国家代码获取表名
     */
    public String getTableName(String taskType, String countryCode) {
        String tablePrefix = TASK_TYPE_TO_TABLE.getOrDefault(taskType, "ts_other_task_record");
        return tablePrefix + "_" + countryCode.toUpperCase();
    }

    /**
     * 映射 ResultSet 到实体对象
     */
    private TsWsTaskRecord mapResultSetToRecord(ResultSet rs) throws SQLException {
        TsWsTaskRecord record = new TsWsTaskRecord();
        record.setPhone(rs.getString("phone"));
        record.setSex(rs.getString("sex"));
        
        try {
            record.setAge(rs.getInt("age"));
            record.setActiveDay(rs.getInt("active_day"));
            record.setBusinessNumber(rs.getInt("business_number"));
        } catch (SQLException e) {
            // 处理数值类型字段可能为null的情况
        }
        
        record.setMultipleAvatars(rs.getString("multiple_avatars"));
        
        // 获取pic字段（头像）
        record.setPic(rs.getString("pic"));
        
        record.setStatus(rs.getString("status"));
        record.setSkin(rs.getString("skin"));
        record.setHairColor(rs.getString("hair_color"));
        record.setUid(rs.getString("uid"));
        record.setUserName(rs.getString("user_name"));
        
        // 处理 ClickHouse JDBC 驱动的 NULL 时间戳问题
        try {
            Timestamp lastOnlineTime = rs.getTimestamp("last_online_time");
            record.setLastOnlineTime(lastOnlineTime);
        } catch (Exception e) {
            // ClickHouse 中 last_online_time 为 NULL 时会抛出异常
        }
        
        // 映射 create_time 字段
        try {
            Timestamp createTime = rs.getTimestamp("create_time");
            record.setCreateTime(createTime);
        } catch (Exception e) {
            // ClickHouse 中 create_time 为 NULL 时会抛出异常
            record.setCreateTime(null);
        }
        
        record.setMember(rs.getString("member"));
        record.setEthnicity(rs.getString("ethnicity"));
        record.setFirstName(rs.getString("first_name"));
        record.setLastName(rs.getString("last_name"));
        // 表中没有country和region字段，使用country_code代替
        record.setCountry(rs.getString("country_code"));
        record.setRegion(null);
        
        return record;
    }

    /**
     * 统计记录总数（根据phone去重）
     */
    public Long countRecords(String tableName) {
        String sql = "SELECT COUNT(DISTINCT phone) FROM " + tableName;
        log.info("统计SQL: {}", sql);
        try {
            return jdbcTemplate.queryForObject(sql, Long.class);
        } catch (Exception e) {
            log.error("统计记录数失败", e);
            return 0L;
        }
    }

    /**
     * 根据条件查询任务记录列表（支持年龄、性别、排除肤色筛选，使用lastPhone避免OFFSET）
     */
    public List<TsWsTaskRecord> selectTaskRecordListWithConditions(String taskType, String countryCode, 
                                                                     Integer minAge, Integer maxAge, 
                                                                     Integer sex, Integer excludeSkin, 
                                                                     String lastPhone, Integer limit) {
        String tableName = getTableName(taskType, countryCode);
        log.info("=== ClickHouse条件查询 ===");
        log.info("taskType: {}, countryCode: {}, 表名: {}, minAge: {}, maxAge: {}, sex: {}, excludeSkin: {}, lastPhone: {}, limit: {}", 
                taskType, countryCode, tableName, minAge, maxAge, sex, excludeSkin, lastPhone, limit);

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT * FROM ").append(tableName);
        sql.append(" WHERE task_type = '").append(taskType).append("'");
        
        // 添加年龄条件
        if (minAge != null && maxAge != null) {
            sql.append(" AND age >= ").append(minAge).append(" AND age <= ").append(maxAge);
        } else if (minAge != null) {
            sql.append(" AND age >= ").append(minAge);
        } else if (maxAge != null) {
            sql.append(" AND age <= ").append(maxAge);
        }
        
        // 添加性别条件
        if (sex != null) {
            sql.append(" AND sex = '").append(sex).append("'");
        }
        
        // 添加排除肤色条件
        if (excludeSkin != null) {
            sql.append(" AND skin != '").append(excludeSkin).append("'");
        }
        
        // 使用lastPhone代替OFFSET（避免大OFFSET导致内存溢出）
        if (lastPhone != null && !lastPhone.isEmpty()) {
            sql.append(" AND phone > '").append(lastPhone).append("'");
        }
        
        sql.append(" ORDER BY phone, create_time DESC");
        
        // 添加 limit
        if (limit != null && limit > 0) {
            sql.append(" LIMIT ").append(limit);
        } else {
            log.warn("未设置 limit，默认限制为 10000 条记录");
            sql.append(" LIMIT 10000");
        }

        log.info("查询SQL: {}", sql.toString());

        try {
            List<TsWsTaskRecord> records = jdbcTemplate.query(
                sql.toString(), 
                (rs, rowNum) -> mapResultSetToRecord(rs)
            );
            log.info("查询完成，记录数: {}", records.size());
            return records;
        } catch (Exception e) {
            log.error("ClickHouse查询失败", e);
            throw new RuntimeException("ClickHouse查询失败: " + e.getMessage(), e);
        }
    }

    /**
     * 根据条件查询任务记录列表（支持skip跳过指定数量）
     */
    public List<TsWsTaskRecord> selectTaskRecordListWithConditionsAndSkip(String taskType, String countryCode, 
                                                                           Integer minAge, Integer maxAge, 
                                                                           Integer sex, Integer excludeSkin, 
                                                                           Integer checkUserNameEmpty,
                                                                           Integer skip, Integer limit) {
        String tableName = getTableName(taskType, countryCode);
        log.info("=== ClickHouse条件查询（支持skip） ===");
        log.info("taskType: {}, countryCode: {}, 表名: {}, minAge: {}, maxAge: {}, sex: {}, excludeSkin: {}, skip: {}, limit: {}", 
                taskType, countryCode, tableName, minAge, maxAge, sex, excludeSkin, skip, limit);

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT * FROM ").append(tableName);
        sql.append(" WHERE task_type = '").append(taskType).append("'");
        
        // 添加年龄条件
        if (minAge != null && maxAge != null) {
            sql.append(" AND age >= ").append(minAge).append(" AND age <= ").append(maxAge);
        } else if (minAge != null) {
            sql.append(" AND age >= ").append(minAge);
        } else if (maxAge != null) {
            sql.append(" AND age <= ").append(maxAge);
        }
        
        // 添加性别条件（某些任务类型的sex字段存储的是中文）
        if (sex != null) {
            String sexValue = convertSexValue(taskType, sex);
            sql.append(" AND sex = '").append(sexValue).append("'");
        }
        
        // 添加排除肤色条件
        if (excludeSkin != null) {
            sql.append(" AND skin != '").append(excludeSkin).append("'");
        }
        
        // 添加user_name是否为空的条件
        if (checkUserNameEmpty != null) {
            if (checkUserNameEmpty == 0) {
                // 查询user_name为空的
                sql.append(" AND (user_name = '' OR user_name IS NULL)");
            } else if (checkUserNameEmpty == 1) {
                // 查询user_name不为空的
                sql.append(" AND user_name != '' AND user_name IS NOT NULL");
            }
        }
        
        // 按create_time降序排序，优先获取最新数据
        sql.append(" ORDER BY create_time DESC");
        
        // 添加 LIMIT（ClickHouse语法：LIMIT offset, count）
        if (skip != null && skip > 0) {
            if (limit != null && limit > 0) {
                sql.append(" LIMIT ").append(skip).append(", ").append(limit);
            } else {
                log.warn("未设置 limit，默认限制为 10000 条记录");
                sql.append(" LIMIT ").append(skip).append(", 10000");
            }
        } else {
            if (limit != null && limit > 0) {
                sql.append(" LIMIT ").append(limit);
            } else {
                log.warn("未设置 limit，默认限制为 10000 条记录");
                sql.append(" LIMIT 10000");
            }
        }

        log.info("查询SQL: {}", sql.toString());

        try {
            List<TsWsTaskRecord> records = jdbcTemplate.query(
                sql.toString(), 
                (rs, rowNum) -> mapResultSetToRecord(rs)
            );
            log.info("查询完成，记录数: {}", records.size());
            return records;
        } catch (Exception e) {
            log.error("ClickHouse查询失败", e);
            throw new RuntimeException("ClickHouse查询失败: " + e.getMessage(), e);
        }
    }

    /**
     * 根据条件查询任务记录列表（按create_time排序，使用lastCreateTime分页，排除pic字段减少内存使用）
     */
    public List<TsWsTaskRecord> selectTaskRecordListWithConditionsByTime(String taskType, String countryCode, 
                                                                           Integer minAge, Integer maxAge, 
                                                                           Integer sex, Integer excludeSkin, 
                                                                           Integer checkUserNameEmpty,
                                                                           String lastCreateTime, Integer limit) {
        String tableName = getTableName(taskType, countryCode);
        log.info("=== ClickHouse条件查询（按时间，包含pic字段） ===");
        log.info("taskType: {}, countryCode: {}, 表名: {}, minAge: {}, maxAge: {}, sex: {}, excludeSkin: {}, lastCreateTime: {}, limit: {}", 
                taskType, countryCode, tableName, minAge, maxAge, sex, excludeSkin, lastCreateTime, limit);

        StringBuilder sql = new StringBuilder();
        // 包含pic字段（合并下载需要头像列）
        sql.append("SELECT phone, sex, age, active_day, business_number, multiple_avatars, ");
        sql.append("status, skin, hair_color, uid, user_name, last_online_time, create_time, ");
        sql.append("member, ethnicity, first_name, last_name, country_code, pic ");
        sql.append("FROM ").append(tableName);
        sql.append(" WHERE task_type = '").append(taskType).append("'");
        
        // 添加年龄条件
        if (minAge != null && maxAge != null) {
            sql.append(" AND age >= ").append(minAge).append(" AND age <= ").append(maxAge);
        } else if (minAge != null) {
            sql.append(" AND age >= ").append(minAge);
        } else if (maxAge != null) {
            sql.append(" AND age <= ").append(maxAge);
        }
        
        // 添加性别条件（某些任务类型的sex字段存储的是中文）
        if (sex != null) {
            String sexValue = convertSexValue(taskType, sex);
            sql.append(" AND sex = '").append(sexValue).append("'");
        }
        
        // 添加排除肤色条件
        if (excludeSkin != null) {
            sql.append(" AND skin != '").append(excludeSkin).append("'");
        }
        
        // 添加user_name是否为空的条件
        if (checkUserNameEmpty != null) {
            if (checkUserNameEmpty == 0) {
                // 查询user_name为空的
                sql.append(" AND (user_name = '' OR user_name IS NULL)");
            } else if (checkUserNameEmpty == 1) {
                // 查询user_name不为空的
                sql.append(" AND user_name != '' AND user_name IS NOT NULL");
            }
        }
        
        // 使用lastCreateTime代替OFFSET（按时间分页）
        if (lastCreateTime != null && !lastCreateTime.isEmpty()) {
            sql.append(" AND create_time < '").append(lastCreateTime).append("'");
        }
        
        // 按create_time降序排序，优先获取最新数据
        sql.append(" ORDER BY create_time DESC");
        
        // 添加 limit
        if (limit != null && limit > 0) {
            sql.append(" LIMIT ").append(limit);
        } else {
            log.warn("未设置 limit，默认限制为 10000 条记录");
            sql.append(" LIMIT 10000");
        }

        log.info("查询SQL: {}", sql.toString());

        try {
            List<TsWsTaskRecord> records = jdbcTemplate.query(
                sql.toString(), 
                (rs, rowNum) -> mapResultSetToRecord(rs)
            );
            log.info("查询完成，记录数: {}", records.size());
            return records;
        } catch (Exception e) {
            log.error("ClickHouse查询失败", e);
            throw new RuntimeException("ClickHouse查询失败: " + e.getMessage(), e);
        }
    }

    /**
     * 根据phone列表查询任务记录（排除pic字段以减少内存使用）
     */
    public List<TsWsTaskRecord> selectTaskRecordListByPhones(String taskType, String countryCode, List<String> phones) {
        if (phones == null || phones.isEmpty()) {
            return new ArrayList<>();
        }
        
        String tableName = getTableName(taskType, countryCode);
        log.info("=== ClickHouse根据phone列表查询（包含pic字段） ===");
        log.info("taskType: {}, countryCode: {}, 表名: {}, phone数量: {}", taskType, countryCode, tableName, phones.size());

        StringBuilder sql = new StringBuilder();
        // 包含pic字段（合并下载需要头像列）
        sql.append("SELECT phone, sex, age, active_day, business_number, multiple_avatars, ");
        sql.append("status, skin, hair_color, uid, user_name, last_online_time, create_time, ");
        sql.append("member, ethnicity, first_name, last_name, country_code, pic ");
        sql.append("FROM ").append(tableName);
        sql.append(" WHERE task_type = '").append(taskType).append("'");
        sql.append(" AND phone IN (");
        
        // 构建IN条件
        for (int i = 0; i < phones.size(); i++) {
            if (i > 0) sql.append(",");
            sql.append("'").append(phones.get(i)).append("'");
        }
        sql.append(")");
        sql.append(" ORDER BY phone, create_time DESC");

        log.info("查询SQL: {} (phone数量: {})", sql.substring(0, Math.min(200, sql.length())), phones.size());

        try {
            List<TsWsTaskRecord> records = jdbcTemplate.query(
                sql.toString(), 
                (rs, rowNum) -> mapResultSetToRecord(rs)
            );
            log.info("查询完成，记录数: {}", records.size());
            return records;
        } catch (Exception e) {
            log.error("ClickHouse查询失败", e);
            throw new RuntimeException("ClickHouse查询失败: " + e.getMessage(), e);
        }
    }

    /**
     * 统计符合条件的记录总数
     */
    public Long countRecordsWithConditions(String taskType, String countryCode, 
                                           Integer minAge, Integer maxAge, 
                                           Integer sex, Integer excludeSkin,
                                           Integer checkUserNameEmpty) {
        String tableName = getTableName(taskType, countryCode);
        
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT COUNT(*) FROM ").append(tableName);
        sql.append(" WHERE task_type = '").append(taskType).append("'");
        
        // 添加年龄条件
        if (minAge != null && maxAge != null) {
            sql.append(" AND age >= ").append(minAge).append(" AND age <= ").append(maxAge);
        } else if (minAge != null) {
            sql.append(" AND age >= ").append(minAge);
        } else if (maxAge != null) {
            sql.append(" AND age <= ").append(maxAge);
        }
        
        // 添加性别条件（某些任务类型的sex字段存储的是中文）
        if (sex != null) {
            String sexValue = convertSexValue(taskType, sex);
            sql.append(" AND sex = '").append(sexValue).append("'");
        }
        
        // 添加排除肤色条件
        if (excludeSkin != null) {
            sql.append(" AND skin != '").append(excludeSkin).append("'");
        }
        
        // 添加user_name是否为空的条件
        if (checkUserNameEmpty != null) {
            if (checkUserNameEmpty == 0) {
                // 查询user_name为空的
                sql.append(" AND (user_name = '' OR user_name IS NULL)");
            } else if (checkUserNameEmpty == 1) {
                // 查询user_name不为空的
                sql.append(" AND user_name != '' AND user_name IS NOT NULL");
            }
        }

        log.info("统计SQL: {}", sql.toString());
        
        try {
            return jdbcTemplate.queryForObject(sql.toString(), Long.class);
        } catch (Exception e) {
            return 0L;
        }
    }

    /**
     * 查询任务记录列表（不需要 taskId，只根据 taskType 和 countryCode）
     * 根据 phone 去重，支持 limit 限制
     * 
     * 注意：ClickHouse 支持海量数据（百万、千万、上亿级别）
     * - 使用窗口函数进行去重，性能优秀
     * - LIMIT 限制在数据库层面执行，不会加载全部数据到内存
     * - 如果不加 LIMIT，建议调用方分批处理或使用流式导出
     * 
     * @param taskType 任务类型
     * @param countryCode 国家代码
     * @param limit 限制数量（null 或 0 表示不限制，建议设置合理值避免内存溢出）
     * @return 记录列表
     */
    public List<TsWsTaskRecord> selectTaskRecordList(String taskType, String countryCode, Integer limit) {
        String tableName = getTableName(taskType, countryCode);
        log.info("=== ClickHouse查询 ===");
        log.info("taskType: {}, countryCode: {}, 表名: {}, limit: {}", taskType, countryCode, tableName, limit);

        // 直接查询，在应用层去重
        // ClickHouse内存限制导致任何GROUP BY都会失败，改为应用层处理
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT * FROM ").append(tableName);
        sql.append(" WHERE task_type = '").append(taskType).append("'");
        sql.append(" ORDER BY create_time DESC");
        
        // 添加 limit 限制（强烈建议设置，避免一次性加载过多数据到内存）
        if (limit != null && limit > 0) {
            sql.append(" LIMIT ").append(limit);
        } else {
            // 如果不设置 limit，默认最多返回 10000 条，防止内存溢出
            log.warn("未设置 limit，默认限制为 10000 条记录");
            sql.append(" LIMIT 10000");
        }

        List<Object> params = new ArrayList<>();

        log.info("查询SQL: {}", sql.toString());

        try {
            List<TsWsTaskRecord> records = jdbcTemplate.query(
                sql.toString(), 
                params.toArray(), 
                (rs, rowNum) -> mapResultSetToRecord(rs)
            );
            log.info("查询完成，记录数: {}", records.size());
            return records;
        } catch (Exception e) {
            log.error("ClickHouse查询失败", e);
            throw new RuntimeException("ClickHouse查询失败: " + e.getMessage(), e);
        }
    }

    /**
     * 流式查询手机号（只查询phone字段，用于TXT导出，避免内存溢出）
     * @param taskType 任务类型
     * @param countryCode 国家代码
     * @param minAge 最小年龄
     * @param maxAge 最大年龄
     * @param sex 性别
     * @param excludeSkin 排除肤色
     * @param checkUserNameEmpty 检查用户名是否为空
     * @param callback 每批数据的回调处理
     * @param batchSize 每批数量
     */
    public void streamPhoneNumbers(String taskType, String countryCode,
                                   Integer minAge, Integer maxAge,
                                   Integer sex, Integer excludeSkin,
                                   Integer checkUserNameEmpty,
                                   java.util.function.Consumer<List<String>> callback,
                                   int batchSize) {
        String tableName = getTableName(taskType, countryCode);
        log.info("=== ClickHouse流式查询手机号 ===");
        log.info("taskType: {}, countryCode: {}, 表名: {}", taskType, countryCode, tableName);

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT DISTINCT phone FROM ").append(tableName);
        sql.append(" WHERE task_type = '").append(taskType).append("'");
        
        // 添加年龄条件
        if (minAge != null && maxAge != null) {
            sql.append(" AND age >= ").append(minAge).append(" AND age <= ").append(maxAge);
        } else if (minAge != null) {
            sql.append(" AND age >= ").append(minAge);
        } else if (maxAge != null) {
            sql.append(" AND age <= ").append(maxAge);
        }
        
        // 添加性别条件
        if (sex != null) {
            String sexValue = convertSexValue(taskType, sex);
            sql.append(" AND sex = '").append(sexValue).append("'");
        }
        
        // 添加排除肤色条件
        if (excludeSkin != null) {
            sql.append(" AND skin != '").append(excludeSkin).append("'");
        }
        
        // 添加user_name是否为空的条件
        if (checkUserNameEmpty != null) {
            if (checkUserNameEmpty == 0) {
                sql.append(" AND (user_name = '' OR user_name IS NULL)");
            } else if (checkUserNameEmpty == 1) {
                sql.append(" AND user_name != '' AND user_name IS NOT NULL");
            }
        }
        
        sql.append(" ORDER BY phone");

        log.info("流式查询SQL: {}", sql.toString());

        try {
            // 使用流式处理，每批处理 batchSize 条
            List<String> batch = new ArrayList<>();
            jdbcTemplate.query(sql.toString(), (rs) -> {
                batch.add(rs.getString("phone"));
                if (batch.size() >= batchSize) {
                    callback.accept(new ArrayList<>(batch));
                    batch.clear();
                }
            });
            // 处理最后一批
            if (!batch.isEmpty()) {
                callback.accept(batch);
            }
            log.info("流式查询完成");
        } catch (Exception e) {
            log.error("ClickHouse流式查询失败", e);
            throw new RuntimeException("ClickHouse流式查询失败: " + e.getMessage(), e);
        }
    }

    /**
     * 转换性别值（某些任务类型的sex字段存储的是中文）
     * @param taskType 任务类型
     * @param sex 性别参数（0=女, 1=男）
     * @return 转换后的性别值
     */
    private String convertSexValue(String taskType, Integer sex) {
        if (sex == null) {
            return null;
        }
        
        // TG类任务（sieveAvatar、tgEffective、sieveLive）的sex字段存储的是中文
        if (taskType != null && (taskType.equals("sieveAvatar") || taskType.equals("tgEffective") || taskType.equals("sieveLive"))) {
            if (sex == 0) {
                return "女";
            } else if (sex == 1) {
                return "男";
            }
        }
        
        // 其他任务类型直接返回数字字符串
        return sex.toString();
    }
}
