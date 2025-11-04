package com.ts.download;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * TS文件下载服务启动类
 * 
 * @author TS Team
 */
@SpringBootApplication
@EnableScheduling
public class TsDownloadApplication {

    public static void main(String[] args) {
        SpringApplication.run(TsDownloadApplication.class, args);
        System.out.println("====================================");
        System.out.println("TS文件下载服务启动成功！");
        System.out.println("Swagger文档地址: http://localhost:8080/swagger-ui/");
        System.out.println("====================================");
    }
}
