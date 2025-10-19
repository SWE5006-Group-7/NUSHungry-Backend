@echo off
REM ===========================================================================
REM Admin Service - 停止脚本 (Windows)
REM 
REM 用途: 停止 Admin Service 及其依赖服务
REM 使用: scripts\stop-services.bat
REM ===========================================================================

setlocal enabledelayedexpansion

echo.
echo ============================================
echo   Admin Service - 停止服务
echo ============================================
echo.

echo 请选择停止方式:
echo   1) 停止容器（保留数据）
echo   2) 停止并删除容器（保留数据卷）
echo   3) 停止并删除所有（包括数据卷）⚠️
echo.

set /p choice="请输入选项 (1-3): "

if "%choice%"=="1" (
    echo [INFO] 停止容器...
    docker-compose stop
    echo [SUCCESS] 容器已停止
) else if "%choice%"=="2" (
    echo [INFO] 停止并删除容器...
    docker-compose down
    echo [SUCCESS] 容器已删除
) else if "%choice%"=="3" (
    echo [WARNING] 将删除所有数据，包括数据库数据！
    set /p confirm="确认删除所有数据? (yes/no): "
    if "!confirm!"=="yes" (
        echo [INFO] 停止并删除所有...
        docker-compose down -v
        echo [SUCCESS] 所有数据已删除
    ) else (
        echo [INFO] 已取消删除操作
    )
) else (
    echo [WARNING] 无效选项，仅停止容器
    docker-compose stop
)

echo.
echo [INFO] 当前状态:
docker-compose ps
echo.

echo [SUCCESS] 操作完成
pause
