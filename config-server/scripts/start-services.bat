@echo off
REM Start Config Server and dependencies

echo ===================================================================
echo   Starting NUSHungry Config Server
echo ===================================================================
echo.

REM Check if Docker is running
docker info >nul 2>&1
if errorlevel 1 (
    echo Error: Docker is not running
    echo Please start Docker and try again.
    exit /b 1
)

REM Check if docker-compose is installed
docker-compose --version >nul 2>&1
if errorlevel 1 (
    echo Error: docker-compose is not installed
    echo Please install docker-compose and try again.
    exit /b 1
)

REM Navigate to config-server directory
cd /d "%~dp0\.."

echo Checking configuration...

REM Check if config repository exists
if not exist "config-repo" (
    echo Config repository not found. Creating sample repository...
    mkdir config-repo

    REM Create sample application.yml
    (
        echo # Common configuration for all services
        echo spring:
        echo   jpa:
        echo     hibernate:
        echo       ddl-auto: validate
        echo     show-sql: false
        echo.
        echo management:
        echo   endpoints:
        echo     web:
        echo       exposure:
        echo         include: health,info,metrics
        echo.
        echo logging:
        echo   level:
        echo     root: INFO
        echo     com.nushungry: DEBUG
    ) > config-repo\application.yml

    echo Sample config repository created
)

echo.
echo Starting services with docker-compose...
echo.

REM Start services
docker-compose up -d

echo.
echo Waiting for services to be healthy...
echo.

REM Wait for Eureka Server
echo Waiting for Eureka Server...
:wait_eureka
timeout /t 2 /nobreak >nul
curl -s http://localhost:8761/actuator/health >nul 2>&1
if errorlevel 1 (
    goto wait_eureka
)
echo Eureka Server is ready

REM Wait for Config Server
echo Waiting for Config Server...
:wait_config
timeout /t 2 /nobreak >nul
curl -s http://localhost:8888/actuator/health >nul 2>&1
if errorlevel 1 (
    goto wait_config
)
echo Config Server is ready

echo.
echo ===================================================================
echo   All services started successfully!
echo ===================================================================
echo.
echo Service URLs:
echo   - Config Server:    http://localhost:8888
echo   - Eureka Dashboard: http://localhost:8761
echo.
echo Config Server Credentials:
echo   - Username: config
echo   - Password: config123
echo.
echo Test Configuration Access:
echo   curl -u config:config123 http://localhost:8888/application/default
echo.
echo View logs:
echo   docker-compose logs -f config-server
echo.
echo Stop services:
echo   scripts\stop-services.bat
echo.
