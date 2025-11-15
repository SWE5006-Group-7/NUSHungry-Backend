#!/bin/bash
###############################################################################
# Review Service 启动脚本
# 用于启动所有依赖服务和应用本身
###############################################################################

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 日志函数
log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 检查 Docker 是否运行
check_docker() {
    if ! docker info > /dev/null 2>&1; then
        log_error "Docker 未运行，请启动 Docker"
        exit 1
    fi
    log_info "Docker 运行正常"
}

# 检查 docker-compose 是否安装
check_docker_compose() {
    if ! command -v docker-compose &> /dev/null; then
        log_error "docker-compose 未安装"
        exit 1
    fi
    log_info "docker-compose 已安装: $(docker-compose --version)"
}

# 启动服务
start_services() {
    log_info "正在启动服务..."
    
    # 返回到 review-service 根目录
    cd "$(dirname "$0")/.."
    
    # 构建并启动服务
    docker-compose up -d --build
    
    log_info "服务启动命令已执行"
}

# 等待服务健康
wait_for_health() {
    local service=$1
    local max_attempts=30
    local attempt=1
    
    log_info "等待 $service 服务健康..."
    
    while [ $attempt -le $max_attempts ]; do
        if docker-compose ps | grep -q "$service.*healthy"; then
            log_info "$service 服务已就绪 ✓"
            return 0
        fi
        
        echo -n "."
        sleep 2
        attempt=$((attempt + 1))
    done
    
    log_warn "$service 服务未就绪，请手动检查"
    return 1
}

# 显示服务状态
show_status() {
    log_info "服务状态:"
    docker-compose ps
}

# 显示服务URL
show_urls() {
    echo ""
    log_info "服务访问地址:"
    echo "  - Review Service API:      http://localhost:8084"
    echo "  - Review Service Swagger:  http://localhost:8084/swagger-ui.html"
    echo "  - Review Service Health:   http://localhost:8084/actuator/health"
    echo "  - RabbitMQ Management:     http://localhost:15672 (admin/password123)"
    echo "  - MongoDB:                 mongodb://localhost:27017 (admin/password123)"
}

# 显示日志命令
show_log_commands() {
    echo ""
    log_info "查看日志命令:"
    echo "  - 所有服务:       docker-compose logs -f"
    echo "  - Review Service: docker-compose logs -f review-service"
    echo "  - MongoDB:        docker-compose logs -f mongodb"
    echo "  - RabbitMQ:       docker-compose logs -f rabbitmq"
}

# 主流程
main() {
    echo "============================================================"
    echo "          Review Service 启动脚本"
    echo "============================================================"
    echo ""
    
    # 检查环境
    check_docker
    check_docker_compose
    
    # 启动服务
    start_services
    
    echo ""
    log_info "等待服务启动..."
    sleep 5
    
    # 等待关键服务健康
    wait_for_health "mongodb"
    wait_for_health "rabbitmq"
    wait_for_health "review-service"
    
    echo ""
    # 显示状态
    show_status
    
    # 显示访问地址
    show_urls
    
    # 显示日志命令
    show_log_commands
    
    echo ""
    echo "============================================================"
    log_info "所有服务已启动完成 ✓"
    echo "============================================================"
}

# 执行主流程
main
