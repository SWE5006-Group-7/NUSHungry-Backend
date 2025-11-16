# Review Service

NUSHungry 评价微服务 - 管理摊位评价、点赞和举报功能。

## 技术栈

- **Spring Boot**: 3.2.3
- **数据库**: MongoDB
- **消息队列**: RabbitMQ
- **API 文档**: Swagger/OpenAPI 3
- **开发工具**: Lombok, Spring Boot Actuator

## 项目结构

```
review-service/
├── src/main/java/com/nushungry/reviewservice/
│   ├── common/                 # 通用响应类
│   ├── config/                 # 配置类 (MongoDB, RabbitMQ, OpenAPI)
│   ├── controller/             # REST API 控制器
│   ├── document/               # MongoDB 文档模型
│   ├── dto/                    # 数据传输对象
│   ├── enums/                  # 枚举类
│   ├── event/                  # 事件定义
│   ├── exception/              # 异常类
│   ├── repository/             # MongoDB 仓储接口
│   └── service/                # 业务逻辑层
└── src/main/resources/
    └── application.yml         # 应用配置
```

## 快速开始

### 前置条件

1. **JDK 17+**
2. **Maven 3.6+**
3. **MongoDB 4.4+** (运行在 localhost:27017)
4. **RabbitMQ 3.8+** (运行在 localhost:5672)

### 安装依赖

```bash
mvn clean install
```

### 运行服务

```bash
mvn spring-boot:run
```

服务将在 **http://localhost:8084** 启动。

### API 文档

启动服务后访问: http://localhost:8084/swagger-ui.html

## 核心功能

### 1. 评价管理 (Review Management)

- **创建评价**: `POST /api/reviews`
- **更新评价**: `PUT /api/reviews/{id}`
- **删除评价**: `DELETE /api/reviews/{id}`
- **获取评价详情**: `GET /api/reviews/{id}`
- **获取摊位评价列表**: `GET /api/reviews/stall/{stallId}`
- **获取用户评价列表**: `GET /api/reviews/user/{userId}`
- **获取评分分布**: `GET /api/reviews/stall/{stallId}/rating-distribution`

### 2. 点赞管理 (Like Management)

- **切换点赞状态**: `POST /api/reviews/{id}/like`
- **检查是否已点赞**: `GET /api/reviews/{id}/is-liked`
- **获取点赞数**: `GET /api/reviews/{id}/like-count`

### 3. 举报管理 (Report Management)

- **举报评价**: `POST /api/reviews/{id}/report`
- **获取评价的举报记录**: `GET /api/reviews/{id}/reports` (管理员)
- **按状态查询举报记录**: `GET /api/reviews/reports/status/{status}` (管理员)
- **处理举报**: `PUT /api/reports/{id}/handle` (管理员)

## 事件发布

服务通过 RabbitMQ 发布以下事件到 `review.exchange`:

### 评分变更事件 (Rating Changed)
- **路由键**: `review.rating.changed`
- **队列**: `review.rating.queue`
- **内容**: 
  ```json
  {
    "stallId": 123,
    "newAverageRating": 4.5,
    "reviewCount": 100,
    "timestamp": "2025-10-19T00:00:00"
  }
  ```

### 价格变更事件 (Price Changed)
- **路由键**: `review.price.changed`
- **队列**: `review.price.queue`
- **内容**:
  ```json
  {
    "stallId": 123,
    "newAveragePrice": 8.5,
    "priceCount": 50,
    "timestamp": "2025-10-19T00:00:00"
  }
  ```

## 数据模型

### ReviewDocument
```java
{
  "id": "string",
  "stallId": 123,
  "stallName": "string",
  "userId": "string",
  "username": "string",
  "userAvatarUrl": "string",
  "rating": 5,
  "comment": "string",
  "imageUrls": ["url1", "url2"],
  "totalCost": 10.0,
  "numberOfPeople": 2,
  "likesCount": 10,
  "createdAt": "2025-10-19T00:00:00",
  "updatedAt": "2025-10-19T00:00:00"
}
```

### ReviewLikeDocument
```java
{
  "id": "string",
  "reviewId": "string",
  "userId": "string",
  "createdAt": "2025-10-19T00:00:00"
}
```

### ReviewReportDocument
```java
{
  "id": "string",
  "reviewId": "string",
  "reporterId": "string",
  "reporterName": "string",
  "reason": "SPAM|OFFENSIVE|FAKE|OTHER",
  "description": "string",
  "status": "PENDING|APPROVED|REJECTED|IGNORED",
  "handledBy": "string",
  "handledAt": "2025-10-19T00:00:00",
  "handleNote": "string",
  "createdAt": "2025-10-19T00:00:00"
}
```

## MongoDB 索引

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

## 配置说明

### application.yml

```yaml
server:
  port: 8084

spring:
  data:
    mongodb:
      host: localhost          # MongoDB 主机
      port: 27017              # MongoDB 端口
      database: nushungry_reviews  # 数据库名称
  
  rabbitmq:
    host: localhost            # RabbitMQ 主机
    port: 5672                 # RabbitMQ 端口
    username: guest            # RabbitMQ 用户名
    password: guest            # RabbitMQ 密码
```

## 健康检查

访问: http://localhost:8084/actuator/health

## 权限认证

服务通过 HTTP Header 获取用户信息：

- `X-User-Id`: 当前用户ID
- `X-Username`: 当前用户名
- `X-User-Avatar`: 当前用户头像URL (可选)
- `X-User-Role`: 当前用户角色 (ROLE_USER / ROLE_ADMIN)

## 开发注意事项

1. **权限控制**: 用户只能编辑/删除自己的评价
2. **自动更新**: 创建/更新/删除评价时自动计算评分和价格并发布事件
3. **点赞原子性**: 点赞操作使用事务保证数据一致性
4. **举报防重**: 同一用户对同一评价只能举报一次
