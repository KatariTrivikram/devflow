# DevFlow Security Architecture & Authentication Specification

## 1. Overview

DevFlow implements a stateless, token-based authentication and authorization mechanism using **Spring Security 6** (Spring Boot 3.3.4), **JSON Web Tokens (JJWT 0.12.6)**, and **BCrypt password hashing**.

All REST endpoints under `/api/**` are protected by default, requiring a valid JWT Bearer token, except for designated public authentication routes (`/api/auth/**`). In addition, Role-Based Access Control (RBAC) is enforced at the filter chain level for administrative operations.

---

## 2. Authentication & Authorization Flow

```
+--------------------------------------------------------------------------------------------------+
|                                    Client (Web Browser / API)                                    |
+--------------------------------------------------------------------------------------------------+
          |                                                                    |
    1. POST /api/auth/login                                          3. Request with Header:
    { "email", "password" }                                          Authorization: Bearer <jwt>
          |                                                                    |
          v                                                                    v
+-------------------------------+                            +-----------------------------------+
|        AuthController         |                            |      JwtAuthenticationFilter      |
+-------------------------------+                            +-----------------------------------+
          |                                                                    |
          v                                                                    | Extracts Bearer token,
+-------------------------------+                                              | extracts username (email),
|          AuthService          |                                              | validates signature & expiry
+-------------------------------+                                              v
          |                                                  +-----------------------------------+
          | AuthenticationManager.authenticate()             |    CustomUserDetailsService       |
          | (verifies BCrypt hash via DB)                    +-----------------------------------+
          |                                                                    |
          v                                                                    | Loads user from DB,
+-------------------------------+                                              | sets Authentication in
|          JwtService           |                                              | SecurityContextHolder
+-------------------------------+                                              v
          |                                                  +-----------------------------------+
          | Generates signed token with claims:              |       Authorization Filter        |
          | - sub (email)                                    |    (Checks hasRole('ADMIN'), etc) |
          | - userId (UUID)                                  +-----------------------------------+
          | - role (e.g., ADMIN, DEVELOPER)                   /                                 \
          v                                                  / (Pass)                            \ (Forbidden)
    2. Response: 200 OK                                     v                                     v
    {                                           +--------------------+                 +--------------------+
      "accessToken": "ey...",                   | Target Controller  |                 | CustomAccessDenied |
      "tokenType": "Bearer",                    |  (e.g., /api/users)|                 |      Handler       |
      "expiresIn": 3600000,                     +--------------------+                 |     (403 JSON)     |
      "user": { ... }                                                                  +--------------------+
    }
```

---

## 3. Core Security Components

### 3.1. Password Hashing (BCrypt)
- **Algorithm**: `BCryptPasswordEncoder` (salted, multi-round hashing).
- Passwords are encrypted before persisting during registration or user creation:
  ```java
  user.setPassword(passwordEncoder.encode(request.getPassword()));
  ```
- Passwords are never stored in plain text and are omitted from all `UserResponse` DTO representations.

### 3.2. User Details (`CustomUserDetails` & `CustomUserDetailsService`)
- `CustomUserDetails` adapts the domain entity `com.devflow.entity.User` to Spring Security's `UserDetails` contract.
- Maps domain `UserRole` (`ADMIN`, `PROJECT_MANAGER`, `DEVELOPER`) to Spring Security `GrantedAuthority` with standard `ROLE_` prefix:
  ```java
  new SimpleGrantedAuthority("ROLE_" + user.getRole().name())
  ```
- `CustomUserDetailsService` queries `UserRepository.findByEmail(email)` to locate the principal for authentication and token validation.

### 3.3. JSON Web Token Service (`JwtService`)
- Uses HMAC-SHA256 (`io.jsonwebtoken.security.Keys.hmacShaKeyFor`).
- Claims embedded in the JWT payload:
  - `sub`: User email address
  - `userId`: String representation of the user UUID
  - `role`: Domain role name (e.g., `DEVELOPER`, `ADMIN`)
  - `iat`: Timestamp of token creation
  - `exp`: Timestamp of token expiration
- Validates token signature, subject match against `UserDetails`, and ensures token has not expired.

### 3.4. Stateless Filter (`JwtAuthenticationFilter`)
- Extends Spring's `OncePerRequestFilter`.
- Intercepts incoming HTTP requests, inspecting the `Authorization` header for `Bearer <token>`.
- If a valid token is presented and no authentication is currently established in the current thread's `SecurityContext`, sets `UsernamePasswordAuthenticationToken` with loaded authorities into `SecurityContextHolder`.

### 3.5. Security Configuration (`SecurityConfig`)
- **Stateless Session**: Configured with `SessionCreationPolicy.STATELESS` (no HTTP session is created or stored on the server).
- **CSRF Protection**: Disabled since authentication relies exclusively on stateless Bearer tokens in headers.
- **Role-Based Access Control Rules**:
  - `POST /api/auth/**` &rarr; `permitAll()`
  - `POST /api/users/**` &rarr; `hasRole("ADMIN")`
  - `PUT /api/users/**` &rarr; `hasRole("ADMIN")`
  - `/api/**` &rarr; `authenticated()`
  - Any other request &rarr; `authenticated()`

---

## 4. Error Handling Contract

DevFlow handles security-related rejections cleanly using standardized JSON matching the platform's `ErrorResponse` schema:

```json
{
  "timestamp": "2026-10-13T22:37:20.738Z",
  "status": 401,
  "message": "Authentication required",
  "path": "/api/projects"
}
```

### 4.1. 401 Unauthorized (`CustomAuthenticationEntryPoint`)
- Triggered when an unauthenticated client attempts to access a protected endpoint, or when an invalid/expired token is supplied.
- Status: `401 Unauthorized`
- Body:
  ```json
  {
    "timestamp": "...",
    "status": 401,
    "message": "Authentication required",
    "path": "/api/projects/1"
  }
  ```

### 4.2. 403 Forbidden (`CustomAccessDeniedHandler`)
- Triggered when an authenticated client attempts an operation for which their assigned role lacks permission (e.g., a `DEVELOPER` calling `POST /api/users`).
- Status: `403 Forbidden`
- Body:
  ```json
  {
    "timestamp": "...",
    "status": 403,
    "message": "Access denied",
    "path": "/api/users"
  }
  ```

---

## 5. Environment Configuration

The security layer requires externalized environment variables to avoid hardcoded secrets:

| Variable | Description | Default / Example |
|---|---|---|
| `JWT_SECRET` | 256-bit (minimum 32-character) secret key for HMAC-SHA256 token signing | Set in production or `.env` |
| `JWT_EXPIRATION` | Token validity duration in milliseconds | `3600000` (1 hour) |

### Sample `.env` or application runtime configuration:
```properties
jwt.secret=${JWT_SECRET}
jwt.expiration=${JWT_EXPIRATION:3600000}
```

---

## 6. API Examples

### 6.1. Register a New User
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Jane Developer",
    "email": "jane@example.com",
    "password": "SecurePassword123!",
    "role": "DEVELOPER"
  }'
```

**Response (201 Created):**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 3600000,
  "user": {
    "id": "e7c4f4a2-19e0-4a8f-9a7c-40adfb88e1a5",
    "name": "Jane Developer",
    "email": "jane@example.com",
    "role": "DEVELOPER",
    "createdAt": "2026-10-13T22:30:00Z",
    "updatedAt": "2026-10-13T22:30:00Z"
  }
}
```

### 6.2. Log In
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "jane@example.com",
    "password": "SecurePassword123!"
  }'
```

**Response (200 OK):**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 3600000,
  "user": {
    "id": "e7c4f4a2-19e0-4a8f-9a7c-40adfb88e1a5",
    "name": "Jane Developer",
    "email": "jane@example.com",
    "role": "DEVELOPER",
    "createdAt": "2026-10-13T22:30:00Z",
    "updatedAt": "2026-10-13T22:30:00Z"
  }
}
```

### 6.3. Access Protected Resource
```bash
curl -X GET http://localhost:8080/api/projects \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

### 6.4. Admin-Only Resource Access (RBAC)
```bash
# Permitted for ADMIN; Forbidden (403) for DEVELOPER or PROJECT_MANAGER
curl -X POST http://localhost:8080/api/users \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Alex Admin",
    "email": "alex@example.com",
    "password": "SecurePassword123!",
    "role": "ADMIN"
  }'
```

---

## 7. Business Authorization (Step 10)

### 7.1. Authentication vs Authorization
- **Authentication**: Establishes *who the user is* via signed JWT and loads their credentials into Spring Security's `SecurityContext`.
- **Authorization**: Determines *whether the user is permitted to perform a specific operation on a specific resource* based on their role, project membership, resource ownership, and domain rules.

### 7.2. Authenticated Actor Identity (Zero Trust for Client IDs)
Client payloads may include fields such as `userId`, `createdById`, or `assignedToId`. DevFlow enforces strict separation:
- **Client Request ID**: Treated solely as the target or related resource (e.g., target user being assigned to an issue or added to a project).
- **Acting User ID**: Always retrieved directly from Spring Security's `SecurityContext` via `SecurityUtils.getCurrentUserId()`.
- Client-supplied `createdById` or `userId` are **never** trusted as the actor for project creation, comment authoring, or audit log recording.

### 7.3. Authorization Architecture
```
Controller (Thin: standard HTTP mapping, validates DTOs)
   ↓
Service (Coordinates business logic, transactions)
   ↓
AuthorizationService & SecurityUtils (Enforces role, ownership, and membership checks)
   ↓
Repository (Optimized existence and ID lookups)
   ↓
Database
```

### 7.4. IDOR (Insecure Direct Object Reference) Prevention
Server-side authorization checks prevent clients from tampering with resource UUIDs:
- **Project Access**: Developers and non-managing PMs attempting `GET /api/projects/{foreign-id}` receive HTTP 403 Forbidden.
- **Issue Updates**: A developer attempting `PUT /api/issues/{unassigned-or-foreign-id}` receives HTTP 403 Forbidden.
- **Comment Modification**: A user attempting `PUT /api/comments/{foreign-id}` or `DELETE /api/comments/{foreign-id}` receives HTTP 403 Forbidden.

### 7.5. Assignment Validation
When an issue is created or updated with an assignee (`assignedToId`):
- DevFlow verifies that the target assignee belongs to the target project (`ProjectMemberRepository.existsByProjectIdAndUserId` or project creator).
- If the assigned user is not a member of the project, a `BusinessRuleException` is thrown, returning **HTTP 400 Bad Request**.

### 7.6. Authorization Matrix

| Resource Operation | ADMIN | PROJECT_MANAGER | DEVELOPER |
| :--- | :--- | :--- | :--- |
| **User Administration** (`POST /api/users`, `PUT /api/users/{id}`) | Full | Forbidden (403) | Forbidden (403) |
| **Project Creation** (`POST /api/projects`) | Allowed | Allowed (actor set to PM) | Forbidden (403) |
| **Project Update/Delete** (`PUT/DELETE /api/projects/{id}`) | Allowed | Allowed for managed projects | Forbidden (403) |
| **Project Viewing** (`GET /api/projects/{id}`) | Allowed | Allowed (if creator or member) | Allowed (if project member) |
| **Membership Management** (`POST/DELETE /api/project-members/**`) | Allowed | Allowed for managed projects | Forbidden (403) |
| **Membership Viewing** (`GET /api/project-members/project/{id}`) | Allowed | Allowed (if creator or member) | Allowed (if project member) |
| **Sprint Management** (`POST/PUT/DELETE /api/sprints/**`) | Allowed | Allowed for managed projects | Forbidden (403) |
| **Sprint Viewing** (`GET /api/sprints/**`) | Allowed | Allowed (if creator or member) | Allowed (if project member) |
| **Issue Creation** (`POST /api/issues`) | Allowed | Allowed for managed projects | Forbidden (403) |
| **Issue Assignment** (`assignedToId`) | Must be project member | Must be project member | Must be project member |
| **Issue Update** (`PUT /api/issues/{id}`) | Allowed | Allowed for managed projects | Allowed for assigned issues only |
| **Issue Deletion** (`DELETE /api/issues/{id}`) | Allowed | Allowed for managed projects | Forbidden (403) |
| **Comment Creation** (`POST /api/comments`) | Project access required | Project access required | Project access required (author=actor) |
| **Comment Update/Delete** (`PUT/DELETE /api/comments/{id}`) | Allowed | Allowed for managed projects | Allowed for own comments only |
| **Activity Audit View** (`GET /api/activities/**`) | Project access required | Project access required | Project access required |

