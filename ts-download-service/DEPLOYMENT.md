# TS文件下载服务部署指南

## 概述
本项目已修改为支持本地文件存储，文件不再上传到COS，而是保存到服务器本地，并通过HTTP接口提供下载。

## 主要变更

### 1. 新增功能
- **本地文件存储**：文件保存到服务器本地磁盘
- **文件下载接口**：通过HTTP接口直接下载文件
- **自动清理**：定时清理过期文件
- **配置切换**：支持本地存储和COS存储之间切换

### 2. 新增文件
- `LocalFileProperties.java` - 本地文件存储配置
- `LocalFileUtil.java` - 本地文件存储工具类
- `FileController.java` - 文件下载控制器
- `FileCleanupTask.java` - 文件清理定时任务

### 3. 修改文件
- `DownloadServiceImpl.java` - 支持本地存储和COS存储切换
- `application.yml` - 添加本地存储配置
- `TsDownloadApplication.java` - 启用定时任务

## 部署步骤

### 1. 修改配置文件
编辑 `src/main/resources/application.yml`：

```yaml
# 本地文件存储配置
local:
  file:
    # 本地文件存储根目录（修改为服务器实际路径）
    base-path: /data/ts-download-files  # Linux服务器路径
    # 服务器域名或IP（修改为实际域名）
    server-domain: http://yourdomain.com  # 或 http://服务器IP:8080
    # 文件访问路径前缀
    access-prefix: /files
    # 是否启用本地存储（true=本地存储，false=COS存储）
    enabled: true
```

### 2. 创建文件存储目录
在服务器上创建文件存储目录：
```bash
# Linux服务器
sudo mkdir -p /data/ts-download-files
sudo chown -R your-user:your-group /data/ts-download-files
sudo chmod 755 /data/ts-download-files

# Windows服务器
mkdir D:\ts-download-files
```

### 3. 构建项目
```bash
mvn clean package -DskipTests
```

### 4. 部署运行
```bash
# 方式1：直接运行jar包
java -jar ts-download-service-1.0.0.jar

# 方式2：使用systemd服务（推荐）
# 创建服务文件 /etc/systemd/system/ts-download.service
sudo systemctl start ts-download
sudo systemctl enable ts-download
```

### 5. 验证部署
访问以下地址验证：
- API文档：`http://yourdomain.com:8080/swagger-ui/`
- 健康检查：`http://yourdomain.com:8080/actuator/health`

## 文件访问方式

### 下载地址格式
生成的下载地址格式为：
```
http://yourdomain.com:8080/files/download/20251104/1730699123456_abc12345_gender_US.xlsx
```

### 目录结构
```
/data/ts-download-files/
├── download/
│   ├── 20251104/          # 按日期分目录
│   │   ├── file1.xlsx
│   │   └── file2.txt
│   └── 20251105/
│       ├── file3.xlsx
│       └── file4.txt
```

## 文件清理策略

### 自动清理
- **每日清理**：每天凌晨2点，删除7天前的文件
- **周度清理**：每周日凌晨3点，删除3天前的文件

### 手动清理
可以通过调用 `LocalFileUtil.cleanupExpiredFiles(days)` 方法手动清理。

## 配置说明

### 本地存储配置
```yaml
local:
  file:
    base-path: /data/ts-download-files    # 文件存储根目录
    server-domain: http://yourdomain.com  # 服务器域名
    access-prefix: /files                 # 访问路径前缀
    enabled: true                         # 启用本地存储
```

### 存储模式切换
- `enabled: true` - 使用本地存储
- `enabled: false` - 使用COS存储（需要配置aws.s3相关参数）

## 安全考虑

### 1. 文件访问控制
- 文件下载接口包含路径安全检查
- 防止目录遍历攻击
- 只允许访问指定目录下的文件

### 2. 磁盘空间监控
建议监控磁盘使用情况，防止文件过多导致磁盘满：
```bash
# 查看磁盘使用情况
df -h /data/ts-download-files

# 查看目录大小
du -sh /data/ts-download-files
```

### 3. 文件权限
确保应用有足够的文件读写权限：
```bash
sudo chown -R app-user:app-group /data/ts-download-files
sudo chmod 755 /data/ts-download-files
```

## 性能优化

### 1. 文件服务
- 使用Nginx作为反向代理，提高文件下载性能
- 启用gzip压缩
- 设置适当的缓存策略

### 2. 存储优化
- 定期清理过期文件
- 监控磁盘使用情况
- 考虑使用SSD提高I/O性能

## 故障排查

### 1. 文件无法下载
- 检查文件是否存在
- 检查文件权限
- 检查配置中的域名是否正确

### 2. 磁盘空间不足
- 手动清理过期文件
- 调整清理策略
- 增加磁盘空间

### 3. 日志查看
```bash
# 查看应用日志
tail -f logs/ts-download.log

# 查看文件清理日志
grep "文件清理" logs/ts-download.log
```

## 示例配置

### Nginx配置（可选）
```nginx
server {
    listen 80;
    server_name yourdomain.com;
    
    # API接口代理
    location /api/ {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
    
    # 文件下载代理（可选，直接用应用服务也可以）
    location /files/ {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        
        # 文件下载优化
        proxy_buffering off;
        proxy_request_buffering off;
    }
}
```

### systemd服务配置
```ini
[Unit]
Description=TS Download Service
After=network.target

[Service]
Type=simple
User=app-user
ExecStart=/usr/bin/java -jar /opt/ts-download/ts-download-service-1.0.0.jar
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

## 联系支持
如有问题，请联系开发团队。
