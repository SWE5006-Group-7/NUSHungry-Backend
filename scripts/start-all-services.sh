#!/bin/bash
set -e  # 遇到错误立即退出

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 检查Docker是否运行
check_docker() {
    if ! docker info > /dev/null 2>&1; then
        echo -e "${RED}❌ Docker未运行,请先启动Docker${NC}"
        exit 1
    fi
}

# 检查docker compose是否可用(V2版本)
check_docker_compose() {
    if ! docker compose version &> /dev/null; then
        echo -e "${RED}❌ Docker Compose未安装,请先安装Docker Compose V2${NC}"
        exit 1
    fi
}

echo "=========================================="
echo -e "${BLUE}启动 NUSHungry 微服务架构${NC}"
echo "=========================================="

# 环境检查
check_docker
check_docker_compose

# 检查 .env 文件
if [ ! -f .env ]; then
    echo -e "${YELLOW}⚠️  未找到 .env 文件,从 .env.example 复制...${NC}"
    if [ ! -f .env.example ]; then
        echo -e "${RED}❌ .env.example 文件不存在${NC}"
        exit 1
    fi
    cp .env.example .env
    echo -e "${GREEN}✅ 已创建 .env 文件,请根据需要修改配置${NC}"
fi

# 启动基础设施服务
echo ""
echo -e "${BLUE}📦 启动基础设施服务 (PostgreSQL, MongoDB, RabbitMQ, MinIO, Redis, Zipkin)...${NC}"
docker compose up -d postgres mongodb rabbitmq minio redis zipkin

if [ $? -ne 0 ]; then
    echo -e "${RED}❌ 基础设施服务启动失败${NC}"
    exit 1
fi

# 等待基础设施服务就绪
echo ""
echo -e "${YELLOW}⏳ 等待基础设施服务启动...${NC}"
sleep 20

# 检查基础设施服务健康状态
echo ""
echo -e "${BLUE}🔍 检查基础设施服务健康状态...${NC}"
docker compose ps postgres mongodb rabbitmq minio redis zipkin

# 启动配置中心
echo ""
echo -e "${BLUE}⚙️  启动配置中心 (Config Server)...${NC}"
docker compose up -d config-server

if [ $? -ne 0 ]; then
    echo -e "${RED}❌ 配置中心启动失败${NC}"
    exit 1
fi

# 等待配置中心就绪
echo ""
echo -e "${YELLOW}⏳ 等待配置中心启动...${NC}"
sleep 15

# 启动服务注册中心
echo ""
echo -e "${BLUE}🔍 启动服务注册中心 (Eureka Server)...${NC}"
docker compose up -d eureka-server

if [ $? -ne 0 ]; then
    echo -e "${RED}❌ Eureka Server启动失败${NC}"
    exit 1
fi

# 等待Eureka就绪
echo ""
echo -e "${YELLOW}⏳ 等待Eureka Server启动...${NC}"
sleep 20

# 启动微服务
echo ""
echo -e "${BLUE}🚀 启动微服务...${NC}"
docker compose up -d admin-service cafeteria-service review-service media-service preference-service gateway-service

if [ $? -ne 0 ]; then
    echo -e "${RED}❌ 微服务启动失败${NC}"
    exit 1
fi

# 等待微服务启动
echo ""
echo -e "${YELLOW}⏳ 等待微服务启动...${NC}"
sleep 30

# 显示所有服务状态
echo ""
echo -e "${BLUE}📊 所有服务状态:${NC}"
docker compose ps

echo ""
echo "=========================================="
echo -e "${GREEN}✅ 所有服务已启动!${NC}"
echo "=========================================="
echo ""
echo -e "${BLUE}核心服务访问地址:${NC}"
echo "  - Gateway (API入口):  http://localhost:8080"
echo "  - Eureka Server:      http://localhost:8761 (eureka/eureka)"
echo "  - Config Server:      http://localhost:8888"
echo ""
echo -e "${BLUE}微服务访问地址:${NC}"
echo "  - Admin Service:      http://localhost:8082"
echo "  - Cafeteria Service:  http://localhost:8083"
echo "  - Review Service:     http://localhost:8084"
echo "  - Media Service:      http://localhost:8085"
echo "  - Preference Service: http://localhost:8086"
echo ""
echo -e "${BLUE}基础设施管理界面:${NC}"
echo "  - RabbitMQ:  http://localhost:15672 (guest/guest)"
echo "  - MinIO:     http://localhost:9001 (minioadmin/minioadmin)"
echo "  - Zipkin:    http://localhost:9411"
echo ""
echo -e "${YELLOW}常用命令:${NC}"
echo "  查看日志: docker compose logs -f [service-name]"
echo "  停止服务: ./scripts/stop-all-services.sh"
echo "=========================================="
