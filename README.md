# Multitenant API

A production-minded Spring Boot REST API for tenant-isolated project management.
Each authenticated request is resolved to a tenant through JWT claims, and all
project operations are scoped to that tenant automatically.

## Why this project

This project demonstrates practical multi-tenant backend patterns:

- Tenant-aware authentication and authorization
- Strong API boundaries that prevent cross-tenant data access
- Clean error handling and predictable API responses
- OpenAPI/Swagger-first developer experience
- Testable architecture with integration tests

## Tech stack

| Area | Choice |
|------|--------|
| Runtime | Java 17, Spring Boot 3.3.x |
| Security | Spring Security, JWT (JJWT), BCrypt |
| Data | Spring Data JPA, Hibernate, PostgreSQL |
| API docs | SpringDoc OpenAPI 3 (Swagger UI) |
| Rate limiting | Bucket4j (per-tenant limits on authenticated traffic) |
| Build | Maven |

## Core features

### Authentication

- `POST /api/auth/register` registers a user against an existing tenant.
- `POST /api/auth/login` returns a JWT for authenticated access.
- JWT validation is handled by `JwtFilter`.
- Tenant context is exposed via `UserPrincipal`.

### Tenant-scoped project management

- `GET /api/projects` (paginated)
- `GET /api/projects/{id}`
- `POST /api/projects`
- `PUT /api/projects/{id}`
- `DELETE /api/projects/{id}`

All project endpoints require authentication. Tenant identity comes from the JWT,
not from URL parameters, which helps enforce isolation at the API layer.

### Error handling and consistency

- `GlobalExceptionHandler` standardizes error responses.
- `ApiError` includes timestamp, status, message, and request path.
- `ResourceNotFoundException` maps to clean 404 responses.

### API documentation

- `GET /` redirects to Swagger UI.
- Swagger UI is available at `/swagger-ui.html` or `/swagger-ui/index.html`.
- Use `Bearer <token>` in the Swagger Authorize flow after login.

## Domain model

- `Tenant`: tenant account boundary
- `User`: email/password/role, linked to tenant
- `Project`: tenant-owned project entity
- `Task`: schema included, linked to project and user (API focus is auth + projects in this iteration)
- `BaseEntity`: shared id and audit timestamps

## Getting started

### Prerequisites

- JDK 17
- Maven 3.9+
- PostgreSQL running locally

Create a database named `multitenant` and ensure at least one tenant row exists
before calling the register endpoint (registration requires a valid `tenantId`).

### Environment variables

The app reads configuration from `src/main/resources/application.yml` and
expects sensitive values from environment variables.

| Variable | Purpose |
|----------|---------|
| `DB_URL` | JDBC URL (default `jdbc:postgresql://localhost:5432/multitenant`) |
| `DB_USERNAME` | Database username |
| `DB_PASSWORD` | Database password |
| `JWT_SECRET` | JWT signing secret (use a strong value) |
| `JWT_EXPIRATION_MS` | Token expiration in milliseconds |
| `SERVER_PORT` | Optional server port (default `8080`) |

### Run locally

```bash
set DB_URL=jdbc:postgresql://localhost:5432/multitenant
set DB_USERNAME=postgres
set DB_PASSWORD=<your-password>
set JWT_SECRET=<long-random-secret>
set JWT_EXPIRATION_MS=3600000
mvn spring-boot:run
```

Then open `http://localhost:8080/` to access Swagger.

### Test and verify

```bash
mvn test
mvn clean verify
```

## Typical API flow

1. Seed or create a tenant in the database.
2. Register a user with that tenant id.
3. Log in to receive a JWT.
4. Authorize in Swagger using `Bearer <token>`.
5. Call project CRUD endpoints.

If rate limits are exceeded, the API returns HTTP 429 with an `ApiError` payload.

## Project structure

```text
src/main/java/com/example/multitenantapi/
├── MultitenantApiApplication.java
├── auth/
├── config/
├── entity/
├── exception/
├── project/
├── repository/
├── security/
└── web/
```

## Notes

This is a strong foundation for a multi-tenant backend. Before production use,
rotate secrets, tighten security defaults, and add tenant lifecycle/admin APIs.
