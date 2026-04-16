package com.ts.download.config;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.spring.boot.autoconfigure.DruidDataSourceBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * ClickHouse 配置
 * 
 * @author TS Team
 */
@Configuration
public class ClickHouseConfig {

    @Value("${spring.datasource.url}")
    private String ckUrl;

    @Value("${spring.datasource.username}")
    private String ckUsername;

    @Value("${spring.datasource.password}")
    private String ckPassword;

    @Value("${spring.datasource.driver-class-name}")
    private String ckDriver;

    /**
     * API 查询专用数据源（主数据源）
     */
    @Bean(name = "clickHouseDataSource")
    @Primary
    @ConfigurationProperties(prefix = "spring.datasource.druid")
    public DataSource clickHouseDataSource() {
        return DruidDataSourceBuilder.create().build();
    }

    /**
     * API 查询专用 JdbcTemplate
     */
    @Bean(name = "jdbcTemplate")
    @Primary
    public JdbcTemplate clickHouseJdbcTemplate() {
        return new JdbcTemplate(clickHouseDataSource());
    }

    /**
     * 迁移写入专用数据源（独立连接池，避免与 API 争用）
     */
    @Bean(name = "migrateWriteDataSource")
    @ConditionalOnProperty(name = "migrate.enabled", havingValue = "true")
    public DataSource migrateWriteDataSource() {
        DruidDataSource ds = new DruidDataSource();
        ds.setDriverClassName(ckDriver);
        ds.setUrl(ckUrl);
        ds.setUsername(ckUsername);
        ds.setPassword(ckPassword);
        ds.setInitialSize(0);
        ds.setMinIdle(0);
        ds.setMaxActive(8);
        ds.setMaxWait(30000);
        ds.setTestWhileIdle(true);
        ds.setValidationQuery("SELECT 1");
        ds.setMinEvictableIdleTimeMillis(60000);
        ds.setTimeBetweenEvictionRunsMillis(60000);
        return ds;
    }

    /**
     * 迁移写入专用 JdbcTemplate
     */
    @Bean(name = "migrateWriteJdbcTemplate")
    @ConditionalOnProperty(name = "migrate.enabled", havingValue = "true")
    public JdbcTemplate migrateWriteJdbcTemplate() {
        return new JdbcTemplate(migrateWriteDataSource());
    }
}
