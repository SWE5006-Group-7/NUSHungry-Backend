# 数据迁移指南

## 概述

本目录包含将评价数据从 MySQL 迁移到 MongoDB 的脚本和工具。

## 前置条件

### 1. 安装 Python 依赖

```bash
pip install pymysql pymongo
```

### 2. 确保数据库服务运行

**MySQL**:
- 确保 MySQL 服务运行在 `localhost:3306`
- 数据库名称: `nushungry_db`
- 用户名/密码: `root/123456` (默认，可在脚本中修改)

**MongoDB**:
- 确保 MongoDB 服务运行在 `localhost:27017`
- 目标数据库: `review_service`

启动 MongoDB (Windows):
```bash
# 方式1: 使用 Docker
docker run -d --name mongodb -p 27017:27017 mongo:7.0

# 方式2: 使用本地安装
mongod --dbpath C:\data\db
```

## 迁移步骤

### 步骤1: 测试运行（Dry Run）

在实际迁移前，建议先进行测试运行，查看迁移数据的预览：

```bash
python migrate_reviews_to_mongodb.py --dry-run
```

输出示例：
```
==============================================================
评价服务数据迁移工具 (MySQL → MongoDB)
==============================================================
⚠ 模拟运行模式（Dry Run）
✓ 成功连接到 MySQL: localhost:3306
✓ 成功连接到 MongoDB: mongodb://localhost:27017/

[1/3] 迁移评价数据...
✓ 从 MySQL 查询到 50 条评价记录
  [Dry Run] 将插入 50 条评价记录

[2/3] 迁移点赞数据...
✓ 从 MySQL 查询到 120 条点赞记录
  [Dry Run] 将插入 120 条点赞记录

[3/3] 迁移举报数据...
✓ 从 MySQL 查询到 5 条举报记录
  [Dry Run] 将插入 5 条举报记录

[Dry Run] 将创建 MongoDB 索引
==============================================================
✓ 数据迁移完成
==============================================================
```

### 步骤2: 执行迁移

确认测试运行无误后，执行实际迁移：

```bash
# 首次迁移（清空目标集合）
python migrate_reviews_to_mongodb.py --clean

# 增量迁移（不清空，跳过重复数据）
python migrate_reviews_to_mongodb.py
```

### 步骤3: 验证数据

脚本会自动验证数据迁移结果，对比 MySQL 和 MongoDB 的数据数量。

您也可以手动验证：

**使用 MongoDB Compass** (图形化工具):
1. 连接到 `mongodb://localhost:27017`
2. 打开数据库 `review_service`
3. 查看集合 `review`, `reviewLike`, `reviewReport`

**使用 mongosh** (命令行):
```bash
mongosh mongodb://localhost:27017/review_service

# 查询评价数量
db.review.countDocuments()

# 查看最新评价
db.review.find().sort({createdAt: -1}).limit(5)

# 查询某个摊位的评价
db.review.find({stallId: 1}).pretty()

# 查看点赞数量
db.reviewLike.countDocuments()

# 查看举报数量
db.reviewReport.countDocuments()

# 查看索引
db.review.getIndexes()
```

## 迁移选项

### 命令行参数

| 参数 | 说明 | 示例 |
|------|------|------|
| `--dry-run` | 模拟运行，不实际插入数据 | `python migrate_reviews_to_mongodb.py --dry-run` |
| `--batch-size` | 批量插入大小（默认100） | `python migrate_reviews_to_mongodb.py --batch-size 50` |
| `--clean` | 迁移前清空目标集合 | `python migrate_reviews_to_mongodb.py --clean` |

### 配置数据库连接

如果您的数据库配置与默认不同，请编辑脚本中的配置：

```python
# MySQL 配置
MYSQL_CONFIG = {
    'host': 'localhost',        # MySQL 主机
    'port': 3306,              # MySQL 端口
    'user': 'root',            # 用户名
    'password': '123456',      # 密码
    'database': 'nushungry_db', # 数据库名
    'charset': 'utf8mb4'
}

# MongoDB 配置
MONGODB_CONFIG = {
    'uri': 'mongodb://localhost:27017/',  # MongoDB URI
    'database': 'review_service'          # 目标数据库
}
```

## 数据转换说明

### Review 表映射

| MySQL 字段 | MongoDB 字段 | 说明 |
|-----------|-------------|------|
| id | _id | 使用 MySQL ID 作为 MongoDB _id |
| stall_id | stallId | 摊位ID |
| user_id | userId | 用户ID |
| rating | rating | 评分 (double) |
| comment | comment | 评价内容 |
| image_urls (JSON) | imageUrls (Array) | 图片URL数组 |
| total_cost | totalCost | 总花费 |
| number_of_people | numberOfPeople | 用餐人数 |
| likes_count | likesCount | 点赞数 |
| created_at | createdAt | 创建时间 |
| updated_at | updatedAt | 更新时间 |

### ReviewLike 表映射

| MySQL 字段 | MongoDB 字段 |
|-----------|-------------|
| id | _id |
| review_id | reviewId |
| user_id | userId |
| created_at | createdAt |

### ReviewReport 表映射

| MySQL 字段 | MongoDB 字段 |
|-----------|-------------|
| id | _id |
| review_id | reviewId |
| reporter_id | reporterId |
| reason | reason (枚举) |
| description | description |
| status | status (枚举) |
| moderator_id | handledBy |
| moderated_at | handledAt |
| created_at | createdAt |

## MongoDB 索引说明

迁移完成后，脚本会自动创建以下索引：

### Review 集合索引

```javascript
db.review.createIndex({ "stallId": 1, "createdAt": -1 })  // 按摊位查询+按时间排序
db.review.createIndex({ "userId": 1, "createdAt": -1 })   // 按用户查询+按时间排序
db.review.createIndex({ "stallId": 1, "likesCount": -1 }) // 按摊位查询+按点赞数排序
db.review.createIndex({ "rating": 1 })                    // 按评分过滤
```

### ReviewLike 集合索引

```javascript
db.reviewLike.createIndex({ "reviewId": 1, "userId": 1 }, { unique: true })  // 唯一约束
db.reviewLike.createIndex({ "reviewId": 1 })                                 // 按评论查询
```

### ReviewReport 集合索引

```javascript
db.reviewReport.createIndex({ "reviewId": 1 })    // 按评论查询
db.reviewReport.createIndex({ "status": 1 })      // 按状态过滤
db.reviewReport.createIndex({ "reporterId": 1 })  // 按举报人查询
```

## 常见问题

### 1. 连接失败

**问题**: `✗ MySQL 连接失败` 或 `✗ MongoDB 连接失败`

**解决**:
- 检查数据库服务是否运行
- 检查端口是否正确
- 检查用户名密码是否正确
- 检查防火墙设置

### 2. 重复键错误

**问题**: `BulkWriteError: E11000 duplicate key error`

**解决**:
- 这是正常现象，脚本会自动跳过重复数据
- 如果想完全重新迁移，使用 `--clean` 选项

### 3. 数据数量不匹配

**问题**: 验证时发现数据数量不一致

**可能原因**:
- MySQL 中存在未审核的评价（脚本只迁移 `APPROVED` 状态）
- 部分数据插入失败（检查错误日志）

**解决**:
- 检查脚本输出的错误信息
- 手动对比数据

### 4. 图片URL格式问题

**问题**: 图片 URL 显示不正确

**说明**:
- 脚本会自动转换 MySQL 的 JSON 格式到 MongoDB 的数组格式
- 如果仍有问题，检查原始数据格式

## 回滚方案

如果迁移出现问题，可以通过以下方式回滚：

### 方式1: 删除 MongoDB 数据

```bash
mongosh mongodb://localhost:27017/review_service

db.review.drop()
db.reviewLike.drop()
db.reviewReport.drop()
```

### 方式2: 删除数据库

```bash
mongosh mongodb://localhost:27017

use review_service
db.dropDatabase()
```

## 性能优化

### 大数据量迁移

如果数据量很大（>10万条），建议：

1. 增加批量插入大小：
```bash
python migrate_reviews_to_mongodb.py --batch-size 500
```

2. 关闭 MongoDB 日志：
```javascript
// 临时关闭日志
db.setProfilingLevel(0)
```

3. 迁移后再创建索引（修改脚本，注释掉索引创建部分）

## 后续步骤

数据迁移完成后：

1. ✅ 启动 review-service 服务，测试 API
2. ✅ 运行单元测试和集成测试
3. ✅ 配置 RabbitMQ 消息队列
4. ✅ 更新 cafeteria-service，监听评分变更事件
5. ✅ 逐步切换前端调用到新的微服务端点

## 技术支持

如有问题，请联系开发团队或查看项目文档：
- `review-service/README.md` - 服务文档
- `PROGRESS.md` - 项目进度
- `AGENTS.md` - 开发规范
