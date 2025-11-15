@echo off
REM Media Service - 停止脚本 (Windows)

echo.
echo ============================================
echo   Media Service - 停止服务
echo ============================================
echo.

echo 请选择停止方式:
echo   1) 停止容器（保留数据）
echo   2) 停止并删除容器（保留数据卷）
echo   3) 停止并删除所有（包括数据卷）⚠️
echo.

set /p choice="请输入选项 (1-3): "

if "%choice%"=="1" (
    docker-compose stop
) else if "%choice%"=="2" (
    docker-compose down
) else if "%choice%"=="3" (
    set /p confirm="确认删除所有数据? (yes/no): "
    if "!confirm!"=="yes" docker-compose down -v
) else (
    docker-compose stop
)

echo [SUCCESS] 操作完成
pause
