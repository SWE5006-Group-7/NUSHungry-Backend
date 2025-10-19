#!/bin/bash

###############################################################################
# Admin Service - 启动脚本 (Linux/Mac)
# 
# 用途: 启动 Admin Service 及其依赖服务
# 使用: ./scripts/start-services.sh
###############################################################################

set -e  # 遇到错误立即退出

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 日志函数
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 打印标题
print_header() {
    echo ""
    echo "============================================"
    echo "  Admin Service - 启动服务"
    echo "============================================"
    echo ""
}

# 检查 Docker 是否安装
check_docker() {
    if ! command -v docker &> /dev/null; then
        log_error "Docker 未安装，请先安装 Docker"
        exit 1
    fi
    
    if ! command -v docker-compose &> /dev/null; then
        log_error "Docker Compose 未安装，请先安装 Docker Compose"
        exit 1
    fi
    
    log_success "Docker 环境检查通过"
}

# 检查端口占用
check_ports() {
    log_info "检查端口占用..."
    
    local ports=(8082 5432 5672 15672 9082)
    local port_occupied=false
    
    for port in "${ports[@]}"; do
        if lsof -Pi :$port -sTCP:LISTEN -t >/dev/null 2>&1 ; then
            log_warning "端口 $port 已被占用"
            port_occupied=true
        fi
    done
    
    if [ "$port_occupied" = true ]; then
        log_warning "部分端口已被占用，可能导致服务启动失败"
        read -p "是否继续启动? (y/n) " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            log_info "已取消启动"
            exit 0
        fi
    else
        log_success "端口检查通过"
    fi
}

# 创建必要的目录
create_directories() {
    log_info "创建必要的目录..."
    mkdir -p logs
    log_success "目录创建完成"
}

# 启动服务
start_services() {
    log_info "启动服务..."
    docker-compose up -d
}

# 等待服务启动
wait_for_services() {
    log_info "等待服务启动..."
    
    local max_attempts=60
    local attempt=0
    
    # 等待 PostgreSQL
    log_info "等待 PostgreSQL 启动..."
    until docker-compose exec -T postgres pg_isready -U admin -d admin_service &> /dev/null || [ $attempt -eq $max_attempts ]; do
        sleep 1
        attempt=$((attempt + 1))
        echo -n "."
    done
    echo ""
    
    if [ $attempt -eq $max_attempts ]; then
        log_error "PostgreSQL 启动超时"
        exit 1
    fi
    log_success "PostgreSQL 已启动"
    
    # 等待 RabbitMQ
    log_info "等待 RabbitMQ 启动..."
    attempt=0
    until docker-compose exec -T rabbitmq rabbitmq-diagnostics ping &> /dev/null || [ $attempt -eq $max_attempts ]; do
        sleep 1
        attempt=$((attempt + 1))
        echo -n "."
    done
    echo ""
    
    if [ $attempt -eq $max_attempts ]; then
        log_error "RabbitMQ 启动超时"
        exit 1
    fi
    log_success "RabbitMQ 已启动"
    
    # 等待 Admin Service
    log_info "等待 Admin Service 启动..."
    attempt=0
    until curl -sf http://localhost:8082/actuator/health &> /dev/null || [ $attempt -eq $max_attempts ]; do
        sleep 2
        attempt=$((attempt + 1))
        echo -n "."
    done
    echo ""
    
    if [ $attempt -eq $max_attempts ]; then
        log_error "Admin Service 启动超时"
        log_info "查看日志: docker-compose logs admin-service"
        exit 1
    fi
    log_success "Admin Service 已启动"
}

# 显示服务状态
show_status() {
    echo ""
    log_info "服务状态:"
    docker-compose ps
    echo ""
}

# 显示访问信息
show_access_info() {
    echo ""
    echo "============================================"
    echo "  服务已成功启动！"
    echo "============================================"
    echo ""
    echo "📋 服务访问信息:"
    echo ""
    echo "  🔹 Admin Service API:"
    echo "     http://localhost:8082"
    echo ""
    echo "  🔹 Swagger UI (API 文档):"
    echo "     http://localhost:8082/swagger-ui.html"
    echo ""
    echo "  🔹 Health Check:"
    echo "     http://localhost:8082/actuator/health"
    echo ""
    echo "  🔹 PostgreSQL:"
    echo "     Host: localhost:5432"
    echo "     Database: admin_service"
    echo "     Username: admin"
    echo "     Password: password123"
    echo ""
    echo "  🔹 RabbitMQ 管理界面:"
    echo "     http://localhost:15672"
    echo "     Username: admin"
    echo "     Password: password123"
    echo ""
    echo "============================================"
    echo ""
    echo "📌 默认管理员账号:"
    echo "   Username: admin"
    echo "   Password: Admin123!"
    echo ""
    echo "============================================"
    echo ""
    echo "📝 常用命令:"
    echo "  - 查看日志:   docker-compose logs -f"
    echo "  - 停止服务:   ./scripts/stop-services.sh"
    echo "  - 重启服务:   docker-compose restart"
    echo ""
}

# 主函数
main() {
    print_header
    check_docker
    check_ports
    create_directories
    start_services
    wait_for_services
    show_status
    show_access_info
}

# 执行主函数
main
