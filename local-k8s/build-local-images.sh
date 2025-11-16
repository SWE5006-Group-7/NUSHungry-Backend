#!/bin/bash
set -e

# 服务列表
SERVICES=("user-service" "cafeteria-service" "review-service" "media-service" "preference-service")
VERSION="local"

echo "🏗️  开始构建本地镜像..."
echo ""

for SERVICE in "${SERVICES[@]}"; do
    echo "📦 构建 $SERVICE..."

    # 进入服务目录
    cd "$SERVICE"

    # Maven 构建 JAR
    echo "  ├── Maven 构建..."
    mvn clean package -DskipTests -q

    # Docker 构建镜像
    echo "  ├── Docker 构建..."
    docker build -t "$SERVICE:$VERSION" . -q

    # 将镜像加载到 Kind 集群
    echo "  └── 加载到 Kind 集群..."
    kind load docker-image "$SERVICE:$VERSION" --name nushungry-local

    cd ..
    echo "✅ $SERVICE 完成"
    echo ""
done

echo "🎉 所有镜像构建完成!"
echo ""
echo "验证镜像:"
docker exec -it nushungry-local-control-plane crictl images | grep local