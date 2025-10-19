@echo off
REM Media Service - 启动脚本 (Windows)

echo.
echo ============================================
echo   Media Service - 启动服务
echo ============================================
echo.

where docker >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Docker 未安装
    pause
    exit /b 1
)

echo [INFO] 创建必要的目录...
if not exist "logs" mkdir logs
if not exist "uploads" mkdir uploads

echo [INFO] 启动服务...
docker-compose up -d

echo [INFO] 等待服务启动（约 60 秒）...
timeout /t 60 /nobreak >nul

echo.
echo ============================================
echo   服务已成功启动！
echo ============================================
echo.
echo 📋 服务访问信息:
echo   🔹 Media Service: http://localhost:8085
echo   🔹 Swagger UI: http://localhost:8085/swagger-ui.html
echo   🔹 PostgreSQL: localhost:5434
echo   🔹 MinIO Console: http://localhost:9001
echo.
echo 📝 测试文件上传:
echo   curl -X POST http://localhost:8085/api/media/upload -F "file=@test.jpg"
echo.
pause
