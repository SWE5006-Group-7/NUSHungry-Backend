[根目录](../CLAUDE.md) > **media-service**

---

# Media Service - 媒体服务

## 变更记录 (Changelog)

### 2025-11-13 10:00:07 UTC
- 初始化模块文档
- 记录媒体文件上传与管理功能
- 整理图片处理与存储策略

---

## 模块职责

Media Service 负责 NUSHungry 平台的媒体文件管理：
- **文件上传**: 接收用户上传的图片文件 (支持前端裁剪后的图片)
- **文件存储**: 本地文件系统存储 (可扩展至云存储如 AWS S3)
- **URL 生成**: 为上传的文件生成可访问的 URL
- **元数据管理**: 在数据库中记录文件路径、大小、上传者等信息
- **图片处理**: 缩略图生成、格式转换 (可选扩展)
- **文件访问**: 提供文件下载/访问接口
- **管理员功能**: 查看所有图片、删除违规图片、统计图片使用情况

---

## 入口与启动

### 主启动类
- **路径**: `src/main/java/com/nushungry/mediaservice/MediaServiceApplication.java`
- **端口**: 8085 (默认)
- **框架**: Spring Boot 3.x + Spring Data JPA

### 启动方式
```bash
# 开发模式
mvn spring-boot:run

# 生产模式
mvn clean package -DskipTests
java -jar target/media-service-0.0.1-SNAPSHOT.jar

# Docker 启动
docker build -t media-service .
docker run -p 8085:8085 -v /path/to/uploads:/uploads media-service
```

### 健康检查
```bash
curl http://localhost:8085/actuator/health
```

---

## 对外接口

### 文件上传接口 (FileUploadController)

| 方法 | 路径 | 说明 | 认证要求 |
|-----|------|-----|---------|
| POST | `/media/upload` | 上传单个文件 | 需登录 |
| GET | `/media/{fileName}` | 下载/访问文件 | 无 |

**上传请求示例:**
```bash
POST /media/upload
Headers:
  X-User-Id: 123
  Content-Type: multipart/form-data

Body:
  file: [binary file data]
```

**上传响应示例:**
```json
{
  "success": true,
  "url": "http://localhost:8085/media/images/20251113_abc123.jpg",
  "fileName": "20251113_abc123.jpg",
  "fileSize": 204800,
  "uploadedAt": "2025-11-13T10:00:00"
}
```

### 管理员接口 (AdminImageController)

| 方法 | 路径 | 说明 | 认证要求 |
|-----|------|-----|---------|
| GET | `/media/admin/images` | 查询所有图片 (分页) | 管理员 |
| GET | `/media/admin/images/{id}` | 获取图片详情 | 管理员 |
| DELETE | `/media/admin/images/{id}` | 删除图片 | 管理员 |
| GET | `/media/admin/stats` | 获取图片统计信息 | 管理员 |

**图片统计响应示例:**
```json
{
  "totalImages": 1500,
  "totalSize": 314572800,  // 字节
  "averageSize": 209715,
  "uploadToday": 50
}
```

---

## 关键依赖与配置

### Maven 依赖
```xml
<!-- 核心依赖 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>

<!-- 数据库 (H2/PostgreSQL/MySQL 可配置) -->
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
</dependency>

<!-- 图片处理 (可选) -->
<dependency>
    <groupId>net.coobird</groupId>
    <artifactId>thumbnailator</artifactId>
    <version>0.4.20</version>
</dependency>

<!-- API 文档 -->
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.2.0</version>
</dependency>
```

### 配置文件
- **位置**: `src/main/resources/application.yml` (或 application.properties，已删除)
- **关键配置项**:

```yaml
server:
  port: 8085

spring:
  application:
    name: media-service
  datasource:
    url: jdbc:postgresql://localhost:5432/nushungry_media
    username: postgres
    password: postgres
  jpa:
    hibernate:
      ddl-auto: update
  servlet:
    multipart:
      max-file-size: 10MB      # 单个文件最大 10MB
      max-request-size: 50MB   # 请求最大 50MB

# 文件存储配置
file:
  upload-dir: ./uploads        # 本地存储路径
  base-url: http://localhost:8085/media/images  # 访问 URL 前缀

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
```

---

## 数据模型

### MediaFile (媒体文件实体)
- **路径**: `src/main/java/com/nushungry/mediaservice/model/MediaFile.java`
- **表名**: `media_files`
- **字段**:
  - `id` (Long): 主键
  - `fileName` (String): 存储文件名 (含时间戳防重复)
  - `originalFileName` (String): 原始文件名
  - `filePath` (String): 服务器存储路径
  - `fileSize` (Long): 文件大小 (字节)
  - `mimeType` (String): MIME 类型 (如 image/jpeg)
  - `url` (String): 可访问的完整 URL
  - `uploadedBy` (String): 上传者用户 ID
  - `createdAt` (LocalDateTime): 上传时间

### 数据库索引
- `media_files.uploaded_by` (查询用户上传的文件)
- `media_files.created_at` (按时间查询)

---

## 核心组件

### 控制器层

#### FileUploadController
- **路径**: `src/main/java/com/nushungry/mediaservice/controller/FileUploadController.java`
- **职责**:
  - 接收 multipart/form-data 文件上传请求
  - 验证文件类型和大小
  - 生成唯一文件名 (防止覆盖)
  - 调用存储服务保存文件
  - 返回文件访问 URL

**文件名生成策略:**
```
{yyyyMMdd}_{UUID}_{originalFileName}
例如: 20251113_a1b2c3d4_profile.jpg
```

#### AdminImageController
- **路径**: `src/main/java/com/nushungry/mediaservice/controller/AdminImageController.java`
- **职责**:
  - 管理员查询所有图片 (分页、排序)
  - 查看图片详情 (元数据、上传者、使用情况)
  - 删除违规图片
  - 统计图片使用情况

### 服务层

#### ImageProcessingService
- **路径**: `src/main/java/com/nushungry/mediaservice/service/ImageProcessingService.java`
- **职责**:
  - 文件存储到本地文件系统
  - 文件读取和流式传输
  - 图片压缩和缩略图生成 (使用 Thumbnailator 库)
  - 文件删除

**关键方法:**
```java
public String saveFile(MultipartFile file, String userId)
public byte[] loadFile(String fileName)
public void deleteFile(String fileName)
public String generateThumbnail(String fileName, int width, int height)
```

#### AdminImageService
- **路径**: `src/main/java/com/nushungry/mediaservice/service/AdminImageService.java`
- **职责**:
  - 图片列表查询和统计
  - 批量删除操作
  - 图片使用情况分析

### 数据访问层

#### MediaFileRepository
- **路径**: `src/main/java/com/nushungry/mediaservice/repository/MediaFileRepository.java`
- **关键方法**:
  - `findByFileName(String fileName)` - 根据文件名查询
  - `findByUploadedBy(String userId, Pageable pageable)` - 查询用户上传的文件
  - `countByCreatedAtAfter(LocalDateTime date)` - 统计指定日期后上传的文件数
  - `sumFileSizeBy()` - 统计总文件大小 (自定义查询)

### 配置类

#### SecurityConfig
- **路径**: `src/main/java/com/nushungry/mediaservice/config/SecurityConfig.java`
- **职责**:
  - 配置公开端点 (文件访问无需认证)
  - 配置受保护端点 (上传需要认证)

#### OpenApiConfig
- **路径**: `src/main/java/com/nushungry/mediaservice/config/OpenApiConfig.java`
- **职责**: 配置 Swagger UI 文档信息

---

## 存储策略

### 本地存储 (当前实现)
- **存储路径**: 配置项 `file.upload-dir` (默认 `./uploads`)
- **目录结构**:
  ```
  uploads/
  ├── 20251113/
  │   ├── 20251113_a1b2c3d4_image1.jpg
  │   └── 20251113_e5f6g7h8_image2.png
  └── 20251114/
      └── ...
  ```
- **优点**: 简单、无额外成本、适合小规模部署
- **缺点**: 无法跨服务器共享、扩展性差、备份困难

### 云存储扩展 (可选)
- **AWS S3**: 使用 AWS SDK 上传到 S3 桶
- **阿里云 OSS**: 使用阿里云 SDK
- **MinIO**: 开源对象存储，兼容 S3 API
- **配置方式**: 在 `application.yml` 中添加云存储配置，修改 `ImageProcessingService` 实现

**S3 配置示例:**
```yaml
cloud:
  aws:
    s3:
      bucket: nushungry-media
      region: ap-southeast-1
      access-key: ${AWS_ACCESS_KEY}
      secret-key: ${AWS_SECRET_KEY}
```

---

## 测试与质量

### 测试文件位置
- **集成测试**: `MediaServiceIntegrationTest.java`
- **控制器测试**: `FileUploadControllerTest.java`
- **服务层测试**: `ImageProcessingServiceTest.java`
- **数据层测试**: `MediaFileRepositoryTest.java`

### 运行测试
```bash
cd media-service
mvn test
mvn test jacoco:report  # 生成覆盖率报告
```

### 测试覆盖范围
- 文件上传流程 (正常、超大文件、非法格式)
- 文件访问和下载
- 文件删除和清理
- 元数据存储和查询

---

## 常见问题 (FAQ)

### Q: 支持哪些文件格式？
A: 默认支持常见图片格式 (JPEG, PNG, GIF, WebP)。可在控制器中添加 MIME 类型白名单验证。

### Q: 文件大小限制是多少？
A: 默认单文件最大 10MB，配置项: `spring.servlet.multipart.max-file-size`。

### Q: 如何防止文件名冲突？
A: 使用时间戳 + UUID 生成唯一文件名，原始文件名仅用于显示。

### Q: 上传的文件如何被其他服务使用？
A: media-service 返回文件访问 URL，其他服务 (如 review-service, cafeteria-service) 存储该 URL 并在需要时展示。

### Q: 如何清理未使用的图片？
A: 需要定期扫描 `media_files` 表，检查图片 URL 是否仍被其他服务引用 (可通过调用其他服务 API 或数据库联表查询)，删除孤立图片。

### Q: 如何迁移到云存储？
A:
1. 修改 `ImageProcessingService` 实现，使用 AWS SDK/阿里云 SDK
2. 更新配置文件添加云存储凭证
3. 逐步迁移现有文件到云端
4. 更新 URL 生成逻辑

### Q: 如何处理图片缩放？
A: 使用 `ImageProcessingService.generateThumbnail()` 生成缩略图，存储为新文件并返回缩略图 URL。

---

## 相关文件清单

### 核心文件
- `src/main/java/com/nushungry/mediaservice/MediaServiceApplication.java` - 启动类
- `src/main/java/com/nushungry/mediaservice/controller/FileUploadController.java` - 文件上传控制器
- `src/main/java/com/nushungry/mediaservice/controller/AdminImageController.java` - 管理员图片控制器
- `src/main/java/com/nushungry/mediaservice/service/ImageProcessingService.java` - 图片处理服务
- `src/main/java/com/nushungry/mediaservice/service/AdminImageService.java` - 管理员图片服务
- `src/main/java/com/nushungry/mediaservice/model/MediaFile.java` - 媒体文件实体
- `src/main/java/com/nushungry/mediaservice/repository/MediaFileRepository.java` - 数据访问接口

### DTO
- `src/main/java/com/nushungry/mediaservice/dto/ImageDetailResponse.java` - 图片详情响应
- `src/main/java/com/nushungry/mediaservice/dto/ImageStatsResponse.java` - 图片统计响应
- `src/main/java/com/nushungry/mediaservice/common/ApiResponse.java` - 统一响应格式

### 配置文件
- `src/main/resources/logback-spring.xml` - 日志配置
- `pom.xml` - Maven 项目配置
- `Dockerfile` - Docker 镜像构建
- `docker-compose.yml` - 本地容器编排

### 测试文件
- `src/test/java/com/nushungry/mediaservice/MediaServiceIntegrationTest.java`
- `src/test/java/com/nushungry/mediaservice/controller/FileUploadControllerTest.java`
- `src/test/java/com/nushungry/mediaservice/service/ImageProcessingServiceTest.java`
- `src/test/java/com/nushungry/mediaservice/repository/MediaFileRepositoryTest.java`

### 脚本文件
- `scripts/init_media_db.sql` - 数据库初始化脚本
- `scripts/start-services.sh` - 启动脚本
- `scripts/stop-services.sh` - 停止脚本

---

**模块维护者**: Media Service Team
**最后更新**: 2025-11-13 10:00:07 UTC
**相关服务**: review-service, cafeteria-service, gateway-service
