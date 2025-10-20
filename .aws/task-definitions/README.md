# ECS Task Definitions

此目录包含所有微服务的 AWS ECS Task Definition 配置文件。

## 📋 配置说明

在使用这些 Task Definition 之前，需要替换以下占位符：

### 必填配置

1. **YOUR_ACCOUNT_ID**: 你的 AWS 账户 ID
   - 获取方式: `aws sts get-caller-identity --query Account --output text`

2. **YOUR_ECR_REGISTRY**: ECR 注册表地址
   - 格式: `YOUR_ACCOUNT_ID.dkr.ecr.ap-southeast-1.amazonaws.com`
   - 示例: `123456789012.dkr.ecr.ap-southeast-1.amazonaws.com`

3. **YOUR_RDS_ENDPOINT**: RDS PostgreSQL 实例的端点
   - 获取方式: AWS RDS Console → 选择实例 → 查看连接信息
   - 示例: `nushungry-dev-postgres.xxxxx.ap-southeast-1.rds.amazonaws.com`

4. **YOUR_MONGODB_ENDPOINT**: MongoDB 实例端点 (仅 review-service)
   - 可使用 AWS DocumentDB 或自托管 MongoDB
   - 示例: `nushungry-dev-docdb.xxxxx.ap-southeast-1.docdb.amazonaws.com`

5. **YOUR_RABBITMQ_ENDPOINT**: RabbitMQ 服务端点
   - 可使用 AWS MQ 或自托管 RabbitMQ
   - 示例: `b-xxxxx.mq.ap-southeast-1.amazonaws.com`

6. **YOUR_MINIO_ENDPOINT**: MinIO/S3 端点 (仅 media-service)
   - 可使用 S3 或自托管 MinIO
   - 示例: `minio.your-domain.com`

7. **YOUR_EFS_FILE_SYSTEM_ID**: EFS 文件系统 ID (仅 media-service)
   - 用于存储上传的媒体文件
   - 获取方式: AWS EFS Console
   - 示例: `fs-xxxxx`

### AWS Secrets Manager 配置

每个服务需要在 AWS Secrets Manager 中创建相应的密钥：

#### admin-service
```bash
aws secretsmanager create-secret \
  --name admin-service/db-password \
  --secret-string "YOUR_DB_PASSWORD"

aws secretsmanager create-secret \
  --name admin-service/jwt-secret \
  --secret-string "YOUR_JWT_SECRET_KEY"

aws secretsmanager create-secret \
  --name rabbitmq/password \
  --secret-string "YOUR_RABBITMQ_PASSWORD"
```

#### cafeteria-service
```bash
aws secretsmanager create-secret \
  --name cafeteria-service/db-password \
  --secret-string "YOUR_DB_PASSWORD"
```

#### media-service
```bash
aws secretsmanager create-secret \
  --name media-service/db-password \
  --secret-string "YOUR_DB_PASSWORD"

aws secretsmanager create-secret \
  --name minio/access-key \
  --secret-string "YOUR_MINIO_ACCESS_KEY"

aws secretsmanager create-secret \
  --name minio/secret-key \
  --secret-string "YOUR_MINIO_SECRET_KEY"
```

#### preference-service
```bash
aws secretsmanager create-secret \
  --name preference-service/db-password \
  --secret-string "YOUR_DB_PASSWORD"
```

#### review-service
```bash
aws secretsmanager create-secret \
  --name review-service/mongodb-username \
  --secret-string "admin"

aws secretsmanager create-secret \
  --name review-service/mongodb-password \
  --secret-string "YOUR_MONGODB_PASSWORD"
```

## 🚀 部署步骤

### 1. 创建 IAM 角色

确保已创建以下 IAM 角色：

- **ecsTaskExecutionRole**: ECS 任务执行角色
  - 需要权限: AmazonECSTaskExecutionRolePolicy
  - 额外权限: 访问 Secrets Manager 和 CloudWatch Logs

- **ecsTaskRole**: ECS 任务角色
  - 根据服务需求添加权限 (如 S3, DynamoDB 等)

### 2. 创建 CloudWatch Logs 日志组

```bash
aws logs create-log-group --log-group-name /ecs/nushungry-dev-admin-service
aws logs create-log-group --log-group-name /ecs/nushungry-dev-cafeteria-service
aws logs create-log-group --log-group-name /ecs/nushungry-dev-media-service
aws logs create-log-group --log-group-name /ecs/nushungry-dev-preference-service
aws logs create-log-group --log-group-name /ecs/nushungry-dev-review-service
```

### 3. 注册 Task Definition

替换占位符后，注册 Task Definition：

```bash
aws ecs register-task-definition \
  --cli-input-json file://.aws/task-definitions/admin-service.json

aws ecs register-task-definition \
  --cli-input-json file://.aws/task-definitions/cafeteria-service.json

aws ecs register-task-definition \
  --cli-input-json file://.aws/task-definitions/media-service.json

aws ecs register-task-definition \
  --cli-input-json file://.aws/task-definitions/preference-service.json

aws ecs register-task-definition \
  --cli-input-json file://.aws/task-definitions/review-service.json
```

### 4. 创建 ECS 服务

在 ECS 集群中创建服务（示例）：

```bash
aws ecs create-service \
  --cluster nushungry-dev-cluster \
  --service-name nushungry-dev-admin-service \
  --task-definition nushungry-dev-admin-service \
  --desired-count 1 \
  --launch-type FARGATE \
  --network-configuration "awsvpcConfiguration={subnets=[subnet-xxxxx],securityGroups=[sg-xxxxx],assignPublicIp=ENABLED}"
```

## 📊 服务配置概览

| 服务 | 端口 | 数据库 | 消息队列 | 存储 |
|------|------|--------|---------|------|
| admin-service | 8082 | PostgreSQL | RabbitMQ | - |
| cafeteria-service | 8083 | PostgreSQL | RabbitMQ | - |
| media-service | 8085 | PostgreSQL | - | MinIO/S3 + EFS |
| preference-service | 8086 | PostgreSQL | - | - |
| review-service | 8084 | MongoDB | RabbitMQ | - |

## 🔧 调整资源配置

默认配置为每个服务分配：
- **CPU**: 512 (0.5 vCPU)
- **Memory**: 1024 MB (1 GB)

根据负载可以调整：
- **低负载服务** (preference-service): cpu: 256, memory: 512
- **高负载服务** (review-service, media-service): cpu: 1024, memory: 2048

## 🔐 安全最佳实践

1. **使用 Secrets Manager** 存储敏感信息，不要硬编码
2. **最小权限原则** 配置 taskRoleArn
3. **启用传输加密** EFS 和数据库连接
4. **配置 VPC 安全组** 限制服务间通信
5. **启用 CloudWatch Logs** 监控和审计

## 📚 参考文档

- [AWS ECS Task Definitions](https://docs.aws.amazon.com/AmazonECS/latest/developerguide/task_definitions.html)
- [AWS Secrets Manager](https://docs.aws.amazon.com/secretsmanager/)
- [AWS EFS with ECS](https://docs.aws.amazon.com/AmazonECS/latest/developerguide/efs-volumes.html)
