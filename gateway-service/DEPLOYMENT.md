# Gateway Service Deployment Guide

## 📋 Overview

The Gateway Service acts as the single entry point for all client requests in the NUSHungry microservices architecture. It provides:

- **Unified API Entry Point**: All client requests go through port 8080
- **Request Routing**: Routes requests to appropriate microservices
- **Authentication & Authorization**: JWT token validation
- **Cross-Cutting Concerns**: CORS, rate limiting, circuit breakers
- **Load Balancing**: Future support for service discovery and load balancing

**Port**: 8080
**Tech Stack**: Spring Cloud Gateway, Spring Boot 3.2.3, Redis (for rate limiting)

---

## 🚀 Quick Start

### Option 1: Docker Compose (Recommended)

```bash
cd gateway-service
docker-compose up -d
```

This will start:
- Gateway Service (port 8080)
- Redis (port 6379) - for rate limiting

### Option 2: Maven

```bash
cd gateway-service
mvn clean install
mvn spring-boot:run
```

### Option 3: JAR

```bash
cd gateway-service
mvn clean package
java -jar target/gateway-service-0.0.1-SNAPSHOT.jar
```

---

## 🔧 Configuration

### Environment Variables

Create a `.env` file or set these environment variables:

```bash
# JWT Configuration
JWT_SECRET=your-secret-key-min-256-bits
JWT_EXPIRATION=86400000

# Redis Configuration (for rate limiting)
SPRING_DATA_REDIS_HOST=localhost
SPRING_DATA_REDIS_PORT=6379
SPRING_DATA_REDIS_PASSWORD=

# Service URLs (when not using Docker)
ADMIN_SERVICE_URL=http://localhost:8082
CAFETERIA_SERVICE_URL=http://localhost:8083
REVIEW_SERVICE_URL=http://localhost:8084
MEDIA_SERVICE_URL=http://localhost:8085
PREFERENCE_SERVICE_URL=http://localhost:8086
```

### Route Configuration

Routes are defined in `application.properties`:

| Route | Service | Path Pattern |
|-------|---------|--------------|
| Admin | admin-service:8082 | `/api/admin/**` |
| Cafeteria | cafeteria-service:8083 | `/api/cafeterias/**`, `/api/stalls/**` |
| Review | review-service:8084 | `/api/reviews/**` |
| Media | media-service:8085 | `/media/**`, `/api/upload/**` |
| Preference | preference-service:8086 | `/api/favorites/**`, `/api/search-history/**` |

---

## 🔐 Security

### JWT Authentication

The gateway validates JWT tokens for all protected endpoints:

1. **Public endpoints** (no authentication required):
   - `/api/admin/auth/login`
   - `/api/admin/auth/register`
   - `/api/cafeterias` (GET only)
   - `/api/stalls` (GET only)
   - `/media/images` (GET only)
   - `/actuator/**`

2. **Protected endpoints**: All other endpoints require a valid JWT token in the `Authorization` header:
   ```
   Authorization: Bearer <your-jwt-token>
   ```

3. **User Information Injection**: The gateway extracts user info from JWT and adds these headers for downstream services:
   - `X-User-Id`: User ID
   - `X-Username`: Username
   - `X-User-Role`: User role (ROLE_USER, ROLE_ADMIN)

### CORS Configuration

CORS is configured to allow requests from:
- `http://localhost:*` (development)
- `http://127.0.0.1:*` (development)
- `https://*.nushungry.com` (production)

Allowed methods: GET, POST, PUT, DELETE, PATCH, OPTIONS

---

## ⚡ Performance Features

### Rate Limiting

Rate limiting is implemented using Redis:
- **Default**: 100 requests per minute per IP address
- **Authenticated users**: 500 requests per minute per user ID

### Circuit Breaker

Circuit breaker is configured for all downstream services:
- **Sliding window size**: 10 requests
- **Failure rate threshold**: 50%
- **Wait duration in open state**: 10 seconds
- **Timeout**: 5 seconds per request

---

## 📖 API Documentation (Swagger UI)

The Gateway Service provides a unified Swagger UI that aggregates API documentation from all microservices.

### Access Swagger UI

Once the gateway and all microservices are running, access the API documentation at:

```
http://localhost:8080/swagger-ui.html
```

### Available API Documentation

The Swagger UI provides documentation for the following services:

| Service | Description | Endpoints |
|---------|-------------|-----------|
| **Admin Service** | Admin authentication, user management, dashboard | `/api/admin/**` |
| **Cafeteria Service** | Cafeteria and stall management | `/api/cafeterias/**`, `/api/stalls/**` |
| **Review Service** | Reviews, likes, and reports | `/api/reviews/**` |
| **Media Service** | Image upload and management | `/media/**`, `/api/upload/**` |
| **Preference Service** | User favorites and search history | `/api/favorites/**`, `/api/search-history/**` |

### Using Swagger UI

1. **Select a service** from the dropdown at the top-right corner
2. **Expand an endpoint** to view request/response details
3. **Try it out**: Test endpoints directly from the browser
   - Click "Try it out"
   - Fill in required parameters
   - Add JWT token in the "Authorize" section for protected endpoints
   - Click "Execute"

### JWT Authentication in Swagger

For protected endpoints:

1. Click the **"Authorize"** button (🔒) at the top
2. Enter your JWT token in the format: `Bearer <your-token>`
3. Click "Authorize"
4. Now all requests will include the Authorization header

**Getting a JWT token**:
```bash
# Login to get token
curl -X POST http://localhost:8080/api/admin/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "admin", "password": "admin123"}'
```

### Individual Service Documentation

Each microservice also exposes its own Swagger UI:

- Admin Service: http://localhost:8082/swagger-ui.html
- Cafeteria Service: http://localhost:8083/swagger-ui.html
- Review Service: http://localhost:8084/swagger-ui.html
- Media Service: http://localhost:8085/swagger-ui.html
- Preference Service: http://localhost:8086/swagger-ui.html

---

## 🧪 Testing the Gateway

### Health Check

```bash
curl http://localhost:8080/actuator/health
```

Expected response:
```json
{
  "status": "UP"
}
```

### Gateway Routes Info

```bash
curl http://localhost:8080/actuator/gateway/routes
```

### Test Authentication Flow

1. **Login** (public endpoint):
```bash
curl -X POST http://localhost:8080/api/admin/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "admin", "password": "admin123"}'
```

2. **Access protected endpoint** with token:
```bash
curl http://localhost:8080/api/admin/dashboard/stats \
  -H "Authorization: Bearer <your-jwt-token>"
```

3. **Test CORS**:
```bash
curl -X OPTIONS http://localhost:8080/api/cafeterias \
  -H "Origin: http://localhost:5173" \
  -H "Access-Control-Request-Method: GET" \
  -v
```

### Test Routing

```bash
# Cafeteria Service
curl http://localhost:8080/api/cafeterias

# Review Service
curl http://localhost:8080/api/reviews \
  -H "Authorization: Bearer <token>"

# Media Service
curl http://localhost:8080/media/images/some-image.jpg

# Preference Service
curl http://localhost:8080/api/favorites \
  -H "Authorization: Bearer <token>"
```

---

## 🐳 Docker Deployment

### Build Image

```bash
cd gateway-service
docker build -t nushungry/gateway-service:latest .
```

### Run Container

```bash
docker run -d \
  --name gateway-service \
  -p 8080:8080 \
  -e JWT_SECRET=your-secret-key \
  -e SPRING_DATA_REDIS_HOST=redis \
  --network nushungry-network \
  nushungry/gateway-service:latest
```

### Docker Compose (Full Stack)

See root `docker-compose.yml` for full stack deployment with all services.

---

## ☁️ AWS ECS Deployment

### Task Definition

Task definition is available at `.aws/task-definitions/gateway-service.json`

Key configurations:
- **CPU**: 512 (0.5 vCPU)
- **Memory**: 1024 MB (1 GB)
- **Container Port**: 8080
- **Health Check**: `/actuator/health`

### Deployment Steps

1. **Build and push Docker image**:
```bash
aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin <account-id>.dkr.ecr.us-east-1.amazonaws.com
docker build -t nushungry/gateway-service .
docker tag nushungry/gateway-service:latest <account-id>.dkr.ecr.us-east-1.amazonaws.com/nushungry/gateway-service:latest
docker push <account-id>.dkr.ecr.us-east-1.amazonaws.com/nushungry/gateway-service:latest
```

2. **Update ECS service**:
```bash
aws ecs update-service \
  --cluster nushungry-cluster \
  --service gateway-service \
  --force-new-deployment
```

### Load Balancer Configuration

The gateway should be behind an Application Load Balancer (ALB):
- **Target Group**: gateway-service-tg
- **Health Check Path**: `/actuator/health`
- **Port**: 8080
- **Protocol**: HTTP (or HTTPS with SSL certificate)

---

## 📊 Monitoring

### Actuator Endpoints

Available at `/actuator/*`:
- `/actuator/health` - Health status
- `/actuator/info` - Application info
- `/actuator/metrics` - Prometheus metrics
- `/actuator/gateway/routes` - Gateway routes information

### Metrics to Monitor

- **Request count** by route
- **Response time** by route
- **Error rate** by route
- **Circuit breaker** status
- **Rate limit** hits

### Logging

Log levels can be configured:
```properties
logging.level.org.springframework.cloud.gateway=DEBUG
logging.level.reactor.netty.http.client=DEBUG
```

---

## 🔧 Troubleshooting

### Gateway not starting

**Symptom**: Service fails to start

**Check**:
1. Redis is running and accessible
2. JWT secret is configured (min 256 bits)
3. Port 8080 is not already in use

```bash
# Check Redis
docker ps | grep redis
redis-cli ping

# Check port
netstat -an | grep 8080
```

### Routing not working

**Symptom**: 404 errors when accessing services through gateway

**Check**:
1. Downstream services are running
2. Service URLs are correctly configured
3. Path patterns match your requests

```bash
# Check routes
curl http://localhost:8080/actuator/gateway/routes

# Check downstream service
curl http://localhost:8082/actuator/health  # admin-service
curl http://localhost:8083/actuator/health  # cafeteria-service
```

### Authentication failures

**Symptom**: 401 Unauthorized errors

**Check**:
1. JWT token is valid and not expired
2. JWT secret matches across gateway and admin-service
3. Authorization header format: `Bearer <token>`

```bash
# Decode JWT token (using jwt.io or jwt-cli)
jwt decode <your-token>

# Test with verbose output
curl -v http://localhost:8080/api/favorites \
  -H "Authorization: Bearer <token>"
```

### CORS errors

**Symptom**: Browser shows CORS errors

**Check**:
1. Frontend origin is in allowed origins list
2. Preflight OPTIONS requests are succeeding

```bash
# Test preflight
curl -X OPTIONS http://localhost:8080/api/cafeterias \
  -H "Origin: http://localhost:5173" \
  -H "Access-Control-Request-Method: GET" \
  -v
```

### High latency

**Symptom**: Slow response times

**Check**:
1. Downstream services health
2. Circuit breaker status
3. Redis connection

```bash
# Check circuit breaker metrics
curl http://localhost:8080/actuator/metrics/resilience4j.circuitbreaker.state

# Check Redis latency
redis-cli --latency
```

---

## 🔄 Updating Routes

To add or modify routes, edit `application.properties`:

```properties
# Add new service route
spring.cloud.gateway.routes[5].id=new-service
spring.cloud.gateway.routes[5].uri=http://localhost:8087
spring.cloud.gateway.routes[5].predicates[0]=Path=/api/new/**
spring.cloud.gateway.routes[5].filters[0]=StripPrefix=1
```

Then restart the gateway service.

---

## 📚 Additional Resources

- **Spring Cloud Gateway Docs**: https://spring.io/projects/spring-cloud-gateway
- **Resilience4J Docs**: https://resilience4j.readme.io/
- **Project Architecture**: `../docs/ARCHITECTURE.md`
- **API Documentation**: `../docs/API_DOCUMENTATION.md`

---

## 🤝 Support

For issues or questions:
1. Check troubleshooting section above
2. Review logs: `docker logs gateway-service`
3. Check service health: `/actuator/health`
4. Refer to project documentation in `/docs` directory
