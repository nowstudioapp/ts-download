package com.ts.download.config;

import com.alibaba.druid.pool.DruidDataSource;
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

    /**
     * 配置 ClickHouse 数据源
     */
    @Bean(name = "clickHouseDataSource")
    @Primary
    @ConfigurationProperties(prefix = "spring.datasource")
    public DataSource clickHouseDataSource() {
        return new DruidDataSource();
    }

    /**
     * 配置 ClickHouse JdbcTemplate
     */
    @Bean(name = "jdbcTemplate")
    @Primary
    public JdbcTemplate clickHouseJdbcTemplate() {
        return new JdbcTemplate(clickHouseDataSource());
    }
}
