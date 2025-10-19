#!/bin/bash
set -e
echo "============================================"
echo "  Preference Service - 启动服务"
echo "============================================"
mkdir -p logs
docker-compose up -d
sleep 30
echo "服务已启动！"
echo "  API: http://localhost:8086"
echo "  Swagger: http://localhost:8086/swagger-ui.html"
