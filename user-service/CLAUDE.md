[根目录](../CLAUDE.md) > **user-service**

---

# User Service - 用户服务

## 变更记录 (Changelog)

### 2025-11-13 10:00:07 UTC
- 初始化模块文档
- 记录用户认证与管理功能
- 整理 JWT 签发与邮件验证流程

---

## 模块职责

User Service 负责 NUSHungry 平台的用户身份管理：
- **用户注册**: 账号创建、邮箱验证
- **用户登录**: 身份验证、JWT Token 签发
- **Token 管理**: 访问令牌 (Access Token) 和刷新令牌 (Refresh Token) 的生成与刷新
- **密码重置**: 邮箱验证码发送、密码修改
- **用户信息管理**: 个人资料查询与更新
- **管理员功能**: 用户列表查询、批量操作、权限管理
- **邮件服务**: 验证码发送、密码重置通知

---

## 入口与启动

### 主启动类
- **路径**: `src/main/java/com/nushungry/userservice/UserServiceApplication.java`
- **端口**: 8081 (默认)
- **上下文路径**: `/api`
- **框架**: Spring Boot 3.2.3 + Spring Security + Spring Data JPA

### 启动方式
```bash
# 开发模式
mvn spring-boot:run

# 生产模式
mvn clean package -DskipTests
java -jar target/user-service-0.0.1-SNAPSHOT.jar

# 指定配置
java -jar target/user-service-0.0.1-SNAPSHOT.jar --spring.config.location=file:/path/to/application.properties
```

### 健康检查
```bash
curl http://localhost:8081/api/actuator/health
```

---

## 对外接口

### 认证接口 (AuthController)

| 方法 | 路径 | 说明 | 认证要求 |
|-----|------|-----|---------|
| POST | `/api/auth/register` | 用户注册 | 无 |
| POST | `/api/auth/login` | 用户登录 | 无 |
| POST | `/api/auth/refresh` | 刷新访问令牌 | 需 Refresh Token |
| POST | `/api/auth/logout` | 用户登出 (撤销刷新令牌) | 需 Access Token |

**请求示例 - 注册:**
```json
POST /api/auth/register
{
  "username": "john_doe",
  "email": "john@example.com",
  "password": "SecurePass123!"
}
```

**响应示例:**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "dGhpcyBpcyBhIHJlZnJlc2ggdG9rZW4...",
  "expiresIn": 3600,
  "tokenType": "Bearer",
  "user": {
    "id": "123",
    "username": "john_doe",
    "email": "john@example.com",
    "role": "ROLE_USER"
  }
}
```

### 用户管理接口 (UserController)

| 方法 | 路径 | 说明 | 认证要求 |
|-----|------|-----|---------|
| GET | `/api/users/me` | 获取当前用户信息 | 需登录 |
| PUT | `/api/users/me` | 更新当前用户信息 | 需登录 |
| PUT | `/api/users/me/password` | 修改密码 | 需登录 |
| DELETE | `/api/users/me` | 注销账号 | 需登录 |

### 密码重置接口 (PasswordResetController)

| 方法 | 路径 | 说明 | 认证要求 |
|-----|------|-----|---------|
| POST | `/api/password-reset/send-code` | 发送验证码到邮箱 | 无 |
| POST | `/api/password-reset/verify-code` | 验证邮箱验证码 | 无 |
| POST | `/api/password-reset/reset` | 使用验证码重置密码 | 无 |

### 管理员接口 (AdminUserController)

| 方法 | 路径 | 说明 | 认证要求 |
|-----|------|-----|---------|
| GET | `/api/admin/users` | 查询用户列表 (分页) | 管理员 |
| GET | `/api/admin/users/{id}` | 获取用户详情 | 管理员 |
| POST | `/api/admin/users` | 创建用户 | 管理员 |
| PUT | `/api/admin/users/{id}` | 更新用户信息 | 管理员 |
| DELETE | `/api/admin/users/{id}` | 删除用户 | 管理员 |
| POST | `/api/admin/users/batch-operation` | 批量操作 (启用/禁用/删除) | 管理员 |

### 管理员认证接口 (AdminAuthController)

| 方法 | 路径 | 说明 | 认证要求 |
|-----|------|-----|---------|
| POST | `/api/admin/auth/login` | 管理员登录 | 无 |

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
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>

<!-- 数据库 -->
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
</dependency>

<!-- JWT -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.11.5</version>
</dependency>

<!-- 邮件服务 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-mail</artifactId>
</dependency>

<!-- 模板引擎 (邮件模板) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-thymeleaf</artifactId>
</dependency>

<!-- 消息队列 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-amqp</artifactId>
</dependency>
```

### 配置文件
- **位置**: `src/main/resources/application.properties`
- **关键配置项**:

```properties
# 服务端口
server.port=8081
server.servlet.context-path=/api

# 数据库连接
spring.datasource.url=jdbc:postgresql://localhost:5432/nushungry_users
spring.datasource.username=postgres
spring.datasource.password=postgres
spring.jpa.hibernate.ddl-auto=update

# JWT 配置
jwt.secret=mySecretKeyForNUSHungryUserServiceThatIsLongEnoughForHS256Algorithm
jwt.access-token.expiration=3600000    # 1 小时
jwt.refresh-token.expiration=2592000000 # 30 天

# 邮件配置
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your-email@gmail.com
spring.mail.password=your-app-password
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true

# 验证码配置
app.verification.code.expiration=300000  # 5 分钟
app.verification.code.length=6

# 其他微服务 URL
services.review-service.url=http://localhost:8084/api
services.preference-service.url=http://localhost:8086/api
services.media-service.url=http://localhost:8086/api
```

---

## 数据模型

### User (用户实体)
- **路径**: `src/main/java/com/nushungry/userservice/model/User.java`
- **表名**: `users`
- **字段**:
  - `id` (Long): 主键
  - `username` (String): 用户名 (唯一)
  - `email` (String): 邮箱 (唯一)
  - `password` (String): 加密密码 (BCrypt)
  - `role` (UserRole): 用户角色 (ROLE_USER / ROLE_ADMIN)
  - `enabled` (Boolean): 账号是否启用
  - `createdAt` (LocalDateTime): 创建时间
  - `updatedAt` (LocalDateTime): 更新时间

### RefreshToken (刷新令牌)
- **路径**: `src/main/java/com/nushungry/userservice/model/RefreshToken.java`
- **表名**: `refresh_tokens`
- **字段**:
  - `id` (Long): 主键
  - `token` (String): 刷新令牌 (UUID)
  - `userId` (Long): 用户 ID
  - `expiryDate` (LocalDateTime): 过期时间
  - `ipAddress` (String): 签发 IP
  - `userAgent` (String): 用户代理
  - `createdAt` (LocalDateTime): 创建时间

### VerificationCode (验证码)
- **路径**: `src/main/java/com/nushungry/userservice/model/VerificationCode.java`
- **表名**: `verification_codes`
- **字段**:
  - `id` (Long): 主键
  - `email` (String): 邮箱
  - `code` (String): 验证码 (6 位数字)
  - `type` (String): 类型 (PASSWORD_RESET / EMAIL_VERIFICATION)
  - `expiryDate` (LocalDateTime): 过期时间
  - `used` (Boolean): 是否已使用

### 数据库索引
- `users.username` (唯一索引)
- `users.email` (唯一索引)
- `refresh_tokens.token` (唯一索引)
- `verification_codes.email` (普通索引)

---

## 核心组件

### 服务层

#### UserService
- **路径**: `src/main/java/com/nushungry/userservice/service/UserService.java`
- **职责**:
  - 用户注册、登录逻辑
  - 密码加密 (BCrypt) 和验证
  - JWT Token 生成
  - 用户信息 CRUD 操作

#### RefreshTokenService
- **路径**: `src/main/java/com/nushungry/userservice/service/RefreshTokenService.java`
- **职责**:
  - 刷新令牌生成和存储
  - 刷新令牌验证和撤销
  - 过期令牌清理

#### EmailService
- **路径**: `src/main/java/com/nushungry/userservice/service/EmailService.java`
- **职责**:
  - 使用 Thymeleaf 渲染邮件模板
  - 发送验证码邮件
  - 发送密码重置成功通知

#### VerificationCodeService
- **路径**: `src/main/java/com/nushungry/userservice/service/VerificationCodeService.java`
- **职责**:
  - 生成随机验证码
  - 验证码存储和验证
  - 防止频繁发送 (限流)

#### AdminUserService
- **路径**: `src/main/java/com/nushungry/userservice/service/AdminUserService.java`
- **职责**:
  - 用户列表查询 (分页、排序、搜索)
  - 批量用户操作 (启用/禁用/删除)
  - 管理员权限验证

### 数据访问层

#### UserRepository
- **路径**: `src/main/java/com/nushungry/userservice/repository/UserRepository.java`
- **关键方法**:
  - `findByUsername(String username)`
  - `findByEmail(String email)`
  - `existsByUsername(String username)`
  - `existsByEmail(String email)`

#### RefreshTokenRepository
- **路径**: `src/main/java/com/nushungry/userservice/repository/RefreshTokenRepository.java`
- **关键方法**:
  - `findByToken(String token)`
  - `deleteByUserId(Long userId)`
  - `deleteByExpiryDateBefore(LocalDateTime date)` - 清理过期令牌

#### VerificationCodeRepository
- **路径**: `src/main/java/com/nushungry/userservice/repository/VerificationCodeRepository.java`
- **关键方法**:
  - `findByEmailAndCode(String email, String code)`
  - `deleteByExpiryDateBefore(LocalDateTime date)` - 清理过期验证码

### 安全配置

#### SecurityConfig
- **路径**: `src/main/java/com/nushungry/userservice/config/SecurityConfig.java`
- **职责**:
  - 配置 Spring Security 过滤器链
  - 定义公开端点 (登录、注册、密码重置)
  - 配置 BCrypt 密码编码器
  - 禁用 CSRF (适用于无状态 API)

#### JwtAuthenticationFilter
- **路径**: `src/main/java/com/nushungry/userservice/filter/JwtAuthenticationFilter.java`
- **职责**:
  - 拦截请求，提取 JWT Token
  - 验证 Token 并设置 Spring Security 认证上下文

---

## 测试与质量

### 测试文件位置
- **服务层测试**: 暂未发现 (建议补充)
- **控制器测试**: 暂未发现 (建议补充)
- **集成测试**: 暂未发现 (建议补充)

### 测试建议
```bash
# 运行测试
cd user-service
mvn test

# 生成覆盖率报告
mvn test jacoco:report
```

### 质量工具
- **单元测试**: JUnit 5 + Mockito
- **集成测试**: @SpringBootTest + H2 内存数据库
- **API 测试**: MockMvc
- **代码覆盖**: JaCoCo
- **静态分析**: SpotBugs, OWASP Dependency Check

### 测试覆盖率目标
- 服务层: 80%+
- 控制器层: 70%+
- 数据访问层: 60%+

---

## 常见问题 (FAQ)

### Q: JWT 密钥如何配置？
A: 在 `application.properties` 中配置 `jwt.secret`，生产环境使用至少 256 位的强随机密钥，并通过环境变量注入。

### Q: 如何配置邮件服务？
A: 修改 `spring.mail.*` 配置项。对于 Gmail，需启用"应用专用密码"。其他 SMTP 服务类似配置。

### Q: 刷新令牌何时过期？
A: 默认 30 天，配置项: `jwt.refresh-token.expiration` (毫秒)。过期后需重新登录。

### Q: 如何处理密码重置？
A: 用户请求 → 发送验证码到邮箱 → 用户提交验证码+新密码 → 服务端验证并更新密码。

### Q: 如何添加新的用户角色？
A: 在 `UserRole` 枚举中添加角色，更新权限配置，修改 JWT Claims 包含新角色。

### Q: 数据库表如何初始化？
A: 开发环境使用 `spring.jpa.hibernate.ddl-auto=update` 自动建表。生产环境建议使用 Flyway/Liquibase 进行版本化迁移。

---

## 相关文件清单

### 核心文件
- `src/main/java/com/nushungry/userservice/UserServiceApplication.java` - 启动类
- `src/main/java/com/nushungry/userservice/controller/AuthController.java` - 认证控制器
- `src/main/java/com/nushungry/userservice/controller/UserController.java` - 用户管理控制器
- `src/main/java/com/nushungry/userservice/controller/PasswordResetController.java` - 密码重置控制器
- `src/main/java/com/nushungry/userservice/controller/AdminUserController.java` - 管理员控制器
- `src/main/java/com/nushungry/userservice/controller/AdminAuthController.java` - 管理员认证控制器
- `src/main/java/com/nushungry/userservice/service/UserService.java` - 用户服务
- `src/main/java/com/nushungry/userservice/service/RefreshTokenService.java` - 刷新令牌服务
- `src/main/java/com/nushungry/userservice/service/EmailService.java` - 邮件服务
- `src/main/java/com/nushungry/userservice/service/VerificationCodeService.java` - 验证码服务
- `src/main/java/com/nushungry/userservice/filter/JwtAuthenticationFilter.java` - JWT 认证过滤器
- `src/main/java/com/nushungry/userservice/config/SecurityConfig.java` - 安全配置

### 数据模型
- `src/main/java/com/nushungry/userservice/model/User.java` - 用户实体
- `src/main/java/com/nushungry/userservice/model/RefreshToken.java` - 刷新令牌
- `src/main/java/com/nushungry/userservice/model/VerificationCode.java` - 验证码
- `src/main/java/com/nushungry/userservice/model/UserRole.java` - 用户角色枚举

### 数据访问层
- `src/main/java/com/nushungry/userservice/repository/UserRepository.java`
- `src/main/java/com/nushungry/userservice/repository/RefreshTokenRepository.java`
- `src/main/java/com/nushungry/userservice/repository/VerificationCodeRepository.java`

### DTO
- `src/main/java/com/nushungry/userservice/dto/` - 30+ DTO 类 (请求/响应对象)

### 配置文件
- `src/main/resources/application.properties` - 主配置文件
- `src/main/resources/templates/verification-code-email.html` - 验证码邮件模板
- `src/main/resources/templates/password-reset-success-email.html` - 密码重置成功邮件模板

### 构建文件
- `pom.xml` - Maven 项目配置

---

**模块维护者**: User Service Team
**最后更新**: 2025-11-13 10:00:07 UTC
**相关服务**: gateway-service, review-service, preference-service, media-service
