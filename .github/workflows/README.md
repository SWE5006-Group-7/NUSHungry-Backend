# GitHub Actions CI/CD 工作流文档

本文档描述了 NUSHungry 微服务项目的 GitHub Actions CI/CD 流程配置。

## 📋 目录

1. [工作流概览](#工作流概览)
2. [CI 工作流 (ci.yml)](#ci-工作流)
3. [构建推送工作流 (build-and-push.yml)](#构建推送工作流)
4. [部署工作流 (deploy.yml)](#部署工作流)
5. [配置 GitHub Secrets](#配置-github-secrets)
6. [使用指南](#使用指南)
7. [故障排查](#故障排查)

## 工作流概览

```
┌─────────────────┐
│  开发者提交代码  │
└────────┬────────┘
         │
         ▼
┌─────────────────────────────────┐
│  CI 工作流 (ci.yml)              │
│  ├─ 代码质量检查                 │
│  ├─ 单元测试 (并行)              │
│  ├─ 安全扫描 (OWASP, SpotBugs)  │
│  ├─ 集成测试                     │
│  ├─ 构建验证                     │
│  └─ Docker 构建验证              │
└────────┬────────────────────────┘
         │ (仅 main 分支)
         ▼
┌─────────────────────────────────┐
│  构建推送工作流                  │
│  (build-and-push.yml)           │
│  ├─ 检测变更的服务               │
│  ├─ 构建 Docker 镜像             │
│  ├─ 推送到 AWS ECR               │
│  ├─ 镜像安全扫描 (Trivy)         │
│  └─ 更新 K8s 清单                │
└────────┬────────────────────────┘
         │ (自动触发)
         ▼
┌─────────────────────────────────┐
│  部署工作流 (deploy.yml)         │
│  ├─ 确定环境配置                 │
│  ├─ 验证前提条件                 │
│  ├─ 创建/更新 K8s Secrets        │
│  ├─ 滚动更新部署                 │
│  ├─ 健康检查                     │
│  └─ 部署 Ingress                 │
└─────────────────────────────────┘
```

## CI 工作流

**文件**: `.github/workflows/ci.yml`

**触发条件**:
- Push 到 `main`, `develop`, `test` 分支
- Pull Request 到上述分支

**主要功能**:

### 1. 代码质量检查
- Maven POM 验证
- 代码格式检查 (Spotless)

### 2. 单元测试 (并行执行)
- 对所有 5 个微服务并行执行测试
- 生成 JaCoCo 覆盖率报告
- 覆盖率阈值: 70%
- 上传测试结果和覆盖率报告

### 3. 安全扫描
- **OWASP Dependency Check**: 检查依赖漏洞 (CVSS ≥ 7.0 视为失败)
- **SpotBugs**: 静态代码分析

### 4. 集成测试 (仅 PR)
- 使用 GitHub Actions Services 启动:
  - PostgreSQL 16
  - MongoDB 7
  - Redis 7
  - RabbitMQ 3.12
- 运行集成测试套件

### 5. 构建验证
- Maven 打包所有服务
- 验证 JAR 文件生成
- 上传构建产物

### 6. Docker 构建验证
- 为每个服务构建 Docker 镜像
- 验证镜像大小和元数据

### 7. CI 总结
- 生成总结报告
- 检查所有 Job 状态

**最佳实践**:
- 使用 `fail-fast: false` 确保所有服务测试都运行
- 缓存 Maven 依赖加速构建
- 并行执行独立任务
- 自动发布测试报告

## 构建推送工作流

**文件**: `.github/workflows/build-and-push.yml`

**触发条件**:
- Push 到 `main` 分支 (服务代码变更时)
- 手动触发 (workflow_dispatch)

**主要功能**:

### 1. 智能变更检测
- **自动触发**: 检测 `git diff` 找出变更的服务
- **手动触发**: 允许指定服务列表或构建所有服务

### 2. Docker 镜像构建
- 使用多阶段构建 (Maven 构建 + JRE 运行)
- 支持 BuildKit 缓存加速
- 自动生成镜像标签:
  - `sha-<commit>`: 短 commit SHA
  - `latest`: 仅 main 分支
  - 手动指定标签

### 3. 推送到 AWS ECR
- 登录 ECR
- 推送多标签镜像
- 设置镜像元数据 (build date, VCS ref)

### 4. 镜像安全扫描
- 使用 Trivy 扫描漏洞
- 检测 CRITICAL 和 HIGH 级别漏洞
- 生成 SARIF 报告并上传到 GitHub Security

### 5. 更新 K8s 清单
- 自动更新 `k8s/services/*/deployment.yaml` 中的镜像标签
- 提交变更到 Git 仓库

### 6. 触发部署
- 发送 `repository_dispatch` 事件
- 自动触发部署工作流 (默认部署到 dev 环境)

**环境变量**:
```yaml
AWS_REGION: ap-southeast-1
ECR_REGISTRY: <account-id>.dkr.ecr.ap-southeast-1.amazonaws.com
```

## 部署工作流

**文件**: `.github/workflows/deploy.yml`

**触发条件**:
- 手动触发 (workflow_dispatch,可选择环境)
- `build-and-push` 工作流成功后自动触发 (默认 dev)
- Repository dispatch 事件

**支持环境**:
- `dev`: 开发环境 (nushungry-dev-eks)
- `staging`: 测试环境 (nushungry-staging-eks)
- `prod`: 生产环境 (nushungry-prod-eks)

**主要功能**:

### 1. 确定部署配置
- 根据触发方式和分支确定目标环境
- 自动映射集群名称和命名空间
- 确定要部署的服务和镜像标签

### 2. 验证前提条件
- 验证 K8s 清单文件存在

### 3. 部署到 EKS
- 配置 kubectl 连接到 EKS
- 创建/更新 Namespace
- 创建/更新 Kubernetes Secrets:
  - `postgres-secret`: PostgreSQL 凭证
  - `mongodb-secret`: MongoDB 凭证
  - `redis-secret`: Redis 配置
  - `jwt-secret`: JWT 签名密钥
  - `amazonmq-secret`: RabbitMQ (Amazon MQ) 凭证

### 4. 滚动更新
- 更新 Deployment 清单中的镜像标签
- 应用 ConfigMap、Deployment、Service
- 应用 HPA (如果存在)
- 等待滚动更新完成 (超时: 600s)

### 5. 部署验证
- 检查 Pod 状态
- 查看 Deployment 状态
- 查看最近事件

### 6. 健康检查
- 等待 Pod 进入 Ready 状态
- 端口转发并访问 `/actuator/health`
- 验证服务健康

### 7. 部署 Ingress
- 所有服务部署完成后部署 Ingress
- 等待 ALB 创建
- 输出 ALB URL

### 8. 部署总结
- 生成详细的部署报告
- 提供验证命令

**部署策略**:
- **并行度**: `max-parallel: 2` (逐步部署,避免同时更新所有服务)
- **失败处理**: `fail-fast: false` (一个服务失败不影响其他服务)
- **超时**: 600 秒

## 配置 GitHub Secrets

在 GitHub 仓库的 `Settings → Secrets and variables → Actions` 中配置以下 Secrets:

### AWS 凭证
```
AWS_ACCESS_KEY_ID       # AWS 访问密钥 ID
AWS_SECRET_ACCESS_KEY   # AWS 密钥
AWS_ACCOUNT_ID          # AWS 账号 ID (12 位数字)
```

### 数据库凭证
```
DB_PASSWORD             # PostgreSQL 密码
POSTGRES_URL            # PostgreSQL 连接 URL
MONGODB_PASSWORD        # MongoDB 密码
MONGODB_URI             # MongoDB 连接 URI
REDIS_PASSWORD          # Redis 密码
REDIS_HOST              # Redis 主机地址
```

### RabbitMQ (Amazon MQ)
```
RABBITMQ_PASSWORD       # RabbitMQ 密码
RABBITMQ_HOST           # Amazon MQ Broker 主机
```

### 应用密钥
```
JWT_SECRET              # JWT 签名密钥 (至少 256 位)
```

**生成强密码示例**:
```bash
# 生成 32 字符随机密码
openssl rand -base64 32

# 生成 JWT Secret (256 位)
openssl rand -base64 32
```

## 使用指南

### 开发流程

1. **创建功能分支**
   ```bash
   git checkout -b feature/new-feature
   ```

2. **开发和本地测试**
   ```bash
   mvn clean test
   docker-compose up -d
   ```

3. **提交代码并创建 PR**
   ```bash
   git add .
   git commit -m "feat: add new feature"
   git push origin feature/new-feature
   ```

4. **PR 触发 CI**
   - 自动运行代码质量检查
   - 运行单元测试和集成测试
   - 执行安全扫描
   - 验证 Docker 构建

5. **合并到 main 分支**
   - 触发构建推送工作流
   - 自动构建 Docker 镜像
   - 推送到 AWS ECR
   - 触发部署到开发环境

### 手动部署

#### 构建特定服务
```bash
# 在 GitHub Actions 界面
Actions → Build and Push Docker Images → Run workflow
输入:
  - services: user-service,cafeteria-service
  - tag: v1.2.0
```

#### 部署到指定环境
```bash
# 在 GitHub Actions 界面
Actions → Deploy to Kubernetes → Run workflow
输入:
  - environment: dev (或 staging/prod)
  - services: all
  - tag: latest
  - skip_health_check: false
```

### 查看部署状态

```bash
# 配置 kubectl (根据环境选择集群)
# 开发环境
aws eks update-kubeconfig --name nushungry-dev-eks --region ap-southeast-1

# 测试环境
aws eks update-kubeconfig --name nushungry-staging-eks --region ap-southeast-1

# 生产环境
aws eks update-kubeconfig --name nushungry-prod-eks --region ap-southeast-1

# 查看 Pod 状态 (替换 <env> 为 dev/staging/prod)
kubectl get pods -n nushungry-<env>

# 查看服务
kubectl get svc -n nushungry-<env>

# 查看日志
kubectl logs -f deployment/user-service -n nushungry-<env>

# 查看 Ingress
kubectl get ingress -n nushungry-<env>
```

## 故障排查

### CI 失败

#### 测试失败
```bash
# 查看测试报告
Actions → 点击失败的运行 → Artifacts → coverage-<service>

# 本地重现
cd <service-name>
mvn clean test
```

#### 安全扫描失败
```bash
# 查看 OWASP 报告
Actions → Artifacts → owasp-dependency-check-report

# 抑制误报
# 编辑 .github/dependency-check-suppressions.xml
```

### 构建失败

#### Maven 构建错误
```bash
# 检查 POM 配置
mvn validate

# 清理缓存
mvn dependency:purge-local-repository
```

#### Docker 构建失败
```bash
# 本地测试构建
cd <service-name>
mvn clean package -DskipTests
docker build -t test:local .
```

### 部署失败

#### kubectl 无法连接
```bash
# 验证 AWS 凭证
aws sts get-caller-identity

# 更新 kubeconfig
aws eks update-kubeconfig --name nushungry-dev-eks --region ap-southeast-1
```

#### Pod 启动失败
```bash
# 查看 Pod 详情
kubectl describe pod <pod-name> -n nushungry-dev

# 查看日志
kubectl logs <pod-name> -n nushungry-dev

# 常见问题:
# 1. ImagePullBackOff: 检查 ECR 权限
# 2. CrashLoopBackOff: 检查应用日志和配置
# 3. Secret not found: 重新运行部署工作流创建 Secrets
```

#### 健康检查失败
```bash
# 端口转发测试
kubectl port-forward svc/<service-name> 8080:8080 -n nushungry-dev
curl http://localhost:8080/actuator/health

# 检查环境变量
kubectl exec <pod-name> -n nushungry-dev -- env | grep SPRING
```

### Secret 配置问题

#### 创建/更新 Secret
```bash
# PostgreSQL
kubectl create secret generic postgres-secret \
  --from-literal=username=postgres \
  --from-literal=password='YOUR_PASSWORD' \
  --from-literal=url='jdbc:postgresql://...' \
  -n nushungry-dev \
  --dry-run=client -o yaml | kubectl apply -f -

# Amazon MQ
kubectl create secret generic amazonmq-secret \
  --from-literal=host='b-xxxx.mq.ap-southeast-1.amazonaws.com' \
  --from-literal=port=5671 \
  --from-literal=username=admin \
  --from-literal=password='YOUR_PASSWORD' \
  --from-literal=vhost="/" \
  --from-literal=ssl="true" \
  -n nushungry-dev \
  --dry-run=client -o yaml | kubectl apply -f -
```

#### 验证 Secret
```bash
# 列出所有 Secrets
kubectl get secrets -n nushungry-dev

# 查看 Secret 内容 (base64 解码)
kubectl get secret amazonmq-secret -n nushungry-dev -o jsonpath='{.data.host}' | base64 -d
```

## 工作流优化建议

### 加速构建
1. **Maven 缓存**: 已配置 `cache: maven`
2. **Docker BuildKit 缓存**: 使用 `cache-from/cache-to: type=gha`
3. **并行执行**: 使用 `strategy.matrix` 并行构建/测试

### 降低成本
1. **智能变更检测**: 仅构建变更的服务
2. **按需部署**: 支持手动触发和服务选择
3. **缓存复用**: 减少重复下载依赖和构建层

### 提高可靠性
1. **失败重试**: 关键步骤设置 `continue-on-error: true`
2. **超时控制**: 设置合理的 `timeout`
3. **健康检查**: 部署后验证服务健康

## 相关文档

- [CICD 部署完整教程](../CICD-DEPLOYMENT-GUIDE.md)
- [Amazon MQ 迁移指南](../AMAZON-MQ-MIGRATION-GUIDE.md)
- [架构和密钥管理指南](../ARCHITECTURE-AND-SECRETS-GUIDE.md)
- [GitHub Actions 官方文档](https://docs.github.com/en/actions)

## 支持

如有问题,请查看:
1. GitHub Actions 运行日志
2. 本文档的故障排查部分
3. 创建 GitHub Issue
