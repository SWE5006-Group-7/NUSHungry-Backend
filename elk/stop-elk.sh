#!/bin/bash

# ELK Stack停止脚本

set -e

echo "========================================="
echo " NUSHungry ELK Stack 停止脚本"
echo "========================================="
echo ""

# 停止ELK Stack
echo "🛑 停止ELK Stack..."
docker-compose down

echo ""
echo "✅ ELK Stack已停止"
echo ""
echo "💡 提示:"
echo "   • 重新启动: ./start-elk.sh"
echo "   • 删除所有数据卷: docker-compose down -v"
echo "   • 查看容器状态: docker-compose ps"
echo ""
