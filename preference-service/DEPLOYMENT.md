# Preference Service 部署文档

## 快速开始

```bash
# 启动服务（包括 PostgreSQL 和 Redis）
docker-compose up -d

# 验证服务健康状态
curl http://localhost:8086/actuator/health

# 查看缓存统计
curl http://localhost:8086/actuator/caches

# API 文档
open http://localhost:8086/swagger-ui.html
```

## 服务信息
- **API**: http://localhost:8086
- **Swagger**: http://localhost:8086/swagger-ui.html
- **PostgreSQL**: localhost:5435 (用户: preference, 密码: password123)
- **Redis**: localhost:6380 (无密码)

## 基础设施

### PostgreSQL 数据库
- **版本**: PostgreSQL 16-alpine
- **端口**: 5435
- **数据库**: preference_service
- **持久化**: 数据存储在 `postgres_data` volume

### Redis 缓存
- **版本**: Redis 7-alpine
- **端口**: 6380
- **持久化**: AOF (Append-Only File) 模式
- **数据存储**: `redis_data` volume

## Redis 缓存策略

### 缓存配置
- **favorites**: 用户收藏列表缓存，TTL 10分钟
- **searchHistory**: 用户搜索历史缓存，TTL 5分钟

### 缓存行为
- **添加收藏**: 自动清除该用户的收藏缓存
- **删除收藏**: 自动清除该用户的收藏缓存
- **添加搜索历史**: 自动清除该用户的搜索历史缓存
- **删除搜索历史**: 自动清除该用户的搜索历史缓存

### 查看缓存状态
```bash
# 查看所有缓存
curl http://localhost:8086/actuator/caches

# 清除特定缓存（需要管理员权限）
curl -X DELETE http://localhost:8086/actuator/caches/favorites

# 连接到 Redis 查看数据
docker exec -it preference-redis redis-cli
> KEYS *
> GET <key>
```

## 测试 API

```bash
# 添加收藏
curl -X POST http://localhost:8086/api/favorites \
  -H "Content-Type: application/json" \
  -d '{"userId":1,"stallId":1}'

# 获取收藏列表（第一次从数据库，第二次从缓存）
curl http://localhost:8086/api/favorites/user/1

# 记录搜索历史
curl -X POST http://localhost:8086/api/search-history \
  -H "Content-Type: application/json" \
  -d '{"userId":1,"keyword":"chinese"}'

# 获取搜索历史（第一次从数据库，第二次从缓存）
curl http://localhost:8086/api/search-history/user/1
```

## 性能优化

### 缓存效果
- **首次查询**: 从 PostgreSQL 数据库读取（~20-50ms）
- **缓存命中**: 从 Redis 读取（~2-5ms）
- **性能提升**: 约 5-10倍

### 监控缓存命中率
```bash
# 查看 Redis 统计信息
docker exec -it preference-redis redis-cli INFO stats

# 关键指标
# - keyspace_hits: 缓存命中次数
# - keyspace_misses: 缓存未命中次数
# - 命中率 = keyspace_hits / (keyspace_hits + keyspace_misses)
```

## 故障排查

### Redis 连接问题
```bash
# 检查 Redis 是否运行
docker ps | grep preference-redis

# 查看 Redis 日志
docker logs preference-redis

# 测试 Redis 连接
docker exec -it preference-redis redis-cli PING
# 应该返回: PONG
```

### 缓存不生效
1. 检查 `application.properties` 中 `spring.cache.type=redis`
2. 确认 Redis 服务正常运行
3. 查看应用日志是否有 Redis 连接错误

### 清理所有数据
```bash
# 停止服务并删除所有数据
docker-compose down -v

# 重新启动
docker-compose up -d
```

## 生产部署建议

### Redis 配置优化
```properties
# 建议生产环境配置
spring.data.redis.password=<strong-password>
spring.data.redis.lettuce.pool.max-active=20
spring.data.redis.lettuce.pool.max-idle=10
spring.data.redis.lettuce.pool.min-idle=5
```

### 缓存 TTL 调整
根据业务需求调整 `RedisConfig.java` 中的 TTL：
- 频繁变化的数据：缩短 TTL（1-5分钟）
- 稳定的数据：延长 TTL（10-30分钟）

### Redis 持久化
生产环境建议同时启用 AOF 和 RDB：
```bash
# 在 docker-compose.yml 中修改
command: redis-server --appendonly yes --save 60 1000
```

**最后更新**: 2025-01-20
