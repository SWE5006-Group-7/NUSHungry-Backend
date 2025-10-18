# Media Service

## Overview
This microservice is responsible for handling media file operations, including file upload, storage, and URL generation. It is designed to work as a standalone Spring Boot service and can be integrated with other services in a microservices architecture.

## Features
- Receive and store uploaded media files (images, etc.)
- Accept files that have been cropped or processed by the frontend
- Generate and return accessible URLs for uploaded files
- Store file metadata in the database
- Support for local file storage (can be extended to cloud storage)
- Transactional operations for data consistency

## API Endpoints
- `POST /media/upload` — Upload a media file (accepts files processed/cropped by frontend)
- `GET /media/{fileName}` — Download or access a media file by file name
- Additional endpoints for file metadata management (if implemented)

## Technologies
- Java 17+
- Spring Boot 3+
- Spring Data JPA
- H2/PostgreSQL/MySQL (configurable)
- Maven

## Optional Extensions
- Cloud storage integration (e.g., AWS S3, Aliyun OSS)
- Image processing (thumbnail generation, format conversion)
- Caching for frequently accessed files
- Message queue integration for event-driven processing

## How to Run
1. Configure database and storage settings in `src/main/resources/application.properties`.
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
- The service expects files to be processed/cropped by the frontend before upload.
- For production, configure environment variables for sensitive settings and consider using cloud storage.

## Contact
For questions or issues, please contact the project maintainer.

