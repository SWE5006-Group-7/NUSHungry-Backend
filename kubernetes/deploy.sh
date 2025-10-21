#!/bin/bash

# NUSHungry Kubernetes 部署脚本
# 用法: ./deploy.sh [namespace] [method]
# 示例: ./deploy.sh nushungry kubectl

set -e

# 默认值
NAMESPACE="${1:-nushungry}"
METHOD="${2:-helm}"
BASE_DIR="$(cd "$(dirname "$0")" && pwd)"

echo "=========================================="
echo "NUSHungry Kubernetes 部署脚本"
echo "=========================================="
echo "命名空间: $NAMESPACE"
echo "部署方法: $METHOD"
echo "=========================================="

# 检查 kubectl
if ! command -v kubectl &> /dev/null; then
    echo "错误: kubectl 未安装或不在 PATH 中"
    exit 1
fi

# 检查集群连接
if ! kubectl cluster-info &> /dev/null; then
    echo "错误: 无法连接到 Kubernetes 集群"
    exit 1
fi

echo "✓ Kubernetes 集群连接正常"

# 函数: 等待 Pod 就绪
wait_for_pods() {
    local label=$1
    local timeout=${2:-300}

    echo "等待 Pod 就绪 (标签: $label, 超时: ${timeout}s)..."
    if kubectl wait --for=condition=ready pod -l "$label" -n "$NAMESPACE" --timeout="${timeout}s" 2>/dev/null; then
        echo "✓ Pod 已就绪"
        return 0
    else
        echo "⚠ 等待超时,部分 Pod 可能未就绪"
        return 1
    fi
}

# 函数: 使用 Helm 部署
deploy_with_helm() {
    echo ""
    echo "使用 Helm 部署..."
    echo "=========================================="

    # 检查 Helm
    if ! command -v helm &> /dev/null; then
        echo "错误: Helm 未安装或不在 PATH 中"
        exit 1
    fi

    echo "✓ Helm 已安装"

    # 检查 values 文件
    CHART_DIR="$BASE_DIR/charts/nushungry"
    VALUES_FILE="$CHART_DIR/values.yaml"

    if [ ! -f "$VALUES_FILE" ]; then
        echo "错误: 找不到 values.yaml 文件: $VALUES_FILE"
        exit 1
    fi

    # 创建命名空间
    echo ""
    echo "创建命名空间: $NAMESPACE"
    kubectl create namespace "$NAMESPACE" --dry-run=client -o yaml | kubectl apply -f -

    # 检查是否已安装
    if helm list -n "$NAMESPACE" | grep -q "nushungry"; then
        echo ""
        echo "检测到现有部署,执行升级..."
        helm upgrade nushungry "$CHART_DIR" \
            -n "$NAMESPACE" \
            -f "$VALUES_FILE" \
            --wait \
            --timeout 10m
        echo "✓ Helm 升级完成"
    else
        echo ""
        echo "执行全新安装..."
        helm install nushungry "$CHART_DIR" \
            -n "$NAMESPACE" \
            -f "$VALUES_FILE" \
            --wait \
            --timeout 10m
        echo "✓ Helm 安装完成"
    fi
}

# 函数: 使用 kubectl 部署
deploy_with_kubectl() {
    echo ""
    echo "使用 kubectl 部署..."
    echo "=========================================="

    BASE_CONFIG_DIR="$BASE_DIR/base"

    # 检查 Secret 文件
    if [ ! -f "$BASE_CONFIG_DIR/secrets.yaml" ]; then
        echo "⚠ 警告: secrets.yaml 不存在"
        echo "请复制 secrets.yaml.example 并填写实际值:"
        echo "  cp $BASE_CONFIG_DIR/secrets.yaml.example $BASE_CONFIG_DIR/secrets.yaml"
        echo ""
        read -p "是否继续? (y/N): " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            exit 1
        fi
    fi

    # 1. 创建命名空间
    echo ""
    echo "[1/8] 创建命名空间..."
    kubectl apply -f "$BASE_CONFIG_DIR/namespace.yaml"

    # 2. 创建 ConfigMap 和 Secret
    echo ""
    echo "[2/8] 创建 ConfigMap 和 Secret..."
    kubectl apply -f "$BASE_CONFIG_DIR/configmaps.yaml"
    if [ -f "$BASE_CONFIG_DIR/secrets.yaml" ]; then
        kubectl apply -f "$BASE_CONFIG_DIR/secrets.yaml"
    fi

    # 3. 创建 PersistentVolume
    echo ""
    echo "[3/8] 创建 PersistentVolume..."
    kubectl apply -f "$BASE_CONFIG_DIR/persistent-volumes.yaml"

    # 4. 部署数据库
    echo ""
    echo "[4/8] 部署数据库 StatefulSet..."
    kubectl apply -f "$BASE_CONFIG_DIR/postgres-statefulset.yaml"
    kubectl apply -f "$BASE_CONFIG_DIR/mongodb-statefulset.yaml"
    kubectl apply -f "$BASE_CONFIG_DIR/redis-statefulset.yaml"
    kubectl apply -f "$BASE_CONFIG_DIR/rabbitmq-statefulset.yaml"

    echo "等待数据库就绪..."
    sleep 10
    wait_for_pods "tier=database" 300

    # 5. 部署基础服务
    echo ""
    echo "[5/8] 部署基础服务 (Eureka, Config Server)..."
    kubectl apply -f "$BASE_CONFIG_DIR/eureka-server-deployment.yaml"
    kubectl apply -f "$BASE_CONFIG_DIR/config-server-deployment.yaml"

    echo "等待基础服务就绪..."
    sleep 10
    wait_for_pods "tier=infrastructure" 300

    # 6. 部署应用服务
    echo ""
    echo "[6/8] 部署应用服务..."
    kubectl apply -f "$BASE_CONFIG_DIR/gateway-service-deployment.yaml"
    kubectl apply -f "$BASE_CONFIG_DIR/admin-service-deployment.yaml"
    kubectl apply -f "$BASE_CONFIG_DIR/cafeteria-service-deployment.yaml"
    kubectl apply -f "$BASE_CONFIG_DIR/review-service-deployment.yaml"
    kubectl apply -f "$BASE_CONFIG_DIR/media-service-deployment.yaml"
    kubectl apply -f "$BASE_CONFIG_DIR/preference-service-deployment.yaml"

    # 7. 创建 Service
    echo ""
    echo "[7/8] 创建 Service..."
    kubectl apply -f "$BASE_CONFIG_DIR/services.yaml"

    # 8. 创建 Ingress
    echo ""
    echo "[8/8] 创建 Ingress..."
    kubectl apply -f "$BASE_CONFIG_DIR/ingress.yaml"

    echo ""
    echo "✓ kubectl 部署完成"
}

# 主流程
case "$METHOD" in
    helm)
        deploy_with_helm
        ;;
    kubectl)
        deploy_with_kubectl
        ;;
    *)
        echo "错误: 不支持的部署方法: $METHOD"
        echo "支持的方法: helm, kubectl"
        exit 1
        ;;
esac

# 显示部署状态
echo ""
echo "=========================================="
echo "部署状态"
echo "=========================================="
echo ""
echo "Pods:"
kubectl get pods -n "$NAMESPACE"
echo ""
echo "Services:"
kubectl get svc -n "$NAMESPACE"
echo ""
echo "Ingress:"
kubectl get ingress -n "$NAMESPACE"

echo ""
echo "=========================================="
echo "✓ 部署完成!"
echo "=========================================="
echo ""
echo "后续步骤:"
echo "1. 查看 Pod 状态: kubectl get pods -n $NAMESPACE -w"
echo "2. 查看日志: kubectl logs -f -n $NAMESPACE -l app=gateway-service"
echo "3. 访问应用: 查看 Ingress 获取访问地址"
echo ""
