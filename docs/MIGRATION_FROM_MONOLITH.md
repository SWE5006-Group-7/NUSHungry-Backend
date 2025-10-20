# NUSHungry Backend - Migration from Monolith to Microservices

## Table of Contents
- [Overview](#overview)
- [Why Microservices?](#why-microservices)
- [Architecture Comparison](#architecture-comparison)
- [Migration Strategy](#migration-strategy)
- [Data Migration](#data-migration)
- [Service-by-Service Migration Guide](#service-by-service-migration-guide)
- [Testing & Validation](#testing--validation)
- [Rollback Plan](#rollback-plan)
- [Post-Migration Tasks](#post-migration-tasks)
- [Common Challenges & Solutions](#common-challenges--solutions)
- [FAQ](#faq)

---

## Overview

This document guides the migration of NUSHungry Backend from a **monolithic Spring Boot application** to a **microservices architecture**. The migration was completed in phases to minimize disruption and ensure data integrity.

### Migration Timeline

| Phase | Duration | Status | Description |
|-------|----------|--------|-------------|
| **Phase 1: Planning** | 2 weeks | ✅ Complete | Architecture design, service boundaries |
| **Phase 2: Infrastructure** | 1 week | ✅ Complete | Docker, databases, message queue setup |
| **Phase 3: Service Development** | 4 weeks | ✅ Complete | Build 5 microservices |
| **Phase 4: Data Migration** | 1 week | ✅ Complete | Split monolithic database |
| **Phase 5: Testing** | 2 weeks | ✅ Complete | Integration & performance testing |
| **Phase 6: Deployment** | 1 week | ✅ Complete | Production rollout with blue-green deployment |

**Total Duration:** ~11 weeks (Q3-Q4 2024)

---

## Why Microservices?

### Problems with Monolithic Architecture

| Issue | Impact | Example |
|-------|--------|---------|
| **Tight Coupling** | Changes in one module affect entire system | Adding review feature requires full redeployment |
| **Scalability Bottleneck** | Can't scale individual components | High traffic on reviews forces scaling entire app |
| **Technology Lock-in** | Stuck with single tech stack | Can't use MongoDB for reviews without migrating all |
| **Deployment Risk** | Single deployment can break everything | Bug in media upload crashes admin panel |
| **Development Bottleneck** | Multiple teams work on same codebase | Merge conflicts, coordination overhead |
| **Long Build Times** | Full rebuild/test for small changes | 15+ minute builds for one-line fix |

### Benefits of Microservices

| Benefit | Description | Measurement |
|---------|-------------|-------------|
| **Independent Deployment** | Deploy services independently | Deploy review-service without touching admin-service |
| **Technology Flexibility** | Choose best tool for each job | MongoDB for reviews, PostgreSQL for relational data |
| **Fault Isolation** | Service failures don't cascade | Media service down doesn't affect cafeteria browsing |
| **Scalability** | Scale services based on demand | Scale review-service 5x during peak hours only |
| **Team Autonomy** | Teams own services end-to-end | Review team deploys independently |
| **Faster Innovation** | Smaller codebases = faster iteration | New features ship in days, not weeks |

### Trade-offs

**Increased Complexity:**
- More moving parts to monitor
- Network latency between services
- Distributed transactions complexity
- DevOps overhead

**When Microservices Make Sense:**
- ✅ Growing team (3+ engineers)
- ✅ Distinct functional domains
- ✅ Different scalability requirements
- ✅ Need for technology diversity

**When to Stay Monolithic:**
- ❌ Small team (1-2 engineers)
- ❌ Simple application
- ❌ Tight coupling unavoidable
- ❌ Limited DevOps resources

---

## Architecture Comparison

### Before: Monolithic Architecture

```
┌─────────────────────────────────────────┐
│        Spring Boot Application          │
│  ┌─────────────────────────────────┐   │
│  │  User Management (Admin)         │   │
│  ├─────────────────────────────────┤   │
│  │  Cafeteria & Stall Management    │   │
│  ├─────────────────────────────────┤   │
│  │  Review System                   │   │
│  ├─────────────────────────────────┤   │
│  │  Media Upload                    │   │
│  ├─────────────────────────────────┤   │
│  │  User Preferences                │   │
│  └─────────────────────────────────┘   │
│                 ↓                        │
│        ┌──────────────────┐             │
│        │  MySQL Database  │             │
│        │  (Single Schema) │             │
│        └──────────────────┘             │
└─────────────────────────────────────────┘

Issues:
- Single point of failure
- Monolithic deployment
- Shared database coupling
- No independent scaling
```

### After: Microservices Architecture

```
┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐
│  admin-service   │  │cafeteria-service │  │ review-service   │
│   :8082          │  │     :8083        │  │     :8084        │
│  ┌──────────┐    │  │  ┌──────────┐    │  │  ┌──────────┐    │
│  │PostgreSQL│    │  │  │PostgreSQL│    │  │  │ MongoDB  │    │
│  │ admin_db │    │  │  │cafeteria │    │  │  │review_db │    │
│  └──────────┘    │  │  └──────────┘    │  │  └──────────┘    │
└──────────────────┘  └──────────────────┘  └──────────────────┘
         ↑                    ↑                      ↓
         └────────────────────┴──────────────────────┘
                           RabbitMQ
                      (Event-Driven Sync)

┌──────────────────┐  ┌──────────────────┐
│  media-service   │  │preference-service│
│      :8085       │  │      :8086       │
│  ┌──────────┐    │  │  ┌──────────┐    │
│  │PostgreSQL│    │  │  │PostgreSQL│    │
│  │ media_db │    │  │  │preference│    │
│  │   MinIO  │    │  │  └──────────┘    │
│  └──────────┘    │  └──────────────────┘
└──────────────────┘

Benefits:
✅ Independent deployment & scaling
✅ Polyglot persistence (PostgreSQL + MongoDB)
✅ Fault isolation
✅ Technology flexibility
```

---

## Migration Strategy

### Approach: Strangler Fig Pattern

We used the **Strangler Fig Pattern** to gradually replace the monolith:

1. **Identify Service Boundaries** - Domain-driven design
2. **Build New Services** - Alongside existing monolith
3. **Migrate Data** - Split database schemas
4. **Route Traffic** - Gradually redirect to new services
5. **Decommission Monolith** - Once all traffic migrated

### Service Decomposition

| Monolith Module | Microservice | Rationale |
|-----------------|--------------|-----------|
| User Management + Auth | **admin-service** | Security-critical, centralized auth |
| Cafeteria & Stall CRUD | **cafeteria-service** | Master data with separate scaling needs |
| Review System | **review-service** | High read/write volume, needs NoSQL |
| Media Upload | **media-service** | File storage, different infrastructure |
| Favorites + Search History | **preference-service** | User-specific, simple bounded context |

### Migration Phases

#### Phase 1: Extract Review Service (Week 1-2)
- **Why first?** High traffic, clear boundaries, NoSQL benefits
- Build review-service with MongoDB
- Implement event publishing (RabbitMQ)
- Migrate review data from MySQL to MongoDB
- Dual-write pattern (write to both databases)
- Validate data consistency
- Route traffic to new service
- Remove monolith review code

#### Phase 2: Extract Cafeteria Service (Week 3-4)
- Build cafeteria-service with PostgreSQL
- Implement event consumption (RabbitMQ)
- Migrate cafeteria/stall/image data
- Update references to use REST APIs
- Validate relationships

#### Phase 3: Extract Remaining Services (Week 5-8)
- admin-service (authentication critical)
- media-service (file storage)
- preference-service (simple, low risk)

#### Phase 4: Integration & Testing (Week 9-10)
- End-to-end testing
- Performance testing
- Load testing
- Security audit

#### Phase 5: Production Deployment (Week 11)
- Blue-green deployment
- Monitor metrics
- Gradual traffic migration
- Monolith decommissioning

---

## Data Migration

### Database Splitting Strategy

**Before (Monolith):**
```
nushungry_db (MySQL)
├── users
├── cafeterias
├── stalls
├── reviews
├── images
├── favorites
└── search_history
```

**After (Microservices):**
```
admin_db (PostgreSQL)
└── users

cafeteria_db (PostgreSQL)
├── cafeterias
├── stalls
└── images

review_db (MongoDB)
└── reviews (collection)

media_db (PostgreSQL)
├── media_files
├── image_metadata
└── upload_sessions

preference_db (PostgreSQL)
├── favorites
└── search_history
```

### Migration Script

We provide automated migration scripts:

```bash
# Linux/Mac
./scripts/migrate_monolith_to_microservices.sh

# Windows
scripts\migrate_monolith_to_microservices.bat
```

**What the script does:**

1. **Backup Original Database**
   ```bash
   mysqldump -u root -p nushungry_db > backup_monolith_$(date +%Y%m%d).sql
   ```

2. **Create PostgreSQL Databases**
   ```sql
   CREATE DATABASE admin_db;
   CREATE DATABASE cafeteria_db;
   CREATE DATABASE media_db;
   CREATE DATABASE preference_db;
   ```

3. **Migrate Data with Schema Conversion**
   - MySQL → PostgreSQL syntax conversion
   - Type mapping (DATETIME → TIMESTAMP, TEXT → VARCHAR)
   - AUTO_INCREMENT → SERIAL

4. **Create MongoDB Database**
   ```javascript
   use review_db
   db.createCollection('reviews')
   ```

5. **Migrate Reviews to MongoDB**
   ```bash
   python review-service/scripts/migrate_reviews_to_mongodb.py
   ```

6. **Validate Data Integrity**
   - Row count verification
   - Foreign key integrity
   - Sample data comparison

### Handling Shared Data

**Problem:** Multiple services need user data (foreign key to `users` table)

**Solution 1: Event-Driven Sync (Chosen)**
- admin-service publishes `UserCreated`, `UserUpdated`, `UserDeleted` events
- Other services consume events and cache user references
- **Trade-off:** Eventual consistency (acceptable for our use case)

**Solution 2: API Calls (Not Chosen)**
- Services call admin-service REST API for user data
- **Trade-off:** Network latency, tight coupling

**Solution 3: Data Duplication (Not Chosen)**
- Each service has its own users table
- **Trade-off:** High inconsistency risk

### Data Consistency

| Consistency Type | Services | Strategy |
|------------------|----------|----------|
| **Strong Consistency** | Admin (users) | Single source of truth |
| **Eventual Consistency** | Review ↔ Cafeteria | RabbitMQ events + retry |
| **No Consistency Required** | Media, Preference | Independent data |

---

## Service-by-Service Migration Guide

### 1. Review Service Migration

**Old Code (Monolith):**
```java
// src/main/java/com/nushungry/controller/ReviewController.java
@RestController
@RequestMapping("/api/reviews")
public class ReviewController {
    @Autowired
    private ReviewRepository reviewRepository;
    
    @PostMapping
    public Review createReview(@RequestBody Review review) {
        return reviewRepository.save(review);
    }
}
```

**New Code (Microservice):**
```java
// review-service/src/main/java/com/nushungry/reviewservice/controller/ReviewController.java
@RestController
@RequestMapping("/api/reviews")
public class ReviewController {
    @Autowired
    private ReviewService reviewService;
    
    @PostMapping
    public ReviewDTO createReview(@RequestBody CreateReviewRequest request) {
        Review review = reviewService.createReview(request);
        // Publish event to RabbitMQ
        eventPublisher.publishReviewCreated(review);
        return ReviewMapper.toDTO(review);
    }
}
```

**Data Migration:**
```bash
cd review-service/scripts
python migrate_reviews_to_mongodb.py --source mysql://localhost:3306/nushungry_db --target mongodb://localhost:27017/review_db
```

**Validation:**
```bash
# Compare row counts
mysql -u root -p -e "SELECT COUNT(*) FROM nushungry_db.reviews;"
mongosh review_db --eval "db.reviews.count()"

# Sample data check
mysql -u root -p -e "SELECT * FROM nushungry_db.reviews LIMIT 5;"
mongosh review_db --eval "db.reviews.find().limit(5)"
```

### 2. Cafeteria Service Migration

**Old Code:**
```java
@RestController
@RequestMapping("/api/cafeterias")
public class CafeteriaController {
    @Autowired
    private CafeteriaRepository cafeteriaRepository;
}
```

**New Code:**
```java
// cafeteria-service/src/main/java/com/nushungry/cafeteriaservice/controller/CafeteriaController.java
@RestController
@RequestMapping("/api/cafeterias")
public class CafeteriaController {
    @Autowired
    private CafeteriaService cafeteriaService;
    
    // Event listener for review updates
    @RabbitListener(queues = "review.created")
    public void handleReviewCreated(ReviewCreatedEvent event) {
        cafeteriaService.updateAverageRating(event.getStallId());
    }
}
```

**Data Migration:**
```bash
psql -U postgres -d cafeteria_db -f cafeteria-service/scripts/init_cafeteria_db.sql
```

### 3. Admin Service Migration

**Authentication Changes:**
```java
// Old: JWT utility in monolith
// src/main/java/com/nushungry/security/JwtUtil.java

// New: Centralized in admin-service
// admin-service/src/main/java/com/nushungry/adminservice/security/JwtUtil.java

@Service
public class JwtUtil {
    @Value("${jwt.secret}")
    private String secret;
    
    public String generateToken(UserDetails userDetails) {
        // Shared across all services via API calls
    }
}
```

**Other Services Validate Tokens:**
```java
// cafeteria-service/src/main/java/com/nushungry/cafeteriaservice/security/JwtAuthFilter.java
@Component
public class JwtAuthFilter extends OncePerRequestFilter {
    @Autowired
    private AdminServiceClient adminServiceClient;  // Feign client
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, ...) {
        String token = extractToken(request);
        // Validate via admin-service API
        User user = adminServiceClient.validateToken(token);
    }
}
```

### 4. Media Service Migration

**File Storage Changes:**

**Before (Monolith):**
```java
// Stored in local filesystem
String uploadDir = "./uploads/";
File file = new File(uploadDir + filename);
fileData.transferTo(file);
```

**After (Microservice):**
```java
// Stored in MinIO (S3-compatible)
@Service
public class MediaStorageService {
    @Autowired
    private MinioClient minioClient;
    
    public String uploadFile(MultipartFile file) {
        minioClient.putObject(
            PutObjectArgs.builder()
                .bucket("media-bucket")
                .object(filename)
                .stream(file.getInputStream(), file.getSize(), -1)
                .build()
        );
    }
}
```

**File Migration:**
```bash
# Copy files from local uploads/ to MinIO
mc mirror ./uploads/ local/media-bucket
```

### 5. Preference Service Migration

**Simplest Migration (No External Dependencies):**

```bash
# Extract tables from monolith
mysqldump -u root -p nushungry_db favorites search_history > preferences.sql

# Convert MySQL → PostgreSQL
sed 's/AUTO_INCREMENT/SERIAL/g' preferences.sql > preferences_pg.sql

# Import to PostgreSQL
psql -U postgres -d preference_db -f preferences_pg.sql
```

---

## Testing & Validation

### Pre-Migration Testing

**1. Monolith Baseline:**
```bash
# Capture baseline metrics
ab -n 1000 -c 10 http://localhost:8080/api/cafeterias
jmeter -n -t baseline_test.jmx -l baseline_results.jtl
```

### Migration Testing

**1. Data Integrity Tests:**
```bash
# Run automated validation
./scripts/validate_migration.sh

# Manual checks
# - Row counts match
# - Foreign keys intact
# - Sample data identical
# - No data loss
```

**2. Integration Tests:**
```bash
# Test service-to-service communication
cd admin-service && mvn test
cd cafeteria-service && mvn test
cd review-service && mvn test
cd media-service && mvn test
cd preference-service && mvn test
```

**3. End-to-End Tests:**
```bash
# Test full user journey
# 1. User login (admin-service)
# 2. Browse cafeterias (cafeteria-service)
# 3. View reviews (review-service)
# 4. Add to favorites (preference-service)
# 5. Upload photo (media-service)
```

**4. Performance Tests:**
```bash
# Compare against baseline
ab -n 1000 -c 10 http://localhost:8083/api/cafeterias

# Load testing
jmeter -n -t load_test.jmx -l microservices_results.jtl
```

**5. Chaos Testing:**
```bash
# Simulate service failures
docker-compose stop review-service
# Verify: Other services continue functioning

# Simulate database failure
docker-compose stop postgres
# Verify: Services handle gracefully
```

### Post-Migration Validation

**Checklist:**
- [ ] All services healthy (`/actuator/health` returns 200)
- [ ] No data loss (row counts match)
- [ ] Foreign key relationships intact
- [ ] API response times < baseline + 20%
- [ ] No error spikes in logs
- [ ] RabbitMQ queues draining normally
- [ ] All integration tests passing
- [ ] Load tests showing expected performance

---

## Rollback Plan

### When to Rollback

**Trigger Conditions:**
- ❌ >5% error rate sustained for >10 minutes
- ❌ Data loss detected
- ❌ Critical service unavailable for >15 minutes
- ❌ Performance degradation >50%

### Rollback Procedures

#### Quick Rollback (< 10 minutes)

**1. Revert to Monolith:**
```bash
# Stop microservices
docker-compose down

# Restore monolith
docker run -d -p 8080:8080 nushungry/monolith:stable
```

**2. Restore Database (if needed):**
```bash
# Restore from pre-migration backup
mysql -u root -p nushungry_db < backup_pre_migration_$(date +%Y%m%d).sql
```

#### Partial Rollback

**Rollback Single Service:**
```bash
# Keep other microservices, rollback one
docker-compose stop review-service

# Route traffic back to monolith for reviews
# Update load balancer/API gateway rules
```

### Rollback Testing

**Practice rollback before migration:**
```bash
# 1. Deploy microservices to staging
# 2. Simulate failure
# 3. Execute rollback
# 4. Verify system operational
# 5. Measure rollback time (target: <10 minutes)
```

---

## Post-Migration Tasks

### Immediate (Week 1)

- [ ] **Monitor Closely**
  - Set up alerts for error rates
  - Dashboard for key metrics
  - On-call rotation established

- [ ] **Performance Optimization**
  - Identify bottlenecks
  - Add database indexes
  - Tune connection pools

- [ ] **Documentation Updates**
  - Update API documentation
  - Update deployment guides
  - Team training on new architecture

### Short-Term (Month 1)

- [ ] **Cost Optimization**
  - Right-size containers
  - Optimize database queries
  - Review AWS resource usage

- [ ] **Security Hardening**
  - Rotate all secrets
  - Enable SSL/TLS
  - Security audit

- [ ] **Observability**
  - Centralized logging (ELK)
  - Distributed tracing (Zipkin)
  - Metrics dashboard (Grafana)

### Long-Term (Quarter 1)

- [ ] **Service Mesh (Optional)**
  - Evaluate Istio/Linkerd
  - Service-to-service authentication

- [ ] **API Gateway**
  - Spring Cloud Gateway
  - Rate limiting
  - Centralized authentication

- [ ] **Advanced Features**
  - Circuit breakers (Resilience4j)
  - Service discovery (Eureka)
  - Config server (Spring Cloud Config)

---

## Common Challenges & Solutions

### Challenge 1: Distributed Transactions

**Problem:** Creating a review requires updating both `reviews` collection (MongoDB) and `stalls.average_rating` (PostgreSQL).

**Solution:** **Saga Pattern with Compensation**
```java
@Transactional
public Review createReview(CreateReviewRequest request) {
    // 1. Save review to MongoDB
    Review review = reviewRepository.save(new Review(request));
    
    try {
        // 2. Publish event to update average rating
        eventPublisher.publishReviewCreated(review);
    } catch (Exception e) {
        // 3. Compensate: Delete review
        reviewRepository.delete(review);
        throw new ReviewCreationException("Failed to publish event", e);
    }
    
    return review;
}
```

### Challenge 2: Data Consistency

**Problem:** Review deleted but average rating not updated (RabbitMQ down).

**Solution:** **Retry + Dead Letter Queue**
```java
@RabbitListener(queues = "review.deleted")
public void handleReviewDeleted(ReviewDeletedEvent event) {
    try {
        cafeteriaService.updateAverageRating(event.getStallId());
    } catch (Exception e) {
        // Retry 3 times, then send to DLQ
        throw new AmqpRejectAndDontRequeueException(e);
    }
}
```

### Challenge 3: Network Latency

**Problem:** Microservices add network overhead (1-5ms per call).

**Solution:**
- **Caching:** Cache frequently accessed data (cafeteria metadata)
- **Batch APIs:** Fetch multiple reviews in one call
- **Async Events:** Don't wait for non-critical operations

### Challenge 4: Testing Complexity

**Problem:** Integration tests require all services running.

**Solution:**
- **Contract Testing:** Use Pact for consumer-driven contracts
- **Mock External Services:** Use WireMock for dependencies
- **Docker Compose for Tests:** Spin up all services in CI/CD

### Challenge 5: Monitoring Complexity

**Problem:** 5 services = 5 sets of logs to check.

**Solution:**
- **Centralized Logging:** ELK Stack (Elasticsearch, Logstash, Kibana)
- **Correlation IDs:** Track requests across services
- **Unified Dashboard:** Grafana with Prometheus metrics

---

## FAQ

### Q: Why not use a single database for all services?

**A:** Violates microservices principles:
- Services are coupled via shared schema
- Can't choose optimal database type per service (MongoDB for reviews, PostgreSQL for relational data)
- Schema changes require coordination across teams

### Q: How do you handle authentication across services?

**A:** **JWT tokens validated by each service:**
1. User logs in via admin-service → JWT token issued
2. Client includes token in requests to other services
3. Each service validates token signature (shared secret)
4. Optional: Call admin-service API to verify token if needed

### Q: What if RabbitMQ goes down?

**A:**
- **Short-term:** Services buffer events locally (eventually consistent)
- **Medium-term:** Dead letter queues for failed messages
- **Long-term:** Consider Kafka for higher availability

### Q: How to handle API versioning?

**A:**
- **URL versioning:** `/api/v1/reviews`, `/api/v2/reviews`
- **Header versioning:** `Accept: application/vnd.api+json;version=2`
- **Deprecation policy:** Support N-1 versions for 6 months

### Q: Can I run microservices on a single machine?

**A:** Yes (for development):
```bash
docker-compose up -d
# All services run on localhost with different ports
```

**But for production**, use:
- AWS ECS/EKS (container orchestration)
- Kubernetes (advanced orchestration)
- Docker Swarm (simpler orchestration)

### Q: What's the minimum team size for microservices?

**A:**
- **Ideal:** 2-3 engineers per service (10-15 total)
- **Minimum:** 3-5 engineers with strong DevOps skills
- **Too small:** 1-2 engineers → Stick with monolith

### Q: How much does this cost compared to monolith?

**A:**
- **Infrastructure:** 20-30% higher (more containers, databases)
- **Development:** Initially slower (learning curve), then faster (parallel work)
- **Maintenance:** Higher (more monitoring, deployment complexity)
- **Trade-off:** Worth it for teams >5 engineers and high-scale applications

---

## Conclusion

The migration from monolith to microservices is a **strategic investment** in scalability, team autonomy, and technological flexibility. While it introduces operational complexity, the benefits outweigh costs for growing applications.

**Key Success Factors:**
1. ✅ Clear service boundaries (domain-driven design)
2. ✅ Robust event-driven architecture (RabbitMQ)
3. ✅ Comprehensive testing (unit, integration, e2e)
4. ✅ Strong DevOps practices (CI/CD, monitoring)
5. ✅ Team buy-in and training

**Next Steps:**
- Monitor production performance
- Gather team feedback
- Iterate on architecture
- Plan advanced features (API Gateway, Service Mesh)

---

**Document Version:** 1.0  
**Migration Completed:** Q4 2024  
**Maintained By:** NUSHungry Architecture Team  
**Contact:** [architecture@nushungry.com](mailto:architecture@nushungry.com)
