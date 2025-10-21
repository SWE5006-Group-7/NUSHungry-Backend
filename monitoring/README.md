# NUSHungry 监控系统 (Prometheus + Grafana)

## 📊 概述

本目录包含 NUSHungry 微服务架构的完整监控解决方案，基于 Prometheus 和 Grafana。

### 监控组件

- **Prometheus** (v2.48.0): 时序数据库和监控系统，负责收集和存储指标
- **Grafana** (v10.2.2): 可视化和分析平台，提供丰富的仪表盘

### 监控的服务

- Gateway Service (8080)
- Eureka Server (8761)
- Config Server (8888)
- Admin Service (8082)
- Cafeteria Service (8083)
- Review Service (8084)
- Media Service (8085)
- Preference Service (8086)

---

## 🚀 快速开始

### 前置条件

1. Docker 和 Docker Compose 已安装
2. 端口 9090 (Prometheus) 和 3000 (Grafana) 未被占用
3. 微服务已运行（可选，但建议运行以查看实时指标）

### 启动监控栈

**Linux/Mac:**
```bash
cd monitoring
chmod +x start-monitoring.sh
./start-monitoring.sh
```

**Windows:**
```cmd
cd monitoring
start-monitoring.bat
```

### 访问监控系统

- **Prometheus UI**: http://localhost:9090
- **Grafana Dashboard**: http://localhost:3000
  - 默认用户名: `admin`
  - 默认密码: `admin` (首次登录会提示修改)

### 停止监控栈

**Linux/Mac:**
```bash
./stop-monitoring.sh
```

**Windows:**
```cmd
stop-monitoring.bat
```

---

## 📁 目录结构

```
monitoring/
├── docker-compose.yml              # Docker Compose 配置
├── prometheus/
│   ├── prometheus.yml              # Prometheus 主配置
│   └── alerts/
│       └── service-alerts.yml      # 告警规则定义
├── grafana/
│   ├── provisioning/
│   │   ├── datasources/
│   │   │   └── prometheus.yml      # Prometheus 数据源配置
│   │   └── dashboards/
│   │       └── dashboards.yml      # 仪表盘自动加载配置
│   └── dashboards/
│       └── nushungry-overview.json # 概览仪表盘
├── start-monitoring.sh/bat         # 启动脚本
├── stop-monitoring.sh/bat          # 停止脚本
└── README.md                       # 本文档
```

---

## 📈 指标说明

### 服务健康指标

- `up`: 服务是否运行 (1=运行, 0=停止)
- `jvm_memory_used_bytes`: JVM 内存使用量
- `jvm_threads_live_threads`: 活跃线程数
- `process_cpu_usage`: CPU 使用率

### HTTP 指标

- `http_server_requests_seconds_count`: 请求总数
- `http_server_requests_seconds_sum`: 请求耗时总和
- `http_server_requests_seconds_bucket`: 响应时间分布（用于计算百分位数）

### 数据库连接池指标 (HikariCP)

- `hikaricp_connections_active`: 活跃连接数
- `hikaricp_connections_idle`: 空闲连接数
- `hikaricp_connections_max`: 最大连接数

---

## 🚨 告警规则

系统包含以下告警规则（定义在 `prometheus/alerts/service-alerts.yml`）：

### 可用性告警
- **ServiceDown**: 服务停止超过 1 分钟
- **ServiceInstanceDown**: 服务实例停止超过 2 分钟

### 错误率告警
- **HighErrorRate**: 5xx 错误率超过 5%，持续 5 分钟
- **CriticalErrorRate**: 5xx 错误率超过 10%，持续 2 分钟

### 性能告警
- **HighResponseTime**: P95 响应时间超过 2 秒，持续 5 分钟
- **SlowResponseTime**: P99 响应时间超过 5 秒，持续 3 分钟

### 资源使用告警
- **HighMemoryUsage**: 堆内存使用率超过 85%，持续 5 分钟
- **CriticalMemoryUsage**: 堆内存使用率超过 95%，持续 2 分钟
- **HighCPUUsage**: CPU 使用率超过 85%，持续 5 分钟

### 其他告警
- **ThreadPoolExhaustion**: 线程池使用率超过 90%
- **DatabaseConnectionPoolLow**: 数据库连接池使用率超过 85%
- **UnusualTrafficSpike**: 请求量激增（3倍于正常水平）
- **TrafficDrop**: 请求量大幅下降（低于正常水平30%）

---

## 📊 Grafana 仪表盘

### NUSHungry Microservices Overview

默认提供的综合监控仪表盘，包含以下面板：

1. **Service Health Status**: 所有服务的健康状态概览
2. **Request Rate**: 每个服务的请求速率（req/s）
3. **Response Time (P95)**: 95th 百分位响应时间
4. **Error Rate (5xx)**: 服务器错误率
5. **JVM Heap Memory Usage**: JVM 堆内存使用率
6. **CPU Usage**: CPU 使用率
7. **JVM Threads**: JVM 线程数

### 访问仪表盘

1. 打开 Grafana: http://localhost:3000
2. 登录（admin/admin）
3. 点击左侧菜单 "Dashboards"
4. 选择 "NUSHungry Microservices Overview"

---

## 🔧 配置说明

### 修改 Prometheus 抓取配置

编辑 `prometheus/prometheus.yml`:

```yaml
scrape_configs:
  - job_name: 'your-service'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['your-service:port']
```

重启 Prometheus 使配置生效：
```bash
docker-compose restart prometheus
```

### 添加新的告警规则

1. 编辑 `prometheus/alerts/service-alerts.yml`
2. 添加新的告警规则
3. 重启 Prometheus: `docker-compose restart prometheus`
4. 验证规则: 访问 http://localhost:9090/alerts

### 创建自定义 Grafana 仪表盘

1. 在 Grafana UI 中创建仪表盘
2. 导出为 JSON: Dashboard Settings → JSON Model
3. 保存到 `grafana/dashboards/your-dashboard.json`
4. 重启 Grafana: `docker-compose restart grafana`

---

## 🔍 常见查询示例

### Prometheus 查询 (PromQL)

在 Prometheus UI (http://localhost:9090) 中执行：

#### 服务可用性
```promql
# 查看所有服务的运行状态
up

# 查看特定服务的状态
up{job="gateway-service"}
```

#### 请求率
```promql
# 每个服务的请求速率 (req/s)
sum(rate(http_server_requests_seconds_count[5m])) by (application)

# 特定服务的请求速率
rate(http_server_requests_seconds_count{application="gateway-service"}[5m])
```

#### 响应时间
```promql
# P95 响应时间
histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket[5m])) by (application, le))

# P99 响应时间
histogram_quantile(0.99, sum(rate(http_server_requests_seconds_bucket[5m])) by (application, le))
```

#### 错误率
```promql
# 5xx 错误率
sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m])) by (application)
/
sum(rate(http_server_requests_seconds_count[5m])) by (application)
```

#### 内存使用
```promql
# 堆内存使用率
jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"}
```

---

## 🛠️ 故障排查

### Prometheus 无法抓取服务指标

**检查步骤:**

1. 确认服务已运行: `docker ps`
2. 确认服务暴露了 Prometheus 端点:
   ```bash
   curl http://localhost:8080/actuator/prometheus
   ```
3. 检查 Prometheus targets: http://localhost:9090/targets
4. 查看 Prometheus 日志:
   ```bash
   docker logs nushungry-prometheus
   ```

### Grafana 无数据显示

**检查步骤:**

1. 确认 Prometheus 数据源配置正确:
   - Grafana → Configuration → Data Sources → Prometheus
   - 测试连接: "Save & Test"
2. 确认时间范围选择正确（右上角时间选择器）
3. 确认服务已运行并且有流量
4. 在 Prometheus UI 中验证查询是否有数据

### 容器无法启动

**检查步骤:**

1. 查看容器日志:
   ```bash
   docker-compose logs prometheus
   docker-compose logs grafana
   ```
2. 检查端口占用:
   ```bash
   # Linux/Mac
   lsof -i :9090
   lsof -i :3000

   # Windows
   netstat -ano | findstr :9090
   netstat -ano | findstr :3000
   ```
3. 检查 Docker 网络:
   ```bash
   docker network ls
   docker network inspect nushungry
   ```

---

## 📚 进一步学习

### Prometheus
- [官方文档](https://prometheus.io/docs/)
- [PromQL 查询语法](https://prometheus.io/docs/prometheus/latest/querying/basics/)
- [告警规则配置](https://prometheus.io/docs/prometheus/latest/configuration/alerting_rules/)

### Grafana
- [官方文档](https://grafana.com/docs/)
- [仪表盘最佳实践](https://grafana.com/docs/grafana/latest/dashboards/best-practices/)
- [社区仪表盘](https://grafana.com/grafana/dashboards/)

### Spring Boot Actuator & Micrometer
- [Spring Boot Actuator](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)
- [Micrometer Prometheus](https://micrometer.io/docs/registry/prometheus)

---

## 📝 最佳实践

1. **定期查看仪表盘**: 至少每天检查一次关键指标
2. **设置告警通知**: 配置 AlertManager 发送邮件/Slack 通知
3. **保留历史数据**: 根据需要调整 Prometheus 数据保留期（默认 30 天）
4. **性能优化**:
   - 避免过于复杂的 PromQL 查询
   - 使用合适的抓取间隔（默认 15s）
   - 定期清理不需要的指标
5. **安全性**:
   - 生产环境中修改 Grafana 默认密码
   - 考虑为 Prometheus 和 Grafana 添加认证

---

## 🤝 贡献

如果发现问题或有改进建议，请联系开发团队。

---

## 📄 许可证

本项目为 NUSHungry 微服务架构的一部分。
