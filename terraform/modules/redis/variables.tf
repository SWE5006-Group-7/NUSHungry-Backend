variable "project_name" {
  type = string
}

variable "environment" {
  type = string
}

variable "vpc_id" {
  type = string
}

variable "private_subnet_ids" {
  type = list(string)
}

variable "allowed_security_groups" {
  type = list(string)
}

variable "engine_version" {
  type    = string
  default = "7.0"
}

variable "node_type" {
  type    = string
  default = "cache.t3.micro"
}

variable "num_cache_clusters" {
  type    = number
  default = 1
}

variable "at_rest_encryption_enabled" {
  type    = bool
  default = true
}

variable "transit_encryption_enabled" {
  type    = bool
  default = false
}

variable "auth_token_enabled" {
  type    = bool
  default = false
}

variable "auth_token" {
  type      = string
  sensitive = true
  default   = null
}

variable "automatic_failover_enabled" {
  type    = bool
  default = false
}

variable "multi_az_enabled" {
  type    = bool
  default = false
}

variable "maintenance_window" {
  type    = string
  default = "sun:05:00-sun:06:00"
}

variable "snapshot_window" {
  type    = string
  default = "03:00-04:00"
}

variable "snapshot_retention_limit" {
  type    = number
  default = 5
}

variable "parameter_group_name" {
  type    = string
  default = "default.redis7"
}
