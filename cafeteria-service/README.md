# cafeteria-service

## 项目简介
- `cafeteria-service` 是 NUSHungry 平台的食堂/档口服务，负责管理食堂、档口、菜品信息、营业时间、评分与评论等功能，并对外提供搜索与列表接口供前端与其他微服务调用。
- 服务提供对档口菜单的 CRUD、档口状态管理、搜索与分页，以及对餐厅评分与评论的读取接口。部分写操作可能需要鉴权（由 API 网关或用户服务控制）。

## 功能模块
- 档口管理：创建、更新、删除档口，配置档口营业时间、位置与标签。
- 菜品管理：档口下菜品的增删改查，支持菜品图片与分类管理。
- 菜单与库存：支持按天/时段的菜单发布与简单库存标记。
- 搜索与分页：按关键字、食堂、标签、评分等条件搜索档口与菜品，并支持分页排序。
- 评论与评分：展示档口评分统计与近期评论（只读接口），评论事件可由 `review-service` 或消息队列消费后入库。

## 技术栈与关键依赖
- Spring Boot 3.x（Web、Data JPA、Actuator）
- PostgreSQL 驱动（持久化层示例配置）
- RabbitMQ（AMQP 消息队列，用于评论/评分事件或档口变更通知）
- Spring Cloud OpenFeign（如需调用其他微服务）
- springdoc-openapi-starter（Swagger UI）

## 运行前准备
- 安装 Java 17 与 Maven 3.9+。
- 准备 PostgreSQL 数据库，默认连接信息建议为 `jdbc:postgresql://localhost:5432/nushungry_db`，请根据环境修改 `spring.datasource.*` 配置。
- 启动 RabbitMQ（默认 `localhost:5672`，guest/guest）。
- 如需与用户服务或管理员服务联调，确保对应服务在配置的 `user.service.url` 或网关地址上可用。

## 配置说明
主要配置集中在 `src/main/resources/application.properties`（或 `application.yml`）：
- `server.port`：默认端口（请根据实际项目检查并替换）。
- `spring.datasource.*`：数据库连接信息，建议在生产环境改为环境变量或外部化配置。
- `user.service.url`：如果使用 Feign 调用用户/鉴权服务，请在配置中设置正确地址。
- `spring.rabbitmq.*`：RabbitMQ 主机、端口与账号。
- 引入 springdoc 后，可通过 `/swagger-ui/index.html` 或 `/v3/api-docs` 访问自动生成的接口文档。

注：仓库中默认配置可能位于 `src/main/resources/application.properties` 或 `target/classes/application.properties`，请以源码下的配置为准。

## 快速开始
1. 复制并调整 `application.properties`（或使用 `--spring.config.location` 指向自定义配置）。
2. 在 `cafeteria-service` 根目录执行依赖下载：
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
   java -jar target/cafeteria-service-0.0.1-SNAPSHOT.jar
   ```
4. 访问健康检查和文档：
   - Actuator: `http://localhost:8082/actuator/health`
   - Swagger UI: `http://localhost:8082/swagger-ui/index.html`

## 常用命令
- 运行测试：`mvn test`
- 重新格式化并校验依赖：`mvn validate`
- 清理构建产物：`mvn clean`

## 接口概览
公共（读）接口：
| 档口列表 | `GET /api/cafeterias` | 返回所有食堂/档口的列表（当前实现为不带分页的列表）。 |
| 档口详情 | `GET /api/cafeterias/{id}` | 根据 ID 返回单个档口详情，404 时返回 NotFound。 |
| 指定食堂的档口列表 | `GET /api/cafeterias/{id}/stalls` | 返回某个食堂下的所有档口。 |
| 档口（档位）列表 | `GET /api/stalls` | 返回所有档口（stall）的列表。 |
| 档口（档位）详情 | `GET /api/stalls/{id}` | 根据 stall ID 返回档口详情，404 时返回 NotFound。 |

管理员（写）接口（位于 `AdminCafeteriaController`）：
| 新建档口 | `POST /api/admin/cafeterias` | 创建新的档口/食堂资源。 | JSON 格式的 `Cafeteria` 对象 | 201 Created，返回 JSON 包含 `success` 与 `cafeteria` 字段（当前实现返回 Map）。 |
| 更新档口 | `PUT /api/admin/cafeterias/{id}` | 更新指定 ID 的档口；若不存在返回 404。 | JSON 格式的 `Cafeteria` 对象 | 200 OK（或 404 Not Found），返回 Map 包含 `success` 与 `cafeteria` 或错误信息。 |
| 删除档口 | `DELETE /api/admin/cafeterias/{id}` | 删除指定 ID 的档口（当前实现不返回错误当资源不存在时；建议在上层鉴权/校验）。 | 无 | 200 OK，返回 Map {"success": true}。 |

实现细节提示：
- `GET /api/cafeterias` 与 `GET /api/stalls` 在当前实现中返回完整列表（非分页），如需分页/筛选，请在对应 Service/Controller 中扩展查询参数（page, size, sort, filter 等）。
- 管理接口返回类型为 `Map<String, Object>`，包含 `success` 字段与 `cafeteria`（created/updated）对象；如果你需要标准化响应，可以考虑引入统一的响应 DTO。


## 消息队列事件
- 交换机/队列：建议与其他服务保持一致的命名约定，例如 `review.exchange`、`review.queue`、`review.routing.key`。
- 默认监听器可消费评论创建/更新事件并更新评分汇总或缓存。

## 开发建议
- 在实现与 `user-service` 的联调时，确认 Feign 接口的请求格式与权限边界。
- 若需要更精细的库存或销量统计，可将简单库存标记扩展为事件驱动的库存服务或引入 Kafka 以处理高吞吐量数据。
- 建议在生产环境将敏感配置改为环境变量，并为 RabbitMQ 与数据库配置独立的低权限账号。

