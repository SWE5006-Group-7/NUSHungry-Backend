# NUSHungry Backend - Development Guide

## Table of Contents
- [Getting Started](#getting-started)
- [Development Environment Setup](#development-environment-setup)
- [Project Structure](#project-structure)
- [Coding Standards](#coding-standards)
- [Development Workflow](#development-workflow)
- [Testing Guidelines](#testing-guidelines)
- [Debugging](#debugging)
- [Common Tasks](#common-tasks)
- [Troubleshooting](#troubleshooting)

---

## Getting Started

### Prerequisites

Install the following tools before starting development:

| Tool | Version | Purpose | Installation |
|------|---------|---------|--------------|
| **Java JDK** | 17 (LTS) | Runtime environment | [Download](https://adoptium.net/) |
| **Maven** | 3.8+ | Build tool | [Download](https://maven.apache.org/download.cgi) |
| **Docker** | 20.10+ | Containerization | [Download](https://www.docker.com/products/docker-desktop) |
| **Docker Compose** | 2.x | Multi-container orchestration | Included with Docker Desktop |
| **Git** | 2.x | Version control | [Download](https://git-scm.com/downloads) |
| **IDE** | IntelliJ IDEA / VS Code | Code editor | [IntelliJ](https://www.jetbrains.com/idea/) / [VS Code](https://code.visualstudio.com/) |
| **PostgreSQL Client** | 14+ (optional) | Database management | [pgAdmin](https://www.pgadmin.org/) or [DBeaver](https://dbeaver.io/) |
| **MongoDB Compass** | Latest (optional) | MongoDB GUI | [Download](https://www.mongodb.com/products/compass) |
| **Postman** | Latest (optional) | API testing | [Download](https://www.postman.com/downloads/) |

### Verify Installation

```bash
# Java
java -version
# Expected: openjdk version "17.x.x"

# Maven
mvn -version
# Expected: Apache Maven 3.8.x

# Docker
docker --version
docker-compose --version
# Expected: Docker version 20.x.x, Docker Compose version 2.x.x

# Git
git --version
# Expected: git version 2.x.x
```

---

## Development Environment Setup

### 1. Clone the Repository

```bash
git clone https://github.com/your-org/nushungry-Backend.git
cd nushungry-Backend
```

### 2. Environment Configuration

Create `.env` file from template:

```bash
cp .env.example .env
```

Edit `.env` with your local settings:

```bash
# Database Configuration
POSTGRES_USER=postgres
POSTGRES_PASSWORD=your_local_password
MONGO_INITDB_ROOT_USERNAME=admin
MONGO_INITDB_ROOT_PASSWORD=your_mongo_password

# JWT Configuration
JWT_SECRET=your-local-secret-key-minimum-256-bits
JWT_EXPIRATION=86400000  # 24 hours

# RabbitMQ Configuration
RABBITMQ_USER=guest
RABBITMQ_PASSWORD=guest

# MinIO Configuration
MINIO_ROOT_USER=minioadmin
MINIO_ROOT_PASSWORD=minioadmin

# Email Configuration (for admin-service password reset)
SPRING_MAIL_HOST=smtp.gmail.com
SPRING_MAIL_PORT=587
SPRING_MAIL_USERNAME=your-email@gmail.com
SPRING_MAIL_PASSWORD=your-app-password
```

### 3. Start Infrastructure Services

Start databases and message broker:

```bash
# Start all infrastructure (PostgreSQL, MongoDB, RabbitMQ, MinIO)
docker-compose up -d postgres-admin postgres-cafeteria postgres-media postgres-preference mongodb rabbitmq minio

# Verify services are running
docker-compose ps
```

**Service URLs:**
- PostgreSQL Admin DB: `localhost:5432`
- PostgreSQL Cafeteria DB: `localhost:5433`
- PostgreSQL Media DB: `localhost:5434`
- PostgreSQL Preference DB: `localhost:5435`
- MongoDB: `localhost:27017`
- RabbitMQ Management: http://localhost:15672 (guest/guest)
- MinIO Console: http://localhost:9001 (minioadmin/minioadmin)

### 4. Initialize Databases

Run initialization scripts for each service:

```bash
# Admin Service
cd admin-service/scripts
psql -h localhost -U postgres -d admin_db -f init_admin_db.sql

# Cafeteria Service
cd ../../cafeteria-service/scripts
psql -h localhost -p 5433 -U postgres -d cafeteria_db -f init_cafeteria_db.sql

# Media Service
cd ../../media-service/scripts
psql -h localhost -p 5434 -U postgres -d media_db -f init_media_db.sql

# Preference Service
cd ../../preference-service/scripts
psql -h localhost -p 5435 -U postgres -d preference_db -f init_preference_db.sql

# Review Service (MongoDB - auto-initialized on first run)
cd ../../review-service
```

### 5. Build All Services

```bash
# From project root
mvn clean install -DskipTests

# Or build individual services
cd admin-service
mvn clean package -DskipTests
```

### 6. Run Services

#### Option A: Run with Maven (Recommended for development)

```bash
# Terminal 1 - Admin Service
cd admin-service
mvn spring-boot:run

# Terminal 2 - Cafeteria Service
cd cafeteria-service
mvn spring-boot:run

# Terminal 3 - Review Service
cd review-service
mvn spring-boot:run

# Terminal 4 - Media Service
cd media-service
mvn spring-boot:run

# Terminal 5 - Preference Service
cd preference-service
mvn spring-boot:run
```

#### Option B: Run with Docker Compose (Full stack)

```bash
# Start all services
docker-compose up -d

# View logs
docker-compose logs -f

# Stop all services
docker-compose down
```

#### Option C: Use Service Scripts

```bash
# Linux/Mac
cd cafeteria-service
./scripts/start-services.sh

# Windows
cd cafeteria-service
.\scripts\start-services.bat
```

### 7. Verify Services

```bash
# Health checks
curl http://localhost:8082/actuator/health  # admin-service
curl http://localhost:8083/actuator/health  # cafeteria-service
curl http://localhost:8084/actuator/health  # review-service
curl http://localhost:8085/actuator/health  # media-service
curl http://localhost:8086/actuator/health  # preference-service
```

---

## Project Structure

### Root Directory Structure

```
nushungry-Backend/
├── admin-service/           # User management & auth
├── cafeteria-service/       # Cafeteria & stall data
├── review-service/          # Reviews & comments
├── media-service/           # File uploads
├── preference-service/      # User preferences
├── src/                     # Legacy monolith (deprecated)
├── docs/                    # Documentation
│   ├── ARCHITECTURE.md
│   ├── DEVELOPMENT.md       # This file
│   └── API_DOCUMENTATION.md
├── scripts/                 # Global utility scripts
├── docker-compose.yml       # Global orchestration
├── .env.example             # Environment template
├── pom.xml                  # Parent POM (optional)
└── README.md
```

### Individual Service Structure

```
<service-name>/
├── src/
│   ├── main/
│   │   ├── java/com/nushungry/<service>/
│   │   │   ├── controller/      # REST endpoints
│   │   │   ├── service/         # Business logic
│   │   │   ├── repository/      # Data access layer
│   │   │   ├── model/           # Domain entities
│   │   │   ├── dto/             # Data transfer objects
│   │   │   ├── event/           # RabbitMQ event handlers
│   │   │   ├── config/          # Configuration classes
│   │   │   ├── exception/       # Custom exceptions
│   │   │   ├── security/        # Security filters (if applicable)
│   │   │   └── <Service>Application.java  # Main class
│   │   └── resources/
│   │       ├── application.properties       # Default config
│   │       ├── application-dev.properties   # Dev profile
│   │       ├── application-docker.properties # Docker profile
│   │       └── application-prod.properties  # Prod profile
│   └── test/
│       └── java/com/nushungry/<service>/
│           ├── controller/      # Controller tests
│           ├── service/         # Service tests
│           ├── repository/      # Repository tests
│           └── integration/     # Integration tests
├── scripts/
│   ├── init_<service>_db.sql    # Database schema
│   ├── MIGRATION_GUIDE.md       # Migration instructions
│   ├── start-services.sh/bat    # Startup scripts
│   └── stop-services.sh/bat     # Shutdown scripts
├── Dockerfile                   # Container image definition
├── docker-compose.yml           # Local development
├── .dockerignore                # Docker build exclusions
├── DEPLOYMENT.md                # Deployment guide
├── pom.xml                      # Maven configuration
└── README.md                    # Service documentation
```

---

## Coding Standards

### 1. Java Code Style

#### Naming Conventions

```java
// Classes: PascalCase
public class CafeteriaService { }

// Interfaces: PascalCase with 'I' prefix (optional) or descriptive name
public interface CafeteriaRepository { }

// Methods: camelCase, verb-based
public List<Cafeteria> findAllCafeterias() { }

// Variables: camelCase
private String cafeteriaName;

// Constants: UPPER_SNAKE_CASE
public static final int MAX_RETRY_ATTEMPTS = 3;

// Packages: lowercase, dot-separated
package com.nushungry.cafeteriaservice.controller;
```

#### Code Organization

```java
// Order of class members:
public class CafeteriaService {
    // 1. Static constants
    private static final int DEFAULT_PAGE_SIZE = 20;
    
    // 2. Static variables
    private static Logger logger = LoggerFactory.getLogger(CafeteriaService.class);
    
    // 3. Instance variables (dependencies first)
    private final CafeteriaRepository cafeteriaRepository;
    private final RabbitTemplate rabbitTemplate;
    
    // 4. Constructor (use @RequiredArgsConstructor or explicit)
    public CafeteriaService(CafeteriaRepository cafeteriaRepository,
                            RabbitTemplate rabbitTemplate) {
        this.cafeteriaRepository = cafeteriaRepository;
        this.rabbitTemplate = rabbitTemplate;
    }
    
    // 5. Public methods
    public List<Cafeteria> getAllCafeterias() { }
    
    // 6. Private helper methods
    private void validateCafeteria(Cafeteria cafeteria) { }
}
```

### 2. Spring Boot Best Practices

#### Dependency Injection

```java
// ✅ GOOD: Constructor injection (immutable, testable)
@Service
@RequiredArgsConstructor  // Lombok
public class CafeteriaService {
    private final CafeteriaRepository repository;
    private final StallService stallService;
}

// ❌ BAD: Field injection
@Service
public class CafeteriaService {
    @Autowired
    private CafeteriaRepository repository;  // Avoid
}
```

#### Controller Design

```java
@RestController
@RequestMapping("/api/cafeterias")
@RequiredArgsConstructor
@Validated
public class CafeteriaController {
    private final CafeteriaService cafeteriaService;
    
    // ✅ GOOD: Use DTOs, proper HTTP methods, meaningful names
    @GetMapping
    public ResponseEntity<List<CafeteriaDto>> getAllCafeterias(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<CafeteriaDto> cafeterias = cafeteriaService.findAll(page, size);
        return ResponseEntity.ok(cafeterias);
    }
    
    @PostMapping
    public ResponseEntity<CafeteriaDto> createCafeteria(
            @Valid @RequestBody CreateCafeteriaRequest request) {
        CafeteriaDto created = cafeteriaService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
    
    // ✅ GOOD: Exception handling with @ControllerAdvice
    @GetMapping("/{id}")
    public ResponseEntity<CafeteriaDto> getCafeteria(@PathVariable Long id) {
        return cafeteriaService.findById(id)
            .map(ResponseEntity::ok)
            .orElseThrow(() -> new ResourceNotFoundException("Cafeteria not found"));
    }
}
```

#### Service Layer

```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)  // Default for all methods
public class CafeteriaService {
    private final CafeteriaRepository repository;
    
    public List<CafeteriaDto> findAll(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return repository.findAll(pageable)
            .map(this::toDto)
            .getContent();
    }
    
    @Transactional  // Override for write operations
    public CafeteriaDto create(CreateCafeteriaRequest request) {
        Cafeteria cafeteria = Cafeteria.builder()
            .name(request.getName())
            .location(request.getLocation())
            .build();
        Cafeteria saved = repository.save(cafeteria);
        return toDto(saved);
    }
    
    private CafeteriaDto toDto(Cafeteria entity) {
        // Mapping logic
    }
}
```

### 3. Error Handling

#### Global Exception Handler

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        ErrorResponse error = ErrorResponse.builder()
            .status(HttpStatus.NOT_FOUND.value())
            .message(ex.getMessage())
            .timestamp(Instant.now())
            .build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .collect(Collectors.toMap(
                FieldError::getField,
                FieldError::getDefaultMessage
            ));
        
        ErrorResponse error = ErrorResponse.builder()
            .status(HttpStatus.BAD_REQUEST.value())
            .message("Validation failed")
            .errors(errors)
            .timestamp(Instant.now())
            .build();
        return ResponseEntity.badRequest().body(error);
    }
}
```

### 4. Validation

```java
// DTOs with validation annotations
public class CreateCafeteriaRequest {
    @NotBlank(message = "Name is required")
    @Size(min = 3, max = 100, message = "Name must be between 3 and 100 characters")
    private String name;
    
    @NotBlank(message = "Location is required")
    private String location;
    
    @DecimalMin(value = "-90.0", message = "Latitude must be >= -90")
    @DecimalMax(value = "90.0", message = "Latitude must be <= 90")
    private BigDecimal latitude;
    
    @DecimalMin(value = "-180.0", message = "Longitude must be >= -180")
    @DecimalMax(value = "180.0", message = "Longitude must be <= 180")
    private BigDecimal longitude;
}
```

### 5. Logging

```java
@Service
@Slf4j  // Lombok annotation
public class CafeteriaService {
    
    public CafeteriaDto create(CreateCafeteriaRequest request) {
        log.info("Creating new cafeteria: {}", request.getName());
        
        try {
            Cafeteria saved = repository.save(cafeteria);
            log.info("Cafeteria created successfully with ID: {}", saved.getId());
            return toDto(saved);
        } catch (Exception e) {
            log.error("Error creating cafeteria: {}", request.getName(), e);
            throw new ServiceException("Failed to create cafeteria", e);
        }
    }
}
```

**Logging Levels:**
- `log.trace()`: Very detailed, method entry/exit
- `log.debug()`: Debugging information, variable values
- `log.info()`: Important business events (creation, updates)
- `log.warn()`: Warnings, recoverable errors
- `log.error()`: Errors, exceptions

---

## Development Workflow

### 1. Git Workflow (Feature Branch)

```bash
# 1. Create feature branch from main
git checkout main
git pull origin main
git checkout -b feature/add-rating-filter

# 2. Make changes and commit frequently
git add .
git commit -m "feat: add rating filter to cafeteria search"

# 3. Keep branch updated with main
git fetch origin main
git rebase origin/main

# 4. Push to remote
git push origin feature/add-rating-filter

# 5. Create Pull Request on GitHub
# 6. After review and approval, merge to main
```

### 2. Commit Message Convention

Follow [Conventional Commits](https://www.conventionalcommits.org/):

```
<type>(<scope>): <short description>

<longer description (optional)>

<footer (optional)>
```

**Types:**
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation changes
- `style`: Code style changes (formatting)
- `refactor`: Code refactoring
- `test`: Adding or updating tests
- `chore`: Maintenance tasks

**Examples:**
```
feat(cafeteria): add rating filter to search endpoint

fix(review): prevent duplicate likes from same user

docs(readme): update deployment instructions

test(preference): add integration tests for favorites
```

### 3. Pull Request Process

1. **Create PR** with descriptive title and description
2. **Link related issues**: "Closes #123"
3. **Wait for CI/CD** checks to pass
4. **Request review** from team members
5. **Address feedback** and push updates
6. **Merge** after approval (squash merge preferred)

---

## Testing Guidelines

### 1. Test Structure

Follow AAA pattern (Arrange, Act, Assert):

```java
@Test
void shouldCreateCafeteria() {
    // Arrange
    CreateCafeteriaRequest request = CreateCafeteriaRequest.builder()
        .name("Test Cafeteria")
        .location("Test Location")
        .build();
    
    // Act
    CafeteriaDto result = cafeteriaService.create(request);
    
    // Assert
    assertThat(result).isNotNull();
    assertThat(result.getName()).isEqualTo("Test Cafeteria");
    assertThat(result.getId()).isPositive();
}
```

### 2. Unit Tests

#### Controller Tests

```java
@WebMvcTest(CafeteriaController.class)
class CafeteriaControllerTest {
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private CafeteriaService cafeteriaService;
    
    @Test
    void shouldReturnCafeteriaList() throws Exception {
        // Arrange
        List<CafeteriaDto> cafeterias = List.of(
            CafeteriaDto.builder().id(1L).name("Cafeteria 1").build()
        );
        when(cafeteriaService.findAll(0, 20)).thenReturn(cafeterias);
        
        // Act & Assert
        mockMvc.perform(get("/api/cafeterias"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(1))
            .andExpect(jsonPath("$[0].name").value("Cafeteria 1"));
    }
}
```

#### Service Tests

```java
@ExtendWith(MockitoExtension.class)
class CafeteriaServiceTest {
    @Mock
    private CafeteriaRepository repository;
    
    @InjectMocks
    private CafeteriaService cafeteriaService;
    
    @Test
    void shouldCreateCafeteria() {
        // Arrange
        Cafeteria entity = Cafeteria.builder()
            .id(1L)
            .name("Test Cafeteria")
            .build();
        when(repository.save(any(Cafeteria.class))).thenReturn(entity);
        
        // Act
        CafeteriaDto result = cafeteriaService.create(new CreateCafeteriaRequest());
        
        // Assert
        assertThat(result.getId()).isEqualTo(1L);
        verify(repository, times(1)).save(any(Cafeteria.class));
    }
}
```

#### Repository Tests

```java
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CafeteriaRepositoryTest {
    @Autowired
    private CafeteriaRepository repository;
    
    @Autowired
    private TestEntityManager entityManager;
    
    @Test
    void shouldFindCafeteriaByName() {
        // Arrange
        Cafeteria cafeteria = Cafeteria.builder()
            .name("Test Cafeteria")
            .location("Test Location")
            .build();
        entityManager.persist(cafeteria);
        entityManager.flush();
        
        // Act
        Optional<Cafeteria> found = repository.findByName("Test Cafeteria");
        
        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Test Cafeteria");
    }
}
```

### 3. Integration Tests

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
class CafeteriaServiceIntegrationTest {
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Test
    void shouldCreateAndRetrieveCafeteria() {
        // Arrange
        CreateCafeteriaRequest request = CreateCafeteriaRequest.builder()
            .name("Integration Test Cafeteria")
            .location("Test Location")
            .build();
        
        // Act: Create
        ResponseEntity<CafeteriaDto> createResponse = restTemplate.postForEntity(
            "/api/admin/cafeterias",
            request,
            CafeteriaDto.class
        );
        
        // Assert: Created
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Long id = createResponse.getBody().getId();
        
        // Act: Retrieve
        ResponseEntity<CafeteriaDto> getResponse = restTemplate.getForEntity(
            "/api/cafeterias/" + id,
            CafeteriaDto.class
        );
        
        // Assert: Retrieved
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getResponse.getBody().getName()).isEqualTo("Integration Test Cafeteria");
    }
}
```

### 4. Test Coverage Goals

- **Controller**: >80% line coverage
- **Service**: >75% line coverage
- **Repository**: >75% line coverage (especially custom queries)
- **Overall**: >70% line coverage

Run coverage report:
```bash
mvn clean test jacoco:report
# Report location: target/site/jacoco/index.html
```

---

## Debugging

### 1. IntelliJ IDEA Debugging

#### Run with Debugger

1. Set breakpoints by clicking left margin
2. Right-click `<Service>Application.java`
3. Select "Debug '<Service>Application'"

#### Remote Debugging (Docker)

Add to `docker-compose.yml`:
```yaml
cafeteria-service:
  environment:
    JAVA_OPTS: "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"
  ports:
    - "8083:8083"
    - "5005:5005"  # Debug port
```

In IntelliJ:
1. Run > Edit Configurations
2. Add "Remote JVM Debug"
3. Host: `localhost`, Port: `5005`
4. Click "Debug"

### 2. Logging Configuration

**`application.properties`:**
```properties
# Root logging level
logging.level.root=INFO

# Package-specific logging
logging.level.com.nushungry.cafeteriaservice=DEBUG
logging.level.org.springframework.web=DEBUG
logging.level.org.hibernate.SQL=DEBUG
logging.level.org.hibernate.type.descriptor.sql.BasicBinder=TRACE

# Log file
logging.file.name=logs/cafeteria-service.log
logging.file.max-size=10MB
logging.file.max-history=30
```

### 3. Database Debugging

#### View SQL Queries

```properties
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
```

#### Connect to Database

```bash
# PostgreSQL
psql -h localhost -p 5433 -U postgres -d cafeteria_db

# MongoDB
mongosh mongodb://localhost:27017/reviews_db -u admin -p password
```

### 4. RabbitMQ Debugging

Access management UI: http://localhost:15672

- **Queues**: View message counts, consume messages
- **Exchanges**: View bindings
- **Connections**: Active consumers/publishers

---

## Common Tasks

### Add a New Endpoint

1. **Create DTO**:
```java
// src/main/java/com/nushungry/cafeteriaservice/dto/CreateStallRequest.java
@Data
@Builder
public class CreateStallRequest {
    @NotBlank
    private String name;
    private Long cafeteriaId;
}
```

2. **Update Service**:
```java
// src/main/java/com/nushungry/cafeteriaservice/service/StallService.java
public StallDto createStall(CreateStallRequest request) {
    // Business logic
}
```

3. **Add Controller Endpoint**:
```java
// src/main/java/com/nushungry/cafeteriaservice/controller/StallController.java
@PostMapping
public ResponseEntity<StallDto> createStall(@Valid @RequestBody CreateStallRequest request) {
    StallDto created = stallService.createStall(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(created);
}
```

4. **Write Tests**:
```java
@Test
void shouldCreateStall() {
    // Test implementation
}
```

### Add a Database Migration

1. Create SQL script in `scripts/migrations/`:
```sql
-- scripts/migrations/V2__add_stall_rating_index.sql
CREATE INDEX idx_stall_rating ON stall(average_rating DESC);
```

2. Apply manually or use Flyway/Liquibase (future)

### Add Environment Variable

1. Update `.env.example`:
```bash
NEW_FEATURE_ENABLED=true
```

2. Update `application.properties`:
```properties
app.features.new-feature=${NEW_FEATURE_ENABLED:false}
```

3. Inject in code:
```java
@Value("${app.features.new-feature}")
private boolean newFeatureEnabled;
```

---

## Troubleshooting

### Service Won't Start

**Error: Port already in use**
```bash
# Find process using port
lsof -i :8083  # Mac/Linux
netstat -ano | findstr :8083  # Windows

# Kill process
kill -9 <PID>  # Mac/Linux
taskkill /PID <PID> /F  # Windows
```

**Error: Cannot connect to database**
- Verify Docker containers are running: `docker ps`
- Check connection string in `application.properties`
- Verify `.env` file exists and has correct credentials

### Tests Failing

**Error: Database not found**
- Use `@AutoConfigureTestDatabase(replace = Replace.NONE)`
- Ensure H2 in-memory DB dependency is present for unit tests

**Error: Context failed to load**
- Check for missing `@SpringBootTest` annotation
- Verify all required beans are created (mocks or real)

### Docker Issues

**Error: Image not found**
```bash
# Rebuild image
docker-compose build <service-name>
```

**Error: Cannot connect to Docker daemon**
- Ensure Docker Desktop is running
- Check Docker settings and permissions

---

## Best Practices Checklist

Before committing code, ensure:

- [ ] Code follows naming conventions
- [ ] No hardcoded values (use configuration)
- [ ] Proper error handling with meaningful messages
- [ ] Logging at appropriate levels
- [ ] Unit tests written and passing
- [ ] Integration tests for new endpoints
- [ ] Code formatted (use IDE formatter)
- [ ] No compiler warnings
- [ ] Documentation updated (if needed)
- [ ] `.env.example` updated (if new variables)
- [ ] Swagger annotations added to new endpoints

---

## Additional Resources

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Spring Data JPA Documentation](https://spring.io/projects/spring-data-jpa)
- [RabbitMQ Tutorials](https://www.rabbitmq.com/getstarted.html)
- [Docker Documentation](https://docs.docker.com/)
- [JUnit 5 User Guide](https://junit.org/junit5/docs/current/user-guide/)
- [Mockito Documentation](https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html)

---

For questions or issues, create an issue in the repository or contact the development team.
