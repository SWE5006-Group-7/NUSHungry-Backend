#!/bin/bash
# Media Service - 停止脚本 (Linux/Mac)

set -e

echo ""
echo "============================================"
echo "  Media Service - 停止服务"
echo "============================================"
echo ""

echo "请选择停止方式:"
echo "  1) 停止容器（保留数据）"
echo "  2) 停止并删除容器（保留数据卷）"
echo "  3) 停止并删除所有（包括数据卷）⚠️"
echo ""
read -p "请输入选项 (1-3): " choice

case $choice in
    1) docker-compose stop ;;
    2) docker-compose down ;;
    3)
        read -p "确认删除所有数据? (yes/no): " confirm
        [ "$confirm" = "yes" ] && docker-compose down -v
        ;;
    *) docker-compose stop ;;
esac

echo "操作完成"
