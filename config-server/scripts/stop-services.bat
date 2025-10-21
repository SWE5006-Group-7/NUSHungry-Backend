@echo off
REM Stop Config Server and dependencies

echo ===================================================================
echo   Stopping NUSHungry Config Server
echo ===================================================================
echo.

REM Navigate to config-server directory
cd /d "%~dp0\.."

echo Stopping services...
echo.

REM Stop services
docker-compose down

echo.
echo ===================================================================
echo   All services stopped successfully!
echo ===================================================================
echo.
echo To remove volumes as well:
echo   docker-compose down -v
echo.
echo To start services again:
echo   scripts\start-services.bat
echo.
