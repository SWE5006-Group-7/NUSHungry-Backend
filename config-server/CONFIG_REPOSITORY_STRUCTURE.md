# Configuration Repository Structure

This document describes the structure and organization of the configuration repository used by the Config Server.

## Repository Structure

```
nushungry-config/
├── application.yml                    # Common configuration for all services
├── application-dev.yml                # Development environment
├── application-staging.yml            # Staging environment
├── application-prod.yml               # Production environment
├── admin-service/
│   ├── admin-service.yml             # Service-specific configuration
│   ├── admin-service-dev.yml         # Dev overrides
│   ├── admin-service-staging.yml     # Staging overrides
│   └── admin-service-prod.yml        # Production overrides
├── cafeteria-service/
│   ├── cafeteria-service.yml
│   ├── cafeteria-service-dev.yml
│   ├── cafeteria-service-staging.yml
│   └── cafeteria-service-prod.yml
├── review-service/
│   ├── review-service.yml
│   ├── review-service-dev.yml
│   ├── review-service-staging.yml
│   └── review-service-prod.yml
├── media-service/
│   ├── media-service.yml
│   ├── media-service-dev.yml
│   ├── media-service-staging.yml
│   └── media-service-prod.yml
├── preference-service/
│   ├── preference-service.yml
│   ├── preference-service-dev.yml
│   ├── preference-service-staging.yml
│   └── preference-service-prod.yml
├── gateway-service/
│   ├── gateway-service.yml
│   ├── gateway-service-dev.yml
│   ├── gateway-service-staging.yml
│   └── gateway-service-prod.yml
└── eureka-server/
    ├── eureka-server.yml
    ├── eureka-server-dev.yml
    ├── eureka-server-staging.yml
    └── eureka-server-prod.yml
```

## Configuration Hierarchy

Spring Cloud Config follows a hierarchical loading order:

1. **application.yml** - Loaded first, contains common properties
2. **application-{profile}.yml** - Environment-specific common properties
3. **{application-name}.yml** - Service-specific properties
4. **{application-name}-{profile}.yml** - Service and environment-specific properties

Properties loaded later override earlier ones.

## Example Configuration Files

### application.yml (Common)
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

logging:
  level:
    root: INFO
    com.nushungry: DEBUG
```

### application-dev.yml (Development)
```yaml
spring:
  jpa:
    show-sql: true
    hibernate:
      ddl-auto: update

logging:
  level:
    root: DEBUG
```

### admin-service.yml (Service-specific)
```yaml
server:
  port: 8082

spring:
  application:
    name: admin-service
  datasource:
    driver-class-name: org.postgresql.Driver

jwt:
  secret: ${JWT_SECRET}
  expiration: 86400000
```

### admin-service-prod.yml (Production)
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5

logging:
  level:
    com.nushungry: INFO
```

## Environment Variables

Sensitive information should be stored in environment variables or secrets management:

- Database passwords: `${DB_PASSWORD}`
- JWT secrets: `${JWT_SECRET}`
- API keys: `${API_KEY}`
- RabbitMQ credentials: `${RABBITMQ_PASSWORD}`
- Redis passwords: `${REDIS_PASSWORD}`

## Setup Instructions

### 1. Create Local Config Repository (Development)

```bash
# Create local Git repository
mkdir ~/nushungry-config
cd ~/nushungry-config
git init

# Create configuration files
touch application.yml
touch application-dev.yml
# ... create other configuration files

# Commit files
git add .
git commit -m "Initial configuration"
```

### 2. Create Remote Config Repository (Production)

```bash
# Create a private GitHub repository
# Clone it locally
git clone https://github.com/your-org/nushungry-config.git
cd nushungry-config

# Add configuration files
# ...

# Push to remote
git add .
git commit -m "Initial configuration"
git push origin main
```

### 3. Configure Config Server

Update `config-server/src/main/resources/application.yml`:

```yaml
spring:
  cloud:
    config:
      server:
        git:
          uri: https://github.com/your-org/nushungry-config.git
          default-label: main
          username: ${GIT_USERNAME}
          password: ${GIT_TOKEN}
```

## Accessing Configurations

### Via Config Server API

```bash
# Get configuration for admin-service in dev environment
curl http://config:config123@localhost:8888/admin-service/dev

# Get configuration for specific profile
curl http://config:config123@localhost:8888/admin-service/staging

# Get raw YAML file
curl http://config:config123@localhost:8888/admin-service/dev/main/admin-service.yml
```

### Via Microservice

Microservices automatically fetch configuration on startup by adding:

```yaml
# bootstrap.yml in each microservice
spring:
  cloud:
    config:
      uri: http://localhost:8888
      username: config
      password: config123
      fail-fast: true
```

## Configuration Refresh

### Manual Refresh

```bash
# Trigger refresh for a specific service
curl -X POST http://localhost:8082/actuator/refresh
```

### Automatic Refresh with Spring Cloud Bus

Future enhancement - will allow broadcasting configuration changes to all services.

## Security Best Practices

1. **Never commit sensitive data** to the Git repository
2. **Use environment variables** for passwords and secrets
3. **Encrypt sensitive properties** using Spring Cloud Config encryption
4. **Use HTTPS** for remote Git repositories
5. **Restrict access** to the config repository
6. **Enable authentication** on Config Server
7. **Use separate repositories** for different environments if needed

## Encryption/Decryption

Spring Cloud Config supports encryption of sensitive properties:

```yaml
# Encrypted value (use {cipher} prefix)
jwt:
  secret: '{cipher}FKSAJDFGYOS8F7GLHAKERGFHLSAJ'
```

To encrypt a value:
```bash
curl http://config:config123@localhost:8888/encrypt -d "my-secret-value"
```

## Migration Checklist

- [ ] Create config repository (local or remote)
- [ ] Add application.yml with common properties
- [ ] Create environment-specific files (dev, staging, prod)
- [ ] Create service-specific configuration files
- [ ] Replace hardcoded properties with ${ENV_VAR} placeholders
- [ ] Update microservices to use Config Server
- [ ] Test configuration loading
- [ ] Set up CI/CD to deploy config changes
- [ ] Document all configuration properties

## Troubleshooting

### Config Server can't connect to Git repository
- Check Git URI in application.yml
- Verify credentials (username/token)
- Ensure network connectivity
- Check repository permissions

### Microservice can't fetch configuration
- Verify Config Server is running
- Check Config Server URL in bootstrap.yml
- Verify authentication credentials
- Check service name matches configuration files
- Review Config Server logs for errors

### Configuration not refreshing
- Ensure `/actuator/refresh` endpoint is enabled
- Verify @RefreshScope annotation on beans
- Check if properties are externalized correctly
- Consider using Spring Cloud Bus for automatic refresh
