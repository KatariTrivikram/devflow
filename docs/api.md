# DevFlow REST API Documentation

## 1. Overview & Response Format

The DevFlow REST API provides consistent, production-grade endpoints for managing software projects, sprints, issues, comments, and activity audit logs.

### 1.1 Paginated Response Format (`PageResponse<T>`)

All paginated endpoints return a standardized JSON structure:

```json
{
  "content": [ ... ],
  "page": 0,
  "size": 20,
  "totalElements": 125,
  "totalPages": 7,
  "first": true,
  "last": false
}
```

#### Fields:
- `content` (`Array<T>`): The page item elements.
- `page` (`integer`): Zero-based page index (default: `0`).
- `size` (`integer`): Size of the page (default: `20`, maximum: `100`).
- `totalElements` (`long`): Total number of elements across all pages.
- `totalPages` (`integer`): Total number of available pages.
- `first` (`boolean`): Indicates whether this is the first page.
- `last` (`boolean`): Indicates whether this is the last page.

---

### 1.2 CORS & Security Headers Configuration

DevFlow incorporates production-grade declarative Cross-Origin Resource Sharing (CORS) in `SecurityConfig`:
- **Allowed Origins**: Externalized via `cors.allowed-origins` property (or `CORS_ALLOWED_ORIGINS` environment variable). Defaults: `http://localhost:5173,http://localhost:3000`.
- **Allowed Methods**: `GET`, `POST`, `PUT`, `DELETE`, `PATCH`, `OPTIONS`.
- **Allowed Headers**: Standard and custom headers (`Authorization`, `Content-Type`, `X-Request-ID`, `Accept`, `Origin`, etc.).
- **Exposed Headers**: `X-Request-ID` is explicitly exposed to browser clients for distributed request tracing and support correlation.
- **Credentials Support**: `Access-Control-Allow-Credentials: true` is enabled for authenticated session exchanges.
- **Preflight Cache**: `3600` seconds (1 hour) `maxAge` to minimize OPTIONS roundtrips.

---

## 2. Pagination & Sorting Principles

### 2.1 Safe Pagination Rules
- **Default Page**: `0`
- **Default Page Size**: `20`
- **Maximum Page Size**: `100`
- **Validation**:
  - `page >= 0`: Requesting a negative page returns `HTTP 400 Bad Request`.
  - `1 <= size <= 100`: Requesting `size <= 0` or `size > 100` returns `HTTP 400 Bad Request`.

### 2.2 Safe Sorting Rules
- Sort format: `sort={field},{direction}` (e.g., `createdAt,desc` or `title,asc`).
- If no direction is provided (e.g., `sort=createdAt`), direction defaults to `asc`.
- Direction is case-insensitive (`asc`, `desc`, `ASC`, `DESC`).
- **Strict Allowlists**: Unknown sort fields are rejected with `HTTP 400 Bad Request` to prevent SQL/JPQL column exposure.

#### Allowed Sort Properties by Resource:
- **Issues**: `createdAt`, `updatedAt`, `title`, `priority`, `status`, `type` (Default: `createdAt,desc`)
- **Projects**: `createdAt`, `updatedAt`, `name`, `status` (Default: `createdAt,desc`)
- **Sprints**: `startDate`, `endDate`, `name`, `status`, `createdAt`, `updatedAt` (Default: `startDate,asc`)
- **Comments**: `createdAt`, `updatedAt` (Default: `createdAt,asc`)
- **Activities**: `createdAt`, `action` (Default: `createdAt,desc`)
- **Notifications**: `createdAt`, `title`, `type`, `isRead` (Default: `createdAt,desc`)
- **Search**: `updatedAt`, `createdAt`, `title`, `name` (Default: `updatedAt,desc`)

### 2.3 Invalid Parameter Handling
- **Invalid enum values** (e.g., `status=INVALID`, `priority=URGENT`): Handled by `GlobalExceptionHandler` returning `HTTP 400 Bad Request` with standard `ErrorResponse`:
  ```json
  {
    "status": 400,
    "message": "Invalid value 'INVALID' for parameter 'status'",
    "path": "/api/issues"
  }
  ```
- **Invalid sort field or direction**: Returns `HTTP 400 Bad Request` with an explanatory message.
- **Unauthorized project or resource**: Returns `HTTP 403 Forbidden` with `"Access denied"`.

---

## 3. Query Endpoints & Filtering

### 3.1 Issues (`GET /api/issues`)

Retrieves issues with dynamic database-level query composition (`JpaSpecificationExecutor`) and database-level authorization scoping.

#### Query Parameters:
| Parameter | Type | Default | Description |
|---|---|---|---|
| `projectId` | UUID | Optional | Filter by project UUID. Verified against user authorization. |
| `sprintId` | UUID | Optional | Filter by sprint UUID. |
| `noSprint` | Boolean | `false` | When `true`, filters issues not assigned to any sprint (`sprint IS NULL`). Useful for backlog views and quick filtering. |
| `assignedToId` | UUID | Optional | Filter by assigned user UUID. |
| `status` | Enum (`TODO`, `IN_PROGRESS`, `CODE_REVIEW`, `DONE`) | Optional | Filter by issue status. |
| `priority` | Enum (`LOW`, `MEDIUM`, `HIGH`, `CRITICAL`) | Optional | Filter by issue priority. |
| `type` | Enum (`BUG`, `FEATURE`, `TASK`, `IMPROVEMENT`) | Optional | Filter by issue type. |
| `search` | String | Optional | Case-insensitive substring search matching `title` OR `description` (`ILIKE %keyword%`). |
| `page` | integer | `0` | Zero-based page index (`>= 0`). |
| `size` | integer | `20` | Page size (`1` to `100`). |
| `sort` | string | `createdAt,desc` | Sort field and direction. |

#### Authorization Scoping:
- If `projectId` is provided, authorization check `requireProjectAccess(projectId)` is enforced; unauthorized users receive `HTTP 403 Forbidden`.
- If `projectId` is not provided:
  - `ADMIN`: Sees issues across all projects.
  - `PROJECT_MANAGER`: Scoped in database SQL to issues in projects they created OR are members of.
  - `DEVELOPER`: Scoped in database SQL to issues in projects they are members of.

#### Representative Examples:
```http
GET /api/issues?projectId=a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11&status=IN_PROGRESS&priority=HIGH&page=0&size=10&sort=createdAt,desc
Authorization: Bearer <token>
```
```http
GET /api/issues?projectId=a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11&search=authentication&page=0&size=20&sort=priority,desc
Authorization: Bearer <token>
```

---

### 3.2 Projects (`GET /api/projects`)

Retrieves projects accessible to the authenticated user with status filtering and name search.

#### Query Parameters:
| Parameter | Type | Default | Description |
|---|---|---|---|
| `status` | Enum (`PLANNING`, `ACTIVE`, `ON_HOLD`, `COMPLETED`, `ARCHIVED`) | Optional | Filter by project status. |
| `search` | String | Optional | Case-insensitive search on project `name` (`ILIKE %keyword%`). |
| `page` | integer | `0` | Zero-based page index (`>= 0`). |
| `size` | integer | `20` | Page size (`1` to `100`). |
| `sort` | string | `createdAt,desc` | Allowed: `createdAt`, `updatedAt`, `name`, `status`. |

#### Representative Example:
```http
GET /api/projects?status=ACTIVE&search=DevFlow&page=0&size=10&sort=name,asc
Authorization: Bearer <token>
```

---

### 3.3 Sprints (`GET /api/projects/{projectId}/sprints`)

Retrieves paginated sprints belonging to a specific project.

#### Query Parameters:
| Parameter | Type | Default | Description |
|---|---|---|---|
| `status` | Enum (`PLANNED`, `ACTIVE`, `COMPLETED`, `CANCELLED`) | Optional | Filter sprints by status. |
| `page` | integer | `0` | Zero-based page index (`>= 0`). |
| `size` | integer | `20` | Page size (`1` to `100`). |
| `sort` | string | `startDate,asc` | Allowed: `startDate`, `endDate`, `name`, `status`, `createdAt`, `updatedAt`. |

#### Representative Example:
```http
GET /api/projects/a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11/sprints?status=ACTIVE&page=0&size=10&sort=startDate,asc
Authorization: Bearer <token>
```

---

### 3.4 Comments (`GET /api/issues/{issueId}/comments`)

Retrieves paginated comments for an issue in chronological order by default.

#### Query Parameters:
| Parameter | Type | Default | Description |
|---|---|---|---|
| `page` | integer | `0` | Zero-based page index (`>= 0`). |
| `size` | integer | `20` | Page size (`1` to `100`). |
| `sort` | string | `createdAt,asc` | Allowed: `createdAt`, `updatedAt`. |

#### Representative Example:
```http
GET /api/issues/b1ffcd88-8b1c-4ef8-bb6d-6bb9bd380a22/comments?page=0&size=20&sort=createdAt,asc
Authorization: Bearer <token>
```

---

### 3.5 Activities (`GET /api/issues/{issueId}/activities`)

Retrieves paginated audit activities for an issue, showing newest activity first by default.

#### Query Parameters:
| Parameter | Type | Default | Description |
|---|---|---|---|
| `page` | integer | `0` | Zero-based page index (`>= 0`). |
| `size` | integer | `20` | Page size (`1` to `100`). |
| `sort` | string | `createdAt,desc` | Allowed: `createdAt`, `action`. |

#### Representative Example:
```http
GET /api/issues/b1ffcd88-8b1c-4ef8-bb6d-6bb9bd380a22/activities?page=0&size=20&sort=createdAt,desc
Authorization: Bearer <token>
```

---

## 4. Performance & Query Execution Architecture

- **Database-level SQL Filtering**: Dynamic queries are compiled into native PostgreSQL queries via Spring Data JPA `JpaSpecificationExecutor` and Criteria API predicates.
- **Subquery Scoping**: Authorization scoping uses SQL subqueries against `project_members` and `projects.created_by_id`, avoiding in-memory stream filtering.
- **Indexes**:
  - `issues`: `idx_issue_project_id`, `idx_issue_sprint_id`, `idx_issue_assigned_to`, `idx_issue_status`, `idx_issue_priority`.
  - `project_members`: `idx_project_member_project_id`, `idx_project_member_user_id`.
  - Note: Case-insensitive search on `title`/`description`/`name` uses standard PostgreSQL ILIKE matching. If dataset scales to hundreds of thousands of issues, a `pg_trgm` gin index on `title` and `description` can be introduced in a future migration.

---

## 5. OpenAPI 3 Specification & Interactive Swagger UI

DevFlow provides interactive API contract documentation powered by Springdoc OpenAPI 3.

### 5.1 Endpoints & URLs (Development)
- **Interactive Swagger UI**: [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)
- **Swagger UI Shortcut**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html) (redirects to `/swagger-ui/index.html`)
- **OpenAPI 3 JSON Specification**: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

### 5.2 Authentication & Authorization Workflow in Swagger UI
All domain endpoints (`/api/**`) are secured with JWT Bearer authentication. To test endpoints interactively in Swagger UI:

1. **Obtain Access Token**:
   - Scroll to the **Authentication** tag in Swagger UI.
   - Execute `POST /api/auth/register` (for new accounts) or `POST /api/auth/login`:
     ```json
     {
       "email": "developer@example.com",
       "password": "Password123!"
     }
     ```
   - Copy the `accessToken` string from the JSON response.

2. **Authorize Swagger UI**:
   - Scroll to the top right of the Swagger UI page and click the green **Authorize** button.
   - In the `bearerAuth (http, Bearer)` dialog, paste the access token in the **Value** field:
     ```
     Bearer eyJhbGciOiJIUzI1NiJ9...
     ```
     *(Note: You may enter just the raw token or `Bearer <token>`)*
   - Click **Authorize**, then click **Close**.

3. **Call Protected APIs**:
   - All subsequent requests executed via the **Try it out** button will automatically include the `Authorization: Bearer <token>` header.
   - Public authentication endpoints (`/api/auth/**`) are marked public and do not require the token.
   - If an expired or invalid token is supplied, `HTTP 401 Unauthorized` is returned.
   - If the user lacks permissions for an operation (e.g., non-admin calling admin endpoints, or accessing unassigned projects), `HTTP 403 Forbidden` is returned.

### 5.3 API Grouping Tags
- `Authentication`: User registration and JWT authentication endpoints.
- `Users`: User administration and retrieval endpoints (Admin-restricted creation/updates).
- `Projects`: Project lifecycle, searching, status filtering, and sprint retrieval.
- `Project Members`: Team membership management and role assignments.
- `Sprints`: Sprint lifecycle and milestone planning.
- `Issues`: Issue tracking, dynamic multi-field filtering, substring search, and sorting.
- `Comments`: Discussion and comment threads on issues.
- `Activities`: Audit trail and chronological change events on issues.
- `Dashboard`: Role-scoped aggregated project, issue, sprint, and assigned workload metrics, plus recent activities.

### 5.4 Error Responses
All endpoints adhere to the standardized `ErrorResponse` schema:
```json
{
  "timestamp": "2026-10-14T10:15:30Z",
  "status": 400,
  "message": "Validation failed",
  "path": "/api/issues",
  "errors": {
    "title": "Title is required",
    "priority": "must not be null"
  }
}
```

---

## 6. Backend API Quality & Production Hardening

DevFlow enforces rigorous production hardening guidelines across all REST endpoints:

### 6.1 Semantic REST HTTP Status Codes
| HTTP Status | Trigger / Scenario | Sample Endpoint |
|---|---|---|
| `201 Created` | Successful creation of a new entity | `POST /api/projects`, `POST /api/auth/register` |
| `200 OK` | Successful retrieval or entity update | `GET /api/projects/{id}`, `PUT /api/issues/{id}` |
| `204 No Content` | Successful deletion of a resource | `DELETE /api/issues/{id}`, `DELETE /api/users/{id}` |
| `400 Bad Request` | Validation failure, malformed JSON, invalid enums, invalid pagination/sorting | Malformed payload, `page < 0`, `size > 100` |
| `401 Unauthorized` | Missing, expired, invalid JWT or bad credentials | Missing `Authorization: Bearer <token>` |
| `403 Forbidden` | Authenticated user lacks role or team membership (IDOR protection) | Non-admin calling `POST /api/users` |
| `404 Not Found` | Resource identifier or route does not exist | `GET /api/projects/{nonExistentId}` |
| `405 Method Not Allowed` | Endpoint invoked with unsupported HTTP method | `PATCH /api/projects` |
| `409 Conflict` | Unique key violation, duplicate entity, DB constraint failure | Duplicate email, duplicate project key |
| `500 Internal Server Error` | Unexpected server failure (sanitized client response, MDC-logged server-side) | Unhandled database or system crash |

### 6.2 Centralized Exception Handling & Message Sanitization
The `GlobalExceptionHandler` intercepts all standard Spring MVC and database exceptions:
- **`HttpMessageNotReadableException`**: Returns `400 Bad Request` ("Malformed JSON request or invalid field format") without exposing internal parser class names.
- **`MissingServletRequestParameterException`**: Returns `400 Bad Request` with parameter name.
- **`HttpRequestMethodNotSupportedException`**: Returns `405 Method Not Allowed` ("HTTP method '{method}' is not supported for this endpoint").
- **`DataIntegrityViolationException`**: Returns `409 Conflict` ("Data integrity constraint violation occurred") without leaking SQL statements, table structures, or raw database connection strings.
- **`NoResourceFoundException`**: Returns `404 Not Found` for unmapped URL routes.
- **`ConstraintViolationException`**: Returns `400 Bad Request` with field error map.
- **`Exception` (fallback)**: Returns `500 Internal Server Error` with sanitized message ("An unexpected internal server error occurred") while logging full stack trace with `X-Request-ID` MDC context server-side.

### 6.3 Sensitive Field Protection & DTO Decoupling
- **No Entity Exposure**: All 8 controllers return strictly DTO responses (`ResponseEntity<...Response>`); JPA entities are never exposed across HTTP boundaries.
- **Zero Sensitive Data Leakage**: Passwords and password hashes are never included in `UserResponse`, `AuthResponse`, error payloads, or log outputs.
- **IDOR Protection**: Creator and author IDs are derived strictly from `SecurityContext` (`securityUtils.getCurrentUserId()`), safely ignoring any client-supplied IDs.

### 6.4 Transaction Boundaries
- **Service Layer Ownership**: All transaction boundaries (`@Transactional`) are encapsulated at the service layer rather than controllers or repositories.
- **Read-Only Optimization**: Read methods are annotated with `@Transactional(readOnly = true)` to avoid Hibernate dirty-checking overhead and enable database read-replica routing.

---

## 7. Dashboard & Project Analytics API

DevFlow exposes a dedicated dashboard analytics endpoint aggregating KPIs and recent activity audit events scoped strictly by the authenticated user's role and project memberships.

### 7.1 Summary Endpoint: `GET /api/dashboard/summary`

- **URL**: `/api/dashboard/summary`
- **Method**: `GET`
- **Authentication**: Required (`Bearer <JWT>`)
- **Authorization**: All authenticated platform roles (`ADMIN`, `PROJECT_MANAGER`, `DEVELOPER`)

#### Role-Based Scoping Model
- **`ADMIN`**: Platform-wide metrics across all projects, sprints, and issues.
- **`PROJECT_MANAGER`**: Scoped to projects created/managed by the PM or where the PM is an active project member.
- **`DEVELOPER`**: Strictly scoped to projects where the developer is an active project member.
- **Empty Scope Handling**: If a non-admin user belongs to zero accessible projects, the service immediately returns zero-valued summary DTOs without executing invalid SQL `IN ()` queries.

#### Response: `200 OK`
```json
{
  "userRole": "PROJECT_MANAGER",
  "projectSummary": {
    "total": 5,
    "active": 3,
    "completed": 1,
    "archived": 1
  },
  "issueSummary": {
    "total": 14,
    "todo": 5,
    "inProgress": 3,
    "codeReview": 2,
    "done": 4,
    "lowPriority": 2,
    "mediumPriority": 4,
    "highPriority": 6,
    "criticalPriority": 2
  },
  "assignedIssueSummary": {
    "total": 3,
    "todo": 1,
    "inProgress": 2,
    "codeReview": 0,
    "done": 0
  },
  "sprintSummary": {
    "total": 6,
    "planned": 1,
    "active": 2,
    "completed": 3
  },
  "recentActivity": [
    {
      "id": "7b58c894-3e61-4177-8c38-89c0a6b7d1e8",
      "issueId": "e264624b-325d-4f74-9c8a-78b1d9bf1b7a",
      "issueTitle": "Fix authentication token refresh race condition",
      "userId": "1cf628ab-bce1-46f9-bf55-0ccb99ef7f22",
      "userName": "Jane Developer",
      "userEmail": "jane@devflow.io",
      "action": "STATUS_CHANGED",
      "oldValue": "IN_PROGRESS",
      "newValue": "CODE_REVIEW",
      "createdAt": "2026-10-15T10:14:22Z"
    }
  ]
}
```

---

## 8. Notifications API

The Notifications API provides persistent, authenticated notification delivery for issue assignments, status/priority changes, comments, and project membership updates.

### 8.1 Data Model & Types

#### Notification Types (`NotificationType`):
- `ISSUE_ASSIGNED`: Triggered when an issue is assigned or reassigned to a user.
- `ISSUE_STATUS_CHANGED`: Triggered when an issue status transitions (e.g. `TODO` -> `IN_PROGRESS`).
- `ISSUE_PRIORITY_CHANGED`: Triggered when an issue priority changes.
- `COMMENT_ADDED`: Triggered when a comment is added to an issue (notifies assignee).
- `PROJECT_MEMBER_ADDED`: Triggered when a user is added to a project.
- `SPRINT_STATUS_CHANGED`: Triggered when a sprint status changes (notifies project team).

#### Reference Types (`NotificationReferenceType`):
- `ISSUE`
- `PROJECT`
- `SPRINT`

### 8.2 Endpoints

#### 8.2.1 Get Authenticated User Notifications (`GET /api/notifications`)
Retrieves paginated notifications for the authenticated user, ordered newest first by default.

**Query Parameters:**
| Parameter | Type | Default | Description |
|---|---|---|---|
| `unreadOnly` | boolean | `false` | When `true`, filters only unread notifications. |
| `page` | integer | `0` | Zero-based page index. |
| `size` | integer | `20` | Page size (`1` to `100`). |
| `sort` | string | `createdAt,desc` | Sort field and direction (`createdAt`, `title`, `type`, `isRead`). |

**Response (HTTP 200 OK):**
```json
{
  "content": [
    {
      "id": "7b58c894-3e61-4177-8c38-89c0a6b7d1e8",
      "type": "ISSUE_ASSIGNED",
      "title": "You were assigned an issue",
      "message": "You were assigned issue \"Fix memory leak\" in Project Phoenix",
      "referenceType": "ISSUE",
      "referenceId": "e264624b-325d-4f74-9c8a-78b1d9bf1b7a",
      "createdAt": "2026-10-15T10:14:22Z",
      "readAt": null,
      "actorId": "1cf628ab-bce1-46f9-bf55-0ccb99ef7f22",
      "actorName": "Jane Developer",
      "read": false
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1,
  "first": true,
  "last": true
}
```

#### 8.2.2 Get Unread Notifications Count (`GET /api/notifications/unread-count`)
Returns the number of unread notifications for the authenticated user.

**Response (HTTP 200 OK):**
```json
{
  "count": 3
}
```

#### 8.2.3 Mark Single Notification Read (`PATCH /api/notifications/{id}/read`)
Marks a single notification as read.
Enforces strict Insecure Direct Object Reference (IDOR) protection: if the notification belongs to another user, `HTTP 403 Forbidden` is returned.

**Response (HTTP 200 OK):**
```json
{
  "id": "7b58c894-3e61-4177-8c38-89c0a6b7d1e8",
  "type": "ISSUE_ASSIGNED",
  "title": "You were assigned an issue",
  "message": "You were assigned issue \"Fix memory leak\" in Project Phoenix",
  "referenceType": "ISSUE",
  "referenceId": "e264624b-325d-4f74-9c8a-78b1d9bf1b7a",
  "createdAt": "2026-10-15T10:14:22Z",
  "readAt": "2026-10-15T10:20:00Z",
  "actorId": "1cf628ab-bce1-46f9-bf55-0ccb99ef7f22",
  "actorName": "Jane Developer",
  "read": true
}
```

#### 8.2.4 Mark All Notifications Read (`PATCH /api/notifications/read-all`)
Bulk marks all unread notifications belonging to the authenticated user as read in a single optimized query.

**Response (HTTP 200 OK):**
```json
{
  "message": "All notifications marked as read",
  "updatedCount": 3
}
```

---

## 9. Global Search API

### 9.1 Overview & Server-Side Security Scoping

The Global Search API allows authenticated users to search across projects, issues, and sprints.

#### Server-Side Security Scoping:
- **`ADMIN`**: Searches across all projects, issues, and sprints in the entire application.
- **`PROJECT_MANAGER`**: Searches projects created by the user or projects where the user is an active member, along with their associated issues and sprints.
- **`DEVELOPER`**: Searches only projects where the user is an active member, along with their associated issues and sprints.
- **Unauthorized Data Isolation**: Direct queries matching IDs or titles of unauthorized records yield zero matches; unauthorized records are never leaked.

---

### 9.2 Search Endpoints

#### 9.2.1 Global Search (`GET /api/search`)

Searches DevFlow resources across projects, issues, and sprints based on query keywords and optional resource type filter.

**Query Parameters:**
- `q` (`string`, required): Search keyword (minimum 1 character; whitespace-trimmed). Blank or missing query returns `HTTP 400 Bad Request`.
- `type` (`string`, optional, default `ALL`): Resource type filter. Allowed values:
  - `ALL`: Aggregates and paginates across Projects, Issues, and Sprints.
  - `PROJECT`: Filters and paginates solely Project matches (name, description).
  - `ISSUE`: Filters and paginates solely Issue matches (title, description).
  - `SPRINT`: Filters and paginates solely Sprint matches (name, goal).
- `page` (`integer`, optional, default `0`): Zero-based page index (`page >= 0`).
- `size` (`integer`, optional, default `20`): Page size limit (`1 <= size <= 100`).
- `sort` (`string`, optional, default `updatedAt,desc`): Safe sort field and direction. Allowlisted fields: `updatedAt`, `createdAt`, `title`, `name`.

**Request Example:**
```http
GET /api/search?q=authentication&type=ALL&page=0&size=20&sort=updatedAt,desc
Authorization: Bearer <jwt-token>
```

**Response (HTTP 200 OK):**
```json
{
  "content": [
    {
      "id": "e264624b-325d-4f74-9c8a-78b1d9bf1b7a",
      "title": "Fix token expiration in authentication flow",
      "description": "JWT tokens expire prematurely on refresh cycles",
      "type": "ISSUE",
      "projectId": "1c7263b5-3168-45fa-9486-2a74c2d667c8",
      "projectName": "DevFlow Core Platform",
      "status": "IN_PROGRESS",
      "priority": "HIGH",
      "createdAt": "2026-10-14T08:30:00Z",
      "updatedAt": "2026-10-15T09:12:00Z"
    },
    {
      "id": "8f3102d9-1234-4bca-9012-3456789abcde",
      "title": "Sprint 4 - Authentication Hardening",
      "description": "Improve session management and OAuth integrations",
      "type": "SPRINT",
      "projectId": "1c7263b5-3168-45fa-9486-2a74c2d667c8",
      "projectName": "DevFlow Core Platform",
      "status": "ACTIVE",
      "priority": null,
      "createdAt": "2026-10-12T10:00:00Z",
      "updatedAt": "2026-10-15T08:00:00Z"
    },
    {
      "id": "1c7263b5-3168-45fa-9486-2a74c2d667c8",
      "title": "DevFlow Core Platform",
      "description": "Core authentication and project management engine",
      "type": "PROJECT",
      "projectId": "1c7263b5-3168-45fa-9486-2a74c2d667c8",
      "projectName": "DevFlow Core Platform",
      "status": "ACTIVE",
      "priority": null,
      "createdAt": "2026-10-10T12:00:00Z",
      "updatedAt": "2026-10-14T16:00:00Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 3,
  "totalPages": 1,
  "first": true,
  "last": true,
  "query": "authentication",
  "type": "ALL"
}
```

**Error Responses:**
- `HTTP 400 Bad Request` (Missing/blank query parameter):
  ```json
  {
    "status": 400,
    "error": "Bad Request",
    "message": "Search query parameter 'q' is required and cannot be blank",
    "timestamp": "2026-10-15T19:25:00Z"
  }
  ```
- `HTTP 400 Bad Request` (Invalid sort property or page):
  ```json
  {
    "status": 400,
    "error": "Bad Request",
    "message": "Invalid sort property 'password'. Allowed fields: [updatedAt, createdAt, title, name]",
    "timestamp": "2026-10-15T19:25:00Z"
  }
  ```
- `HTTP 401 Unauthorized` (Missing or invalid JWT token):
  ```json
  {
    "status": 401,
    "error": "Unauthorized",
    "message": "Full authentication is required to access this resource",
    "timestamp": "2026-10-15T19:25:00Z"
  }
  ```

---

## 10. Project & Issue Workflow Refinement Endpoints

DevFlow provides dedicated summary and project-scoped audit endpoints to support integrated project workspaces and strict workflow lifecycle integrity.

### 10.1 Project Summary Metrics (`GET /api/projects/{id}/summary`)

Retrieves real aggregated workflow metrics, issue status counts, member counts, and active sprint progress for a single project in a single request.

- **URL**: `/api/projects/{id}/summary`
- **Method**: `GET`
- **Path Variable**: `id` (`UUID`): Project UUID.
- **Authentication**: Required (`Bearer <JWT>`)
- **Authorization**: Accessible to `ADMIN`, project creator (`PROJECT_MANAGER`), and active members of the project (`DEVELOPER`, `PROJECT_MANAGER`). Unauthorized requests return `HTTP 403 Forbidden`.

#### Response Example (`200 OK`):
```json
{
  "projectId": "1c7263b5-3168-45fa-9486-2a74c2d667c8",
  "totalIssues": 24,
  "openIssues": 18,
  "todoIssues": 8,
  "inProgressIssues": 7,
  "codeReviewIssues": 3,
  "doneIssues": 6,
  "totalMembers": 5,
  "activeSprint": {
    "id": "8f3102d9-1234-4bca-9012-3456789abcde",
    "name": "Sprint 3 - Core Refinement",
    "status": "ACTIVE",
    "startDate": "2026-10-10",
    "endDate": "2026-10-24",
    "totalIssues": 12,
    "completedIssues": 5
  }
}
```

*Note: If the project has no currently `ACTIVE` sprint, `activeSprint` is `null`.*

---

### 10.2 Project Recent Activities (`GET /api/activities/project/{projectId}`)

Retrieves chronological issue audit activities across all issues within a specific project.

- **URL**: `/api/activities/project/{projectId}`
- **Method**: `GET`
- **Path Variable**: `projectId` (`UUID`): Project UUID.
- **Query Parameters**:
  - `limit` (`integer`, optional, default `10`): Maximum number of recent activity records to return (capped at `50`).
- **Authentication**: Required (`Bearer <JWT>`)
- **Authorization**: User must have access to the target project (`ADMIN`, project creator, or project member).

#### Response Example (`200 OK`):
```json
[
  {
    "id": "7b58c894-3e61-4177-8c38-89c0a6b7d1e8",
    "issueId": "e264624b-325d-4f74-9c8a-78b1d9bf1b7a",
    "issueTitle": "Fix token expiration race condition",
    "userId": "1cf628ab-bce1-46f9-bf55-0ccb99ef7f22",
    "userName": "Jane Developer",
    "userEmail": "jane@devflow.io",
    "action": "STATUS_CHANGED",
    "oldValue": "IN_PROGRESS",
    "newValue": "CODE_REVIEW",
    "createdAt": "2026-10-15T10:14:22Z"
  }
]
```

---

### 10.3 Project Membership Integrity Business Rule

DevFlow enforces referential workflow integrity when managing project team members:

- **Endpoint**: `DELETE /api/projects/{projectId}/members/{userId}`
- **Business Rule**: A project member **cannot be removed** from a project if they currently have issues assigned to them in that project.
- **Behavior**: The server queries `IssueRepository.countByProjectIdAndAssignedToId(projectId, userId)`. If assigned issue count > 0, the operation is blocked and throws `BusinessRuleException`.
- **Response**: `HTTP 400 Bad Request`
  ```json
  {
    "status": 400,
    "error": "Bad Request",
    "message": "Cannot remove member: User still has 3 assigned issue(s) in this project. Please reassign or unassign these issues first.",
    "path": "/api/projects/1c7263b5-3168-45fa-9486-2a74c2d667c8/members/1cf628ab-bce1-46f9-bf55-0ccb99ef7f22"
  }
  ```

---

## 11. Backend Production Hardening & Consistency Guarantees

### 11.1 Cross-Cutting Authorization & Scoped Queries
All high-level convenience query endpoints enforce project-level authorization scoping via JPA Specifications:
- `GET /api/issues/status/{status}`: Automatically scoped to the current user's accessible projects using `IssueSpecification.isAccessibleByUser(currentUserId, currentUserRole)`.
- `GET /api/issues/priority/{priority}`: Scoped in SQL to accessible projects.
- `GET /api/issues/assignee/{assignedToId}`: Enforces that non-admin callers may only inspect their own assigned issues, and results are restricted to projects in which they retain active membership.
- `GET /api/projects/status/{status}` & `GET /api/projects/search`: Scoped at the SQL database layer via `ProjectSpecification.isAccessibleByUser`, preventing in-memory loading of unauthorized projects.

### 11.2 N+1 Query Elimination
- `NotificationRepository`: Paginated reads (`findByRecipientIdOrderByCreatedAtDesc`, `findByRecipientIdAndIsReadOrderByCreatedAtDesc`) use `@EntityGraph(attributePaths = {"actor"})` to eagerly load actor details in a single SQL join.
- `ActivityRepository`: Paginated reads (`findByIssueId`, `findByIssueIdOrderByCreatedAtDesc`) use `@EntityGraph(attributePaths = {"user", "issue"})` to eagerly load actor and issue details, eliminating N+1 SELECT queries during audit mapping.

### 11.3 High-Frequency Performance Indexes (Flyway V3)
DevFlow migration `V3__add_performance_indexes.sql` establishes dedicated B-tree indexes for all frequent foreign keys and composite filter combinations:
- `projects(created_by_id)` and `projects(status)`
- `sprints(project_id)` and `sprints(project_id, status)`
- `issues(project_id, status)` and `issues(project_id, sprint_id)`
- `comments(issue_id, created_at)`
- `activities(issue_id, created_at DESC)` and `activities(created_at DESC)`

### 11.4 Sanitized Error Handling & Traceability
- `GlobalExceptionHandler` intercepts unchecked `IllegalArgumentException` returning `HTTP 400 Bad Request` with standardized `ErrorResponse`.
- MDC context binds a unique `requestId` to every incoming request via `RequestIdFilter`, ensuring structured, sanitized logs without exposing passwords, JWT signatures, or internal stack traces to clients.

