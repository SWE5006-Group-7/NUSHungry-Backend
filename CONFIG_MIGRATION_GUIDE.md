# 配置中心迁移指南

本文档说明如何将现有微服务迁移到Spring Cloud Config Server进行集中式配置管理。

## 📋 迁移概览

**目标**: 将所有微服务的配置从本地`application.properties/yml`迁移到Config Server统一管理。

**✅ 迁移完成 (100%)**:
- ✅ 创建配置仓库 (`config-repo/`)
- ✅ 所有服务的配置文件已创建 (7个服务)
- ✅ Config Server已配置并指向配置仓库
- ✅ **所有7个微服务**已完成Config Client迁移:
  - ✅ admin-service
  - ✅ cafeteria-service
  - ✅ preference-service
  - ✅ media-service
  - ✅ review-service
  - ✅ gateway-service
  - ✅ eureka-server

---

## 🔧 迁移步骤

### 第一步: 添加Config Client依赖

为每个微服务的`pom.xml`添加依赖:

```xml
<!-- Spring Cloud Config Client -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-config</artifactId>
</dependency>
```

**位置**: 在`spring-boot-starter-actuator`依赖之后添加。

**✅ 已完成所有服务 (7/7)**:
- ✅ admin-service
- ✅ cafeteria-service
- ✅ preference-service
- ✅ media-service
- ✅ review-service
- ✅ gateway-service
- ✅ eureka-server

---

### 第二步: 创建bootstrap.yml

在每个服务的`src/main/resources/`目录下创建`bootstrap.yml`文件:

```yaml
spring:
  application:
    name: <service-name>  # 必须与config-repo中的文件名匹配

  profiles:
    active: ${SPRING_PROFILES_ACTIVE:dev}

  cloud:
    config:
      uri: ${CONFIG_SERVER_URI:http://localhost:8888}
      username: ${CONFIG_SERVER_USERNAME:config}
      password: ${CONFIG_SERVER_PASSWORD:config123}
      fail-fast: true
      retry:
        max-attempts: 6
        initial-interval: 1000
        multiplier: 1.1
        max-interval: 2000
```

**重要**:
- `spring.application.name` 必须与`config-repo/`中的配置文件名匹配
- 例如: `preference-service` → `config-repo/preference-service.yml`

**✅ 已完成所有服务 (7/7)**:
- ✅ admin-service → `admin-service/src/main/resources/bootstrap.yml`
- ✅ cafeteria-service → `cafeteria-service/src/main/resources/bootstrap.yml`
- ✅ preference-service → `preference-service/src/main/resources/bootstrap.yml`
- ✅ media-service → `media-service/src/main/resources/bootstrap.yml`
- ✅ review-service → `review-service/src/main/resources/bootstrap.yml`
- ✅ gateway-service → `gateway-service/src/main/resources/bootstrap.yml`
- ✅ eureka-server → `eureka-server/src/main/resources/bootstrap.yml`

---

### 第三步: (可选) 简化本地application.properties

创建bootstrap.yml后,本地的`application.properties`可以保留最小配置,或者完全删除(所有配置从Config Server获取)。

**建议做法**:
1. **保留本地配置文件** - 作为fallback,防止Config Server不可用
2. **仅保留服务名和端口** - 其他配置从Config Server获取
3. **环境变量优先** - 使用环境变量覆盖配置

**示例 - 简化后的application.properties**:
```properties
# 仅保留核心配置
spring.application.name=admin-service
server.port=8082

# 其他所有配置从Config Server获取
```

---

### 第四步: 测试配置加载

#### 1. 启动Config Server

```bash
cd config-server
mvn spring-boot:run
```

验证Config Server启动成功:
```bash
curl -u config:config123 http://localhost:8888/admin-service/dev
```

#### 2. 测试服务配置获取

启动已迁移的服务(如admin-service):
```bash
cd admin-service
mvn spring-boot:run
```

检查日志,确认:
- ✅ 成功连接到Config Server
- ✅ 加载了正确的配置文件
- ✅ 服务正常启动

**成功日志示例**:
```
Fetching config from server at: http://localhost:8888
Located environment: name=admin-service, profiles=[dev], label=null
```

---

## 📁 配置仓库结构

```
config-repo/
├── application.yml              # 公共配置 - 所有服务共享
├── application-dev.yml          # 开发环境公共配置
├── application-prod.yml         # 生产环境公共配置
├── admin-service.yml            # Admin Service专属配置
├── cafeteria-service.yml        # Cafeteria Service专属配置
├── preference-service.yml       # Preference Service专属配置
├── media-service.yml            # Media Service专属配置
├── review-service.yml           # Review Service专属配置
├── gateway-service.yml          # Gateway Service专属配置
└── eureka-server.yml            # Eureka Server专属配置
```

**配置加载优先级** (后者覆盖前者):
1. `application.yml` - 公共配置
2. `application-{profile}.yml` - 环境专属公共配置
3. `{service-name}.yml` - 服务专属配置
4. `{service-name}-{profile}.yml` - 服务+环境专属配置 (可选)
5. 环境变量 - 最高优先级

---

## 🔒 安全配置

### 敏感信息管理

**永远不要将敏感信息直接写入配置文件!**

使用环境变量:
```yaml
# config-repo/admin-service.yml
spring:
  datasource:
    password: ${ADMIN_DB_PASSWORD}  # 从环境变量读取
jwt:
  secret: ${JWT_SECRET}              # 从环境变量读取
```

### Config Server加密支持

Config Server支持加密敏感属性:

```bash
# 加密值
curl http://localhost:8888/encrypt -d "my-secret-value"

# 在配置文件中使用加密值
password: '{cipher}AQAEnFjknLZKJ...'
```

---

## 🧪 测试清单

### 单元测试

需要在测试配置中禁用Config Client:

**方法1: 使用test profile**
```yaml
# src/test/resources/application-test.properties
spring.cloud.config.enabled=false
```

**方法2: 使用@TestPropertySource**
```java
@SpringBootTest
@TestPropertySource(properties = {
    "spring.cloud.config.enabled=false"
})
class MyServiceTest {
    // 测试代码
}
```

### 集成测试

1. ✅ Config Server能正常启动
2. ✅ 服务能从Config Server获取配置
3. ✅ 不同环境(dev/prod)能加载正确配置
4. ✅ 环境变量能正确覆盖配置
5. ✅ Config Server不可用时服务能fallback到本地配置

---

## 🚀 部署注意事项

### Docker部署

确保服务能访问Config Server:

```yaml
# docker-compose.yml
services:
  config-server:
    image: nushungry/config-server
    ports:
      - "8888:8888"
    networks:
      - nushungry-network

  admin-service:
    image: nushungry/admin-service
    depends_on:
      - config-server
    environment:
      - CONFIG_SERVER_URI=http://config-server:8888
    networks:
      - nushungry-network
```

### 启动顺序

1. **Config Server** - 最先启动
2. **Eureka Server** - 第二启动
3. **其他微服务** - 按依赖顺序启动
4. **Gateway Service** - 最后启动

---

## 🔄 配置刷新

### 手动刷新

修改配置后,无需重启服务即可刷新:

```bash
# 刷新单个服务
curl -X POST http://localhost:8082/actuator/refresh

# 需要在Bean上添加@RefreshScope注解
@Service
@RefreshScope
public class MyService {
    @Value("${my.property}")
    private String myProperty;
}
```

### 自动刷新 (使用Spring Cloud Bus - 可选)

使用RabbitMQ/Kafka广播配置更新到所有服务实例。

---

## ❓ 故障排查

### 问题1: 服务启动时连接Config Server失败

**症状**: `Could not locate PropertySource`

**解决**:
1. 确认Config Server正在运行: `curl http://localhost:8888/actuator/health`
2. 检查`bootstrap.yml`中的`spring.application.name`是否与配置文件名匹配
3. 检查认证信息是否正确
4. 查看Config Server日志

### 问题2: 配置没有加载

**症状**: 服务使用了错误的或默认的配置值

**解决**:
1. 检查`bootstrap.yml`的`spring.profiles.active`
2. 验证配置文件在config-repo中存在
3. 测试Config Server API: `curl -u config:config123 http://localhost:8888/{service-name}/{profile}`
4. 检查配置优先级和覆盖规则

### 问题3: 测试失败

**症状**: 单元测试/集成测试连接Config Server失败

**解决**:
在测试配置中禁用Config Client:
```properties
spring.cloud.config.enabled=false
```

---

## 📚 参考资料

- [Spring Cloud Config文档](https://docs.spring.io/spring-cloud-config/docs/current/reference/html/)
- [Config Server DEPLOYMENT.md](config-server/DEPLOYMENT.md)
- [配置仓库README](config-repo/README.md)

---

## ✅ 迁移进度追踪

### 已完成 (7/7) 🎉

| 服务 | pom.xml | bootstrap.yml | 测试配置 | 状态 |
|-----|---------|--------------|---------|------|
| admin-service | ✅ | ✅ | ✅ | 🟢 完成 |
| cafeteria-service | ✅ | ✅ | ✅ | 🟢 完成 |
| preference-service | ✅ | ✅ | ✅ | 🟢 完成 |
| media-service | ✅ | ✅ | ✅ | 🟢 完成 |
| review-service | ✅ | ✅ | ✅ | 🟢 完成 |
| gateway-service | ✅ | ✅ | ✅ | 🟢 完成 |
| eureka-server | ✅ | ✅ | ✅ | 🟢 完成 |

**完成度**: 100% (7/7 服务)

---

## 📝 后续步骤

1. ✅ 完成所有7个服务的Config Client迁移
2. ✅ 更新所有服务的测试配置（禁用Config Client）
3. ⏳ 更新Docker Compose配置（添加Config Server依赖）
4. ⏳ 更新CI/CD管道（配置环境变量）
5. ⏳ 更新生产部署文档

---

**最后更新**: 2025-10-20
**作者**: NUSHungry Team
