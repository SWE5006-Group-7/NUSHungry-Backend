#!/bin/bash
# Media Service - 启动脚本 (Linux/Mac)

set -e

GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m'

echo ""
echo "============================================"
echo "  Media Service - 启动服务"
echo "============================================"
echo ""

echo -e "${BLUE}[INFO]${NC} 创建必要的目录..."
mkdir -p logs uploads

echo -e "${BLUE}[INFO]${NC} 启动服务..."
docker-compose up -d

echo -e "${BLUE}[INFO]${NC} 等待服务启动..."
sleep 60

echo ""
echo "============================================"
echo "  服务已成功启动！"
echo "============================================"
echo ""
echo "📋 服务访问信息:"
echo "  🔹 Media Service: http://localhost:8085"
echo "  🔹 Swagger UI: http://localhost:8085/swagger-ui.html"
echo "  🔹 PostgreSQL: localhost:5434"
echo "  🔹 MinIO Console: http://localhost:9001"
echo ""
echo "📝 测试文件上传:"
echo '  curl -X POST http://localhost:8085/api/media/upload -F "file=@test.jpg"'
echo ""
