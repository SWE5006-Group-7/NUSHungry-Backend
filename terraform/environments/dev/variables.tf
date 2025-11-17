variable "project_name" {
  description = "项目名称"
  type        = string
  default     = "nushungry"
}

variable "environment" {
  description = "环境名称"
  type        = string
  default     = "dev"
}

variable "aws_region" {
  description = "AWS 区域"
  type        = string
  default     = "ap-southeast-1"
}

variable "vpc_cidr" {
  description = "VPC CIDR 块"
  type        = string
  default     = "10.0.0.0/16"
}

variable "db_password" {
  description = "PostgreSQL 主密码"
  type        = string
  sensitive   = true
}

variable "mongodb_password" {
  description = "MongoDB 主密码"
  type        = string
  sensitive   = true
}

# ============================================================================
# Amazon MQ (RabbitMQ) 配置
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
}

variable "amazonmq_instance_type" {
  description = "Amazon MQ 实例类型 (dev: mq.t3.micro, prod: mq.m5.large)"
  type        = string
  default     = "mq.t3.micro"
}

variable "amazonmq_deployment_mode" {
  description = "部署模式: SINGLE_INSTANCE (单可用区) 或 CLUSTER_MULTI_AZ (多可用区)"
  type        = string
  default     = "SINGLE_INSTANCE"
}

variable "enable_amazonmq_alarms" {
  description = "是否启用 Amazon MQ CloudWatch 告警"
  type        = bool
  default     = true
}

variable "amazonmq_connection_threshold" {
  description = "Amazon MQ 连接数告警阈值"
  type        = number
  default     = 100
}

variable "amazonmq_memory_threshold" {
  description = "Amazon MQ 内存使用告警阈值 (字节, mq.t3.micro 默认 400MB)"
  type        = number
  default     = 400000000
}
