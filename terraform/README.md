# NUSHungry Terraform 基础设施配置

这个目录包含 NUSHungry 微服务项目的 Terraform 基础设施即代码(IaC)配置。

## 📁 项目结构

```
terraform/
├── modules/                    # 可复用的 Terraform 模块
│   ├── vpc/                   # VPC 网络模块
│   ├── eks/                   # EKS Kubernetes 集群模块
│   ├── rds/                   # PostgreSQL 数据库模块
│   ├── documentdb/            # MongoDB 兼容数据库模块
│   ├── redis/                 # Redis 缓存模块
│   └── ecr/                   # Docker 镜像仓库模块
└── environments/              # 环境配置
    ├── dev/                   # 开发环境
    │   ├── main.tf
    │   ├── variables.tf
    │   ├── outputs.tf
    │   └── terraform.tfvars.example
    └── prod/                  # 生产环境 (待创建)
```

## 🚀 快速开始

### 前置要求

1. **安装工具**:
   - [Terraform](https://www.terraform.io/downloads) >= 1.0
   - [AWS CLI](https://aws.amazon.com/cli/) 已配置

2. **AWS 凭证**:
   ```bash
   aws configure
   # 输入 Access Key ID 和 Secret Access Key
   ```

### 部署步骤

#### 1. 创建配置文件

```bash
cd terraform/environments/dev
cp terraform.tfvars.example terraform.tfvars
```

编辑 `terraform.tfvars`,填入真实密码:

```hcl
db_password      = "your-strong-password-here"
mongodb_password = "your-strong-mongodb-password-here"
```

#### 2. 初始化 Terraform

```bash
terraform init
```

#### 3. 验证配置

```bash
terraform validate
```

#### 4. 查看执行计划

```bash
terraform plan
```

#### 5. 应用配置

```bash
terraform apply
# 输入 yes 确认
```

部署大约需要 15-20 分钟。

#### 6. 获取输出值

```bash
# 查看所有输出
terraform output

# 配置 kubectl
terraform output -raw configure_kubectl | bash

# 验证 EKS 连接
kubectl get nodes
```

## 📊 创建的资源

### 网络 (VPC)
- 1 个 VPC (10.0.0.0/16)
- 2 个公有子网
- 2 个私有子网
- 2 个 NAT Gateway
- 1 个 Internet Gateway

### 计算 (EKS)
- 1 个 EKS 集群 (Kubernetes 1.28)
- 1 个节点组 (2-4 个 t3.medium 实例)
- 相关 IAM 角色和安全组

### 数据库
- **RDS PostgreSQL 16**: db.t3.micro, 20GB
- **DocumentDB 5.0**: db.t3.medium, 1 实例
- **Redis 7.0**: cache.t3.micro

### 镜像仓库
- 5 个 ECR 仓库 (每个微服务一个)

## 💰 成本估算

开发环境月度成本 (美元):

| 资源 | 配置 | 月成本 |
|------|------|--------|
| EKS 集群 | 1 个 | $73 |
| EC2 节点 | 2 x t3.medium | $60 |
| RDS | db.t3.micro | $15 |
| DocumentDB | db.t3.medium | $65 |
| Redis | cache.t3.micro | $12 |
| NAT Gateway | 2 个 | $65 |
| 其他 | ALB, ECR, 流量 | $29 |
| **总计** | | **~$319** |

## 🔧 常用命令

```bash
# 查看当前状态
terraform show

# 查看输出值
terraform output

# 销毁所有资源 (⚠️ 危险操作)
terraform destroy

# 格式化代码
terraform fmt -recursive

# 验证配置
terraform validate

# 刷新状态
terraform refresh
```

## 📝 配置说明

### Dev 环境特点

- 使用较小的实例类型以降低成本
- 单可用区部署 (非 Multi-AZ)
- 关闭删除保护
- 备份保留 7 天

### Prod 环境建议

创建 `environments/prod/` 时应:

1. 使用更大的实例类型
2. 启用 Multi-AZ
3. 启用删除保护
4. 增加备份保留期到 30 天
5. 启用增强监控
6. 考虑使用 Spot 实例混合

## 🔒 安全注意事项

1. **永远不要提交 `*.tfvars` 文件到 Git**
   - 已在 `.gitignore` 中排除
   - 使用 `.tfvars.example` 作为模板

2. **使用强密码**
   ```bash
   # 生成随机密码
   openssl rand -base64 32
   ```

3. **保护状态文件**
   - 考虑使用 S3 后端存储
   - 启用状态文件加密
   - 使用 DynamoDB 实现状态锁

4. **最小权限原则**
   - 为 CI/CD 创建专用 IAM 用户
   - 仅授予必要权限

## 🆘 故障排查

### 问题 1: 权限不足

```
Error: creating EC2 VPC: UnauthorizedOperation
```

**解决**: 检查 IAM 权限,确保有 EC2、VPC 相关权限。

### 问题 2: 资源已存在

```
Error: resource already exists
```

**解决**:
```bash
# 导入现有资源
terraform import aws_vpc.main vpc-xxxxx
```

### 问题 3: 状态锁定

```
Error: Error acquiring the state lock
```

**解决**:
```bash
terraform force-unlock LOCK_ID
```

## 🔄 更新流程

1. 修改配置文件
2. 运行 `terraform plan` 查看变更
3. 运行 `terraform apply` 应用变更

Terraform 会自动计算最小变更集。

## 🧹 清理资源

⚠️ **警告**: 此操作会删除所有资源且不可恢复!

```bash
cd terraform/environments/dev
terraform destroy
# 输入 yes 确认
```

删除过程约需 10-15 分钟。

## 📚 参考资源

- [Terraform AWS Provider 文档](https://registry.terraform.io/providers/hashicorp/aws/latest/docs)
- [AWS EKS 最佳实践](https://aws.github.io/aws-eks-best-practices/)
- [Terraform 官方教程](https://learn.hashicorp.com/terraform)

## 🤝 贡献

如需修改基础设施:

1. 在模块中修改通用配置
2. 在环境目录中修改特定配置
3. 提交 PR 前运行 `terraform fmt` 和 `terraform validate`

## 📄 许可证

本项目使用的 Terraform 配置遵循项目主许可证。

---

**维护者**: NUSHungry Team
**最后更新**: 2024-11-15
