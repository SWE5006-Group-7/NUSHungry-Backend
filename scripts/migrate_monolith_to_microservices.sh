#!/bin/bash

################################################################################
# NUS Hungry - 单体架构到微服务架构数据迁移脚本
# 
# 功能:
#   1. 备份单体数据库 (MySQL)
#   2. 创建微服务数据库 (PostgreSQL × 4, MongoDB × 1)
#   3. 执行数据库初始化脚本
#   4. 迁移数据到各个微服务
#   5. 验证数据完整性
#
# 使用方法:
#   ./scripts/migrate_monolith_to_microservices.sh [--dry-run] [--skip-backup]
#
# 选项:
#   --dry-run       仅显示要执行的命令，不实际执行
#   --skip-backup   跳过数据库备份步骤（仅用于测试）
#   --help          显示帮助信息
#
# 前置条件:
#   - MySQL 服务运行中（单体数据库）
#   - PostgreSQL 服务运行中（端口 5432-5435）
#   - MongoDB 服务运行中（端口 27017）
#   - Python 3 已安装（用于 Review Service 迁移）
#   - pymysql, pymongo Python 包已安装
#
# 作者: NUS Hungry Team
# 日期: 2025-01-19
################################################################################

set -e  # 遇到错误立即退出

# ==================== 配置常量 ====================

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 项目根目录
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BACKUP_DIR="${PROJECT_ROOT}/backups"
LOG_DIR="${PROJECT_ROOT}/logs"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
LOG_FILE="${LOG_DIR}/migration_${TIMESTAMP}.log"

# 单体数据库配置 (MySQL)
MYSQL_HOST="${MYSQL_HOST:-localhost}"
MYSQL_PORT="${MYSQL_PORT:-3306}"
MYSQL_USER="${MYSQL_USER:-root}"
MYSQL_PASSWORD="${MYSQL_PASSWORD:-123456}"
MYSQL_DATABASE="${MYSQL_DATABASE:-nushungry_db}"

# PostgreSQL 配置
PG_HOST="${PG_HOST:-localhost}"
PG_USER="${PG_USER:-postgres}"
PG_PASSWORD="${PG_PASSWORD:-postgres}"

# MongoDB 配置
MONGO_HOST="${MONGO_HOST:-localhost}"
MONGO_PORT="${MONGO_PORT:-27017}"
MONGO_URI="mongodb://${MONGO_HOST}:${MONGO_PORT}/"

# 微服务数据库配置
declare -A PG_DATABASES=(
    ["admin_service"]="5432"
    ["cafeteria_service"]="5433"
    ["media_service"]="5434"
    ["preference_service"]="5435"
)

# 命令行参数
DRY_RUN=false
SKIP_BACKUP=false

# ==================== 辅助函数 ====================

# 日志函数
log() {
    echo -e "${GREEN}[$(date '+%Y-%m-%d %H:%M:%S')]${NC} $1" | tee -a "$LOG_FILE"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1" | tee -a "$LOG_FILE"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1" | tee -a "$LOG_FILE"
}

log_info() {
    echo -e "${BLUE}[INFO]${NC} $1" | tee -a "$LOG_FILE"
}

# 执行命令（支持 dry-run 模式）
run_command() {
    local cmd="$1"
    log_info "执行: $cmd"
    
    if [ "$DRY_RUN" = true ]; then
        echo "  [DRY-RUN] 跳过执行"
        return 0
    fi
    
    if eval "$cmd" >> "$LOG_FILE" 2>&1; then
        log "✓ 成功"
        return 0
    else
        log_error "✗ 失败"
        return 1
    fi
}

# 检查命令是否存在
check_command() {
    if ! command -v "$1" &> /dev/null; then
        log_error "命令 '$1' 未找到，请先安装"
        return 1
    fi
    return 0
}

# 检查端口是否开放
check_port() {
    local host=$1
    local port=$2
    local service=$3
    
    if timeout 2 bash -c "</dev/tcp/$host/$port" 2>/dev/null; then
        log "✓ $service 运行中 (${host}:${port})"
        return 0
    else
        log_error "✗ $service 未运行 (${host}:${port})"
        return 1
    fi
}

# 确认操作
confirm() {
    local message=$1
    if [ "$DRY_RUN" = true ]; then
        return 0
    fi
    
    echo -e "${YELLOW}⚠️  $message${NC}"
    read -p "是否继续? [y/N] " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        log_warning "用户取消操作"
        exit 1
    fi
}

# 显示帮助信息
show_help() {
    cat << EOF
NUS Hungry - 数据迁移脚本

使用方法:
  ./scripts/migrate_monolith_to_microservices.sh [选项]

选项:
  --dry-run       仅显示要执行的命令，不实际执行
  --skip-backup   跳过数据库备份步骤（仅用于测试）
  --help          显示此帮助信息

示例:
  # 正常迁移
  ./scripts/migrate_monolith_to_microservices.sh

  # 测试模式（不实际执行）
  ./scripts/migrate_monolith_to_microservices.sh --dry-run

  # 跳过备份（用于测试环境）
  ./scripts/migrate_monolith_to_microservices.sh --skip-backup

环境变量:
  MYSQL_HOST        MySQL 主机地址（默认: localhost）
  MYSQL_PORT        MySQL 端口（默认: 3306）
  MYSQL_USER        MySQL 用户名（默认: root）
  MYSQL_PASSWORD    MySQL 密码（默认: 123456）
  PG_HOST           PostgreSQL 主机地址（默认: localhost）
  PG_USER           PostgreSQL 用户名（默认: postgres）
  PG_PASSWORD       PostgreSQL 密码（默认: postgres）
  MONGO_HOST        MongoDB 主机地址（默认: localhost）
  MONGO_PORT        MongoDB 端口（默认: 27017）

EOF
    exit 0
}

# ==================== 主要功能函数 ====================

# 1. 前置检查
check_prerequisites() {
    log "=========================================="
    log "步骤 1/6: 前置条件检查"
    log "=========================================="
    
    # 检查必要的命令
    log_info "检查必要的命令..."
    local commands=("mysql" "psql" "mongo" "python3" "mysqldump")
    for cmd in "${commands[@]}"; do
        check_command "$cmd" || exit 1
    done
    
    # 检查 Python 依赖
    log_info "检查 Python 依赖..."
    if python3 -c "import pymysql, pymongo" 2>/dev/null; then
        log "✓ Python 依赖已安装"
    else
        log_warning "Python 依赖未安装，正在安装..."
        run_command "pip3 install pymysql pymongo"
    fi
    
    # 检查服务状态
    log_info "检查数据库服务状态..."
    check_port "$MYSQL_HOST" "$MYSQL_PORT" "MySQL" || exit 1
    check_port "$PG_HOST" "5432" "PostgreSQL" || exit 1
    check_port "$MONGO_HOST" "$MONGO_PORT" "MongoDB" || exit 1
    
    # 检查 MySQL 连接
    log_info "测试 MySQL 连接..."
    if mysql -h "$MYSQL_HOST" -P "$MYSQL_PORT" -u "$MYSQL_USER" -p"$MYSQL_PASSWORD" -e "SELECT 1;" &>/dev/null; then
        log "✓ MySQL 连接成功"
    else
        log_error "MySQL 连接失败，请检查配置"
        exit 1
    fi
    
    # 创建目录
    mkdir -p "$BACKUP_DIR" "$LOG_DIR"
    
    log "✓ 前置检查完成"
    echo ""
}

# 2. 备份单体数据库
backup_monolith_database() {
    if [ "$SKIP_BACKUP" = true ]; then
        log_warning "跳过数据库备份（--skip-backup）"
        return 0
    fi
    
    log "=========================================="
    log "步骤 2/6: 备份单体数据库"
    log "=========================================="
    
    local backup_file="${BACKUP_DIR}/nushungry_db_backup_${TIMESTAMP}.sql"
    
    confirm "即将备份 MySQL 数据库: ${MYSQL_DATABASE}"
    
    log_info "备份文件: $backup_file"
    run_command "mysqldump -h $MYSQL_HOST -P $MYSQL_PORT -u $MYSQL_USER -p$MYSQL_PASSWORD \
        --single-transaction --routines --triggers --events \
        $MYSQL_DATABASE > $backup_file"
    
    # 验证备份文件
    if [ -f "$backup_file" ] && [ -s "$backup_file" ]; then
        local size=$(du -h "$backup_file" | cut -f1)
        log "✓ 备份完成: $backup_file (大小: $size)"
    else
        log_error "备份文件为空或不存在"
        exit 1
    fi
    
    echo ""
}

# 3. 创建微服务数据库
create_microservice_databases() {
    log "=========================================="
    log "步骤 3/6: 创建微服务数据库"
    log "=========================================="
    
    # 创建 PostgreSQL 数据库
    for db_name in "${!PG_DATABASES[@]}"; do
        local port="${PG_DATABASES[$db_name]}"
        log_info "创建 PostgreSQL 数据库: $db_name (端口: $port)"
        
        # 检查数据库是否已存在
        if PGPASSWORD="$PG_PASSWORD" psql -h "$PG_HOST" -p "$port" -U "$PG_USER" \
            -lqt | cut -d \| -f 1 | grep -qw "$db_name"; then
            log_warning "数据库 $db_name 已存在，跳过创建"
        else
            run_command "PGPASSWORD=$PG_PASSWORD createdb -h $PG_HOST -p $port -U $PG_USER $db_name"
        fi
    done
    
    # 创建 MongoDB 数据库（MongoDB 会在首次插入数据时自动创建）
    log_info "MongoDB 数据库 'review_service' 将在首次插入数据时自动创建"
    
    log "✓ 微服务数据库创建完成"
    echo ""
}

# 4. 执行数据库初始化脚本
initialize_databases() {
    log "=========================================="
    log "步骤 4/6: 执行数据库初始化脚本"
    log "=========================================="
    
    # Admin Service
    log_info "初始化 admin-service 数据库..."
    run_command "PGPASSWORD=$PG_PASSWORD psql -h $PG_HOST -p 5432 -U $PG_USER -d admin_service \
        -f ${PROJECT_ROOT}/admin-service/scripts/init_admin_db.sql"
    
    # Cafeteria Service
    log_info "初始化 cafeteria-service 数据库..."
    run_command "PGPASSWORD=$PG_PASSWORD psql -h $PG_HOST -p 5433 -U $PG_USER -d cafeteria_service \
        -f ${PROJECT_ROOT}/cafeteria-service/scripts/init_cafeteria_db.sql"
    
    # Media Service
    log_info "初始化 media-service 数据库..."
    run_command "PGPASSWORD=$PG_PASSWORD psql -h $PG_HOST -p 5434 -U $PG_USER -d media_service \
        -f ${PROJECT_ROOT}/media-service/scripts/init_media_db.sql"
    
    # Preference Service
    log_info "初始化 preference-service 数据库..."
    run_command "PGPASSWORD=$PG_PASSWORD psql -h $PG_HOST -p 5435 -U $PG_USER -d preference_service \
        -f ${PROJECT_ROOT}/preference-service/scripts/init_preference_db.sql"
    
    log "✓ 数据库初始化完成"
    echo ""
}

# 5. 迁移数据
migrate_data() {
    log "=========================================="
    log "步骤 5/6: 迁移数据到微服务"
    log "=========================================="
    
    confirm "即将开始数据迁移，这可能需要几分钟"
    
    # 5.1 迁移 Admin Service 数据（users 表）
    log_info "迁移用户数据到 admin-service..."
    run_command "mysql -h $MYSQL_HOST -P $MYSQL_PORT -u $MYSQL_USER -p$MYSQL_PASSWORD \
        -N -B -e 'SELECT id, username, password, email, role, enabled, avatar_url, 
        created_at, updated_at, last_login FROM users' $MYSQL_DATABASE | \
        while IFS=\$'\\t' read -r id username password email role enabled avatar_url created_at updated_at last_login; do
            PGPASSWORD=$PG_PASSWORD psql -h $PG_HOST -p 5432 -U $PG_USER -d admin_service -c \
            \"INSERT INTO users (id, username, password, email, role, enabled, avatar_url, created_at, updated_at, last_login)
             VALUES (\$id, '\$username', '\$password', '\$email', '\$role', \$enabled, '\$avatar_url', '\$created_at', '\$updated_at', '\$last_login')
             ON CONFLICT (id) DO NOTHING;\"
        done"
    
    # 5.2 迁移 Preference Service 数据
    log_info "迁移收藏数据到 preference-service..."
    run_command "mysql -h $MYSQL_HOST -P $MYSQL_PORT -u $MYSQL_USER -p$MYSQL_PASSWORD \
        -N -B -e 'SELECT id, user_id, stall_id, created_at, sort_order FROM favorites' $MYSQL_DATABASE | \
        while IFS=\$'\\t' read -r id user_id stall_id created_at sort_order; do
            PGPASSWORD=$PG_PASSWORD psql -h $PG_HOST -p 5435 -U $PG_USER -d preference_service -c \
            \"INSERT INTO favorites (id, user_id, stall_id, created_at, sort_order)
             VALUES (\$id, \$user_id, \$stall_id, '\$created_at', \$sort_order)
             ON CONFLICT (id) DO NOTHING;\"
        done"
    
    log_info "迁移搜索历史到 preference-service..."
    run_command "mysql -h $MYSQL_HOST -P $MYSQL_PORT -u $MYSQL_USER -p$MYSQL_PASSWORD \
        -N -B -e 'SELECT id, user_id, keyword, search_time, search_type, result_count, ip_address FROM search_history' $MYSQL_DATABASE | \
        while IFS=\$'\\t' read -r id user_id keyword search_time search_type result_count ip_address; do
            PGPASSWORD=$PG_PASSWORD psql -h $PG_HOST -p 5435 -U $PG_USER -d preference_service -c \
            \"INSERT INTO search_history (id, user_id, keyword, search_time, search_type, result_count, ip_address)
             VALUES (\$id, \$user_id, '\$keyword', '\$search_time', '\$search_type', \$result_count, '\$ip_address')
             ON CONFLICT (id) DO NOTHING;\"
        done"
    
    # 5.3 迁移 Review Service 数据（使用 Python 脚本）
    log_info "迁移评价数据到 review-service (MongoDB)..."
    if [ -f "${PROJECT_ROOT}/review-service/scripts/migrate_reviews_to_mongodb.py" ]; then
        run_command "cd ${PROJECT_ROOT}/review-service/scripts && python3 migrate_reviews_to_mongodb.py"
    else
        log_warning "Review Service 迁移脚本不存在，跳过"
    fi
    
    # 注意: Cafeteria Service 的数据已经在初始化脚本中导入（init_cafeteria_db.sql）
    log_info "Cafeteria Service 数据已通过初始化脚本导入"
    
    log "✓ 数据迁移完成"
    echo ""
}

# 6. 验证数据完整性
validate_data() {
    log "=========================================="
    log "步骤 6/6: 验证数据完整性"
    log "=========================================="
    
    # 验证 Admin Service
    log_info "验证 admin-service..."
    local admin_users=$(PGPASSWORD=$PG_PASSWORD psql -h $PG_HOST -p 5432 -U $PG_USER -d admin_service \
        -t -c "SELECT COUNT(*) FROM users;" | tr -d ' ')
    log "  用户数量: $admin_users"
    
    # 验证 Cafeteria Service
    log_info "验证 cafeteria-service..."
    local cafeterias=$(PGPASSWORD=$PG_PASSWORD psql -h $PG_HOST -p 5433 -U $PG_USER -d cafeteria_service \
        -t -c "SELECT COUNT(*) FROM cafeteria;" | tr -d ' ')
    local stalls=$(PGPASSWORD=$PG_PASSWORD psql -h $PG_HOST -p 5433 -U $PG_USER -d cafeteria_service \
        -t -c "SELECT COUNT(*) FROM stall;" | tr -d ' ')
    log "  食堂数量: $cafeterias"
    log "  档口数量: $stalls"
    
    # 验证 Preference Service
    log_info "验证 preference-service..."
    local favorites=$(PGPASSWORD=$PG_PASSWORD psql -h $PG_HOST -p 5435 -U $PG_USER -d preference_service \
        -t -c "SELECT COUNT(*) FROM favorites;" | tr -d ' ')
    local search_history=$(PGPASSWORD=$PG_PASSWORD psql -h $PG_HOST -p 5435 -U $PG_USER -d preference_service \
        -t -c "SELECT COUNT(*) FROM search_history;" | tr -d ' ')
    log "  收藏数量: $favorites"
    log "  搜索历史: $search_history"
    
    # 验证 Review Service (MongoDB)
    log_info "验证 review-service..."
    local reviews=$(mongo --host $MONGO_HOST --port $MONGO_PORT --quiet --eval \
        "db.getSiblingDB('review_service').reviews.count()" 2>/dev/null || echo "0")
    local review_likes=$(mongo --host $MONGO_HOST --port $MONGO_PORT --quiet --eval \
        "db.getSiblingDB('review_service').review_likes.count()" 2>/dev/null || echo "0")
    log "  评价数量: $reviews"
    log "  点赞数量: $review_likes"
    
    # 对比原始数据
    log_info "对比原始数据..."
    local mysql_users=$(mysql -h $MYSQL_HOST -P $MYSQL_PORT -u $MYSQL_USER -p$MYSQL_PASSWORD \
        -N -B -e "SELECT COUNT(*) FROM users" $MYSQL_DATABASE 2>/dev/null)
    local mysql_favorites=$(mysql -h $MYSQL_HOST -P $MYSQL_PORT -u $MYSQL_USER -p$MYSQL_PASSWORD \
        -N -B -e "SELECT COUNT(*) FROM favorites" $MYSQL_DATABASE 2>/dev/null)
    
    log "  MySQL 用户: $mysql_users → PostgreSQL 用户: $admin_users"
    log "  MySQL 收藏: $mysql_favorites → PostgreSQL 收藏: $favorites"
    
    # 判断是否成功
    if [ "$admin_users" -gt 0 ] && [ "$cafeterias" -gt 0 ] && [ "$stalls" -gt 0 ]; then
        log "✓ 数据验证通过"
    else
        log_error "数据验证失败，请检查日志"
        exit 1
    fi
    
    echo ""
}

# 7. 完成总结
show_summary() {
    log "=========================================="
    log "迁移完成！"
    log "=========================================="
    
    log_info "备份文件位置: $BACKUP_DIR"
    log_info "日志文件位置: $LOG_FILE"
    
    cat << EOF

${GREEN}✓ 数据迁移成功完成！${NC}

下一步操作:
  1. 启动微服务:
     ${BLUE}cd ${PROJECT_ROOT}${NC}
     ${BLUE}docker-compose up -d${NC}

  2. 验证服务健康:
     ${BLUE}docker-compose ps${NC}

  3. 测试 API 端点:
     - Admin Service:      http://localhost:8082/health
     - Cafeteria Service:  http://localhost:8083/health
     - Review Service:     http://localhost:8084/health
     - Media Service:      http://localhost:8085/health
     - Preference Service: http://localhost:8086/health

  4. 查看详细文档:
     ${BLUE}cat docs/DATABASE_ARCHITECTURE.md${NC}

${YELLOW}⚠️  重要提示:${NC}
  - 备份文件已保存，如需回滚请使用备份恢复
  - 建议在生产环境部署前进行充分测试
  - 监控 RabbitMQ 队列确保事件正常消费

EOF
}

# ==================== 主流程 ====================

main() {
    # 解析命令行参数
    while [[ $# -gt 0 ]]; do
        case $1 in
            --dry-run)
                DRY_RUN=true
                shift
                ;;
            --skip-backup)
                SKIP_BACKUP=true
                shift
                ;;
            --help)
                show_help
                ;;
            *)
                log_error "未知选项: $1"
                echo "使用 --help 查看帮助信息"
                exit 1
                ;;
        esac
    done
    
    # 显示配置信息
    cat << EOF
${BLUE}=========================================${NC}
${BLUE}  NUS Hungry 数据迁移脚本${NC}
${BLUE}=========================================${NC}

配置信息:
  MySQL:      ${MYSQL_HOST}:${MYSQL_PORT}
  PostgreSQL: ${PG_HOST}:5432-5435
  MongoDB:    ${MONGO_HOST}:${MONGO_PORT}
  项目根目录: ${PROJECT_ROOT}
  备份目录:   ${BACKUP_DIR}
  日志文件:   ${LOG_FILE}
  
模式:
  Dry-Run:    ${DRY_RUN}
  跳过备份:   ${SKIP_BACKUP}

EOF
    
    if [ "$DRY_RUN" = true ]; then
        log_warning "运行在 DRY-RUN 模式，不会执行实际操作"
    fi
    
    sleep 2
    
    # 执行迁移步骤
    check_prerequisites
    backup_monolith_database
    create_microservice_databases
    initialize_databases
    migrate_data
    validate_data
    show_summary
}

# 捕获 Ctrl+C
trap 'log_error "用户中断操作"; exit 130' INT

# 执行主流程
main "$@"
