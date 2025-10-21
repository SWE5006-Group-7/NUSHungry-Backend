#!/bin/bash

# ============================================
# Stop Monitoring Stack (Prometheus + Grafana)
# ============================================

set -e

echo "=========================================="
echo "Stopping NUSHungry Monitoring Stack"
echo "=========================================="
echo ""

# 停止服务
echo "🛑 Stopping Prometheus and Grafana..."
docker-compose down

echo ""
echo "✅ Monitoring Stack Stopped Successfully"
echo ""
echo "💡 To remove all monitoring data, run:"
echo "   docker-compose down -v"
echo ""
