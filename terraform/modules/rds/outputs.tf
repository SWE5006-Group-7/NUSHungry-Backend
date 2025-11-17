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

output "database_name" {
  description = "主数据库名称"
  value       = aws_db_instance.postgres.db_name
}

output "master_username" {
  description = "主用户名"
  value       = aws_db_instance.postgres.username
}

# 所有微服务数据库名称列表
output "microservices_databases" {
  description = "所有微服务数据库名称"
  value = {
    user_service       = "nushungry_users"
    cafeteria_service  = "cafeteria_db"
    media_service      = "media_db"
    preference_service = "preference_db"
  }
}

# 数据库初始化状态
# 注意: 需要通过 Kubernetes Job 手动初始化数据库
output "databases_initialized" {
  description = "数据库初始化完成状态(需通过 K8s Job 执行)"
  value       = false
}

# 完整的数据库连接 URL(用于 Kubernetes ConfigMap)
output "database_urls" {
  description = "所有微服务的数据库连接 URL"
  value = {
    user_service       = "jdbc:postgresql://${aws_db_instance.postgres.address}:5432/nushungry_users"
    cafeteria_service  = "jdbc:postgresql://${aws_db_instance.postgres.address}:5432/cafeteria_db"
    media_service      = "jdbc:postgresql://${aws_db_instance.postgres.address}:5432/media_db"
    preference_service = "jdbc:postgresql://${aws_db_instance.postgres.address}:5432/preference_db"
  }
}

# psql 连接命令示例
output "psql_connection_commands" {
  description = "psql 连接命令示例"
  value = {
    user_service       = "psql -h ${aws_db_instance.postgres.address} -U ${var.master_username} -d nushungry_users"
    cafeteria_service  = "psql -h ${aws_db_instance.postgres.address} -U ${var.master_username} -d cafeteria_db"
    media_service      = "psql -h ${aws_db_instance.postgres.address} -U ${var.master_username} -d media_db"
    preference_service = "psql -h ${aws_db_instance.postgres.address} -U ${var.master_username} -d preference_db"
  }
}
