#!/bin/bash
###############################################################################
# Review Service 停止脚本
# 用于停止所有服务
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

# 停止服务
stop_services() {
    log_info "正在停止服务..."
    
    # 返回到 review-service 根目录
    cd "$(dirname "$0")/.."
    
    # 停止服务
    docker-compose down
    
    log_info "服务已停止"
}

# 停止并删除数据卷
stop_with_volumes() {
    log_warn "将删除所有数据卷（数据库数据将丢失）"
    read -p "确认删除？(y/N): " -n 1 -r
    echo
    
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        cd "$(dirname "$0")/.."
        docker-compose down -v
        log_info "服务和数据卷已删除"
    else
        log_info "已取消"
    fi
}

# 主流程
main() {
    echo "============================================================"
    echo "          Review Service 停止脚本"
    echo "============================================================"
    echo ""
    
    # 检查参数
    if [ "$1" == "--clean" ] || [ "$1" == "-c" ]; then
        stop_with_volumes
    else
        stop_services
    fi
    
    echo ""
    log_info "提示:"
    echo "  - 重新启动:      ./scripts/start-services.sh"
    echo "  - 删除数据卷:    ./scripts/stop-services.sh --clean"
    echo "  - 查看容器:      docker ps -a"
    echo "  - 查看数据卷:    docker volume ls"
    echo ""
}

# 执行主流程
main "$@"
