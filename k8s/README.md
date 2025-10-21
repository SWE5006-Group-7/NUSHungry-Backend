# Kubernetes 部署配置

本目录包含 NUSHungry 微服务系统的 Kubernetes 部署配置文件。

## 📁 目录结构

```
k8s/
├── namespaces/          # 命名空间定义
├── secrets/             # 敏感信息（密码、密钥）
├── configmaps/          # 配置文件
├── databases/           # 数据库部署（PostgreSQL, MongoDB）
├── infrastructure/      # 基础设施（Redis, RabbitMQ, MinIO, Zipkin）
├── services/            # 微服务部署
├── ingress/             # 外部访问路由
└── README.md           # 本文件
```

## 🚀 快速部署

### 前提条件

1. **已安装 Kubernetes 集群**（选择其一）：
   - Minikube（本地测试）
   - K3s（生产环境轻量级方案）
   - 云服务商托管 K8s（AWS EKS、GCP GKE、Azure AKS）

2. **已安装 kubectl**：
   ```bash
   kubectl version --client
   ```

3. **已配置 kubectl 连接到集群**：
   ```bash
   kubectl cluster-info
   kubectl get nodes
   ```

### 部署步骤

```bash
# 1. 进入 k8s 目录
cd k8s/

# 2. 创建命名空间
kubectl apply -f namespaces/

# 3. ⚠️ 创建 Secrets（先修改密码！）
# 方式 A：使用配置文件（不推荐，密码会提交到 Git）
kubectl apply -f secrets/

# 方式 B：使用命令行创建（推荐，密码不会泄露）
kubectl create secret generic nushungry-secrets \
  --from-literal=POSTGRES_USER=postgres \
  --from-literal=POSTGRES_PASSWORD=$(openssl rand -base64 32) \
  --from-literal=MONGO_USER=admin \
  --from-literal=MONGO_PASSWORD=$(openssl rand -base64 32) \
  --from-literal=RABBITMQ_USER=nushungry \
  --from-literal=RABBITMQ_PASSWORD=$(openssl rand -base64 32) \
  --from-literal=MINIO_USER=minioadmin \
  --from-literal=MINIO_PASSWORD=$(openssl rand -base64 32) \
  --from-literal=EUREKA_USERNAME=eureka \
  --from-literal=EUREKA_PASSWORD=$(openssl rand -base64 32) \
  --from-literal=JWT_SECRET=$(openssl rand -base64 32) \
  --from-literal=REDIS_PASSWORD=$(openssl rand -base64 32) \
  --namespace=nushungry

# 4. 创建 ConfigMaps
kubectl apply -f configmaps/

# 5. 部署数据库
kubectl apply -f databases/

# 等待数据库就绪（约 1-2 分钟）
kubectl wait --for=condition=ready pod -l app=postgres -n nushungry --timeout=300s
kubectl wait --for=condition=ready pod -l app=mongodb -n nushungry --timeout=300s

# 6. 部署基础设施
kubectl apply -f infrastructure/

# 等待基础设施就绪
kubectl wait --for=condition=ready pod -l app=redis -n nushungry --timeout=180s
kubectl wait --for=condition=ready pod -l app=rabbitmq -n nushungry --timeout=180s

# 7. 部署微服务
kubectl apply -f services/

# 等待所有微服务就绪（约 2-3 分钟）
kubectl wait --for=condition=ready pod -l component=microservice -n nushungry --timeout=300s

# 8. 部署 Ingress（可选，用于外部访问）
kubectl apply -f ingress/

# 9. 查看所有资源
kubectl get all -n nushungry
```

## 🔍 验证部署

### 查看所有 Pod 状态

```bash
kubectl get pods -n nushungry
```

期望输出：所有 Pod 的 STATUS 应为 `Running`，READY 应为 `1/1`

### 查看服务

```bash
kubectl get services -n nushungry
```

### 查看日志

```bash
# 查看特定服务的日志
kubectl logs -f deployment/gateway-service -n nushungry

# 查看最近 100 行日志
kubectl logs --tail=100 deployment/admin-service -n nushungry

# 查看所有微服务的日志
kubectl logs -f -l component=microservice -n nushungry
```

### 端口转发（本地访问）

```bash
# 转发 Gateway Service
kubectl port-forward service/gateway-service 8080:8080 -n nushungry

# 转发 Eureka Dashboard
kubectl port-forward service/eureka-server 8761:8761 -n nushungry

# 转发 RabbitMQ 管理界面
kubectl port-forward service/rabbitmq 15672:15672 -n nushungry
```

然后在浏览器访问：
- API Gateway: http://localhost:8080
- Eureka: http://localhost:8761
- RabbitMQ: http://localhost:15672

### 健康检查

```bash
# 在 Pod 内执行健康检查
kubectl exec -it deployment/gateway-service -n nushungry -- curl http://localhost:8080/actuator/health

# 或通过端口转发后本地检查
curl http://localhost:8080/actuator/health
```

## 🔧 常用运维命令

### 扩缩容

```bash
# 手动扩容
kubectl scale deployment/cafeteria-service --replicas=3 -n nushungry

# 查看副本数
kubectl get deployment cafeteria-service -n nushungry
```

### 滚动更新

```bash
# 更新镜像
kubectl set image deployment/gateway-service gateway-service=your-registry/gateway-service:v2.0 -n nushungry

# 查看更新状态
kubectl rollout status deployment/gateway-service -n nushungry

# 查看更新历史
kubectl rollout history deployment/gateway-service -n nushungry

# 回滚到上一版本
kubectl rollout undo deployment/gateway-service -n nushungry

# 回滚到指定版本
kubectl rollout undo deployment/gateway-service --to-revision=2 -n nushungry
```

### 重启服务

```bash
# 重启单个 Pod
kubectl delete pod <pod-name> -n nushungry

# 重启整个 Deployment
kubectl rollout restart deployment/gateway-service -n nushungry

# 重启所有微服务
kubectl rollout restart deployment -l component=microservice -n nushungry
```

### 调试

```bash
# 进入 Pod 内部
kubectl exec -it deployment/gateway-service -n nushungry -- /bin/bash

# 查看 Pod 详细信息
kubectl describe pod <pod-name> -n nushungry

# 查看事件
kubectl get events -n nushungry --sort-by='.lastTimestamp'

# 查看资源使用情况
kubectl top nodes
kubectl top pods -n nushungry
```

## 📊 监控

### 查看资源配额

```bash
kubectl describe resourcequota -n nushungry
```

### 查看持久化卷

```bash
kubectl get pv
kubectl get pvc -n nushungry
```

### 查看 Ingress

```bash
kubectl get ingress -n nushungry
kubectl describe ingress nushungry-ingress -n nushungry
```

## 🗑️ 卸载

### 删除所有资源（保留数据）

```bash
kubectl delete -f ingress/
kubectl delete -f services/
kubectl delete -f infrastructure/
kubectl delete -f databases/
kubectl delete -f configmaps/
kubectl delete -f secrets/
kubectl delete -f namespaces/
```

### 删除所有资源（包括数据）

```bash
# ⚠️ 警告：这会删除所有数据！
kubectl delete namespace nushungry

# 删除持久化卷（如果需要）
kubectl delete pv -l app=nushungry
```

## 🔒 安全最佳实践

1. **不要将 Secrets 提交到 Git**
   - 将 `k8s/secrets/` 添加到 `.gitignore`
   - 使用 kubectl 命令行或密钥管理工具（如 Vault）创建 Secrets

2. **使用 RBAC 控制访问权限**
   ```bash
   kubectl create serviceaccount nushungry-sa -n nushungry
   kubectl create rolebinding nushungry-admin --clusterrole=admin --serviceaccount=nushungry:nushungry-sa -n nushungry
   ```

3. **启用网络策略**
   - 限制 Pod 之间的通信
   - 只允许必要的网络访问

4. **定期更新镜像**
   ```bash
   kubectl set image deployment/<name> <container>=<new-image> -n nushungry
   ```

5. **使用资源限制**
   - 在 Deployment 中设置 resources.limits 和 resources.requests

## 📚 相关文档

- [Kubernetes 官方文档](https://kubernetes.io/docs/home/)
- [kubectl 命令参考](https://kubernetes.io/docs/reference/kubectl/)
- [服务器部署完整指南](../docs/SERVER_DEPLOYMENT_GUIDE.md)
- [快速开始指南](../QUICKSTART_GUIDE.md)

## ❓ 常见问题

### 1. Pod 无法启动

```bash
# 查看详细错误信息
kubectl describe pod <pod-name> -n nushungry

# 常见原因：
# - 镜像拉取失败（ImagePullBackOff）
# - 资源不足（Pending）
# - 配置错误（CrashLoopBackOff）
```

### 2. 服务无法访问

```bash
# 检查 Service
kubectl get svc -n nushungry

# 检查端点
kubectl get endpoints -n nushungry

# 测试 Service
kubectl run test-pod --image=curlimages/curl -it --rm -n nushungry -- curl http://gateway-service:8080/actuator/health
```

### 3. 数据持久化失败

```bash
# 检查 PVC 状态
kubectl get pvc -n nushungry

# 检查 PV 状态
kubectl get pv

# 查看存储类
kubectl get storageclass
```

## 🎯 生产环境建议

1. **使用 Helm 管理部署**
   - 简化配置管理
   - 支持版本控制和回滚

2. **配置自动扩缩容（HPA）**
   ```bash
   kubectl autoscale deployment gateway-service --cpu-percent=50 --min=2 --max=10 -n nushungry
   ```

3. **使用 cert-manager 自动管理 HTTPS 证书**

4. **配置日志聚合（ELK Stack）**

5. **配置监控告警（Prometheus + Grafana + Alertmanager）**

祝部署顺利！🚀
