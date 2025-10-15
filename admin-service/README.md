# admin-service

## 项目简介
- `admin-service` 是 NUSHungry 平台的后台管理服务，负责管理员身份认证、用户生命周期管理以及仪表盘数据聚合。
- 借助 Spring Security + JWT 提供接口保护，通过 OpenFeign 与用户中心服务交互，并使用 RabbitMQ 监听用户事件以保持后台数据同步。

## 功能模块
- 管理员认证与鉴权：基于 JWT 的 Bearer Token 校验，定义管理员专属的受保护接口。
- 用户管理：支持分页检索、单用户详情、创建/更新/删除、批量启用禁用、角色调整及密码重置等操作。
- 仪表盘数据：聚合系统运行状态、活跃用户、投诉统计与用户增长趋势等信息，为后台提供可视化指标。
- 事件监听：监听 `user.exchange` 上的用户事件并记录日志，后续可扩展为缓存刷新、审计等动作。
- 监控与文档：暴露 Spring Boot Actuator 端点与 Swagger UI 以便健康检查和接口调试。

## 技术栈与关键依赖
- Spring Boot 3.2.3（Web、Security、Data JPA、Actuator、AMQP）
- Spring Cloud OpenFeign 4.1.0（跨服务调用）
- PostgreSQL 驱动（持久化层示例配置）
- RabbitMQ（AMQP 消息队列）
- jjwt 0.11.5（JWT 生成与解析）
- springdoc-openapi-starter 2.2.0（Swagger UI）
- Lombok 1.18.32（简化样板代码）

## 运行前准备
- 安装 Java 17 与 Maven 3.9+。
- 准备 PostgreSQL 数据库，默认连接信息为 `jdbc:postgresql://localhost:5432/nushungry_db`，请根据环境修改 `spring.datasource.*` 配置。
- 启动 RabbitMQ（默认 `localhost:5672`，guest/guest）。
- 确保 `user-service` 已经在配置的 `user.service.url`（默认 `http://localhost:8081`）上可用，否则 Feign 调用会失败。

## 配置说明
主要配置集中在 `src/main/resources/application.properties`：
- `server.port=8082`：管理后台默认端口。
- `spring.datasource.*`：数据库连接信息，建议在生产环境改为环境变量或外部化配置。
- `jwt.secret`/`jwt.expiration`：JWT 签发密钥与过期时间，请替换为安全值。
- `user.service.url`：用户服务的网关地址。
- `spring.rabbitmq.*`：RabbitMQ 主机、端口与账号。
- 引入 springdoc 后，可通过 `/swagger-ui/index.html` 或 `/v3/api-docs` 访问自动生成的接口文档。

## 快速开始
1. 复制并调整 `application.properties`（或使用 `--spring.config.location` 指向自定义配置）。
2. 在 `admin-service` 根目录执行依赖下载：
   ```bash
   mvn dependency:go-offline
   ```
3. 启动应用：
   ```bash
   mvn spring-boot:run
   ```
   或先构建再运行：
   ```bash
   mvn clean package -DskipTests
   java -jar target/admin-service-0.0.1-SNAPSHOT.jar
   ```
4. 访问健康检查和文档：
   - Actuator: `http://localhost:8082/actuator/health`
   - Swagger UI: `http://localhost:8082/swagger-ui/index.html`

## 常用命令
- 运行测试：`mvn test`
- 重新格式化并校验依赖：`mvn validate`
- 清理构建产物：`mvn clean`

## 接口概览
| 资源 | 方法 & 路径 | 说明 |
| --- | --- | --- |
| 管理员探活 | `GET /api/admin/auth/test` | 验证认证链路是否正常。 |
| 管理仪表盘 | `GET /api/admin/dashboard/stats`<br>`GET /api/admin/dashboard/stats/users`<br>`GET /api/admin/dashboard/user-growth` | 获取整体统计、用户统计及用户增长趋势。 |
| 用户管理 | `GET /api/admin/users`（分页查询）<br>`GET /api/admin/users/{id}`<br>`POST /api/admin/users`<br>`PUT /api/admin/users/{id}`<br>`DELETE /api/admin/users/{id}`<br>`PATCH /api/admin/users/{id}/status`<br>`PUT /api/admin/users/{id}/role`<br>`POST /api/admin/users/{id}/reset-password`<br>`POST /api/admin/users/batch` | 支持 CRUD、状态切换、角色调整、密码重置与批量操作。 |

> 所有受保护接口需携带 `Authorization: Bearer <token>`，并要求用户具备 `ROLE_ADMIN` 权限。

## 消息队列事件
- 交换机：`user.exchange`
- 队列：`user.queue`
- 路由键：`user.routing.key`
- 默认监听器 `UserEventListener` 会记录 `CREATED`、`UPDATED`、`DELETED` 三类用户事件；可在对应方法中扩展缓存刷新、审计、通知等逻辑。

## 开发建议
- 结合 `UserServiceClient` 的 Feign 接口开发联调时，留意参数名与请求体结构需与用户服务保持同步。
- 若将模拟仪表盘数据替换为真实来源，可在 `DashboardService` 中接入数据库或调用其他微服务。
- 建议在生产环境将敏感配置改为环境变量，并为 RabbitMQ 与数据库配置独立的低权限账号。
