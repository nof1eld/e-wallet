# E-Wallet Service

A mini electronic wallet REST API built with Spring Boot, supporting concurrency-safe deposits, withdrawals, and transfers between accounts, with JWT authentication and role-based access control.

## How to run the project

### Prerequisites
- Java 17+
- Maven (or use the included `./mvnw`)
- Docker and Docker Compose if you want to run the full stack containerized

### Running with Docker
```bash
docker compose up --build
```
This starts both the Spring Boot app and PostgreSQL. The app runs on `http://localhost:8080` and Postgres is available on `localhost:5432`.

### Running locally
```bash
./mvnw spring-boot:run
```
This mode expects a PostgreSQL database running on `localhost:5432` with the credentials from `src/main/resources/application.properties`.

- API docs (Swagger UI): `http://localhost:8080/swagger-ui/index.html`

### Running tests
```bash
./mvnw test
```
Includes concurrency tests proving correctness under simultaneous operations (see `ConcurrencyTest.java`).

### Getting started
Accounts are not seeded automatically. To create one:
```bash
# 1. Register a user
curl -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username": "alice", "password": "yourpassword"}'

# 2. Use the returned token to create an account for yourself
curl -X POST http://localhost:8080/accounts \
  -H "Authorization: Bearer <token>"
```

## Architecture

Standard layered architecture:
- **`controller/`** — HTTP concerns only (request/response mapping)
- **`service/`** — business logic
- **`repository/`** — data access (Spring Data JPA)
- **`entity/`** — database-mapped models
- **`dto/`** — API-facing request/response shapes, kept separate from entities
- **`exception/`** — custom exceptions + a centralized `GlobalExceptionHandler` for consistent error responses
- **`event/`** — deferred processing (audit logging, notifications), decoupled from business logic via Spring's event system
- **`config/`** — security setup (JWT filter, authorization rules)

### Key design decisions
Soon




## Trade-offs and things I'd do differently with more time
Soon
