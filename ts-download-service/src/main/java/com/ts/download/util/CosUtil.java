package com.ts.download.util;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.ts.download.config.CosProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.Date;

/**
 * 腾讯云COS工具类
 * 
 * @author TS Team
 */
@Slf4j
@Component
public class CosUtil {

    @Autowired
    private AmazonS3 cosClient;

    @Autowired
    private CosProperties cosProperties;

    /**
     * 上传文件到COS
     * 
     * @param file 本地文件
     * @param folder 文件夹路径（如：download/20251030）
     * @return 下载URL
     */
    public String uploadToS3(File file, String folder) {
        try {
            String fileName = file.getName();
            String key = folder + "/" + fileName;

            log.info("开始上传文件到COS，bucket={}, key={}", cosProperties.getBucketName(), key);

            // 设置文件元数据
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.length());
            
            // 根据文件扩展名设置Content-Type
            if (fileName.endsWith(".txt")) {
                metadata.setContentType("text/plain");
            } else if (fileName.endsWith(".xlsx")) {
                metadata.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            }

            // 上传文件
            try (InputStream inputStream = new FileInputStream(file)) {
                PutObjectRequest putObjectRequest = new PutObjectRequest(
                        cosProperties.getBucketName(),
                        key,
                        inputStream,
                        metadata
                );
                
                cosClient.putObject(putObjectRequest);
            }

            log.info("文件上传成功，key={}", key);

            // 生成预签名URL（有效期7天）
            Date expiration = new Date(System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000L);
            URL url = cosClient.generatePresignedUrl(cosProperties.getBucketName(), key, expiration);
            
            String downloadUrl = url.toString();
            log.info("生成下载URL成功：{}", downloadUrl);
            
            return downloadUrl;

        } catch (Exception e) {
            log.error("上传文件到COS失败", e);
            throw new RuntimeException("上传文件到COS失败: " + e.getMessage(), e);
        }
    }
}
