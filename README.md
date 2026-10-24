# DevFlow

## Purpose
DevFlow is a full-stack web application designed for software development teams to manage projects, team members, sprints, issues, comments, activity history, and development progress.

## Technology Stack
- **Backend:** Java 21, Spring Boot 3, Spring Data JPA, Hibernate, Spring Security (JWT)
- **Frontend:** React 19, Vite, Axios
- **Database:** PostgreSQL (Supabase)
- **Build Tools:** Maven, npm

## Architecture
The application is built as a well-structured modular monolith.
- **Backend:** Layered architecture (Controller -> Service -> Repository -> Database) using DTOs for the API layer.
- **Frontend:** Feature-oriented React structure avoiding "everything in App.jsx" patterns.

## Development Prerequisites
- Java 21
- Maven
- Node.js & npm
- Docker (optional, for local database)
- Git

## Current Project Status
- [x] Initial project foundation (Backend and Frontend)
- [x] Supabase PostgreSQL connection & Flyway V1 schema versioning
- [x] Core domain entities, enums, and JPA repositories
- [x] Service layer with domain business logic
- [x] REST API controllers & global exception handling
- [x] Spring Security, JWT authentication, and RBAC / IDOR authorization
- [x] Advanced pagination, sorting, dynamic specifications, and search
- [x] OpenAPI 3 (Swagger UI) documentation
- [x] Real PostgreSQL integration testing with Testcontainers
- [x] Backend observability, Actuator health checks, and structured logging
- [x] Backend API quality, semantic status codes, and production hardening
- [x] Full-Stack Feature Integration (Projects, Sprints, Issues, Kanban, Comments, Activity, Dashboard, Notifications, Global Search)
- [x] Containerization, Docker Compose, and Environment Configuration (Step 29)
- [x] Continuous Integration Pipeline with GitHub Actions (Step 30)

## Testing

DevFlow includes a comprehensive automated testing suite across both backend and frontend:
- **Backend Tests (294 total):** 239 unit & WebMvc tests (`mvn test "-Dtest=!*IntegrationTest"`) and 55 Testcontainers PostgreSQL integration tests (`mvn test "-Dtest=*IntegrationTest"`).
- **Frontend Tests (283 total):** 283 component, service, routing, and integration tests across 44 test files (`npm test -- --run`).
- **Database Integration Tests:** Real database tests using Testcontainers running PostgreSQL (`postgres:15-alpine`), verifying Flyway migrations, Hibernate validation, constraint enforcement, and dynamic JPA criteria queries.
- **Observability & Actuator Tests:** Automated tests verifying `/actuator/health`, `/actuator/info`, `X-Request-ID` correlation, and error sanitization.
- **API Hardening & Quality Regression Tests:** MockMvc regression tests validating malformed JSON handling, unsupported HTTP methods, input validation, sensitive field masking, pagination boundaries, and RBAC / IDOR security.

For detailed REST API & hardening documentation, see [docs/api.md](docs/api.md).
For detailed testing documentation, see [docs/testing.md](docs/testing.md).
For observability and logging conventions, see [docs/observability.md](docs/observability.md).

```bash
# Run all tests (unit + integration)
mvn clean test

# Run only PostgreSQL integration tests
mvn test -Dtest=*IntegrationTest
```

## Running Locally

### Backend
1. Navigate to the `backend` directory.
2. Ensure you have set up your `.env` file (see `.env.example`).
3. Run with Maven: `mvn spring-boot:run`
4. Access Swagger UI at `http://localhost:8080/swagger-ui/index.html`
5. Access Health Check at `http://localhost:8080/actuator/health`
6. Access Application Info at `http://localhost:8080/actuator/info`

### Frontend
1. Navigate to the `frontend` directory.
2. Install dependencies: `npm install`
3. Start the Vite development server: `npm run dev`

---

## Docker Development

DevFlow provides a production-like local container stack utilizing multi-stage Docker builds and Docker Compose.

### 1. Prerequisites
- [Docker](https://docs.docker.com/get-docker/) (v20.10+)
- [Docker Compose](https://docs.docker.com/compose/) (v2.0+)
- An active Supabase PostgreSQL database instance

### 2. Environment Variables
Create a `.env` file in the project root by copying the template:
```bash
cp .env.example .env
```
Populate the required credentials in `.env`:
- `DB_URL`: Supabase JDBC URL (`jdbc:postgresql://db.xxxxxx.supabase.co:5432/postgres`)
- `DB_USERNAME`: Database username (`postgres`)
- `DB_PASSWORD`: Database password
- `JWT_SECRET`: Minimum 256-bit secret key for HMAC-SHA token signing
- `JWT_EXPIRATION`: Token validity in milliseconds (default: `3600000` = 1 hour)
- `CORS_ALLOWED_ORIGINS`: Allowed origins (e.g. `http://localhost:5173,http://localhost:3000,http://localhost:80`)
- `SERVER_PORT`: Backend port (default: `8080`)
- `VITE_API_BASE_URL`: URL used by browser clients to reach backend (default: `http://localhost:8080`)

> [!NOTE]
> **Supabase PostgreSQL is External**: Docker Compose intentionally does NOT spin up a local PostgreSQL container. The Spring Boot backend connects directly to Supabase over the network.

### 3. Build & Run Containers
To validate, build images, and start the local production-like stack:
```bash
# Validate compose configuration
docker compose config

# Build container images
docker compose build

# Start services in the background
docker compose up -d
```

### 4. Stopping Containers
```bash
# Stop and remove containers and network
docker compose down
```

### 5. Access Endpoints & Health Probes
- **Frontend SPA**: `http://localhost:3000`
- **Backend API**: `http://localhost:8080`
- **Backend Health Check**: `http://localhost:8080/actuator/health`
- **Backend Info**: `http://localhost:8080/actuator/info`
- **Swagger UI**: `http://localhost:8080/swagger-ui/index.html`

### 6. Architecture & Client Communication
```text
Browser Client (User's machine)
    ├── Loads Static HTML/JS ──> React Container (Nginx :80 mapped to :3000)
    └── Dispatches API Calls  ──> Spring Boot Container (Tomcat :8080)
                                      └── JDBC Queries ──> Supabase PostgreSQL (External)
```
- **Direct Browser Communication**: Because the React frontend executes inside the user's browser, API calls route to `VITE_API_BASE_URL` (`http://localhost:8080`), protected by CORS.
- **Reverse Proxy Fallback**: Nginx is also configured to forward any relative `/api/` requests to `http://backend:8080/api/` inside `devflow-network`.
- **SPA Client Routing**: Nginx uses `try_files $uri $uri/ /index.html;` to ensure deep routes (`/dashboard`, `/projects/:id`, `/issues/:id`, `/notifications`, `/search`) load seamlessly without HTTP 404 errors on browser refresh.

### 7. Execution Environments Comparison
| Dimension | Normal Local Development | Dockerized Local Execution | Future Cloud Deployment |
|---|---|---|---|
| **Backend** | `mvn spring-boot:run` (hot recompile) | Eclipse Temurin 21 JRE container | Containerized / Managed App Service |
| **Frontend** | Vite Dev Server (`npm run dev` with HMR) | Nginx Alpine serving optimized build | CDN / Edge static web hosting |
| **Routing** | Vite dev server history fallback | Nginx `try_files` SPA fallback | Web server / CDN rewrite rules |
| **Database** | External Supabase PostgreSQL | External Supabase PostgreSQL | External Supabase PostgreSQL |

---

## CI/CD

DevFlow uses **GitHub Actions** for automated continuous integration to ensure high code quality, security, and build determinism across every pull request and push to the `main` branch.

### Workflow Architecture (`.github/workflows/ci.yml`)

The pipeline executes three independent jobs concurrently:

```text
GitHub Push / Pull Request (main)
        ├──> backend-test   (JDK 21, Maven cache, Unit tests, Testcontainers PostgreSQL)
        ├──> frontend-test  (Node 20, npm cache, Vitest, Oxlint, Vite build)
        └──> docker-build   (Docker Compose config validation, Multi-stage image builds)
```

1. **`backend-test` (Backend Unit & Integration Tests)**:
   - Sets up Eclipse Temurin JDK 21 with automatic Maven dependency caching (`backend/pom.xml`).
   - Executes fast unit and WebMvc tests: `mvn test "-Dtest=!*IntegrationTest" -B`.
   - Executes real PostgreSQL integration tests via Testcontainers: `mvn test "-Dtest=*IntegrationTest" -B`.
2. **`frontend-test` (Frontend Quality & Build Validation)**:
   - Sets up Node.js 20 LTS with deterministic lockfile caching (`frontend/package-lock.json`).
   - Installs dependencies cleanly via `npm ci`.
   - Runs unit and component test suites: `npm test -- --run`.
   - Runs code linter: `npm run lint`.
   - Validates production bundle generation: `npm run build`.
3. **`docker-build` (Docker Compose & Image Build Validation)**:
   - Validates Compose schema syntax without requiring real credentials: `docker compose --env-file .env.example config`.
   - Builds both multi-stage production images (`devflow-backend` and `devflow-frontend`) via `docker compose --env-file .env.example build`.

### Quality Gates
- **Branch Protection**: Pull requests targeting `main` require all three jobs (`backend-test`, `frontend-test`, `docker-build`) to pass before merge.
- **Secret Hygiene**: CI does not require or log production Supabase credentials or JWT secrets. Build validations utilize standard non-sensitive defaults.
- **Fail-Fast Philosophy**: No tests or checks use swallowed exit codes or `continue-on-error`.

---

## Production Configuration

DevFlow is architected for cloud-native deployment with strict separation between local development, Dockerized staging, CI automation, and cloud production.

### 1. Backend Production Environment Variables
The Spring Boot backend runtime requires the following environment variables supplied by the cloud platform:

| Variable | Required | Description | Example / Format |
|---|---|---|---|
| `DB_URL` | **Yes** | Production PostgreSQL JDBC connection string with SSL | `jdbc:postgresql://db.<ref>.supabase.co:5432/postgres?sslmode=require` |
| `DB_USERNAME` | **Yes** | Database username | `postgres` |
| `DB_PASSWORD` | **Yes** | Secure production database password | *(Cloud Secret)* |
| `JWT_SECRET` | **Yes** | Cryptographic secret for HMAC-SHA256 (>= 256 bits / 32 chars) | *(Cloud Secret)* |
| `JWT_EXPIRATION` | No | Token lifespan in milliseconds (default: `3600000` = 1 hr) | `3600000` |
| `CORS_ALLOWED_ORIGINS` | **Yes** | Exact HTTPS origin(s) of the deployed frontend | `https://devflow.example.com` |
| `SERVER_PORT` | No | Listener port for the embedded Tomcat server | `8080` (or cloud provider `$PORT`) |

### 2. Frontend Production Build Configuration
- `VITE_API_BASE_URL` is a **build-time frontend configuration value, NOT a runtime secret**.
- Vite statically inlines `import.meta.env.VITE_API_BASE_URL` into the compiled JavaScript bundle during `npm run build`.
- The cloud deployment pipeline or Docker build must provide this variable targeting the production backend HTTPS URL (e.g. `https://api.devflow.example.com`).
- **Never put secrets into any `VITE_*` variable** as all frontend code is publicly readable in the client browser.

### 3. Production Database Strategy (Supabase)
- **Authoritative Provider**: Supabase PostgreSQL remains the persistent cloud production database.
- **Schema Management**: Flyway executes versioned schema migrations (`V1`, `V2`, `V3`) at application startup.
- **Validation**: Hibernate `ddl-auto` is locked to `validate`, ensuring no automatic DDL modifications, drops, or table alterations occur outside Flyway migrations.
- **Encrypted Transport**: All production JDBC connections must enforce `?sslmode=require`.

### 4. Cryptographic JWT Key Generation
- The backend enforces a minimum 256-bit (32 character) key for HMAC-SHA256 signature generation.
- Production keys must be generated using a cryptographically secure pseudo-random number generator (CSPRNG):
  ```bash
  openssl rand -base64 32
  ```
- If an unconfigured or weak secret (< 32 characters) is supplied, the backend immediately fails fast at startup.

### 5. Production CORS Enforcement
- `CORS_ALLOWED_ORIGINS` must be explicitly configured to match only the deployed frontend origin (e.g., `https://devflow.example.com`).
- Wildcard origins (`*`) are prohibited when credentials (`allowCredentials=true`) are enabled; attempting to configure `*` triggers a startup validation error.
- Multiple trusted origins can be comma-separated without trailing slashes.

### 6. HTTPS & Transport Security
- Both frontend and backend are designed under the assumption of HTTPS in production.
- Transport Layer Security (TLS) termination is handled upstream by the cloud load balancer or edge reverse proxy (e.g., Cloudflare, ALB, Cloud Run ingress).
- Nginx provides client security headers (`X-Frame-Options`, `X-Content-Type-Options`, `X-XSS-Protection`, `Referrer-Policy: strict-origin-when-cross-origin`).

### 7. Actuator & Observability Security
- Only `/actuator/health` and `/actuator/info` are exposed over HTTP.
- `/actuator/health` is configured with `show-details=never` to prevent leaking internal database versions or disk layouts to unauthenticated requests.
- Standard liveness and readiness probes are enabled at `/actuator/health/liveness` and `/actuator/health/readiness`.
- Environment variable exposure via `/actuator/info` is explicitly disabled (`management.info.env.enabled=false`).

### 8. Cloud Secrets & Environment Segregation
- **Zero Committed Secrets**: The repository contains only sanitized templates (`.env.example`, `.env.production.example`, `frontend/.env.production.example`).
- **Git Protection**: `.gitignore` ignores `.env` and `.env.*` across all subdirectories while preserving example templates.
- **Cloud Delivery**: Production credentials (`DB_PASSWORD`, `JWT_SECRET`) must be injected exclusively via the cloud hosting platform's secret manager or container environment settings at runtime.

---

## Render Deployment — Backend Web Service

DevFlow's Spring Boot backend is configured for deployment as a **Render Web Service** powered by Docker.

### 1. Service Specifications
- **Service Type**: Web Service
- **Runtime**: Docker
- **Root Directory**: `backend` (or repo root pointing to `backend/Dockerfile`)
- **Docker Context**: `backend` (or repo root depending on dashboard root setting)
- **Dockerfile Path**: `Dockerfile` (or `backend/Dockerfile`)
- **Instance Type**: Starter / Free / Standard (requires >= 512 MB RAM recommended for JVM)
- **Health Check Path**: `/actuator/health`

### 2. Port Behavior
- Render automatically injects a `PORT` environment variable (typically `10000`).
- The Spring Boot backend resolves its listener port dynamically via `server.port=${PORT:${SERVER_PORT:8080}}`.
- The application automatically binds to Render's injected `PORT` without requiring manual port override.

### 3. Database Persistence
- **Provider**: External Supabase PostgreSQL instance.
- **Schema Lifecycle**: Flyway automatically validates and applies migrations (`V1`, `V2`, `V3`) against the database on container boot.
- **JPA Safety**: Hibernate `ddl-auto=validate` enforces that schema alterations occur strictly via versioned Flyway scripts.

### 4. Required Render Environment Variables
Configure the following in the Render Dashboard under **Environment**:

| Key | Value / Guidance | Type |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://db.<project-ref>.supabase.co:5432/postgres?sslmode=require` | Secret / Variable |
| `DB_USERNAME` | `postgres` (or Supabase db user) | Plaintext |
| `DB_PASSWORD` | Your Supabase database password | **Secret** |
| `JWT_SECRET` | 256-bit key (`openssl rand -base64 32`) | **Secret** |
| `CORS_ALLOWED_ORIGINS` | Initial: `http://localhost:5173` (update to Render frontend URL once deployed in Step 32B) | Plaintext |
| `JWT_EXPIRATION` | `3600000` *(Optional, defaults to 1 hr)* | Plaintext |

> [!IMPORTANT]
> Never commit production credentials to GitHub. Inject all database passwords and JWT secrets exclusively via Render's Environment settings.

---

## Render Deployment — Frontend Static Site

DevFlow's React SPA frontend is prepared for high-performance edge deployment as a **Render Static Site**.

### 1. Service Specifications
- **Service Type**: Static Site
- **Build Environment**: Node.js
- **Root Directory**: `frontend` (if configuring service root) or repo root
- **Build Command**: `npm ci && npm run build` (or `cd frontend && npm ci && npm run build`)
- **Publish Directory**: `dist` (or `frontend/dist`)

### 2. Environment Variables (Build-Time Only)
Configure under **Environment** in the Render Dashboard:

| Key | Description | Type |
|---|---|---|
| `VITE_API_BASE_URL` | Public HTTPS URL of the deployed Spring Boot backend API | Plaintext (Build-Time) |

> [!WARNING]
> **No Secrets in Frontend**: `VITE_API_BASE_URL` is statically inlined into compiled client JavaScript by Vite during `npm run build`. Never put database credentials, passwords, JWT secrets, or private keys into `VITE_*` variables.

### 3. SPA Client-Side Routing (Rewrite Rule)
Because DevFlow utilizes React Router for deep client routes (`/dashboard`, `/projects/:id`, `/issues/:id`, `/notifications`, `/search`), direct browser navigation or page refreshes require an SPA rewrite rule.

In the Render Dashboard under **Redirects / Rewrites**, add:
- **Type**: `Rewrite`
- **Source**: `/*`
- **Destination**: `/index.html`

This ensures Render's edge server returns `/index.html` with HTTP 200 for all client route requests while preserving direct static asset resolution for `/assets/*`.






