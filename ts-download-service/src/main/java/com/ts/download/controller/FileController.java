package com.ts.download.controller;

import com.ts.download.config.LocalFileProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 文件下载控制器
 * 提供静态文件访问功能
 * 
 * @author TS Team
 */
@Slf4j
@RestController
@RequestMapping("/files")
public class FileController {

    @Autowired
    private LocalFileProperties localFileProperties;

    /**
     * 下载文件
     * 访问路径：/files/{folder}/{fileName}
     * 例如：/files/download/20251104/1730699123456_abc12345_gender_US.xlsx
     */
    @GetMapping("/{folder}/{fileName}")
    public ResponseEntity<Resource> downloadFile(
            @PathVariable String folder,
            @PathVariable String fileName,
            HttpServletRequest request) {
        
        try {
            // 构建文件路径
            Path filePath = Paths.get(localFileProperties.getBasePath(), folder, fileName);
            
            log.info("请求下载文件：{}", filePath.toString());
            
            // 检查文件是否存在
            if (!Files.exists(filePath)) {
                log.warn("文件不存在：{}", filePath.toString());
                return ResponseEntity.notFound().build();
            }
            
            // 创建资源对象
            Resource resource = new FileSystemResource(filePath.toFile());
            
            // 获取文件的MIME类型
            String contentType = null;
            try {
                contentType = Files.probeContentType(filePath);
            } catch (IOException ex) {
                log.info("无法确定文件类型");
            }
            
            // 如果无法确定文件类型，使用默认类型
            if (contentType == null) {
                if (fileName.endsWith(".xlsx")) {
                    contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
                } else if (fileName.endsWith(".txt")) {
                    contentType = "text/plain";
                } else {
                    contentType = "application/octet-stream";
                }
            }
            
            // 设置响应头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(contentType));
            
            // 设置文件下载名称（处理中文文件名）
            String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8.toString())
                    .replaceAll("\\+", "%20");
            headers.setContentDispositionFormData("attachment", encodedFileName);
            
            // 设置文件大小
            headers.setContentLength(resource.contentLength());
            
            log.info("文件下载成功：{}, 大小：{}字节", fileName, resource.contentLength());
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(resource);
                    
        } catch (Exception e) {
            log.error("文件下载失败：{}/{}", folder, fileName, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 支持多级目录的文件下载
     * 访问路径：/files/{folder}/{subFolder}/{fileName}
     * 例如：/files/download/20251104/subfolder/file.xlsx
     */
    @GetMapping("/{folder}/{subFolder}/{fileName}")
    public ResponseEntity<Resource> downloadFileWithSubFolder(
            @PathVariable String folder,
            @PathVariable String subFolder,
            @PathVariable String fileName,
            HttpServletRequest request) {
        
        try {
            // 构建文件路径
            Path filePath = Paths.get(localFileProperties.getBasePath(), folder, subFolder, fileName);
            
            log.info("请求下载文件：{}", filePath.toString());
            
            // 检查文件是否存在
            if (!Files.exists(filePath)) {
                log.warn("文件不存在：{}", filePath.toString());
                return ResponseEntity.notFound().build();
            }
            
            // 安全检查：确保文件路径在允许的目录内
            Path basePath = Paths.get(localFileProperties.getBasePath()).toAbsolutePath().normalize();
            Path requestedPath = filePath.toAbsolutePath().normalize();
            
            if (!requestedPath.startsWith(basePath)) {
                log.warn("非法文件访问路径：{}", requestedPath.toString());
                return ResponseEntity.badRequest().build();
            }
            
            // 创建资源对象
            Resource resource = new FileSystemResource(filePath.toFile());
            
            // 获取文件的MIME类型
            String contentType = null;
            try {
                contentType = Files.probeContentType(filePath);
            } catch (IOException ex) {
                log.info("无法确定文件类型");
            }
            
            // 如果无法确定文件类型，使用默认类型
            if (contentType == null) {
                if (fileName.endsWith(".xlsx")) {
                    contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
                } else if (fileName.endsWith(".txt")) {
                    contentType = "text/plain";
                } else {
                    contentType = "application/octet-stream";
                }
            }
            
            // 设置响应头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(contentType));
            
            // 设置文件下载名称（处理中文文件名）
            String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8.toString())
                    .replaceAll("\\+", "%20");
            headers.setContentDispositionFormData("attachment", encodedFileName);
            
            // 设置文件大小
            headers.setContentLength(resource.contentLength());
            
            log.info("文件下载成功：{}, 大小：{}字节", fileName, resource.contentLength());
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(resource);
                    
        } catch (Exception e) {
            log.error("文件下载失败：{}/{}/{}", folder, subFolder, fileName, e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
