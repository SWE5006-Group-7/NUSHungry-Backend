@echo off
REM ============================================
REM Stop Monitoring Stack (Prometheus + Grafana)
REM Windows Batch Script
REM ============================================

echo ==========================================
echo Stopping NUSHungry Monitoring Stack
echo ==========================================
echo.

REM Stop services
echo Stopping Prometheus and Grafana...
docker-compose down

echo.
echo ==========================================
echo Monitoring Stack Stopped Successfully
echo ==========================================
echo.
echo To remove all monitoring data, run:
echo    docker-compose down -v
echo.
pause
