output "redis_endpoint" {
  description = "Redis 主端点"
  value       = aws_elasticache_replication_group.redis.primary_endpoint_address
}

output "redis_port" {
  description = "Redis 端口"
  value       = 6379
}

output "security_group_id" {
  description = "Redis 安全组 ID"
  value       = aws_security_group.redis.id
}
