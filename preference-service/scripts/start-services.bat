@echo off
echo ============================================
echo   Preference Service - 启动服务
echo ============================================
echo.
if not exist "logs" mkdir logs
docker-compose up -d
timeout /t 30 /nobreak >nul
echo.
echo 服务已启动！
echo   API: http://localhost:8086
echo   Swagger: http://localhost:8086/swagger-ui.html
echo   PostgreSQL: localhost:5435
pause
