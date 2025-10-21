# Eureka Server Deployment Guide

## 📋 Table of Contents

- [Overview](#overview)
- [Prerequisites](#prerequisites)
- [Local Development](#local-development)
- [Docker Deployment](#docker-deployment)
- [AWS ECS Deployment](#aws-ecs-deployment)
- [Configuration](#configuration)
- [Monitoring](#monitoring)
- [Troubleshooting](#troubleshooting)

## 🎯 Overview

Eureka Server provides service registry and discovery for the NUSHungry microservices architecture. It enables:

- **Service Registration**: Microservices automatically register themselves
- **Service Discovery**: Clients can discover service instances dynamically
- **Load Balancing**: Gateway uses Eureka for client-side load balancing
- **Health Monitoring**: Tracks service health and availability
- **Dashboard UI**: Web interface to monitor registered services

**Service Details**:
- **Port**: 8761
- **Health Check**: `/actuator/health`
- **Dashboard**: `http://localhost:8761`
- **Authentication**: Basic Auth (username: eureka, password: eureka)

---

## 🛠️ Prerequisites

### Required Software
- **Java**: JDK 17 or higher
- **Maven**: 3.8+
- **Docker**: 20.10+ (for containerized deployment)
- **Docker Compose**: 2.x+ (for local multi-container setup)

### Optional
- **AWS CLI**: For ECS deployment
- **kubectl**: For Kubernetes deployment (future)

---

## 💻 Local Development

### 1. Build from Source

```bash
# Navigate to eureka-server directory
cd eureka-server

# Build the project
mvn clean install

# Run tests
mvn test

# Package as JAR
mvn package -DskipTests
```

### 2. Run Locally

```bash
# Run with Maven
mvn spring-boot:run

# Or run the JAR directly
java -jar target/eureka-server-1.0.0.jar
```

### 3. Access Dashboard

Open your browser and navigate to:
```
http://localhost:8761
```

**Credentials**:
- Username: `eureka`
- Password: `eureka`

### 4. Verify Health

```bash
# Health check (public endpoint)
curl http://localhost:8761/actuator/health

# Expected response
{
  "status": "UP"
}
```

---

## 🐳 Docker Deployment

### Standalone Docker Container

#### Build Image

```bash
# Build Docker image
docker build -t eureka-server:latest .

# Verify image
docker images | grep eureka-server
```

#### Run Container

```bash
# Run with default settings
docker run -d \
  --name eureka-server \
  -p 8761:8761 \
  eureka-server:latest

# Run with custom credentials
docker run -d \
  --name eureka-server \
  -p 8761:8761 \
  -e SPRING_SECURITY_USER_NAME=admin \
  -e SPRING_SECURITY_USER_PASSWORD=your-secure-password \
  eureka-server:latest
```

#### Check Logs

```bash
# View logs
docker logs -f eureka-server

# Check container status
docker ps | grep eureka-server
```

### Docker Compose (Recommended for Local Development)

#### Using Standalone Compose

```bash
# Start Eureka Server
docker-compose up -d

# Check status
docker-compose ps

# View logs
docker-compose logs -f

# Stop service
docker-compose down
```

#### Using Root Compose (Full Stack)

```bash
# From project root directory
cd ..

# Start all services (including Eureka)
docker-compose up -d

# Start only Eureka and its dependencies
docker-compose up -d eureka-server

# Check Eureka registration status
docker-compose logs -f gateway-service | grep Eureka
```

---

## ☁️ AWS ECS Deployment

### Prerequisites

1. **AWS Account** with appropriate permissions
2. **ECR Repository** created: `nushungry/eureka-server`
3. **ECS Cluster** created: `nushungry-dev-cluster`
4. **VPC and Subnets** configured
5. **Security Groups** allowing inbound traffic on port 8761
6. **Secrets Manager** with credentials:
   - `nushungry/eureka/username`
   - `nushungry/eureka/password`

### 1. Build and Push Image to ECR

```bash
# Authenticate to ECR
aws ecr get-login-password --region ap-southeast-1 | \
  docker login --username AWS --password-stdin YOUR_ACCOUNT_ID.dkr.ecr.ap-southeast-1.amazonaws.com

# Build image
mvn package -DskipTests
docker build -t eureka-server:latest .

# Tag image
docker tag eureka-server:latest \
  YOUR_ACCOUNT_ID.dkr.ecr.ap-southeast-1.amazonaws.com/nushungry/eureka-server:latest

# Push to ECR
docker push YOUR_ACCOUNT_ID.dkr.ecr.ap-southeast-1.amazonaws.com/nushungry/eureka-server:latest
```

### 2. Register Task Definition

```bash
# Update task definition with your account ID
sed -i 's/YOUR_ACCOUNT_ID/123456789012/g' .aws/task-definitions/eureka-server.json

# Register task definition
aws ecs register-task-definition \
  --cli-input-json file://.aws/task-definitions/eureka-server.json
```

### 3. Create ECS Service

```bash
# Create service
aws ecs create-service \
  --cluster nushungry-dev-cluster \
  --service-name nushungry-dev-eureka-server \
  --task-definition nushungry-dev-eureka-server \
  --desired-count 1 \
  --launch-type FARGATE \
  --network-configuration "awsvpcConfiguration={subnets=[subnet-xxx],securityGroups=[sg-xxx],assignPublicIp=ENABLED}" \
  --load-balancers "targetGroupArn=arn:aws:elasticloadbalancing:...,containerName=eureka-server,containerPort=8761"
```

### 4. Verify Deployment

```bash
# Check service status
aws ecs describe-services \
  --cluster nushungry-dev-cluster \
  --services nushungry-dev-eureka-server

# Check tasks
aws ecs list-tasks \
  --cluster nushungry-dev-cluster \
  --service-name nushungry-dev-eureka-server
```

### 5. Access Dashboard

Once deployed, access the dashboard through:
- **Load Balancer**: `http://your-alb-dns.ap-southeast-1.elb.amazonaws.com:8761`
- **Direct IP**: `http://task-public-ip:8761` (if public IP is assigned)

---

## ⚙️ Configuration

### Environment Variables

| Variable | Description | Default | Required |
|----------|-------------|---------|----------|
| `SERVER_PORT` | Server port | 8761 | No |
| `SPRING_PROFILES_ACTIVE` | Active profile | default | No |
| `SPRING_SECURITY_USER_NAME` | Dashboard username | eureka | No |
| `SPRING_SECURITY_USER_PASSWORD` | Dashboard password | eureka | Yes (Production) |
| `EUREKA_CLIENT_REGISTER_WITH_EUREKA` | Self-register | false | No |
| `EUREKA_CLIENT_FETCH_REGISTRY` | Fetch registry | false | No |
| `EUREKA_SERVER_ENABLE_SELF_PRESERVATION` | Self-preservation mode | true | No |

### Application Profiles

#### Development (default)
```properties
# application.properties
eureka.server.enable-self-preservation=true
logging.level.com.netflix.eureka=DEBUG
```

#### Production (docker/ecs)
```properties
# application-prod.properties
eureka.server.enable-self-preservation=true
eureka.server.eviction-interval-timer-in-ms=60000
logging.level.com.netflix.eureka=INFO
```

### Security Configuration

**⚠️ Important**: Always change default credentials in production!

```bash
# Docker
docker run -e SPRING_SECURITY_USER_PASSWORD=your-strong-password ...

# Docker Compose
environment:
  SPRING_SECURITY_USER_PASSWORD: ${EUREKA_PASSWORD}

# ECS (use Secrets Manager)
"secrets": [
  {
    "name": "SPRING_SECURITY_USER_PASSWORD",
    "valueFrom": "arn:aws:secretsmanager:region:account:secret:name"
  }
]
```

---

## 📊 Monitoring

### Dashboard Metrics

Access the dashboard to view:
- **Registered instances**: Number and status of services
- **Instance info**: IP, port, health status
- **Last renewal**: Time of last heartbeat
- **Lease info**: Lease expiration and renewal intervals

### Actuator Endpoints

```bash
# Health check
curl -u eureka:eureka http://localhost:8761/actuator/health

# Metrics
curl -u eureka:eureka http://localhost:8761/actuator/metrics

# Application info
curl -u eureka:eureka http://localhost:8761/actuator/info
```

### CloudWatch (ECS Deployment)

Monitor the following metrics:
- **CPU Utilization**: Should be < 50% under normal load
- **Memory Utilization**: Should be < 70%
- **Task Count**: Should match desired count
- **Health Check**: Should always pass

### Logging

```bash
# Docker logs
docker logs -f eureka-server

# Docker Compose logs
docker-compose logs -f eureka-server

# ECS logs (CloudWatch)
aws logs tail /ecs/nushungry-dev-eureka-server --follow
```

---

## 🔧 Troubleshooting

### Common Issues

#### 1. Services Not Registering

**Symptoms**: Dashboard shows no registered services

**Solutions**:
```bash
# Check client configuration
# Ensure microservices have correct Eureka URL
eureka.client.service-url.defaultZone=http://eureka:eureka@eureka-server:8761/eureka/

# Verify network connectivity
docker exec -it admin-service curl http://eureka-server:8761/actuator/health

# Check client logs
docker logs admin-service | grep Eureka
```

#### 2. Authentication Failures

**Symptoms**: 401 Unauthorized errors

**Solutions**:
```bash
# Verify credentials in client configuration
# Format: http://username:password@host:port/eureka/

# Check environment variables
docker exec eureka-server env | grep SPRING_SECURITY

# Test with curl
curl -u eureka:eureka http://localhost:8761/
```

#### 3. Self-Preservation Mode

**Symptoms**: Dashboard shows "EMERGENCY! EUREKA MAY BE INCORRECTLY CLAIMING INSTANCES ARE UP WHEN THEY'RE NOT"

**Solutions**:
```bash
# This is normal in development with few instances
# To disable (not recommended for production):
eureka.server.enable-self-preservation=false

# Or wait for more services to register (threshold is 85%)
```

#### 4. Port Conflicts

**Symptoms**: "Address already in use" error

**Solutions**:
```bash
# Check if port 8761 is in use
lsof -i :8761  # Mac/Linux
netstat -ano | findstr :8761  # Windows

# Use a different port
docker run -p 8762:8761 ...

# Or stop the conflicting process
kill -9 <PID>
```

#### 5. Memory Issues

**Symptoms**: OutOfMemoryError or container restarts

**Solutions**:
```bash
# Increase memory limit (Docker)
docker run --memory="2g" ...

# Update ECS task definition
"memory": "2048"

# Adjust JVM heap size
JAVA_OPTS="-Xmx1024m -Xms512m"
```

### Health Check Failures

```bash
# Check application logs
docker logs eureka-server | tail -50

# Verify health endpoint
curl http://localhost:8761/actuator/health

# Check dependencies
docker-compose ps

# Restart service
docker-compose restart eureka-server
```

### Debugging Tips

```bash
# Enable debug logging
docker run -e LOGGING_LEVEL_COM_NETFLIX_EUREKA=DEBUG ...

# Access container shell
docker exec -it eureka-server sh

# Check Java process
docker exec eureka-server ps aux | grep java

# View JVM memory
docker exec eureka-server jstat -gc 1
```

---

## 🔗 Related Documentation

- [Spring Cloud Netflix Eureka Documentation](https://docs.spring.io/spring-cloud-netflix/docs/current/reference/html/#service-discovery-eureka-server)
- [ARCHITECTURE.md](../docs/ARCHITECTURE.md) - Overall system architecture
- [OPERATIONS.md](../docs/OPERATIONS.md) - Operations and maintenance guide
- [README.md](../README.md) - Project overview and quick start

---

## 📝 Notes

- **High Availability**: For production, run at least 2 Eureka instances
- **Peer Discovery**: Multiple Eureka servers can replicate registry data
- **Client Caching**: Clients cache registry data and can survive Eureka outages
- **Heartbeats**: Services send heartbeats every 30 seconds by default
- **Lease Expiration**: Services are evicted after 90 seconds without heartbeat

---

## 🤝 Support

For issues or questions:
1. Check the [Troubleshooting](#troubleshooting) section
2. Review application logs
3. Consult Spring Cloud Netflix documentation
4. Open an issue in the project repository
