output "cluster_id" {
  description = "EKS 集群 ID"
  value       = aws_eks_cluster.main.id
}

output "cluster_name" {
  description = "EKS 集群名称"
  value       = aws_eks_cluster.main.name
}

output "cluster_endpoint" {
  description = "EKS 集群端点"
  value       = aws_eks_cluster.main.endpoint
}

output "cluster_certificate_authority_data" {
  description = "EKS 集群 CA 证书"
  value       = aws_eks_cluster.main.certificate_authority[0].data
}

output "cluster_security_group_id" {
  description = "EKS 集群安全组 ID"
  value       = aws_security_group.cluster.id
}

output "node_security_group_id" {
  description = "EKS 节点安全组 ID"
  value       = aws_security_group.node_group.id
}

output "oidc_provider_arn" {
  description = "OIDC Provider ARN"
  value       = aws_iam_openid_connect_provider.cluster.arn
}

output "node_group_id" {
  description = "EKS 节点组 ID"
  value       = aws_eks_node_group.main.id
}

output "ebs_csi_driver_role_arn" {
  description = "EBS CSI Driver IAM 角色 ARN"
  value       = aws_iam_role.ebs_csi_driver.arn
}

output "ebs_csi_driver_addon_version" {
  description = "EBS CSI Driver 插件版本"
  value       = aws_eks_addon.ebs_csi_driver.addon_version
}
