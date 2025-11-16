output "db_instance_endpoint" {
  description = "RDS 实例端点"
  value       = aws_db_instance.postgres.endpoint
}

output "db_instance_address" {
  description = "RDS 实例地址"
  value       = aws_db_instance.postgres.address
}

output "db_instance_port" {
  description = "RDS 实例端口"
  value       = aws_db_instance.postgres.port
}

output "db_instance_id" {
  description = "RDS 实例 ID"
  value       = aws_db_instance.postgres.id
}

output "security_group_id" {
  description = "RDS 安全组 ID"
  value       = aws_security_group.rds.id
}
