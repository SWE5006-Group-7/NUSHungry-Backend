@echo off
chcp 65001 >nul
setlocal EnableDelayedExpansion

REM ################################################################################
REM NUS Hungry - 单体架构到微服务架构数据迁移脚本 (Windows)
REM 
REM 功能:
REM   1. 备份单体数据库 (MySQL)
REM   2. 创建微服务数据库 (PostgreSQL × 4, MongoDB × 1)
REM   3. 执行数据库初始化脚本
REM   4. 迁移数据到各个微服务
REM   5. 验证数据完整性
REM
REM 使用方法:
REM   scripts\migrate_monolith_to_microservices.bat [dry-run] [skip-backup]
REM
REM 选项:
REM   dry-run       仅显示要执行的命令，不实际执行
REM   skip-backup   跳过数据库备份步骤（仅用于测试）
REM   help          显示帮助信息
REM
REM 前置条件:
REM   - MySQL 服务运行中（单体数据库）
REM   - PostgreSQL 服务运行中（端口 5432-5435）
REM   - MongoDB 服务运行中（端口 27017）
REM   - Python 3 已安装（用于 Review Service 迁移）
REM   - pymysql, pymongo Python 包已安装
REM
REM 作者: NUS Hungry Team
REM 日期: 2025-01-19
REM ################################################################################

REM ==================== 配置常量 ====================

REM 项目根目录
set "PROJECT_ROOT=%~dp0.."
cd /d "%PROJECT_ROOT%"

REM 备份和日志目录
set "BACKUP_DIR=%PROJECT_ROOT%\backups"
set "LOG_DIR=%PROJECT_ROOT%\logs"

REM 生成时间戳
for /f "tokens=2 delims==" %%I in ('wmic os get localdatetime /value') do set datetime=%%I
set "TIMESTAMP=%datetime:~0,4%%datetime:~4,2%%datetime:~6,2%_%datetime:~8,2%%datetime:~10,2%%datetime:~12,2%"
set "LOG_FILE=%LOG_DIR%\migration_%TIMESTAMP%.log"

REM 单体数据库配置 (MySQL)
if not defined MYSQL_HOST set "MYSQL_HOST=localhost"
if not defined MYSQL_PORT set "MYSQL_PORT=3306"
if not defined MYSQL_USER set "MYSQL_USER=root"
if not defined MYSQL_PASSWORD set "MYSQL_PASSWORD=123456"
if not defined MYSQL_DATABASE set "MYSQL_DATABASE=nushungry_db"

REM PostgreSQL 配置
if not defined PG_HOST set "PG_HOST=localhost"
if not defined PG_USER set "PG_USER=postgres"
if not defined PG_PASSWORD set "PG_PASSWORD=postgres"
set "PGPASSWORD=%PG_PASSWORD%"

REM MongoDB 配置
if not defined MONGO_HOST set "MONGO_HOST=localhost"
if not defined MONGO_PORT set "MONGO_PORT=27017"

REM 命令行参数
set "DRY_RUN=false"
set "SKIP_BACKUP=false"

REM 解析命令行参数
:parse_args
if "%~1"=="" goto :end_parse_args
if /i "%~1"=="dry-run" (
    set "DRY_RUN=true"
    shift
    goto :parse_args
)
if /i "%~1"=="skip-backup" (
    set "SKIP_BACKUP=true"
    shift
    goto :parse_args
)
if /i "%~1"=="help" (
    call :show_help
    exit /b 0
)
echo [错误] 未知选项: %~1
echo 使用 'help' 参数查看帮助信息
exit /b 1
:end_parse_args

REM ==================== 辅助函数 ====================

:log
echo [%date% %time:~0,8%] %~1
echo [%date% %time:~0,8%] %~1 >> "%LOG_FILE%"
goto :eof

:log_error
echo [错误] %~1
echo [错误] %~1 >> "%LOG_FILE%"
goto :eof

:log_warning
echo [警告] %~1
echo [警告] %~1 >> "%LOG_FILE%"
goto :eof

:log_info
echo [信息] %~1
echo [信息] %~1 >> "%LOG_FILE%"
goto :eof

:log_success
echo [成功] %~1
echo [成功] %~1 >> "%LOG_FILE%"
goto :eof

:check_command
where %~1 >nul 2>&1
if errorlevel 1 (
    call :log_error "命令 '%~1' 未找到，请先安装"
    exit /b 1
)
goto :eof

:check_port
powershell -Command "try { $client = New-Object System.Net.Sockets.TcpClient('%~1', %~2); $client.Close(); exit 0 } catch { exit 1 }" >nul 2>&1
if errorlevel 1 (
    call :log_error "%~3 未运行 (%~1:%~2)"
    exit /b 1
) else (
    call :log "✓ %~3 运行中 (%~1:%~2)"
)
goto :eof

:confirm
if "%DRY_RUN%"=="true" goto :eof
echo.
echo ⚠️  %~1
set /p "REPLY=是否继续? [y/N]: "
if /i not "!REPLY!"=="y" (
    call :log_warning "用户取消操作"
    exit /b 1
)
goto :eof

:show_help
echo.
echo NUS Hungry - 数据迁移脚本 (Windows)
echo.
echo 使用方法:
echo   scripts\migrate_monolith_to_microservices.bat [选项]
echo.
echo 选项:
echo   dry-run       仅显示要执行的命令，不实际执行
echo   skip-backup   跳过数据库备份步骤（仅用于测试）
echo   help          显示此帮助信息
echo.
echo 示例:
echo   REM 正常迁移
echo   scripts\migrate_monolith_to_microservices.bat
echo.
echo   REM 测试模式（不实际执行）
echo   scripts\migrate_monolith_to_microservices.bat dry-run
echo.
echo   REM 跳过备份（用于测试环境）
echo   scripts\migrate_monolith_to_microservices.bat skip-backup
echo.
echo 环境变量:
echo   MYSQL_HOST        MySQL 主机地址（默认: localhost）
echo   MYSQL_PORT        MySQL 端口（默认: 3306）
echo   MYSQL_USER        MySQL 用户名（默认: root）
echo   MYSQL_PASSWORD    MySQL 密码（默认: 123456）
echo   PG_HOST           PostgreSQL 主机地址（默认: localhost）
echo   PG_USER           PostgreSQL 用户名（默认: postgres）
echo   PG_PASSWORD       PostgreSQL 密码（默认: postgres）
echo   MONGO_HOST        MongoDB 主机地址（默认: localhost）
echo   MONGO_PORT        MongoDB 端口（默认: 27017）
echo.
goto :eof

REM ==================== 主要功能函数 ====================

:check_prerequisites
call :log "=========================================="
call :log "步骤 1/6: 前置条件检查"
call :log "=========================================="

REM 检查必要的命令
call :log_info "检查必要的命令..."
call :check_command mysql || exit /b 1
call :check_command psql || exit /b 1
call :check_command mongo || exit /b 1
call :check_command python || exit /b 1
call :check_command mysqldump || exit /b 1

REM 检查 Python 依赖
call :log_info "检查 Python 依赖..."
python -c "import pymysql, pymongo" 2>nul
if errorlevel 1 (
    call :log_warning "Python 依赖未安装，正在安装..."
    pip install pymysql pymongo >> "%LOG_FILE%" 2>&1
)

REM 检查服务状态
call :log_info "检查数据库服务状态..."
call :check_port "%MYSQL_HOST%" "%MYSQL_PORT%" "MySQL" || exit /b 1
call :check_port "%PG_HOST%" "5432" "PostgreSQL" || exit /b 1
call :check_port "%MONGO_HOST%" "%MONGO_PORT%" "MongoDB" || exit /b 1

REM 检查 MySQL 连接
call :log_info "测试 MySQL 连接..."
mysql -h %MYSQL_HOST% -P %MYSQL_PORT% -u %MYSQL_USER% -p%MYSQL_PASSWORD% -e "SELECT 1;" >nul 2>&1
if errorlevel 1 (
    call :log_error "MySQL 连接失败，请检查配置"
    exit /b 1
)
call :log "✓ MySQL 连接成功"

REM 创建目录
if not exist "%BACKUP_DIR%" mkdir "%BACKUP_DIR%"
if not exist "%LOG_DIR%" mkdir "%LOG_DIR%"

call :log_success "前置检查完成"
echo.
goto :eof

:backup_monolith_database
if "%SKIP_BACKUP%"=="true" (
    call :log_warning "跳过数据库备份（skip-backup）"
    goto :eof
)

call :log "=========================================="
call :log "步骤 2/6: 备份单体数据库"
call :log "=========================================="

set "BACKUP_FILE=%BACKUP_DIR%\nushungry_db_backup_%TIMESTAMP%.sql"

call :confirm "即将备份 MySQL 数据库: %MYSQL_DATABASE%"

call :log_info "备份文件: %BACKUP_FILE%"
if "%DRY_RUN%"=="false" (
    mysqldump -h %MYSQL_HOST% -P %MYSQL_PORT% -u %MYSQL_USER% -p%MYSQL_PASSWORD% ^
        --single-transaction --routines --triggers --events ^
        %MYSQL_DATABASE% > "%BACKUP_FILE%" 2>> "%LOG_FILE%"
    
    if exist "%BACKUP_FILE%" (
        for %%A in ("%BACKUP_FILE%") do set "SIZE=%%~zA"
        call :log_success "备份完成: %BACKUP_FILE% (大小: !SIZE! 字节)"
    ) else (
        call :log_error "备份文件为空或不存在"
        exit /b 1
    )
) else (
    call :log_info "[DRY-RUN] 跳过备份"
)

echo.
goto :eof

:create_microservice_databases
call :log "=========================================="
call :log "步骤 3/6: 创建微服务数据库"
call :log "=========================================="

REM 创建 PostgreSQL 数据库
set "databases=admin_service:5432 cafeteria_service:5433 media_service:5434 preference_service:5435"

for %%D in (%databases%) do (
    for /f "tokens=1,2 delims=:" %%A in ("%%D") do (
        call :log_info "创建 PostgreSQL 数据库: %%A (端口: %%B)"
        
        if "%DRY_RUN%"=="false" (
            REM 检查数据库是否已存在
            psql -h %PG_HOST% -p %%B -U %PG_USER% -lqt | findstr /C:"%%A" >nul 2>&1
            if errorlevel 1 (
                createdb -h %PG_HOST% -p %%B -U %PG_USER% %%A >> "%LOG_FILE%" 2>&1
            ) else (
                call :log_warning "数据库 %%A 已存在，跳过创建"
            )
        ) else (
            call :log_info "[DRY-RUN] 跳过创建"
        )
    )
)

call :log_info "MongoDB 数据库 'review_service' 将在首次插入数据时自动创建"

call :log_success "微服务数据库创建完成"
echo.
goto :eof

:initialize_databases
call :log "=========================================="
call :log "步骤 4/6: 执行数据库初始化脚本"
call :log "=========================================="

if "%DRY_RUN%"=="false" (
    REM Admin Service
    call :log_info "初始化 admin-service 数据库..."
    psql -h %PG_HOST% -p 5432 -U %PG_USER% -d admin_service ^
        -f "%PROJECT_ROOT%\admin-service\scripts\init_admin_db.sql" >> "%LOG_FILE%" 2>&1

    REM Cafeteria Service
    call :log_info "初始化 cafeteria-service 数据库..."
    psql -h %PG_HOST% -p 5433 -U %PG_USER% -d cafeteria_service ^
        -f "%PROJECT_ROOT%\cafeteria-service\scripts\init_cafeteria_db.sql" >> "%LOG_FILE%" 2>&1

    REM Media Service
    call :log_info "初始化 media-service 数据库..."
    psql -h %PG_HOST% -p 5434 -U %PG_USER% -d media_service ^
        -f "%PROJECT_ROOT%\media-service\scripts\init_media_db.sql" >> "%LOG_FILE%" 2>&1

    REM Preference Service
    call :log_info "初始化 preference-service 数据库..."
    psql -h %PG_HOST% -p 5435 -U %PG_USER% -d preference_service ^
        -f "%PROJECT_ROOT%\preference-service\scripts\init_preference_db.sql" >> "%LOG_FILE%" 2>&1
) else (
    call :log_info "[DRY-RUN] 跳过初始化"
)

call :log_success "数据库初始化完成"
echo.
goto :eof

:migrate_data
call :log "=========================================="
call :log "步骤 5/6: 迁移数据到微服务"
call :log "=========================================="

call :confirm "即将开始数据迁移，这可能需要几分钟"

if "%DRY_RUN%"=="false" (
    REM 5.1 迁移 Preference Service 数据（使用 Python 辅助脚本更可靠）
    call :log_info "迁移收藏和搜索历史到 preference-service..."
    
    REM 创建临时 Python 脚本进行迁移
    echo import pymysql > "%TEMP%\migrate_preference.py"
    echo import psycopg2 >> "%TEMP%\migrate_preference.py"
    echo. >> "%TEMP%\migrate_preference.py"
    echo mysql_conn = pymysql.connect(host='%MYSQL_HOST%', port=%MYSQL_PORT%, user='%MYSQL_USER%', password='%MYSQL_PASSWORD%', database='%MYSQL_DATABASE%') >> "%TEMP%\migrate_preference.py"
    echo pg_conn = psycopg2.connect(host='%PG_HOST%', port=5435, user='%PG_USER%', password='%PG_PASSWORD%', database='preference_service') >> "%TEMP%\migrate_preference.py"
    echo. >> "%TEMP%\migrate_preference.py"
    echo # 迁移 favorites >> "%TEMP%\migrate_preference.py"
    echo mysql_cur = mysql_conn.cursor() >> "%TEMP%\migrate_preference.py"
    echo mysql_cur.execute("SELECT id, user_id, stall_id, created_at, sort_order FROM favorites") >> "%TEMP%\migrate_preference.py"
    echo pg_cur = pg_conn.cursor() >> "%TEMP%\migrate_preference.py"
    echo for row in mysql_cur: >> "%TEMP%\migrate_preference.py"
    echo     pg_cur.execute("INSERT INTO favorites (id, user_id, stall_id, created_at, sort_order) VALUES (%%s, %%s, %%s, %%s, %%s) ON CONFLICT (id) DO NOTHING", row) >> "%TEMP%\migrate_preference.py"
    echo pg_conn.commit() >> "%TEMP%\migrate_preference.py"
    echo print(f"Migrated {mysql_cur.rowcount} favorites") >> "%TEMP%\migrate_preference.py"
    echo. >> "%TEMP%\migrate_preference.py"
    echo # 迁移 search_history >> "%TEMP%\migrate_preference.py"
    echo mysql_cur.execute("SELECT id, user_id, keyword, search_time, search_type, result_count, ip_address FROM search_history") >> "%TEMP%\migrate_preference.py"
    echo for row in mysql_cur: >> "%TEMP%\migrate_preference.py"
    echo     pg_cur.execute("INSERT INTO search_history (id, user_id, keyword, search_time, search_type, result_count, ip_address) VALUES (%%s, %%s, %%s, %%s, %%s, %%s, %%s) ON CONFLICT (id) DO NOTHING", row) >> "%TEMP%\migrate_preference.py"
    echo pg_conn.commit() >> "%TEMP%\migrate_preference.py"
    echo print(f"Migrated {mysql_cur.rowcount} search_history") >> "%TEMP%\migrate_preference.py"
    echo. >> "%TEMP%\migrate_preference.py"
    echo mysql_conn.close() >> "%TEMP%\migrate_preference.py"
    echo pg_conn.close() >> "%TEMP%\migrate_preference.py"
    
    python "%TEMP%\migrate_preference.py" >> "%LOG_FILE%" 2>&1
    
    REM 5.2 迁移 Review Service 数据（使用 Python 脚本）
    call :log_info "迁移评价数据到 review-service (MongoDB)..."
    if exist "%PROJECT_ROOT%\review-service\scripts\migrate_reviews_to_mongodb.py" (
        cd /d "%PROJECT_ROOT%\review-service\scripts"
        python migrate_reviews_to_mongodb.py >> "%LOG_FILE%" 2>&1
        cd /d "%PROJECT_ROOT%"
    ) else (
        call :log_warning "Review Service 迁移脚本不存在，跳过"
    )
    
    call :log_info "Cafeteria Service 数据已通过初始化脚本导入"
) else (
    call :log_info "[DRY-RUN] 跳过数据迁移"
)

call :log_success "数据迁移完成"
echo.
goto :eof

:validate_data
call :log "=========================================="
call :log "步骤 6/6: 验证数据完整性"
call :log "=========================================="

if "%DRY_RUN%"=="false" (
    REM 验证 Cafeteria Service
    call :log_info "验证 cafeteria-service..."
    for /f %%A in ('psql -h %PG_HOST% -p 5433 -U %PG_USER% -d cafeteria_service -t -c "SELECT COUNT(*) FROM cafeteria;"') do set "cafeterias=%%A"
    for /f %%A in ('psql -h %PG_HOST% -p 5433 -U %PG_USER% -d cafeteria_service -t -c "SELECT COUNT(*) FROM stall;"') do set "stalls=%%A"
    call :log "  食堂数量: !cafeterias!"
    call :log "  档口数量: !stalls!"
    
    REM 验证 Preference Service
    call :log_info "验证 preference-service..."
    for /f %%A in ('psql -h %PG_HOST% -p 5435 -U %PG_USER% -d preference_service -t -c "SELECT COUNT(*) FROM favorites;"') do set "favorites=%%A"
    for /f %%A in ('psql -h %PG_HOST% -p 5435 -U %PG_USER% -d preference_service -t -c "SELECT COUNT(*) FROM search_history;"') do set "search_history=%%A"
    call :log "  收藏数量: !favorites!"
    call :log "  搜索历史: !search_history!"
    
    REM 验证 Review Service (MongoDB)
    call :log_info "验证 review-service..."
    for /f %%A in ('mongo --host %MONGO_HOST% --port %MONGO_PORT% --quiet --eval "db.getSiblingDB('review_service').reviews.count()" 2^>nul') do set "reviews=%%A"
    if "!reviews!"=="" set "reviews=0"
    call :log "  评价数量: !reviews!"
    
    REM 判断是否成功
    if !cafeterias! GTR 0 if !stalls! GTR 0 (
        call :log_success "数据验证通过"
    ) else (
        call :log_error "数据验证失败，请检查日志"
        exit /b 1
    )
) else (
    call :log_info "[DRY-RUN] 跳过验证"
)

echo.
goto :eof

:show_summary
call :log "=========================================="
call :log "迁移完成！"
call :log "=========================================="

call :log_info "备份文件位置: %BACKUP_DIR%"
call :log_info "日志文件位置: %LOG_FILE%"

echo.
echo ✓ 数据迁移成功完成！
echo.
echo 下一步操作:
echo   1. 启动微服务:
echo      cd %PROJECT_ROOT%
echo      docker-compose up -d
echo.
echo   2. 验证服务健康:
echo      docker-compose ps
echo.
echo   3. 测试 API 端点:
echo      - Admin Service:      http://localhost:8082/health
echo      - Cafeteria Service:  http://localhost:8083/health
echo      - Review Service:     http://localhost:8084/health
echo      - Media Service:      http://localhost:8085/health
echo      - Preference Service: http://localhost:8086/health
echo.
echo   4. 查看详细文档:
echo      type docs\DATABASE_ARCHITECTURE.md
echo.
echo ⚠️  重要提示:
echo   - 备份文件已保存，如需回滚请使用备份恢复
echo   - 建议在生产环境部署前进行充分测试
echo   - 监控 RabbitMQ 队列确保事件正常消费
echo.

goto :eof

REM ==================== 主流程 ====================

:main
REM 显示配置信息
echo =========================================
echo   NUS Hungry 数据迁移脚本 (Windows)
echo =========================================
echo.
echo 配置信息:
echo   MySQL:      %MYSQL_HOST%:%MYSQL_PORT%
echo   PostgreSQL: %PG_HOST%:5432-5435
echo   MongoDB:    %MONGO_HOST%:%MONGO_PORT%
echo   项目根目录: %PROJECT_ROOT%
echo   备份目录:   %BACKUP_DIR%
echo   日志文件:   %LOG_FILE%
echo.
echo 模式:
echo   Dry-Run:    %DRY_RUN%
echo   跳过备份:   %SKIP_BACKUP%
echo.

if "%DRY_RUN%"=="true" (
    call :log_warning "运行在 DRY-RUN 模式，不会执行实际操作"
)

timeout /t 2 /nobreak >nul

REM 执行迁移步骤
call :check_prerequisites
if errorlevel 1 exit /b 1

call :backup_monolith_database
if errorlevel 1 exit /b 1

call :create_microservice_databases
if errorlevel 1 exit /b 1

call :initialize_databases
if errorlevel 1 exit /b 1

call :migrate_data
if errorlevel 1 exit /b 1

call :validate_data
if errorlevel 1 exit /b 1

call :show_summary

exit /b 0

REM 执行主流程
call :main

endlocal
