package com.ts.download.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;

/**
 * Swagger配置
 * 
 * @author TS Team
 */
@Configuration
public class SwaggerConfig {

    @Value("${swagger.enabled:true}")
    private Boolean enabled;

    @Value("${swagger.title:TS文件下载服务API}")
    private String title;

    @Value("${swagger.description:提供文件下载地址生成功能}")
    private String description;

    @Value("${swagger.version:1.0.0}")
    private String version;

    @Value("${swagger.base-package:com.ts.download.controller}")
    private String basePackage;

    @Bean
    public Docket createRestApi() {
        return new Docket(DocumentationType.OAS_30)
                .enable(enabled)
                .apiInfo(apiInfo())
                .select()
                .apis(RequestHandlerSelectors.basePackage(basePackage))
                .paths(PathSelectors.any())
                .build();
    }

    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title(title)
                .description(description)
                .version(version)
                .build();
    }
}
