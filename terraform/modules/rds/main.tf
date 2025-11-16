# RDS PostgreSQL 模块

# 子网组
resource "aws_db_subnet_group" "main" {
  name       = "${var.project_name}-${var.environment}-rds-subnet"
  subnet_ids = var.private_subnet_ids

  tags = {
    Name        = "${var.project_name}-${var.environment}-rds-subnet"
    Environment = var.environment
  }
}

# 安全组
resource "aws_security_group" "rds" {
  name        = "${var.project_name}-${var.environment}-rds-sg"
  description = "Security group for RDS PostgreSQL"
  vpc_id      = var.vpc_id

  ingress {
    description     = "PostgreSQL from EKS"
    from_port       = 5432
    to_port         = 5432
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
    Name        = "${var.project_name}-${var.environment}-rds-sg"
    Environment = var.environment
  }
}

# RDS 实例
resource "aws_db_instance" "postgres" {
  identifier     = "${var.project_name}-${var.environment}-postgres"
  engine         = "postgres"
  engine_version = var.engine_version
  instance_class = var.instance_class

  allocated_storage     = var.allocated_storage
  max_allocated_storage = var.max_allocated_storage
  storage_type          = var.storage_type
  storage_encrypted     = var.storage_encrypted

  db_name  = var.database_name
  username = var.master_username
  password = var.master_password
  port     = 5432

  db_subnet_group_name   = aws_db_subnet_group.main.name
  vpc_security_group_ids = [aws_security_group.rds.id]
  publicly_accessible    = var.publicly_accessible

  multi_az               = var.multi_az
  backup_retention_period = var.backup_retention_period
  backup_window          = var.backup_window
  maintenance_window     = var.maintenance_window

  enabled_cloudwatch_logs_exports = var.enabled_cloudwatch_logs_exports
  monitoring_interval             = var.monitoring_interval
  monitoring_role_arn             = var.monitoring_interval > 0 ? aws_iam_role.rds_monitoring[0].arn : null

  skip_final_snapshot       = var.skip_final_snapshot
  final_snapshot_identifier = var.skip_final_snapshot ? null : "${var.project_name}-${var.environment}-postgres-final-${formatdate("YYYY-MM-DD-hhmm", timestamp())}"

  deletion_protection = var.deletion_protection
  apply_immediately   = var.apply_immediately

  tags = {
    Name        = "${var.project_name}-${var.environment}-postgres"
    Environment = var.environment
  }
}

# IAM 角色用于增强监控
resource "aws_iam_role" "rds_monitoring" {
  count = var.monitoring_interval > 0 ? 1 : 0
  name  = "${var.project_name}-${var.environment}-rds-monitoring-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Action = "sts:AssumeRole"
      Effect = "Allow"
      Principal = {
        Service = "monitoring.rds.amazonaws.com"
      }
    }]
  })

  tags = {
    Name        = "${var.project_name}-${var.environment}-rds-monitoring-role"
    Environment = var.environment
  }
}

# 附加策略到 IAM 角色
resource "aws_iam_role_policy_attachment" "rds_monitoring" {
  count      = var.monitoring_interval > 0 ? 1 : 0
  role       = aws_iam_role.rds_monitoring[0].name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonRDSEnhancedMonitoringRole"
}
