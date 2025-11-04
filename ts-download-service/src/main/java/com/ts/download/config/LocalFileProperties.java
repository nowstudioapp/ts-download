package com.ts.download.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 本地文件存储配置属性
 * 
 * @author TS Team
 */
@Data
@Component
@ConfigurationProperties(prefix = "local.file")
public class LocalFileProperties {

    /** 本地文件存储根目录 */
    private String basePath = "D:/ts-download-files";

    /** 服务器域名或IP */
    private String serverDomain = "http://localhost:8080";

    /** 文件访问路径前缀 */
    private String accessPrefix = "/files";

    /** 是否启用本地存储（true=本地存储，false=COS存储） */
    private boolean enabled = true;
}
