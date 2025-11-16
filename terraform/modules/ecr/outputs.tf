output "repository_urls" {
  description = "ECR 仓库 URL 映射"
  value = {
    for service, repo in aws_ecr_repository.services :
    service => repo.repository_url
  }
}

output "repository_arns" {
  description = "ECR 仓库 ARN 映射"
  value = {
    for service, repo in aws_ecr_repository.services :
    service => repo.arn
  }
}
