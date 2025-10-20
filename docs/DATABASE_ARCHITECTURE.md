# 数据库架构与拆分策略

## 📋 目录

- [架构概览](#架构概览)
- [设计原则](#设计原则)
- [数据库拆分映射](#数据库拆分映射)
- [共享数据处理](#共享数据处理)
- [数据一致性策略](#数据一致性策略)
- [迁移策略](#迁移策略)
- [性能优化](#性能优化)

---

## 架构概览

NUS Hungry 从单体架构 (Monolith) 迁移到微服务架构 (Microservices)，采用 **Database-per-Service** 和 **Polyglot Persistence** 模式，确保服务独立性和技术多样性。

### 原始单体架构

```
┌─────────────────────────────────────┐
│   MySQL: nushungry_db (单一数据库)   │
├─────────────────────────────────────┤
│ • users (用户)                       │
│ • cafeteria (食堂)                   │
│ • stall (档口)                       │
│ • image/images (图片)                │
│ • favorites (收藏)                   │
│ • search_history (搜索历史)          │
│ • review (评价)                      │
│ • review_likes (点赞)                │
│ • review_reports (举报)              │
│ • moderation_log (审核日志)          │
│ • refresh_tokens (刷新令牌)          │
│ • verification_codes (验证码)        │
└─────────────────────────────────────┘
```

### 微服务架构

```
┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐
│ admin-service   │  │cafeteria-service│  │ media-service   │
│ PostgreSQL      │  │ PostgreSQL      │  │ PostgreSQL      │
├─────────────────┤  ├─────────────────┤  ├─────────────────┤
│ • users         │  │ • cafeteria     │  │ • media_files   │
│ • admin_audit   │  │ • stall         │  │ • image_metadata│
│ • dashboard_    │  │ • image         │  │ • upload_session│
│   cache         │  └─────────────────┘  └─────────────────┘
└─────────────────┘

┌─────────────────┐  ┌─────────────────┐
│preference-service│  │ review-service  │
│ PostgreSQL      │  │ MongoDB         │
├─────────────────┤  ├─────────────────┤
│ • favorites     │  │ • reviews       │
│ • search_history│  │ • review_likes  │
└─────────────────┘  │ • review_replies│
                     └─────────────────┘
```

---

## 设计原则

### 1. Database-per-Service Pattern
每个微服务拥有**独立的数据库实例**，确保：
- ✅ **服务自治性**: 服务可独立开发、部署和扩展
- ✅ **技术多样性**: 不同服务可选择最适合的数据库（PostgreSQL、MongoDB）
- ✅ **故障隔离**: 一个数据库故障不影响其他服务
- ❌ **挑战**: 需要处理分布式事务和数据一致性

### 2. Polyglot Persistence（多态持久化）
根据业务特性选择最佳数据库：

| 服务 | 数据库 | 选型理由 |
|------|--------|---------|
| **admin-service** | PostgreSQL | 需要事务支持、复杂查询、审计日志 |
| **cafeteria-service** | PostgreSQL | 关系型数据、地理位置查询、外键约束 |
| **media-service** | PostgreSQL | 结构化元数据、ACID 事务 |
| **preference-service** | PostgreSQL | 用户收藏和搜索历史、唯一约束 |
| **review-service** | MongoDB | 灵活的文档结构、高并发写入、JSON 图片数组 |

### 3. 事件驱动架构（Event-Driven）
通过 **RabbitMQ** 实现服务间异步通信和数据同步：
- ✅ 降低服务耦合度
- ✅ 提高系统可扩展性
- ✅ 保证最终一致性

---

## 数据库拆分映射

### 完整映射表

| 单体表 (MySQL) | 目标服务 | 目标数据库 | 目标表 | 迁移策略 |
|---------------|---------|-----------|--------|---------|
| **users** | admin-service | PostgreSQL | users | **全量迁移** + 事件同步 |
| **cafeteria** | cafeteria-service | PostgreSQL | cafeteria | **全量迁移** + 评分同步 |
| **stall** | cafeteria-service | PostgreSQL | stall | **全量迁移** + 评分同步 |
| **image** | cafeteria-service | PostgreSQL | image | **全量迁移**（关联 cafeteria/stall） |
| **image** | media-service | PostgreSQL | media_files | **选择性迁移**（独立文件） |
| **favorites** | preference-service | PostgreSQL | favorites | **全量迁移** |
| **search_history** | preference-service | PostgreSQL | search_history | **全量迁移** |
| **review** | review-service | MongoDB | reviews | **全量迁移** + 转换为文档 |
| **review_likes** | review-service | MongoDB | review_likes | **全量迁移** + 转换为文档 |
| **review_reports** | review-service | MongoDB | review_reports | **待定**（或保留在单体） |
| **moderation_log** | admin-service | PostgreSQL | admin_audit_logs | **合并迁移** |
| **refresh_tokens** | admin-service | PostgreSQL | refresh_tokens | **保留**（共享认证） |
| **verification_codes** | admin-service | PostgreSQL | verification_codes | **保留**（共享认证） |

---

## 详细迁移方案

### 1. admin-service (PostgreSQL: admin_service)

#### 数据职责
- 用户认证与授权
- 管理员操作审计
- 仪表盘缓存
- 全局用户管理

#### 迁移表
```sql
-- 主表
users (从 MySQL users 全量迁移)
  ├─ id, username, password, email, role, enabled
  ├─ created_at, updated_at, last_login
  └─ avatar_url

-- 新增表
admin_audit_logs (合并 moderation_log)
  ├─ admin_id, action, target_type, target_id
  ├─ description, ip_address, status
  └─ created_at

dashboard_cache (新增)
  ├─ cache_key, cache_value
  └─ expires_at
```

#### 数据来源
- **users**: 从单体 MySQL `users` 表全量迁移
- **admin_audit_logs**: 合并单体 `moderation_log` + 新增管理员操作日志
- **dashboard_cache**: 运行时生成

#### 同步策略
- **写操作**: Admin Service 作为用户数据的主服务 (Master)
- **事件发布**: 用户创建/更新/删除时发布 RabbitMQ 事件
- **其他服务**: 订阅事件更新本地缓存

---

### 2. cafeteria-service (PostgreSQL: cafeteria_service)

#### 数据职责
- 食堂与档口基础信息
- 地理位置数据
- 评分数据（通过事件同步）
- 食堂/档口关联图片

#### 迁移表
```sql
cafeteria (从 MySQL cafeteria 全量迁移)
  ├─ id, name, location, latitude, longitude
  ├─ halal_info, seating_capacity, opening_hours
  └─ average_rating, review_count (事件更新)

stall (从 MySQL stall 全量迁移)
  ├─ id, name, cuisine_type, cafeteria_id
  ├─ latitude, longitude, average_price
  └─ average_rating, review_count (事件更新)

image (从 MySQL image 选择性迁移)
  ├─ entity_type IN ('CAFETERIA', 'STALL')
  └─ file_url, thumbnail_url, entity_id
```

#### 数据来源
- **cafeteria/stall**: 从单体 MySQL 全量迁移（8 个食堂 + 7 个档口）
- **image**: 仅迁移 `entity_type = 'CAFETERIA'` 或 `'STALL'` 的图片
- **评分数据**: 从 Review Service 事件同步

#### 评分同步
```
Review Service (评价创建/更新/删除)
    ↓ RabbitMQ Event
Cafeteria Service (更新 average_rating, review_count)
```

---

### 3. media-service (PostgreSQL: media_service)

#### 数据职责
- 文件上传与管理
- 图片元数据（宽度、高度、格式）
- 多存储支持（本地、MinIO、S3）

#### 迁移表
```sql
media_files (从 MySQL image 选择性迁移)
  ├─ file_name, original_name, file_path, file_url
  ├─ file_size, content_type, storage_type
  └─ uploader_id, entity_type, entity_id

image_metadata (新增)
  ├─ media_file_id, width, height, format
  └─ thumbnail_url, compressed

upload_sessions (新增)
  ├─ session_id, user_id, status
  └─ file_count, total_size
```

#### 数据来源
- **media_files**: 
  - 从单体 MySQL `image` 表迁移独立文件（如用户头像）
  - 新上传的文件直接写入此服务
- **image_metadata**: 重新生成图片元数据（宽度、高度等）

#### 图片迁移策略
1. **关联图片** (`entity_type = 'CAFETERIA'/'STALL'`): 迁移到 Cafeteria Service
2. **评价图片** (`entity_type = 'REVIEW'`): 迁移到 Review Service (MongoDB)
3. **用户头像** (`entity_type = 'USER'` 或独立上传): 迁移到 Media Service

---

### 4. preference-service (PostgreSQL: preference_service)

#### 数据职责
- 用户收藏档口列表
- 用户搜索历史
- 批量操作支持

#### 迁移表
```sql
favorites (从 MySQL favorites 全量迁移)
  ├─ id, user_id, stall_id
  ├─ created_at, sort_order
  └─ UNIQUE (user_id, stall_id)

search_history (从 MySQL search_history 全量迁移)
  ├─ id, user_id, keyword, search_type
  ├─ search_time, result_count
  └─ ip_address
```

#### 数据来源
- **favorites**: 从单体 MySQL `favorites` 表全量迁移（约 11 条记录）
- **search_history**: 从单体 MySQL `search_history` 表全量迁移（约 5 条记录）

#### 数据验证
```sql
-- MySQL 源数据统计
SELECT COUNT(*) FROM favorites;  -- 预期: 11
SELECT COUNT(*) FROM search_history;  -- 预期: 5

-- PostgreSQL 目标数据验证
SELECT COUNT(*) FROM preference_service.favorites;
SELECT COUNT(*) FROM preference_service.search_history;
```

---

### 5. review-service (MongoDB: review_service)

#### 数据职责
- 评价内容与图片
- 评价点赞
- 评价回复
- 审核状态管理

#### 迁移集合
```javascript
// reviews 集合 (从 MySQL review 迁移)
{
  _id: ObjectId,
  reviewId: Long,          // 原 MySQL ID
  stallId: Long,
  stallName: String,       // 从 stall 表获取
  userId: Long,
  username: String,        // 从 users 表获取
  userAvatarUrl: String,
  rating: Double,
  comment: String,
  imageUrls: [String],     // JSON 数组转换
  totalCost: Double,
  numberOfPeople: Int,
  likesCount: Int,
  moderationStatus: String, // PENDING/APPROVED/REJECTED
  createdAt: ISODate,
  updatedAt: ISODate
}

// review_likes 集合 (从 MySQL review_likes 迁移)
{
  _id: ObjectId,
  reviewId: Long,
  userId: Long,
  createdAt: ISODate
}

// review_replies 集合 (新增功能)
{
  _id: ObjectId,
  reviewId: Long,
  userId: Long,
  content: String,
  createdAt: ISODate
}
```

#### 数据来源
- **reviews**: 从单体 MySQL `review` 表迁移（约 11 条评价）
  - 联表查询 `stall` 获取档口名称
  - 联表查询 `users` 获取用户信息
  - `image_urls` JSON 数组转换为 MongoDB 数组
  - 仅迁移 `moderation_status = 'APPROVED'` 的评价
- **review_likes**: 从单体 MySQL `review_likes` 表迁移（约 8 条点赞）

#### MySQL 到 MongoDB 转换
```python
# Python 迁移脚本示例
import pymysql
import pymongo

mysql_conn = pymysql.connect(host='localhost', user='root', password='***', database='nushungry_db')
mongo_client = pymongo.MongoClient('mongodb://localhost:27017/')
mongo_db = mongo_client['review_service']

cursor = mysql_conn.cursor(pymysql.cursors.DictCursor)
cursor.execute("""
    SELECT r.*, s.name as stall_name, u.username, u.avatar_url
    FROM review r
    LEFT JOIN stall s ON r.stall_id = s.id
    LEFT JOIN users u ON r.user_id = u.id
    WHERE r.moderation_status = 'APPROVED'
""")

for row in cursor:
    document = {
        'reviewId': row['id'],
        'stallId': row['stall_id'],
        'stallName': row['stall_name'],
        'userId': row['user_id'],
        'username': row['username'],
        'userAvatarUrl': row['avatar_url'],
        'rating': row['rating'],
        'comment': row['comment'],
        'imageUrls': json.loads(row['image_urls']) if row['image_urls'] else [],
        'likesCount': row['likes_count'],
        'createdAt': row['created_at'],
        'updatedAt': row['updated_at']
    }
    mongo_db.reviews.insert_one(document)
```

---

## 共享数据处理

### 1. 用户数据共享

#### 问题
多个服务需要用户信息（用户名、头像等）用于显示。

#### 解决方案：事件驱动的数据同步
```
Admin Service (用户主服务)
    ↓ 用户创建/更新
RabbitMQ Event (UserCreated / UserUpdated)
    ↓ 订阅
[Review Service, Cafeteria Service, Preference Service]
    ↓ 更新本地缓存/嵌入数据
```

#### 实现细节
- **Admin Service**: 作为用户数据的权威来源 (Source of Truth)
- **其他服务**: 维护用户数据的**只读副本**或**嵌入式数据**
- **事件格式**:
  ```json
  {
    "eventType": "UserUpdated",
    "userId": 123,
    "username": "admin",
    "avatarUrl": "/uploads/avatars/xxx.jpg",
    "timestamp": "2025-01-19T10:00:00Z"
  }
  ```

### 2. 评分数据同步

#### 问题
Cafeteria Service 需要显示档口的平均评分和评价数量，但评价数据在 Review Service。

#### 解决方案：评价事件聚合
```
Review Service (评价创建/删除)
    ↓ RabbitMQ Event
Cafeteria Service (监听评价事件)
    ↓ 计算新的平均评分
UPDATE stall SET average_rating = ?, review_count = ?
```

#### 事件类型
- `ReviewCreated`: 新增评价 → `review_count++`, 重新计算 `average_rating`
- `ReviewUpdated`: 更新评分 → 重新计算 `average_rating`
- `ReviewDeleted`: 删除评价 → `review_count--`, 重新计算 `average_rating`

---

## 数据一致性策略

### 1. 强一致性 vs 最终一致性

| 场景 | 一致性模型 | 实现方式 |
|-----|----------|---------|
| 用户认证 | **强一致性** | Admin Service 单一数据源 |
| 评分同步 | **最终一致性** | RabbitMQ 事件 + 重试机制 |
| 收藏列表 | **强一致性** | Preference Service 单一数据源 |
| 搜索历史 | **最终一致性** | 异步写入，允许短暂延迟 |

### 2. 分布式事务处理

#### Saga 模式 (未实现，待优化)
对于跨服务的复杂操作（如删除用户时级联删除相关数据），采用 **Saga 模式**：
```
1. Admin Service: 删除用户
   ↓ 发布 UserDeleted 事件
2. Review Service: 删除该用户的所有评价
   ↓ 发布 ReviewsDeleted 事件
3. Preference Service: 删除收藏和搜索历史
   ↓ 完成
```

如果任何步骤失败，执行**补偿事务**回滚。

### 3. 幂等性保证

所有事件处理器必须实现**幂等性**，防止重复消费：
```java
@RabbitListener(queues = "user.updated")
public void handleUserUpdated(UserUpdatedEvent event) {
    // 检查事件是否已处理
    if (processedEventRepository.existsByEventId(event.getEventId())) {
        log.info("Event {} already processed, skipping", event.getEventId());
        return;
    }
    
    // 处理事件
    userCacheService.updateUser(event.getUserId(), event);
    
    // 记录已处理
    processedEventRepository.save(new ProcessedEvent(event.getEventId()));
}
```

---

## 迁移策略

### 阶段 1: 准备阶段
- [x] 创建微服务数据库（PostgreSQL × 4, MongoDB × 1）
- [x] 编写数据库初始化脚本（`init_*.sql`）
- [x] 创建数据迁移脚本（`migrate_reviews_to_mongodb.py`）
- [ ] 数据备份（单体数据库全量备份）

### 阶段 2: 增量迁移（推荐）
**策略**: 逐步迁移，保持单体系统运行，最小化风险。

```
Week 1: Preference Service
  ├─ 迁移 favorites, search_history
  ├─ 双写（单体 + 微服务）
  └─ 验证数据一致性

Week 2: Cafeteria Service
  ├─ 迁移 cafeteria, stall, image
  ├─ 切换读流量到微服务
  └─ 保持单体写入

Week 3: Review Service
  ├─ 迁移 review, review_likes
  ├─ 配置事件发布
  └─ 双写验证

Week 4: Admin & Media Service
  ├─ 迁移 users, media_files
  ├─ 切换认证到 Admin Service
  └─ 全面切换到微服务
```

### 阶段 3: 全量迁移（快速部署）
**策略**: 一次性迁移所有数据，适用于测试环境或用户量少的场景。

```bash
# 1. 停止单体应用
docker stop nushungry-backend

# 2. 数据库备份
mysqldump -u root -p nushungry_db > backup_$(date +%F).sql

# 3. 执行迁移脚本
./scripts/migrate_monolith_to_microservices.sh

# 4. 验证数据
./scripts/validate_migration.sh

# 5. 启动微服务
docker-compose up -d
```

### 阶段 4: 验证与回滚
```bash
# 数据验证脚本
#!/bin/bash

echo "验证 Admin Service..."
psql -h localhost -U admin -d admin_service -c "SELECT COUNT(*) FROM users;"

echo "验证 Cafeteria Service..."
psql -h localhost -U cafeteria -d cafeteria_service -c "SELECT COUNT(*) FROM cafeteria;"
psql -h localhost -U cafeteria -d cafeteria_service -c "SELECT COUNT(*) FROM stall;"

echo "验证 Review Service..."
mongo --eval "db.reviews.count()" review_service

echo "验证 Preference Service..."
psql -h localhost -U preference -d preference_service -c "SELECT COUNT(*) FROM favorites;"
```

### 回滚方案
如果迁移失败：
```bash
# 1. 停止微服务
docker-compose down

# 2. 恢复单体数据库
mysql -u root -p nushungry_db < backup_2025-01-19.sql

# 3. 重启单体应用
docker start nushungry-backend
```

---

## 性能优化

### 1. 数据库索引策略

#### Admin Service
```sql
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_audit_admin_id ON admin_audit_logs(admin_id);
CREATE INDEX idx_audit_created_at ON admin_audit_logs(created_at DESC);
```

#### Cafeteria Service
```sql
CREATE INDEX idx_cafeteria_location ON cafeteria(latitude, longitude);
CREATE INDEX idx_stall_cafeteria ON stall(cafeteria_id);
CREATE INDEX idx_stall_rating ON stall(average_rating DESC);
```

#### Review Service (MongoDB)
```javascript
db.reviews.createIndex({ stallId: 1, createdAt: -1 });
db.reviews.createIndex({ userId: 1 });
db.review_likes.createIndex({ reviewId: 1, userId: 1 }, { unique: true });
```

#### Preference Service
```sql
CREATE INDEX idx_favorites_user ON favorites(user_id);
CREATE INDEX idx_favorites_stall ON favorites(stall_id);
CREATE INDEX idx_search_history_user ON search_history(user_id);
CREATE INDEX idx_search_history_time ON search_history(search_time DESC);
```

### 2. 缓存策略（未实现，待优化）

推荐引入 **Redis** 缓存热点数据：
- **用户信息缓存** (Admin Service): TTL = 1 小时
- **档口列表缓存** (Cafeteria Service): TTL = 5 分钟
- **评价列表缓存** (Review Service): TTL = 1 分钟
- **收藏列表缓存** (Preference Service): TTL = 5 分钟

### 3. 数据库连接池

所有服务使用 **HikariCP** 连接池：
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 10
      minimum-idle: 2
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
```

### 4. 批量操作优化

- **批量插入**: 使用 `COPY` 命令（PostgreSQL）或 `insertMany`（MongoDB）
- **批量更新**: 合并多个更新操作为单个事务
- **分页查询**: 统一使用 `LIMIT` + `OFFSET`（PostgreSQL）或 `skip` + `limit`（MongoDB）

---

## 监控与维护

### 1. 数据一致性监控
定期运行脚本验证数据一致性：
```bash
# 检查评分数据是否同步
./scripts/validate_rating_sync.sh
```

### 2. 事件队列监控
监控 RabbitMQ 队列堆积：
```bash
# 查看队列消息数量
rabbitmqadmin list queues name messages
```

### 3. 数据库健康检查
```sql
-- PostgreSQL
SELECT pg_database_size('admin_service') AS size;

-- MongoDB
db.stats()
```

---

## 未来优化方向

### 1. 引入 API Gateway
统一路由所有微服务请求，简化客户端调用。

### 2. 服务注册与发现
使用 **Spring Cloud Netflix Eureka** 实现动态服务发现。

### 3. 分布式追踪
集成 **Spring Cloud Sleuth + Zipkin** 追踪跨服务调用链。

### 4. 配置中心
使用 **Spring Cloud Config** 集中管理配置。

### 5. 读写分离
为高并发服务（如 Review Service）配置主从复制。

---

## 参考资料

- [Database per Service Pattern](https://microservices.io/patterns/data/database-per-service.html)
- [Event-Driven Architecture](https://microservices.io/patterns/data/event-driven-architecture.html)
- [Saga Pattern](https://microservices.io/patterns/data/saga.html)
- [PostgreSQL Best Practices](https://wiki.postgresql.org/wiki/Performance_Optimization)
- [MongoDB Schema Design](https://www.mongodb.com/docs/manual/core/data-model-design/)

---

## 联系与支持

如有问题或建议，请联系架构团队：
- 📧 Email: team@nushungry.com
- 📚 文档: [docs/ARCHITECTURE.md](./ARCHITECTURE.md)
- 🔧 迁移脚本: [scripts/migrate_monolith_to_microservices.sh](../scripts/migrate_monolith_to_microservices.sh)
