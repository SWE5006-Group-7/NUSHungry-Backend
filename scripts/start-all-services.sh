#!/bin/bash

echo "=========================================="
echo "启动 NUSHungry 微服务架构"
echo "=========================================="

# 检查 .env 文件
if [ ! -f .env ]; then
    echo "⚠️  未找到 .env 文件，从 .env.example 复制..."
    cp .env.example .env
    echo "✅ 已创建 .env 文件，请根据需要修改配置"
fi

# 启动基础设施服务
echo ""
echo "📦 启动基础设施服务 (PostgreSQL, MongoDB, RabbitMQ, MinIO)..."
docker-compose up -d postgres mongodb rabbitmq minio

# 等待基础设施服务就绪
echo ""
echo "⏳ 等待基础设施服务启动..."
sleep 20

# 检查基础设施服务健康状态
echo ""
echo "🔍 检查基础设施服务健康状态..."
docker-compose ps postgres mongodb rabbitmq minio

# 启动微服务
echo ""
echo "🚀 启动微服务..."
docker-compose up -d admin-service cafeteria-service review-service media-service preference-service

# 等待微服务启动
echo ""
echo "⏳ 等待微服务启动..."
sleep 30

# 显示所有服务状态
echo ""
echo "📊 所有服务状态："
docker-compose ps

echo ""
echo "=========================================="
echo "✅ 所有服务已启动！"
echo "=========================================="
echo ""
echo "服务访问地址："
echo "  - Admin Service:      http://localhost:8082"
echo "  - Cafeteria Service:  http://localhost:8083"
echo "  - Review Service:     http://localhost:8084"
echo "  - Media Service:      http://localhost:8085"
echo "  - Preference Service: http://localhost:8086"
echo ""
echo "基础设施管理界面："
echo "  - RabbitMQ:  http://localhost:15672 (guest/guest)"
echo "  - MinIO:     http://localhost:9001 (minioadmin/minioadmin)"
echo ""
echo "查看日志: docker-compose logs -f [service-name]"
echo "停止服务: ./scripts/stop-all-services.sh"
echo "=========================================="
