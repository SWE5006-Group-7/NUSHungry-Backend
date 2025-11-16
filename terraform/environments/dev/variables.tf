variable "project_name" {
  description = "项目名称"
  type        = string
  default     = "nushungry"
}

variable "environment" {
  description = "环境名称"
  type        = string
  default     = "dev"
}

variable "aws_region" {
  description = "AWS 区域"
  type        = string
  default     = "ap-southeast-1"
}

variable "vpc_cidr" {
  description = "VPC CIDR 块"
  type        = string
  default     = "10.0.0.0/16"
}

variable "db_password" {
  description = "PostgreSQL 主密码"
  type        = string
  sensitive   = true
}

variable "mongodb_password" {
  description = "MongoDB 主密码"
  type        = string
  sensitive   = true
}
