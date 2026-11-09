
# Task Management REST API

A secure Task Management REST API built with Java 25, Spring Boot 4, Spring Security, JWT authentication, Spring Data JPA, and MySQL.

The application allows users to register, log in, and manage their own tasks. Each task belongs to a registered user, and users cannot access or modify tasks belonging to another user.

## Features

- User registration with BCrypt password hashing
- User login with JWT authentication
- Stateless API security
- Create, read, update, and delete tasks
- Task ownership enforcement
- Task status: TODO, IN_PROGRESS, DONE
- Task due dates
- Pagination and optional status filtering
- Bean Validation for request validation
- Consistent JSON error responses
- Automated unit, controller, and integration tests
- H2 in-memory database for integration testing

## Technology Stack

| Technology | Purpose |
|---|---|
| Java 25 | Programming language |
| Spring Boot 4.0.9 | Backend framework |
| Spring Web MVC | REST API development |
| Spring Security | Authentication and authorization |
| JWT (HS256) | Token-based authentication |
| BCrypt | Password hashing |
| Spring Data JPA / Hibernate | Database persistence |
| MySQL 8 | Application database |
| H2 | Automated integration testing |
| Maven Wrapper | Build and dependency management |
| JUnit 5 / Mockito / MockMvc | Automated testing |

## Prerequisites

Install:

- Java JDK 25
- MySQL Server 8
- Git
- Postman (optional)

The project includes Maven Wrapper, so a separate Maven installation is not required.

## Database Setup

Create a MySQL database:

```sql
CREATE DATABASE task_management_db;
```

Create a dedicated application user and grant database access. Use your own secure password.

```sql
CREATE USER 'task_app'@'localhost'
IDENTIFIED BY 'REPLACE_WITH_A_STRONG_PASSWORD';

GRANT ALL PRIVILEGES
ON task_management_db.*
TO 'task_app'@'localhost';
```

If the database user already exists, skip the user creation command.

## Environment Variables

The application reads sensitive configuration values from environment variables.

Required variables:

| Variable | Description |
|---|---|
| DB_USERNAME | MySQL application username |
| DB_PASSWORD | MySQL application password |
| JWT_SECRET | Base64-encoded secret key, at least 32 decoded bytes |

Example for Windows PowerShell:

```powershell
$env:DB_USERNAME = "task_app"

$securePassword = Read-Host "Enter database password" -AsSecureString
$env:DB_PASSWORD = [System.Net.NetworkCredential]::new("", $securePassword).Password
Remove-Variable securePassword

$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$key = New-Object byte[] 32
$rng.GetBytes($key)
$env:JWT_SECRET = [Convert]::ToBase64String($key)
$rng.Dispose()
Remove-Variable rng, key
```

Never commit real passwords, JWT secrets, or generated access tokens.

## Run the Application

Clone the repository:

```bash
git clone https://github.com/KaushaniHettiarachchige/task-management-api
cd task-management-api
```

Set the required environment variables in the same terminal before running the application.

On Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

On macOS/Linux:

```bash
./mvnw spring-boot:run
```

Application base URL:

`http://localhost:8080`

## API Endpoints

| Method | Endpoint | Authentication | Description |
|---|---|---|---|
| POST | /api/auth/register | Public | Register a new user |
| POST | /api/auth/login | Public | Log in and receive JWT |
| POST | /api/tasks | JWT required | Create a task |
| GET | /api/tasks | JWT required | List own tasks |
| GET | /api/tasks/{id} | JWT required | Get own task |
| PUT | /api/tasks/{id} | JWT required | Update own task |
| DELETE | /api/tasks/{id} | JWT required | Delete own task |

## API Request Examples

### Register

`POST /api/auth/register`

```json
{
  "email": "user@example.com",
  "password": "Password123!"
}
```

Returns HTTP 201 Created.

### Login

`POST /api/auth/login`

```json
{
  "email": "user@example.com",
  "password": "Password123!"
}
```

Successful response example:

```json
{
  "accessToken": "YOUR_JWT_TOKEN",
  "tokenType": "Bearer",
  "expiresIn": 3600
}
```

### JWT Authorization

For protected endpoints, send:

`Authorization: Bearer YOUR_JWT_TOKEN`

Tokens expire after 60 minutes by default.

### Create Task

`POST /api/tasks`

```json
{
  "title": "Complete Backend Assignment",
  "description": "Implement and test the REST API",
  "status": "TODO",
  "dueDate": "2026-11-20"
}
```

Returns HTTP 201 Created.

### List Tasks

`GET /api/tasks?page=0&size=10`

Filter by status:

`GET /api/tasks?page=0&size=10&status=DONE`

Results are returned in a paginated JSON response.

### Get Task

`GET /api/tasks/1`

Returns HTTP 200 if the task belongs to the authenticated user.

### Update Task

`PUT /api/tasks/1`

```json
{
  "title": "Complete Backend Assignment",
  "description": "Implementation and testing completed",
  "status": "DONE",
  "dueDate": "2026-11-20"
}
```

Returns HTTP 200 on success.

### Delete Task

`DELETE /api/tasks/1`

Returns HTTP 204 No Content on success.

## Security

- Passwords are stored as BCrypt hashes, not plaintext.
- JWT uses HS256 signing.
- APIs use stateless authentication.
- Registration and login are public endpoints.
- Task endpoints require JWT authentication.
- Tasks are queried using both task ID and authenticated owner ID.
- Users cannot read, update, or delete another user's tasks.
- Missing or invalid JWT tokens result in HTTP 401.
- Access-denied responses use HTTP 403 where applicable.
- Non-owned tasks return HTTP 404 to avoid revealing their existence.

## Validation and Error Handling

Request validation is performed using Jakarta Bean Validation.

Examples include:

- Invalid or missing email
- Password length outside permitted limits
- Missing task title
- Invalid task status
- Duplicate registration
- Invalid login credentials
- Missing or inaccessible task

Errors use a consistent JSON structure:

```json
{
  "timestamp": "2026-11-10T00:00:00Z",
  "status": 401,
  "error": "Unauthorized",
  "message": "Authentication is required or the token is invalid",
  "path": "/api/tasks"
}
```

Depending on the error, validation field details may also be included.

## Automated Testing

The project contains 25 automated tests covering authentication, tasks, controller behavior, and task ownership security.

| Test Class | Tests |
|---|---:|
| AuthServiceTest | 6 |
| TaskServiceTest | 7 |
| AuthControllerTest | 5 |
| TaskSecurityIntegrationTest | 6 |
| TaskManagementApiApplicationTests | 1 |
| Total | 25 |

The test suite uses:

- JUnit and Mockito for service unit testing
- MockMvc for controller tests
- Spring Boot integration testing
- H2 in-memory database for integration tests

Run all tests:

Windows:

```powershell
.\mvnw.cmd clean test
```

macOS/Linux:

```bash
./mvnw clean test
```

Run the full Maven verification lifecycle:

```powershell
.\mvnw.cmd clean verify
```

Expected result: BUILD SUCCESS with zero test failures.

## Design Decisions

### Layered Architecture

The application separates responsibilities across Controllers, Services, Repositories, Entities, DTOs, Configuration, and Exception Handling.

### DTO-Based API Responses

DTOs prevent unnecessary exposure of internal entity fields such as password hashes and simplify API contracts.

### Database-Level Task Ownership Filtering

Task queries include the authenticated owner ID to prevent users from retrieving or modifying another user's tasks.

### Stateless JWT Authentication

JWT authentication avoids server-side login sessions and enables protected REST endpoints.

### Environment-Based Secrets

Database credentials and JWT signing secrets are externalized rather than hardcoded.

### Isolated Integration Testing

Integration tests use an H2 database to avoid modifying the development MySQL database.

## Known Limitations

- JWT refresh tokens and token revocation are not implemented.
- Automated ownership tests use mocked JWT authentication and do not independently verify JWT cryptographic signatures.
- Schema updates currently use Hibernate `ddl-auto=update` rather than versioned migrations.
- Advanced task search and configurable sorting are not implemented.
- No separate ADMIN task management functionality is implemented.

## Future Improvements

- Refresh token support
- JWT revocation/logout mechanism
- OpenAPI/Swagger documentation
- Database migrations with Flyway or Liquibase
- Task keyword searching and configurable sorting
- More comprehensive API integration tests
- Centralized structured logging and monitoring

## Author

Kaushani Hettiarachchige
BSc (Hons) in Information Technology specialize in Software Engineering
SLIIT
