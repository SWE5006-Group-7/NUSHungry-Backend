# Media Service 部署文档

## 快速开始

### 使用 Docker Compose

```bash
cd media-service

# 启动所有服务
docker-compose up -d

# 或使用启动脚本
./scripts/start-services.sh  # Linux/Mac
./scripts/start-services.bat # Windows
```

### 验证服务

```bash
# 健康检查
curl http://localhost:8085/actuator/health

# API 文档
open http://localhost:8085/swagger-ui.html

# 测试文件上传
curl -X POST http://localhost:8085/api/media/upload \
  -F "file=@test.jpg" \
  -F "entityType=STALL" \
  -F "entityId=1"
```

## 服务访问信息

- **Media Service**: http://localhost:8085
- **Swagger UI**: http://localhost:8085/swagger-ui.html
- **PostgreSQL**: localhost:5434 (用户: media, 密码: password123)
- **MinIO Console**: http://localhost:9001 (用户: minioadmin, 密码: minioadmin)
- **MinIO API**: http://localhost:9000

## 环境变量

| 变量名 | 说明 | 默认值 |
|--------|------|--------|
| DB_HOST | PostgreSQL主机 | localhost |
| DB_PORT | PostgreSQL端口 | 5432 |
| DB_NAME | 数据库名 | media_service |
| FILE_STORAGE_TYPE | 存储类型 | local |
| FILE_UPLOAD_DIR | 本地存储路径 | /app/uploads |
| FILE_MAX_SIZE | 最大文件大小 | 10485760 (10MB) |
| IMAGE_MAX_SIZE | 最大图片大小 | 5242880 (5MB) |
| MINIO_ENDPOINT | MinIO端点 | http://minio:9000 |

## 文件存储

### 本地存储

文件保存在 `./uploads` 目录，通过 Docker volume 映射到容器的 `/app/uploads`。

### MinIO 对象存储

1. 访问 MinIO Console: http://localhost:9001
2. 创建 bucket: `nushungry-media`
3. 设置环境变量 `FILE_STORAGE_TYPE=minio`
4. 重启服务

## 性能优化

### 文件压缩

图片上传时自动压缩，压缩配置在 application.yml:

```yaml
media:
  image:
    compression:
      enabled: true
      quality: 0.8
      max-width: 1920
      max-height: 1080
```

### 缓存策略

- CDN 集成（生产环境推荐）
- 浏览器缓存: 24小时
- 缩略图生成和缓存

## 故障排查

### 文件上传失败

1. 检查文件大小限制
2. 检查存储空间
3. 检查文件权限: `ls -la uploads/`

### MinIO 连接失败

```bash
# 检查 MinIO 状态
docker logs media-minio

# 测试连接
curl http://localhost:9000/minio/health/live
```

---

**最后更新**: 2025-01-19
