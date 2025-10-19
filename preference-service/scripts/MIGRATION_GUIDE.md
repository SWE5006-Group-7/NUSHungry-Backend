# Preference Service 数据库迁移指南

## 概述
迁移用户收藏和搜索历史数据到独立的 PostgreSQL 数据库。

## 快速开始

### 初始化数据库
```bash
cd preference-service
docker-compose up -d
```

### 从 MySQL 迁移数据

```bash
# 导出数据
mysqldump -h localhost -u root -p nushungry_db favorites search_history > preference_data.sql

# 转换格式并导入
psql -h localhost -p 5435 -U preference -d preference_service -f preference_data.sql
```

### 验证数据

```sql
SELECT COUNT(*) FROM favorites;
SELECT COUNT(*) FROM search_history;
```

**最后更新**: 2025-01-19
