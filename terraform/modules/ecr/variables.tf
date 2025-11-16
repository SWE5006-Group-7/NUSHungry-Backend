variable "project_name" {
  description = "项目名称"
  type        = string
}

variable "environment" {
  description = "环境名称"
  type        = string
}

variable "image_tag_mutability" {
  description = "镜像标签可变性"
  type        = string
  default     = "MUTABLE"
}

variable "scan_on_push" {
  description = "推送时是否扫描"
  type        = bool
  default     = true
}

variable "encryption_type" {
  description = "加密类型"
  type        = string
  default     = "AES256"
}

variable "image_count_to_keep" {
  description = "保留的镜像数量"
  type        = number
  default     = 10
}
