# Admin Service 部署文档

## 目录
- [部署架构](#部署架构)
- [环境要求](#环境要求)
- [本地开发部署](#本地开发部署)
- [生产环境部署](#生产环境部署)
- [环境变量配置](#环境变量配置)
- [健康检查和监控](#健康检查和监控)
- [安全配置](#安全配置)
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
    │  Admin  │   │  Admin  │  │  Admin  │
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
cd nushungry-Backend/admin-service
```

#### 步骤2: 启动所有服务
```bash
docker-compose up -d
```

查看服务状态:
```bash
docker-compose ps
```

预期输出:
```
NAME                STATUS              PORTS
admin-service       Up (healthy)        0.0.0.0:8082->8082/tcp
admin-postgres      Up (healthy)        0.0.0.0:5432->5432/tcp
admin-rabbitmq      Up (healthy)        0.0.0.0:5672->5672/tcp, 0.0.0.0:15672->15672/tcp
```

#### 步骤3: 查看日志
```bash
# 查看所有服务日志
docker-compose logs -f

# 查看单个服务日志
docker-compose logs -f admin-service
```

#### 步骤4: 验证服务

**健康检查**:
```bash
curl http://localhost:8082/actuator/health
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
open http://localhost:8082/swagger-ui.html

# API Docs JSON
curl http://localhost:8082/v3/api-docs
```

**RabbitMQ 管理界面**:
```bash
open http://localhost:15672
# 默认账号: admin / password123
```

**PostgreSQL 连接**:
```bash
psql -h localhost -p 5432 -U admin -d admin_service
# 密码: password123
```

#### 步骤5: 测试管理员登录
```bash
# 使用默认管理员账号登录
curl -X POST http://localhost:8082/api/admin/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "Admin123!"
  }'
```

预期响应:
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "username": "admin",
  "role": "ROLE_ADMIN",
  "expiresIn": 86400
}
```

#### 步骤6: 停止服务
```bash
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
- PostgreSQL (端口 5432)
- RabbitMQ (端口 5672)

#### 步骤1: 初始化数据库
```bash
psql -h localhost -U postgres -c "CREATE DATABASE admin_service;"
psql -h localhost -U admin -d admin_service -f scripts/init_admin_db.sql
```

#### 步骤2: 配置环境变量
```bash
export DB_HOST=localhost
export DB_PORT=5432
export DB_NAME=admin_service
export DB_USERNAME=admin
export DB_PASSWORD=password123
export RABBITMQ_HOST=localhost
export RABBITMQ_PORT=5672
export JWT_SECRET=your-secret-key-for-development
```

#### 步骤3: 构建并运行
```bash
# 构建
mvn clean package -DskipTests

# 运行
java -jar target/admin-service-0.0.1-SNAPSHOT.jar
```

或使用 Maven 插件:
```bash
mvn spring-boot:run
```

---

## 生产环境部署

### 方式1: Docker 部署

#### 步骤1: 构建镜像
```bash
# 构建生产镜像
docker build -t nushungry/admin-service:latest .

# 标记版本
docker tag nushungry/admin-service:latest nushungry/admin-service:v1.0.0
```

#### 步骤2: 推送到容器仓库
```bash
# Docker Hub
docker push nushungry/admin-service:latest
docker push nushungry/admin-service:v1.0.0

# AWS ECR
aws ecr get-login-password --region ap-southeast-1 | docker login --username AWS --password-stdin <account-id>.dkr.ecr.ap-southeast-1.amazonaws.com
docker tag nushungry/admin-service:latest <account-id>.dkr.ecr.ap-southeast-1.amazonaws.com/admin-service:latest
docker push <account-id>.dkr.ecr.ap-southeast-1.amazonaws.com/admin-service:latest
```

#### 步骤3: 部署到服务器
```bash
# 拉取镜像
docker pull nushungry/admin-service:latest

# 运行容器
docker run -d \
  --name admin-service \
  -p 8082:8082 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e DB_HOST=postgres.prod.local \
  -e DB_USERNAME=admin \
  -e DB_PASSWORD=${DB_PASSWORD} \
  -e RABBITMQ_HOST=rabbitmq.prod.local \
  -e JWT_SECRET=${JWT_SECRET} \
  --restart unless-stopped \
  nushungry/admin-service:latest
```

---

### 方式2: AWS ECS 部署

#### 步骤1: 创建 Task Definition

创建文件 `.aws/task-definitions/admin-service.json`:

```json
{
  "family": "admin-service",
  "networkMode": "awsvpc",
  "requiresCompatibilities": ["FARGATE"],
  "cpu": "1024",
  "memory": "2048",
  "executionRoleArn": "arn:aws:iam::ACCOUNT_ID:role/ecsTaskExecutionRole",
  "taskRoleArn": "arn:aws:iam::ACCOUNT_ID:role/ecsTaskRole",
  "containerDefinitions": [
    {
      "name": "admin-service",
      "image": "ACCOUNT_ID.dkr.ecr.ap-southeast-1.amazonaws.com/admin-service:latest",
      "portMappings": [
        {
          "containerPort": 8082,
          "protocol": "tcp"
        }
      ],
      "environment": [
        {"name": "SPRING_PROFILES_ACTIVE", "value": "prod"},
        {"name": "DB_HOST", "value": "admin-db.cluster-xxx.ap-southeast-1.rds.amazonaws.com"},
        {"name": "DB_PORT", "value": "5432"},
        {"name": "DB_NAME", "value": "admin_service"},
        {"name": "RABBITMQ_HOST", "value": "b-xxx.mq.ap-southeast-1.amazonaws.com"},
        {"name": "RABBITMQ_PORT", "value": "5671"}
      ],
      "secrets": [
        {"name": "DB_USERNAME", "valueFrom": "arn:aws:secretsmanager:ap-southeast-1:ACCOUNT_ID:secret:admin-db-username"},
        {"name": "DB_PASSWORD", "valueFrom": "arn:aws:secretsmanager:ap-southeast-1:ACCOUNT_ID:secret:admin-db-password"},
        {"name": "JWT_SECRET", "valueFrom": "arn:aws:secretsmanager:ap-southeast-1:ACCOUNT_ID:secret:admin-jwt-secret"},
        {"name": "RABBITMQ_USERNAME", "valueFrom": "arn:aws:secretsmanager:ap-southeast-1:ACCOUNT_ID:secret:rabbitmq-username"},
        {"name": "RABBITMQ_PASSWORD", "valueFrom": "arn:aws:secretsmanager:ap-southeast-1:ACCOUNT_ID:secret:rabbitmq-password"}
      ],
      "logConfiguration": {
        "logDriver": "awslogs",
        "options": {
          "awslogs-group": "/ecs/admin-service",
          "awslogs-region": "ap-southeast-1",
          "awslogs-stream-prefix": "ecs"
        }
      },
      "healthCheck": {
        "command": ["CMD-SHELL", "curl -f http://localhost:8082/actuator/health || exit 1"],
        "interval": 30,
        "timeout": 5,
        "retries": 3,
        "startPeriod": 60
      }
    }
  ]
}
```

#### 步骤2: 注册 Task Definition
```bash
aws ecs register-task-definition \
  --cli-input-json file://.aws/task-definitions/admin-service.json
```

#### 步骤3: 创建 ECS Service
```bash
aws ecs create-service \
  --cluster nushungry-cluster \
  --service-name admin-service \
  --task-definition admin-service:1 \
  --desired-count 2 \
  --launch-type FARGATE \
  --network-configuration "awsvpcConfiguration={subnets=[subnet-xxx,subnet-yyy],securityGroups=[sg-xxx],assignPublicIp=DISABLED}" \
  --load-balancers "targetGroupArn=arn:aws:elasticloadbalancing:ap-southeast-1:ACCOUNT_ID:targetgroup/admin-service/xxx,containerName=admin-service,containerPort=8082"
```

#### 步骤4: 配置自动扩展
```bash
# 注册可扩展目标
aws application-autoscaling register-scalable-target \
  --service-namespace ecs \
  --scalable-dimension ecs:service:DesiredCount \
  --resource-id service/nushungry-cluster/admin-service \
  --min-capacity 2 \
  --max-capacity 10

# 创建扩展策略（基于 CPU）
aws application-autoscaling put-scaling-policy \
  --service-namespace ecs \
  --scalable-dimension ecs:service:DesiredCount \
  --resource-id service/nushungry-cluster/admin-service \
  --policy-name cpu-scaling-policy \
  --policy-type TargetTrackingScaling \
  --target-tracking-scaling-policy-configuration file://scaling-policy.json
```

`scaling-policy.json`:
```json
{
  "TargetValue": 70.0,
  "PredefinedMetricSpecification": {
    "PredefinedMetricType": "ECSServiceAverageCPUUtilization"
  },
  "ScaleInCooldown": 300,
  "ScaleOutCooldown": 60
}
```

---

## 环境变量配置

### 必需环境变量

| 变量名 | 描述 | 示例 | 默认值 |
|--------|------|------|--------|
| `SPRING_PROFILES_ACTIVE` | Spring Profile | `prod` | `dev` |
| `DB_HOST` | PostgreSQL 主机 | `postgres.prod.local` | `localhost` |
| `DB_PORT` | PostgreSQL 端口 | `5432` | `5432` |
| `DB_NAME` | 数据库名称 | `admin_service` | `admin_service` |
| `DB_USERNAME` | 数据库用户名 | `admin` | `admin` |
| `DB_PASSWORD` | 数据库密码 | `***` | - |
| `RABBITMQ_HOST` | RabbitMQ 主机 | `rabbitmq.prod.local` | `localhost` |
| `RABBITMQ_PORT` | RabbitMQ 端口 | `5672` | `5672` |
| `RABBITMQ_USERNAME` | RabbitMQ 用户名 | `admin` | `guest` |
| `RABBITMQ_PASSWORD` | RabbitMQ 密码 | `***` | `guest` |
| `JWT_SECRET` | JWT 密钥 | `***` | - |
| `JWT_EXPIRATION` | JWT 过期时间（毫秒） | `86400000` | `86400000` (24h) |

### 可选环境变量

| 变量名 | 描述 | 默认值 |
|--------|------|--------|
| `JAVA_OPTS` | JVM 参数 | `-Xms512m -Xmx1024m` |
| `SWAGGER_ENABLED` | 启用 Swagger | `false` (生产环境) |
| `ALLOWED_ORIGINS` | CORS 允许的域名 | `*` |
| `LOG_LEVEL` | 日志级别 | `INFO` |
| `MAX_POOL_SIZE` | 数据库连接池大小 | `10` |

### 环境变量文件示例

创建 `.env` 文件（⚠️ 不要提交到 Git）:

```bash
# Database
DB_HOST=postgres.prod.local
DB_PORT=5432
DB_NAME=admin_service
DB_USERNAME=admin
DB_PASSWORD=super-secret-password

# RabbitMQ
RABBITMQ_HOST=rabbitmq.prod.local
RABBITMQ_PORT=5672
RABBITMQ_USERNAME=admin
RABBITMQ_PASSWORD=rabbitmq-secret

# Security
JWT_SECRET=your-256-bit-secret-key-change-in-production
JWT_EXPIRATION=86400000

# Application
SPRING_PROFILES_ACTIVE=prod
SWAGGER_ENABLED=false
ALLOWED_ORIGINS=https://nushungry.com,https://admin.nushungry.com

# JVM
JAVA_OPTS=-Xms1g -Xmx2g -XX:+UseG1GC -XX:MaxGCPauseMillis=200
```

使用环境变量文件:
```bash
docker-compose --env-file .env up -d
```

---

## 健康检查和监控

### 健康检查端点

#### 1. 基础健康检查
```bash
curl http://localhost:8082/actuator/health
```

响应示例:
```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP",
      "details": {
        "database": "PostgreSQL",
        "validationQuery": "isValid()"
      }
    },
    "diskSpace": {
      "status": "UP",
      "details": {
        "total": 499963174912,
        "free": 375123582976,
        "threshold": 10485760
      }
    },
    "ping": {
      "status": "UP"
    },
    "rabbitmq": {
      "status": "UP",
      "details": {
        "version": "3.12.0"
      }
    }
  }
}
```

#### 2. 详细健康信息
```bash
curl http://localhost:8082/actuator/health/db
curl http://localhost:8082/actuator/health/rabbitmq
```

#### 3. 应用信息
```bash
curl http://localhost:8082/actuator/info
```

#### 4. 性能指标
```bash
# JVM 内存使用
curl http://localhost:8082/actuator/metrics/jvm.memory.used

# HTTP 请求统计
curl http://localhost:8082/actuator/metrics/http.server.requests

# 数据库连接池
curl http://localhost:8082/actuator/metrics/hikaricp.connections.active
```

### Prometheus 监控

#### 启用 Prometheus 端点
在 `application.yml` 中配置:
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus,metrics
  metrics:
    export:
      prometheus:
        enabled: true
```

#### 抓取指标
```bash
curl http://localhost:8082/actuator/prometheus
```

#### Prometheus 配置
`prometheus.yml`:
```yaml
scrape_configs:
  - job_name: 'admin-service'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['admin-service:8082']
```

### 日志监控

#### 查看实时日志
```bash
# Docker 日志
docker logs -f admin-service

# 文件日志
tail -f logs/admin-service.log
```

#### 日志级别动态调整
```bash
# 设置 DEBUG 级别
curl -X POST http://localhost:8082/actuator/loggers/com.nushungry \
  -H "Content-Type: application/json" \
  -d '{"configuredLevel":"DEBUG"}'

# 恢复 INFO 级别
curl -X POST http://localhost:8082/actuator/loggers/com.nushungry \
  -H "Content-Type: application/json" \
  -d '{"configuredLevel":"INFO"}'
```

---

## 安全配置

### 1. JWT 密钥管理

⚠️ **生产环境必须修改默认密钥！**

生成安全的 JWT 密钥:
```bash
# 方法 1: OpenSSL
openssl rand -base64 64

# 方法 2: Python
python3 -c "import secrets; print(secrets.token_urlsafe(64))"
```

### 2. 数据库密码

使用 AWS Secrets Manager 或环境变量:
```bash
# 存储到 AWS Secrets Manager
aws secretsmanager create-secret \
  --name admin-db-password \
  --secret-string "your-secure-password"

# 在 ECS Task Definition 中引用
"secrets": [
  {
    "name": "DB_PASSWORD",
    "valueFrom": "arn:aws:secretsmanager:region:account:secret:admin-db-password"
  }
]
```

### 3. HTTPS 配置

#### 使用 Nginx 反向代理
```nginx
server {
    listen 443 ssl http2;
    server_name admin.nushungry.com;

    ssl_certificate /etc/nginx/ssl/cert.pem;
    ssl_certificate_key /etc/nginx/ssl/key.pem;

    location / {
        proxy_pass http://admin-service:8082;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

### 4. 防火墙规则

仅允许必要的端口:
```bash
# 允许 HTTPS
ufw allow 443/tcp

# 允许内部服务通信
ufw allow from 10.0.0.0/8 to any port 8082
ufw allow from 10.0.0.0/8 to any port 5432
ufw allow from 10.0.0.0/8 to any port 5672
```

---

## 故障排查

### 问题 1: 服务无法启动

**症状**: 容器启动后立即退出

**排查步骤**:
```bash
# 查看容器日志
docker logs admin-service

# 查看退出代码
docker inspect admin-service --format='{{.State.ExitCode}}'
```

**常见原因**:
1. 数据库连接失败
   ```bash
   # 测试数据库连接
   psql -h $DB_HOST -U $DB_USERNAME -d $DB_NAME
   ```

2. 端口已被占用
   ```bash
   # 查看端口占用
   netstat -tuln | grep 8082
   lsof -i :8082
   ```

3. 环境变量缺失
   ```bash
   # 检查环境变量
   docker exec admin-service env | grep DB_
   ```

---

### 问题 2: JWT 认证失败

**症状**: 返回 401 Unauthorized

**排查步骤**:
```bash
# 检查 JWT 配置
docker exec admin-service env | grep JWT_

# 验证 Token
curl -X POST http://localhost:8082/api/admin/auth/validate \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**解决方案**:
1. 确保 JWT_SECRET 配置正确
2. 检查 Token 是否过期
3. 验证请求头格式: `Authorization: Bearer <token>`

---

### 问题 3: 数据库连接池耗尽

**症状**: `HikariPool - Connection is not available`

**排查步骤**:
```bash
# 查看活跃连接
curl http://localhost:8082/actuator/metrics/hikaricp.connections.active

# 查看数据库连接数
psql -U admin -d admin_service -c "SELECT count(*) FROM pg_stat_activity;"
```

**解决方案**:
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
```

---

### 问题 4: RabbitMQ 消息堆积

**症状**: 消息队列持续增长

**排查步骤**:
```bash
# 登录 RabbitMQ 管理界面
open http://localhost:15672

# 查看队列状态
rabbitmqadmin list queues name messages consumers
```

**解决方案**:
1. 增加消费者数量
2. 优化消息处理逻辑
3. 启用消息确认机制

---

## 回滚方案

### 场景 1: 新版本有 Bug

#### Docker 部署回滚
```bash
# 停止当前版本
docker stop admin-service

# 启动上一个版本
docker run -d \
  --name admin-service \
  -p 8082:8082 \
  --env-file .env \
  nushungry/admin-service:v1.0.0
```

#### ECS 部署回滚
```bash
# 更新服务使用旧的 Task Definition
aws ecs update-service \
  --cluster nushungry-cluster \
  --service admin-service \
  --task-definition admin-service:3
```

---

### 场景 2: 数据库迁移失败

```bash
# 恢复数据库备份
psql -U admin -d admin_service < backup_20250119.sql

# 重启服务
docker-compose restart admin-service
```

---

### 场景 3: 配置错误

```bash
# 编辑环境变量
nano .env

# 重新部署
docker-compose up -d --force-recreate admin-service
```

---

## 性能优化建议

### 1. JVM 调优
```bash
# 生产环境推荐参数
JAVA_OPTS="
  -Xms2g -Xmx4g
  -XX:+UseG1GC
  -XX:MaxGCPauseMillis=200
  -XX:+ParallelRefProcEnabled
  -XX:+UnlockExperimentalVMOptions
  -XX:+UseStringDeduplication
  -XX:+HeapDumpOnOutOfMemoryError
  -XX:HeapDumpPath=/var/log/admin-service/heap_dump.hprof
"
```

### 2. 数据库优化
```sql
-- 创建必要的索引
CREATE INDEX CONCURRENTLY idx_users_last_login ON users(last_login DESC);

-- 分析表统计信息
ANALYZE users;
ANALYZE admin_audit_logs;

-- 定期清理旧数据
DELETE FROM admin_audit_logs WHERE created_at < NOW() - INTERVAL '90 days';
```

### 3. 缓存策略
```yaml
spring:
  cache:
    type: redis
    redis:
      time-to-live: 3600000
      cache-null-values: false
```

---

## 联系与支持

遇到问题请联系:
- **技术支持**: tech@nushungry.com
- **文档**: [GitHub Wiki](https://github.com/SWE5006-Group-7/nushungry-Backend/wiki)
- **问题报告**: [GitHub Issues](https://github.com/SWE5006-Group-7/nushungry-Backend/issues)

---

**最后更新**: 2025-10-19  
**版本**: v1.0.0
