# Cafeteria Service 部署文档

## 目录
- [部署架构](#部署架构)
- [环境要求](#环境要求)
- [本地开发部署](#本地开发部署)
- [生产环境部署](#生产环境部署)
- [环境变量配置](#环境变量配置)
- [健康检查和监控](#健康检查和监控)
- [性能优化](#性能优化)
- [故障排查](#故障排查)
- [回滚方案](#回滚方案)

---

## 部署架构

```
┌─────────────────────────────────────────────────────────┐
│                    API Gateway                           │
│              (Spring Cloud Gateway)                      │
└──────────────────────┬──────────────────────────────────┘
                       │
         ┌─────────────┼─────────────┐
         │             │             │
    ┌────▼────┐   ┌────▼────┐  ┌────▼────┐
    │Cafeteria│   │Cafeteria│  │Cafeteria│
    │ Service │   │ Service │  │ Service │
    │Instance1│   │Instance2│  │Instance3│
    └────┬────┘   └────┬────┘  └────┬────┘
         │             │             │
         └─────────────┼─────────────┘
                       │
         ┌─────────────┼─────────────┐
         │             │             │
    ┌────▼────┐   ┌────▼────┐  ┌────▼────┐
    │PostgreSQL│   │RabbitMQ │  │  Redis  │
    │ Primary │   │ Cluster │  │  Cache  │
    └────┬────┘   └─────────┘  └─────────┘
         │
    ┌────▼────┐
    │PostgreSQL│
    │ Replica │
    └─────────┘
```

### 服务职责
- **Cafeteria Service**: 食堂和档口目录服务
- **PostgreSQL**: 存储食堂、档口、图片元数据
- **RabbitMQ**: 监听评价更新事件，同步评分
- **Redis** (可选): 缓存热门食堂和档口数据

---

## 环境要求

### 硬件要求

| 环境 | CPU | 内存 | 磁盘 |
|------|-----|------|------|
| 开发环境 | 2 Core | 4 GB | 20 GB |
| 测试环境 | 4 Core | 8 GB | 50 GB |
| 生产环境 | 8 Core | 16 GB | 200 GB (SSD) |

### 软件要求

- **Java**: JDK 17+
- **Docker**: 20.10+
- **Docker Compose**: 2.0+ (可选)
- **PostgreSQL**: 16+
- **RabbitMQ**: 3.12+
- **Maven**: 3.9+ (构建时)

---

## 本地开发部署

### 方式1: Docker Compose (推荐)

#### 步骤1: 克隆代码
```bash
cd nushungry-Backend/cafeteria-service
```

#### 步骤2: 启动所有服务
```bash
# 启动所有服务
docker-compose up -d

# 或使用启动脚本（Windows）
./scripts/start-services.bat

# 或使用启动脚本（Linux/Mac）
./scripts/start-services.sh
```

查看服务状态:
```bash
docker-compose ps
```

预期输出:
```
NAME                  STATUS              PORTS
cafeteria-service     Up (healthy)        0.0.0.0:8083->8083/tcp
cafeteria-postgres    Up (healthy)        0.0.0.0:5433->5432/tcp
cafeteria-rabbitmq    Up (healthy)        0.0.0.0:5673->5672/tcp, 0.0.0.0:15673->15672/tcp
```

#### 步骤3: 查看日志
```bash
# 查看所有服务日志
docker-compose logs -f

# 查看单个服务日志
docker-compose logs -f cafeteria-service

# 查看数据库初始化日志
docker-compose logs postgres | grep "Cafeteria Service database initialization"
```

#### 步骤4: 验证服务

**健康检查**:
```bash
curl http://localhost:8083/actuator/health
```

预期响应:
```json
{
  "status": "UP",
  "components": {
    "db": {"status": "UP"},
    "rabbitmq": {"status": "UP"},
    "diskSpace": {"status": "UP"}
  }
}
```

**API 文档**:
```bash
# Swagger UI
# Windows
start http://localhost:8083/swagger-ui.html

# Linux/Mac
open http://localhost:8083/swagger-ui.html

# 或使用 curl
curl http://localhost:8083/v3/api-docs
```

**RabbitMQ 管理界面**:
```bash
# 访问管理界面
start http://localhost:15673
# 默认账号: cafeteria / password123
```

**PostgreSQL 连接**:
```bash
psql -h localhost -p 5433 -U cafeteria -d cafeteria_service
# 密码: password123
```

#### 步骤5: 测试 API

**获取所有食堂**:
```bash
curl http://localhost:8083/api/cafeterias
```

预期响应:
```json
[
  {
    "id": 1,
    "name": "Fine Food",
    "location": "Town Plaza",
    "latitude": 1.305,
    "longitude": 103.773,
    "seatingCapacity": 410,
    "averageRating": 0.0,
    "reviewCount": 0,
    "halalInfo": "HALAL FOOD OPTIONS AVAILABLE"
  },
  ...
]
```

**获取食堂详情**:
```bash
curl http://localhost:8083/api/cafeterias/1
```

**获取食堂的所有档口**:
```bash
curl http://localhost:8083/api/cafeterias/2/stalls
```

**搜索档口**:
```bash
# 按名称搜索
curl "http://localhost:8083/api/stalls/search?keyword=chinese"

# 按菜系类型搜索
curl "http://localhost:8083/api/stalls/search?cuisineType=Chinese"
```

**获取热门档口**:
```bash
curl "http://localhost:8083/api/stalls/popular?limit=10"
```

#### 步骤6: 测试 RabbitMQ 事件监听

发送评价更新事件：
```bash
# 使用 RabbitMQ 管理界面发送测试消息
curl -X POST http://localhost:15673/api/exchanges/%2Fcafeteria/review.events/publish \
  -u cafeteria:password123 \
  -H "Content-Type: application/json" \
  -d '{
    "properties": {
      "content_type": "application/json"
    },
    "routing_key": "review.rating.updated",
    "payload": "{\"stallId\":1,\"newAverageRating\":4.5,\"newReviewCount\":10}",
    "payload_encoding": "string"
  }'
```

验证评分更新：
```bash
curl http://localhost:8083/api/stalls/1 | jq '.averageRating, .reviewCount'
```

#### 步骤7: 停止服务
```bash
# 使用停止脚本（Windows）
./scripts/stop-services.bat

# 或使用停止脚本（Linux/Mac）
./scripts/stop-services.sh

# 或使用 docker-compose
# 停止但保留数据
docker-compose stop

# 停止并删除容器（保留数据卷）
docker-compose down

# 停止并删除所有数据
docker-compose down -v
```

---

### 方式2: 本地 Maven 运行

#### 前置条件
确保本地已安装并启动:
- PostgreSQL (端口 5433)
- RabbitMQ (端口 5673)

#### 步骤1: 初始化数据库
```bash
# 创建数据库
psql -h localhost -p 5433 -U postgres -c "CREATE DATABASE cafeteria_service;"

# 创建用户（如果不存在）
psql -h localhost -p 5433 -U postgres -c "CREATE USER cafeteria WITH PASSWORD 'password123';"
psql -h localhost -p 5433 -U postgres -c "GRANT ALL PRIVILEGES ON DATABASE cafeteria_service TO cafeteria;"

# 执行初始化脚本
psql -h localhost -p 5433 -U cafeteria -d cafeteria_service -f scripts/init_cafeteria_db.sql
```

#### 步骤2: 配置环境变量
```bash
# Windows (PowerShell)
$env:DB_HOST="localhost"
$env:DB_PORT="5433"
$env:DB_NAME="cafeteria_service"
$env:DB_USERNAME="cafeteria"
$env:DB_PASSWORD="password123"
$env:RABBITMQ_HOST="localhost"
$env:RABBITMQ_PORT="5673"
$env:RABBITMQ_USERNAME="cafeteria"
$env:RABBITMQ_PASSWORD="password123"
$env:RABBITMQ_VHOST="/cafeteria"

# Linux/Mac
export DB_HOST=localhost
export DB_PORT=5433
export DB_NAME=cafeteria_service
export DB_USERNAME=cafeteria
export DB_PASSWORD=password123
export RABBITMQ_HOST=localhost
export RABBITMQ_PORT=5673
export RABBITMQ_USERNAME=cafeteria
export RABBITMQ_PASSWORD=password123
export RABBITMQ_VHOST=/cafeteria
```

#### 步骤3: 构建和运行
```bash
# 构建项目
mvn clean package -DskipTests

# 运行应用
java -jar target/cafeteria-service-0.0.1-SNAPSHOT.jar

# 或使用 Maven 插件运行
mvn spring-boot:run
```

#### 步骤4: 验证服务
```bash
curl http://localhost:8083/actuator/health
```

---

## 生产环境部署

### 方式1: Docker 部署

#### 步骤1: 构建 Docker 镜像
```bash
cd cafeteria-service

# 构建镜像
docker build -t nushungry/cafeteria-service:1.0.0 .

# 推送到 Docker Hub（可选）
docker tag nushungry/cafeteria-service:1.0.0 nushungry/cafeteria-service:latest
docker push nushungry/cafeteria-service:1.0.0
docker push nushungry/cafeteria-service:latest
```

#### 步骤2: 运行容器
```bash
# 创建网络
docker network create nushungry-network

# 启动 PostgreSQL
docker run -d \
  --name cafeteria-postgres \
  --network nushungry-network \
  -e POSTGRES_DB=cafeteria_service \
  -e POSTGRES_USER=cafeteria \
  -e POSTGRES_PASSWORD=<STRONG_PASSWORD> \
  -v cafeteria-postgres-data:/var/lib/postgresql/data \
  -p 5433:5432 \
  postgres:16-alpine

# 启动 RabbitMQ
docker run -d \
  --name cafeteria-rabbitmq \
  --network nushungry-network \
  -e RABBITMQ_DEFAULT_USER=cafeteria \
  -e RABBITMQ_DEFAULT_PASS=<STRONG_PASSWORD> \
  -e RABBITMQ_DEFAULT_VHOST=/cafeteria \
  -p 5673:5672 \
  -p 15673:15672 \
  rabbitmq:3.12-management-alpine

# 启动 Cafeteria Service
docker run -d \
  --name cafeteria-service \
  --network nushungry-network \
  -e DB_HOST=cafeteria-postgres \
  -e DB_PORT=5432 \
  -e DB_NAME=cafeteria_service \
  -e DB_USERNAME=cafeteria \
  -e DB_PASSWORD=<STRONG_PASSWORD> \
  -e RABBITMQ_HOST=cafeteria-rabbitmq \
  -e RABBITMQ_PORT=5672 \
  -e RABBITMQ_USERNAME=cafeteria \
  -e RABBITMQ_PASSWORD=<STRONG_PASSWORD> \
  -e RABBITMQ_VHOST=/cafeteria \
  -e SPRING_PROFILES_ACTIVE=prod \
  -p 8083:8083 \
  nushungry/cafeteria-service:1.0.0
```

---

### 方式2: AWS ECS 部署（推荐生产环境）

#### 前提条件
- AWS CLI 配置
- ECR 仓库创建
- ECS 集群创建
- RDS PostgreSQL 实例
- Amazon MQ (RabbitMQ)

#### 步骤1: 推送镜像到 ECR
```bash
# 登录 ECR
aws ecr get-login-password --region ap-southeast-1 | \
  docker login --username AWS --password-stdin <AWS_ACCOUNT_ID>.dkr.ecr.ap-southeast-1.amazonaws.com

# 构建并推送
docker build -t cafeteria-service .
docker tag cafeteria-service:latest <AWS_ACCOUNT_ID>.dkr.ecr.ap-southeast-1.amazonaws.com/cafeteria-service:latest
docker push <AWS_ACCOUNT_ID>.dkr.ecr.ap-southeast-1.amazonaws.com/cafeteria-service:latest
```

#### 步骤2: 创建 Task Definition
创建 `task-definition.json`:

```json
{
  "family": "cafeteria-service",
  "networkMode": "awsvpc",
  "requiresCompatibilities": ["FARGATE"],
  "cpu": "1024",
  "memory": "2048",
  "executionRoleArn": "arn:aws:iam::<AWS_ACCOUNT_ID>:role/ecsTaskExecutionRole",
  "taskRoleArn": "arn:aws:iam::<AWS_ACCOUNT_ID>:role/ecsTaskRole",
  "containerDefinitions": [
    {
      "name": "cafeteria-service",
      "image": "<AWS_ACCOUNT_ID>.dkr.ecr.ap-southeast-1.amazonaws.com/cafeteria-service:latest",
      "cpu": 1024,
      "memory": 2048,
      "portMappings": [
        {
          "containerPort": 8083,
          "protocol": "tcp"
        }
      ],
      "essential": true,
      "environment": [
        {"name": "SPRING_PROFILES_ACTIVE", "value": "prod"},
        {"name": "DB_HOST", "value": "cafeteria-db.xxxxx.ap-southeast-1.rds.amazonaws.com"},
        {"name": "DB_PORT", "value": "5432"},
        {"name": "DB_NAME", "value": "cafeteria_service"},
        {"name": "RABBITMQ_HOST", "value": "b-xxxxx.mq.ap-southeast-1.amazonaws.com"}
      ],
      "secrets": [
        {"name": "DB_USERNAME", "valueFrom": "arn:aws:secretsmanager:ap-southeast-1:<AWS_ACCOUNT_ID>:secret:cafeteria-db-user"},
        {"name": "DB_PASSWORD", "valueFrom": "arn:aws:secretsmanager:ap-southeast-1:<AWS_ACCOUNT_ID>:secret:cafeteria-db-pass"},
        {"name": "RABBITMQ_USERNAME", "valueFrom": "arn:aws:secretsmanager:ap-southeast-1:<AWS_ACCOUNT_ID>:secret:rabbitmq-user"},
        {"name": "RABBITMQ_PASSWORD", "valueFrom": "arn:aws:secretsmanager:ap-southeast-1:<AWS_ACCOUNT_ID>:secret:rabbitmq-pass"}
      ],
      "logConfiguration": {
        "logDriver": "awslogs",
        "options": {
          "awslogs-group": "/ecs/cafeteria-service",
          "awslogs-region": "ap-southeast-1",
          "awslogs-stream-prefix": "ecs"
        }
      },
      "healthCheck": {
        "command": ["CMD-SHELL", "curl -f http://localhost:8083/actuator/health || exit 1"],
        "interval": 30,
        "timeout": 5,
        "retries": 3,
        "startPeriod": 60
      }
    }
  ]
}
```

#### 步骤3: 注册 Task Definition
```bash
aws ecs register-task-definition --cli-input-json file://task-definition.json
```

#### 步骤4: 创建或更新 Service
```bash
aws ecs create-service \
  --cluster nushungry-cluster \
  --service-name cafeteria-service \
  --task-definition cafeteria-service \
  --desired-count 3 \
  --launch-type FARGATE \
  --network-configuration "awsvpcConfiguration={subnets=[subnet-xxxxx,subnet-yyyyy],securityGroups=[sg-xxxxx],assignPublicIp=DISABLED}" \
  --load-balancers "targetGroupArn=arn:aws:elasticloadbalancing:ap-southeast-1:<AWS_ACCOUNT_ID>:targetgroup/cafeteria-service-tg,containerName=cafeteria-service,containerPort=8083"
```

---

## 环境变量配置

### 必需环境变量

| 变量名 | 说明 | 默认值 | 示例 |
|--------|------|--------|------|
| `DB_HOST` | PostgreSQL 主机 | localhost | cafeteria-postgres |
| `DB_PORT` | PostgreSQL 端口 | 5432 | 5433 |
| `DB_NAME` | 数据库名称 | cafeteria_service | cafeteria_service |
| `DB_USERNAME` | 数据库用户名 | cafeteria | cafeteria |
| `DB_PASSWORD` | 数据库密码 | - | password123 |
| `RABBITMQ_HOST` | RabbitMQ 主机 | localhost | cafeteria-rabbitmq |
| `RABBITMQ_PORT` | RabbitMQ 端口 | 5672 | 5673 |
| `RABBITMQ_USERNAME` | RabbitMQ 用户名 | cafeteria | cafeteria |
| `RABBITMQ_PASSWORD` | RabbitMQ 密码 | - | password123 |
| `RABBITMQ_VHOST` | RabbitMQ 虚拟主机 | /cafeteria | /cafeteria |

### 可选环境变量

| 变量名 | 说明 | 默认值 |
|--------|------|--------|
| `SPRING_PROFILES_ACTIVE` | Spring Profile | dev |
| `JAVA_OPTS` | JVM 参数 | -Xms512m -Xmx1024m |
| `SWAGGER_ENABLED` | 启用 Swagger | true |
| `ALLOWED_ORIGINS` | CORS 允许的源 | * |
| `REDIS_HOST` | Redis 主机（缓存） | - |
| `REDIS_PORT` | Redis 端口 | 6379 |

---

## 健康检查和监控

### 健康检查端点

```bash
# 总体健康状态
curl http://localhost:8083/actuator/health

# 详细健康信息
curl http://localhost:8083/actuator/health/details

# 数据库连接状态
curl http://localhost:8083/actuator/health/db

# RabbitMQ 连接状态
curl http://localhost:8083/actuator/health/rabbitmq
```

### 指标端点

```bash
# Prometheus 指标
curl http://localhost:8083/actuator/prometheus

# 应用信息
curl http://localhost:8083/actuator/info

# JVM 指标
curl http://localhost:8083/actuator/metrics/jvm.memory.used
```

### 日志查看

```bash
# Docker 日志
docker logs -f cafeteria-service --tail=100

# Docker Compose 日志
docker-compose logs -f cafeteria-service

# 日志文件（容器内）
docker exec cafeteria-service tail -f /var/log/cafeteria-service/application.log
```

---

## 性能优化

### 数据库优化

```sql
-- 分析查询性能
EXPLAIN ANALYZE SELECT * FROM stall WHERE cafeteria_id = 2;

-- 创建复合索引
CREATE INDEX idx_stall_cafeteria_rating ON stall(cafeteria_id, average_rating DESC);

-- 更新统计信息
ANALYZE cafeteria;
ANALYZE stall;
ANALYZE image;

-- 查看索引使用情况
SELECT schemaname, tablename, indexname, idx_scan, idx_tup_read, idx_tup_fetch
FROM pg_stat_user_indexes
ORDER BY idx_scan DESC;
```

### Redis 缓存配置（可选）

```yaml
# application-prod.yml
spring:
  cache:
    type: redis
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      timeout: 2000ms
      lettuce:
        pool:
          max-active: 8
          max-idle: 8
          min-idle: 0
```

### JVM 调优

```bash
# 生产环境 JVM 参数
export JAVA_OPTS="-Xms2g -Xmx2g \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200 \
  -XX:ParallelGCThreads=4 \
  -XX:ConcGCThreads=2 \
  -XX:InitiatingHeapOccupancyPercent=45 \
  -XX:+HeapDumpOnOutOfMemoryError \
  -XX:HeapDumpPath=/var/log/cafeteria-service/heap-dump.hprof \
  -Dcom.sun.management.jmxremote \
  -Dcom.sun.management.jmxremote.port=9083 \
  -Dcom.sun.management.jmxremote.authenticate=false \
  -Dcom.sun.management.jmxremote.ssl=false"
```

---

## 故障排查

### 问题1: 服务无法启动

**症状**: 容器反复重启

**排查步骤**:
```bash
# 查看日志
docker logs cafeteria-service

# 检查健康检查
docker inspect cafeteria-service | jq '.[0].State.Health'

# 检查环境变量
docker exec cafeteria-service env | grep -E "DB|RABBITMQ"
```

**常见原因**:
- 数据库连接失败
- RabbitMQ 连接失败
- 端口已被占用

---

### 问题2: 数据库连接失败

**症状**: `Unable to acquire JDBC Connection`

**解决方案**:
```bash
# 检查数据库是否启动
docker ps | grep postgres

# 测试数据库连接
psql -h localhost -p 5433 -U cafeteria -d cafeteria_service

# 检查防火墙
netstat -an | grep 5433
```

---

### 问题3: RabbitMQ 消息未被消费

**症状**: 评分未更新

**排查步骤**:
```bash
# 检查队列
curl -u cafeteria:password123 http://localhost:15673/api/queues/%2Fcafeteria

# 查看消息堆积
curl -u cafeteria:password123 http://localhost:15673/api/queues/%2Fcafeteria/review.rating.updated

# 检查消费者
curl -u cafeteria:password123 http://localhost:15673/api/consumers/%2Fcafeteria
```

---

### 问题4: API 响应缓慢

**排查步骤**:
```bash
# 查看数据库慢查询
SELECT pid, now() - query_start AS duration, query
FROM pg_stat_activity
WHERE state = 'active' AND now() - query_start > interval '5 seconds';

# 检查连接池
curl http://localhost:8083/actuator/metrics/hikaricp.connections.active

# 查看 JVM 内存
curl http://localhost:8083/actuator/metrics/jvm.memory.used
```

---

## 回滚方案

### Docker 部署回滚

```bash
# 停止当前版本
docker stop cafeteria-service
docker rm cafeteria-service

# 启动旧版本
docker run -d \
  --name cafeteria-service \
  --network nushungry-network \
  -e DB_HOST=... \
  -p 8083:8083 \
  nushungry/cafeteria-service:0.9.0  # 旧版本
```

### ECS 部署回滚

```bash
# 回滚到上一个稳定版本
aws ecs update-service \
  --cluster nushungry-cluster \
  --service cafeteria-service \
  --task-definition cafeteria-service:10  # 指定旧的 Task Definition 版本
```

### 数据库回滚

```sql
-- 恢复备份
pg_restore -h localhost -p 5433 -U cafeteria -d cafeteria_service \
  --clean --if-exists \
  /backups/cafeteria_service_backup_20250119.dump
```

---

## 联系与支持

如遇到部署问题，请联系：
- **技术支持**: tech@nushungry.com
- **API 文档**: http://localhost:8083/swagger-ui.html
- **架构文档**: [ARCHITECTURE.md](../../docs/ARCHITECTURE.md)
- **迁移指南**: [MIGRATION_GUIDE.md](./scripts/MIGRATION_GUIDE.md)

---

**最后更新**: 2025-01-19
**版本**: 1.0.0
