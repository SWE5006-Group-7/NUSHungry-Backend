@echo off
chcp 65001 >nul
echo ==========================================
echo 停止 NUSHungry 微服务架构
echo ==========================================

REM 停止所有服务
echo.
echo 🛑 停止所有服务...
docker-compose down

echo.
echo ==========================================
echo ✅ 所有服务已停止！
echo ==========================================
echo.
echo 如需清理数据卷，请运行：
echo   docker-compose down -v
echo.
echo 如需重新启动，请运行：
echo   scripts\start-all-services.bat
echo ==========================================
pause
