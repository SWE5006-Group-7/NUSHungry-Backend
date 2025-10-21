#!/bin/bash

# ELK Stack启动脚本
# 用于快速启动Elasticsearch、Logstash、Kibana和Filebeat

set -e

echo "========================================="
echo " NUSHungry ELK Stack 启动脚本"
echo "========================================="
echo ""

# 检查Docker是否运行
if ! docker info > /dev/null 2>&1; then
    echo "❌ 错误: Docker未运行，请先启动Docker"
    exit 1
fi

# 检查docker-compose是否安装
if ! command -v docker-compose &> /dev/null; then
    echo "❌ 错误: docker-compose未安装"
    exit 1
fi

# 设置虚拟内存（Linux系统需要）
if [[ "$OSTYPE" == "linux-gnu"* ]]; then
    echo "📝 设置虚拟内存参数..."
    sudo sysctl -w vm.max_map_count=262144
fi

# 创建必要的目录
echo "📁 创建日志目录..."
mkdir -p ../logs

# 启动ELK Stack
echo ""
echo "🚀 启动ELK Stack..."
echo "   - Elasticsearch: http://localhost:9200"
echo "   - Logstash: TCP 5000, HTTP 9600"
echo "   - Kibana: http://localhost:5601"
echo "   - Filebeat: 日志收集器"
echo ""

docker-compose up -d

# 等待服务启动
echo ""
echo "⏳ 等待服务启动（约60秒）..."
sleep 10

# 检查Elasticsearch
echo -n "检查Elasticsearch状态..."
for i in {1..30}; do
    if curl -s http://localhost:9200/_cluster/health > /dev/null 2>&1; then
        echo " ✅"
        break
    fi
    echo -n "."
    sleep 2
done

# 检查Logstash
echo -n "检查Logstash状态..."
for i in {1..30}; do
    if curl -s http://localhost:9600 > /dev/null 2>&1; then
        echo " ✅"
        break
    fi
    echo -n "."
    sleep 2
done

# 检查Kibana
echo -n "检查Kibana状态..."
for i in {1..30}; do
    if curl -s http://localhost:5601/api/status > /dev/null 2>&1; then
        echo " ✅"
        break
    fi
    echo -n "."
    sleep 2
done

echo ""
echo "========================================="
echo " ✅ ELK Stack 启动完成！"
echo "========================================="
echo ""
echo "📊 访问地址:"
echo "   • Kibana UI: http://localhost:5601"
echo "   • Elasticsearch API: http://localhost:9200"
echo "   • Logstash Metrics: http://localhost:9600"
echo ""
echo "📖 下一步:"
echo "   1. 在Kibana中创建索引模式: nushungry-logs-*"
echo "   2. 启动微服务，日志将自动发送到ELK"
echo "   3. 在Kibana Discover页面查看日志"
echo ""
echo "💡 查看服务状态: docker-compose ps"
echo "📝 查看服务日志: docker-compose logs -f"
echo "🛑 停止服务: ./stop-elk.sh"
echo ""
