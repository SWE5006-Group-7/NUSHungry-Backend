[根目录](../CLAUDE.md) > **preference-service**

---

# Preference Service - 用户偏好服务

## 变更记录 (Changelog)

### 2025-11-13 10:00:07 UTC
- 初始化模块文档
- 记录用户收藏与搜索历史功能
- 整理 Redis 缓存与批量操作接口

---

## 模块职责

Preference Service 负责 NUSHungry 平台的用户个性化功能：
- **收藏管理**: 用户收藏档口，按创建时间或自定义顺序排序
- **搜索历史**: 记录用户搜索关键词，支持清空历史
- **批量操作**: 批量删除收藏或搜索历史
- **缓存优化**: 使用 Redis 缓存用户收藏和历史，减少数据库查询
- **快速访问**: 为用户提供快捷访问常用档口的入口

---

## 入口与启动

### 主启动类
- **路径**: `src/main/java/com/nushungry/preference/PreferenceServiceApplication.java`
- **端口**: 8086 (默认)
- **框架**: Spring Boot 3.x + Spring Data JPA + Redis

### 启动方式
```bash
# 开发模式
mvn spring-boot:run

# 生产模式
mvn clean package -DskipTests
java -jar target/preference-service-0.0.1-SNAPSHOT.jar

# Docker 启动
docker build -t preference-service .
docker run -p 8086:8086 preference-service
```

### 健康检查
```bash
curl http://localhost:8086/actuator/health
```

---

## 对外接口

### 收藏管理接口 (FavoriteController)

| 方法 | 路径 | 说明 | 认证要求 |
|-----|------|-----|---------|
| POST | `/preference/favorite/add` | 添加收藏 | 需登录 |
| POST | `/preference/favorite/remove` | 取消收藏 | 需登录 |
| POST | `/preference/favorite/batchRemove` | 批量删除收藏 | 需登录 |
| GET | `/preference/favorite/list` | 获取用户收藏列表 | 需登录 |
| GET | `/preference/favorite/sorted` | 获取排序后的收藏列表 | 需登录 |
| PUT | `/preference/favorite/update-order` | 更新收藏顺序 | 需登录 |

**添加收藏请求示例:**
```json
POST /preference/favorite/add
Headers:
  X-User-Id: 123

Body:
{
  "userId": "123",
  "stallId": 456
}
```

**收藏列表响应示例:**
```json
[
  {
    "id": 1,
    "userId": "123",
    "stallId": 456,
    "stallName": "Chicken Rice Stall",
    "stallImageUrl": "http://example.com/chicken-rice.jpg",
    "order": 1,
    "createdAt": "2025-11-13T10:00:00"
  },
  {
    "id": 2,
    "userId": "123",
    "stallId": 789,
    "stallName": "Western Food",
    "stallImageUrl": "http://example.com/western.jpg",
    "order": 2,
    "createdAt": "2025-11-12T15:30:00"
  }
]
```

**批量删除请求示例:**
```json
POST /preference/favorite/batchRemove?userId=123
Body:
[456, 789]  // stallId 数组
```

### 搜索历史接口 (SearchHistoryController)

| 方法 | 路径 | 说明 | 认证要求 |
|-----|------|-----|---------|
| POST | `/preference/search-history/add` | 添加搜索记录 | 需登录 |
| POST | `/preference/search-history/remove` | 删除搜索记录 | 需登录 |
| POST | `/preference/search-history/batchRemove` | 批量删除搜索记录 | 需登录 |
| GET | `/preference/search-history/list` | 获取搜索历史列表 | 需登录 |
| DELETE | `/preference/search-history/clear` | 清空搜索历史 | 需登录 |

**添加搜索历史请求示例:**
```json
POST /preference/search-history/add
Headers:
  X-User-Id: 123

Body:
{
  "userId": "123",
  "keyword": "chicken rice"
}
```

**搜索历史响应示例:**
```json
[
  {
    "id": 1,
    "userId": "123",
    "keyword": "chicken rice",
    "createdAt": "2025-11-13T10:00:00"
  },
  {
    "id": 2,
    "userId": "123",
    "keyword": "western food",
    "createdAt": "2025-11-12T18:45:00"
  }
]
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
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>

<!-- 数据库 (H2/PostgreSQL/MySQL 可配置) -->
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
  port: 8086

spring:
  application:
    name: preference-service
  datasource:
    url: jdbc:postgresql://localhost:5432/nushungry_preferences
    username: postgres
    password: postgres
  jpa:
    hibernate:
      ddl-auto: update

  # Redis 缓存配置
  cache:
    type: redis
  data:
    redis:
      host: localhost
      port: 6379
      password:
      timeout: 3000ms
      lettuce:
        pool:
          max-active: 8
          max-idle: 8
          min-idle: 0
          max-wait: -1ms

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
```

---

## 数据模型

### Favorite (收藏实体)
- **路径**: `src/main/java/com/nushungry/preference/entity/Favorite.java`
- **表名**: `favorites`
- **字段**:
  - `id` (Long): 主键
  - `userId` (String): 用户 ID
  - `stallId` (Long): 档口 ID
  - `order` (Integer): 自定义排序顺序 (可选)
  - `createdAt` (LocalDateTime): 创建时间

### SearchHistory (搜索历史实体)
- **路径**: `src/main/java/com/nushungry/preference/entity/SearchHistory.java`
- **表名**: `search_histories`
- **字段**:
  - `id` (Long): 主键
  - `userId` (String): 用户 ID
  - `keyword` (String): 搜索关键词
  - `createdAt` (LocalDateTime): 搜索时间

### 数据库索引
- `favorites.user_id, stall_id` (复合唯一索引，防止重复收藏)
- `favorites.user_id, created_at` (查询优化)
- `search_histories.user_id, created_at` (查询优化)

---

## 核心组件

### 服务层

#### FavoriteService
- **路径**: `src/main/java/com/nushungry/preference/service/FavoriteService.java`
- **职责**:
  - 收藏 CRUD 操作
  - 使用 `@Cacheable` 缓存用户收藏列表
  - 使用 `@CacheEvict` 在增删改时清除缓存
  - 调用 cafeteria-service 获取档口详细信息 (通过 RestTemplate/Feign)

**缓存注解示例:**
```java
@Cacheable(value = "userFavorites", key = "#userId")
public List<FavoriteResponse> getUserFavorites(String userId)

@CacheEvict(value = "userFavorites", key = "#userId")
public void addFavorite(String userId, Long stallId)
```

#### SearchHistoryService
- **路径**: `src/main/java/com/nushungry/preference/service/SearchHistoryService.java`
- **职责**:
  - 搜索历史记录和查询
  - 防止重复记录相同关键词 (或更新时间戳)
  - 限制历史记录数量 (如最多保留 50 条)
  - 使用 Redis 缓存最近搜索

### 数据访问层

#### FavoriteRepository
- **路径**: `src/main/java/com/nushungry/preference/repository/FavoriteRepository.java`
- **关键方法**:
  - `findByUserIdOrderByCreatedAtDesc(String userId)` - 按时间倒序查询收藏
  - `findByUserIdOrderByOrderAsc(String userId)` - 按自定义顺序查询
  - `findByUserIdAndStallId(String userId, Long stallId)` - 检查是否已收藏
  - `deleteByUserIdAndStallIdIn(String userId, List<Long> stallIds)` - 批量删除

#### SearchHistoryRepository
- **路径**: `src/main/java/com/nushungry/preference/repository/SearchHistoryRepository.java`
- **关键方法**:
  - `findByUserIdOrderByCreatedAtDesc(String userId)` - 按时间倒序查询历史
  - `deleteByUserIdAndKeywordIn(String userId, List<String> keywords)` - 批量删除
  - `deleteByUserId(String userId)` - 清空用户所有历史
  - `countByUserId(String userId)` - 统计历史记录数量

### 配置类

#### RedisConfig
- **路径**: `src/main/java/com/nushungry/preference/config/RedisConfig.java`
- **职责**:
  - 配置 Redis 连接池
  - 配置 RedisTemplate 序列化策略 (JSON)
  - 配置缓存过期时间

#### OpenApiConfig
- **路径**: `src/main/java/com/nushungry/preference/config/OpenApiConfig.java`
- **职责**: 配置 Swagger UI 文档信息

---

## 测试与质量

### 测试文件位置
- **集成测试**: `PreferenceServiceIntegrationTest.java`
- **控制器测试**:
  - `FavoriteControllerTest.java`
  - `SearchHistoryControllerTest.java`
- **服务层测试**:
  - `FavoriteServiceTest.java`
  - `FavoriteServiceCacheTest.java`
  - `SearchHistoryServiceTest.java`
  - `SearchHistoryServiceCacheTest.java`
- **数据层测试**:
  - `FavoriteRepositoryTest.java`
  - `SearchHistoryRepositoryTest.java`

### 运行测试
```bash
cd preference-service
mvn test
mvn test jacoco:report  # 生成覆盖率报告
```

### 测试覆盖率
- 控制器层: 2 个测试文件
- 服务层: 4 个测试文件 (包括缓存测试)
- 数据层: 2 个测试文件

---

## 常见问题 (FAQ)

### Q: 如何防止用户重复收藏同一档口？
A: 数据库对 `user_id + stall_id` 创建唯一索引，插入时若已存在则抛出异常或忽略。

### Q: 收藏列表如何获取档口详情？
A: preference-service 调用 cafeteria-service 的 `/api/stalls/{id}` 接口获取档口名称、图片等信息，组装成 FavoriteResponse 返回。

### Q: 搜索历史如何限制数量？
A: 在添加新记录前检查 `countByUserId(userId)`，若超过上限 (如 50)，删除最早的记录。

### Q: 缓存何时失效？
A: 增删改操作使用 `@CacheEvict` 清除缓存；也可配置 Redis 过期时间 (如 1 小时)。

### Q: 如何实现拖拽排序？
A: 前端发送新的 order 列表，后端批量更新 `order` 字段，清除缓存。

### Q: 如何处理档口被删除的情况？
A: 定期扫描 favorites 表，调用 cafeteria-service 检查档口是否存在，删除失效收藏。

---

## 相关文件清单

### 核心文件
- `src/main/java/com/nushungry/preference/PreferenceServiceApplication.java` - 启动类
- `src/main/java/com/nushungry/preference/controller/FavoriteController.java` - 收藏控制器
- `src/main/java/com/nushungry/preference/controller/SearchHistoryController.java` - 搜索历史控制器
- `src/main/java/com/nushungry/preference/service/FavoriteService.java` - 收藏服务
- `src/main/java/com/nushungry/preference/service/SearchHistoryService.java` - 搜索历史服务
- `src/main/java/com/nushungry/preference/entity/Favorite.java` - 收藏实体
- `src/main/java/com/nushungry/preference/entity/SearchHistory.java` - 搜索历史实体
- `src/main/java/com/nushungry/preference/repository/FavoriteRepository.java` - 收藏数据访问接口
- `src/main/java/com/nushungry/preference/repository/SearchHistoryRepository.java` - 搜索历史数据访问接口

### DTO
- `src/main/java/com/nushungry/preference/dto/FavoriteResponse.java` - 收藏响应对象
- `src/main/java/com/nushungry/preference/dto/UpdateFavoriteOrderRequest.java` - 更新顺序请求对象

### 配置文件
- `src/main/resources/logback-spring.xml` - 日志配置
- `pom.xml` - Maven 项目配置
- `Dockerfile` - Docker 镜像构建
- `docker-compose.yml` - 本地容器编排

### 测试文件
- `src/test/java/com/nushungry/preference/` - 8+ 测试文件

### 脚本文件
- `scripts/init_preference_db.sql` - 数据库初始化脚本
- `scripts/start-services.sh` - 启动脚本
- `scripts/stop-services.sh` - 停止脚本

---

**模块维护者**: Preference Service Team
**最后更新**: 2025-11-13 10:00:07 UTC
**相关服务**: cafeteria-service, gateway-service, user-service
