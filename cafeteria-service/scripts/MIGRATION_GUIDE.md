# Cafeteria Service 数据库迁移指南

## 📋 目录
- [概述](#概述)
- [前置条件](#前置条件)
- [迁移策略](#迁移策略)
- [迁移步骤](#迁移步骤)
- [数据验证](#数据验证)
- [回滚方案](#回滚方案)
- [常见问题](#常见问题)

---

## 概述

本指南说明如何将 Cafeteria Service 相关数据从单体 MySQL 数据库迁移到独立的 PostgreSQL 数据库。

### 涉及的表
- **cafeteria** - 食堂表（8 条记录）
- **stall** - 档口表（~50 条记录）
- **image** - 图片表（食堂和档口相关图片）

### 迁移类型
- ✅ **全量迁移** - 迁移所有食堂、档口和图片数据
- ✅ **双写模式** - 新老系统并行运行（过渡期）
- ✅ **事件驱动同步** - 通过 RabbitMQ 保持数据一致性

---

## 前置条件

### 软件要求
- PostgreSQL 16+
- MySQL 8.0+ (源数据库)
- Python 3.9+ (用于数据验证脚本)
- Docker & Docker Compose (推荐)

### Python 依赖（可选）
```bash
pip install psycopg2-binary pymysql pandas
```

### 权限要求
- MySQL: 拥有 `SELECT` 权限
- PostgreSQL: 拥有 `CREATE, INSERT, UPDATE, DELETE` 权限

---

## 迁移策略

### 策略 A: 使用初始化脚本（推荐）
适用于：开发环境、测试环境、初次部署

**流程：**
1. 使用 `init_cafeteria_db.sql` 初始化数据库
2. 脚本自动创建表结构并插入初始数据
3. 通过 API 或数据库手动补充缺失数据

**优点：**
- ✅ 简单快速，一键部署
- ✅ 包含 NUS 8 个食堂的真实数据
- ✅ 自动创建索引和约束
- ✅ 无需额外的数据转换

**缺点：**
- ❌ 不包含用户上传的自定义档口数据
- ❌ 需要手动补充图片数据

---

### 策略 B: 从 MySQL 全量迁移（生产环境）
适用于：生产环境、包含用户自定义数据

**流程：**
1. 从 MySQL 导出所有数据
2. 转换数据格式（MySQL → PostgreSQL）
3. 导入到 PostgreSQL
4. 验证数据完整性
5. 配置 RabbitMQ 事件同步

**优点：**
- ✅ 保留所有历史数据
- ✅ 保留用户自定义档口
- ✅ 无数据丢失

**缺点：**
- ❌ 需要数据格式转换
- ❌ 需要停机时间
- ❌ 需要验证外键关系

---

## 迁移步骤

### Step 1: 初始化目标数据库

#### 方法 1: 使用 Docker Compose（推荐）
```bash
cd cafeteria-service

# 启动 PostgreSQL（自动执行初始化脚本）
docker-compose up -d postgres

# 查看日志确认初始化成功
docker-compose logs postgres | grep "Cafeteria Service database initialization completed"
```

#### 方法 2: 手动执行 SQL
```bash
# 创建数据库
createdb -h localhost -U postgres cafeteria_service

# 执行初始化脚本
psql -h localhost -U postgres -d cafeteria_service -f scripts/init_cafeteria_db.sql
```

#### 验证初始数据
```sql
-- 连接到数据库
psql -h localhost -U cafeteria -d cafeteria_service

-- 查看表列表
\dt

-- 查看食堂数量
SELECT COUNT(*) FROM cafeteria;  -- 预期: 8

-- 查看档口数量
SELECT COUNT(*) FROM stall;      -- 预期: 7

-- 查看食堂详情
SELECT id, name, location, seating_capacity, average_rating, review_count 
FROM cafeteria 
ORDER BY id;

-- 查看每个食堂的档口数量
SELECT 
    c.name AS cafeteria_name,
    COUNT(s.id) AS stall_count
FROM cafeteria c
LEFT JOIN stall s ON s.cafeteria_id = c.id
GROUP BY c.id, c.name
ORDER BY c.id;
```

预期输出：
```
 cafeteria_name           | stall_count
--------------------------+-------------
 Fine Food                | 2
 Flavours @ UTown         | 4
 Central Square @ YIH     | 1
 Frontier                 | 0
 PGP Aircon Canteen       | 0
 Techno Edge              | 0
 The Deck                 | 0
 The Terrace              | 0
```

---

### Step 2: 从 MySQL 导出增量数据（可选）

如果需要迁移额外的用户自定义档口：

#### 导出用户自定义档口
```bash
# 导出所有档口数据
mysqldump -h localhost -u root -p nushungry_db stall \
  --no-create-info \
  --complete-insert \
  --where="id > 8" \
  > stall_custom.sql
```

#### 使用 Python 脚本导出
创建 `export_cafeteria_data.py`:

```python
import pymysql
import pandas as pd
import json

# MySQL 连接
mysql_conn = pymysql.connect(
    host='localhost',
    user='root',
    password='your_password',
    database='nushungry_db'
)

# 导出档口数据（排除初始数据）
stall_query = "SELECT * FROM stall WHERE id > 8"
stall_df = pd.read_sql(stall_query, mysql_conn)

# 导出图片数据
image_query = """
SELECT * FROM image 
WHERE entity_type IN ('CAFETERIA', 'STALL')
"""
image_df = pd.read_sql(image_query, mysql_conn)

mysql_conn.close()

# 保存为 CSV
stall_df.to_csv('stall_custom.csv', index=False)
image_df.to_csv('image_data.csv', index=False)

print(f"导出 {len(stall_df)} 个自定义档口")
print(f"导出 {len(image_df)} 张图片记录")
```

---

### Step 3: 转换数据格式

#### MySQL vs PostgreSQL 差异

| 特性 | MySQL | PostgreSQL |
|------|-------|------------|
| 布尔类型 | `tinyint(1)` | `BOOLEAN` |
| 双精度 | `double` | `DOUBLE PRECISION` |
| 自增 ID | `AUTO_INCREMENT` | `BIGSERIAL` |
| 时间格式 | `datetime(6)` | `TIMESTAMP` |
| 小数 | `decimal(3,2)` | `DECIMAL(3,2)` |

#### 转换脚本 `convert_stall_data.py`:

```python
import pandas as pd
from datetime import datetime

# 读取导出的数据
df = pd.read_csv('stall_custom.csv')

# 数据转换
df['created_at'] = pd.to_datetime(df['created_at'], errors='coerce')
df['updated_at'] = pd.to_datetime(df['updated_at'], errors='coerce')

# 处理 NULL 值
df = df.where(pd.notnull(df), None)

# 生成 PostgreSQL INSERT 语句
with open('insert_custom_stalls.sql', 'w', encoding='utf-8') as f:
    for _, row in df.iterrows():
        values = []
        for col in df.columns:
            val = row[col]
            if val is None:
                values.append('NULL')
            elif isinstance(val, (int, float)):
                values.append(str(val))
            else:
                # 转义单引号
                val_escaped = str(val).replace("'", "''")
                values.append(f"'{val_escaped}'")
        
        f.write(f"""
INSERT INTO stall ({', '.join(df.columns)})
VALUES ({', '.join(values)})
ON CONFLICT (id) DO UPDATE SET
    name = EXCLUDED.name,
    updated_at = EXCLUDED.updated_at;
        """)

print("转换完成，生成文件: insert_custom_stalls.sql")
```

---

### Step 4: 导入增量数据到 PostgreSQL

```bash
# 方法 1: 执行生成的 SQL 文件
psql -h localhost -U cafeteria -d cafeteria_service -f insert_custom_stalls.sql

# 方法 2: 使用 Python 直接导入
python import_custom_stalls.py
```

#### Python 导入脚本 `import_custom_stalls.py`:

```python
import psycopg2
import pandas as pd

# PostgreSQL 连接
pg_conn = psycopg2.connect(
    host='localhost',
    port=5433,  # 注意端口映射
    database='cafeteria_service',
    user='cafeteria',
    password='password123'
)
cursor = pg_conn.cursor()

# 读取数据
df = pd.read_csv('stall_custom.csv')

# 批量插入
for _, row in df.iterrows():
    cursor.execute("""
        INSERT INTO stall (
            id, name, cuisine_type, halal_info, contact, image_url,
            latitude, longitude, average_price, average_rating, review_count,
            cafeteria_id, created_at, updated_at
        )
        VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
        ON CONFLICT (id) DO UPDATE SET
            name = EXCLUDED.name,
            updated_at = EXCLUDED.updated_at
    """, tuple(row))

pg_conn.commit()

# 重置序列
cursor.execute("SELECT setval('stall_id_seq', (SELECT MAX(id) FROM stall));")
pg_conn.commit()

cursor.close()
pg_conn.close()

print("增量数据导入完成！")
```

---

### Step 5: 配置 RabbitMQ 事件同步

为了保持数据一致性，配置 RabbitMQ 监听评价更新事件：

#### 事件监听配置

```java
// ReviewEventListener.java
@RabbitListener(queues = "review.rating.updated")
public void handleReviewRatingUpdate(ReviewRatingUpdateEvent event) {
    Long stallId = event.getStallId();
    Double newAverageRating = event.getNewAverageRating();
    Integer newReviewCount = event.getNewReviewCount();
    
    stallService.updateRating(stallId, newAverageRating, newReviewCount);
    log.info("Updated stall {} rating to {}", stallId, newAverageRating);
}
```

#### 测试事件监听
```bash
# 发布测试事件
curl -X POST http://localhost:15673/api/exchanges/%2Fcafeteria/review.events/publish \
  -u cafeteria:password123 \
  -H "Content-Type: application/json" \
  -d '{
    "properties": {},
    "routing_key": "review.rating.updated",
    "payload": "{\"stallId\":1,\"newAverageRating\":4.5,\"newReviewCount\":10}",
    "payload_encoding": "string"
  }'
```

---

## 数据验证

### 验证清单

#### 1. 记录数量验证
```sql
-- PostgreSQL
SELECT COUNT(*) as cafeteria_count FROM cafeteria;  -- 预期: 8
SELECT COUNT(*) as stall_count FROM stall;          -- 预期: 7+
SELECT COUNT(*) as image_count FROM image;          -- 预期: 视迁移的图片数量

-- MySQL (对比)
SELECT COUNT(*) FROM cafeteria;
SELECT COUNT(*) FROM stall;
SELECT COUNT(*) FROM image WHERE entity_type IN ('CAFETERIA', 'STALL');
```

#### 2. 数据完整性验证
```sql
-- 验证外键关系
SELECT 
    s.id AS stall_id,
    s.name AS stall_name,
    c.id AS cafeteria_id,
    c.name AS cafeteria_name
FROM stall s
LEFT JOIN cafeteria c ON s.cafeteria_id = c.id
WHERE s.cafeteria_id IS NOT NULL;

-- 验证孤儿档口（无关联食堂）
SELECT * FROM stall WHERE cafeteria_id IS NULL;

-- 验证图片关联
SELECT 
    entity_type,
    COUNT(*) as count
FROM image
GROUP BY entity_type;
```

#### 3. 地理坐标验证
```sql
-- 验证所有食堂都有有效坐标
SELECT id, name, latitude, longitude
FROM cafeteria
WHERE latitude IS NULL OR longitude IS NULL OR latitude = 0 OR longitude = 0;

-- 验证坐标范围（NUS 大约在 1.29-1.31, 103.77-103.78）
SELECT id, name, latitude, longitude
FROM cafeteria
WHERE latitude NOT BETWEEN 1.28 AND 1.32
   OR longitude NOT BETWEEN 103.76 AND 103.79;
```

#### 4. 评分数据验证
```sql
-- 验证评分范围
SELECT id, name, average_rating, review_count
FROM stall
WHERE average_rating < 0 OR average_rating > 5;

-- 验证评分与评价数量的一致性
SELECT id, name, average_rating, review_count
FROM stall
WHERE (review_count > 0 AND average_rating = 0)
   OR (review_count = 0 AND average_rating > 0);
```

---

## 回滚方案

### 场景 1: 迁移失败

```bash
# 1. 停止服务
docker-compose down cafeteria-service

# 2. 删除 PostgreSQL 数据
docker-compose down postgres
docker volume rm cafeteria-service_postgres_data

# 3. 重新初始化
docker-compose up -d postgres
```

### 场景 2: 数据错误

```sql
-- 清空数据重新导入
TRUNCATE TABLE image CASCADE;
TRUNCATE TABLE stall CASCADE;
TRUNCATE TABLE cafeteria CASCADE;

-- 重新执行初始化脚本
\i scripts/init_cafeteria_db.sql
```

### 场景 3: 部分回滚

```sql
-- 只回滚档口数据
DELETE FROM stall WHERE id > 8;
SELECT setval('stall_id_seq', 8);

-- 只回滚图片数据
DELETE FROM image WHERE created_at > '2025-10-19 00:00:00';
```

---

## 常见问题

### Q1: 部分档口没有关联食堂？
**原因**: `cafeteria_id` 为 NULL

**解决方案**:
```sql
-- 查找孤儿档口
SELECT * FROM stall WHERE cafeteria_id IS NULL;

-- 手动关联到默认食堂
UPDATE stall 
SET cafeteria_id = 2  -- Flavours @ UTown
WHERE cafeteria_id IS NULL;
```

---

### Q2: 坐标数据丢失？
**原因**: 档口继承食堂的坐标

**解决方案**:
```sql
-- 为档口设置食堂的坐标
UPDATE stall s
SET latitude = c.latitude,
    longitude = c.longitude
FROM cafeteria c
WHERE s.cafeteria_id = c.id
  AND (s.latitude IS NULL OR s.longitude IS NULL);
```

---

### Q3: 评分数据不同步？
**原因**: RabbitMQ 事件未正确处理

**解决方案**:
```bash
# 1. 检查 RabbitMQ 连接
curl http://localhost:15673/api/queues/%2Fcafeteria/review.rating.updated \
  -u cafeteria:password123

# 2. 检查死信队列
curl http://localhost:15673/api/queues/%2Fcafeteria/review.rating.updated.dlq \
  -u cafeteria:password123

# 3. 手动触发同步
curl -X POST http://localhost:8083/api/admin/sync-ratings
```

---

### Q4: 图片无法访问？
**原因**: 文件路径迁移问题

**解决方案**:
```sql
-- 更新图片 URL 前缀
UPDATE image
SET file_url = REPLACE(file_url, 'http://localhost:8080', 'http://localhost:8085')
WHERE file_url LIKE 'http://localhost:8080%';

-- 或使用相对路径
UPDATE image
SET file_url = '/media/' || file_name
WHERE file_url IS NOT NULL;
```

---

### Q5: 如何批量更新评分？
**解决方案**:

创建存储过程：
```sql
CREATE OR REPLACE FUNCTION sync_all_ratings()
RETURNS void AS $$
DECLARE
    stall_record RECORD;
BEGIN
    FOR stall_record IN SELECT id FROM stall LOOP
        -- 从 review-service 获取最新评分
        -- 这里需要通过应用层调用 API
        RAISE NOTICE 'Syncing ratings for stall %', stall_record.id;
    END LOOP;
END;
$$ LANGUAGE plpgsql;

-- 执行同步
SELECT sync_all_ratings();
```

---

## 性能优化

### 索引优化
```sql
-- 分析查询性能
EXPLAIN ANALYZE 
SELECT * FROM stall WHERE cafeteria_id = 2;

-- 创建额外的复合索引
CREATE INDEX IF NOT EXISTS idx_stall_cafeteria_rating 
ON stall(cafeteria_id, average_rating DESC);

-- 创建部分索引（仅索引有评分的档口）
CREATE INDEX IF NOT EXISTS idx_stall_with_reviews 
ON stall(average_rating DESC) 
WHERE review_count > 0;
```

### 查询优化
```sql
-- 使用物化视图缓存热门档口
CREATE MATERIALIZED VIEW popular_stalls AS
SELECT 
    s.id,
    s.name,
    s.cuisine_type,
    s.average_rating,
    s.review_count,
    c.name AS cafeteria_name
FROM stall s
JOIN cafeteria c ON s.cafeteria_id = c.id
WHERE s.review_count > 0
ORDER BY s.average_rating DESC, s.review_count DESC
LIMIT 100;

-- 创建索引
CREATE UNIQUE INDEX ON popular_stalls(id);

-- 定期刷新（每小时）
REFRESH MATERIALIZED VIEW CONCURRENTLY popular_stalls;
```

### 清理过期数据
```sql
-- 删除无关联的孤儿图片
DELETE FROM image
WHERE entity_type = 'STALL' 
  AND entity_id NOT IN (SELECT id FROM stall);

DELETE FROM image
WHERE entity_type = 'CAFETERIA' 
  AND entity_id NOT IN (SELECT id FROM cafeteria);
```

---

## 监控与维护

### 定期检查脚本
创建 `health_check.sql`:

```sql
-- 数据健康检查
SELECT 
    'Cafeterias' AS entity,
    COUNT(*) AS count,
    COUNT(*) FILTER (WHERE average_rating > 0) AS rated_count
FROM cafeteria
UNION ALL
SELECT 
    'Stalls',
    COUNT(*),
    COUNT(*) FILTER (WHERE average_rating > 0)
FROM stall
UNION ALL
SELECT 
    'Images',
    COUNT(*),
    COUNT(*) FILTER (WHERE created_at > CURRENT_DATE - INTERVAL '7 days')
FROM image;

-- 孤儿数据检查
SELECT 'Orphan stalls' AS issue, COUNT(*) 
FROM stall WHERE cafeteria_id IS NULL
UNION ALL
SELECT 'Orphan images', COUNT(*) 
FROM image 
WHERE entity_type = 'STALL' 
  AND entity_id NOT IN (SELECT id FROM stall);
```

---

## 联系与支持

如遇到迁移问题，请联系：
- **技术支持**: tech@nushungry.com
- **文档**: [Architecture Documentation](../../docs/ARCHITECTURE.md)
- **API 文档**: http://localhost:8083/swagger-ui.html

---

**最后更新**: 2025-01-19
