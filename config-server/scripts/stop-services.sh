#!/bin/bash

# Stop Config Server and dependencies

set -e

echo "==================================================================="
echo "  Stopping NUSHungry Config Server"
echo "==================================================================="
echo ""

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Navigate to config-server directory
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$SCRIPT_DIR/.."

echo -e "${YELLOW}🛑 Stopping services...${NC}"
echo ""

# Stop services
docker-compose down

echo ""
echo -e "${GREEN}==================================================================="
echo -e "  ✅ All services stopped successfully!"
echo -e "===================================================================${NC}"
echo ""
echo "To remove volumes as well:"
echo "  docker-compose down -v"
echo ""
echo "To start services again:"
echo "  ./scripts/start-services.sh"
echo ""
