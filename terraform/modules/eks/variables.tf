variable "project_name" {
  type = string
}

variable "environment" {
  type = string
}

variable "vpc_id" {
  type = string
}

variable "public_subnet_ids" {
  type = list(string)
}

variable "private_subnet_ids" {
  type = list(string)
}

variable "kubernetes_version" {
  description = "Kubernetes 版本"
  type        = string
  default     = "1.28"
}

variable "cluster_log_types" {
  description = "启用的集群日志类型"
  type        = list(string)
  default     = ["api", "audit", "authenticator"]
}

variable "instance_types" {
  description = "节点实例类型"
  type        = list(string)
  default     = ["t3.medium"]
}

variable "capacity_type" {
  description = "容量类型 (ON_DEMAND 或 SPOT)"
  type        = string
  default     = "ON_DEMAND"
}

variable "disk_size" {
  description = "节点磁盘大小 (GB)"
  type        = number
  default     = 20
}

variable "desired_capacity" {
  description = "期望节点数量"
  type        = number
  default     = 2
}

variable "min_capacity" {
  description = "最小节点数量"
  type        = number
  default     = 1
}

variable "max_capacity" {
  description = "最大节点数量"
  type        = number
  default     = 4
}

variable "ebs_csi_driver_version" {
  description = "EBS CSI Driver 版本"
  type        = string
  default     = "v1.35.0-eksbuild.1"  # 可通过 aws eks describe-addon-versions --addon-name aws-ebs-csi-driver 查询最新版本
}
