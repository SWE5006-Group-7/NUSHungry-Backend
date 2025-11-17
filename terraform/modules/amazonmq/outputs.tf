# ============================================================================
# Amazon MQ 模块输出
# ============================================================================

output "broker_id" {
  description = "Amazon MQ Broker ID"
  value       = aws_mq_broker.rabbitmq.id
}

output "broker_arn" {
  description = "Amazon MQ Broker ARN"
  value       = aws_mq_broker.rabbitmq.arn
}

output "broker_name" {
  description = "Amazon MQ Broker 名称"
  value       = aws_mq_broker.rabbitmq.broker_name
}

# ============================================================================
# 连接端点
# ============================================================================

output "broker_endpoints" {
  description = "Amazon MQ Broker 端点列表 (AMQP)"
  value       = aws_mq_broker.rabbitmq.instances[*].endpoints
}

output "broker_amqp_endpoint" {
  description = "RabbitMQ AMQP 端点 (TLS, 端口 5671)"
  value       = try(aws_mq_broker.rabbitmq.instances[0].endpoints[0], "")
}

output "broker_console_url" {
  description = "RabbitMQ Web 管理控制台 URL (TLS, 端口 15671)"
  value       = try(aws_mq_broker.rabbitmq.instances[0].console_url, "")
}

# ============================================================================
# 连接信息 (用于 Kubernetes ConfigMap/Secret)
# ============================================================================

output "rabbitmq_host" {
  description = "RabbitMQ 主机地址 (从 AMQP 端点提取)"
  value       = try(regex("amqps://([^:]+):5671", aws_mq_broker.rabbitmq.instances[0].endpoints[0])[0], "")
}

output "rabbitmq_port" {
  description = "RabbitMQ AMQP 端口 (TLS)"
  value       = "5671"
}

output "rabbitmq_username" {
  description = "RabbitMQ 用户名"
  value       = var.rabbitmq_username
  sensitive   = true
}

output "rabbitmq_password" {
  description = "RabbitMQ 密码"
  value       = var.rabbitmq_password
  sensitive   = true
}

output "rabbitmq_vhost" {
  description = "RabbitMQ 虚拟主机 (默认为 /)"
  value       = "/"
}

# ============================================================================
# Spring Boot 连接配置
# ============================================================================

output "spring_rabbitmq_addresses" {
  description = "Spring Boot RabbitMQ 连接地址 (格式: amqps://host:5671)"
  value       = try(aws_mq_broker.rabbitmq.instances[0].endpoints[0], "")
}

output "spring_rabbitmq_ssl_enabled" {
  description = "Spring Boot RabbitMQ SSL 启用标志"
  value       = "true"
}

# ============================================================================
# 安全组
# ============================================================================

output "security_group_id" {
  description = "Amazon MQ 安全组 ID"
  value       = aws_security_group.amazonmq.id
}

# ============================================================================
# CloudWatch
# ============================================================================

output "cloudwatch_log_group_name" {
  description = "CloudWatch 日志组名称"
  value       = aws_cloudwatch_log_group.amazonmq.name
}

output "cloudwatch_log_group_arn" {
  description = "CloudWatch 日志组 ARN"
  value       = aws_cloudwatch_log_group.amazonmq.arn
}

# ============================================================================
# 配置信息
# ============================================================================

output "configuration_id" {
  description = "Amazon MQ 配置 ID"
  value       = aws_mq_configuration.rabbitmq.id
}

output "configuration_revision" {
  description = "Amazon MQ 配置版本"
  value       = aws_mq_configuration.rabbitmq.latest_revision
}

output "deployment_mode" {
  description = "部署模式"
  value       = var.deployment_mode
}

output "instance_type" {
  description = "实例类型"
  value       = var.instance_type
}

output "engine_version" {
  description = "RabbitMQ 引擎版本"
  value       = var.rabbitmq_engine_version
}

# ============================================================================
# Kubernetes 环境变量映射 (用于服务配置)
# ============================================================================

output "k8s_env_vars" {
  description = "Kubernetes 环境变量映射"
  value = {
    SPRING_RABBITMQ_HOST      = try(regex("amqps://([^:]+):5671", aws_mq_broker.rabbitmq.instances[0].endpoints[0])[0], "")
    SPRING_RABBITMQ_PORT      = "5671"
    SPRING_RABBITMQ_USERNAME  = var.rabbitmq_username
    SPRING_RABBITMQ_PASSWORD  = var.rabbitmq_password
    SPRING_RABBITMQ_VHOST     = "/"
    SPRING_RABBITMQ_SSL       = "true"
    SPRING_RABBITMQ_ADDRESSES = try(aws_mq_broker.rabbitmq.instances[0].endpoints[0], "")
  }
  sensitive = true
}
