#!/bin/bash
# Terraform 配置快速设置脚本

set -e

echo "🚀 NUSHungry Terraform 配置向导"
echo "================================"
echo ""

# 检查当前目录
if [ ! -f "terraform.tfvars.example" ]; then
    echo "❌ 错误: 请在 terraform/environments/dev/ 目录下运行此脚本"
    exit 1
fi

# 检查是否已有配置文件
if [ -f "terraform.tfvars" ]; then
    echo "⚠️  terraform.tfvars 已存在"
    read -p "是否覆盖? (y/N): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo "取消操作"
        exit 0
    fi
fi

echo "📝 开始配置..."
echo ""

# 生成密码
echo "1️⃣  生成数据库密码..."
DB_PASSWORD=$(openssl rand -base64 32 | tr -d '\n')
MONGODB_PASSWORD=$(openssl rand -base64 32 | tr -d '\n')
echo "✅ 密码已生成"
echo ""

# 询问区域
echo "2️⃣  选择 AWS 区域:"
echo "   1) ap-southeast-1 (新加坡) - 推荐"
echo "   2) us-east-1 (美国东部)"
echo "   3) eu-west-1 (爱尔兰)"
read -p "请选择 (默认 1): " region_choice

case $region_choice in
    2) AWS_REGION="us-east-1" ;;
    3) AWS_REGION="eu-west-1" ;;
    *) AWS_REGION="ap-southeast-1" ;;
esac

echo "✅ 区域: $AWS_REGION"
echo ""

# 创建配置文件
echo "3️⃣  创建 terraform.tfvars..."
cat > terraform.tfvars << EOF
# NUSHungry Terraform 配置
# ⚠️ 警告: 此文件包含敏感信息,不要提交到 Git!

project_name = "nushungry"
environment  = "dev"
aws_region   = "$AWS_REGION"

# 数据库密码 (自动生成)
db_password      = "$DB_PASSWORD"
mongodb_password = "$MONGODB_PASSWORD"
EOF

echo "✅ terraform.tfvars 已创建"
echo ""

# 显示密码
echo "4️⃣  生成的密码 (请妥善保存):"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "PostgreSQL 密码:"
echo "$DB_PASSWORD"
echo ""
echo "MongoDB 密码:"
echo "$MONGODB_PASSWORD"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

# 保存密码到安全文件
echo "$DB_PASSWORD" > .db_password.secret
echo "$MONGODB_PASSWORD" > .mongodb_password.secret
chmod 600 .db_password.secret .mongodb_password.secret

echo "💾 密码已保存到:"
echo "   - .db_password.secret"
echo "   - .mongodb_password.secret"
echo ""

# 检查 AWS CLI
echo "5️⃣  检查 AWS 配置..."
if command -v aws &> /dev/null; then
    if aws sts get-caller-identity &> /dev/null; then
        echo "✅ AWS CLI 已配置"
        aws sts get-caller-identity | grep -E "Account|Arn"
    else
        echo "⚠️  AWS CLI 未配置,请运行:"
        echo "   aws configure"
    fi
else
    echo "❌ AWS CLI 未安装"
    echo "   请参考: https://aws.amazon.com/cli/"
fi
echo ""

# 检查 Terraform
echo "6️⃣  检查 Terraform..."
if command -v terraform &> /dev/null; then
    echo "✅ Terraform 已安装: $(terraform version | head -1)"
else
    echo "❌ Terraform 未安装"
    echo "   请参考: https://www.terraform.io/downloads"
fi
echo ""

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "✅ 配置完成!"
echo ""
echo "📋 下一步操作:"
echo "   1. 确保 AWS CLI 已配置 (aws configure)"
echo "   2. 初始化 Terraform: terraform init"
echo "   3. 查看计划: terraform plan"
echo "   4. 应用配置: terraform apply"
echo ""
echo "📚 详细文档: ../../README.md"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
