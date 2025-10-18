# Preference Service

## Overview
This microservice is responsible for managing user preferences, including favorites (collection), sorting, batch deletion, and search history. It is designed to be a standalone Spring Boot service and can be integrated with other services in a microservices architecture.

## Features
- Add, remove, and list user favorites
- Batch remove favorites
- Sort favorites by creation time
- Add, remove, and list user search history
- Batch remove search history
- Clear all search history for a user
- Transactional operations for data consistency

## API Endpoints
### Favorites
- `POST /preference/favorite/add` — Add a favorite for a user
- `POST /preference/favorite/remove` — Remove a favorite for a user
- `POST /preference/favorite/batchRemove` — Batch remove favorites (userId as query param, stallIds as JSON array in body)
- `GET /preference/favorite/list?userId={userId}` — List all favorites for a user
- `GET /preference/favorite/sorted?userId={userId}` — List sorted favorites for a user

### Search History
- `POST /preference/search-history/add` — Add a search history record
- `POST /preference/search-history/remove` — Remove a search history record
- `POST /preference/search-history/batchRemove` — Batch remove search history (userId as query param, keywords as JSON array in body)
- `GET /preference/search-history/list?userId={userId}` — List all search history for a user
- `DELETE /preference/search-history/clear?userId={userId}` — Clear all search history for a user

## Technologies
- Java 17+
- Spring Boot 3+
- Spring Data JPA
- H2/PostgreSQL/MySQL (configurable)
- Maven

## Optional Extensions
- Redis or other caching solution for accelerating queries or caching stall details
- FeignClient/RestTemplate for synchronous calls to directory (cafeteria) service
- Message queue (RabbitMQ/Kafka) for event-driven cache/index refresh (if needed)

## How to Run
1. Configure database connection in `src/main/resources/application.properties`.
2. Build and run the service:
   ```bash
   mvn clean install
   mvn spring-boot:run
   ```
3. Use Postman or curl to test the API endpoints.

## Testing
- Run all unit tests:
  ```bash
  mvn test
  ```
- Test reports are generated in `target/surefire-reports/`.

## Notes
- Batch operations require POST method with JSON array in request body.
- All endpoints return standard JSON responses.
- For production, configure environment variables for sensitive settings.

## Contact
For questions or issues, please contact the project maintainer.

