package com.ts.download.util;

import com.ts.download.config.LocalFileProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 本地文件存储工具类
 * 
 * @author TS Team
 */
@Slf4j
@Component
public class LocalFileUtil {

    @Autowired
    private LocalFileProperties localFileProperties;

    /**
     * 保存文件到本地存储
     * 
     * @param sourceFile 源文件
     * @param folder 文件夹路径（如：download/20251030）
     * @return 下载URL
     */
    public String saveToLocal(File sourceFile, String folder) {
        try {
            String fileName = sourceFile.getName();
            
            // 创建目标目录
            Path targetDir = Paths.get(localFileProperties.getBasePath(), folder);
            Files.createDirectories(targetDir);
            
            // 目标文件路径
            Path targetFile = targetDir.resolve(fileName);
            
            log.info("开始保存文件到本地，源文件={}, 目标文件={}", sourceFile.getAbsolutePath(), targetFile.toString());
            
            // 复制文件到目标位置
            Files.copy(sourceFile.toPath(), targetFile, StandardCopyOption.REPLACE_EXISTING);
            
            log.info("文件保存成功，目标路径={}", targetFile.toString());
            
            // 生成下载URL
            String downloadUrl = generateDownloadUrl(folder, fileName);
            log.info("生成下载URL成功：{}", downloadUrl);
            
            return downloadUrl;
            
        } catch (IOException e) {
            log.error("保存文件到本地失败", e);
            throw new RuntimeException("保存文件到本地失败: " + e.getMessage(), e);
        }
    }

    /**
     * 生成下载URL
     * 
     * @param folder 文件夹路径
     * @param fileName 文件名
     * @return 完整的下载URL
     */
    private String generateDownloadUrl(String folder, String fileName) {
        String serverDomain = localFileProperties.getServerDomain();
        String accessPrefix = localFileProperties.getAccessPrefix();
        
        // 确保域名不以/结尾
        if (serverDomain.endsWith("/")) {
            serverDomain = serverDomain.substring(0, serverDomain.length() - 1);
        }
        
        // 确保访问前缀以/开头
        if (!accessPrefix.startsWith("/")) {
            accessPrefix = "/" + accessPrefix;
        }
        
        // 构建完整URL
        return serverDomain + accessPrefix + "/" + folder + "/" + fileName;
    }

    /**
     * 检查文件是否存在
     * 
     * @param folder 文件夹路径
     * @param fileName 文件名
     * @return 是否存在
     */
    public boolean fileExists(String folder, String fileName) {
        Path filePath = Paths.get(localFileProperties.getBasePath(), folder, fileName);
        return Files.exists(filePath);
    }

    /**
     * 获取文件的完整路径
     * 
     * @param folder 文件夹路径
     * @param fileName 文件名
     * @return 文件的完整路径
     */
    public Path getFilePath(String folder, String fileName) {
        return Paths.get(localFileProperties.getBasePath(), folder, fileName);
    }

    /**
     * 删除文件
     * 
     * @param folder 文件夹路径
     * @param fileName 文件名
     * @return 是否删除成功
     */
    public boolean deleteFile(String folder, String fileName) {
        try {
            Path filePath = Paths.get(localFileProperties.getBasePath(), folder, fileName);
            return Files.deleteIfExists(filePath);
        } catch (IOException e) {
            log.error("删除文件失败", e);
            return false;
        }
    }

    /**
     * 清理过期文件（删除指定天数前的文件）
     * 
     * @param days 保留天数
     */
    public void cleanupExpiredFiles(int days) {
        try {
            Path basePath = Paths.get(localFileProperties.getBasePath());
            if (!Files.exists(basePath)) {
                return;
            }
            
            long cutoffTime = System.currentTimeMillis() - (days * 24 * 60 * 60 * 1000L);
            
            Files.walk(basePath)
                .filter(Files::isRegularFile)
                .filter(path -> {
                    try {
                        return Files.getLastModifiedTime(path).toMillis() < cutoffTime;
                    } catch (IOException e) {
                        return false;
                    }
                })
                .forEach(path -> {
                    try {
                        Files.delete(path);
                        log.info("删除过期文件：{}", path.toString());
                    } catch (IOException e) {
                        log.error("删除过期文件失败：{}", path.toString(), e);
                    }
                });
                
        } catch (IOException e) {
            log.error("清理过期文件失败", e);
        }
    }
}
