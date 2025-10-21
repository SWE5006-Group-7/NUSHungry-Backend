#!/bin/bash

# NUSHungry Kubernetes 卸载脚本
# 用法: ./undeploy.sh [namespace] [method]
# 示例: ./undeploy.sh nushungry helm

set -e

# 默认值
NAMESPACE="${1:-nushungry}"
METHOD="${2:-helm}"
BASE_DIR="$(cd "$(dirname "$0")" && pwd)"

echo "=========================================="
echo "NUSHungry Kubernetes 卸载脚本"
echo "=========================================="
echo "命名空间: $NAMESPACE"
echo "卸载方法: $METHOD"
echo "=========================================="

# 确认操作
echo ""
echo "⚠️  警告: 此操作将删除所有资源!"
read -p "是否继续? (y/N): " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "操作已取消"
    exit 0
fi

# 检查 kubectl
if ! command -v kubectl &> /dev/null; then
    echo "错误: kubectl 未安装或不在 PATH 中"
    exit 1
fi

# 函数: 使用 Helm 卸载
undeploy_with_helm() {
    echo ""
    echo "使用 Helm 卸载..."
    echo "=========================================="

    # 检查 Helm
    if ! command -v helm &> /dev/null; then
        echo "错误: Helm 未安装或不在 PATH 中"
        exit 1
    fi

    # 检查 release 是否存在
    if helm list -n "$NAMESPACE" | grep -q "nushungry"; then
        echo "卸载 Helm release..."
        helm uninstall nushungry -n "$NAMESPACE"
        echo "✓ Helm release 已卸载"
    else
        echo "未找到 Helm release: nushungry"
    fi

    # 询问是否删除 PVC
    echo ""
    read -p "是否删除 PersistentVolumeClaim (数据将丢失)? (y/N): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        echo "删除 PVC..."
        kubectl delete pvc -n "$NAMESPACE" --all --ignore-not-found=true
        echo "✓ PVC 已删除"
    fi
}

# 函数: 使用 kubectl 卸载
undeploy_with_kubectl() {
    echo ""
    echo "使用 kubectl 卸载..."
    echo "=========================================="

    BASE_CONFIG_DIR="$BASE_DIR/base"

    # 按反向顺序删除资源
    echo ""
    echo "[1/8] 删除 Ingress..."
    kubectl delete -f "$BASE_CONFIG_DIR/ingress.yaml" --ignore-not-found=true

    echo ""
    echo "[2/8] 删除 Service..."
    kubectl delete -f "$BASE_CONFIG_DIR/services.yaml" --ignore-not-found=true

    echo ""
    echo "[3/8] 删除应用服务 Deployment..."
    kubectl delete -f "$BASE_CONFIG_DIR/gateway-service-deployment.yaml" --ignore-not-found=true
    kubectl delete -f "$BASE_CONFIG_DIR/admin-service-deployment.yaml" --ignore-not-found=true
    kubectl delete -f "$BASE_CONFIG_DIR/cafeteria-service-deployment.yaml" --ignore-not-found=true
    kubectl delete -f "$BASE_CONFIG_DIR/review-service-deployment.yaml" --ignore-not-found=true
    kubectl delete -f "$BASE_CONFIG_DIR/media-service-deployment.yaml" --ignore-not-found=true
    kubectl delete -f "$BASE_CONFIG_DIR/preference-service-deployment.yaml" --ignore-not-found=true

    echo ""
    echo "[4/8] 删除基础服务 Deployment..."
    kubectl delete -f "$BASE_CONFIG_DIR/eureka-server-deployment.yaml" --ignore-not-found=true
    kubectl delete -f "$BASE_CONFIG_DIR/config-server-deployment.yaml" --ignore-not-found=true

    echo ""
    echo "[5/8] 删除数据库 StatefulSet..."
    kubectl delete -f "$BASE_CONFIG_DIR/postgres-statefulset.yaml" --ignore-not-found=true
    kubectl delete -f "$BASE_CONFIG_DIR/mongodb-statefulset.yaml" --ignore-not-found=true
    kubectl delete -f "$BASE_CONFIG_DIR/redis-statefulset.yaml" --ignore-not-found=true
    kubectl delete -f "$BASE_CONFIG_DIR/rabbitmq-statefulset.yaml" --ignore-not-found=true

    # 询问是否删除 PVC
    echo ""
    read -p "是否删除 PersistentVolumeClaim (数据将丢失)? (y/N): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        echo "[6/8] 删除 PVC..."
        kubectl delete pvc -n "$NAMESPACE" --all --ignore-not-found=true
    else
        echo "[6/8] 跳过删除 PVC"
    fi

    echo ""
    echo "[7/8] 删除 PersistentVolume..."
    kubectl delete -f "$BASE_CONFIG_DIR/persistent-volumes.yaml" --ignore-not-found=true

    echo ""
    echo "[8/8] 删除 ConfigMap 和 Secret..."
    kubectl delete -f "$BASE_CONFIG_DIR/configmaps.yaml" --ignore-not-found=true
    if [ -f "$BASE_CONFIG_DIR/secrets.yaml" ]; then
        kubectl delete -f "$BASE_CONFIG_DIR/secrets.yaml" --ignore-not-found=true
    fi

    echo ""
    echo "✓ kubectl 卸载完成"
}

# 主流程
case "$METHOD" in
    helm)
        undeploy_with_helm
        ;;
    kubectl)
        undeploy_with_kubectl
        ;;
    *)
        echo "错误: 不支持的卸载方法: $METHOD"
        echo "支持的方法: helm, kubectl"
        exit 1
        ;;
esac

# 询问是否删除命名空间
echo ""
read -p "是否删除命名空间 $NAMESPACE (所有资源将被删除)? (y/N): " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo "删除命名空间..."
    kubectl delete namespace "$NAMESPACE" --ignore-not-found=true
    echo "✓ 命名空间已删除"
else
    echo "保留命名空间"
fi

echo ""
echo "=========================================="
echo "✓ 卸载完成!"
echo "=========================================="
