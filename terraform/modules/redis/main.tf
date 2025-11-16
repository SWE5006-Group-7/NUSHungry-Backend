# ElastiCache Redis 模块

# 子网组
resource "aws_elasticache_subnet_group" "main" {
  name       = "${var.project_name}-${var.environment}-redis-subnet"
  subnet_ids = var.private_subnet_ids

  tags = {
    Name        = "${var.project_name}-${var.environment}-redis-subnet"
    Environment = var.environment
  }
}

# 安全组
resource "aws_security_group" "redis" {
  name        = "${var.project_name}-${var.environment}-redis-sg"
  description = "Security group for Redis"
  vpc_id      = var.vpc_id

  ingress {
    description     = "Redis from EKS"
    from_port       = 6379
    to_port         = 6379
    protocol        = "tcp"
    security_groups = var.allowed_security_groups
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name        = "${var.project_name}-${var.environment}-redis-sg"
    Environment = var.environment
  }
}

# Redis 集群
resource "aws_elasticache_replication_group" "redis" {
  replication_group_id = "${var.project_name}-${var.environment}-redis"
  description          = "Redis cache for ${var.project_name} ${var.environment}"

  engine         = "redis"
  engine_version = var.engine_version
  node_type      = var.node_type
  num_cache_clusters = var.num_cache_clusters
  port           = 6379

  subnet_group_name  = aws_elasticache_subnet_group.main.name
  security_group_ids = [aws_security_group.redis.id]

  at_rest_encryption_enabled = var.at_rest_encryption_enabled
  transit_encryption_enabled = var.transit_encryption_enabled
  auth_token                 = var.transit_encryption_enabled && var.auth_token_enabled ? var.auth_token : null

  automatic_failover_enabled = var.automatic_failover_enabled
  multi_az_enabled           = var.multi_az_enabled

  maintenance_window       = var.maintenance_window
  snapshot_window          = var.snapshot_window
  snapshot_retention_limit = var.snapshot_retention_limit

  parameter_group_name = var.parameter_group_name

  tags = {
    Name        = "${var.project_name}-${var.environment}-redis"
    Environment = var.environment
  }
}
