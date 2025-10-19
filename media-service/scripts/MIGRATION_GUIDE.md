# Media Service 数据库迁移指南

## 📋 目录
- [概述](#概述)
- [前置条件](#前置条件)
- [迁移策略](#迁移策略)
- [迁移步骤](#迁移步骤)
- [数据验证](#数据验证)
- [文件迁移](#文件迁移)
- [常见问题](#常见问题)

---

## 概述

本指南说明如何将 Media Service 相关数据从单体架构迁移到独立的 PostgreSQL 数据库，并处理文件存储迁移。

### 涉及的表
- **media_files** - 文件元数据表（新表）
- **image_metadata** - 图片元数据表（新表）
- **upload_sessions** - 上传会话表（新表）

### 涉及的文件
- **图片文件**: 食堂、档口、评价的图片
- **用户头像**: 用户上传的头像
- **其他文件**: PDF、文档等

---

## 前置条件

### 软件要求
- PostgreSQL 16+
- Docker & Docker Compose
- MinIO (可选，用于对象存储)

---

## 迁移策略

### 策略 A: 全新部署（推荐开发环境）

**流程：**
1. 运行 `init_media_db.sql` 初始化数据库
2. 创建文件存储目录
3. 文件通过应用上传时自动记录元数据

**优点：**
- 简单快速
- 无历史数据负担

---

### 策略 B: 数据和文件迁移（生产环境）

**流程：**
1. 初始化数据库
2. 从单体架构的 `image` 表导出元数据
3. 转换为 `media_files` 表格式
4. 复制文件到新的存储位置
5. 导入元数据到数据库

---

## 迁移步骤

### Step 1: 初始化数据库

```bash
cd media-service

# 使用 Docker Compose 启动
docker-compose up -d postgres

# 或手动执行
psql -h localhost -p 5434 -U media -d media_service -f scripts/init_media_db.sql
```

### Step 2: 验证表结构

```sql
\c media_service

-- 查看表列表
\dt

-- 预期表: media_files, image_metadata, upload_sessions

-- 查看视图
\dv

-- 预期视图: file_statistics, image_statistics
```

---

## 文件迁移

### 本地文件迁移

```bash
# 从单体架构复制文件
cp -r ../uploads/* ./uploads/

# 调整文件权限
chown -R 1000:1000 ./uploads
```

### MinIO 迁移

```bash
# 安装 MinIO Client
wget https://dl.min.io/client/mc/release/linux-amd64/mc
chmod +x mc

# 配置 MinIO
mc alias set local http://localhost:9000 minioadmin minioadmin

# 创建 bucket
mc mb local/nushungry-media

# 上传文件
mc cp --recursive ../uploads/* local/nushungry-media/
```

---

## 数据验证

### 文件统计

```sql
-- 查看文件统计
SELECT * FROM file_statistics;

-- 查看图片统计
SELECT * FROM image_statistics;

-- 验证文件数量
SELECT 
    storage_type,
    COUNT(*) as file_count,
    pg_size_pretty(SUM(file_size)) as total_size
FROM media_files
GROUP BY storage_type;
```

---

## 常见问题

### Q1: 文件路径错误？

**解决方案**:
```sql
-- 更新文件路径前缀
UPDATE media_files
SET file_url = REPLACE(file_url, 'http://localhost:8080', 'http://localhost:8085')
WHERE file_url LIKE 'http://localhost:8080%';
```

### Q2: 清理过期会话？

**解决方案**:
```sql
-- 手动清理
SELECT clean_expired_sessions();

-- 查看清理结果
SELECT * FROM upload_sessions WHERE status IN ('COMPLETED', 'FAILED');
```

---

**最后更新**: 2025-01-19
