package com.ts.download.config;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 腾讯云COS配置（使用AWS S3 SDK兼容）
 * 
 * @author TS Team
 */
@Configuration
public class CosConfig {

    @Autowired
    private CosProperties cosProperties;

    @Bean
    public AmazonS3 cosClient() {
        // 使用腾讯云的SecretId和SecretKey
        BasicAWSCredentials credentials = new BasicAWSCredentials(
                cosProperties.getAccessKeyId(),
                cosProperties.getSecretAccessKey()
        );

        ClientConfiguration clientConfig = new ClientConfiguration();
        clientConfig.setMaxConnections(100);
        clientConfig.setConnectionTimeout(30000);
        clientConfig.setSocketTimeout(60000);

        // 腾讯云COS兼容AWS S3协议
        return AmazonS3ClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(credentials))
                .withEndpointConfiguration(
                        new AwsClientBuilder.EndpointConfiguration(
                                cosProperties.getEndpoint(),
                                cosProperties.getRegion()
                        )
                )
                .withClientConfiguration(clientConfig)
                .withPathStyleAccessEnabled(false)
                .build();
    }
}
