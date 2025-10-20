# NUSHungry Backend - Microservices Architecture

Backend system for the NUSHungry application, implemented using a microservices architecture with Spring Boot, Docker, and message-driven communication.

## 📋 Table of Contents
- [System Architecture](#system-architecture)
- [Microservices Overview](#microservices-overview)
- [Technology Stack](#technology-stack)
- [Quick Start](#quick-start)
- [Project Structure](#project-structure)
- [Documentation](#documentation)

---

## 🏗️ System Architecture

The NUSHungry backend has been refactored from a monolithic architecture to a **microservices architecture**, providing better scalability, maintainability, and independent deployment capabilities.

```mermaid
graph TB
    subgraph "Client Layer"
        Client[Web/Mobile Client]
    end
    
    subgraph "Microservices"
        Admin[admin-service<br/>:8082]
        Cafeteria[cafeteria-service<br/>:8083]
        Review[review-service<br/>:8084]
        Media[media-service<br/>:8085]
        Preference[preference-service<br/>:8086]
    end
    
    subgraph "Data Layer"
        PG1[(PostgreSQL<br/>Admin DB<br/>:5432)]
        PG2[(PostgreSQL<br/>Cafeteria DB<br/>:5433)]
        PG3[(PostgreSQL<br/>Media DB<br/>:5434)]
        PG4[(PostgreSQL<br/>Preference DB<br/>:5435)]
        Mongo[(MongoDB<br/>Review DB<br/>:27017)]
    end
    
    subgraph "Infrastructure"
        RabbitMQ[RabbitMQ<br/>:5672, 15672]
        MinIO[MinIO<br/>:9000, 9001]
    end
    
    Client --> Admin
    Client --> Cafeteria
    Client --> Review
    Client --> Media
    Client --> Preference
    
    Admin --> PG1
    Admin --> RabbitMQ
    Cafeteria --> PG2
    Cafeteria --> RabbitMQ
    Review --> Mongo
    Review --> RabbitMQ
    Media --> PG3
    Media --> MinIO
    Preference --> PG4
    
    Review -.Event.-> RabbitMQ
    RabbitMQ -.Event.-> Cafeteria
```

### Architecture Principles
- **Service Independence**: Each microservice has its own database and can be deployed independently
- **Event-Driven**: Services communicate asynchronously via RabbitMQ for loose coupling
- **Polyglot Persistence**: PostgreSQL for relational data, MongoDB for document-based reviews
- **API-First**: RESTful APIs with Swagger/OpenAPI documentation
- **Containerization**: Docker-based deployment for consistency across environments

---

## 🚀 Microservices Overview

| Service | Port | Database | Status | Description |
|---------|------|----------|--------|-------------|
| **admin-service** | 8082 | PostgreSQL (5432) | ✅ Production | User management, authentication (JWT), admin dashboard |
| **cafeteria-service** | 8083 | PostgreSQL (5433) | ✅ Production | Cafeteria and stall management, ratings aggregation |
| **review-service** | 8084 | MongoDB (27017) | ✅ Production | Review creation, likes, comments (event publisher) |
| **media-service** | 8085 | PostgreSQL (5434) | ✅ Production | Image/file uploads, processing, storage (MinIO) |
| **preference-service** | 8086 | PostgreSQL (5435) | ✅ Production | User favorites, search history |

### Key Features by Service

#### 🔐 admin-service
- JWT-based authentication & authorization
- User CRUD operations
- Role-based access control (Admin/User)
- Dashboard statistics aggregation
- Password reset with email verification
- RabbitMQ event consumption

#### 🍽️ cafeteria-service
- Cafeteria and stall information management
- Geographic location support (coordinates)
- Operating hours management
- Real-time rating aggregation (via RabbitMQ events)
- Image association with cafeterias/stalls
- Advanced search and filtering

#### ⭐ review-service
- Review creation, update, deletion
- Like/unlike functionality
- Comment system (nested replies)
- MongoDB for flexible document storage
- Event publishing to RabbitMQ (rating updates)
- Full-text search capabilities

#### 📸 media-service
- Multi-format image upload (JPEG, PNG, WebP)
- Image processing (resizing, compression)
- MinIO object storage integration
- File metadata tracking
- Association with cafeterias/stalls/reviews

#### ❤️ preference-service
- User favorites management
- Search history tracking
- Batch operations (add/remove multiple favorites)
- Privacy-focused (user data isolation)

---

## 🛠️ Technology Stack

### Core Framework
- **Spring Boot**: 3.2.3
- **Java**: 17 (LTS)
- **Build Tool**: Maven

### Databases
- **PostgreSQL**: 14+ (Relational data)
- **MongoDB**: 6.0+ (Document storage)

### Message Queue
- **RabbitMQ**: 3.12+ (Async communication)

### Storage
- **MinIO**: Latest (Object storage for media files)

### Containerization
- **Docker**: 20.10+
- **Docker Compose**: 2.x

### Security
- **Spring Security**: JWT authentication
- **BCrypt**: Password hashing

### Documentation
- **Swagger/OpenAPI**: 3.0 (API documentation)

### Testing
- **JUnit 5**: Unit testing
- **Mockito**: Mocking framework
- **TestContainers**: Integration testing

### CI/CD
- **GitHub Actions**: Automated CI/CD pipeline
- **AWS ECS**: Production deployment

---

## ⚡ Quick Start

### Prerequisites

- **Java**: 17 or higher
- **Maven**: 3.8+
- **Docker**: 20.10+ (with Docker Compose)
- **Git**: For version control

### 🚀 Option 1: Docker Compose (Recommended)

Start all microservices and infrastructure with a single command:

```bash
# 1. Clone the repository
git clone <repository-url>
cd nushungry-Backend

# 2. Copy environment variables template
cp .env.example .env

# 3. Start all services
docker-compose up -d

# 4. Check service health
docker-compose ps
```

**Service URLs:**
- Admin Service: http://localhost:8082
- Cafeteria Service: http://localhost:8083
- Review Service: http://localhost:8084
- Media Service: http://localhost:8085
- Preference Service: http://localhost:8086
- RabbitMQ Management: http://localhost:15672 (guest/guest)
- MinIO Console: http://localhost:9001 (minioadmin/minioadmin)

### 🔧 Option 2: Run Individual Services

Each service can be run independently for development:

```bash
# Example: Run cafeteria-service
cd cafeteria-service
./scripts/start-services.sh  # Linux/Mac
# or
.\scripts\start-services.bat  # Windows
```

Refer to each service's `DEPLOYMENT.md` for detailed instructions.

### 🛠️ Option 3: Manual Build & Run

```bash
# Build all services
mvn clean install -DskipTests

# Run a specific service
cd admin-service
mvn spring-boot:run
```

### 📊 Verify Deployment

```bash
# Health checks
curl http://localhost:8082/actuator/health  # admin-service
curl http://localhost:8083/actuator/health  # cafeteria-service
curl http://localhost:8084/actuator/health  # review-service
curl http://localhost:8085/actuator/health  # media-service
curl http://localhost:8086/actuator/health  # preference-service
```

### 🗄️ Database Initialization

Databases are automatically initialized when services start. To manually initialize:

```bash
# Run initialization scripts (if needed)
cd <service-name>/scripts
psql -U postgres -d <database-name> -f init_<service>_db.sql
```

---

## 📁 Project Structure

```
nushungry-Backend/
├── admin-service/               # User management & authentication
│   ├── src/
│   ├── Dockerfile
│   ├── docker-compose.yml
│   ├── DEPLOYMENT.md
│   └── scripts/
│       ├── init_admin_db.sql
│       ├── MIGRATION_GUIDE.md
│       └── start-services.sh/bat
│
├── cafeteria-service/           # Cafeteria & stall management
│   ├── src/
│   ├── Dockerfile
│   ├── docker-compose.yml
│   ├── DEPLOYMENT.md
│   └── scripts/
│       ├── init_cafeteria_db.sql
│       ├── MIGRATION_GUIDE.md
│       └── start-services.sh/bat
│
├── review-service/              # Reviews, likes & comments
│   ├── src/
│   ├── Dockerfile
│   ├── docker-compose.yml
│   ├── DEPLOYMENT.md
│   └── scripts/
│       ├── migrate_reviews_to_mongodb.py
│       ├── MIGRATION_GUIDE.md
│       └── start-services.sh/bat
│
├── media-service/               # Image uploads & processing
│   ├── src/
│   ├── Dockerfile
│   ├── docker-compose.yml
│   ├── DEPLOYMENT.md
│   └── scripts/
│       ├── init_media_db.sql
│       ├── MIGRATION_GUIDE.md
│       └── start-services.sh/bat
│
├── preference-service/          # Favorites & search history
│   ├── src/
│   ├── Dockerfile
│   ├── docker-compose.yml
│   ├── DEPLOYMENT.md
│   └── scripts/
│       ├── init_preference_db.sql
│       ├── MIGRATION_GUIDE.md
│       └── start-services.sh/bat
│
├── src/                         # Legacy monolith (deprecated)
├── docs/                        # System documentation
│   ├── ARCHITECTURE.md          # Architecture details
│   ├── DEVELOPMENT.md           # Development guide
│   └── API_DOCUMENTATION.md     # API reference
│
├── scripts/                     # Global scripts
│   ├── start-all-services.sh/bat
│   └── stop-all-services.sh/bat
│
├── .github/workflows/           # CI/CD pipelines
│   ├── ci.yml
│   └── cd.yml
│
├── docker-compose.yml           # Global orchestration
├── .env.example                 # Environment variables template
├── pom.xml                      # Parent POM (optional)
├── PROGRESS.md                  # Migration progress tracking
└── README.md                    # This file
```

### Service Structure (Example: cafeteria-service)

```
cafeteria-service/
├── src/
│   ├── main/
│   │   ├── java/com/nushungry/cafeteriaservice/
│   │   │   ├── controller/       # REST endpoints
│   │   │   ├── service/          # Business logic
│   │   │   ├── repository/       # Data access
│   │   │   ├── model/            # JPA entities
│   │   │   ├── dto/              # Data transfer objects
│   │   │   ├── event/            # RabbitMQ listeners
│   │   │   ├── config/           # Configuration classes
│   │   │   └── CafeteriaServiceApplication.java
│   │   └── resources/
│   │       ├── application.properties
│   │       └── application-docker.properties
│   └── test/
│       └── java/                 # Unit & integration tests
├── Dockerfile
├── docker-compose.yml
└── DEPLOYMENT.md
```

---

## 📖 Documentation

### Core Documents
- **[ARCHITECTURE.md](docs/ARCHITECTURE.md)**: Detailed system architecture, design patterns, and data flow
- **[DEVELOPMENT.md](docs/DEVELOPMENT.md)**: Local development setup, coding standards, and best practices
- **[API_DOCUMENTATION.md](docs/API_DOCUMENTATION.md)**: Complete API reference for all services
- **[PROGRESS.md](PROGRESS.md)**: Microservices migration progress and task tracking

### Service-Specific Docs
Each service has its own documentation:
- **DEPLOYMENT.md**: Deployment instructions (local, Docker, AWS ECS)
- **MIGRATION_GUIDE.md**: Database migration from monolith
- **README.md**: Service-specific features and endpoints

### API Documentation (Swagger)
Access interactive API documentation when services are running:
- Admin Service: http://localhost:8082/swagger-ui.html
- Cafeteria Service: http://localhost:8083/swagger-ui.html
- Review Service: http://localhost:8084/swagger-ui.html
- Media Service: http://localhost:8085/swagger-ui.html
- Preference Service: http://localhost:8086/swagger-ui.html

---

## 🧪 Testing

### Run All Tests
```bash
# Run tests for all services
mvn test

# Run tests for a specific service
cd cafeteria-service
mvn test
```

### Test Coverage
- **Unit Tests**: Controller, Service, Repository layers (>70% coverage)
- **Integration Tests**: Full API flow, database interactions, event handling
- **Custom Query Tests**: All `@Query` annotated repository methods

### Run Integration Tests
```bash
mvn verify -P integration-tests
```

---

## 🚢 Deployment

### Local Development
Use Docker Compose (see [Quick Start](#quick-start))

### AWS ECS Production
Refer to individual service `DEPLOYMENT.md` files for:
- ECR image building and pushing
- ECS task definition configuration
- Service deployment and updates
- Environment variable management

### CI/CD Pipeline
GitHub Actions automatically:
- Runs tests on pull requests
- Builds Docker images on merge to main
- Deploys to AWS ECS (production)

---

## 🔧 Configuration

### Environment Variables

Copy `.env.example` to `.env` and configure:

```bash
# Database
POSTGRES_USER=postgres
POSTGRES_PASSWORD=your_password

# RabbitMQ
RABBITMQ_USER=guest
RABBITMQ_PASSWORD=guest

# MinIO
MINIO_ROOT_USER=minioadmin
MINIO_ROOT_PASSWORD=minioadmin

# JWT
JWT_SECRET=your-secret-key
JWT_EXPIRATION=86400000
```

See `.env.example` for complete configuration options.

---

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

### Development Guidelines
- Follow existing code structure and naming conventions
- Write unit tests for new features (>70% coverage)
- Update relevant documentation
- Run `mvn test` before committing
- Use meaningful commit messages

---

## 📝 License

This project is part of the NUSHungry application for NUS students.

---

## 📧 Support

For issues, questions, or contributions:
- Create an issue in the repository
- Contact the development team

---

## 🗺️ Roadmap

### Completed ✅
- Microservices architecture implementation
- Docker containerization
- Event-driven communication (RabbitMQ)
- Comprehensive testing suite
- CI/CD pipeline setup

### In Progress 🚧
- API Gateway integration
- Service discovery (Eureka)
- Distributed tracing (Zipkin)

### Planned 📋
- Kubernetes deployment
- Centralized logging (ELK Stack)
- Monitoring & alerting (Prometheus + Grafana)
- Rate limiting & circuit breakers
