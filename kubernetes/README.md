# NUSHungry Kubernetes 部署指南

本目录包含 NUSHungry 微服务平台的完整 Kubernetes 部署配置。

## 📋 目录结构

```
kubernetes/
├── base/                           # 基础 Kubernetes 配置
│   ├── namespace.yaml              # 命名空间定义
│   ├── configmaps.yaml             # 配置映射
│   ├── secrets.yaml.example        # Secret 示例（复制并填写实际值）
│   ├── services.yaml               # 所有微服务的 Service 定义
│   ├── ingress.yaml                # Ingress 路由配置
│   ├── persistent-volumes.yaml     # 持久化存储配置
│   ├── *-deployment.yaml           # 微服务 Deployment 配置
│   ├── *-statefulset.yaml          # 数据库 StatefulSet 配置
│   └── ...
├── overlays/                       # Kustomize 环境覆盖配置
│   ├── dev/                        # 开发环境
│   └── prod/                       # 生产环境
├── charts/                         # Helm Charts
│   └── nushungry/                  # NUSHungry Helm Chart
│       ├── Chart.yaml
│       ├── values.yaml
│       ├── templates/
│       └── README.md
└── README.md                       # 本文件
```

## 🚀 快速开始

### 前提条件

1. **Kubernetes 集群** (1.19+)
   - Minikube (本地开发)
   - Amazon EKS (AWS)
   - Google GKE (GCP)
   - Azure AKS (Azure)
   - 自建 Kubernetes 集群

2. **kubectl** (与集群版本匹配)
   ```bash
   kubectl version --client
   ```

3. **Helm** (3.2.0+)
   ```bash
   helm version
   ```

4. **Ingress Controller** (推荐 NGINX)
   ```bash
   # 安装 NGINX Ingress Controller
   kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.8.1/deploy/static/provider/cloud/deploy.yaml
   ```

5. **cert-manager** (可选，用于自动管理 TLS 证书)
   ```bash
   kubectl apply -f https://github.com/cert-manager/cert-manager/releases/download/v1.13.0/cert-manager.yaml
   ```

### 方法 1: 使用 Helm Chart 部署 (推荐)

#### 1. 准备配置文件

```bash
cd kubernetes/charts/nushungry
cp values.yaml values-prod.yaml
```

#### 2. 编辑 `values-prod.yaml` 并修改以下内容:

```yaml
# 修改域名
ingress:
  hosts:
    - host: api.your-domain.com

# 修改所有密码 (CHANGE_ME_* 部分)
secrets:
  jwt:
    secret: "your-secure-jwt-secret-key"
  postgresql:
    admin:
      password: "your-admin-db-password"
  # ... 修改所有密码
```

#### 3. 部署应用

```bash
# 创建命名空间
kubectl create namespace nushungry

# 使用 Helm 部署
helm install nushungry . \
  -n nushungry \
  -f values-prod.yaml

# 查看部署状态
kubectl get pods -n nushungry -w
```

#### 4. 验证部署

```bash
# 检查所有 Pod 是否运行
kubectl get pods -n nushungry

# 检查所有 Service
kubectl get svc -n nushungry

# 检查 Ingress
kubectl get ingress -n nushungry

# 查看 Gateway Service 日志
kubectl logs -n nushungry -l app=gateway-service
```

### 方法 2: 使用 kubectl 直接部署

#### 1. 创建 Secret 配置

```bash
cd kubernetes/base

# 复制并编辑 Secret 示例
cp secrets.yaml.example secrets.yaml

# 编辑 secrets.yaml 并填写 base64 编码的密码
# 生成 base64: echo -n "your-password" | base64
vim secrets.yaml
```

#### 2. 按顺序部署资源

```bash
# 1. 创建命名空间
kubectl apply -f namespace.yaml

# 2. 创建 ConfigMap 和 Secret
kubectl apply -f configmaps.yaml
kubectl apply -f secrets.yaml

# 3. 创建 PersistentVolume
kubectl apply -f persistent-volumes.yaml

# 4. 部署数据库 StatefulSet
kubectl apply -f postgres-statefulset.yaml
kubectl apply -f mongodb-statefulset.yaml
kubectl apply -f redis-statefulset.yaml
kubectl apply -f rabbitmq-statefulset.yaml

# 等待数据库就绪
kubectl wait --for=condition=ready pod -l tier=database -n nushungry --timeout=300s

# 5. 部署微服务
kubectl apply -f eureka-server-deployment.yaml
kubectl apply -f config-server-deployment.yaml

# 等待基础服务就绪
kubectl wait --for=condition=ready pod -l app=eureka-server -n nushungry --timeout=300s
kubectl wait --for=condition=ready pod -l app=config-server -n nushungry --timeout=300s

# 6. 部署应用服务
kubectl apply -f gateway-service-deployment.yaml
kubectl apply -f admin-service-deployment.yaml
kubectl apply -f cafeteria-service-deployment.yaml
kubectl apply -f review-service-deployment.yaml
kubectl apply -f media-service-deployment.yaml
kubectl apply -f preference-service-deployment.yaml

# 7. 创建 Service
kubectl apply -f services.yaml

# 8. 创建 Ingress
kubectl apply -f ingress.yaml
```

#### 3. 检查部署状态

```bash
# 查看所有资源
kubectl get all -n nushungry

# 查看 Pod 详情
kubectl describe pods -n nushungry

# 查看事件
kubectl get events -n nushungry --sort-by='.lastTimestamp'
```

## 🔧 配置说明

### 环境变量

所有微服务通过以下方式获取配置:

1. **ConfigMap** (`configmaps.yaml`) - 非敏感配置
2. **Secret** (`secrets.yaml`) - 敏感信息 (密码、密钥)
3. **Config Server** - 集中式配置管理

### 资源配置

每个服务的资源限制可在 Deployment 或 Helm values.yaml 中调整:

```yaml
resources:
  requests:
    memory: "768Mi"
    cpu: "500m"
  limits:
    memory: "1.5Gi"
    cpu: "1000m"
```

### 副本数量

根据负载调整副本数:

```yaml
spec:
  replicas: 3  # 修改为所需副本数
```

或使用 Horizontal Pod Autoscaler (HPA):

```bash
kubectl autoscale deployment gateway-service \
  --cpu-percent=70 \
  --min=3 \
  --max=10 \
  -n nushungry
```

### 持久化存储

#### 本地开发 (hostPath)

默认使用 `hostPath` 用于本地测试:

```yaml
spec:
  hostPath:
    path: "/mnt/data/nushungry/media"
```

#### 云环境 (动态配置)

取消注释并使用对应的 StorageClass:

```yaml
# AWS EBS
storageClassName: gp3

# GCP Persistent Disk
storageClassName: pd-ssd

# Azure Disk
storageClassName: managed-premium
```

## 🌐 访问应用

### 获取 Ingress IP/域名

```bash
kubectl get ingress -n nushungry
```

### 本地开发 (Minikube)

```bash
# 启动 Minikube tunnel
minikube tunnel

# 或者使用端口转发
kubectl port-forward -n nushungry svc/gateway-service 8080:80
```

### 生产环境

配置 DNS 记录指向 Ingress LoadBalancer IP:

```
api.your-domain.com -> <INGRESS_IP>
```

## 🔍 监控和日志

### 查看 Pod 日志

```bash
# 实时查看日志
kubectl logs -f -n nushungry <pod-name>

# 查看前一个容器的日志 (重启后)
kubectl logs -n nushungry <pod-name> --previous

# 查看特定服务的所有 Pod 日志
kubectl logs -n nushungry -l app=gateway-service --tail=100
```

### 进入 Pod 调试

```bash
kubectl exec -it -n nushungry <pod-name> -- /bin/sh
```

### Prometheus + Grafana

如果启用了监控:

```bash
# 访问 Grafana
kubectl port-forward -n monitoring svc/grafana 3000:80

# 访问 Prometheus
kubectl port-forward -n monitoring svc/prometheus 9090:9090
```

## 🔄 更新和回滚

### 更新镜像

```bash
# 更新单个服务
kubectl set image deployment/gateway-service \
  gateway-service=nushungry/gateway-service:v2.0 \
  -n nushungry

# 查看滚动更新状态
kubectl rollout status deployment/gateway-service -n nushungry
```

### 回滚部署

```bash
# 查看历史版本
kubectl rollout history deployment/gateway-service -n nushungry

# 回滚到上一个版本
kubectl rollout undo deployment/gateway-service -n nushungry

# 回滚到指定版本
kubectl rollout undo deployment/gateway-service --to-revision=2 -n nushungry
```

### Helm 升级

```bash
# 升级 Helm release
helm upgrade nushungry ./charts/nushungry \
  -n nushungry \
  -f values-prod.yaml

# 回滚 Helm release
helm rollback nushungry 1 -n nushungry
```

## 🗑️ 清理资源

### 删除 Helm 部署

```bash
helm uninstall nushungry -n nushungry

# 删除 PVC (可选)
kubectl delete pvc -n nushungry --all
```

### 删除 kubectl 部署

```bash
# 删除所有资源
kubectl delete namespace nushungry

# 或逐个删除
kubectl delete -f kubernetes/base/
```

## 🛠️ 故障排查

### Pod 启动失败

```bash
# 查看 Pod 详情
kubectl describe pod -n nushungry <pod-name>

# 查看事件
kubectl get events -n nushungry --sort-by='.lastTimestamp'

# 查看日志
kubectl logs -n nushungry <pod-name>
```

### 常见问题

#### 1. ImagePullBackOff

```bash
# 检查镜像名称和标签
kubectl describe pod <pod-name> -n nushungry

# 确保镜像已推送到仓库
docker images | grep nushungry
```

#### 2. CrashLoopBackOff

```bash
# 查看容器日志
kubectl logs <pod-name> -n nushungry --previous

# 检查健康检查配置
kubectl describe pod <pod-name> -n nushungry
```

#### 3. PVC Pending

```bash
# 检查 StorageClass
kubectl get sc

# 检查 PVC 状态
kubectl describe pvc -n nushungry
```

#### 4. Service 无法访问

```bash
# 检查 Service 端点
kubectl get endpoints -n nushungry

# 检查 Pod 标签匹配
kubectl get pods -n nushungry --show-labels
```

## 📚 参考文档

- [Kubernetes 官方文档](https://kubernetes.io/docs/home/)
- [Helm 文档](https://helm.sh/docs/)
- [NGINX Ingress Controller](https://kubernetes.github.io/ingress-nginx/)
- [cert-manager](https://cert-manager.io/docs/)

## 📧 联系支持

如有问题,请联系 NUSHungry 团队或提交 GitHub Issue。
