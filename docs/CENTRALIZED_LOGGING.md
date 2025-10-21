# 集中式日志系统 (ELK Stack)

本文档说明如何使用ELK Stack（Elasticsearch、Logstash、Kibana）实现NUSHungry微服务架构的集中式日志管理。

---

## 📋 目录

- [架构概述](#架构概述)
- [快速开始](#快速开始)
- [配置说明](#配置说明)
- [使用指南](#使用指南)
- [查询示例](#查询示例)
- [故障排查](#故障排查)
- [最佳实践](#最佳实践)
- [性能优化](#性能优化)

---

## 🏗️ 架构概述

### 组件说明

```
┌──────────────┐    JSON日志      ┌───────────┐    处理/过滤     ┌──────────────┐
│ 微服务       │ ───────────────> │ Logstash  │ ──────────────> │ Elasticsearch│
│ (8个服务)    │   TCP:5000       │           │                 │   索引/存储   │
└──────────────┘                  └───────────┘                 └──────────────┘
                                                                        │
                                                                        │ 查询
┌──────────────┐                  ┌───────────┐                        │
│ Docker       │ ───────────────> │ Filebeat  │ ───────────────────────┘
│ 容器日志     │   容器日志卷      │           │
└──────────────┘                  └───────────┘
                                        │
                                        ▼
                                  ┌───────────┐
                                  │  Kibana   │  可视化/查询界面
                                  │  :5601    │
                                  └───────────┘
```

### 核心功能

1. **集中式日志收集**: 所有微服务日志统一收集到Elasticsearch
2. **JSON格式化**: 结构化日志便于搜索和分析
3. **实时搜索**: 基于Elasticsearch的全文搜索
4. **可视化分析**: Kibana仪表盘和图表
5. **日志保留策略**: 自动管理历史日志
6. **多维度过滤**: 按服务、级别、时间等维度查询

---

## 🚀 快速开始

### 前置条件

- Docker 20.10+
- Docker Compose 2.0+
- 至少 4GB 可用内存（推荐 8GB）

### 1. 启动ELK Stack

```bash
# 进入ELK目录
cd elk

# 启动所有服务
docker-compose up -d

# 查看服务状态
docker-compose ps

# 查看日志
docker-compose logs -f
```

### 2. 验证服务运行

```bash
# 检查Elasticsearch
curl http://localhost:9200/_cluster/health

# 检查Logstash
curl http://localhost:9600

# 访问Kibana
# 浏览器打开: http://localhost:5601
```

### 3. 启动微服务

微服务会自动将日志发送到Logstash：

```bash
# 启动所有微服务
cd ..
docker-compose up -d

# 或单独启动某个服务
cd admin-service
mvn spring-boot:run
```

### 4. 配置Kibana索引模式

1. 打开 Kibana: http://localhost:5601
2. 导航到 **Management > Stack Management > Index Patterns**
3. 点击 **Create index pattern**
4. 输入索引模式: `nushungry-logs-*`
5. 选择时间字段: `@timestamp`
6. 点击 **Create index pattern**

---

## ⚙️ 配置说明

### 微服务日志配置

每个微服务的 `logback-spring.xml` 配置了三个appender：

#### 1. 控制台输出（CONSOLE）
```xml
<appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
    <encoder>
        <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level [${serviceName}] %logger{36} - %msg%n</pattern>
    </encoder>
</appender>
```

#### 2. 文件输出（FILE）
```xml
<appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
    <file>logs/${serviceName}.log</file>
    <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
        <fileNamePattern>logs/${serviceName}-%d{yyyy-MM-dd}.%i.log</fileNamePattern>
        <maxFileSize>100MB</maxFileSize>
        <maxHistory>30</maxHistory>
    </rollingPolicy>
</appender>
```

#### 3. Logstash输出（LOGSTASH）
```xml
<appender name="LOGSTASH" class="net.logstash.logback.appender.LogstashTcpSocketAppender">
    <destination>${logstashHost}:${logstashPort}</destination>
    <encoder class="net.logstash.logback.encoder.LogstashEncoder">
        <customFields>{"service_name":"${serviceName}","environment":"${environment}"}</customFields>
    </encoder>
</appender>
```

### 环境变量配置

可以通过环境变量或application.properties配置Logstash连接：

```properties
# application.properties
logstash.host=localhost
logstash.port=5000
spring.profiles.active=dev
```

或Docker Compose环境变量：

```yaml
environment:
  - LOGSTASH_HOST=logstash
  - LOGSTASH_PORT=5000
  - SPRING_PROFILES_ACTIVE=prod
```

---

## 📖 使用指南

### 在Kibana中查看日志

1. **打开Discover页面**: http://localhost:5601/app/discover
2. **选择索引模式**: `nushungry-logs-*`
3. **选择时间范围**: 右上角时间选择器
4. **搜索和过滤**: 使用搜索栏和过滤器

### 常用字段说明

| 字段名 | 说明 | 示例值 |
|--------|------|--------|
| `@timestamp` | 日志时间戳 | `2025-10-21T10:30:00.000Z` |
| `service_name` | 服务名称 | `admin-service` |
| `level` | 日志级别 | `INFO`, `ERROR`, `DEBUG` |
| `message` | 日志消息 | `User logged in successfully` |
| `logger_name` | Logger名称 | `com.nushungry.admin.controller.AuthController` |
| `thread_name` | 线程名称 | `http-nio-8082-exec-1` |
| `stack_trace` | 异常堆栈 | (仅错误日志) |
| `environment` | 环境标识 | `dev`, `prod` |

---

## 🔍 查询示例

### 基础查询

#### 1. 查看特定服务的日志
```
service_name: "admin-service"
```

#### 2. 查看错误日志
```
level: "ERROR"
```

#### 3. 查看多个服务的日志
```
service_name: ("admin-service" OR "cafeteria-service")
```

#### 4. 查看特定时间范围的日志
```
@timestamp: [2025-10-21T00:00:00 TO 2025-10-21T23:59:59]
```

### 高级查询

#### 1. 查找包含特定关键词的日志
```
message: *login* AND service_name: "admin-service"
```

#### 2. 查找异常日志
```
stack_trace: * AND level: "ERROR"
```

#### 3. 查找慢SQL查询
```
message: *slow* AND logger_name: *hibernate*
```

#### 4. 按服务和级别过滤
```
service_name: "review-service" AND (level: "ERROR" OR level: "WARN")
```

#### 5. 查找用户操作日志
```
message: *userId* AND service_name: "preference-service"
```

### KQL (Kibana Query Language) 查询

#### 1. 模糊匹配
```kql
message: *authentication*
```

#### 2. 精确匹配
```kql
service_name: "admin-service" AND level: "ERROR"
```

#### 3. 范围查询
```kql
@timestamp >= "2025-10-21T00:00:00" AND @timestamp <= "2025-10-21T23:59:59"
```

#### 4. 存在性查询
```kql
stack_trace: *
```

---

## 🛠️ 故障排查

### 问题1: 服务无法连接到Logstash

**症状**: 微服务日志显示连接Logstash失败

**解决方案**:
```bash
# 1. 检查Logstash是否运行
docker-compose ps logstash

# 2. 检查Logstash日志
docker-compose logs logstash

# 3. 验证端口是否开放
nc -zv localhost 5000

# 4. 检查网络连接
docker network inspect elk_elk
```

### 问题2: Kibana中看不到日志

**症状**: Kibana Discover页面无数据

**排查步骤**:
```bash
# 1. 检查Elasticsearch中是否有数据
curl "http://localhost:9200/nushungry-logs-*/_count"

# 2. 验证索引模式是否正确
# Kibana > Management > Index Patterns

# 3. 检查时间范围是否正确
# Kibana > Discover > 时间选择器

# 4. 查看Logstash处理管道
curl "http://localhost:9600/_node/stats/pipelines"
```

### 问题3: Elasticsearch内存不足

**症状**: Elasticsearch频繁OOM或重启

**解决方案**:
```yaml
# 修改 elk/docker-compose.yml
elasticsearch:
  environment:
    - ES_JAVA_OPTS=-Xms1g -Xmx1g  # 增加堆内存
```

### 问题4: 日志丢失

**症状**: 部分日志未出现在Kibana

**排查步骤**:
```bash
# 1. 检查Logstash队列大小
docker-compose logs logstash | grep "queue"

# 2. 增加异步队列大小（logback-spring.xml）
<appender name="ASYNC_LOGSTASH" class="ch.qos.logback.classic.AsyncAppender">
    <queueSize>1024</queueSize>  <!-- 默认512，可增加 -->
</appender>

# 3. 检查磁盘空间
df -h
```

### 问题5: 查询速度慢

**症状**: Kibana查询响应缓慢

**优化方法**:
```bash
# 1. 减小时间范围
# 2. 使用索引生命周期管理(ILM)
# 3. 增加Elasticsearch节点数量
# 4. 优化查询语句，避免通配符开头
```

---

## 💡 最佳实践

### 1. 日志级别使用

```java
// ERROR: 严重错误，需要立即处理
log.error("Failed to process payment: {}", errorMessage, exception);

// WARN: 警告信息，可能需要关注
log.warn("Payment processing slow, took {}ms", duration);

// INFO: 重要业务事件
log.info("User {} logged in successfully", userId);

// DEBUG: 详细调试信息（生产环境关闭）
log.debug("Processing request with params: {}", params);

// TRACE: 非常详细的追踪信息（仅开发环境）
log.trace("Method entry: {}", methodName);
```

### 2. 结构化日志

使用MDC（Mapped Diagnostic Context）添加上下文信息：

```java
import org.slf4j.MDC;

// 添加用户ID到所有后续日志
MDC.put("userId", user.getId());
MDC.put("requestId", UUID.randomUUID().toString());

try {
    // 业务逻辑
    log.info("Processing user request");
} finally {
    MDC.clear(); // 清理MDC
}
```

### 3. 异常日志

```java
try {
    // 业务逻辑
} catch (Exception e) {
    // ✅ 好的做法: 包含异常对象
    log.error("Failed to save user data for userId: {}", userId, e);

    // ❌ 不好的做法: 仅记录消息
    log.error("Error: " + e.getMessage());
}
```

### 4. 敏感信息脱敏

```java
// ❌ 不要记录敏感信息
log.info("User login: username={}, password={}", username, password);

// ✅ 脱敏处理
log.info("User login: username={}", maskSensitiveData(username));
```

### 5. 日志聚合和去重

```java
// 避免在循环中打印大量重复日志
int processedCount = 0;
for (User user : users) {
    processUser(user);
    processedCount++;
}
// ✅ 汇总日志
log.info("Processed {} users", processedCount);
```

---

## ⚡ 性能优化

### 1. Elasticsearch优化

```yaml
# elk/docker-compose.yml
elasticsearch:
  environment:
    - ES_JAVA_OPTS=-Xms2g -Xmx2g  # 设置堆内存为物理内存的50%
    - indices.memory.index_buffer_size=30%  # 索引缓冲区
```

### 2. Logstash优化

```yaml
# elk/logstash/config/logstash.yml
pipeline.workers: 4  # 增加工作线程数
pipeline.batch.size: 125  # 批处理大小
pipeline.batch.delay: 50  # 批处理延迟（毫秒）
```

### 3. 索引生命周期管理(ILM)

创建ILM策略自动管理索引：

```bash
# 创建ILM策略
curl -X PUT "localhost:9200/_ilm/policy/nushungry-logs-policy" \
-H 'Content-Type: application/json' -d'
{
  "policy": {
    "phases": {
      "hot": {
        "actions": {
          "rollover": {
            "max_age": "7d",
            "max_size": "50GB"
          }
        }
      },
      "delete": {
        "min_age": "30d",
        "actions": {
          "delete": {}
        }
      }
    }
  }
}
'
```

### 4. Logback异步优化

```xml
<!-- 增加异步队列容量 -->
<appender name="ASYNC_LOGSTASH" class="ch.qos.logback.classic.AsyncAppender">
    <queueSize>1024</queueSize>
    <discardingThreshold>0</discardingThreshold>
    <neverBlock>false</neverBlock>
    <appender-ref ref="LOGSTASH"/>
</appender>
```

---

## 📊 Kibana仪表盘示例

### 创建基础监控仪表盘

1. **打开 Kibana Dashboard**: http://localhost:5601/app/dashboards
2. **创建新仪表盘**: 点击 "Create dashboard"
3. **添加可视化组件**:

#### 组件1: 日志级别分布（饼图）
- 可视化类型: Pie
- 指标: Count
- 分桶: Terms Aggregation on `level.keyword`

#### 组件2: 服务日志量（柱状图）
- 可视化类型: Vertical Bar
- X轴: Terms Aggregation on `service_name.keyword`
- Y轴: Count

#### 组件3: 错误日志趋势（折线图）
- 可视化类型: Line
- 过滤器: `level: "ERROR"`
- X轴: Date Histogram on `@timestamp`
- Y轴: Count

#### 组件4: 热门错误消息（表格）
- 可视化类型: Data Table
- 过滤器: `level: "ERROR"`
- 行: Terms Aggregation on `message.keyword` (Top 10)
- 指标: Count

---

## 🔐 安全配置

### 生产环境建议

1. **启用Elasticsearch安全**:
```yaml
# elk/docker-compose.yml
elasticsearch:
  environment:
    - xpack.security.enabled=true
    - ELASTIC_PASSWORD=your_strong_password
```

2. **配置Kibana认证**:
```yaml
kibana:
  environment:
    - ELASTICSEARCH_USERNAME=kibana_system
    - ELASTICSEARCH_PASSWORD=your_password
```

3. **使用TLS加密**:
```yaml
elasticsearch:
  environment:
    - xpack.security.http.ssl.enabled=true
    - xpack.security.transport.ssl.enabled=true
```

---

## 📚 相关资源

- [Elasticsearch官方文档](https://www.elastic.co/guide/en/elasticsearch/reference/current/index.html)
- [Logstash官方文档](https://www.elastic.co/guide/en/logstash/current/index.html)
- [Kibana官方文档](https://www.elastic.co/guide/en/kibana/current/index.html)
- [Logback文档](http://logback.qos.ch/documentation.html)
- [Logstash Logback Encoder](https://github.com/logfellow/logstash-logback-encoder)

---

## 🤝 贡献

如有问题或建议，请提交Issue或Pull Request到项目仓库。

---

**最后更新**: 2025-10-21
