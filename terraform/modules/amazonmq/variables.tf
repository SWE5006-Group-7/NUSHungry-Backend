# ============================================================================
# Amazon MQ 模块变量定义
# ============================================================================

variable "project_name" {
  description = "项目名称"
  type        = string
}

variable "environment" {
  description = "环境名称 (dev, staging, prod)"
  type        = string
}

variable "vpc_id" {
  description = "VPC ID"
  type        = string
}

variable "subnet_ids" {
  description = "子网 ID 列表 (私有子网)"
  type        = list(string)
}

variable "allowed_security_group_ids" {
  description = "允许访问 Amazon MQ 的安全组 ID 列表 (通常是 EKS 节点安全组)"
  type        = list(string)
}

# ============================================================================
# RabbitMQ 配置
# ============================================================================

variable "rabbitmq_engine_version" {
  description = "RabbitMQ 引擎版本"
  type        = string
  default     = "3.12.13"
  # 可用版本: 3.8.x, 3.9.x, 3.10.x, 3.11.x, 3.12.x
  # 查看最新版本: aws mq describe-broker-engine-types --engine-type rabbitmq
}

variable "instance_type" {
  description = "Broker 实例类型"
  type        = string
  default     = "mq.t3.micro"
  # 开发环境: mq.t3.micro
  # 生产环境: mq.m5.large, mq.m5.xlarge
  # 完整列表: https://docs.aws.amazon.com/amazon-mq/latest/developer-guide/broker-instance-types.html
}

variable "deployment_mode" {
  description = "部署模式: SINGLE_INSTANCE (单可用区) 或 CLUSTER_MULTI_AZ (多可用区集群)"
  type        = string
  default     = "SINGLE_INSTANCE"

  validation {
    condition     = contains(["SINGLE_INSTANCE", "CLUSTER_MULTI_AZ"], var.deployment_mode)
    error_message = "deployment_mode 必须是 SINGLE_INSTANCE 或 CLUSTER_MULTI_AZ"
  }
}

variable "storage_type" {
  description = "存储类型: ebs (块存储) 或 efs (文件存储)"
  type        = string
  default     = "ebs"

  validation {
    condition     = contains(["ebs", "efs"], var.storage_type)
    error_message = "storage_type 必须是 ebs 或 efs"
  }
}

# ============================================================================
# 认证配置
# ============================================================================

variable "rabbitmq_username" {
  description = "RabbitMQ 管理员用户名"
  type        = string
  default     = "admin"
}

variable "rabbitmq_password" {
  description = "RabbitMQ 管理员密码 (至少 12 个字符)"
  type        = string
  sensitive   = true

  validation {
    condition     = length(var.rabbitmq_password) >= 12
    error_message = "RabbitMQ 密码必须至少 12 个字符"
  }
}

# ============================================================================
# 维护窗口配置
# ============================================================================

variable "maintenance_day_of_week" {
  description = "维护窗口的星期几 (MONDAY, TUESDAY, etc.)"
  type        = string
  default     = "SUNDAY"
}

variable "maintenance_time_of_day" {
  description = "维护窗口的时间 (HH:MM 格式, UTC)"
  type        = string
  default     = "03:00"
}

variable "maintenance_time_zone" {
  description = "维护窗口的时区"
  type        = string
  default     = "Asia/Singapore"
}

variable "auto_minor_version_upgrade" {
  description = "是否启用自动小版本升级"
  type        = bool
  default     = true
}

# ============================================================================
# 日志配置
# ============================================================================

variable "log_retention_days" {
  description = "CloudWatch 日志保留天数"
  type        = number
  default     = 7
}

# ============================================================================
# CloudWatch 告警配置
# ============================================================================

variable "enable_cloudwatch_alarms" {
  description = "是否启用 CloudWatch 告警"
  type        = bool
  default     = true
}

variable "alarm_sns_topic_arns" {
  description = "告警通知的 SNS Topic ARN 列表"
  type        = list(string)
  default     = []
}

variable "connection_count_threshold" {
  description = "连接数告警阈值"
  type        = number
  default     = 100
}

variable "cpu_utilization_threshold" {
  description = "CPU 使用率告警阈值 (百分比)"
  type        = number
  default     = 80
}

variable "memory_usage_threshold" {
  description = "内存使用率告警阈值 (字节)"
  type        = number
  default     = 400000000 # 400MB for mq.t3.micro (512MB total)
}

# ============================================================================
# 标签
# ============================================================================

variable "additional_tags" {
  description = "额外的资源标签"
  type        = map(string)
  default     = {}
}
