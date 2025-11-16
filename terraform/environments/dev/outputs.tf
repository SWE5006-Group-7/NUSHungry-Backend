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
