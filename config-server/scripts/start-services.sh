#!/bin/bash

# Start Config Server and dependencies
# This script starts the Config Server along with Eureka Server

set -e

echo "==================================================================="
echo "  Starting NUSHungry Config Server"
echo "==================================================================="
echo ""

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo -e "${RED}❌ Error: Docker is not running${NC}"
    echo "Please start Docker and try again."
    exit 1
fi

# Check if docker-compose is installed
if ! command -v docker-compose &> /dev/null; then
    echo -e "${RED}❌ Error: docker-compose is not installed${NC}"
    echo "Please install docker-compose and try again."
    exit 1
fi

# Navigate to config-server directory
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$SCRIPT_DIR/.."

echo -e "${YELLOW}📋 Checking configuration...${NC}"

# Check if config repository exists
if [ ! -d "./config-repo" ]; then
    echo -e "${YELLOW}⚠️  Config repository not found. Creating sample repository...${NC}"
    mkdir -p ./config-repo

    # Create sample application.yml
    cat > ./config-repo/application.yml << 'EOF'
# Common configuration for all services
spring:
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics

logging:
  level:
    root: INFO
    com.nushungry: DEBUG
EOF

    echo -e "${GREEN}✅ Sample config repository created${NC}"
fi

echo ""
echo -e "${YELLOW}🚀 Starting services with docker-compose...${NC}"
echo ""

# Start services
docker-compose up -d

echo ""
echo -e "${YELLOW}⏳ Waiting for services to be healthy...${NC}"
echo ""

# Wait for Eureka Server
echo -n "Waiting for Eureka Server"
for i in {1..60}; do
    if curl -s http://localhost:8761/actuator/health > /dev/null 2>&1; then
        echo -e " ${GREEN}✓${NC}"
        break
    fi
    echo -n "."
    sleep 2
    if [ $i -eq 60 ]; then
        echo -e " ${RED}✗${NC}"
        echo -e "${RED}❌ Eureka Server failed to start${NC}"
        exit 1
    fi
done

# Wait for Config Server
echo -n "Waiting for Config Server"
for i in {1..60}; do
    if curl -s http://localhost:8888/actuator/health > /dev/null 2>&1; then
        echo -e " ${GREEN}✓${NC}"
        break
    fi
    echo -n "."
    sleep 2
    if [ $i -eq 60 ]; then
        echo -e " ${RED}✗${NC}"
        echo -e "${RED}❌ Config Server failed to start${NC}"
        exit 1
    fi
done

echo ""
echo -e "${GREEN}==================================================================="
echo -e "  ✅ All services started successfully!"
echo -e "===================================================================${NC}"
echo ""
echo "Service URLs:"
echo "  • Config Server:    http://localhost:8888"
echo "  • Eureka Dashboard: http://localhost:8761"
echo ""
echo "Config Server Credentials:"
echo "  • Username: config"
echo "  • Password: config123"
echo ""
echo "Test Configuration Access:"
echo "  curl -u config:config123 http://localhost:8888/application/default"
echo ""
echo "View logs:"
echo "  docker-compose logs -f config-server"
echo ""
echo "Stop services:"
echo "  ./scripts/stop-services.sh"
echo ""
