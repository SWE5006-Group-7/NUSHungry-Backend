# NUSHungry 微服务监控系统

## 📋 目录

- [概述](#概述)
- [架构设计](#架构设计)
- [快速开始](#快速开始)
- [Prometheus配置](#prometheus配置)
- [Grafana仪表盘](#grafana仪表盘)
- [告警系统](#告警系统)
- [指标说明](#指标说明)
- [最佳实践](#最佳实践)
- [故障排查](#故障排查)

---

## 概述

NUSHungry 微服务架构采用 **Prometheus + Grafana** 构建完整的监控解决方案，实现对所有微服务的实时监控、指标收集、可视化和告警。

### 监控目标

- **服务可用性**: 实时监控服务运行状态
- **性能指标**: 跟踪响应时间、吞吐量、错误率
- **资源使用**: 监控 CPU、内存、线程池、数据库连接池
- **业务指标**: 自定义业务相关指标
- **告警通知**: 及时发现和响应系统异常

### 技术栈

- **Prometheus** v2.48.0: 时序数据库和监控系统
- **Grafana** v10.2.2: 数据可视化和分析平台
- **Micrometer**: Spring Boot 指标收集库
- **Spring Boot Actuator**: 应用监控和管理端点

---

## 架构设计

### 整体架构

```
┌─────────────────────────────────────────────────────────────┐
│                    Grafana (可视化层)                         │
│  - 仪表盘展示                                                 │
│  - 数据查询和分析                                             │
│  - 告警通知 (可选)                                           │
└────────────────────┬────────────────────────────────────────┘
                     │ HTTP查询
                     ↓
┌─────────────────────────────────────────────────────────────┐
│                 Prometheus (数据存储层)                       │
│  - 时序数据库                                                 │
│  - PromQL 查询引擎                                           │
│  - 告警规则评估                                              │
│  - 数据抓取调度                                              │
└──────┬──────┬──────┬──────┬──────┬──────┬──────┬───────────┘
       │      │      │      │      │      │      │
       │ HTTP Pull (每15秒)                     │
       ↓      ↓      ↓      ↓      ↓      ↓      ↓
┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐
│Gateway │ │ Admin  │ │Cafeteria│ │Review │ │ Media  │
│Service │ │Service │ │Service │ │Service │ │Service │
└────────┘ └────────┘ └────────┘ └────────┘ └────────┘
  :8080      :8082      :8083      :8084      :8085
    ↓          ↓          ↓          ↓          ↓
  /actuator/prometheus (Micrometer 暴露指标)

┌────────┐ ┌────────┐ ┌────────┐
│Preference│ │Eureka │ │Config │
│Service │ │Server  │ │Server  │
└────────┘ └────────┘ └────────┘
  :8086      :8761      :8888
```

### 数据流程

1. **指标收集**: 各微服务通过 Micrometer 收集 JVM、HTTP、数据库等指标
2. **指标暴露**: 通过 `/actuator/prometheus` 端点以 Prometheus 格式暴露
3. **指标抓取**: Prometheus 定期（15秒）Pull 拉取各服务的指标
4. **指标存储**: Prometheus 将指标存储在本地时序数据库（保留30天）
5. **指标查询**: Grafana 通过 PromQL 查询 Prometheus 数据
6. **数据展示**: Grafana 仪表盘实时展示指标图表
7. **告警评估**: Prometheus 持续评估告警规则并触发告警

---

## 快速开始

### 1. 启动监控栈

**前置条件:**
- Docker 和 Docker Compose 已安装
- 端口 9090 和 3000 未被占用

**启动命令:**

```bash
# Linux/Mac
cd nushungry-Backend/monitoring
chmod +x start-monitoring.sh
./start-monitoring.sh

# Windows
cd nushungry-Backend\monitoring
start-monitoring.bat
```

**预期输出:**
```
==========================================
Starting NUSHungry Monitoring Stack
==========================================

🚀 Starting Prometheus and Grafana...
⏳ Waiting for services to be healthy...
✅ Prometheus is healthy
✅ Grafana is healthy

==========================================
✅ Monitoring Stack Started Successfully
==========================================

📊 Access URLs:
   - Prometheus: http://localhost:9090
   - Grafana:    http://localhost:3000
```

### 2. 访问 Grafana

1. 打开浏览器访问: http://localhost:3000
2. 使用默认凭据登录:
   - Username: `admin`
   - Password: `admin`
3. 首次登录会提示修改密码（可跳过）
4. 进入首页后，点击 "Dashboards" → "NUSHungry Microservices Overview"

### 3. 查看指标

在 Grafana 仪表盘中可以看到:
- 所有服务的健康状态
- 请求速率和响应时间
- 错误率趋势
- JVM 内存和 CPU 使用情况

---

## Prometheus配置

### 配置文件结构

```
monitoring/prometheus/
├── prometheus.yml          # 主配置文件
└── alerts/
    └── service-alerts.yml  # 告警规则
```

### 主配置说明 (prometheus.yml)

```yaml
global:
  scrape_interval: 15s      # 全局抓取间隔
  evaluation_interval: 15s  # 告警规则评估间隔
  external_labels:
    cluster: 'nushungry'
    environment: 'dev'

scrape_configs:
  # 定义所有需要监控的服务
  - job_name: 'gateway-service'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['gateway-service:8080']
        labels:
          service: 'gateway-service'
          tier: 'gateway'
```

### 关键配置参数

| 参数 | 默认值 | 说明 |
|------|--------|------|
| `scrape_interval` | 15s | 指标抓取间隔 |
| `evaluation_interval` | 15s | 告警规则评估频率 |
| `storage.tsdb.retention.time` | 30d | 数据保留时间 |
| `metrics_path` | /metrics | 指标端点路径 |

### 添加新服务监控

在 `prometheus.yml` 中添加新的 job:

```yaml
scrape_configs:
  - job_name: 'new-service'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['new-service:port']
        labels:
          service: 'new-service'
          tier: 'business'
```

重启 Prometheus:
```bash
docker-compose restart prometheus
```

---

## Grafana仪表盘

### 默认仪表盘: NUSHungry Microservices Overview

包含以下面板:

#### 1. Service Health Status
- **类型**: Stat (状态指示器)
- **指标**: `up{job=~".*-service"}`
- **说明**: 实时显示所有服务的健康状态
- **颜色**: 绿色=运行, 红色=停止

#### 2. Request Rate (req/s)
- **类型**: Time Series (时序图)
- **指标**: `sum(rate(http_server_requests_seconds_count[5m])) by (application)`
- **说明**: 每个服务的HTTP请求速率（每秒请求数）

#### 3. Response Time (P95)
- **类型**: Time Series
- **指标**: `histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket[5m])) by (application, le))`
- **说明**: 95th百分位响应时间（95%的请求在此时间内完成）

#### 4. Error Rate (5xx)
- **类型**: Time Series
- **指标**:
  ```promql
  sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m])) by (application)
  /
  sum(rate(http_server_requests_seconds_count[5m])) by (application)
  ```
- **说明**: 服务器错误率（5xx错误占总请求的比例）

#### 5. JVM Heap Memory Usage
- **类型**: Time Series
- **指标**: `jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"}`
- **说明**: JVM堆内存使用率

#### 6. CPU Usage
- **类型**: Time Series
- **指标**: `process_cpu_usage`
- **说明**: 进程CPU使用率

#### 7. JVM Threads
- **类型**: Time Series
- **指标**: `jvm_threads_live_threads`
- **说明**: JVM活跃线程数

### 创建自定义仪表盘

1. 在 Grafana 中点击 "+" → "Dashboard"
2. 添加 Panel，选择 Prometheus 数据源
3. 输入 PromQL 查询
4. 调整可视化类型和样式
5. 保存仪表盘

**导出仪表盘:**
1. Dashboard Settings → JSON Model
2. 复制 JSON
3. 保存到 `monitoring/grafana/dashboards/your-dashboard.json`
4. 重启 Grafana 自动加载

---

## 告警系统

### 告警规则概览

所有告警规则定义在 `prometheus/alerts/service-alerts.yml`。

### 告警分类

#### 1. 服务可用性告警

| 告警名称 | 触发条件 | 持续时间 | 严重程度 |
|---------|---------|---------|---------|
| ServiceDown | `up == 0` | 1分钟 | critical |
| ServiceInstanceDown | `up{job=~".*-service"} == 0` | 2分钟 | warning |

#### 2. 错误率告警

| 告警名称 | 触发条件 | 持续时间 | 严重程度 |
|---------|---------|---------|---------|
| HighErrorRate | 5xx错误率 > 5% | 5分钟 | warning |
| CriticalErrorRate | 5xx错误率 > 10% | 2分钟 | critical |

#### 3. 性能告警

| 告警名称 | 触发条件 | 持续时间 | 严重程度 |
|---------|---------|---------|---------|
| HighResponseTime | P95响应时间 > 2秒 | 5分钟 | warning |
| SlowResponseTime | P99响应时间 > 5秒 | 3分钟 | critical |

#### 4. 资源使用告警

| 告警名称 | 触发条件 | 持续时间 | 严重程度 |
|---------|---------|---------|---------|
| HighMemoryUsage | 堆内存使用率 > 85% | 5分钟 | warning |
| CriticalMemoryUsage | 堆内存使用率 > 95% | 2分钟 | critical |
| HighCPUUsage | CPU使用率 > 85% | 5分钟 | warning |
| ThreadPoolExhaustion | 线程使用率 > 90% | 5分钟 | warning |
| DatabaseConnectionPoolLow | 数据库连接池使用率 > 85% | 5分钟 | warning |

#### 5. 流量异常告警

| 告警名称 | 触发条件 | 持续时间 | 严重程度 |
|---------|---------|---------|---------|
| UnusualTrafficSpike | 请求量为正常的3倍以上 | 5分钟 | info |
| TrafficDrop | 请求量低于正常的30% | 10分钟 | warning |

### 告警规则示例

```yaml
groups:
  - name: service_availability
    interval: 30s
    rules:
      - alert: ServiceDown
        expr: up == 0
        for: 1m
        labels:
          severity: critical
          category: availability
        annotations:
          summary: "Service {{ $labels.job }} is down"
          description: "Service {{ $labels.job }} ({{ $labels.instance }}) has been down for more than 1 minute."
```

### 查看告警状态

1. Prometheus UI: http://localhost:9090/alerts
2. Grafana: Alerting → Alert Rules

### 添加新的告警规则

1. 编辑 `prometheus/alerts/service-alerts.yml`
2. 添加新规则到对应的 group
3. 验证语法: `promtool check rules alerts/service-alerts.yml`
4. 重启 Prometheus: `docker-compose restart prometheus`

---

## 指标说明

### JVM 指标

| 指标名称 | 类型 | 说明 |
|---------|------|------|
| `jvm_memory_used_bytes` | Gauge | JVM内存使用量（字节） |
| `jvm_memory_max_bytes` | Gauge | JVM最大内存（字节） |
| `jvm_threads_live_threads` | Gauge | 活跃线程数 |
| `jvm_threads_peak_threads` | Gauge | 峰值线程数 |
| `jvm_gc_pause_seconds` | Summary | GC暂停时间 |
| `jvm_gc_memory_allocated_bytes_total` | Counter | 已分配内存总量 |

### HTTP 指标

| 指标名称 | 类型 | 说明 |
|---------|------|------|
| `http_server_requests_seconds_count` | Counter | HTTP请求总数 |
| `http_server_requests_seconds_sum` | Counter | HTTP请求耗时总和 |
| `http_server_requests_seconds_bucket` | Histogram | HTTP请求耗时分布 |

**标签:**
- `application`: 应用名称
- `uri`: 请求路径
- `method`: HTTP方法
- `status`: 响应状态码
- `outcome`: 请求结果（SUCCESS/CLIENT_ERROR/SERVER_ERROR）

### 系统指标

| 指标名称 | 类型 | 说明 |
|---------|------|------|
| `process_cpu_usage` | Gauge | 进程CPU使用率（0-1） |
| `system_cpu_usage` | Gauge | 系统CPU使用率（0-1） |
| `process_uptime_seconds` | Gauge | 进程运行时间（秒） |

### 数据库连接池指标 (HikariCP)

| 指标名称 | 类型 | 说明 |
|---------|------|------|
| `hikaricp_connections_active` | Gauge | 活跃连接数 |
| `hikaricp_connections_idle` | Gauge | 空闲连接数 |
| `hikaricp_connections_pending` | Gauge | 等待连接的线程数 |
| `hikaricp_connections_max` | Gauge | 最大连接数 |
| `hikaricp_connections_min` | Gauge | 最小连接数 |
| `hikaricp_connections_timeout_total` | Counter | 连接超时次数 |

### 自定义业务指标

可以使用 Micrometer API 添加自定义指标:

```java
@Service
public class ReviewService {

    private final Counter reviewCounter;

    public ReviewService(MeterRegistry registry) {
        this.reviewCounter = Counter.builder("reviews.created")
            .description("Total reviews created")
            .tag("service", "review-service")
            .register(registry);
    }

    public void createReview(Review review) {
        // 业务逻辑
        reviewCounter.increment();
    }
}
```

---

## 最佳实践

### 1. 指标设计

- **命名规范**: 使用小写字母和下划线，如 `http_server_requests_seconds`
- **标签使用**: 合理使用标签进行维度划分，但避免高基数标签（如user_id）
- **指标类型选择**:
  - Counter: 累计值（如请求总数）
  - Gauge: 瞬时值（如内存使用量）
  - Histogram: 分布统计（如响应时间）
  - Summary: 百分位统计

### 2. 查询优化

- **避免昂贵的查询**: 减少 `rate()`、`histogram_quantile()` 等函数的嵌套
- **合理使用时间窗口**: 根据需要选择合适的时间范围（如 `[5m]`, `[1h]`）
- **使用 recording rules**: 预计算复杂查询，提高查询效率

### 3. 告警配置

- **设置合理阈值**: 根据实际业务场景调整告警阈值
- **使用持续时间**: 避免瞬时抖动触发告警（如 `for: 5m`）
- **告警分级**:
  - `critical`: 需要立即处理
  - `warning`: 需要关注
  - `info`: 仅供参考

### 4. 存储管理

- **数据保留策略**: 根据需求调整保留时间（默认30天）
- **定期清理**: 删除不再需要的指标和标签
- **磁盘监控**: 监控 Prometheus 数据目录的磁盘使用

### 5. 安全性

- **修改默认密码**: 生产环境必须修改 Grafana 默认密码
- **访问控制**: 配置防火墙规则，限制监控端点的访问
- **HTTPS**: 生产环境使用 HTTPS 访问 Grafana
- **认证**: 为 Prometheus 和 Grafana 添加认证机制

---

## 故障排查

### 问题1: Prometheus 无法抓取服务指标

**症状:**
- Targets 页面显示服务为 DOWN
- 错误信息: "connection refused" 或 "context deadline exceeded"

**排查步骤:**

1. **检查服务是否运行:**
   ```bash
   docker ps | grep service-name
   ```

2. **检查网络连接:**
   ```bash
   # 从 Prometheus 容器内测试
   docker exec nushungry-prometheus wget -O- http://gateway-service:8080/actuator/prometheus
   ```

3. **检查端点是否暴露:**
   ```bash
   curl http://localhost:8080/actuator/prometheus
   ```

4. **检查 application.yml 配置:**
   ```yaml
   management:
     endpoints:
       web:
         exposure:
           include: health,info,metrics,prometheus
   ```

5. **检查依赖是否添加:**
   ```xml
   <dependency>
       <groupId>io.micrometer</groupId>
       <artifactId>micrometer-registry-prometheus</artifactId>
   </dependency>
   ```

### 问题2: Grafana 无数据显示

**症状:**
- 仪表盘显示 "No Data"
- 查询返回空结果

**排查步骤:**

1. **检查数据源配置:**
   - 进入 Configuration → Data Sources → Prometheus
   - 点击 "Save & Test"
   - 确保显示 "Data source is working"

2. **验证 Prometheus 有数据:**
   - 访问 http://localhost:9090
   - 执行查询: `up`
   - 确认有返回数据

3. **检查时间范围:**
   - 确保 Grafana 时间选择器范围正确
   - 尝试选择 "Last 1 hour"

4. **检查 PromQL 查询:**
   - 在 Prometheus UI 中测试查询
   - 确认查询语法正确

### 问题3: 告警未触发

**症状:**
- 即使条件满足，告警也不触发
- Alerts 页面显示 "Pending" 或 "Inactive"

**排查步骤:**

1. **检查告警规则语法:**
   ```bash
   docker exec nushungry-prometheus promtool check rules /etc/prometheus/alerts/service-alerts.yml
   ```

2. **查看告警状态:**
   - 访问 http://localhost:9090/alerts
   - 查看告警的当前状态和评估结果

3. **检查持续时间:**
   - 确认 `for` 子句设置的持续时间是否过长
   - 尝试临时降低持续时间进行测试

4. **查看 Prometheus 日志:**
   ```bash
   docker logs nushungry-prometheus | grep -i alert
   ```

### 问题4: 内存使用过高

**症状:**
- Prometheus 或 Grafana 容器内存占用持续增长
- 容器被 OOM Killer 杀死

**排查步骤:**

1. **检查 Prometheus 数据量:**
   ```bash
   # 查看时序数据库大小
   docker exec nushungry-prometheus du -sh /prometheus
   ```

2. **减少保留时间:**
   ```yaml
   # 在 docker-compose.yml 中调整
   --storage.tsdb.retention.time=15d  # 从30d减少到15d
   ```

3. **优化查询:**
   - 减少复杂的 PromQL 查询
   - 避免高基数标签

4. **增加容器内存限制:**
   ```yaml
   # docker-compose.yml
   services:
     prometheus:
       deploy:
         resources:
           limits:
             memory: 2G
   ```

### 问题5: 仪表盘加载缓慢

**症状:**
- Grafana 仪表盘打开很慢
- 查询执行时间过长

**排查步骤:**

1. **简化查询:**
   - 减少面板数量
   - 优化 PromQL 查询

2. **调整刷新间隔:**
   - 将自动刷新从 10s 调整到 30s 或更长

3. **使用 recording rules:**
   - 预计算复杂查询
   - 添加到 `prometheus.yml`:
     ```yaml
     rule_files:
       - "recording_rules.yml"
     ```

4. **增加查询超时:**
   ```yaml
   # Grafana datasource 配置
   jsonData:
     queryTimeout: "60s"
   ```

---

## 附录

### PromQL 常用查询

```promql
# 1. 服务可用性
up

# 2. 请求速率 (QPS)
rate(http_server_requests_seconds_count[5m])

# 3. 平均响应时间
rate(http_server_requests_seconds_sum[5m])
/
rate(http_server_requests_seconds_count[5m])

# 4. P95 响应时间
histogram_quantile(0.95,
  rate(http_server_requests_seconds_bucket[5m])
)

# 5. 错误率
sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m]))
/
sum(rate(http_server_requests_seconds_count[5m]))

# 6. 内存使用率
jvm_memory_used_bytes{area="heap"}
/
jvm_memory_max_bytes{area="heap"}

# 7. Top N 慢接口
topk(10,
  histogram_quantile(0.95,
    sum(rate(http_server_requests_seconds_bucket[5m])) by (uri, le)
  )
)

# 8. 按服务统计请求量
sum(rate(http_server_requests_seconds_count[5m])) by (application)
```

### 参考资源

- [Prometheus 官方文档](https://prometheus.io/docs/)
- [Grafana 官方文档](https://grafana.com/docs/)
- [Micrometer 文档](https://micrometer.io/docs)
- [Spring Boot Actuator 文档](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)
- [PromQL 教程](https://prometheus.io/docs/prometheus/latest/querying/basics/)

---

**文档版本**: 1.0
**最后更新**: 2025-10-21
**维护者**: NUSHungry 开发团队
