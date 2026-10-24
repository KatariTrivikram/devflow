# DevFlow Testing Strategy & Integration Test Guide

## 1. Overview

DevFlow employs a multi-tiered, production-grade testing strategy designed to verify functionality at every layer of the architecture while maintaining fast feedback loops and test reliability:

| Test Tier | Technology | Purpose | Execution Speed |
| :--- | :--- | :--- | :--- |
| **Unit Tests** | JUnit 5 + Mockito | Test individual business service methods and utilities in isolation with zero external dependencies. | Sub-second |
| **Web MVC Tests** | `@WebMvcTest` + MockMvc | Verify REST endpoints, HTTP status codes, request validation, serialization, and deserialization. | Fast (in-memory) |
| **Security Tests** | Spring Security Test + MockMvc | Verify JWT authentication, role-based authorization (RBAC), and IDOR prevention rules. | Fast (in-memory) |
| **Database Integration Tests** | Testcontainers + PostgreSQL | Spin up a real disposable PostgreSQL Docker container to verify Flyway migrations, JPA mappings, database constraints, and dynamic queries. | Real database verification |

---

## 2. Testcontainers PostgreSQL Architecture

### Dedicated Test Profile & Isolation
- DevFlow never touches production or cloud databases (e.g., Supabase) during automated tests.
- All database integration tests run against an isolated **`test`** profile defined in [`application-test.properties`](file:///d:/projects/Dev_Flow/devflow/backend/src/test/resources/application-test.properties).
- Integration test classes inherit from [`AbstractPostgresIntegrationTest`](file:///d:/projects/Dev_Flow/devflow/backend/src/test/java/com/devflow/integration/AbstractPostgresIntegrationTest.java).

### Singleton Container Pattern
To optimize test runtimes and resource consumption, DevFlow utilizes a static singleton container:
- A single Docker container running **`postgres:15-alpine`** is initialized once per JVM test execution.
- `@DynamicPropertySource` dynamically injects the ephemeral container's JDBC URL, username, and password into Spring's `Environment`.
- Automatic container cleanup on JVM termination is handled by Ryuk.

### Dynamic Configuration
```properties
# application-test.properties
spring.application.name=devflow-test
spring.jpa.hibernate.ddl-auto=validate
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration
```

### Docker Engine Compatibility
For compatibility with modern Docker Desktop (Engine 25+ / API 1.44+), [`src/test/resources/docker-java.properties`](file:///d:/projects/Dev_Flow/devflow/backend/src/test/resources/docker-java.properties) specifies:
```properties
api.version=1.44
```

---

## 3. Flyway & JPA Schema Validation

On container startup, Spring Boot and Flyway coordinate as follows:
1. **Empty PostgreSQL Database:** The Testcontainer starts with a clean `devflow_test` database.
2. **Flyway Migration Execution:** Flyway automatically applies `V1__initial_schema.sql`, creating all 7 domain tables (`users`, `projects`, `project_members`, `sprints`, `issues`, `comments`, `activities`), indexes, and constraints.
3. **Hibernate DDL Validation:** With `spring.jpa.hibernate.ddl-auto=validate`, Hibernate verifies that every JPA entity mapping (`@Entity`, `@Table`, `@Column`, `@JoinColumn`, `@Check`) matches the PostgreSQL schema exactly. If there is any discrepancy, application context initialization fails immediately.

---

## 4. Test Isolation via `@Transactional` Rollback

Every integration test class inheriting from `AbstractPostgresIntegrationTest` is annotated with `@Transactional`.
- Each test method runs inside its own database transaction.
- When the test completes (whether passing or failing), Spring's test framework automatically rolls back the transaction.
- Tests that verify database constraint violations invoke `saveAndFlush()` to trigger immediate SQL execution against PostgreSQL, catching `DataIntegrityViolationException` while maintaining isolation.

---

## 5. Integration Test Suite Breakdown

### 5.1. `FlywaySchemaIntegrationTest`
- **Location:** [`src/test/java/com/devflow/integration/FlywaySchemaIntegrationTest.java`](file:///d:/projects/Dev_Flow/devflow/backend/src/test/java/com/devflow/integration/FlywaySchemaIntegrationTest.java)
- **Verifications:**
  - Injects `Flyway` and verifies that migration version `1` ("initial schema") is applied and in `SUCCESS` state.
  - Queries `information_schema.tables` using `JdbcTemplate` to confirm that all 7 application tables plus `flyway_schema_history` exist in the `public` schema.

### 5.2. `UserRepositoryIntegrationTest`
- **Location:** [`src/test/java/com/devflow/integration/UserRepositoryIntegrationTest.java`](file:///d:/projects/Dev_Flow/devflow/backend/src/test/java/com/devflow/integration/UserRepositoryIntegrationTest.java)
- **Verifications:**
  - Persisting a user with generated UUID, verifying `@CreationTimestamp` and `@UpdateTimestamp` population.
  - Retrieving users by email and testing `existsByEmail()`.
  - Enforcing PostgreSQL unique constraint `idx_user_email`: attempting to save two users with identical emails raises `DataIntegrityViolationException`.

### 5.3. `ProjectRepositoryIntegrationTest`
- **Location:** [`src/test/java/com/devflow/integration/ProjectRepositoryIntegrationTest.java`](file:///d:/projects/Dev_Flow/devflow/backend/src/test/java/com/devflow/integration/ProjectRepositoryIntegrationTest.java)
- **Verifications:**
  - Project CRUD and foreign-key mapping to the creator `User`.
  - Querying projects by creator ID, status, and case-insensitive name filtering.
  - Enforcing unique constraint `uk_project_user` on `project_members`: preventing duplicate memberships for the same user in a project.
  - Enforcing PostgreSQL table CHECK constraint `sprints_end_date_check`: attempting to save a sprint where `end_date < start_date` triggers `DataIntegrityViolationException`.
  - Successfully persisting valid sprints where `end_date >= start_date`.

### 5.4. `IssueRepositoryIntegrationTest`
- **Location:** [`src/test/java/com/devflow/integration/IssueRepositoryIntegrationTest.java`](file:///d:/projects/Dev_Flow/devflow/backend/src/test/java/com/devflow/integration/IssueRepositoryIntegrationTest.java)
- **Verifications:**
  - Issue persistence across enums (`IssueType`, `IssuePriority`, `IssueStatus`) and foreign keys (`project_id`, `sprint_id`, `assigned_to_id`).
  - Pagination via `Pageable` across project, sprint, assignee, status, and priority queries.
  - Comment persistence and strict chronological ordering (`findByIssueIdOrderByCreatedAtAsc`).
  - Activity audit log persistence and strict reverse-chronological ordering (`findByIssueIdOrderByCreatedAtDesc`).

### 5.5. `IssueSpecificationIntegrationTest`
- **Location:** [`src/test/java/com/devflow/integration/IssueSpecificationIntegrationTest.java`](file:///d:/projects/Dev_Flow/devflow/backend/src/test/java/com/devflow/integration/IssueSpecificationIntegrationTest.java)
- **Verifications:**
  - Dynamic criteria filtering via Spring Data JPA `Specification<Issue>` on real PostgreSQL.
  - Filter combinations (`projectId` + `status` + `priority`).
  - Complex multi-field combinations with pagination (`projectId` + `sprintId` + `assignedToId` + `type`).
  - Case-insensitive search across issue `title` and `description` using PostgreSQL `LOWER(title) LIKE %...%`.
  - Role-Based Access Control (RBAC) criteria subquery:
    - `ADMIN`: unrestricted access across all projects.
    - `PROJECT_MANAGER`: access to projects created by the PM or where the PM is a member.
    - `DEVELOPER`: restricted access strictly to projects where the developer holds an active membership.

### 5.6. `ActuatorIntegrationTest`
- **Location:** [`src/test/java/com/devflow/integration/ActuatorIntegrationTest.java`](file:///d:/projects/Dev_Flow/devflow/backend/src/test/java/com/devflow/integration/ActuatorIntegrationTest.java)
- **Verifications:**
  - `/actuator/health` returns status `UP` with PostgreSQL database connectivity component verified against real Testcontainer.
  - `/actuator/info` returns safe application metadata without leaking secrets or credentials.
  - Verifies that sensitive endpoints (`/actuator/env`, `/actuator/beans`, `/actuator/heapdump`) are not publicly exposed.
  - Verifies that `X-Request-ID` is returned in response headers.

---

## 6. Running the Tests

### Prerequisites
- Java 21 JDK
- Maven 3.9+
- Docker Desktop (or Docker daemon) running locally

### Commands

#### Run All Tests (Unit + Integration)
```bash
mvn clean test
```

#### Run Only Integration Tests (Testcontainers)
```bash
mvn test -Dtest=*IntegrationTest
```

#### Run a Specific Integration Test
```bash
mvn test -Dtest=FlywaySchemaIntegrationTest
mvn test -Dtest=UserRepositoryIntegrationTest
mvn test -Dtest=ProjectRepositoryIntegrationTest
mvn test -Dtest=IssueRepositoryIntegrationTest
mvn test -Dtest=IssueSpecificationIntegrationTest
mvn test -Dtest=ActuatorIntegrationTest
```

#### Full Build and Packaging
```bash
mvn clean package
```
