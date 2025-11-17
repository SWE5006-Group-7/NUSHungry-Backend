# ============================================================================
# Amazon MQ (RabbitMQ) 模块
# ============================================================================
# 用途: 创建托管的 RabbitMQ 消息队列服务
# 功能:
#   - 单可用区或多可用区部署
#   - 自动故障转移
#   - 自动备份和补丁管理
#   - 集成 CloudWatch 日志和监控
# ============================================================================

# ============================================================================
# Amazon MQ Broker 配置
# ============================================================================

resource "aws_mq_configuration" "rabbitmq" {
  name           = "${var.project_name}-${var.environment}-rabbitmq-config"
  description    = "RabbitMQ configuration for ${var.project_name}"
  engine_type    = "RabbitMQ"
  engine_version = var.rabbitmq_engine_version

  data = <<DATA
# Default RabbitMQ delivery acknowledgement timeout is 30 minutes in milliseconds
consumer_timeout = 1800000

# Enable lazy queues for better memory management
queue_master_locator = min-masters
DATA

  tags = {
    Name        = "${var.project_name}-${var.environment}-rabbitmq-config"
    Environment = var.environment
    Project     = var.project_name
    ManagedBy   = "terraform"
  }
}

# ============================================================================
# Amazon MQ Broker
# ============================================================================

resource "aws_mq_broker" "rabbitmq" {
  broker_name = "${var.project_name}-${var.environment}-rabbitmq"

  # 引擎配置
  engine_type        = "RabbitMQ"
  engine_version     = var.rabbitmq_engine_version
  host_instance_type = var.instance_type
  deployment_mode    = var.deployment_mode # SINGLE_INSTANCE 或 CLUSTER_MULTI_AZ

  # 配置引用
  configuration {
    id       = aws_mq_configuration.rabbitmq.id
    revision = aws_mq_configuration.rabbitmq.latest_revision
  }

  # 用户认证
  user {
    username = var.rabbitmq_username
    password = var.rabbitmq_password
  }

  # 网络配置
  subnet_ids         = var.deployment_mode == "CLUSTER_MULTI_AZ" ? var.subnet_ids : [var.subnet_ids[0]]
  security_groups    = [aws_security_group.amazonmq.id]
  publicly_accessible = false

  # 日志配置
  logs {
    general = true
  }

  # 维护窗口
  maintenance_window_start_time {
    day_of_week = var.maintenance_day_of_week
    time_of_day = var.maintenance_time_of_day
    time_zone   = var.maintenance_time_zone
  }

  # 自动小版本升级
  auto_minor_version_upgrade = var.auto_minor_version_upgrade

  # 存储类型
  storage_type = var.storage_type # EBS 或 EFS

  tags = {
    Name        = "${var.project_name}-${var.environment}-rabbitmq"
    Environment = var.environment
    Project     = var.project_name
    ManagedBy   = "terraform"
    Service     = "amazonmq"
  }
}

# ============================================================================
# Security Group for Amazon MQ
# ============================================================================

resource "aws_security_group" "amazonmq" {
  name_prefix = "${var.project_name}-${var.environment}-amazonmq-"
  description = "Security group for Amazon MQ RabbitMQ broker"
  vpc_id      = var.vpc_id

  # RabbitMQ AMQP 端口 (5671 for TLS)
  ingress {
    description     = "RabbitMQ AMQP with TLS from EKS"
    from_port       = 5671
    to_port         = 5671
    protocol        = "tcp"
    security_groups = var.allowed_security_group_ids
  }

  # RabbitMQ Web Console (15671 for TLS)
  ingress {
    description     = "RabbitMQ Management Console with TLS"
    from_port       = 15671
    to_port         = 15671
    protocol        = "tcp"
    security_groups = var.allowed_security_group_ids
  }

  # 出站流量
  egress {
    description = "Allow all outbound traffic"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name        = "${var.project_name}-${var.environment}-amazonmq-sg"
    Environment = var.environment
    Project     = var.project_name
    ManagedBy   = "terraform"
  }

  lifecycle {
    create_before_destroy = true
  }
}

# ============================================================================
# CloudWatch 日志组
# ============================================================================

resource "aws_cloudwatch_log_group" "amazonmq" {
  name              = "/aws/amazonmq/${var.project_name}-${var.environment}"
  retention_in_days = var.log_retention_days

  tags = {
    Name        = "${var.project_name}-${var.environment}-amazonmq-logs"
    Environment = var.environment
    Project     = var.project_name
    ManagedBy   = "terraform"
  }
}

# ============================================================================
# CloudWatch 告警 - 连接数过高
# ============================================================================

resource "aws_cloudwatch_metric_alarm" "rabbitmq_connection_count" {
  count = var.enable_cloudwatch_alarms ? 1 : 0

  alarm_name          = "${var.project_name}-${var.environment}-rabbitmq-high-connections"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "2"
  metric_name         = "ConnectionCount"
  namespace           = "AWS/AmazonMQ"
  period              = "300"
  statistic           = "Average"
  threshold           = var.connection_count_threshold
  alarm_description   = "This metric monitors RabbitMQ connection count"
  treat_missing_data  = "notBreaching"

  dimensions = {
    Broker = aws_mq_broker.rabbitmq.broker_name
  }

  alarm_actions = var.alarm_sns_topic_arns

  tags = {
    Name        = "${var.project_name}-${var.environment}-rabbitmq-connection-alarm"
    Environment = var.environment
    Project     = var.project_name
  }
}

# ============================================================================
# CloudWatch 告警 - CPU 使用率过高
# ============================================================================

resource "aws_cloudwatch_metric_alarm" "rabbitmq_cpu_utilization" {
  count = var.enable_cloudwatch_alarms ? 1 : 0

  alarm_name          = "${var.project_name}-${var.environment}-rabbitmq-high-cpu"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "2"
  metric_name         = "CpuUtilization"
  namespace           = "AWS/AmazonMQ"
  period              = "300"
  statistic           = "Average"
  threshold           = var.cpu_utilization_threshold
  alarm_description   = "This metric monitors RabbitMQ CPU utilization"
  treat_missing_data  = "notBreaching"

  dimensions = {
    Broker = aws_mq_broker.rabbitmq.broker_name
  }

  alarm_actions = var.alarm_sns_topic_arns

  tags = {
    Name        = "${var.project_name}-${var.environment}-rabbitmq-cpu-alarm"
    Environment = var.environment
    Project     = var.project_name
  }
}

# ============================================================================
# CloudWatch 告警 - 内存使用率过高
# ============================================================================

resource "aws_cloudwatch_metric_alarm" "rabbitmq_memory_usage" {
  count = var.enable_cloudwatch_alarms ? 1 : 0

  alarm_name          = "${var.project_name}-${var.environment}-rabbitmq-high-memory"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "2"
  metric_name         = "RabbitMQMemUsed"
  namespace           = "AWS/AmazonMQ"
  period              = "300"
  statistic           = "Average"
  threshold           = var.memory_usage_threshold
  alarm_description   = "This metric monitors RabbitMQ memory usage"
  treat_missing_data  = "notBreaching"

  dimensions = {
    Broker = aws_mq_broker.rabbitmq.broker_name
  }

  alarm_actions = var.alarm_sns_topic_arns

  tags = {
    Name        = "${var.project_name}-${var.environment}-rabbitmq-memory-alarm"
    Environment = var.environment
    Project     = var.project_name
  }
}
