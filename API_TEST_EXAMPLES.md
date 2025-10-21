# API 测试示例

## 1. 健康检查

```bash
# 检查 Gateway 是否正常
curl http://localhost:8080/actuator/health

# 检查所有服务是否在 Eureka 注册
curl http://eureka:eureka@localhost:8761/eureka/apps
```

## 2. 管理员登录（获取 JWT Token）

```bash
# 登录获取 Token
curl -X POST http://localhost:8080/api/admin/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "admin123"
  }'

# 返回示例：
# {
#   "token": "eyJhbGciOiJIUzI1NiJ9...",
#   "username": "admin",
#   "role": "ROLE_ADMIN"
# }
```

## 3. 查询食堂列表（通过 Gateway）

```bash
# 不需要认证的公开接口
curl http://localhost:8080/api/cafeterias

# 返回示例：
# [
#   {
#     "id": 1,
#     "name": "The Deck",
#     "location": "Faculty of Engineering",
#     "averageRating": 4.2
#   },
#   ...
# ]
```

## 4. 创建食堂（需要管理员权限）

```bash
# 使用上面获取的 Token
curl -X POST http://localhost:8080/api/admin/cafeterias \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -d '{
    "name": "新食堂",
    "location": "UTown",
    "openingHours": "07:00-22:00",
    "description": "测试食堂"
  }'
```

## 5. 上传图片（Media Service）

```bash
# 上传单张图片
curl -X POST http://localhost:8080/media/upload \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -F "file=@/path/to/your/image.jpg"

# 返回示例：
# {
#   "url": "http://localhost:8085/uploads/20250121_123456_image.jpg",
#   "filename": "20250121_123456_image.jpg"
# }
```

## 6. 添加收藏（Preference Service）

```bash
curl -X POST http://localhost:8080/api/favorites \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -d '{
    "stallId": 1
  }'
```

## 7. 创建评价（Review Service）

```bash
curl -X POST http://localhost:8080/api/reviews \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -d '{
    "stallId": 1,
    "rating": 5,
    "comment": "非常好吃！",
    "images": ["http://localhost:8085/uploads/image1.jpg"]
  }'
```

## 8. 查看 Dashboard 统计（Admin Service）

```bash
curl http://localhost:8080/api/admin/dashboard/stats \
  -H "Authorization: Bearer YOUR_ADMIN_TOKEN_HERE"

# 返回示例：
# {
#   "totalUsers": 150,
#   "totalCafeterias": 8,
#   "totalStalls": 45,
#   "totalReviews": 320
# }
```

## 使用 Postman 测试

1. **导入 Swagger 定义**：
   - 访问 http://localhost:8080/v3/api-docs
   - 复制 JSON
   - 在 Postman 中选择 Import → Raw Text → 粘贴

2. **设置环境变量**：
   - 创建环境 `NUSHungry Local`
   - 添加变量：
     - `baseUrl`: `http://localhost:8080`
     - `token`: `<登录后获取的 JWT Token>`

3. **配置 Authorization**：
   - Type: Bearer Token
   - Token: `{{token}}`

## 常见问题

### 1. 401 Unauthorized
- 检查 Token 是否已过期
- 检查 Authorization header 格式: `Bearer <token>`

### 2. 503 Service Unavailable
- 检查目标服务是否启动: `docker-compose ps`
- 查看 Eureka 是否注册: http://localhost:8761

### 3. 500 Internal Server Error
- 查看服务日志: `docker-compose logs -f <service-name>`
- 检查数据库连接是否正常
