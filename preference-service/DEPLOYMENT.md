# Preference Service 部署文档

## 快速开始

```bash
# 启动服务
docker-compose up -d

# 验证
curl http://localhost:8086/actuator/health

# API 文档
open http://localhost:8086/swagger-ui.html
```

## 服务信息
- **API**: http://localhost:8086
- **Swagger**: http://localhost:8086/swagger-ui.html
- **PostgreSQL**: localhost:5435 (用户: preference, 密码: password123)

## 测试 API

```bash
# 添加收藏
curl -X POST http://localhost:8086/api/favorites \
  -H "Content-Type: application/json" \
  -d '{"userId":1,"stallId":1}'

# 获取收藏列表
curl http://localhost:8086/api/favorites/user/1

# 记录搜索历史
curl -X POST http://localhost:8086/api/search-history \
  -H "Content-Type: application/json" \
  -d '{"userId":1,"keyword":"chinese"}'
```

**最后更新**: 2025-01-19
