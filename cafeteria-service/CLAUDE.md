[根目录](../CLAUDE.md) > **cafeteria-service**

---

# Cafeteria Service - 食堂档口服务

## 变更记录 (Changelog)

### 2025-11-13 10:00:07 UTC
- 初始化模块文档
- 记录食堂档口管理功能
- 整理 Redis 缓存与 RabbitMQ 事件监听机制

---

## 模块职责

Cafeteria Service 负责 NUSHungry 平台的食堂和档口信息管理：
- **食堂管理**: 食堂信息的增删改查
- **档口管理**: 档口基本信息、营业时间、位置、标签管理
- **搜索查询**: 按关键字、食堂、标签、评分等条件搜索档口
- **图片管理**: 食堂和档口图片关联、展示
- **评分汇总**: 监听 review-service 评分变更事件，更新档口平均评分
- **缓存优化**: 使用 Redis 缓存热点食堂和档口数据

---

## 入口与启动

### 主启动类
- **路径**: `src/main/java/com/nushungry/cafeteriaservice/CafeteriaServiceApplication.java`
- **端口**: 8083 (默认)
- **框架**: Spring Boot 3.2.3 + Spring Data JPA + Redis + RabbitMQ

### 启动方式
```bash
# 开发模式
mvn spring-boot:run

# 生产模式
mvn clean package -DskipTests
java -jar target/cafeteria-service-0.0.1-SNAPSHOT.jar

# Docker 启动
docker build -t cafeteria-service .
docker run -p 8083:8083 cafeteria-service
```

### 健康检查
```bash
curl http://localhost:8083/actuator/health
```

---

## 对外接口

### 公开接口 (CafeteriaController)

| 方法 | 路径 | 说明 | 认证要求 |
|-----|------|-----|---------|
| GET | `/api/cafeterias` | 获取所有食堂列表 | 无 |
| GET | `/api/cafeterias/{id}` | 获取食堂详情 | 无 |
| GET | `/api/cafeterias/{id}/stalls` | 获取食堂下的所有档口 | 无 |

### 档口接口 (StallController)

| 方法 | 路径 | 说明 | 认证要求 |
|-----|------|-----|---------|
| GET | `/api/stalls` | 获取所有档口列表 | 无 |
| GET | `/api/stalls/{id}` | 获取档口详情 | 无 |
| GET | `/api/stalls/search` | 搜索档口 (支持多条件筛选) | 无 |

**搜索参数示例:**
```
GET /api/stalls/search?keyword=chicken&cafeteriaId=1&minRating=4.0&page=0&size=20&sort=rating,desc
```

### 管理员接口 (AdminCafeteriaController)

| 方法 | 路径 | 说明 | 认证要求 |
|-----|------|-----|---------|
| POST | `/api/admin/cafeterias` | 创建食堂 | 管理员 |
| PUT | `/api/admin/cafeterias/{id}` | 更新食堂信息 | 管理员 |
| DELETE | `/api/admin/cafeterias/{id}` | 删除食堂 | 管理员 |

### 管理员档口接口 (AdminStallController)

| 方法 | 路径 | 说明 | 认证要求 |
|-----|------|-----|---------|
| POST | `/api/admin/stalls` | 创建档口 | 管理员 |
| PUT | `/api/admin/stalls/{id}` | 更新档口信息 | 管理员 |
| DELETE | `/api/admin/stalls/{id}` | 删除档口 | 管理员 |
| PATCH | `/api/admin/stalls/{id}/status` | 更新档口状态 (营业/打烊) | 管理员 |

### 图片管理接口 (CafeteriaImageController / StallImageController)

| 方法 | 路径 | 说明 | 认证要求 |
|-----|------|-----|---------|
| POST | `/api/cafeterias/{id}/images` | 关联图片到食堂 | 管理员 |
| DELETE | `/api/cafeterias/{id}/images/{imageId}` | 移除食堂图片 | 管理员 |
| POST | `/api/stalls/{id}/images` | 关联图片到档口 | 管理员 |
| DELETE | `/api/stalls/{id}/images/{imageId}` | 移除档口图片 | 管理员 |

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

<!-- 数据库 -->
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
</dependency>

<!-- 缓存 (Redis) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-cache</artifactId>
</dependency>

<!-- 消息队列 (RabbitMQ) -->
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
- **位置**: `src/main/resources/application.properties`
- **关键配置项**:

```properties
# 服务配置
spring.application.name=cafeteria-service
server.port=8083

# 数据库配置
spring.datasource.url=jdbc:postgresql://localhost:5432/nushungry
spring.datasource.username=postgres
spring.datasource.password=postgres
spring.jpa.hibernate.ddl-auto=update
spring.jpa.open-in-view=false

# Redis 缓存配置
spring.cache.type=redis
spring.data.redis.host=localhost
spring.data.redis.port=6379
spring.data.redis.password=
spring.data.redis.timeout=3000ms
spring.data.redis.lettuce.pool.max-active=8
spring.data.redis.lettuce.pool.max-idle=8

# RabbitMQ 配置
spring.rabbitmq.host=localhost
spring.rabbitmq.port=5672
spring.rabbitmq.username=guest
spring.rabbitmq.password=guest

# Actuator 监控
management.endpoints.web.exposure.include=health,info,metrics,prometheus

# Swagger 文档
springdoc.api-docs.enabled=true
springdoc.swagger-ui.enabled=true
```

---

## 数据模型

### Cafeteria (食堂实体)
- **路径**: `src/main/java/com/nushungry/cafeteriaservice/model/Cafeteria.java`
- **表名**: `cafeterias`
- **字段**:
  - `id` (Long): 主键
  - `name` (String): 食堂名称
  - `location` (String): 位置描述
  - `latitude` (Double): 纬度
  - `longitude` (Double): 经度
  - `description` (String): 描述
  - `openingHours` (String): 营业时间
  - `status` (CafeteriaStatus): 状态 (OPEN/CLOSED/MAINTENANCE)
  - `images` (List<Image>): 关联图片
  - `createdAt` (LocalDateTime): 创建时间
  - `updatedAt` (LocalDateTime): 更新时间

### Stall (档口实体)
- **路径**: `src/main/java/com/nushungry/cafeteriaservice/model/Stall.java`
- **表名**: `stalls`
- **字段**:
  - `id` (Long): 主键
  - `name` (String): 档口名称
  - `cafeteriaId` (Long): 所属食堂 ID
  - `description` (String): 描述
  - `cuisine` (String): 菜系标签 (如: Chinese, Western, Indian)
  - `averagePrice` (Double): 平均价格
  - `averageRating` (Double): 平均评分 (由 review-service 更新)
  - `reviewCount` (Integer): 评价数量
  - `openingHours` (String): 营业时间
  - `contactNumber` (String): 联系电话
  - `images` (List<Image>): 关联图片
  - `createdAt` (LocalDateTime): 创建时间
  - `updatedAt` (LocalDateTime): 更新时间

### Image (图片实体)
- **路径**: `src/main/java/com/nushungry/cafeteriaservice/model/Image.java`
- **表名**: `images`
- **字段**:
  - `id` (Long): 主键
  - `url` (String): 图片 URL
  - `entityType` (String): 关联实体类型 (CAFETERIA/STALL)
  - `entityId` (Long): 关联实体 ID
  - `uploadedBy` (String): 上传者用户 ID
  - `createdAt` (LocalDateTime): 上传时间

### 数据库索引
- `stalls.cafeteria_id` (外键索引)
- `stalls.average_rating` (查询优化)
- `stalls.cuisine` (分类查询)
- `images.entity_type, entity_id` (复合索引)

---

## 核心组件

### 服务层

#### CafeteriaService
- **路径**: `src/main/java/com/nushungry/cafeteriaservice/service/CafeteriaService.java`
- **职责**:
  - 食堂 CRUD 操作
  - 使用 `@Cacheable` 缓存食堂列表和详情
  - 使用 `@CacheEvict` 在更新/删除时清除缓存

**缓存注解示例:**
```java
@Cacheable(value = "cafeterias", key = "#id")
public Cafeteria getCafeteriaById(Long id)

@CacheEvict(value = "cafeterias", allEntries = true)
public void updateCafeteria(Long id, Cafeteria cafeteria)
```

#### StallService
- **路径**: `src/main/java/com/nushungry/cafeteriaservice/service/StallService.java`
- **职责**:
  - 档口 CRUD 操作
  - 搜索查询 (支持关键字、食堂、评分、价格区间筛选)
  - 评分更新 (接收 RabbitMQ 评分变更事件)
  - Redis 缓存档口详情

#### ImageService
- **路径**: `src/main/java/com/nushungry/cafeteriaservice/service/ImageService.java`
- **职责**:
  - 图片与食堂/档口的关联管理
  - 图片上传记录 (URL 由 media-service 提供)
  - 图片删除和清理

### 事件监听器

#### ReviewEventListener
- **路径**: `src/main/java/com/nushungry/cafeteriaservice/listener/ReviewEventListener.java`
- **职责**:
  - 监听 RabbitMQ `review.rating.changed` 队列
  - 接收评分变更事件 (stallId, newAverageRating, reviewCount)
  - 更新档口平均评分和评价数量
  - 清除相关 Redis 缓存

**事件消息格式:**
```json
{
  "stallId": 123,
  "newAverageRating": 4.5,
  "reviewCount": 100,
  "timestamp": "2025-11-13T10:00:00"
}
```

### 数据访问层

#### CafeteriaRepository
- **路径**: `src/main/java/com/nushungry/cafeteriaservice/repository/CafeteriaRepository.java`
- **关键方法**:
  - `findAll()` - 获取所有食堂
  - `findById(Long id)` - 根据 ID 查询食堂

#### StallRepository
- **路径**: `src/main/java/com/nushungry/cafeteriaservice/repository/StallRepository.java`
- **关键方法**:
  - `findByCafeteriaId(Long cafeteriaId)` - 查询食堂下的档口
  - `findByNameContainingIgnoreCase(String name)` - 按名称模糊搜索
  - 支持 JPA Specification 动态查询

#### ImageRepository
- **路径**: `src/main/java/com/nushungry/cafeteriaservice/repository/ImageRepository.java`
- **关键方法**:
  - `findByEntityTypeAndEntityId(String type, Long id)` - 查询实体关联的图片
  - `deleteById(Long id)` - 删除图片记录

### 配置类

#### RedisConfig
- **路径**: `src/main/java/com/nushungry/cafeteriaservice/config/RedisConfig.java`
- **职责**:
  - 配置 Redis 连接池
  - 配置 RedisTemplate 序列化策略 (JSON)
  - 配置缓存过期时间

#### RabbitMQConfig
- **路径**: `src/main/java/com/nushungry/cafeteriaservice/config/RabbitMQConfig.java`
- **职责**:
  - 声明交换机 (review.exchange)
  - 声明队列 (review.rating.queue)
  - 绑定队列与路由键 (review.rating.changed)

#### OpenApiConfig
- **路径**: `src/main/java/com/nushungry/cafeteriaservice/config/OpenApiConfig.java`
- **职责**: 配置 Swagger UI 文档信息

---

## 测试与质量

### 测试文件位置
- **集成测试**: `src/test/java/com/nushungry/cafeteriaservice/CafeteriaServiceIntegrationTest.java`
- **控制器测试**:
  - `AdminCafeteriaControllerTest.java`
  - `CafeteriaControllerTest.java`
  - `StallControllerTest.java`
- **服务层测试**:
  - `CafeteriaServiceTest.java`
  - `CafeteriaServiceCacheTest.java`
  - `StallServiceTest.java`
- **数据层测试**:
  - `CafeteriaRepositoryTest.java`
  - `StallRepositoryTest.java`
  - `ImageRepositoryTest.java`
- **监听器测试**: `ReviewEventListenerTest.java`

### 运行测试
```bash
cd cafeteria-service
mvn test
mvn test jacoco:report  # 生成覆盖率报告
```

### 测试覆盖率
- 控制器层: 11 个测试文件
- 服务层: 3 个测试文件 (包括缓存测试)
- 数据层: 3 个测试文件
- 监听器: 1 个测试文件

---

## 常见问题 (FAQ)

### Q: 如何配置 Redis 缓存？
A: 修改 `application.properties` 中的 `spring.data.redis.*` 配置项，确保 Redis 服务可访问。

### Q: 评分如何更新？
A: cafeteria-service 监听 RabbitMQ `review.rating.changed` 队列，review-service 在评价创建/更新/删除后发布评分变更事件。

### Q: 如何添加新的搜索条件？
A: 在 `StallService` 中使用 JPA Specification 动态构建查询条件，添加新的过滤参数到 `StallSearchRequest` DTO。

### Q: 图片存储在哪里？
A: 图片由 media-service 存储，cafeteria-service 仅保存图片 URL 和关联关系。

### Q: 缓存何时失效？
A: 更新/删除操作时使用 `@CacheEvict` 清除缓存；也可配置缓存过期时间 (TTL)。

---

## 相关文件清单

### 核心文件
- `src/main/java/com/nushungry/cafeteriaservice/CafeteriaServiceApplication.java` - 启动类
- `src/main/java/com/nushungry/cafeteriaservice/controller/CafeteriaController.java` - 食堂控制器
- `src/main/java/com/nushungry/cafeteriaservice/controller/StallController.java` - 档口控制器
- `src/main/java/com/nushungry/cafeteriaservice/controller/AdminCafeteriaController.java` - 管理员食堂控制器
- `src/main/java/com/nushungry/cafeteriaservice/controller/AdminStallController.java` - 管理员档口控制器
- `src/main/java/com/nushungry/cafeteriaservice/controller/CafeteriaImageController.java` - 食堂图片控制器
- `src/main/java/com/nushungry/cafeteriaservice/controller/StallImageController.java` - 档口图片控制器
- `src/main/java/com/nushungry/cafeteriaservice/service/CafeteriaService.java` - 食堂服务
- `src/main/java/com/nushungry/cafeteriaservice/service/StallService.java` - 档口服务
- `src/main/java/com/nushungry/cafeteriaservice/service/ImageService.java` - 图片服务
- `src/main/java/com/nushungry/cafeteriaservice/listener/ReviewEventListener.java` - 评分事件监听器

### 数据模型
- `src/main/java/com/nushungry/cafeteriaservice/model/Cafeteria.java` - 食堂实体
- `src/main/java/com/nushungry/cafeteriaservice/model/Stall.java` - 档口实体
- `src/main/java/com/nushungry/cafeteriaservice/model/Image.java` - 图片实体
- `src/main/java/com/nushungry/cafeteriaservice/model/CafeteriaStatus.java` - 食堂状态枚举

### 数据访问层
- `src/main/java/com/nushungry/cafeteriaservice/repository/CafeteriaRepository.java`
- `src/main/java/com/nushungry/cafeteriaservice/repository/StallRepository.java`
- `src/main/java/com/nushungry/cafeteriaservice/repository/ImageRepository.java`

### 配置文件
- `src/main/resources/application.properties` - 主配置文件
- `src/main/resources/logback-spring.xml` - 日志配置

### 构建文件
- `pom.xml` - Maven 项目配置
- `Dockerfile` - Docker 镜像构建
- `docker-compose.yml` - 本地容器编排

### 测试文件
- `src/test/java/com/nushungry/cafeteriaservice/` - 11+ 测试文件

### 脚本文件
- `scripts/init_cafeteria_db.sql` - 数据库初始化脚本
- `scripts/start-services.sh` - 启动脚本
- `scripts/stop-services.sh` - 停止脚本

---

**模块维护者**: Cafeteria Service Team
**最后更新**: 2025-11-13 10:00:07 UTC
**相关服务**: review-service, media-service, gateway-service
