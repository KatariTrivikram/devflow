# DevFlow Observability, Health Checks & Logging Guide

## 1. Overview

DevFlow implements a lightweight, production-ready observability architecture designed to provide operational visibility, container probe integration, request correlation, and security auditing without exposing internal secrets or depending on heavy external infrastructure.

```
Incoming Request
      │
      ▼
┌─────────────────────────┐
│     RequestIdFilter     │  ◄── Extracts/generates 'X-Request-ID' & populates SLF4J MDC
└───────────┬─────────────┘
            ▼
┌─────────────────────────┐
│   SecurityFilterChain   │  ◄── Authenticates JWT, evaluates RBAC, logs security events
└───────────┬─────────────┘
            ▼
┌─────────────────────────┐
│  Controllers / Services │  ◄── Logs business lifecycle events (INFO / WARN)
└───────────┬─────────────┘
            ▼
┌─────────────────────────┐
│ GlobalExceptionHandler  │  ◄── Logs 500 stack traces server-side; sanitizes client response
└─────────────────────────┘
```

---

## 2. Spring Boot Actuator Endpoints

DevFlow integrates `spring-boot-starter-actuator` configured to expose only essential operational endpoints beneath `/actuator`.

### 2.1. Available Endpoints

| Endpoint | HTTP Method | Public Access | Purpose |
| :--- | :--- | :--- | :--- |
| **`/actuator/health`** | `GET` | Yes | Container liveness/readiness probes; verifies PostgreSQL database connectivity. |
| **`/actuator/info`** | `GET` | Yes | Exposes application metadata (name, version, description) without secrets. |

### 2.2. Health Endpoint (`/actuator/health`)
Configured with `management.endpoint.health.show-details=always`, the health endpoint evaluates both application uptime and real PostgreSQL connectivity via Spring Boot's standard `DataSourceHealthIndicator`.

#### Sample Response:
```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP",
      "details": {
        "database": "PostgreSQL",
        "validationQuery": "isValid()"
      }
    },
    "diskSpace": {
      "status": "UP",
      "details": {
        "total": 510842273792,
        "free": 312543940608,
        "threshold": 10485760,
        "path": "D:\\projects\\Dev_Flow\\devflow\\backend\\.",
        "exists": true
      }
    },
    "ping": {
      "status": "UP"
    }
  }
}
```

### 2.3. Application Information Endpoint (`/actuator/info`)
Configured with `management.info.env.enabled=true` and custom safe metadata keys under `info.app.*`.

#### Sample Response:
```json
{
  "app": {
    "name": "DevFlow Backend",
    "description": "DevFlow Project and Issue Management Platform",
    "version": "0.0.1-SNAPSHOT",
    "java": {
      "version": "21"
    }
  }
}
```

### 2.4. Actuator Security Decision
- **Why Public?** Cloud container orchestrators (e.g., Kubernetes, Docker healthcheck, AWS ECS, Google Cloud Run) require unauthenticated access to probe `/actuator/health` to determine if a container should receive traffic or restart.
- **Why Strictly Filtered?** Sensitive operational endpoints (`/actuator/env`, `/actuator/beans`, `/actuator/heapdump`, `/actuator/configprops`, `/actuator/threaddump`) are **excluded** via `management.endpoints.web.exposure.include=health,info`. Furthermore, Spring Security's `SecurityFilterChain` enforces `.anyRequest().authenticated()` for all unpermitted paths, preventing unauthorized access.

---

## 3. Request Correlation (`X-Request-ID`)

### 3.1. How It Works
1. **Extraction or Generation:** The [`RequestIdFilter`](file:///d:/projects/Dev_Flow/devflow/backend/src/main/java/com/devflow/filter/RequestIdFilter.java) runs with `Ordered.HIGHEST_PRECEDENCE` as the first servlet filter before Spring Security.
2. **Preservation:** If the client provides an `X-Request-ID` HTTP header, it is preserved (trimmed and capped at 64 characters to prevent header injection). If absent, a random `UUIDv4` is generated.
3. **MDC Injection:** The request ID is added to SLF4J's Mapped Diagnostic Context (MDC) under key `requestId`.
4. **Response Header:** The `X-Request-ID` is stamped onto the HTTP response header for both successful and error responses.
5. **Thread-Safe Cleanup:** In a `finally` block, `MDC.remove("requestId")` is executed, preventing thread pool contamination.

---

## 4. Structured Logging Conventions

### 4.1. Correlation Log Pattern
Spring Boot's log formatting includes the application name and correlated request ID:
```properties
logging.pattern.correlation=[${spring.application.name:},%X{requestId:-}] 
```

#### Sample Log Output:
```text
2026-10-14T15:38:08.912+05:30  WARN 16908 --- [devflow-test] [devflow,1c2710d9-1cd7-4f0b-ac0c-80c97a0e2fac] c.d.s.CustomAuthenticationEntryPoint : Unauthorized access attempt to [GET /api/users]: Full authentication is required to access this resource
2026-10-14T15:38:09.396+05:30  INFO 16908 --- [devflow-test] [devflow,trace-custom-999] com.devflow.service.AuthService : User logged in successfully: userId=d9d4d512-1b47-4a4e-ba44-9776c24db4df, email=jane.doe@example.com
```

### 4.2. Log Level Standards

| Level | Intended Usage | Examples |
| :--- | :--- | :--- |
| **`INFO`** | Key application lifecycle milestones and successful high-value business operations. | Application started, user registered, user logged in, project created/deleted, issue created/assigned. |
| **`WARN`** | Recoverable, unexpected, or suspicious events that do not halt the application. | JWT validation failure, invalid credentials, unauthorized access attempt, access denied (RBAC). |
| **`ERROR`** | Unexpected runtime exceptions, database failures, or system faults requiring investigation. | Unhandled 500 exceptions, transaction failures, infrastructure connectivity drops. |
| **`DEBUG`** | Granular diagnostic information for local development troubleshooting. | Resource not found details, dynamic specification query building details. |

### 4.3. Sensitive Information Security Rules (What Never to Log)
Under **no circumstances** should the following appear in application logs:
- ❌ Plaintext passwords or password hashes.
- ❌ Raw JWT tokens or `Authorization` headers.
- ❌ Database passwords or Supabase connection strings containing credentials.
- ❌ Sensitive request payload bodies (e.g., registration request password fields).
- ❌ Client secret keys or HMAC signing keys.

---

## 5. Global Exception Handling & Response Sanitization

The [`GlobalExceptionHandler`](file:///d:/projects/Dev_Flow/devflow/backend/src/main/java/com/devflow/exception/GlobalExceptionHandler.java) ensures a strict separation between internal diagnostics and client responses:

1. **Server-Side Diagnostics:**
   ```java
   log.error("Unhandled exception occurred while processing request [{} {}]: {}",
           request.getMethod(), request.getRequestURI(), ex.getMessage(), ex);
   ```
   The full exception message and stack trace are logged to the server logs accompanied by the request's `X-Request-ID`.

2. **Client-Facing Sanitization:**
   ```json
   {
     "status": 500,
     "message": "An unexpected internal server error occurred",
     "path": "/api/projects/123"
   }
   ```
   The client receives an HTTP 500 `ErrorResponse` with zero stack traces, SQL syntax details, or infrastructure clues.

---

## 6. How Developers Can Debug Failures Using Request IDs

When a client or frontend experiences an API error:
1. **Locate the Request ID:** Inspect the HTTP response headers in the browser Network tab or API client for `X-Request-ID`:
   ```http
   HTTP/1.1 500 Internal Server Error
   X-Request-ID: c9a4b271-88f1-419b-a312-34f71a0e8821
   ```
2. **Search the Server Logs:** Filter server logs using the correlation ID:
   ```bash
   grep "c9a4b271-88f1-419b-a312-34f71a0e8821" application.log
   ```
3. **Instant Root Cause:** The search isolates the exact sequence of events for that single HTTP request—from entry into `JwtAuthenticationFilter`, through service execution, down to the exact stack trace logged by `GlobalExceptionHandler`.
