# Config Server Deployment Guide

Complete deployment guide for the NUSHungry Config Server - centralized configuration management for all microservices.

## Table of Contents
- [Quick Start](#quick-start)
- [Local Development](#local-development)
- [Docker Deployment](#docker-deployment)
- [Production Deployment (AWS ECS)](#production-deployment-aws-ecs)
- [Configuration Repository Setup](#configuration-repository-setup)
- [Client Configuration](#client-configuration)
- [Testing](#testing)
- [Troubleshooting](#troubleshooting)
- [Security](#security)

---

## Quick Start

### Prerequisites
- JDK 17 or higher
- Maven 3.8+
- Docker and Docker Compose
- Git

### Start with Docker Compose

```bash
# Clone the repository
cd config-server

# Start services
./scripts/start-services.sh  # Linux/Mac
scripts\start-services.bat    # Windows

# Access Config Server
curl -u config:config123 http://localhost:8888/application/default
```

---

## Local Development

### 1. Setup Local Config Repository

Create a local Git repository for configurations:

```bash
# Create repository directory
mkdir ~/nushungry-config
cd ~/nushungry-config

# Initialize Git
git init

# Create common configuration
cat > application.yml << 'EOF'
# Common configuration for all services
spring:
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics

logging:
  level:
    root: INFO
    com.nushungry: DEBUG
EOF

# Commit
git add .
git commit -m "Initial configuration"
```

### 2. Configure Config Server

Update `src/main/resources/application.yml`:

```yaml
spring:
  cloud:
    config:
      server:
        git:
          uri: file:///Users/yourname/nushungry-config
          # Or use absolute path on Windows:
          # uri: file:///C:/Users/yourname/nushungry-config
```

### 3. Run Config Server

```bash
# Build
mvn clean install

# Run
mvn spring-boot:run

# Or run with specific profile
mvn spring-boot:run -Dspring-boot.run.profiles=native
```

Config Server will start on port 8888.

### 4. Test Configuration Access

```bash
# Test with curl
curl -u config:config123 http://localhost:8888/application/default

# Test specific service configuration
curl -u config:config123 http://localhost:8888/admin-service/dev

# Get raw YAML file
curl -u config:config123 http://localhost:8888/admin-service/dev/main/admin-service.yml
```

---

## Docker Deployment

### 1. Build Docker Image

```bash
# From config-server directory
docker build -t nushungry/config-server:latest .

# Or with specific tag
docker build -t nushungry/config-server:1.0.0 .
```

### 2. Run with Docker

```bash
# Run standalone (with native profile)
docker run -d \
  --name config-server \
  -p 8888:8888 \
  -e SPRING_PROFILES_ACTIVE=native \
  -e CONFIG_SERVER_USERNAME=config \
  -e CONFIG_SERVER_PASSWORD=config123 \
  -v $(pwd)/config-repo:/config-repo:ro \
  nushungry/config-server:latest

# Or with Git repository
docker run -d \
  --name config-server \
  -p 8888:8888 \
  -e SPRING_PROFILES_ACTIVE=git \
  -e CONFIG_GIT_URI=https://github.com/your-org/nushungry-config.git \
  -e CONFIG_GIT_BRANCH=main \
  -e GIT_USERNAME=your-username \
  -e GIT_TOKEN=your-token \
  nushungry/config-server:latest
```

### 3. Run with Docker Compose

```bash
# Start all services
docker-compose up -d

# View logs
docker-compose logs -f config-server

# Stop services
docker-compose down
```

### 4. Health Check

```bash
# Check health status
curl http://localhost:8888/actuator/health

# Expected response:
# {"status":"UP"}
```

---

## Production Deployment (AWS ECS)

### Prerequisites
- AWS CLI configured
- ECR repository created
- ECS cluster set up
- Task definition prepared
- Secrets Manager for sensitive data

### 1. Push Image to ECR

```bash
# Login to ECR
aws ecr get-login-password --region us-east-1 | \
  docker login --username AWS --password-stdin <aws-account-id>.dkr.ecr.us-east-1.amazonaws.com

# Tag image
docker tag nushungry/config-server:latest \
  <aws-account-id>.dkr.ecr.us-east-1.amazonaws.com/nushungry/config-server:latest

# Push to ECR
docker push <aws-account-id>.dkr.ecr.us-east-1.amazonaws.com/nushungry/config-server:latest
```

### 2. Create Task Definition

See `.aws/task-definitions/config-server.json` for complete task definition.

Key configuration:
```json
{
  "containerDefinitions": [
    {
      "name": "config-server",
      "image": "<ecr-image-uri>",
      "portMappings": [
        {
          "containerPort": 8888,
          "protocol": "tcp"
        }
      ],
      "environment": [
        {
          "name": "SPRING_PROFILES_ACTIVE",
          "value": "git"
        }
      ],
      "secrets": [
        {
          "name": "CONFIG_SERVER_PASSWORD",
          "valueFrom": "arn:aws:secretsmanager:..."
        },
        {
          "name": "CONFIG_GIT_URI",
          "valueFrom": "arn:aws:secretsmanager:..."
        }
      ]
    }
  ]
}
```

### 3. Deploy to ECS

```bash
# Register task definition
aws ecs register-task-definition \
  --cli-input-json file://.aws/task-definitions/config-server.json

# Update service
aws ecs update-service \
  --cluster nushungry-cluster \
  --service config-server \
  --task-definition config-server:latest \
  --force-new-deployment

# Wait for deployment
aws ecs wait services-stable \
  --cluster nushungry-cluster \
  --services config-server
```

### 4. Configure Load Balancer

- Create Application Load Balancer
- Configure target group (port 8888)
- Set up health check: `/actuator/health`
- Configure SSL certificate
- Update security groups

---

## Configuration Repository Setup

### Repository Structure

```
nushungry-config/
├── application.yml                    # Common config
├── application-dev.yml                # Dev environment
├── application-staging.yml            # Staging environment
├── application-prod.yml               # Production environment
├── admin-service.yml                  # Service-specific
├── admin-service-dev.yml
├── admin-service-prod.yml
├── cafeteria-service.yml
├── review-service.yml
├── media-service.yml
├── preference-service.yml
├── gateway-service.yml
└── eureka-server.yml
```

### Configuration Hierarchy

Config Server loads configurations in this order (later overrides earlier):

1. `application.yml` - Common properties
2. `application-{profile}.yml` - Environment-specific common
3. `{service-name}.yml` - Service-specific
4. `{service-name}-{profile}.yml` - Service + environment specific

### Example Configurations

#### application.yml (Common)
```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
  endpoint:
    health:
      show-details: always

logging:
  level:
    root: INFO
    com.nushungry: DEBUG
```

#### application-prod.yml (Production)
```yaml
spring:
  jpa:
    show-sql: false
    hibernate:
      ddl-auto: validate

logging:
  level:
    root: WARN
    com.nushungry: INFO
```

#### admin-service.yml (Service-specific)
```yaml
server:
  port: 8082

spring:
  application:
    name: admin-service
  datasource:
    driver-class-name: org.postgresql.Driver
    url: jdbc:postgresql://${DB_HOST:localhost}:5432/admin_db
    username: ${DB_USERNAME:postgres}
    password: ${DB_PASSWORD}

jwt:
  secret: ${JWT_SECRET}
  expiration: 86400000
```

### Using Environment Variables

Always use environment variables for sensitive data:

```yaml
spring:
  datasource:
    password: ${DB_PASSWORD}
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/mydb

jwt:
  secret: ${JWT_SECRET}

rabbitmq:
  password: ${RABBITMQ_PASSWORD}
```

---

## Client Configuration

### 1. Add Dependencies

Add to microservice `pom.xml`:

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-config</artifactId>
</dependency>

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

### 2. Create bootstrap.yml

Create `src/main/resources/bootstrap.yml` in each microservice:

```yaml
spring:
  application:
    name: admin-service  # Must match config file name
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

### 3. Enable Configuration Refresh

Add `@RefreshScope` to beans that should be reloadable:

```java
@Service
@RefreshScope
public class MyService {
    @Value("${my.property}")
    private String myProperty;
}
```

### 4. Trigger Configuration Refresh

```bash
# Refresh single service
curl -X POST http://localhost:8082/actuator/refresh

# Or use Spring Cloud Bus for broadcasting (future enhancement)
```

---

## Testing

### Unit Tests

```bash
# Run all tests
mvn test

# Run specific test
mvn test -Dtest=ConfigServerApplicationTest
```

### Integration Tests

```bash
# Run integration tests
mvn verify
```

### Manual Testing

```bash
# 1. Test health endpoint (no auth required)
curl http://localhost:8888/actuator/health

# 2. Test authentication
curl -u config:config123 http://localhost:8888/application/default

# 3. Test wrong credentials
curl -u wrong:wrong http://localhost:8888/application/default
# Expected: 401 Unauthorized

# 4. Test service-specific config
curl -u config:config123 http://localhost:8888/admin-service/dev

# 5. Test configuration refresh
curl -u config:config123 http://localhost:8888/application/default
# Modify config in Git repository
curl -u config:config123 http://localhost:8888/application/default
# Should return updated configuration

# 6. Test with different profiles
curl -u config:config123 http://localhost:8888/admin-service/prod
curl -u config:config123 http://localhost:8888/admin-service/staging
```

### Load Testing

```bash
# Install Apache Bench
# Ubuntu: sudo apt-get install apache2-utils
# Mac: brew install httpd

# Run load test
ab -n 1000 -c 10 -A config:config123 \
  http://localhost:8888/application/default
```

---

## Troubleshooting

### Config Server Won't Start

**Problem**: Service fails to start

**Solutions**:
```bash
# Check logs
docker-compose logs config-server

# Common issues:
# 1. Port 8888 already in use
netstat -an | grep 8888
# Kill process or use different port

# 2. Git repository not accessible
# Check CONFIG_GIT_URI
# Verify credentials
# Check network connectivity

# 3. Dependency conflicts
mvn dependency:tree
mvn clean install -U
```

### Can't Access Configuration

**Problem**: Microservice can't fetch configuration

**Solutions**:
```bash
# 1. Verify Config Server is running
curl http://localhost:8888/actuator/health

# 2. Test authentication
curl -u config:config123 http://localhost:8888/application/default

# 3. Check bootstrap.yml in microservice
# Ensure spring.application.name matches config file name

# 4. Enable debug logging
LOGGING_LEVEL_ORG_SPRINGFRAMEWORK_CLOUD_CONFIG=DEBUG

# 5. Check network connectivity
ping config-server  # If using Docker
```

### Configuration Not Refreshing

**Problem**: Changes in Git not reflected

**Solutions**:
```bash
# 1. Force pull from Git
# Set CONFIG_FORCE_PULL=true

# 2. Clear config server cache
# Restart config server

# 3. Check Git repository
cd ~/nushungry-config
git log -1
git pull origin main

# 4. Trigger manual refresh
curl -X POST http://localhost:8082/actuator/refresh
```

### Authentication Failures

**Problem**: 401 Unauthorized errors

**Solutions**:
```bash
# 1. Check credentials
# Default: config/config123

# 2. Verify environment variables
echo $CONFIG_SERVER_USERNAME
echo $CONFIG_SERVER_PASSWORD

# 3. Check security configuration
# Review SecurityConfig.java

# 4. Test with curl
curl -v -u config:config123 http://localhost:8888/application/default
```

---

## Security

### Production Security Checklist

- [ ] **Change default credentials**
  ```yaml
  spring:
    security:
      user:
        name: ${CONFIG_SERVER_USERNAME}
        password: ${CONFIG_SERVER_PASSWORD}
  ```

- [ ] **Use HTTPS**
  - Configure SSL certificate
  - Redirect HTTP to HTTPS
  - Update all client URIs to `https://`

- [ ] **Encrypt sensitive properties**
  ```bash
  # Generate encryption key
  export ENCRYPT_KEY=my-secret-key

  # Encrypt value
  curl http://localhost:8888/encrypt -d "sensitive-value"

  # Use in config file
  password: '{cipher}AQAEnFjknLZKJ...'
  ```

- [ ] **Use AWS Secrets Manager**
  - Store credentials in Secrets Manager
  - Reference in task definition
  - Rotate credentials regularly

- [ ] **Restrict Git repository access**
  - Use private repository
  - Deploy keys or SSH keys
  - Limit access to config team only

- [ ] **Network security**
  - Config Server in private subnet
  - Security groups restrict access
  - VPC peering if needed

- [ ] **Audit logging**
  - Enable CloudWatch logs
  - Log all config access
  - Monitor for suspicious activity

### Encryption Setup

```yaml
# Add to application.yml
encrypt:
  key: ${ENCRYPT_KEY}

# Or use key store
encrypt:
  key-store:
    location: classpath:/config-server.jks
    password: ${KEYSTORE_PASSWORD}
    alias: config-server-key
    secret: ${KEY_PASSWORD}
```

---

## API Reference

### Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/actuator/health` | GET | Health check (public) |
| `/actuator/info` | GET | Application info |
| `/actuator/metrics` | GET | Metrics |
| `/{application}/{profile}` | GET | Get configuration |
| `/{application}/{profile}/{label}` | GET | Get config from branch |
| `/encrypt` | POST | Encrypt value |
| `/decrypt` | POST | Decrypt value |

### Configuration URL Patterns

```
/{application}/{profile}[/{label}]
/{application}-{profile}.yml
/{label}/{application}-{profile}.yml
/{application}-{profile}.properties
/{label}/{application}-{profile}.properties
```

Examples:
```
http://localhost:8888/admin-service/dev
http://localhost:8888/admin-service/prod
http://localhost:8888/admin-service/dev/main
http://localhost:8888/admin-service-dev.yml
```

---

## Monitoring

### Health Checks

```bash
# Basic health check
curl http://localhost:8888/actuator/health

# Detailed health
curl -u config:config123 http://localhost:8888/actuator/health
```

### Metrics

```bash
# JVM metrics
curl -u config:config123 http://localhost:8888/actuator/metrics/jvm.memory.used

# HTTP requests
curl -u config:config123 http://localhost:8888/actuator/metrics/http.server.requests
```

### Logs

```bash
# Docker logs
docker logs config-server -f

# Docker Compose logs
docker-compose logs -f config-server

# Logs location in container
/var/log/config-server/
```

---

## Performance Tuning

### JVM Options

```yaml
# docker-compose.yml
environment:
  - JAVA_OPTS=-Xmx512m -Xms256m -XX:+UseG1GC
```

### Caching

Config Server caches configurations by default. Adjust cache settings:

```yaml
spring:
  cloud:
    config:
      server:
        git:
          refresh-rate: 30  # Seconds between Git pulls
          force-pull: false  # Don't force pull on every request
```

### Connection Pooling

For high-traffic scenarios, tune connection pools:

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
```

---

## Backup and Recovery

### Backup Configuration Repository

```bash
# Clone repository
git clone https://github.com/your-org/nushungry-config.git backup-$(date +%Y%m%d)

# Or create backup branch
cd nushungry-config
git checkout -b backup-$(date +%Y%m%d)
git push origin backup-$(date +%Y%m%d)
```

### Disaster Recovery

1. **Config Server Failure**
   - Deploy new instance from image
   - Point to same Git repository
   - Update DNS/Load balancer

2. **Git Repository Corruption**
   - Restore from backup
   - Roll back to known good commit
   - Update Config Server URI if needed

3. **Complete System Failure**
   - Restore from infrastructure as code (Terraform)
   - Deploy Config Server first
   - Then deploy dependent services

---

## Support and Maintenance

### Regular Maintenance Tasks

**Daily**:
- Monitor health checks
- Review error logs
- Check service metrics

**Weekly**:
- Review configuration changes
- Update dependencies
- Check for security updates

**Monthly**:
- Rotate credentials
- Review and cleanup old configurations
- Performance tuning review

### Getting Help

- Check logs: `docker-compose logs config-server`
- Review documentation: [Spring Cloud Config](https://cloud.spring.io/spring-cloud-config)
- GitHub issues: [Project Issues](https://github.com/your-org/nushungry/issues)

---

## Version History

- **1.0.0** (2025-10-20): Initial release
  - Basic Config Server setup
  - Git repository integration
  - Security configuration
  - Docker support
  - Eureka integration

---

## License

Copyright © 2025 NUSHungry Team. All rights reserved.
