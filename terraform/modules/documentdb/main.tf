# DocumentDB (MongoDB 兼容) 模块

# 子网组
resource "aws_docdb_subnet_group" "main" {
  name       = "${var.project_name}-${var.environment}-docdb-subnet"
  subnet_ids = var.private_subnet_ids

  tags = {
    Name        = "${var.project_name}-${var.environment}-docdb-subnet"
    Environment = var.environment
  }
}

# 安全组
resource "aws_security_group" "docdb" {
  name        = "${var.project_name}-${var.environment}-docdb-sg"
  description = "Security group for DocumentDB"
  vpc_id      = var.vpc_id

  ingress {
    description     = "MongoDB from EKS"
    from_port       = 27017
    to_port         = 27017
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
    Name        = "${var.project_name}-${var.environment}-docdb-sg"
    Environment = var.environment
  }
}

# DocumentDB 集群
resource "aws_docdb_cluster" "main" {
  cluster_identifier      = "${var.project_name}-${var.environment}-docdb"
  engine                  = "docdb"
  engine_version          = var.engine_version
  master_username         = var.master_username
  master_password         = var.master_password
  port                    = 27017

  db_subnet_group_name    = aws_docdb_subnet_group.main.name
  vpc_security_group_ids  = [aws_security_group.docdb.id]

  backup_retention_period = var.backup_retention_period
  preferred_backup_window = var.backup_window
  preferred_maintenance_window = var.maintenance_window

  storage_encrypted       = var.storage_encrypted
  enabled_cloudwatch_logs_exports = ["audit", "profiler"]

  skip_final_snapshot     = var.skip_final_snapshot
  final_snapshot_identifier = var.skip_final_snapshot ? null : "${var.project_name}-${var.environment}-docdb-final-${formatdate("YYYY-MM-DD-hhmm", timestamp())}"

  deletion_protection     = var.deletion_protection

  tags = {
    Name        = "${var.project_name}-${var.environment}-docdb"
    Environment = var.environment
  }
}

# DocumentDB 实例
resource "aws_docdb_cluster_instance" "main" {
  count              = var.instance_count
  identifier         = "${var.project_name}-${var.environment}-docdb-${count.index + 1}"
  cluster_identifier = aws_docdb_cluster.main.id
  instance_class     = var.instance_class

  tags = {
    Name        = "${var.project_name}-${var.environment}-docdb-${count.index + 1}"
    Environment = var.environment
  }
}
