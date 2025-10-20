# NUSHungry Backend - API Documentation

## Table of Contents
- [Overview](#overview)
- [Authentication](#authentication)
- [API Services](#api-services)
  - [Admin Service](#1-admin-service-8082)
  - [Cafeteria Service](#2-cafeteria-service-8083)
  - [Review Service](#3-review-service-8084)
  - [Media Service](#4-media-service-8085)
  - [Preference Service](#5-preference-service-8086)
- [Common Patterns](#common-patterns)
- [Error Handling](#error-handling)
- [Rate Limiting](#rate-limiting)
- [Swagger UI Access](#swagger-ui-access)

---

## Overview

The NUSHungry backend exposes RESTful APIs across 5 microservices. All services follow consistent patterns for authentication, error handling, and response formats.

### Base URLs

| Service | Base URL (Local) | Base URL (Production) |
|---------|------------------|----------------------|
| Admin | `http://localhost:8082` | `https://api.nushungry.com/admin` |
| Cafeteria | `http://localhost:8083` | `https://api.nushungry.com/cafeteria` |
| Review | `http://localhost:8084` | `https://api.nushungry.com/review` |
| Media | `http://localhost:8085` | `https://api.nushungry.com/media` |
| Preference | `http://localhost:8086` | `https://api.nushungry.com/preference` |

### Content Types
- **Request**: `application/json` (default) or `multipart/form-data` (file uploads)
- **Response**: `application/json`

---

## Authentication

### JWT Token-Based Authentication

Most endpoints require a valid JWT token in the `Authorization` header:

```http
Authorization: Bearer <JWT_TOKEN>
```

### Obtaining a Token

**Endpoint**: `POST /api/auth/login`  
**Service**: admin-service (Port 8082)

**Request**:
```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

**Response** (200 OK):
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "user": {
    "id": 1,
    "username": "john_doe",
    "email": "user@example.com",
    "role": "USER"
  },
  "expiresAt": "2024-10-20T10:00:00Z"
}
```

### Token Expiration
- **Default**: 24 hours (86400000 milliseconds)
- **Refresh**: Re-authenticate with login endpoint

### Public Endpoints (No Auth Required)
- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/auth/forgot-password`
- `POST /api/auth/reset-password`
- `GET /api/cafeterias` (read-only)
- `GET /api/stalls` (read-only)
- `GET /actuator/health` (all services)

---

## API Services

## 1. Admin Service (8082)

### 1.1 Authentication Endpoints

#### Register User
```http
POST /api/auth/register
Content-Type: application/json
```

**Request Body**:
```json
{
  "username": "john_doe",
  "email": "john@example.com",
  "password": "SecurePass123!"
}
```

**Response** (201 Created):
```json
{
  "id": 1,
  "username": "john_doe",
  "email": "john@example.com",
  "role": "USER",
  "enabled": true,
  "createdAt": "2024-10-19T10:00:00Z"
}
```

**Validation Rules**:
- `username`: 3-50 characters, alphanumeric + underscore
- `email`: Valid email format
- `password`: Minimum 8 characters, at least 1 uppercase, 1 lowercase, 1 digit

---

#### Login
```http
POST /api/auth/login
Content-Type: application/json
```

**Request Body**:
```json
{
  "email": "john@example.com",
  "password": "SecurePass123!"
}
```

**Response** (200 OK):
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "user": {
    "id": 1,
    "username": "john_doe",
    "email": "john@example.com",
    "role": "USER"
  },
  "expiresAt": "2024-10-20T10:00:00Z"
}
```

---

#### Forgot Password
```http
POST /api/auth/forgot-password
Content-Type: application/json
```

**Request Body**:
```json
{
  "email": "john@example.com"
}
```

**Response** (200 OK):
```json
{
  "message": "Password reset code sent to your email",
  "expiresIn": 900  // 15 minutes in seconds
}
```

**Email Content**:
- 6-digit verification code (e.g., `123456`)
- Valid for 15 minutes

---

#### Reset Password
```http
POST /api/auth/reset-password
Content-Type: application/json
```

**Request Body**:
```json
{
  "email": "john@example.com",
  "code": "123456",
  "newPassword": "NewSecurePass123!"
}
```

**Response** (200 OK):
```json
{
  "message": "Password reset successful"
}
```

---

### 1.2 Admin User Management (Admin Only)

#### List All Users
```http
GET /api/admin/users?page=0&size=20
Authorization: Bearer <ADMIN_TOKEN>
```

**Response** (200 OK):
```json
{
  "content": [
    {
      "id": 1,
      "username": "john_doe",
      "email": "john@example.com",
      "role": "USER",
      "enabled": true,
      "createdAt": "2024-10-19T10:00:00Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 100,
  "totalPages": 5
}
```

---

#### Update User
```http
PUT /api/admin/users/{id}
Authorization: Bearer <ADMIN_TOKEN>
Content-Type: application/json
```

**Request Body**:
```json
{
  "role": "ADMIN",
  "enabled": false
}
```

**Response** (200 OK):
```json
{
  "id": 1,
  "username": "john_doe",
  "email": "john@example.com",
  "role": "ADMIN",
  "enabled": false,
  "updatedAt": "2024-10-19T11:00:00Z"
}
```

---

#### Delete User
```http
DELETE /api/admin/users/{id}
Authorization: Bearer <ADMIN_TOKEN>
```

**Response** (204 No Content)

---

### 1.3 Dashboard Statistics (Admin Only)

```http
GET /api/admin/dashboard/stats
Authorization: Bearer <ADMIN_TOKEN>
```

**Response** (200 OK):
```json
{
  "totalUsers": 1500,
  "totalCafeterias": 15,
  "totalStalls": 120,
  "totalReviews": 8450,
  "averageRating": 4.2,
  "newUsersToday": 25,
  "newReviewsToday": 87
}
```

---

## 2. Cafeteria Service (8083)

### 2.1 Cafeteria Endpoints

#### List All Cafeterias
```http
GET /api/cafeterias?page=0&size=20
```

**Query Parameters**:
- `page` (optional): Page number (default: 0)
- `size` (optional): Items per page (default: 20)
- `sort` (optional): Sort field (e.g., `name,asc`)

**Response** (200 OK):
```json
{
  "content": [
    {
      "id": 1,
      "name": "The Deck",
      "location": "Faculty of Engineering",
      "latitude": 1.2996,
      "longitude": 103.7739,
      "description": "Popular cafeteria with diverse food options",
      "averageRating": 4.3,
      "totalReviews": 523,
      "imageUrl": "http://localhost:8085/media/cafeteria_1.jpg",
      "stalls": [
        {
          "id": 1,
          "name": "Western Food",
          "cuisineType": "Western",
          "averageRating": 4.5
        }
      ]
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 15,
  "totalPages": 1
}
```

---

#### Get Cafeteria by ID
```http
GET /api/cafeterias/{id}
```

**Response** (200 OK):
```json
{
  "id": 1,
  "name": "The Deck",
  "location": "Faculty of Engineering",
  "latitude": 1.2996,
  "longitude": 103.7739,
  "description": "Popular cafeteria with diverse food options",
  "averageRating": 4.3,
  "totalReviews": 523,
  "openingHours": "Mon-Fri: 7:00 AM - 9:00 PM",
  "images": [
    {
      "id": 1,
      "url": "http://localhost:8085/media/cafeteria_1_main.jpg",
      "altText": "Main entrance"
    }
  ],
  "stalls": [
    {
      "id": 1,
      "name": "Western Food",
      "cuisineType": "Western",
      "averageRating": 4.5,
      "totalReviews": 187
    }
  ]
}
```

---

#### Search Cafeterias
```http
GET /api/cafeterias/search?q=deck&minRating=4.0
```

**Query Parameters**:
- `q` (required): Search keyword (name, location, description)
- `minRating` (optional): Minimum average rating filter
- `maxDistance` (optional): Max distance from coordinates (future)

**Response** (200 OK): Same as List All Cafeterias

---

### 2.2 Stall Endpoints

#### List All Stalls
```http
GET /api/stalls?page=0&size=20
```

**Response** (200 OK):
```json
{
  "content": [
    {
      "id": 1,
      "name": "Western Food",
      "cafeteriaId": 1,
      "cafeteriaName": "The Deck",
      "cuisineType": "Western",
      "description": "Grilled chicken, pasta, burgers",
      "averageRating": 4.5,
      "totalReviews": 187,
      "openingHours": "Mon-Fri: 7:00 AM - 8:00 PM",
      "imageUrl": "http://localhost:8085/media/stall_1.jpg"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 120,
  "totalPages": 6
}
```

---

#### Get Stall by ID
```http
GET /api/stalls/{id}
```

**Response** (200 OK):
```json
{
  "id": 1,
  "name": "Western Food",
  "cafeteria": {
    "id": 1,
    "name": "The Deck"
  },
  "cuisineType": "Western",
  "description": "Grilled chicken, pasta, burgers, fries",
  "averageRating": 4.5,
  "totalReviews": 187,
  "openingHours": "Mon-Fri: 7:00 AM - 8:00 PM",
  "images": [
    {
      "id": 2,
      "url": "http://localhost:8085/media/stall_1_food.jpg",
      "altText": "Signature grilled chicken"
    }
  ]
}
```

---

#### Get Stalls by Cafeteria
```http
GET /api/stalls/cafeteria/{cafeteriaId}
```

**Response** (200 OK): Array of stall objects

---

### 2.3 Admin Cafeteria Management (Admin Only)

#### Create Cafeteria
```http
POST /api/admin/cafeterias
Authorization: Bearer <ADMIN_TOKEN>
Content-Type: application/json
```

**Request Body**:
```json
{
  "name": "New Cafeteria",
  "location": "UTown",
  "latitude": 1.3057,
  "longitude": 103.7727,
  "description": "Modern cafeteria with international cuisine"
}
```

**Response** (201 Created): Cafeteria object

---

#### Update Cafeteria
```http
PUT /api/admin/cafeterias/{id}
Authorization: Bearer <ADMIN_TOKEN>
Content-Type: application/json
```

**Request Body**: Same as Create

**Response** (200 OK): Updated cafeteria object

---

#### Delete Cafeteria
```http
DELETE /api/admin/cafeterias/{id}
Authorization: Bearer <ADMIN_TOKEN>
```

**Response** (204 No Content)

---

## 3. Review Service (8084)

### 3.1 Review Endpoints

#### Create Review
```http
POST /api/reviews
Authorization: Bearer <TOKEN>
Content-Type: application/json
```

**Request Body**:
```json
{
  "stallId": 1,
  "stallName": "Western Food",
  "rating": 5,
  "content": "Excellent grilled chicken! Portion size is generous."
}
```

**Validation Rules**:
- `stallId`: Required, positive integer
- `rating`: Required, integer between 1-5
- `content`: Optional, max 1000 characters

**Response** (201 Created):
```json
{
  "id": "507f1f77bcf86cd799439011",
  "userId": 1,
  "userName": "john_doe",
  "stallId": 1,
  "stallName": "Western Food",
  "rating": 5,
  "content": "Excellent grilled chicken! Portion size is generous.",
  "likesCount": 0,
  "likedBy": [],
  "comments": [],
  "createdAt": "2024-10-19T10:30:00Z",
  "updatedAt": "2024-10-19T10:30:00Z"
}
```

---

#### Get Review by ID
```http
GET /api/reviews/{id}
```

**Response** (200 OK): Review object (same as Create response)

---

#### Update Review
```http
PUT /api/reviews/{id}
Authorization: Bearer <TOKEN>
Content-Type: application/json
```

**Request Body**:
```json
{
  "rating": 4,
  "content": "Updated: Food is good but can be pricey."
}
```

**Authorization**: Only the review author can update

**Response** (200 OK): Updated review object

---

#### Delete Review
```http
DELETE /api/reviews/{id}
Authorization: Bearer <TOKEN>
```

**Authorization**: Only the review author or admin can delete

**Response** (204 No Content)

---

#### Get Reviews by Stall
```http
GET /api/reviews/stall/{stallId}?page=0&size=20&sortBy=createdAt
```

**Query Parameters**:
- `page` (optional): Page number (default: 0)
- `size` (optional): Items per page (default: 20)
- `sortBy` (optional): `createdAt` (newest first), `likesCount` (most liked first), `rating` (highest first)

**Response** (200 OK):
```json
{
  "content": [
    {
      "id": "507f1f77bcf86cd799439011",
      "userId": 1,
      "userName": "john_doe",
      "stallId": 1,
      "rating": 5,
      "content": "Excellent grilled chicken!",
      "likesCount": 15,
      "commentsCount": 3,
      "createdAt": "2024-10-19T10:30:00Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 187,
  "totalPages": 10
}
```

---

### 3.2 Like Endpoints

#### Like a Review
```http
POST /api/reviews/{id}/like
Authorization: Bearer <TOKEN>
```

**Response** (200 OK):
```json
{
  "reviewId": "507f1f77bcf86cd799439011",
  "likesCount": 16,
  "liked": true
}
```

---

#### Unlike a Review
```http
DELETE /api/reviews/{id}/like
Authorization: Bearer <TOKEN>
```

**Response** (200 OK):
```json
{
  "reviewId": "507f1f77bcf86cd799439011",
  "likesCount": 15,
  "liked": false
}
```

---

### 3.3 Comment Endpoints

#### Add Comment to Review
```http
POST /api/reviews/{id}/comments
Authorization: Bearer <TOKEN>
Content-Type: application/json
```

**Request Body**:
```json
{
  "content": "I totally agree! Their chicken is amazing."
}
```

**Response** (201 Created):
```json
{
  "commentId": "abc123def456",
  "userId": 2,
  "userName": "jane_smith",
  "content": "I totally agree! Their chicken is amazing.",
  "createdAt": "2024-10-19T11:00:00Z"
}
```

---

#### Reply to Comment
```http
POST /api/reviews/{reviewId}/comments/{commentId}/replies
Authorization: Bearer <TOKEN>
Content-Type: application/json
```

**Request Body**:
```json
{
  "content": "Thanks! You should try their pasta too."
}
```

**Response** (201 Created):
```json
{
  "replyId": "xyz789abc123",
  "userId": 1,
  "userName": "john_doe",
  "content": "Thanks! You should try their pasta too.",
  "createdAt": "2024-10-19T11:05:00Z"
}
```

---

#### Delete Comment
```http
DELETE /api/reviews/{reviewId}/comments/{commentId}
Authorization: Bearer <TOKEN>
```

**Authorization**: Only the comment author or admin can delete

**Response** (204 No Content)

---

## 4. Media Service (8085)

### 4.1 File Upload

#### Upload Single File
```http
POST /media/upload
Authorization: Bearer <TOKEN>
Content-Type: multipart/form-data
```

**Form Data**:
- `file`: Image file (JPEG, PNG, WebP)
- `entityType`: `cafeteria`, `stall`, or `review`
- `entityId`: ID of the associated entity

**Example with cURL**:
```bash
curl -X POST http://localhost:8085/media/upload \
  -H "Authorization: Bearer <TOKEN>" \
  -F "file=@/path/to/image.jpg" \
  -F "entityType=stall" \
  -F "entityId=1"
```

**Response** (201 Created):
```json
{
  "id": 1,
  "originalFilename": "grilled_chicken.jpg",
  "storedFilename": "1634567890_grilled_chicken.jpg",
  "filePath": "/media/2024/10/19/1634567890_grilled_chicken.jpg",
  "url": "http://localhost:8085/media/1634567890_grilled_chicken.jpg",
  "fileSize": 524288,
  "mimeType": "image/jpeg",
  "entityType": "stall",
  "entityId": 1,
  "uploadedBy": 1,
  "createdAt": "2024-10-19T10:30:00Z",
  "metadata": {
    "width": 1920,
    "height": 1080,
    "format": "JPEG",
    "thumbnailUrl": "http://localhost:8085/media/thumb_1634567890_grilled_chicken.jpg"
  }
}
```

---

#### Upload Multiple Files (Batch)
```http
POST /media/upload/batch
Authorization: Bearer <TOKEN>
Content-Type: multipart/form-data
```

**Form Data**:
- `files[]`: Multiple image files
- `entityType`: `cafeteria`, `stall`, or `review`
- `entityId`: ID of the associated entity

**Response** (201 Created):
```json
{
  "uploadedFiles": [
    { "id": 1, "url": "http://localhost:8085/media/file1.jpg" },
    { "id": 2, "url": "http://localhost:8085/media/file2.jpg" }
  ],
  "totalUploaded": 2,
  "failedUploads": []
}
```

---

### 4.2 File Retrieval

#### Get File Metadata
```http
GET /media/{id}
```

**Response** (200 OK): Media file object (same as upload response)

---

#### Get Files by Entity
```http
GET /media/cafeteria/{cafeteriaId}
GET /media/stall/{stallId}
GET /media/review/{reviewId}
```

**Response** (200 OK):
```json
[
  {
    "id": 1,
    "url": "http://localhost:8085/media/image1.jpg",
    "thumbnailUrl": "http://localhost:8085/media/thumb_image1.jpg",
    "altText": "Cafeteria entrance",
    "createdAt": "2024-10-19T10:00:00Z"
  }
]
```

---

### 4.3 File Deletion

```http
DELETE /media/{id}
Authorization: Bearer <TOKEN>
```

**Authorization**: Only the uploader or admin can delete

**Response** (204 No Content)

---

## 5. Preference Service (8086)

### 5.1 Favorites

#### Add Favorite
```http
POST /preference/favorites
Authorization: Bearer <TOKEN>
Content-Type: application/json
```

**Request Body**:
```json
{
  "stallId": 1
}
```

**Response** (201 Created):
```json
{
  "id": 1,
  "userId": 1,
  "stallId": 1,
  "stallName": "Western Food",
  "cafeteriaName": "The Deck",
  "createdAt": "2024-10-19T10:30:00Z"
}
```

---

#### Get User Favorites
```http
GET /preference/favorites
Authorization: Bearer <TOKEN>
```

**Response** (200 OK):
```json
[
  {
    "id": 1,
    "stallId": 1,
    "stallName": "Western Food",
    "cafeteriaName": "The Deck",
    "averageRating": 4.5,
    "createdAt": "2024-10-19T10:30:00Z"
  }
]
```

---

#### Remove Favorite
```http
DELETE /preference/favorites/{id}
Authorization: Bearer <TOKEN>
```

**Response** (204 No Content)

---

#### Batch Add Favorites
```http
POST /preference/favorites/batch
Authorization: Bearer <TOKEN>
Content-Type: application/json
```

**Request Body**:
```json
{
  "stallIds": [1, 2, 3, 4]
}
```

**Response** (201 Created):
```json
{
  "addedCount": 4,
  "favorites": [ /* array of favorite objects */ ]
}
```

---

#### Batch Remove Favorites
```http
DELETE /preference/favorites/batch
Authorization: Bearer <TOKEN>
Content-Type: application/json
```

**Request Body**:
```json
{
  "favoriteIds": [1, 2, 3]
}
```

**Response** (200 OK):
```json
{
  "removedCount": 3
}
```

---

### 5.2 Search History

#### Add Search History
```http
POST /preference/search-history
Authorization: Bearer <TOKEN>
Content-Type: application/json
```

**Request Body**:
```json
{
  "keyword": "chicken rice"
}
```

**Response** (201 Created):
```json
{
  "id": 1,
  "userId": 1,
  "keyword": "chicken rice",
  "searchTime": "2024-10-19T10:30:00Z"
}
```

---

#### Get Search History
```http
GET /preference/search-history?limit=10
Authorization: Bearer <TOKEN>
```

**Query Parameters**:
- `limit` (optional): Max number of results (default: 20)

**Response** (200 OK):
```json
[
  {
    "keyword": "chicken rice",
    "lastSearched": "2024-10-19T10:30:00Z",
    "searchCount": 5
  },
  {
    "keyword": "western food",
    "lastSearched": "2024-10-18T15:20:00Z",
    "searchCount": 3
  }
]
```

---

#### Clear Search History
```http
DELETE /preference/search-history
Authorization: Bearer <TOKEN>
```

**Response** (204 No Content)

---

#### Batch Delete Search Keywords
```http
DELETE /preference/search-history/batch
Authorization: Bearer <TOKEN>
Content-Type: application/json
```

**Request Body**:
```json
{
  "keywords": ["chicken rice", "western food"]
}
```

**Response** (200 OK):
```json
{
  "deletedCount": 2
}
```

---

## Common Patterns

### Pagination

Most list endpoints support pagination:

**Request**:
```http
GET /api/cafeterias?page=0&size=20&sort=name,asc
```

**Response**:
```json
{
  "content": [ /* array of items */ ],
  "page": 0,
  "size": 20,
  "totalElements": 100,
  "totalPages": 5,
  "first": true,
  "last": false
}
```

### Filtering

Use query parameters for filtering:
```http
GET /api/stalls?cuisineType=Western&minRating=4.0
```

### Sorting

Specify sort field and direction:
```http
GET /api/reviews?sortBy=likesCount&direction=desc
```

---

## Error Handling

### Standard Error Response

```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Cafeteria with ID 999 not found",
  "path": "/api/cafeterias/999",
  "timestamp": "2024-10-19T10:30:00Z"
}
```

### Common HTTP Status Codes

| Code | Meaning | Example |
|------|---------|---------|
| 200 | OK | Successful GET, PUT |
| 201 | Created | Successful POST |
| 204 | No Content | Successful DELETE |
| 400 | Bad Request | Invalid input, validation error |
| 401 | Unauthorized | Missing or invalid JWT token |
| 403 | Forbidden | User lacks permission |
| 404 | Not Found | Resource doesn't exist |
| 409 | Conflict | Duplicate resource (e.g., email already exists) |
| 422 | Unprocessable Entity | Business logic error |
| 500 | Internal Server Error | Server-side error |

### Validation Error Response

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "timestamp": "2024-10-19T10:30:00Z",
  "errors": {
    "name": "Name is required",
    "email": "Invalid email format",
    "rating": "Rating must be between 1 and 5"
  }
}
```

---

## Rate Limiting

### Current Status
- **Not implemented yet** (planned for future releases)

### Planned Limits
- **Authenticated users**: 100 requests/minute
- **Anonymous users**: 20 requests/minute
- **Admin users**: 500 requests/minute

**Response when rate limit exceeded** (429 Too Many Requests):
```json
{
  "status": 429,
  "error": "Too Many Requests",
  "message": "Rate limit exceeded. Retry after 60 seconds.",
  "retryAfter": 60,
  "timestamp": "2024-10-19T10:30:00Z"
}
```

---

## Swagger UI Access

Interactive API documentation is available via Swagger UI for each service:

| Service | Swagger UI URL |
|---------|----------------|
| Admin | http://localhost:8082/swagger-ui.html |
| Cafeteria | http://localhost:8083/swagger-ui.html |
| Review | http://localhost:8084/swagger-ui.html |
| Media | http://localhost:8085/swagger-ui.html |
| Preference | http://localhost:8086/swagger-ui.html |

### Using Swagger UI

1. **Access the URL** for your desired service
2. **Authenticate**: Click "Authorize" button, enter `Bearer <YOUR_JWT_TOKEN>`
3. **Try endpoints**: Expand endpoint, click "Try it out", fill parameters, execute

---

## Postman Collection

**Coming Soon**: Downloadable Postman collection with pre-configured requests for all endpoints.

Location: `docs/postman/NUSHungry_API_Collection.json`

---

## Support

For API questions or issues:
- Check [DEVELOPMENT.md](./DEVELOPMENT.md) for local setup
- Create an issue in the repository
- Contact the development team

---

**Last Updated**: October 2024  
**API Version**: 1.0
