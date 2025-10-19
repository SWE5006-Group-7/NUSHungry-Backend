# Admin Service 数据库迁移指南

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

本指南说明如何将 Admin Service 相关数据从单体 MySQL 数据库迁移到独立的 PostgreSQL 数据库。

### 涉及的表
- **users** - 用户表（管理员账号）
- **admin_audit_logs** - 审计日志（新表）
- **dashboard_cache** - 仪表盘缓存（新表）

### 迁移类型
- ✅ **全量迁移** - 迁移所有用户数据
- ✅ **增量迁移** - 仅迁移管理员账号
- ✅ **双写模式** - 新老系统并行运行

---

## 前置条件

### 软件要求
- PostgreSQL 16+
- MySQL 8.0+ (源数据库)
- Python 3.9+ (用于迁移脚本)
- Docker & Docker Compose (推荐)

### Python 依赖
```bash
pip install psycopg2-binary pymysql pandas
```

### 权限要求
- MySQL: 拥有 `SELECT` 权限
- PostgreSQL: 拥有 `CREATE, INSERT, UPDATE, DELETE` 权限

---

## 迁移策略

### 策略 A: 全新部署（推荐）
适用于：开发环境、测试环境

**流程：**
1. 初始化 PostgreSQL 数据库
2. 运行 `init_admin_db.sql` 脚本
3. 使用默认管理员账号登录
4. 通过应用创建其他管理员

**优点：**
- ✅ 简单快速
- ✅ 数据干净
- ✅ 无依赖

**缺点：**
- ❌ 不保留历史数据

---

### 策略 B: 数据迁移（生产环境）
适用于：生产环境、需保留历史数据

**流程：**
1. 从 MySQL 导出管理员数据
2. 转换为 PostgreSQL 格式
3. 导入到 PostgreSQL
4. 验证数据完整性

**优点：**
- ✅ 保留历史数据
- ✅ 用户无需重新注册

**缺点：**
- ❌ 需要停机时间
- ❌ 数据格式转换复杂

---

## 迁移步骤

### Step 1: 初始化目标数据库

#### 使用 Docker Compose（推荐）
```bash
cd admin-service
docker-compose up -d postgres
```

等待 PostgreSQL 启动并自动执行 `init_admin_db.sql`。

#### 手动执行 SQL
```bash
psql -h localhost -U admin -d admin_service -f scripts/init_admin_db.sql
```

#### 验证表结构
```sql
-- 连接到数据库
psql -h localhost -U admin -d admin_service

-- 查看表列表
\dt

-- 查看 users 表结构
\d users

-- 查看初始管理员
SELECT id, username, email, role, enabled FROM users;
```

预期输出：
```
 id | username   | email                   | role       | enabled
----+------------+-------------------------+------------+---------
  1 | admin      | admin@nushungry.com     | ROLE_ADMIN | t
  2 | superadmin | superadmin@nushungry.com| ROLE_ADMIN | t
```

---

### Step 2: 从 MySQL 导出数据（可选）

#### 方法 1: 使用 mysqldump
```bash
# 导出管理员用户数据
mysqldump -h localhost -u root -p nushungry_db users \
  --where="role='ROLE_ADMIN'" \
  --no-create-info \
  --complete-insert \
  > users_admin_only.sql
```

#### 方法 2: 使用 Python 脚本
创建 `export_admin_users.py`:

```python
import pymysql
import psycopg2
from psycopg2.extras import execute_values
import pandas as pd

# MySQL 连接
mysql_conn = pymysql.connect(
    host='localhost',
    user='root',
    password='your_password',
    database='nushungry_db'
)

# 导出管理员用户
query = "SELECT * FROM users WHERE role = 'ROLE_ADMIN'"
df = pd.read_sql(query, mysql_conn)
mysql_conn.close()

print(f"导出 {len(df)} 个管理员账号")
df.to_csv('admin_users.csv', index=False)
```

---

### Step 3: 转换数据格式

#### MySQL vs PostgreSQL 差异

| 特性 | MySQL | PostgreSQL |
|------|-------|------------|
| 布尔类型 | `tinyint(1)` | `BOOLEAN` |
| 枚举类型 | `ENUM('A','B')` | `VARCHAR + CHECK` |
| 自增 ID | `AUTO_INCREMENT` | `BIGSERIAL` |
| 时间格式 | `datetime(6)` | `TIMESTAMP` |

#### 转换脚本 `convert_data.py`:

```python
import pandas as pd

# 读取导出的数据
df = pd.read_csv('admin_users.csv')

# 数据转换
df['enabled'] = df['enabled'].apply(lambda x: 't' if x == 1 else 'f')
df['created_at'] = pd.to_datetime(df['created_at'])
df['updated_at'] = pd.to_datetime(df['updated_at'])

# 生成 PostgreSQL INSERT 语句
with open('insert_admin_users.sql', 'w') as f:
    for _, row in df.iterrows():
        f.write(f"""
INSERT INTO users (id, username, password, email, created_at, updated_at, enabled, role, avatar_url, last_login)
VALUES ({row['id']}, '{row['username']}', '{row['password']}', '{row['email']}', 
        '{row['created_at']}', '{row['updated_at']}', {row['enabled']}, '{row['role']}', 
        {f"'{row['avatar_url']}'" if pd.notna(row['avatar_url']) else 'NULL'}, 
        {f"'{row['last_login']}'" if pd.notna(row['last_login']) else 'NULL'})
ON CONFLICT (username) DO NOTHING;
        """)

print("转换完成，生成文件: insert_admin_users.sql")
```

---

### Step 4: 导入数据到 PostgreSQL

```bash
# 方法 1: 执行生成的 SQL 文件
psql -h localhost -U admin -d admin_service -f insert_admin_users.sql

# 方法 2: 直接使用 Python 导入
python import_to_postgres.py
```

#### Python 导入脚本 `import_to_postgres.py`:

```python
import psycopg2
import pandas as pd

# PostgreSQL 连接
pg_conn = psycopg2.connect(
    host='localhost',
    port=5432,
    database='admin_service',
    user='admin',
    password='password123'
)
cursor = pg_conn.cursor()

# 读取转换后的数据
df = pd.read_csv('admin_users.csv')

# 批量插入
for _, row in df.iterrows():
    cursor.execute("""
        INSERT INTO users (id, username, password, email, created_at, updated_at, enabled, role, avatar_url, last_login)
        VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
        ON CONFLICT (username) DO NOTHING
    """, (row['id'], row['username'], row['password'], row['email'],
          row['created_at'], row['updated_at'], row['enabled'], row['role'],
          row['avatar_url'], row['last_login']))

pg_conn.commit()
cursor.close()
pg_conn.close()

print("数据导入完成！")
```

---

## 数据验证

### 验证清单

#### 1. 记录数量验证
```sql
-- PostgreSQL
SELECT COUNT(*) as pg_count FROM users WHERE role = 'ROLE_ADMIN';

-- MySQL (对比)
SELECT COUNT(*) as mysql_count FROM users WHERE role = 'ROLE_ADMIN';
```

#### 2. 数据一致性验证
```sql
-- 验证关键字段
SELECT 
    COUNT(*) as total,
    COUNT(DISTINCT username) as unique_usernames,
    COUNT(DISTINCT email) as unique_emails
FROM users;

-- 验证角色分布
SELECT role, COUNT(*) FROM users GROUP BY role;

-- 验证启用状态
SELECT enabled, COUNT(*) FROM users GROUP BY enabled;
```

#### 3. 密码验证
```bash
# 尝试使用迁移的账号登录
curl -X POST http://localhost:8082/api/admin/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"Admin123!"}'
```

#### 4. 审计日志验证
```sql
-- 验证审计日志表
SELECT COUNT(*) FROM admin_audit_logs;

-- 验证缓存表
SELECT COUNT(*) FROM dashboard_cache;
```

---

## 回滚方案

### 场景 1: 迁移失败

```bash
# 1. 停止 admin-service
docker-compose down admin-service

# 2. 删除 PostgreSQL 数据
docker-compose down postgres
docker volume rm admin-service_postgres_data

# 3. 重新初始化
docker-compose up -d postgres
```

### 场景 2: 数据错误

```sql
-- 清空数据重新导入
TRUNCATE TABLE users RESTART IDENTITY CASCADE;
TRUNCATE TABLE admin_audit_logs RESTART IDENTITY CASCADE;
TRUNCATE TABLE dashboard_cache RESTART IDENTITY CASCADE;
```

### 场景 3: 回退到 MySQL

```bash
# 继续使用单体架构
# 无需操作，保持 MySQL 数据库不变
```

---

## 常见问题

### Q1: 密码无法登录？
**原因**: BCrypt 密码格式不兼容

**解决方案**:
```sql
-- 重置密码（密码: Admin123!）
UPDATE users 
SET password = '$2a$10$XqYnJzYy3ZxR3xC8YvGxXeY8qJh2N5VqQvL3kJhFxYvGxXeY8qJh2N'
WHERE username = 'admin';
```

---

### Q2: 自增 ID 冲突？
**原因**: 序列未正确设置

**解决方案**:
```sql
-- 重置序列
SELECT setval('users_id_seq', (SELECT MAX(id) FROM users));
SELECT setval('admin_audit_logs_id_seq', (SELECT MAX(id) FROM admin_audit_logs));
SELECT setval('dashboard_cache_id_seq', (SELECT MAX(id) FROM dashboard_cache));
```

---

### Q3: 时间格式错误？
**原因**: 时区问题

**解决方案**:
```sql
-- 设置时区
SET TIME ZONE 'Asia/Singapore';

-- 转换时间格式
ALTER TABLE users ALTER COLUMN created_at TYPE TIMESTAMP USING created_at::TIMESTAMP;
```

---

### Q4: 如何定期同步数据？
**解决方案**: 使用 RabbitMQ 事件同步

1. 单体架构发布用户变更事件
2. Admin Service 监听事件并更新本地数据
3. 确保最终一致性

---

## 性能优化

### 索引优化
```sql
-- 分析查询计划
EXPLAIN ANALYZE SELECT * FROM users WHERE username = 'admin';

-- 创建额外索引
CREATE INDEX IF NOT EXISTS idx_users_last_login ON users(last_login DESC);
```

### 缓存优化
```sql
-- 清理过期缓存
SELECT clean_expired_cache();

-- 查看缓存命中率
SELECT 
    cache_key,
    (EXTRACT(EPOCH FROM (expires_at - created_at))) as ttl_seconds
FROM dashboard_cache
ORDER BY created_at DESC;
```

---

## 联系与支持

如遇到迁移问题，请联系：
- **技术支持**: tech@nushungry.com
- **文档**: [Architecture Documentation](../../docs/ARCHITECTURE.md)

---

**最后更新**: 2025-10-19
