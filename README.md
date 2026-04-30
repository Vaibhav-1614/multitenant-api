# Multitenant API

Spring Boot REST API for **multi-tenant project management**: users belong to a tenant; **JWT** carries tenant context; **projects** are fully CRUD-scoped per tenant.

## Stack

| Area | Choice |
|------|--------|
| Runtime | Java **17**, Spring Boot **3.3.x** |
| Persistence | Spring Data JPA, Hibernate, **PostgreSQL** (H2 in tests) |
| Security | Spring Security, **JWT** (JJWT), BCrypt passwords |
| API docs | **SpringDoc OpenAPI 3** (Swagger UI) |
| Rate limiting | **Bucket4j** — **100 requests / minute per tenant** (authenticated traffic) |
| Packaging | Maven; optional **Docker** + **docker-compose** |

## What’s included

### Domain model (`entity`, `repository`)

- **BaseEntity** — shared auditing fields (`id`, `createdAt`, `updatedAt`).
- **Tenant** — billing/plan-friendly tenant record.
- **User** — email, hashed password, `UserRole`, linked **tenant**.
- **Project** — name, description, status; scoped by **tenant**.
- **Task** — linked to **Project** + **User**, `TaskStatus` enum  
  *(schema present; REST surface in this iteration focuses on **projects** and **auth**.)*

### Auth (`auth`, `security`)

- `POST /api/auth/register` — create user against an existing **tenant ID** (`RegisterRequest`: email, password, role, tenantId).
- `POST /api/auth/login` — returns **JWT** for subsequent calls.
- **JwtFilter** validates Bearer tokens; **UserPrincipal** exposes user id + **tenant id** for downstream code.
- **SecurityConfig** — stateless session, JWT filter + **RateLimitFilter** chain.

### Tenant-scoped projects (`project`)

- `GET /api/projects` — paginated list (**Spring Data Pageable**, tenant-only).
- `GET /api/projects/{id}`
- `POST /api/projects`
- `PUT /api/projects/{id}`
- `DELETE /api/projects/{id}`  

All `/api/projects/**` endpoints require authentication; tenant comes from JWT, **not** from the URL.

### Cross-cutting (`exception`)

- **GlobalExceptionHandler** + **ApiError** JSON error shape (timestamp, HTTP status, message, path).
- **ResourceNotFoundException** mapped to clean 404s.

### Entry point & docs

- **`GET /**` redirects to **Swagger UI** so the base URL is useful in the browser (`web/RootController`).
- OpenAPI UI: **`/swagger-ui.html`** (or `/swagger-ui/index.html`).  
  Use **Authorize** with `Bearer <token>` after login.

### Tests (`src/test`)

- **MockMvc** integration tests against **auth** endpoints with **H2** (`application-test.yml`).

### Docker

- **`Dockerfile`** — multi-stage JVM image for the app.
- **`docker-compose.yml`** — **Postgres 16** + app; uses **development-only** defaults for DB/JWT (`postgres` DB password inside the compose network). Override with real secrets in production.

---

## Prerequisites

- **JDK 17** and **Maven 3.9+** (or Docker Desktop for compose builds).
- A **PostgreSQL** database named **`multitenant`** (when not using Compose), and at least one **tenant** row registered so registrations can reference a valid **`tenant_id`**.

---

## Configuration & secrets

The app reads settings from **`src/main/resources/application.yml`**. Sensitive values come from **environment variables** — **do not commit real production passwords or JWT secrets.**

| Variable | Purpose |
|----------|---------|
| `DB_URL` | JDBC URL (default `jdbc:postgresql://localhost:5432/multitenant`) |
| `DB_USERNAME` | DB user |
| `DB_PASSWORD` | DB password (**set explicitly for local Postgres** when not using default `postgres`) |
| `JWT_SECRET` | Base64-compatible signing secret (**change in prod**) |
| `JWT_EXPIRATION_MS` | Token lifetime |
| `SERVER_PORT` | Optional (default **8080**) |

**Committed files:** only **defaults** (`postgres`-style placeholders in YAML and Compose for local/docker dev). `.gitignore` excludes `.env`, `.env.local`, `target/`, and `.local-tools/`.

---

## Run locally (Maven)

```bash
cd multitenant-api
export DB_URL=jdbc:postgresql://localhost:5432/multitenant
export DB_USERNAME=postgres
export DB_PASSWORD=<your-password>
export JWT_SECRET=<long-random-base64-compatible-string>
export JWT_EXPIRATION_MS=3600000
mvn spring-boot:run
```

Then open **http://localhost:8080/** (redirects to Swagger).

```bash
mvn test          # integration tests with H2
mvn clean verify # full CI-style build
```

## Run with Docker Compose

```bash
docker compose up --build
```

App: **http://localhost:8080** — DB credentials match `docker-compose.yml` unless you change them.

---

## Quick API workflow (Swagger)

1. Ensure a tenant exists (e.g. seed `tenants` in DB).
2. **Register** with `tenantId` pointing at that tenant.
3. **Login**, copy JWT.
4. **Authorize** in Swagger (`Bearer <token>`).
5. Call **Projects** CRUD.

Rate limits apply **per tenant** after authentication (429 with JSON **ApiError** when exceeded).

---

## Project layout (high level)

```
src/main/java/com/example/multitenantapi/
├── MultitenantApiApplication.java
├── auth/
├── config/          SecurityConfig, SwaggerConfig
├── entity/
├── exception/
├── project/
├── repository/
├── security/        JwtFilter, JwtUtil, RateLimitFilter, UserPrincipal
└── web/             RootController (redirect to Swagger)
```

---

## License / disclaimer

Toy **example** codebase: replace default JWT secrets, tighten security defaults, add production-ready observability and tenant provisioning APIs before exposing publicly.
