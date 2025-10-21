@echo off
REM ELK Stack启动脚本 (Windows版本)
REM 用于快速启动Elasticsearch、Logstash、Kibana和Filebeat

echo =========================================
echo  NUSHungry ELK Stack 启动脚本
echo =========================================
echo.

REM 检查Docker是否运行
docker info >nul 2>&1
if errorlevel 1 (
    echo ❌ 错误: Docker未运行，请先启动Docker Desktop
    pause
    exit /b 1
)

REM 检查docker-compose是否安装
docker-compose --version >nul 2>&1
if errorlevel 1 (
    echo ❌ 错误: docker-compose未安装
    pause
    exit /b 1
)

REM 创建必要的目录
echo 📁 创建日志目录...
if not exist ..\logs mkdir ..\logs

REM 启动ELK Stack
echo.
echo 🚀 启动ELK Stack...
echo    - Elasticsearch: http://localhost:9200
echo    - Logstash: TCP 5000, HTTP 9600
echo    - Kibana: http://localhost:5601
echo    - Filebeat: 日志收集器
echo.

docker-compose up -d

REM 等待服务启动
echo.
echo ⏳ 等待服务启动（约60秒）...
timeout /t 10 /nobreak >nul

REM 检查Elasticsearch
echo 检查Elasticsearch状态...
:check_es
curl -s http://localhost:9200/_cluster/health >nul 2>&1
if errorlevel 1 (
    timeout /t 2 /nobreak >nul
    goto check_es
)
echo ✅ Elasticsearch已就绪

REM 检查Logstash
echo 检查Logstash状态...
:check_logstash
curl -s http://localhost:9600 >nul 2>&1
if errorlevel 1 (
    timeout /t 2 /nobreak >nul
    goto check_logstash
)
echo ✅ Logstash已就绪

REM 检查Kibana
echo 检查Kibana状态...
:check_kibana
curl -s http://localhost:5601/api/status >nul 2>&1
if errorlevel 1 (
    timeout /t 2 /nobreak >nul
    goto check_kibana
)
echo ✅ Kibana已就绪

echo.
echo =========================================
echo  ✅ ELK Stack 启动完成！
echo =========================================
echo.
echo 📊 访问地址:
echo    • Kibana UI: http://localhost:5601
echo    • Elasticsearch API: http://localhost:9200
echo    • Logstash Metrics: http://localhost:9600
echo.
echo 📖 下一步:
echo    1. 在Kibana中创建索引模式: nushungry-logs-*
echo    2. 启动微服务，日志将自动发送到ELK
echo    3. 在Kibana Discover页面查看日志
echo.
echo 💡 查看服务状态: docker-compose ps
echo 📝 查看服务日志: docker-compose logs -f
echo 🛑 停止服务: stop-elk.bat
echo.

pause
