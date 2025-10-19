@echo off
REM ===========================================================================
REM Admin Service - 启动脚本 (Windows)
REM 
REM 用途: 启动 Admin Service 及其依赖服务
REM 使用: scripts\start-services.bat
REM ===========================================================================

setlocal enabledelayedexpansion

echo.
echo ============================================
echo   Admin Service - 启动服务
echo ============================================
echo.

REM 检查 Docker 是否安装
where docker >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Docker 未安装，请先安装 Docker Desktop
    pause
    exit /b 1
)

where docker-compose >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Docker Compose 未安装
    pause
    exit /b 1
)

echo [INFO] Docker 环境检查通过
echo.

REM 创建必要的目录
echo [INFO] 创建必要的目录...
if not exist "logs" mkdir logs
echo [SUCCESS] 目录创建完成
echo.

REM 启动服务
echo [INFO] 启动服务...
docker-compose up -d

if %errorlevel% neq 0 (
    echo [ERROR] 服务启动失败
    pause
    exit /b 1
)

echo [SUCCESS] 服务启动命令已执行
echo.

REM 等待服务启动
echo [INFO] 等待服务启动（约 60 秒）...
timeout /t 60 /nobreak >nul

REM 检查服务状态
echo [INFO] 检查服务状态...
docker-compose ps
echo.

REM 显示访问信息
echo ============================================
echo   服务已成功启动！
echo ============================================
echo.
echo 📋 服务访问信息:
echo.
echo   🔹 Admin Service API:
echo      http://localhost:8082
echo.
echo   🔹 Swagger UI (API 文档):
echo      http://localhost:8082/swagger-ui.html
echo.
echo   🔹 Health Check:
echo      http://localhost:8082/actuator/health
echo.
echo   🔹 PostgreSQL:
echo      Host: localhost:5432
echo      Database: admin_service
echo      Username: admin
echo      Password: password123
echo.
echo   🔹 RabbitMQ 管理界面:
echo      http://localhost:15672
echo      Username: admin
echo      Password: password123
echo.
echo ============================================
echo.
echo 📌 默认管理员账号:
echo    Username: admin
echo    Password: Admin123!
echo.
echo ============================================
echo.
echo 📝 常用命令:
echo   - 查看日志:   docker-compose logs -f
echo   - 停止服务:   scripts\stop-services.bat
echo   - 重启服务:   docker-compose restart
echo.

pause
