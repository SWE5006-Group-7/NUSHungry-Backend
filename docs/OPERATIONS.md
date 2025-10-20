# NUSHungry Backend - Operations Manual

## Table of Contents
- [Overview](#overview)
- [Deployment](#deployment)
- [Monitoring & Health Checks](#monitoring--health-checks)
- [Logging](#logging)
- [Backup & Recovery](#backup--recovery)
- [Scaling](#scaling)
- [Security Operations](#security-operations)
- [Performance Tuning](#performance-tuning)
- [Troubleshooting](#troubleshooting)
- [Maintenance](#maintenance)
- [Incident Response](#incident-response)

---

## Overview

This operations manual provides guidance for deploying, monitoring, and maintaining the NUSHungry microservices architecture in production environments.

### Service Inventory

| Service | Port | Database | Dependencies | Health Check |
|---------|------|----------|--------------|--------------|
| **admin-service** | 8082 | PostgreSQL (admin_db) | RabbitMQ | `/actuator/health` |
| **cafeteria-service** | 8083 | PostgreSQL (cafeteria_db) | RabbitMQ | `/actuator/health` |
| **review-service** | 8084 | MongoDB (review_db) | RabbitMQ | `/actuator/health` |
| **media-service** | 8085 | PostgreSQL (media_db) | MinIO | `/actuator/health` |
| **preference-service** | 8086 | PostgreSQL (preference_db) | - | `/actuator/health` |

### Infrastructure Components

| Component | Port(s) | Purpose | Management UI |
|-----------|---------|---------|---------------|
| **PostgreSQL** | 5432 | Relational database | - |
| **MongoDB** | 27017 | Document database | - |
| **RabbitMQ** | 5672, 15672 | Message broker | `http://localhost:15672` |
| **MinIO** | 9000, 9001 | Object storage | `http://localhost:9001` |

---

## Deployment

### 1. Local Development Deployment

#### Quick Start with Docker Compose

```bash
# 1. Clone and configure
git clone https://github.com/your-org/nushungry-Backend.git
cd nushungry-Backend
cp .env.example .env

# 2. Edit .env with your credentials
nano .env

# 3. Start all services
docker-compose up -d

# 4. Verify all services are healthy
docker-compose ps
docker-compose logs -f
```

#### Manual Start (Development)

```bash
# Start infrastructure only
docker-compose up -d postgres mongodb rabbitmq minio

# Start each service manually
cd admin-service && mvn spring-boot:run
cd cafeteria-service && mvn spring-boot:run
cd review-service && mvn spring-boot:run
cd media-service && mvn spring-boot:run
cd preference-service && mvn spring-boot:run
```

### 2. AWS ECS Production Deployment

#### Prerequisites

- AWS Account with ECS access
- ECR repositories for each service
- ECS Cluster configured
- RDS PostgreSQL instance
- DocumentDB (MongoDB-compatible)
- Amazon MQ (RabbitMQ) or self-hosted
- S3 bucket for media storage
- Secrets Manager for sensitive data

#### Deployment Steps

**A. Prepare Infrastructure**

```bash
# Create ECR repositories
aws ecr create-repository --repository-name nushungry/admin-service
aws ecr create-repository --repository-name nushungry/cafeteria-service
aws ecr create-repository --repository-name nushungry/review-service
aws ecr create-repository --repository-name nushungry/media-service
aws ecr create-repository --repository-name nushungry/preference-service

# Create ECS cluster
aws ecs create-cluster --cluster-name nushungry-production

# Create RDS PostgreSQL instance
aws rds create-db-instance \
  --db-instance-identifier nushungry-db \
  --db-instance-class db.t3.medium \
  --engine postgres \
  --master-username postgres \
  --master-user-password <secure-password> \
  --allocated-storage 20
```

**B. Build and Push Images**

```bash
# Login to ECR
aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin <account-id>.dkr.ecr.us-east-1.amazonaws.com

# Build and push each service
for service in admin cafeteria review media preference; do
  cd ${service}-service
  docker build -t nushungry/${service}-service:latest .
  docker tag nushungry/${service}-service:latest <account-id>.dkr.ecr.us-east-1.amazonaws.com/nushungry/${service}-service:latest
  docker push <account-id>.dkr.ecr.us-east-1.amazonaws.com/nushungry/${service}-service:latest
  cd ..
done
```

**C. Deploy ECS Services**

```bash
# Register task definitions (use files from .aws/task-definitions/)
aws ecs register-task-definition --cli-input-json file://.aws/task-definitions/admin-service.json

# Create ECS services
aws ecs create-service \
  --cluster nushungry-production \
  --service-name admin-service \
  --task-definition admin-service \
  --desired-count 2 \
  --launch-type FARGATE \
  --network-configuration "awsvpcConfiguration={subnets=[subnet-xxx],securityGroups=[sg-xxx],assignPublicIp=ENABLED}"
```

**D. Configure Load Balancer**

```bash
# Create Application Load Balancer
aws elbv2 create-load-balancer \
  --name nushungry-alb \
  --subnets subnet-xxx subnet-yyy \
  --security-groups sg-xxx

# Create target groups and register services
aws elbv2 create-target-group \
  --name admin-service-tg \
  --protocol HTTP \
  --port 8082 \
  --vpc-id vpc-xxx \
  --target-type ip \
  --health-check-path /actuator/health
```

### 3. CI/CD Deployment

The project uses GitHub Actions for automated deployments. See `.github/workflows/cd.yml`.

**Trigger Deployment:**

```bash
# Automatic: Push to main branch
git push origin main

# Manual: Trigger workflow with specific service
gh workflow run cd.yml -f service=admin-service
```

**Monitor Deployment:**

```bash
gh run list --workflow=cd.yml
gh run view <run-id> --log
```

---

## Monitoring & Health Checks

### Health Check Endpoints

Each service exposes Spring Boot Actuator endpoints:

```bash
# Check service health
curl http://localhost:8082/actuator/health

# Example response
{
  "status": "UP",
  "components": {
    "db": {"status": "UP"},
    "diskSpace": {"status": "UP"},
    "ping": {"status": "UP"}
  }
}
```

### Monitoring All Services

```bash
# Script to check all services
#!/bin/bash
services=(
  "admin-service:8082"
  "cafeteria-service:8083"
  "review-service:8084"
  "media-service:8085"
  "preference-service:8086"
)

for service in "${services[@]}"; do
  name="${service%%:*}"
  port="${service##*:}"
  status=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:${port}/actuator/health)
  if [ "$status" == "200" ]; then
    echo "✅ $name is healthy"
  else
    echo "❌ $name is down (HTTP $status)"
  fi
done
```

### Metrics Endpoints

```bash
# View all available metrics
curl http://localhost:8082/actuator/metrics

# View specific metric
curl http://localhost:8082/actuator/metrics/jvm.memory.used
curl http://localhost:8082/actuator/metrics/http.server.requests
```

### Infrastructure Monitoring

**PostgreSQL:**
```bash
# Connect to database
docker exec -it nushungry-postgres psql -U postgres

# Check active connections
SELECT count(*) FROM pg_stat_activity;

# Check database sizes
SELECT datname, pg_size_pretty(pg_database_size(datname)) FROM pg_database;
```

**MongoDB:**
```bash
# Connect to MongoDB
docker exec -it nushungry-mongodb mongosh -u admin -p admin123

# Check database stats
use review_db
db.stats()

# Check collection sizes
db.reviews.stats()
```

**RabbitMQ:**
```bash
# Web UI: http://localhost:15672 (guest/guest)

# CLI commands
docker exec nushungry-rabbitmq rabbitmqctl list_queues
docker exec nushungry-rabbitmq rabbitmqctl list_connections
```

**MinIO:**
```bash
# Web UI: http://localhost:9001 (minioadmin/minioadmin)

# CLI (mc client)
mc alias set local http://localhost:9000 minioadmin minioadmin
mc ls local/
mc du local/media-bucket
```

### Recommended Monitoring Stack (Future)

- **Prometheus** - Metrics collection
- **Grafana** - Visualization dashboards
- **ELK Stack** - Centralized logging
- **Zipkin/Jaeger** - Distributed tracing

---

## Logging

### Log Locations

**Docker Containers:**
```bash
# View service logs
docker-compose logs -f admin-service
docker-compose logs -f --tail=100 cafeteria-service

# View all services
docker-compose logs -f

# Export logs to file
docker-compose logs > logs.txt
```

**Local Development:**
```bash
# Spring Boot logs are in project root
tail -f admin-service/logs/application.log
```

### Log Levels

Configure in `application.properties`:

```properties
# Global log level
logging.level.root=INFO

# Service-specific logging
logging.level.com.nushungry=DEBUG
logging.level.org.springframework.web=DEBUG
logging.level.org.hibernate.SQL=DEBUG

# Output to file
logging.file.name=logs/application.log
logging.pattern.file=%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n
```

### Common Log Patterns

**Search for errors:**
```bash
docker-compose logs | grep ERROR
docker-compose logs | grep -i exception
```

**Filter by service and time:**
```bash
docker-compose logs --since 30m admin-service
docker-compose logs --until 2024-01-01T00:00:00
```

---

## Backup & Recovery

### Database Backups

#### PostgreSQL Backup

**Full Database Dump:**
```bash
# Backup all databases
docker exec nushungry-postgres pg_dumpall -U postgres > backup_all_$(date +%Y%m%d).sql

# Backup specific database
docker exec nushungry-postgres pg_dump -U postgres admin_db > backup_admin_$(date +%Y%m%d).sql
```

**Automated Backup Script:**
```bash
#!/bin/bash
# backup-postgres.sh

BACKUP_DIR="/backups/postgres"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
DATABASES=("admin_db" "cafeteria_db" "media_db" "preference_db")

mkdir -p $BACKUP_DIR

for db in "${DATABASES[@]}"; do
  docker exec nushungry-postgres pg_dump -U postgres $db > $BACKUP_DIR/${db}_${TIMESTAMP}.sql
  gzip $BACKUP_DIR/${db}_${TIMESTAMP}.sql
done

# Keep only last 7 days
find $BACKUP_DIR -name "*.sql.gz" -mtime +7 -delete

echo "Backup completed: $TIMESTAMP"
```

**Restore:**
```bash
# Restore database
docker exec -i nushungry-postgres psql -U postgres admin_db < backup_admin_20240101.sql
```

#### MongoDB Backup

**Dump:**
```bash
# Backup specific database
docker exec nushungry-mongodb mongodump \
  --username admin \
  --password admin123 \
  --authenticationDatabase admin \
  --db review_db \
  --out /backup

# Copy backup to host
docker cp nushungry-mongodb:/backup ./mongodb_backup_$(date +%Y%m%d)
```

**Restore:**
```bash
# Restore database
docker exec nushungry-mongodb mongorestore \
  --username admin \
  --password admin123 \
  --authenticationDatabase admin \
  --db review_db \
  /backup/review_db
```

### File Storage Backup

**MinIO/S3 Backup:**
```bash
# Backup MinIO data
mc mirror local/media-bucket ./media_backup_$(date +%Y%m%d)

# Restore
mc mirror ./media_backup_20240101 local/media-bucket
```

### Docker Volume Backup

```bash
# Backup volumes
docker run --rm \
  -v nushungry_postgres_data:/data \
  -v $(pwd):/backup \
  alpine tar czf /backup/postgres_volume_$(date +%Y%m%d).tar.gz -C /data .

# Restore
docker run --rm \
  -v nushungry_postgres_data:/data \
  -v $(pwd):/backup \
  alpine tar xzf /backup/postgres_volume_20240101.tar.gz -C /data
```

### Backup Schedule Recommendations

| Resource | Frequency | Retention | Method |
|----------|-----------|-----------|--------|
| PostgreSQL | Daily | 30 days | `pg_dump` + S3 |
| MongoDB | Daily | 30 days | `mongodump` + S3 |
| Media Files | Weekly | 90 days | MinIO mirror to S3 |
| Configuration | On change | Indefinite | Git repository |
| Docker Volumes | Weekly | 14 days | Volume backup |

---

## Scaling

### Horizontal Scaling

#### Scale Individual Services

**Docker Compose:**
```bash
# Scale specific service
docker-compose up -d --scale admin-service=3

# Scale multiple services
docker-compose up -d --scale admin-service=2 --scale cafeteria-service=3
```

**AWS ECS:**
```bash
# Update desired count
aws ecs update-service \
  --cluster nushungry-production \
  --service admin-service \
  --desired-count 3
```

#### Load Balancing

For production, use Application Load Balancer (ALB) to distribute traffic:

```
Internet → ALB → Target Group → ECS Tasks (auto-scaling)
```

### Vertical Scaling

#### Increase Container Resources

**docker-compose.yml:**
```yaml
services:
  admin-service:
    deploy:
      resources:
        limits:
          cpus: '2'
          memory: 2G
        reservations:
          cpus: '0.5'
          memory: 512M
```

**ECS Task Definition:**
```json
{
  "containerDefinitions": [{
    "name": "admin-service",
    "memory": 2048,
    "cpu": 1024
  }]
}
```

#### Increase JVM Heap

**application.properties:**
```properties
spring.application.name=admin-service
java.opts=-Xms512m -Xmx2048m -XX:MaxMetaspaceSize=256m
```

### Database Scaling

**PostgreSQL:**
- Read replicas for read-heavy workloads
- Connection pooling (HikariCP already configured)
- Partitioning large tables

**MongoDB:**
- Replica sets for high availability
- Sharding for horizontal scaling
- Index optimization

---

## Security Operations

### Secret Management

#### Rotate Secrets

**Database Passwords:**
```bash
# 1. Update password in database
docker exec -it nushungry-postgres psql -U postgres
ALTER USER postgres PASSWORD 'new_secure_password';

# 2. Update .env file
nano .env

# 3. Restart services
docker-compose restart
```

**JWT Secret Rotation:**
```bash
# 1. Generate new secret (minimum 256 bits)
openssl rand -base64 32

# 2. Update .env
JWT_SECRET=<new-secret>

# 3. Restart admin-service
docker-compose restart admin-service

# Note: This will invalidate all existing tokens
```

#### Secrets in AWS

Use AWS Secrets Manager:

```bash
# Store secret
aws secretsmanager create-secret \
  --name nushungry/postgres/password \
  --secret-string "secure-password"

# Update ECS task definition to reference secret
{
  "secrets": [
    {
      "name": "POSTGRES_PASSWORD",
      "valueFrom": "arn:aws:secretsmanager:region:account:secret:nushungry/postgres/password"
    }
  ]
}
```

### Security Scanning

**Container Scanning:**
```bash
# Scan for vulnerabilities with Snyk
snyk container test nushungry/admin-service:latest

# Scan with Trivy
trivy image nushungry/admin-service:latest
```

**Dependency Scanning:**
```bash
# OWASP Dependency Check (integrated in CI/CD)
mvn org.owasp:dependency-check-maven:check
```

### SSL/TLS Configuration

**Development (Self-signed):**
```bash
# Generate keystore
keytool -genkeypair -alias nushungry -keyalg RSA -keysize 2048 \
  -storetype PKCS12 -keystore keystore.p12 -validity 3650

# application.properties
server.ssl.enabled=true
server.ssl.key-store=classpath:keystore.p12
server.ssl.key-store-password=changeit
server.ssl.key-store-type=PKCS12
```

**Production:**
Use Let's Encrypt with Nginx reverse proxy or AWS Certificate Manager with ALB.

### Network Security

**Firewall Rules:**
```bash
# Allow only necessary ports
ufw allow 8082/tcp  # Admin service
ufw allow 8083/tcp  # Cafeteria service
# ... other services
ufw deny 5432/tcp   # Block direct database access
ufw deny 27017/tcp
```

**Docker Network Isolation:**
```yaml
# docker-compose.yml
services:
  admin-service:
    networks:
      - frontend
      - backend
  postgres:
    networks:
      - backend  # Not accessible from frontend
```

---

## Performance Tuning

### Application Performance

#### JVM Tuning

**Optimal JVM settings:**
```bash
JAVA_OPTS="-Xms1024m -Xmx2048m \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200 \
  -XX:+HeapDumpOnOutOfMemoryError \
  -XX:HeapDumpPath=/logs/heapdump.hprof"
```

#### Connection Pool Tuning

**application.properties:**
```properties
# HikariCP (default for Spring Boot)
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=30000
spring.datasource.hikari.idle-timeout=600000
spring.datasource.hikari.max-lifetime=1800000
```

#### Caching

**Enable Redis caching (future enhancement):**
```properties
spring.cache.type=redis
spring.redis.host=localhost
spring.redis.port=6379
```

### Database Performance

#### PostgreSQL Optimization

**Connection pooling:**
```sql
-- Check active connections
SELECT count(*) FROM pg_stat_activity;

-- Set max connections
ALTER SYSTEM SET max_connections = 200;
```

**Indexing:**
```sql
-- Create indexes on frequently queried columns
CREATE INDEX idx_user_email ON users(email);
CREATE INDEX idx_review_stall_id ON reviews(stall_id);
CREATE INDEX idx_favorite_user_id ON favorites(user_id);
```

**Vacuum and analyze:**
```bash
# Run manually
docker exec nushungry-postgres psql -U postgres -c "VACUUM ANALYZE;"

# Enable autovacuum (should be on by default)
ALTER SYSTEM SET autovacuum = on;
```

#### MongoDB Optimization

**Indexing:**
```javascript
// Create indexes
db.reviews.createIndex({ stallId: 1 })
db.reviews.createIndex({ userId: 1, createdAt: -1 })
db.reviews.createIndex({ stallId: 1, rating: -1 })
```

**Query performance:**
```javascript
// Analyze slow queries
db.setProfilingLevel(2)  // Profile all queries
db.system.profile.find().sort({ millis: -1 }).limit(10)
```

### RabbitMQ Performance

**Configuration:**
```properties
# Consumer prefetch
spring.rabbitmq.listener.simple.prefetch=10

# Connection pooling
spring.rabbitmq.cache.channel.size=25
spring.rabbitmq.cache.connection.size=10
```

**Monitoring:**
```bash
# Check queue depth
docker exec nushungry-rabbitmq rabbitmqctl list_queues

# Check consumer throughput
docker exec nushungry-rabbitmq rabbitmqctl list_consumers
```

---

## Troubleshooting

### Common Issues

#### Service Won't Start

**Symptom:** Service fails to start or crashes immediately

**Diagnosis:**
```bash
# Check logs
docker-compose logs admin-service

# Check health
docker ps -a
docker inspect admin-service
```

**Common Causes:**
1. **Port conflict:** Another service using the same port
   ```bash
   netstat -ano | findstr :8082  # Windows
   lsof -i :8082  # Linux/Mac
   ```
   **Solution:** Stop conflicting service or change port

2. **Database not ready:** Service starts before database
   **Solution:** Add health check dependencies in docker-compose.yml

3. **Environment variable missing:** Required config not set
   **Solution:** Check .env file matches .env.example

#### Database Connection Failed

**Symptom:** `Unable to acquire JDBC Connection`

**Diagnosis:**
```bash
# Test database connectivity
docker exec -it nushungry-postgres psql -U postgres -c "\l"

# Check database logs
docker-compose logs postgres
```

**Solutions:**
1. Verify credentials in .env
2. Ensure database is created: `CREATE DATABASE admin_db;`
3. Check firewall rules
4. Verify network connectivity: `docker network inspect nushungry-backend_nushungry-network`

#### RabbitMQ Connection Issues

**Symptom:** `IOException: Connection refused`

**Diagnosis:**
```bash
# Check RabbitMQ status
docker exec nushungry-rabbitmq rabbitmqctl status

# Check connections
docker exec nushungry-rabbitmq rabbitmqctl list_connections
```

**Solutions:**
1. Verify RabbitMQ is running: `docker ps | grep rabbitmq`
2. Check credentials in .env
3. Verify queue exists: `docker exec nushungry-rabbitmq rabbitmqctl list_queues`

#### High Memory Usage

**Symptom:** Container using excessive memory

**Diagnosis:**
```bash
# Check memory usage
docker stats

# Check JVM memory
curl http://localhost:8082/actuator/metrics/jvm.memory.used
```

**Solutions:**
1. Reduce JVM heap size: `-Xmx1024m`
2. Enable G1GC: `-XX:+UseG1GC`
3. Check for memory leaks: Heap dump analysis
4. Scale horizontally instead of vertically

#### Slow API Responses

**Symptom:** High response times (>2 seconds)

**Diagnosis:**
```bash
# Check metrics
curl http://localhost:8082/actuator/metrics/http.server.requests

# Check database slow queries (PostgreSQL)
docker exec -it nushungry-postgres psql -U postgres -c "
SELECT query, mean_exec_time, calls 
FROM pg_stat_statements 
ORDER BY mean_exec_time DESC 
LIMIT 10;"
```

**Solutions:**
1. Add database indexes
2. Enable caching
3. Optimize N+1 queries (use JOIN or batch loading)
4. Enable connection pooling
5. Scale service horizontally

### Debug Mode

**Enable debug logging:**
```properties
# application.properties
logging.level.com.nushungry=DEBUG
logging.level.org.hibernate.SQL=DEBUG
logging.level.org.springframework.security=DEBUG
```

**Remote debugging:**
```bash
# Start service with debug port
java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005 -jar app.jar

# Connect from IDE to localhost:5005
```

---

## Maintenance

### Routine Maintenance Tasks

#### Daily
- ✅ Check service health endpoints
- ✅ Review error logs
- ✅ Monitor disk space usage

#### Weekly
- ✅ Review database slow queries
- ✅ Check RabbitMQ queue depths
- ✅ Backup verification (test restore)
- ✅ Update dependency vulnerabilities

#### Monthly
- ✅ Database vacuum and analyze
- ✅ Review and archive old logs
- ✅ Security patch updates
- ✅ Performance benchmark testing
- ✅ Capacity planning review

### Service Updates

**Zero-Downtime Deployment:**
```bash
# 1. Deploy new version alongside old
docker-compose up -d --scale admin-service=2 admin-service-v2

# 2. Health check new version
curl http://localhost:8082/actuator/health

# 3. Update load balancer to route to new version

# 4. Scale down old version
docker-compose stop admin-service
```

### Database Migrations

**Flyway (recommended for future):**
```properties
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration
```

**Manual migration:**
```bash
# 1. Backup database
docker exec nushungry-postgres pg_dump -U postgres admin_db > backup_pre_migration.sql

# 2. Apply migration script
docker exec -i nushungry-postgres psql -U postgres admin_db < migration_v2.sql

# 3. Verify
docker exec -it nushungry-postgres psql -U postgres admin_db -c "\dt"
```

---

## Incident Response

### Incident Severity Levels

| Level | Description | Response Time | Example |
|-------|-------------|---------------|---------|
| **P0 - Critical** | Complete service outage | < 15 minutes | All services down |
| **P1 - High** | Major functionality broken | < 1 hour | Authentication service down |
| **P2 - Medium** | Degraded performance | < 4 hours | Slow API responses |
| **P3 - Low** | Minor issue | < 24 hours | Non-critical feature bug |

### Incident Response Playbook

#### P0 - Complete Outage

1. **Detect & Alert**
   - Health check failure
   - Monitoring alert triggered

2. **Immediate Actions**
   ```bash
   # Check all services
   docker-compose ps
   
   # View recent logs
   docker-compose logs --tail=100 --since=10m
   
   # Check infrastructure
   docker exec nushungry-postgres pg_isready -U postgres
   docker exec nushungry-rabbitmq rabbitmqctl status
   ```

3. **Rollback (if deployment-related)**
   ```bash
   # Rollback to previous version
   docker-compose down
   git checkout <previous-stable-tag>
   docker-compose up -d
   ```

4. **Escalation**
   - Notify on-call engineer
   - Update status page
   - Inform stakeholders

5. **Post-Mortem**
   - Root cause analysis
   - Document incident
   - Implement preventive measures

#### Emergency Contacts

```
On-Call Engineer: [Phone/Email]
DevOps Lead: [Phone/Email]
Database Admin: [Phone/Email]
AWS Support: [Support Case URL]
```

---

## Appendix

### Useful Commands Cheat Sheet

```bash
# Service Management
docker-compose up -d                    # Start all services
docker-compose down                     # Stop all services
docker-compose restart admin-service    # Restart service
docker-compose logs -f                  # View logs

# Health Checks
curl http://localhost:8082/actuator/health
docker-compose ps

# Database
docker exec -it nushungry-postgres psql -U postgres
docker exec -it nushungry-mongodb mongosh -u admin -p admin123

# RabbitMQ
docker exec nushungry-rabbitmq rabbitmqctl list_queues
docker exec nushungry-rabbitmq rabbitmqctl list_consumers

# Cleanup
docker system prune -a                  # Remove unused containers/images
docker volume prune                     # Remove unused volumes
```

### External Resources

- [Spring Boot Production-Ready Features](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)
- [Docker Production Best Practices](https://docs.docker.com/develop/dev-best-practices/)
- [PostgreSQL Performance Tuning](https://wiki.postgresql.org/wiki/Performance_Optimization)
- [MongoDB Production Notes](https://docs.mongodb.com/manual/administration/production-notes/)
- [RabbitMQ Best Practices](https://www.rabbitmq.com/production-checklist.html)

---

**Document Version:** 1.0  
**Last Updated:** 2024-01-01  
**Maintained By:** NUSHungry DevOps Team
