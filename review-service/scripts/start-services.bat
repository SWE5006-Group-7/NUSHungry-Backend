@echo off
REM ============================================================================
REM Review Service 启动脚本 (Windows)
REM ============================================================================

echo ============================================================
echo           Review Service 启动脚本
echo ============================================================
echo.

REM 切换到 review-service 根目录
cd /d "%~dp0\.."

REM 检查 Docker 是否运行
docker info >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Docker 未运行，请启动 Docker Desktop
    pause
    exit /b 1
)
echo [INFO] Docker 运行正常

REM 检查 docker-compose 是否安装
docker-compose --version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] docker-compose 未安装
    pause
    exit /b 1
)
echo [INFO] docker-compose 已安装

REM 启动服务
echo [INFO] 正在启动服务...
docker-compose up -d --build

if errorlevel 1 (
    echo [ERROR] 服务启动失败
    pause
    exit /b 1
)

echo.
echo [INFO] 等待服务启动...
timeout /t 10 /nobreak >nul

REM 显示服务状态
echo.
echo [INFO] 服务状态:
docker-compose ps

REM 显示访问地址
echo.
echo ============================================================
echo [INFO] 服务访问地址:
echo   - Review Service API:      http://localhost:8084
echo   - Review Service Swagger:  http://localhost:8084/swagger-ui.html
echo   - Review Service Health:   http://localhost:8084/actuator/health
echo   - RabbitMQ Management:     http://localhost:15672 (admin/password123)
echo   - MongoDB:                 mongodb://localhost:27017
echo.
echo [INFO] 查看日志命令:
echo   - 所有服务:       docker-compose logs -f
echo   - Review Service: docker-compose logs -f review-service
echo   - MongoDB:        docker-compose logs -f mongodb
echo   - RabbitMQ:       docker-compose logs -f rabbitmq
echo ============================================================
echo.
echo [INFO] 所有服务已启动完成
echo.
pause
