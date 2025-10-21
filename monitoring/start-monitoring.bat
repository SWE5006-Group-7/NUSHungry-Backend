@echo off
REM ============================================
REM Start Monitoring Stack (Prometheus + Grafana)
REM Windows Batch Script
REM ============================================

echo ==========================================
echo Starting NUSHungry Monitoring Stack
echo ==========================================
echo.

REM Check if Docker is running
docker info >nul 2>&1
if errorlevel 1 (
    echo X Error: Docker is not running.
    echo Please start Docker Desktop and try again.
    exit /b 1
)

REM Check if nushungry network exists
docker network inspect nushungry >nul 2>&1
if errorlevel 1 (
    echo Warning: 'nushungry' network does not exist.
    echo Creating 'nushungry' network...
    docker network create nushungry
    echo Network created successfully.
)

REM Start monitoring stack
echo Starting Prometheus and Grafana...
echo.
docker-compose up -d

REM Wait for services
echo.
echo Waiting for services to be healthy...
timeout /t 10 /nobreak >nul

echo.
echo ==========================================
echo Monitoring Stack Started Successfully
echo ==========================================
echo.
echo Access URLs:
echo    - Prometheus: http://localhost:9090
echo    - Grafana:    http://localhost:3000
echo.
echo Grafana Credentials:
echo    - Username: admin
echo    - Password: admin
echo    (You will be prompted to change password on first login)
echo.
echo View metrics:
echo    1. Open Grafana (http://localhost:3000)
echo    2. Login with credentials above
echo    3. Go to Dashboards - NUSHungry Microservices Overview
echo.
echo Tip: Make sure your microservices are running to see metrics.
echo.
pause
