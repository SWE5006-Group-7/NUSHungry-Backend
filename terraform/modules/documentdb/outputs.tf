output "cluster_endpoint" {
  description = "DocumentDB 集群端点"
  value       = aws_docdb_cluster.main.endpoint
}

output "cluster_reader_endpoint" {
  description = "DocumentDB 集群读端点"
  value       = aws_docdb_cluster.main.reader_endpoint
}

output "cluster_port" {
  description = "DocumentDB 端口"
  value       = aws_docdb_cluster.main.port
}

output "security_group_id" {
  description = "DocumentDB 安全组 ID"
  value       = aws_security_group.docdb.id
}
