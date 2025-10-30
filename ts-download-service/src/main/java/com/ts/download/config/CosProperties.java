package com.ts.download.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 腾讯云COS配置属性
 * 
 * @author TS Team
 */
@Data
@Component
@ConfigurationProperties(prefix = "aws.s3")
public class CosProperties {

    /** 存储桶名称 */
    private String bucketName;

    /** 区域 */
    private String region;

    /** 访问密钥ID (SecretId) */
    private String accessKeyId;

    /** 访问密钥 (SecretKey) */
    private String secretAccessKey;

    /** 端点 */
    private String endpoint;
}
