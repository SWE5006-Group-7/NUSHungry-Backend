# Dev 环境输出

output "vpc_id" {
  description = "VPC ID"
  value       = module.vpc.vpc_id
}

output "eks_cluster_name" {
  description = "EKS 集群名称"
  value       = module.eks.cluster_name
}

output "eks_cluster_endpoint" {
  description = "EKS 集群端点"
  value       = module.eks.cluster_endpoint
}

output "rds_endpoint" {
  description = "RDS 端点"
  value       = module.rds.db_instance_endpoint
}

output "documentdb_endpoint" {
  description = "DocumentDB 端点"
  value       = module.documentdb.cluster_endpoint
}

output "redis_endpoint" {
  description = "Redis 端点"
  value       = module.redis.redis_endpoint
}

output "ecr_repositories" {
  description = "ECR 仓库 URL"
  value       = module.ecr.repository_urls
}

output "configure_kubectl" {
  description = "配置 kubectl 的命令"
  value       = "aws eks update-kubeconfig --name ${module.eks.cluster_name} --region ${var.aws_region}"
}

# ============================================================================
# Amazon MQ 输出
# ============================================================================

output "amazonmq_broker_id" {
  description = "Amazon MQ Broker ID"
  value       = module.amazonmq.broker_id
}

output "amazonmq_broker_name" {
  description = "Amazon MQ Broker 名称"
  value       = module.amazonmq.broker_name
}

output "amazonmq_console_url" {
  description = "RabbitMQ Web 管理控制台 URL"
  value       = module.amazonmq.broker_console_url
}

output "rabbitmq_host" {
  description = "RabbitMQ 主机地址 (用于 Spring Boot 配置)"
  value       = module.amazonmq.rabbitmq_host
}

output "rabbitmq_port" {
  description = "RabbitMQ 端口 (5671, TLS)"
  value       = module.amazonmq.rabbitmq_port
}

output "rabbitmq_amqp_endpoint" {
  description = "RabbitMQ AMQP 完整端点"
  value       = module.amazonmq.broker_amqp_endpoint
}

# ============================================================================
# Kubernetes 配置输出
# ============================================================================

output "create_k8s_amazonmq_secret" {
  description = "创建 Kubernetes Secret 的命令"
  value       = <<-EOT
    kubectl create secret generic amazonmq-secret \
      --from-literal=host=${module.amazonmq.rabbitmq_host} \
      --from-literal=port=${module.amazonmq.rabbitmq_port} \
      --from-literal=username=${var.rabbitmq_username} \
      --from-literal=password='${var.rabbitmq_password}' \
      --from-literal=vhost="/" \
      --from-literal=ssl="true" \
      -n nushungry-${var.environment}
  EOT
  sensitive   = true
}
