# ECR 仓库模块 - Docker 镜像仓库

locals {
  services = [
    "user-service",
    "cafeteria-service",
    "review-service",
    "media-service",
    "preference-service"
  ]
}

# 为每个微服务创建 ECR 仓库
resource "aws_ecr_repository" "services" {
  for_each = toset(local.services)

  name                 = "${var.project_name}-${var.environment}-${each.value}"
  image_tag_mutability = var.image_tag_mutability

  image_scanning_configuration {
    scan_on_push = var.scan_on_push
  }

  encryption_configuration {
    encryption_type = var.encryption_type
  }

  tags = {
    Name        = "${var.project_name}-${var.environment}-${each.value}"
    Environment = var.environment
    Service     = each.value
  }
}

# 生命周期策略 - 保留最近的镜像
resource "aws_ecr_lifecycle_policy" "services" {
  for_each   = aws_ecr_repository.services
  repository = each.value.name

  policy = jsonencode({
    rules = [{
      rulePriority = 1
      description  = "Keep last ${var.image_count_to_keep} images"
      selection = {
        tagStatus     = "any"
        countType     = "imageCountMoreThan"
        countNumber   = var.image_count_to_keep
      }
      action = {
        type = "expire"
      }
    }]
  })
}
