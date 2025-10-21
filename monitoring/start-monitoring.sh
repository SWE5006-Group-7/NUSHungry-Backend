#!/bin/bash

# ============================================
# Start Monitoring Stack (Prometheus + Grafana)
# ============================================

set -e

echo "=========================================="
echo "Starting NUSHungry Monitoring Stack"
echo "=========================================="
echo ""

# 检查Docker是否运行
if ! docker info > /dev/null 2>&1; then
    echo "❌ Error: Docker is not running."
    echo "Please start Docker Desktop and try again."
    exit 1
fi

# 检查是否存在主应用网络
if ! docker network inspect nushungry > /dev/null 2>&1; then
    echo "⚠️  Warning: 'nushungry' network does not exist."
    echo "Creating 'nushungry' network..."
    docker network create nushungry
    echo "✅ Network created successfully."
fi

# 启动监控栈
echo "🚀 Starting Prometheus and Grafana..."
echo ""
docker-compose up -d

# 等待服务启动
echo ""
echo "⏳ Waiting for services to be healthy..."
sleep 10

# 健康检查
check_service() {
    local service=$1
    local url=$2
    local max_attempts=30
    local attempt=1

    while [ $attempt -le $max_attempts ]; do
        if curl -s "$url" > /dev/null 2>&1; then
            echo "✅ $service is healthy"
            return 0
        fi
        echo "   Attempt $attempt/$max_attempts: Waiting for $service..."
        sleep 2
        attempt=$((attempt + 1))
    done

    echo "❌ $service failed to start"
    return 1
}

# 检查Prometheus
check_service "Prometheus" "http://localhost:9090/-/healthy"

# 检查Grafana
check_service "Grafana" "http://localhost:3000/api/health"

echo ""
echo "=========================================="
echo "✅ Monitoring Stack Started Successfully"
echo "=========================================="
echo ""
echo "📊 Access URLs:"
echo "   - Prometheus: http://localhost:9090"
echo "   - Grafana:    http://localhost:3000"
echo ""
echo "🔐 Grafana Credentials:"
echo "   - Username: admin"
echo "   - Password: admin"
echo "   (You will be prompted to change password on first login)"
echo ""
echo "📈 View metrics:"
echo "   1. Open Grafana (http://localhost:3000)"
echo "   2. Login with credentials above"
echo "   3. Go to Dashboards → NUSHungry Microservices Overview"
echo ""
echo "💡 Tip: Make sure your microservices are running to see metrics."
echo ""
