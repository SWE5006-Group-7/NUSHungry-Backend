#!/bin/bash

###############################################################################
# Admin Service - 停止脚本 (Linux/Mac)
# 
# 用途: 停止 Admin Service 及其依赖服务
# 使用: ./scripts/stop-services.sh
###############################################################################

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

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

# 打印标题
print_header() {
    echo ""
    echo "============================================"
    echo "  Admin Service - 停止服务"
    echo "============================================"
    echo ""
}

# 停止服务
stop_services() {
    log_info "停止服务..."
    
    # 提供停止选项
    echo "请选择停止方式:"
    echo "  1) 停止容器（保留数据）"
    echo "  2) 停止并删除容器（保留数据卷）"
    echo "  3) 停止并删除所有（包括数据卷）⚠️"
    echo ""
    read -p "请输入选项 (1-3): " choice
    
    case $choice in
        1)
            log_info "停止容器..."
            docker-compose stop
            log_success "容器已停止"
            ;;
        2)
            log_info "停止并删除容器..."
            docker-compose down
            log_success "容器已删除"
            ;;
        3)
            log_warning "将删除所有数据，包括数据库数据！"
            read -p "确认删除所有数据? (yes/no): " confirm
            if [ "$confirm" = "yes" ]; then
                log_info "停止并删除所有..."
                docker-compose down -v
                log_success "所有数据已删除"
            else
                log_info "已取消删除操作"
            fi
            ;;
        *)
            log_warning "无效选项，仅停止容器"
            docker-compose stop
            ;;
    esac
}

# 显示状态
show_status() {
    echo ""
    log_info "当前状态:"
    docker-compose ps
    echo ""
}

# 主函数
main() {
    print_header
    stop_services
    show_status
    log_success "操作完成"
}

# 执行主函数
main
