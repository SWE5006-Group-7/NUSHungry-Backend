[根目录](../CLAUDE.md) > **review-service**

---

# Review Service - 评价服务

## 变更记录 (Changelog)

### 2025-11-13 10:00:07 UTC
- 初始化模块文档
- 记录评价系统功能与数据模型
- 整理 MongoDB 存储与 RabbitMQ 事件发布机制

---

## 模块职责

Review Service 负责 NUSHungry 平台的评价与互动功能：
- **评价管理**: 用户对档口的评价（评分、评论、图片）增删改查
- **点赞功能**: 用户对评价的点赞/取消点赞
- **举报管理**: 用户举报不当评价，管理员审核处理
- **评分计算**: 自动计算档口平均评分和评价数量
- **价格统计**: 基于评价中的消费金额计算档口平均价格
- **事件发布**: 通过 RabbitMQ 向其他服务发布评分/价格变更事件
- **管理员审核**: 管理员查看举报、处理举报、删除不当评价

---

## 入口与启动

### 主启动类
- **路径**: `src/main/java/com/nushungry/reviewservice/ReviewServiceApplication.java`
- **端口**: 8084 (默认)
- **框架**: Spring Boot 3.2.3 + Spring Data MongoDB + RabbitMQ

### 启动方式
```bash
# 开发模式
mvn spring-boot:run

# 生产模式
mvn clean package -DskipTests
java -jar target/review-service-0.0.1-SNAPSHOT.jar

# Docker 启动
docker build -t review-service .
docker run -p 8084:8084 review-service
```

### 健康检查
```bash
curl http://localhost:8084/actuator/health
```

---

## 对外接口

### 评价管理接口 (ReviewController)

| 方法 | 路径 | 说明 | 认证要求 |
|-----|------|-----|---------|
| POST | `/api/reviews` | 创建评价 | 需登录 |
| PUT | `/api/reviews/{id}` | 更新评价 (仅自己的) | 需登录 |
| DELETE | `/api/reviews/{id}` | 删除评价 (仅自己的) | 需登录 |
| GET | `/api/reviews/{id}` | 获取评价详情 | 无 |
| GET | `/api/reviews/stall/{stallId}` | 获取档口评价列表 | 无 |
| GET | `/api/reviews/user/{userId}` | 获取用户评价列表 | 无 |
| GET | `/api/reviews/stall/{stallId}/rating-distribution` | 获取评分分布统计 | 无 |

**创建评价请求示例:**
```json
POST /api/reviews
Headers:
  X-User-Id: 123
  X-Username: john_doe
  X-User-Avatar: http://example.com/avatar.jpg

Body:
{
  "stallId": 456,
  "stallName": "Chicken Rice Stall",
  "rating": 5,
  "comment": "Delicious chicken rice!",
  "imageUrls": ["http://example.com/photo1.jpg"],
  "totalCost": 5.0,
  "numberOfPeople": 1
}
```

**评价响应示例:**
```json
{
  "id": "507f1f77bcf86cd799439011",
  "stallId": 456,
  "stallName": "Chicken Rice Stall",
  "userId": "123",
  "username": "john_doe",
  "userAvatarUrl": "http://example.com/avatar.jpg",
  "rating": 5,
  "comment": "Delicious chicken rice!",
  "imageUrls": ["http://example.com/photo1.jpg"],
  "totalCost": 5.0,
  "numberOfPeople": 1,
  "likesCount": 0,
  "createdAt": "2025-11-13T10:00:00",
  "updatedAt": "2025-11-13T10:00:00"
}
```

### 点赞管理接口 (ReviewLikeController)

| 方法 | 路径 | 说明 | 认证要求 |
|-----|------|-----|---------|
| POST | `/api/reviews/{id}/like` | 切换点赞状态 (已赞取消，未赞添加) | 需登录 |
| GET | `/api/reviews/{id}/is-liked` | 检查当前用户是否已点赞 | 需登录 |
| GET | `/api/reviews/{id}/like-count` | 获取点赞数 | 无 |

### 举报管理接口 (ReviewReportController)

| 方法 | 路径 | 说明 | 认证要求 |
|-----|------|-----|---------|
| POST | `/api/reviews/{id}/report` | 举报评价 | 需登录 |
| GET | `/api/reviews/{id}/reports` | 获取评价的举报记录 | 管理员 |

**举报请求示例:**
```json
POST /api/reviews/{id}/report
Headers:
  X-User-Id: 789
  X-Username: reporter_user

Body:
{
  "reason": "SPAM",  // SPAM | OFFENSIVE | FAKE | OTHER
  "description": "This review contains spam content."
}
```

### 管理员接口 (AdminReviewController, AdminReportController)

| 方法 | 路径 | 说明 | 认证要求 |
|-----|------|-----|---------|
| GET | `/api/admin/reviews` | 查询所有评价 (分页) | 管理员 |
| DELETE | `/api/admin/reviews/{id}` | 删除评价 | 管理员 |
| GET | `/api/admin/reports` | 查询所有举报 (分页) | 管理员 |
| GET | `/api/admin/reports/status/{status}` | 按状态查询举报 | 管理员 |
| PUT | `/api/admin/reports/{id}/handle` | 处理举报 | 管理员 |

**处理举报请求示例:**
```json
PUT /api/admin/reports/{id}/handle
Headers:
  X-User-Id: 999
  X-Username: admin_user

Body:
{
  "status": "APPROVED",  // APPROVED | REJECTED | IGNORED
  "handleNote": "Confirmed spam, review deleted."
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

<!-- MongoDB -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-mongodb</artifactId>
</dependency>

<!-- RabbitMQ -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-amqp</artifactId>
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
  port: 8084

spring:
  application:
    name: review-service
  data:
    mongodb:
      host: localhost
      port: 27017
      database: nushungry_reviews
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
```

---

## 数据模型

### ReviewDocument (评价文档)
- **路径**: `src/main/java/com/nushungry/reviewservice/document/ReviewDocument.java`
- **集合**: `reviews`
- **字段**:
  - `id` (String): MongoDB ObjectId
  - `stallId` (Long): 档口 ID
  - `stallName` (String): 档口名称
  - `userId` (String): 用户 ID
  - `username` (String): 用户名
  - `userAvatarUrl` (String): 用户头像 URL
  - `rating` (Integer): 评分 (1-5)
  - `comment` (String): 评论内容
  - `imageUrls` (List<String>): 图片 URL 列表
  - `totalCost` (Double): 总消费金额
  - `numberOfPeople` (Integer): 消费人数
  - `likesCount` (Integer): 点赞数
  - `createdAt` (LocalDateTime): 创建时间
  - `updatedAt` (LocalDateTime): 更新时间

### ReviewLikeDocument (点赞文档)
- **路径**: `src/main/java/com/nushungry/reviewservice/document/ReviewLikeDocument.java`
- **集合**: `review_likes`
- **字段**:
  - `id` (String): MongoDB ObjectId
  - `reviewId` (String): 评价 ID
  - `userId` (String): 用户 ID
  - `createdAt` (LocalDateTime): 点赞时间
- **唯一索引**: `reviewId + userId` (防止重复点赞)

### ReviewReportDocument (举报文档)
- **路径**: `src/main/java/com/nushungry/reviewservice/document/ReviewReportDocument.java`
- **集合**: `review_reports`
- **字段**:
  - `id` (String): MongoDB ObjectId
  - `reviewId` (String): 评价 ID
  - `reporterId` (String): 举报者 ID
  - `reporterName` (String): 举报者用户名
  - `reason` (ReportReason): 举报原因枚举
    - `SPAM`: 垃圾信息
    - `OFFENSIVE`: 冒犯性内容
    - `FAKE`: 虚假评价
    - `OTHER`: 其他
  - `description` (String): 举报详细描述
  - `status` (ReportStatus): 处理状态枚举
    - `PENDING`: 待处理
    - `APPROVED`: 已批准
    - `REJECTED`: 已拒绝
    - `IGNORED`: 已忽略
  - `handledBy` (String): 处理人 ID
  - `handledAt` (LocalDateTime): 处理时间
  - `handleNote` (String): 处理备注
  - `createdAt` (LocalDateTime): 举报时间

### MongoDB 索引建议
```javascript
// reviews 集合
db.reviews.createIndex({ "stallId": 1, "createdAt": -1 })
db.reviews.createIndex({ "stallId": 1, "likesCount": -1 })
db.reviews.createIndex({ "userId": 1, "createdAt": -1 })
db.reviews.createIndex({ "rating": 1 })

// review_likes 集合
db.review_likes.createIndex({ "reviewId": 1, "userId": 1 }, { unique: true })

// review_reports 集合
db.review_reports.createIndex({ "reviewId": 1 })
db.review_reports.createIndex({ "status": 1 })
```

---

## 核心组件

### 服务层

#### ReviewService
- **路径**: `src/main/java/com/nushungry/reviewservice/service/ReviewService.java`
- **职责**:
  - 评价 CRUD 操作
  - 权限验证 (用户只能编辑/删除自己的评价)
  - 创建/更新/删除评价后触发评分计算和事件发布
  - 分页查询档口/用户评价列表

#### ReviewLikeService
- **路径**: `src/main/java/com/nushungry/reviewservice/service/ReviewLikeService.java`
- **职责**:
  - 点赞/取消点赞逻辑
  - 原子性更新评价点赞数
  - 防止重复点赞 (唯一索引)

#### ReviewReportService
- **路径**: `src/main/java/com/nushungry/reviewservice/service/ReviewReportService.java`
- **职责**:
  - 举报评价
  - 防止重复举报 (同一用户对同一评价只能举报一次)
  - 管理员处理举报 (批准/拒绝/忽略)

#### RatingCalculationService
- **路径**: `src/main/java/com/nushungry/reviewservice/service/RatingCalculationService.java`
- **职责**:
  - 计算档口平均评分
  - 统计评价数量
  - 计算评分分布 (1-5 星各多少条)

#### PriceCalculationService
- **路径**: `src/main/java/com/nushungry/reviewservice/service/PriceCalculationService.java`
- **职责**:
  - 基于评价中的 totalCost/numberOfPeople 计算人均价格
  - 计算档口平均价格

#### EventPublisherService
- **路径**: `src/main/java/com/nushungry/reviewservice/service/EventPublisherService.java`
- **职责**:
  - 发布评分变更事件到 RabbitMQ
  - 发布价格变更事件到 RabbitMQ
  - 交换机: `review.exchange`
  - 路由键:
    - `review.rating.changed` (评分变更)
    - `review.price.changed` (价格变更)

### 事件定义

#### RatingChangedEvent
- **路径**: `src/main/java/com/nushungry/reviewservice/event/RatingChangedEvent.java`
- **字段**:
  - `stallId`: 档口 ID
  - `newAverageRating`: 新的平均评分
  - `reviewCount`: 评价数量
  - `timestamp`: 时间戳

#### PriceChangedEvent
- **路径**: `src/main/java/com/nushungry/reviewservice/event/PriceChangedEvent.java`
- **字段**:
  - `stallId`: 档口 ID
  - `newAveragePrice`: 新的平均价格
  - `priceCount`: 价格样本数量
  - `timestamp`: 时间戳

### 数据访问层

#### ReviewRepository
- **路径**: `src/main/java/com/nushungry/reviewservice/repository/ReviewRepository.java`
- **关键方法**:
  - `findByStallIdOrderByCreatedAtDesc(Long stallId, Pageable pageable)` - 按时间倒序查询档口评价
  - `findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable)` - 查询用户评价
  - `countByStallId(Long stallId)` - 统计档口评价数量
  - 自定义聚合查询评分分布

#### ReviewLikeRepository
- **路径**: `src/main/java/com/nushungry/reviewservice/repository/ReviewLikeRepository.java`
- **关键方法**:
  - `findByReviewIdAndUserId(String reviewId, String userId)` - 查询点赞记录
  - `deleteByReviewIdAndUserId(String reviewId, String userId)` - 取消点赞
  - `countByReviewId(String reviewId)` - 统计点赞数

#### ReviewReportRepository
- **路径**: `src/main/java/com/nushungry/reviewservice/repository/ReviewReportRepository.java`
- **关键方法**:
  - `findByReviewId(String reviewId)` - 查询评价的举报记录
  - `findByStatus(ReportStatus status, Pageable pageable)` - 按状态查询举报
  - `existsByReviewIdAndReporterId(String reviewId, String reporterId)` - 检查是否已举报

### 配置类

#### MongoConfig
- **路径**: `src/main/java/com/nushungry/reviewservice/config/MongoConfig.java`
- **职责**: 配置 MongoDB 连接和审计功能

#### RabbitMQConfig
- **路径**: `src/main/java/com/nushungry/reviewservice/config/RabbitMQConfig.java`
- **职责**:
  - 声明交换机 `review.exchange`
  - 声明队列 `review.rating.queue`, `review.price.queue`
  - 绑定路由键

---

## 测试与质量

### 测试文件位置
- **控制器测试**:
  - `ReviewControllerTest.java`
  - `ReviewLikeControllerTest.java`
  - `ReviewReportControllerTest.java`
- **服务层测试**:
  - `ReviewServiceTest.java`
  - `ReviewServiceIntegrationTest.java`
  - `ReviewLikeServiceTest.java`
  - `ReviewReportServiceTest.java`
  - `RatingCalculationServiceTest.java`
  - `PriceCalculationServiceTest.java`
  - `EventPublisherServiceTest.java`
- **数据层测试**:
  - `ReviewRepositoryTest.java`
  - `ReviewLikeRepositoryTest.java`
  - `ReviewReportRepositoryTest.java`
- **集成测试**:
  - `CrossServiceIntegrationTest.java` - 跨服务集成测试
  - `PerformanceTest.java` - 性能测试

### 运行测试
```bash
cd review-service
mvn test
mvn test jacoco:report  # 生成覆盖率报告
```

### 测试覆盖率
- 控制器层: 3 个测试文件
- 服务层: 7 个测试文件
- 数据层: 3 个测试文件
- 集成/性能测试: 2 个文件

---

## 常见问题 (FAQ)

### Q: 评价创建后如何通知其他服务？
A: review-service 在评价创建/更新/删除后，通过 EventPublisherService 向 RabbitMQ 发布评分和价格变更事件，cafeteria-service 监听并更新档口信息。

### Q: 如何防止用户重复点赞？
A: MongoDB 对 `review_likes` 集合的 `reviewId + userId` 字段创建唯一索引，数据库层面保证不会重复插入。

### Q: 用户能否编辑他人的评价？
A: 不能。ReviewService 在更新/删除前验证 `X-User-Id` 是否与评价的 userId 匹配，不匹配抛出 UnauthorizedException。

### Q: 举报如何防止滥用？
A: 系统检查同一用户对同一评价是否已举报 (existsByReviewIdAndReporterId)，防止重复举报。

### Q: 评分如何计算？
A: RatingCalculationService 聚合档口所有评价的 rating 字段，计算平均值和分布统计。

### Q: MongoDB 如何备份？
A: 使用 `mongodump` 导出数据，定期备份到云存储或文件系统。

---

## 相关文件清单

### 核心文件
- `src/main/java/com/nushungry/reviewservice/ReviewServiceApplication.java` - 启动类
- `src/main/java/com/nushungry/reviewservice/controller/ReviewController.java` - 评价控制器
- `src/main/java/com/nushungry/reviewservice/controller/ReviewLikeController.java` - 点赞控制器
- `src/main/java/com/nushungry/reviewservice/controller/ReviewReportController.java` - 举报控制器
- `src/main/java/com/nushungry/reviewservice/controller/AdminReviewController.java` - 管理员评价控制器
- `src/main/java/com/nushungry/reviewservice/controller/AdminReportController.java` - 管理员举报控制器
- `src/main/java/com/nushungry/reviewservice/service/ReviewService.java` - 评价服务
- `src/main/java/com/nushungry/reviewservice/service/ReviewLikeService.java` - 点赞服务
- `src/main/java/com/nushungry/reviewservice/service/ReviewReportService.java` - 举报服务
- `src/main/java/com/nushungry/reviewservice/service/RatingCalculationService.java` - 评分计算服务
- `src/main/java/com/nushungry/reviewservice/service/PriceCalculationService.java` - 价格计算服务
- `src/main/java/com/nushungry/reviewservice/service/EventPublisherService.java` - 事件发布服务

### 数据模型
- `src/main/java/com/nushungry/reviewservice/document/ReviewDocument.java` - 评价文档
- `src/main/java/com/nushungry/reviewservice/document/ReviewLikeDocument.java` - 点赞文档
- `src/main/java/com/nushungry/reviewservice/document/ReviewReportDocument.java` - 举报文档
- `src/main/java/com/nushungry/reviewservice/enums/ReportReason.java` - 举报原因枚举
- `src/main/java/com/nushungry/reviewservice/enums/ReportStatus.java` - 举报状态枚举

### 事件
- `src/main/java/com/nushungry/reviewservice/event/RatingChangedEvent.java`
- `src/main/java/com/nushungry/reviewservice/event/PriceChangedEvent.java`

### 配置文件
- `src/main/resources/logback-spring.xml` - 日志配置
- `pom.xml` - Maven 项目配置

### 测试文件
- `src/test/java/com/nushungry/reviewservice/` - 15+ 测试文件

---

**模块维护者**: Review Service Team
**最后更新**: 2025-11-13 10:00:07 UTC
**相关服务**: cafeteria-service, gateway-service, user-service
