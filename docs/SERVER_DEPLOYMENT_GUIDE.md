# 🖥️ 服务器部署完整指南

本指南介绍如何在全新的 Linux 服务器上部署 NUSHungry 微服务系统。

---

## 📋 目录

1. [服务器要求](#服务器要求)
2. [方式 A：Docker Compose 部署（推荐新手）](#方式-a-docker-compose-部署)
3. [方式 B：Kubernetes 部署（推荐生产环境）](#方式-b-kubernetes-部署)
4. [安全配置](#安全配置)
5. [监控和运维](#监控和运维)

---

## 服务器要求

### 最低配置（单服务器部署）

```yaml
CPU: 4 核心
内存: 8GB RAM
磁盘: 50GB SSD
操作系统: Ubuntu 22.04 LTS / CentOS 8 / RHEL 8
网络: 公网 IP + 开放端口 80, 443, 8080
```

### 推荐配置（生产环境）

```yaml
CPU: 8 核心+
内存: 16GB RAM+
磁盘: 100GB SSD+
操作系统: Ubuntu 22.04 LTS
网络: 负载均衡器 + 多台服务器
```

### Kubernetes 集群配置

```yaml
主节点（Master）: 2 核 / 4GB / 20GB SSD
工作节点（Worker）: 4 核 / 8GB / 50GB SSD（至少 3 台）
总计: 至少 4 台服务器
```

---

## 方式 A: Docker Compose 部署

### 第 1 步：安装必要软件

#### 1.1 安装 Docker

```bash
# Ubuntu/Debian
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh
sudo usermod -aG docker $USER

# 重新登录以应用组权限变更
exit
# 重新登录后验证
docker --version
```

#### 1.2 安装 Docker Compose

```bash
# 安装最新版 Docker Compose
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose

# 验证安装
docker-compose --version
```

#### 1.3 安装其他必要工具

```bash
# Git
sudo apt update
sudo apt install -y git curl wget vim

# 防火墙配置（可选，根据云服务商而定）
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw allow 8080/tcp
sudo ufw allow 8761/tcp  # Eureka Dashboard
sudo ufw enable
```

### 第 2 步：克隆项目

```bash
# 创建项目目录
mkdir -p /opt/nushungry
cd /opt/nushungry

# 克隆代码（替换为你的仓库地址）
git clone <your-repo-url> .

# 进入后端目录
cd nushungry-Backend
```

### 第 3 步：配置环境变量

```bash
# 创建生产环境配置文件
cp .env.example .env

# 编辑配置（⚠️ 务必修改默认密码！）
vim .env
```

**`.env` 文件示例（生产环境）：**

```bash
# Spring Profile
SPRING_PROFILES_ACTIVE=prod

# PostgreSQL
POSTGRES_USER=nushungry_user
POSTGRES_PASSWORD=<强密码-请修改>
POSTGRES_DB=nushungry

# MongoDB
MONGO_USER=nushungry_mongo
MONGO_PASSWORD=<强密码-请修改>

# RabbitMQ
RABBITMQ_USER=nushungry_mq
RABBITMQ_PASSWORD=<强密码-请修改>

# MinIO
MINIO_USER=nushungry_minio
MINIO_PASSWORD=<强密码-请修改>

# Eureka
EUREKA_USERNAME=eureka_admin
EUREKA_PASSWORD=<强密码-请修改>

# JWT Secret（⚠️ 必须修改为随机字符串）
JWT_SECRET=<生成一个至少32字符的随机字符串>

# Redis（可选）
REDIS_PASSWORD=<强密码-请修改>
```

**生成强密码的方法：**

```bash
# 生成 32 字符随机密码
openssl rand -base64 32

# 或使用 UUID
uuidgen
```

### 第 4 步：构建和启动服务

```bash
# 构建所有服务镜像（首次需要 10-20 分钟）
docker-compose build

# 启动所有服务
docker-compose up -d

# 查看服务状态
docker-compose ps

# 查看日志
docker-compose logs -f
```

### 第 5 步：验证部署

```bash
# 检查所有服务健康状态
curl http://localhost:8080/actuator/health

# 查看 Eureka 注册的服务
curl http://<EUREKA_USERNAME>:<EUREKA_PASSWORD>@localhost:8761/eureka/apps

# 测试 API
curl http://localhost:8080/api/cafeterias
```

### 第 6 步：配置反向代理（Nginx）

```bash
# 安装 Nginx
sudo apt install -y nginx

# 创建配置文件
sudo vim /etc/nginx/sites-available/nushungry
```

**Nginx 配置示例：**

```nginx
upstream nushungry_backend {
    server localhost:8080;
}

server {
    listen 80;
    server_name your-domain.com;  # 替换为你的域名

    # API 代理
    location /api/ {
        proxy_pass http://nushungry_backend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # Swagger UI
    location /swagger-ui/ {
        proxy_pass http://nushungry_backend;
    }

    # 静态文件
    location /uploads/ {
        alias /opt/nushungry/nushungry-Backend/media_uploads/;
        expires 30d;
    }

    # 前端（如果有）
    location / {
        root /var/www/nushungry-frontend/dist;
        try_files $uri $uri/ /index.html;
    }
}
```

```bash
# 启用配置
sudo ln -s /etc/nginx/sites-available/nushungry /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl reload nginx
```

### 第 7 步：配置 HTTPS（Let's Encrypt）

```bash
# 安装 Certbot
sudo apt install -y certbot python3-certbot-nginx

# 自动配置 HTTPS
sudo certbot --nginx -d your-domain.com

# 测试自动续期
sudo certbot renew --dry-run
```

---

## 方式 B: Kubernetes 部署

### Kubernetes 简介

Kubernetes (K8s) 是一个**容器编排平台**，用于自动化部署、扩展和管理容器化应用。

**核心概念：**
- **Pod**：最小部署单元，包含一个或多个容器
- **Deployment**：管理 Pod 的副本数量和更新策略
- **Service**：为 Pod 提供稳定的网络访问入口
- **Ingress**：管理外部访问集群内服务的路由规则

### 第 1 步：安装 Kubernetes

#### 选项 A：使用 Minikube（单机测试）

```bash
# 安装 Minikube
curl -LO https://storage.googleapis.com/minikube/releases/latest/minikube-linux-amd64
sudo install minikube-linux-amd64 /usr/local/bin/minikube

# 启动 Minikube
minikube start --cpus=4 --memory=8192 --driver=docker

# 验证
kubectl cluster-info
kubectl get nodes
```

#### 选项 B：使用 K3s（轻量级生产环境）

```bash
# 安装 K3s（在主节点上）
curl -sfL https://get.k3s.io | sh -

# 验证
sudo k3s kubectl get nodes

# 配置 kubectl（可选）
mkdir -p ~/.kube
sudo cp /etc/rancher/k3s/k3s.yaml ~/.kube/config
sudo chown $USER ~/.kube/config

# 获取 Worker 节点加入命令
sudo cat /var/lib/rancher/k3s/server/node-token

# 在 Worker 节点上执行（替换 <TOKEN> 和 <MASTER_IP>）
curl -sfL https://get.k3s.io | K3S_URL=https://<MASTER_IP>:6443 K3S_TOKEN=<TOKEN> sh -
```

#### 选项 C：使用云服务商的 Kubernetes（推荐生产）

```bash
# AWS EKS
eksctl create cluster --name nushungry --region us-west-2 --nodes 3

# Google GKE
gcloud container clusters create nushungry --num-nodes=3

# Azure AKS
az aks create --resource-group myResourceGroup --name nushungry --node-count 3
```

### 第 2 步：安装 kubectl

```bash
# 安装 kubectl
curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"
sudo install -o root -g root -m 0755 kubectl /usr/local/bin/kubectl

# 验证
kubectl version --client
```

### 第 3 步：创建 Kubernetes 配置文件

我已经为项目准备了完整的 K8s 配置文件，位于 `k8s/` 目录。让我们查看结构：

```bash
k8s/
├── namespaces/
│   └── nushungry-namespace.yaml
├── configmaps/
│   └── app-config.yaml
├── secrets/
│   └── app-secrets.yaml
├── databases/
│   ├── postgres-deployment.yaml
│   ├── postgres-service.yaml
│   ├── mongodb-deployment.yaml
│   └── mongodb-service.yaml
├── infrastructure/
│   ├── redis-deployment.yaml
│   ├── rabbitmq-deployment.yaml
│   ├── minio-deployment.yaml
│   └── zipkin-deployment.yaml
├── services/
│   ├── eureka-server.yaml
│   ├── admin-service.yaml
│   ├── cafeteria-service.yaml
│   ├── review-service.yaml
│   ├── media-service.yaml
│   ├── preference-service.yaml
│   └── gateway-service.yaml
└── ingress/
    └── ingress.yaml
```

### 第 4 步：部署到 Kubernetes

```bash
# 1. 创建命名空间
kubectl apply -f k8s/namespaces/

# 2. 创建 Secrets（⚠️ 先编辑 secrets 文件，替换为真实密码）
kubectl apply -f k8s/secrets/

# 3. 创建 ConfigMaps
kubectl apply -f k8s/configmaps/

# 4. 部署数据库
kubectl apply -f k8s/databases/

# 等待数据库就绪
kubectl wait --for=condition=ready pod -l app=postgres -n nushungry --timeout=300s
kubectl wait --for=condition=ready pod -l app=mongodb -n nushungry --timeout=300s

# 5. 部署基础设施
kubectl apply -f k8s/infrastructure/

# 6. 部署微服务
kubectl apply -f k8s/services/

# 7. 部署 Ingress
kubectl apply -f k8s/ingress/

# 8. 查看所有资源
kubectl get all -n nushungry
```

### 第 5 步：验证部署

```bash
# 查看所有 Pod
kubectl get pods -n nushungry

# 查看服务
kubectl get services -n nushungry

# 查看日志
kubectl logs -f deployment/gateway-service -n nushungry

# 端口转发（本地测试）
kubectl port-forward service/gateway-service 8080:8080 -n nushungry

# 查看 Eureka Dashboard
kubectl port-forward service/eureka-server 8761:8761 -n nushungry
# 然后访问 http://localhost:8761
```

### 第 6 步：配置 Ingress（外部访问）

```bash
# 安装 Nginx Ingress Controller
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.8.1/deploy/static/provider/cloud/deploy.yaml

# 获取 Ingress 外部 IP
kubectl get ingress -n nushungry

# 配置 DNS 解析
# 将你的域名指向 Ingress 的外部 IP
```

---

## 🔒 安全配置

### 1. 修改所有默认密码

```bash
# 使用强密码生成器
openssl rand -base64 32

# 在 .env 或 k8s/secrets/ 中更新所有密码
```

### 2. 配置防火墙

```bash
# 只开放必要端口
sudo ufw default deny incoming
sudo ufw default allow outgoing
sudo ufw allow ssh
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw enable
```

### 3. 使用 HTTPS

```bash
# Docker Compose 部署：使用 Let's Encrypt + Nginx
# Kubernetes 部署：使用 cert-manager

# 安装 cert-manager（K8s）
kubectl apply -f https://github.com/cert-manager/cert-manager/releases/download/v1.13.0/cert-manager.yaml
```

### 4. 定期更新

```bash
# 更新系统
sudo apt update && sudo apt upgrade -y

# 更新 Docker 镜像
docker-compose pull
docker-compose up -d

# 更新 K8s 部署
kubectl set image deployment/<deployment-name> <container-name>=<new-image> -n nushungry
```

---

## 📊 监控和运维

### 1. 日志管理

```bash
# Docker Compose
docker-compose logs -f --tail=100 gateway-service

# Kubernetes
kubectl logs -f deployment/gateway-service -n nushungry --tail=100
```

### 2. 资源监控

```bash
# Docker
docker stats

# Kubernetes
kubectl top nodes
kubectl top pods -n nushungry
```

### 3. 健康检查

```bash
# 自动化健康检查脚本
while true; do
  curl -f http://localhost:8080/actuator/health || echo "Gateway is DOWN!"
  sleep 60
done
```

### 4. 备份数据

```bash
# PostgreSQL 备份
docker exec nushungry-postgres pg_dumpall -U postgres > backup_$(date +%Y%m%d).sql

# MongoDB 备份
docker exec nushungry-mongodb mongodump --archive > backup_$(date +%Y%m%d).archive

# Kubernetes 备份
kubectl exec -n nushungry postgres-0 -- pg_dumpall -U postgres > backup.sql
```

---

## 🚀 自动化部署（CI/CD）

### GitHub Actions 示例

```yaml
# .github/workflows/deploy.yml
name: Deploy to Production

on:
  push:
    branches: [ main ]

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: Build Docker images
        run: docker-compose build

      - name: Push to Registry
        run: |
          echo ${{ secrets.DOCKER_PASSWORD }} | docker login -u ${{ secrets.DOCKER_USERNAME }} --password-stdin
          docker-compose push

      - name: Deploy to K8s
        run: |
          kubectl set image deployment/gateway-service gateway=myregistry/gateway:${{ github.sha }} -n nushungry
```

---

## ❓ 常见问题

### 1. 服务无法启动

```bash
# 查看详细日志
docker-compose logs <service-name>
kubectl describe pod <pod-name> -n nushungry

# 检查资源限制
docker stats
kubectl top pods -n nushungry
```

### 2. 数据库连接失败

```bash
# 检查网络连接
docker-compose exec admin-service ping postgres
kubectl exec -it <pod-name> -n nushungry -- ping postgres-service

# 检查密码是否正确
docker-compose logs postgres | grep password
```

### 3. Out of Memory

```bash
# 增加 JVM 堆内存（在 Dockerfile 或 deployment.yaml 中）
ENV JAVA_OPTS="-Xmx2g -Xms1g"

# 或调整容器内存限制
resources:
  limits:
    memory: "2Gi"
  requests:
    memory: "1Gi"
```

---

## 📚 相关文档

- [Docker Compose 官方文档](https://docs.docker.com/compose/)
- [Kubernetes 官方文档](https://kubernetes.io/docs/home/)
- [K3s 文档](https://docs.k3s.io/)
- [Nginx 配置指南](https://nginx.org/en/docs/)
- [Let's Encrypt 证书](https://letsencrypt.org/getting-started/)

---

## 📞 获取帮助

如有问题，请查看：
- [QUICKSTART_GUIDE.md](../QUICKSTART_GUIDE.md) - 快速开始
- [PROGRESS.md](../PROGRESS.md) - 项目进度
- [API_TEST_EXAMPLES.md](../API_TEST_EXAMPLES.md) - API 测试

祝部署顺利！🎉
