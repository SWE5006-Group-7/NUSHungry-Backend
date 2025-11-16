terraform {
  required_version = ">= 1.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }

  # 可选: 使用 S3 作为后端存储 Terraform 状态
  # backend "s3" {
  #   bucket = "nushungry-terraform-state"
  #   key    = "dev/terraform.tfstate"
  #   region = "ap-southeast-1"
  #   encrypt = true
  #   dynamodb_table = "terraform-state-lock"
  # }
}

provider "aws" {
  region = var.aws_region

  default_tags {
    tags = {
      Project     = var.project_name
      Environment = var.environment
      ManagedBy   = "Terraform"
    }
  }
}

# VPC 模块
module "vpc" {
  source = "../../modules/vpc"

  project_name         = var.project_name
  environment          = var.environment
  vpc_cidr             = var.vpc_cidr
  public_subnet_count  = 2
  private_subnet_count = 2
  enable_nat_gateway   = true
  nat_gateway_count    = 2
  enable_flow_logs     = false
}

# EKS 模块
module "eks" {
  source = "../../modules/eks"

  project_name       = var.project_name
  environment        = var.environment
  vpc_id             = module.vpc.vpc_id
  public_subnet_ids  = module.vpc.public_subnet_ids
  private_subnet_ids = module.vpc.private_subnet_ids

  kubernetes_version = "1.28"
  instance_types     = ["t3.medium"]
  capacity_type      = "ON_DEMAND"
  desired_capacity   = 2
  min_capacity       = 1
  max_capacity       = 4

  depends_on = [module.vpc]
}

# RDS PostgreSQL 模块
module "rds" {
  source = "../../modules/rds"

  project_name              = var.project_name
  environment               = var.environment
  vpc_id                    = module.vpc.vpc_id
  private_subnet_ids        = module.vpc.private_subnet_ids
  allowed_security_groups   = [module.eks.cluster_security_group_id]

  engine_version            = "16.1"
  instance_class            = "db.t3.micro"
  allocated_storage         = 20
  max_allocated_storage     = 100
  database_name             = "nushungry"
  master_username           = "postgres"
  master_password           = var.db_password
  multi_az                  = false
  backup_retention_period   = 7
  skip_final_snapshot       = true
  deletion_protection       = false

  depends_on = [module.vpc]
}

# DocumentDB 模块
module "documentdb" {
  source = "../../modules/documentdb"

  project_name            = var.project_name
  environment             = var.environment
  vpc_id                  = module.vpc.vpc_id
  private_subnet_ids      = module.vpc.private_subnet_ids
  allowed_security_groups = [module.eks.cluster_security_group_id]

  engine_version          = "5.0.0"
  instance_class          = "db.t3.medium"
  instance_count          = 1
  master_username         = "admin"
  master_password         = var.mongodb_password
  backup_retention_period = 7
  skip_final_snapshot     = true
  deletion_protection     = false

  depends_on = [module.vpc]
}

# Redis 模块
module "redis" {
  source = "../../modules/redis"

  project_name            = var.project_name
  environment             = var.environment
  vpc_id                  = module.vpc.vpc_id
  private_subnet_ids      = module.vpc.private_subnet_ids
  allowed_security_groups = [module.eks.cluster_security_group_id]

  engine_version              = "7.0"
  node_type                   = "cache.t3.micro"
  num_cache_clusters          = 1
  automatic_failover_enabled  = false
  multi_az_enabled            = false
  auth_token_enabled          = false
  snapshot_retention_limit    = 5

  depends_on = [module.vpc]
}

# ECR 模块
module "ecr" {
  source = "../../modules/ecr"

  project_name     = var.project_name
  environment      = var.environment
  image_count_to_keep = 10
}
