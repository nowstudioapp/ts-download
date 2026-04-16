package com.ts.download.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import com.alibaba.druid.pool.DruidDataSource;

import javax.sql.DataSource;

@Configuration
@ConditionalOnProperty(name = "migrate.enabled", havingValue = "true")
public class MigrateDataSourceConfig {

    @Value("${migrate.mysql.url}")
    private String mysqlUrl;

    @Value("${migrate.mysql.username}")
    private String mysqlUsername;

    @Value("${migrate.mysql.password}")
    private String mysqlPassword;

    @Value("${migrate.old-clickhouse.url}")
    private String oldCkUrl;

    @Value("${migrate.old-clickhouse.username}")
    private String oldCkUsername;

    @Value("${migrate.old-clickhouse.password}")
    private String oldCkPassword;

    /**
     * 旧 MySQL：每小时只查一次任务列表，懒初始化 + 空闲即回收，不维持长连接
     */
    @Bean("oldMysqlDataSource")
    public DataSource oldMysqlDataSource() {
        DruidDataSource ds = new DruidDataSource();
        ds.setDriverClassName("com.mysql.cj.jdbc.Driver");
        ds.setUrl(mysqlUrl);
        ds.setUsername(mysqlUsername);
        ds.setPassword(mysqlPassword);
        ds.setInitialSize(0);
        ds.setMinIdle(0);
        ds.setMaxActive(2);
        ds.setMaxWait(30000);
        ds.setTestWhileIdle(true);
        ds.setValidationQuery("SELECT 1");
        ds.setMinEvictableIdleTimeMillis(60000);
        ds.setTimeBetweenEvictionRunsMillis(60000);
        return ds;
    }

    /**
     * 旧 ClickHouse：迁移时批量读取，迁移完空闲即回收
     */
    @Bean("oldClickHouseDataSource")
    public DataSource oldClickHouseDataSource() {
        DruidDataSource ds = new DruidDataSource();
        ds.setDriverClassName("ru.yandex.clickhouse.ClickHouseDriver");
        ds.setUrl(oldCkUrl);
        ds.setUsername(oldCkUsername);
        ds.setPassword(oldCkPassword);
        ds.setInitialSize(0);
        ds.setMinIdle(0);
        ds.setMaxActive(5);
        ds.setMaxWait(30000);
        ds.setTestWhileIdle(true);
        ds.setValidationQuery("SELECT 1");
        ds.setMinEvictableIdleTimeMillis(60000);
        ds.setTimeBetweenEvictionRunsMillis(60000);
        return ds;
    }

    @Bean("oldMysqlJdbcTemplate")
    public JdbcTemplate oldMysqlJdbcTemplate() {
        return new JdbcTemplate(oldMysqlDataSource());
    }

    @Bean("oldClickHouseJdbcTemplate")
    public JdbcTemplate oldClickHouseJdbcTemplate() {
        return new JdbcTemplate(oldClickHouseDataSource());
    }
}
