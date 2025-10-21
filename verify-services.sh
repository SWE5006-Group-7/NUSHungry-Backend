#!/bin/bash

echo "========================================"
echo "检查 NUSHungry 微服务健康状态"
echo "========================================"
echo ""

echo "[1/8] 检查 Eureka Server (服务注册中心)..."
if curl -s http://localhost:8761/actuator/health | grep -q "UP"; then
    echo "✅ Eureka Server - 正常"
else
    echo "❌ Eureka Server - 异常"
fi
echo ""

echo "[2/8] 检查 Gateway Service (API网关)..."
if curl -s http://localhost:8080/actuator/health | grep -q "UP"; then
    echo "✅ Gateway Service - 正常"
else
    echo "❌ Gateway Service - 异常"
fi
echo ""

echo "[3/8] 检查 Admin Service..."
if curl -s http://localhost:8082/actuator/health | grep -q "UP"; then
    echo "✅ Admin Service - 正常"
else
    echo "❌ Admin Service - 异常"
fi
echo ""

echo "[4/8] 检查 Cafeteria Service..."
if curl -s http://localhost:8083/actuator/health | grep -q "UP"; then
    echo "✅ Cafeteria Service - 正常"
else
    echo "❌ Cafeteria Service - 异常"
fi
echo ""

echo "[5/8] 检查 Review Service..."
if curl -s http://localhost:8084/actuator/health | grep -q "UP"; then
    echo "✅ Review Service - 正常"
else
    echo "❌ Review Service - 异常"
fi
echo ""

echo "[6/8] 检查 Media Service..."
if curl -s http://localhost:8085/actuator/health | grep -q "UP"; then
    echo "✅ Media Service - 正常"
else
    echo "❌ Media Service - 异常"
fi
echo ""

echo "[7/8] 检查 Preference Service..."
if curl -s http://localhost:8086/actuator/health | grep -q "UP"; then
    echo "✅ Preference Service - 正常"
else
    echo "❌ Preference Service - 异常"
fi
echo ""

echo "[8/8] 检查基础设施服务..."
echo "  - PostgreSQL (5432)"
echo "  - MongoDB (27017)"
echo "  - Redis (6379)"
echo "  - RabbitMQ (5672, 15672)"
echo "  - MinIO (9000, 9001)"
echo "  - Zipkin (9411)"
echo ""

echo "========================================"
echo "🌐 访问以下 URL 查看各服务状态："
echo "========================================"
echo "📊 Eureka Dashboard:        http://localhost:8761"
echo "    (用户名: eureka, 密码: eureka)"
echo ""
echo "🔌 API Gateway:             http://localhost:8080"
echo "📝 Swagger API 文档:        http://localhost:8080/swagger-ui.html"
echo ""
echo "🐰 RabbitMQ 管理界面:       http://localhost:15672"
echo "    (用户名: guest, 密码: guest)"
echo ""
echo "📦 MinIO 控制台:            http://localhost:9001"
echo "    (用户名: minioadmin, 密码: minioadmin)"
echo ""
echo "🔍 Zipkin 追踪界面:         http://localhost:9411"
echo ""
echo "========================================"
echo "测试完成！"
echo "========================================"
