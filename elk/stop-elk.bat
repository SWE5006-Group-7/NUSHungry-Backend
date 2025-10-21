@echo off
REM ELK Stack停止脚本 (Windows版本)

echo =========================================
echo  NUSHungry ELK Stack 停止脚本
echo =========================================
echo.

REM 停止ELK Stack
echo 🛑 停止ELK Stack...
docker-compose down

echo.
echo ✅ ELK Stack已停止
echo.
echo 💡 提示:
echo    • 重新启动: start-elk.bat
echo    • 删除所有数据卷: docker-compose down -v
echo    • 查看容器状态: docker-compose ps
echo.

pause
