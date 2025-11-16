variable "project_name" {
  description = "项目名称"
  type        = string
}

variable "environment" {
  description = "环境名称"
  type        = string
}

variable "vpc_cidr" {
  description = "VPC CIDR 块"
  type        = string
  default     = "10.0.0.0/16"
}

variable "public_subnet_count" {
  description = "公有子网数量"
  type        = number
  default     = 2
}

variable "private_subnet_count" {
  description = "私有子网数量"
  type        = number
  default     = 2
}

variable "enable_nat_gateway" {
  description = "是否启用 NAT Gateway"
  type        = bool
  default     = true
}

variable "nat_gateway_count" {
  description = "NAT Gateway 数量 (1 = 单个共享, 2+ = 每个可用区一个)"
  type        = number
  default     = 1
}

variable "enable_flow_logs" {
  description = "是否启用 VPC Flow Logs"
  type        = bool
  default     = false
}
