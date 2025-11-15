@echo off
REM ============================================================================
REM Review Service 停止脚本 (Windows)
REM ============================================================================

echo ============================================================
echo           Review Service 停止脚本
echo ============================================================
echo.

REM 切换到 review-service 根目录
cd /d "%~dp0\.."

REM 检查参数
if "%1"=="--clean" goto clean
if "%1"=="-c" goto clean

REM 正常停止
echo [INFO] 正在停止服务...
docker-compose down

echo.
echo [INFO] 服务已停止
echo.
echo [INFO] 提示:
echo   - 重新启动:      scripts\start-services.bat
echo   - 删除数据卷:    scripts\stop-services.bat --clean
echo   - 查看容器:      docker ps -a
echo   - 查看数据卷:    docker volume ls
echo.
pause
exit /b 0

:clean
echo [WARN] 将删除所有数据卷（数据库数据将丢失）
set /p confirm="确认删除？(y/N): "
if /i not "%confirm%"=="y" (
    echo [INFO] 已取消
    pause
    exit /b 0
)

echo [INFO] 正在停止服务并删除数据卷...
docker-compose down -v

echo.
echo [INFO] 服务和数据卷已删除
echo.
pause
