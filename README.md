# Resource Booking System

RESTful backend for booking resources (rooms, equipment, and similar assets) with JWT authentication, role-based access control, and conflict-aware reservations.

## Features

- JWT login (stateless, BCrypt password hashing)
- Resource CRUD (ADMIN write, USER/ADMIN read)
- Reservation CRUD with ownership rules
- Reservation filtering by status, minPrice, and maxPrice
- Pagination and sorting
- Overlap/conflict detection for active reservations
- Bean Validation and consistent JSON error responses
- OpenAPI / Swagger UI

## Technology stack

- Java 21
- Spring Boot 4.1.1
- Spring Web MVC
- Spring Data JPA / Hibernate
- Spring Security
- JWT (Nimbus JOSE+JWT)
- MySQL
- Bean Validation
- Springdoc OpenAPI
- JUnit 5, Mockito, Spring Boot Test
- Maven Wrapper

## Prerequisites

- JDK 21
- Maven Wrapper (included)
- Local MySQL 8+ listening on `localhost:3306`
- Existing database named `resource_booking` (do not create a second database)

The application maps to these existing tables: `users`, `resources`, `reservations`. Hibernate is configured with `ddl-auto=update` so it will not drop or recreate the schema.

## MySQL setup

Database name: `resource_booking`

Database username: `brtrackrecipt`

Prefer supplying the database password through the `DB_PASSWORD` environment variable instead of committing credentials. Local development can fall back to the defaults in `src/main/resources/application.properties`.

Do not confuse the MySQL account with application logins. The MySQL user is only for JDBC. Application users are rows in the `users` table (see seed users below).

## Environment variables

| Variable | Purpose | Local default |
| --- | --- | --- |
| `DB_URL` | JDBC URL | `jdbc:mysql://localhost:3306/resource_booking` |
| `DB_USERNAME` | Database username | `brtrackrecipt` |
| `DB_PASSWORD` | Database password | local default in `application.properties` |
| `JWT_SECRET` | HMAC secret (at least 32 bytes) | development-only secret in `application.properties` |
| `JWT_EXPIRATION` | Token lifetime in milliseconds | `86400000` |

Never use the database password as the JWT secret.

## How to run (Windows)

```powershell
.\mvnw.cmd spring-boot:run
```

## How to build

```powershell
.\mvnw.cmd clean package
```

## How to test

```powershell
.\mvnw.cmd test
```

## Swagger

After the application is running:

- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs

Use **Authorize** and paste `Bearer <token>` after login.

## Seed application users

On startup the seeder creates these users if they do not already exist (passwords are BCrypt hashed):

| Username | Password | Role stored in DB | Spring authority |
| --- | --- | --- | --- |
| `admin` | `Admin@123` | `ADMIN` | `ROLE_ADMIN` |
| `user` | `User@123` | `USER` | `ROLE_USER` |

## Example login

```http
POST /auth/login
Content-Type: application/json

{
  "username": "user",
  "password": "User@123"
}
```

Response:

```json
{
  "token": "JWT_TOKEN"
}
```

Send the token on later requests:

```http
Authorization: Bearer JWT_TOKEN
```

## Example API requests

Create a resource (ADMIN):

```http
POST /resources
Authorization: Bearer <admin-token>
Content-Type: application/json

{
  "name": "Conference Room A",
  "description": "Large meeting room",
  "type": "ROOM",
  "available": true
}
```

Create a reservation (USER or ADMIN). Ownership comes from the JWT, never from a client `userId`:

```http
POST /reservations
Authorization: Bearer <user-token>
Content-Type: application/json

{
  "resourceId": 1,
  "startTime": "2026-09-01T10:00:00",
  "endTime": "2026-09-01T12:00:00",
  "price": 500.00
}
```

List reservations with filters, pagination, and sorting:

```http
GET /reservations?status=CONFIRMED&minPrice=100&maxPrice=1000&page=0&size=10&sort=price,desc
```

## Role permissions

| Endpoint | USER | ADMIN |
| --- | --- | --- |
| `POST /auth/login` | public | public |
| `GET /resources`, `GET /resources/{id}` | yes | yes |
| `POST/PUT/DELETE /resources` | no | yes |
| `POST /reservations` | own booking | booking for the authenticated admin user |
| `GET /reservations` | own rows only | all rows |
| `GET /reservations/{id}` | own row only | any row |
| `PUT/DELETE /reservations/{id}` | no | yes |

## Project structure

```
src/main/java/com/roshan/resourcebooking/
  ResourceBookingSystemApplication.java
  config/
  controller/
  dto/
  entity/
  exception/
  repository/
  security/
  service/
```
