# 🚀 微服务新手快速开始指南

欢迎！这份指南将帮助你在 **10 分钟内**启动完整的 NUSHungry 微服务系统。

---

## 📋 前提条件

### 必须安装（二选一）

**选项 A：Docker Desktop（推荐新手）**
- Windows/Mac: 下载并安装 [Docker Desktop](https://www.docker.com/products/docker-desktop/)
- 安装后验证：
  ```bash
  docker --version          # 应显示版本号如 Docker version 24.0.7
  docker-compose --version  # 应显示版本号如 Docker Compose version v2.23.0
  ```

**选项 B：本地开发环境（适合开发者）**
- JDK 17+
- Maven 3.8+
- PostgreSQL 15+
- MongoDB 6+
- Redis 7+
- RabbitMQ 3.12+

---

## 🎯 快速启动（推荐：Docker 方式）

### 第 1 步：克隆项目并进入目录

```bash
git clone <your-repo-url>
cd nushungry-Backend
```

### 第 2 步：一键启动所有服务

```bash
# Windows 用户
.\scripts\start-all-services.bat

# Linux/Mac 用户
chmod +x scripts/start-all-services.sh
./scripts/start-all-services.sh
```

**预计启动时间**：2-3 分钟（首次启动需下载 Docker 镜像，约 5-10 分钟）

### 第 3 步：验证服务状态

```bash
# Windows
.\verify-services.bat

# Linux/Mac
chmod +x verify-services.sh
./verify-services.sh
```

看到所有服务显示 ✅ 表示启动成功！

---

## 🌐 访问系统

启动成功后，打开浏览器访问以下地址：

| 服务 | URL | 说明 | 默认账号 |
|------|-----|------|----------|
| **🔌 API Gateway** | http://localhost:8080 | 统一 API 入口 | - |
| **📝 Swagger API 文档** | http://localhost:8080/swagger-ui.html | 在线测试所有 API | - |
| **📊 Eureka Dashboard** | http://localhost:8761 | 查看服务注册状态 | eureka / eureka |
| **🐰 RabbitMQ 管理** | http://localhost:15672 | 消息队列监控 | guest / guest |
| **🔍 Zipkin 追踪** | http://localhost:9411 | 分布式请求追踪 | - |

---

## 🧪 快速测试 API

### 方式 A：使用 Swagger UI（最简单）

1. 访问 http://localhost:8080/swagger-ui.html
2. 展开任意 API 端点（如 `GET /api/cafeterias`）
3. 点击 **"Try it out"**
4. 点击 **"Execute"** 查看结果

### 方式 B：使用命令行（curl）

```bash
# 1. 查询所有食堂
curl http://localhost:8080/api/cafeterias

# 2. 管理员登录获取 Token
curl -X POST http://localhost:8080/api/admin/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'

# 3. 使用 Token 访问受保护的 API
curl http://localhost:8080/api/admin/dashboard/stats \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"
```

更多测试示例请查看 [API_TEST_EXAMPLES.md](./API_TEST_EXAMPLES.md)

---

## 📊 监控和日志

### 查看服务日志

```bash
# 查看所有服务日志
docker-compose logs -f

# 查看特定服务日志
docker-compose logs -f gateway-service
docker-compose logs -f admin-service
docker-compose logs -f cafeteria-service
```

### 查看服务注册状态

访问 **Eureka Dashboard**：http://localhost:8761

你应该看到以下服务已注册：
- ADMIN-SERVICE
- CAFETERIA-SERVICE
- REVIEW-SERVICE
- MEDIA-SERVICE
- PREFERENCE-SERVICE
- GATEWAY-SERVICE

### 查看分布式追踪

访问 **Zipkin**：http://localhost:9411

1. 点击 **"Run Query"** 查看最近的请求
2. 点击任意 Trace 查看完整的调用链路
3. 可以看到请求在各个微服务之间的流转过程

---

## 🛑 停止和清理

### 停止所有服务

```bash
# Windows
.\scripts\stop-all-services.bat

# Linux/Mac
chmod +x scripts/stop-all-services.sh
./scripts/stop-all-services.sh

# 或者使用 Docker Compose 命令
docker-compose down
```

### 清理所有数据（包括数据库）

```bash
# ⚠️ 警告：这会删除所有数据！
docker-compose down -v

# 删除所有镜像（释放磁盘空间）
docker system prune -a
```

---

## ❓ 常见问题

### 1. 端口被占用

**错误信息**：`Bind for 0.0.0.0:8080 failed: port is already allocated`

**解决方法**：
```bash
# Windows：查找占用端口的进程
netstat -ano | findstr :8080
taskkill /PID <PID> /F

# Linux/Mac：查找并杀死进程
lsof -i :8080
kill -9 <PID>

# 或者修改 docker-compose.yml 中的端口映射
```

### 2. 服务启动失败

**排查步骤**：

1. **查看服务日志**：
   ```bash
   docker-compose logs <service-name>
   ```

2. **检查依赖服务是否就绪**：
   ```bash
   docker-compose ps
   ```
   所有服务的 `State` 应该是 `Up (healthy)`

3. **重启单个服务**：
   ```bash
   docker-compose restart <service-name>
   ```

### 3. 数据库连接失败

**检查数据库是否启动**：
```bash
docker-compose ps postgres mongodb
```

**查看数据库日志**：
```bash
docker-compose logs postgres
docker-compose logs mongodb
```

### 4. 内存不足

Docker Desktop 默认内存限制可能不够，建议调整：
- Windows/Mac：打开 Docker Desktop → Settings → Resources
- 推荐配置：
  - Memory: 8GB+
  - CPUs: 4+
  - Swap: 2GB+

### 5. 服务无法互相访问

**检查网络**：
```bash
docker network ls
docker network inspect nushungry-Backend_nushungry-network
```

**重建网络**：
```bash
docker-compose down
docker-compose up -d
```

---

## 📚 下一步学习

### 理解微服务架构

1. **阅读架构文档**：[docs/ARCHITECTURE.md](./docs/ARCHITECTURE.md)
2. **查看进度文档**：[PROGRESS.md](./PROGRESS.md)
3. **学习开发指南**：[docs/DEVELOPMENT.md](./docs/DEVELOPMENT.md)

### 微服务间通信

- **同步通信**：通过 API Gateway 调用其他服务（REST API）
- **异步通信**：通过 RabbitMQ 发送事件消息
- **服务发现**：所有服务自动注册到 Eureka

### 监控和调试

- **日志聚合**：ELK Stack（Elasticsearch + Logstash + Kibana）
- **指标监控**：Prometheus + Grafana
- **分布式追踪**：Zipkin

### 本地开发

如果你想在本地修改代码并调试：

```bash
# 1. 只启动基础设施
docker-compose up -d postgres mongodb redis rabbitmq minio zipkin eureka-server

# 2. 在 IDE 中启动单个服务进行调试
# 例如在 IntelliJ IDEA 中右键点击 AdminServiceApplication.java → Run

# 3. 确保 application.properties 中的数据库连接指向 localhost
```

---

## 🎉 成功启动检查清单

- [ ] 所有 Docker 容器状态为 `Up (healthy)`
- [ ] Eureka Dashboard 显示所有服务已注册
- [ ] 可以访问 Swagger UI 并看到所有 API
- [ ] 可以成功调用 `GET /api/cafeterias` 获取数据
- [ ] 可以登录管理员账号并获取 JWT Token
- [ ] RabbitMQ 管理界面可以访问
- [ ] Zipkin 可以看到请求追踪信息

**全部完成？恭喜你成功启动了完整的微服务系统！🎊**

---

## 📞 获取帮助

- **查看详细文档**：[docs/](./docs/) 目录下有完整的开发和运维文档
- **查看 API 测试示例**：[API_TEST_EXAMPLES.md](./API_TEST_EXAMPLES.md)
- **查看项目进度**：[PROGRESS.md](./PROGRESS.md)

祝你学习愉快！🚀
