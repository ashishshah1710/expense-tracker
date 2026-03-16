# Expense Tracker - End-to-End Code Walkthrough

## Table of Contents

1. [Project Overview](#1-project-overview)
2. [Technology Stack](#2-technology-stack)
3. [Project Structure](#3-project-structure)
4. [Application Entry Point](#4-application-entry-point)
5. [Configuration](#5-configuration)
6. [Data Model Layer](#6-data-model-layer)
7. [Repository Layer (Data Access)](#7-repository-layer-data-access)
8. [DTO Layer (Data Transfer Objects)](#8-dto-layer-data-transfer-objects)
9. [Security Layer (JWT Authentication)](#9-security-layer-jwt-authentication)
10. [Service Layer (Business Logic)](#10-service-layer-business-logic)
11. [Controller Layer (REST Endpoints)](#11-controller-layer-rest-endpoints)
12. [Exception Handling](#12-exception-handling)
13. [API Documentation (Swagger/OpenAPI)](#13-api-documentation-swaggeropenapi)
14. [Integration Tests](#14-integration-tests)
15. [Docker Setup](#15-docker-setup)
16. [Request Lifecycle - End-to-End Flow](#16-request-lifecycle---end-to-end-flow)

---

## 1. Project Overview

This is a **REST API** for tracking personal expenses, built with Spring Boot. It provides:

- **User registration and login** with JWT-based authentication
- **CRUD operations** on expenses (Create, Read, Update, Delete)
- **Role-based access control** (USER vs ADMIN)
- **Pagination and filtering** on expense queries
- **Swagger UI** for API documentation and testing

There is no frontend — it is a pure backend API consumed via HTTP clients (Postman, curl, Swagger UI, etc.).

---

## 2. Technology Stack

| Layer           | Technology                                  |
|-----------------|---------------------------------------------|
| Language        | Java 17                                     |
| Framework       | Spring Boot 3.2.5                           |
| Web             | Spring Web (REST controllers)               |
| Security        | Spring Security + JWT (jjwt 0.12.5)         |
| Data Access     | Spring Data JPA + Hibernate                 |
| Validation      | Bean Validation (`jakarta.validation`)       |
| Database        | PostgreSQL 15                               |
| API Docs        | SpringDoc OpenAPI 2.3.0 (Swagger UI)        |
| Build Tool      | Maven                                       |
| Containerization| Docker + Docker Compose                     |
| Testing         | JUnit 5, Testcontainers, AssertJ            |
| Utilities       | Lombok (reduces boilerplate)                |

---

## 3. Project Structure

```
expense-tracker/
├── src/main/java/com/ashish/expensetracker/
│   ├── ExpenseTrackerApplication.java      # App entry point
│   ├── config/
│   │   └── OpenApiConfig.java              # Swagger/OpenAPI configuration
│   ├── controller/
│   │   ├── AuthController.java             # Registration & login endpoints
│   │   └── ExpenseController.java          # Expense CRUD endpoints
│   ├── dto/
│   │   ├── RegisterRequest.java            # Registration input
│   │   ├── LoginRequest.java               # Login input
│   │   ├── AuthResponse.java               # Auth output (token + user info)
│   │   ├── ExpenseRequest.java             # Expense input (create/update)
│   │   ├── ExpenseResponse.java            # Expense output
│   │   └── PagedResponse.java              # Generic paginated response wrapper
│   ├── exception/
│   │   ├── ErrorResponse.java              # Standard error JSON structure
│   │   ├── GlobalExceptionHandler.java     # Centralized error handling
│   │   ├── ResourceNotFoundException.java  # 404 exception
│   │   └── UnauthorizedException.java      # 403 exception
│   ├── model/
│   │   ├── User.java                       # User JPA entity
│   │   ├── Expense.java                    # Expense JPA entity
│   │   ├── Role.java                       # Enum: ROLE_USER, ROLE_ADMIN
│   │   └── Category.java                   # Enum: FOOD, TRANSPORT, etc.
│   ├── repository/
│   │   ├── UserRepository.java             # User database queries
│   │   └── ExpenseRepository.java          # Expense database queries
│   ├── security/
│   │   ├── SecurityConfig.java             # Spring Security configuration
│   │   ├── JwtTokenProvider.java           # JWT token generation & validation
│   │   ├── JwtAuthFilter.java              # Intercepts requests to validate JWT
│   │   └── CustomUserDetailsService.java   # Loads user from DB for Spring Security
│   └── service/
│       ├── AuthService.java                # Registration & login logic
│       └── ExpenseService.java             # Expense CRUD logic
├── src/main/resources/
│   └── application.yml                     # App configuration
├── src/test/
│   ├── java/.../integration/
│   │   └── ExpenseIntegrationTest.java     # End-to-end API tests
│   └── resources/
│       └── application-test.yml            # Test-specific config
├── Dockerfile                              # Multi-stage Docker build
├── docker-compose.yml                      # Runs app + PostgreSQL
└── pom.xml                                 # Maven dependencies & build
```

---

## 4. Application Entry Point

**File:** `ExpenseTrackerApplication.java`

```java
@SpringBootApplication
public class ExpenseTrackerApplication {
    public static void main(String[] args) {
        SpringApplication.run(ExpenseTrackerApplication.class, args);
    }
}
```

`@SpringBootApplication` combines three annotations:
- `@Configuration` — marks this as a config class
- `@EnableAutoConfiguration` — Spring Boot auto-configures beans based on dependencies (e.g., it sees `spring-boot-starter-data-jpa` + PostgreSQL driver on classpath, so it auto-creates a `DataSource`)
- `@ComponentScan` — scans `com.ashish.expensetracker` and all sub-packages for `@Component`, `@Service`, `@Repository`, `@Controller` classes

When the app starts, Spring Boot:
1. Reads `application.yml`
2. Connects to PostgreSQL
3. Hibernate creates/updates tables based on JPA entities (`ddl-auto: update`)
4. Registers all beans (controllers, services, security filters, etc.)
5. Starts the embedded Tomcat server on port 8080

---

## 5. Configuration

**File:** `application.yml`

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/expense_tracker
    username: postgres
    password: postgres
  jpa:
    hibernate:
      ddl-auto: update          # Auto-creates/updates DB tables from entities
    open-in-view: false          # Disables lazy-loading outside transactions (best practice)

jwt:
  secret: <base64-encoded-key>  # HMAC signing key for JWT tokens
  expiration-ms: 86400000       # Token validity: 24 hours (in milliseconds)

springdoc:
  swagger-ui:
    path: /swagger-ui.html      # Swagger UI accessible here
```

**Key points:**
- `ddl-auto: update` means Hibernate compares entity definitions to existing tables and applies ALTER statements. In production, you'd use `validate` with a migration tool like Flyway.
- `open-in-view: false` prevents lazy-loaded JPA proxies from firing database queries inside the controller/view layer — keeps data access strictly inside `@Service`/`@Transactional` boundaries.
- The JWT secret is Base64-encoded; it gets decoded into a `SecretKey` at startup.

---

## 6. Data Model Layer

### 6.1 User Entity

**File:** `model/User.java`

| Column       | Type      | Constraints                    |
|--------------|-----------|--------------------------------|
| `id`         | Long      | Primary key, auto-generated    |
| `username`   | String    | Unique, max 50 chars           |
| `email`      | String    | Unique, max 100 chars          |
| `password`   | String    | BCrypt-hashed, max 255 chars   |
| `role`       | Role enum | ROLE_USER or ROLE_ADMIN         |
| `created_at` | Instant   | Set automatically via `@PrePersist` |

`@PrePersist` on `onCreate()` sets `createdAt = Instant.now()` before the first INSERT — Hibernate calls this lifecycle hook automatically.

### 6.2 Expense Entity

**File:** `model/Expense.java`

| Column       | Type         | Constraints                     |
|--------------|--------------|---------------------------------|
| `id`         | Long         | Primary key, auto-generated     |
| `user_id`    | FK → User    | Many-to-one, lazy-fetched       |
| `amount`     | BigDecimal   | Precision 12, scale 2           |
| `category`   | Category enum| FOOD, TRANSPORT, HOUSING, etc.  |
| `description`| String       | Optional, max 500 chars         |
| `date`       | LocalDate    | The date of the expense         |
| `created_at` | Instant      | Set automatically via `@PrePersist` |

**Relationship:** Each `Expense` belongs to one `User` via `@ManyToOne(fetch = FetchType.LAZY)`. Lazy loading means the `User` object is only fetched from the DB when explicitly accessed (not on every expense query).

### 6.3 Enums

**Role** — `ROLE_USER`, `ROLE_ADMIN` (Spring Security expects the `ROLE_` prefix)

**Category** — `FOOD`, `TRANSPORT`, `HOUSING`, `UTILITIES`, `ENTERTAINMENT`, `HEALTHCARE`, `SHOPPING`, `OTHER`

---

## 7. Repository Layer (Data Access)

### 7.1 UserRepository

**File:** `repository/UserRepository.java`

```java
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    boolean existsByEmail(String email);
    boolean existsByUsername(String username);
}
```

Extends `JpaRepository` which gives you `save()`, `findById()`, `findAll()`, `delete()`, etc. for free. The custom methods use **Spring Data query derivation** — Spring reads the method name and auto-generates the SQL:
- `findByUsername` → `SELECT * FROM users WHERE username = ?`
- `existsByEmail` → `SELECT EXISTS(SELECT 1 FROM users WHERE email = ?)`

### 7.2 ExpenseRepository

**File:** `repository/ExpenseRepository.java`

```java
public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    Page<Expense> findByUser(User user, Pageable pageable);
    Optional<Expense> findByIdAndUser(Long id, User user);

    @Query("SELECT e FROM Expense e WHERE (:user IS NULL OR e.user = :user) " +
            "AND (:category IS NULL OR e.category = :category) " +
            "AND (:startDate IS NULL OR e.date >= :startDate) " +
            "AND (:endDate IS NULL OR e.date <= :endDate) " +
            "AND (:minAmount IS NULL OR e.amount >= :minAmount) " +
            "AND (:maxAmount IS NULL OR e.amount <= :maxAmount)")
    Page<Expense> findByUserWithFilters(...);
}
```

The `findByUserWithFilters` method uses a **custom JPQL query** that dynamically filters based on which parameters are non-null. If `category` is null, the `(:category IS NULL OR ...)` condition evaluates to true and skips that filter. This avoids building dynamic queries in Java code.

The `Page` return type + `Pageable` parameter gives automatic pagination (page number, page size, sort order).

---

## 8. DTO Layer (Data Transfer Objects)

DTOs decouple the API contract from the database entities. This means you can change the DB schema without breaking the API, and vice versa.

### Auth DTOs

| DTO               | Fields                              | Purpose            |
|-------------------|-------------------------------------|--------------------|
| `RegisterRequest` | username, email, password           | Registration input |
| `LoginRequest`    | username, password                  | Login input        |
| `AuthResponse`    | token, username, role               | Auth output        |

### Expense DTOs

| DTO               | Fields                                        | Purpose           |
|-------------------|-----------------------------------------------|--------------------|
| `ExpenseRequest`  | amount, category, description, date           | Create/update input|
| `ExpenseResponse` | id, userId, amount, category, description, date, createdAt | Output |
| `PagedResponse<T>`| content, page, size, totalElements, totalPages | Paginated wrapper |

### Validation (on Request DTOs)

Bean Validation annotations enforce rules before the request reaches service code:

```java
@NotBlank(message = "Username is required")
@Size(min = 3, max = 50)
private String username;

@NotNull(message = "Amount is required")
@DecimalMin(value = "0.01", message = "Amount must be greater than 0")
private BigDecimal amount;
```

When `@Valid` is used on a controller parameter, Spring automatically validates the request body and throws `MethodArgumentNotValidException` if rules fail.

---

## 9. Security Layer (JWT Authentication)

This is the most complex part. Here's how every piece fits together.

### 9.1 SecurityConfig

**File:** `security/SecurityConfig.java`

```java
http
    .csrf(csrf -> csrf.disable())                         // REST API, no CSRF needed
    .sessionManagement(session ->
        session.sessionCreationPolicy(STATELESS))         // No HTTP sessions — JWT only
    .authorizeHttpRequests(auth -> auth
        .requestMatchers("/api/v1/auth/**").permitAll()   // Login/register are public
        .requestMatchers("/swagger-ui/**", ...).permitAll()
        .anyRequest().authenticated())                    // Everything else needs a token
    .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
```

**What this does:**
1. Disables CSRF protection (CSRF tokens are for browser-based sessions; this is a stateless API)
2. Sets session policy to STATELESS — Spring won't create HTTP sessions
3. Allows unauthenticated access to `/api/v1/auth/**` (register & login) and Swagger UI
4. Requires authentication for all other endpoints
5. Adds the `JwtAuthFilter` before Spring's default auth filter so JWT tokens are checked first

**Beans defined:**
- `PasswordEncoder` → `BCryptPasswordEncoder` (hashes passwords before storing)
- `AuthenticationManager` → used by `AuthService` to verify username + password during login

### 9.2 JwtTokenProvider

**File:** `security/JwtTokenProvider.java`

Three responsibilities:

1. **Generate token** — Creates a signed JWT containing the username (subject) and role (claim), with a 24-hour expiry:
   ```java
   Jwts.builder()
       .subject(username)
       .claim("role", role)
       .issuedAt(now)
       .expiration(expiryDate)
       .signWith(secretKey)    // HMAC-SHA signature
       .compact();
   ```

2. **Extract username** — Parses and verifies the token signature, then returns the subject:
   ```java
   Jwts.parser().verifyWith(secretKey).build()
       .parseSignedClaims(token).getPayload().getSubject();
   ```

3. **Validate token** — Attempts to parse; returns `false` if the token is expired, malformed, or has an invalid signature.

### 9.3 JwtAuthFilter

**File:** `security/JwtAuthFilter.java`

This is a **servlet filter** that runs on every HTTP request. It extends `OncePerRequestFilter` (guaranteed to run exactly once per request).

**Flow:**
1. Extract the `Authorization` header
2. If it starts with `Bearer `, strip the prefix to get the raw JWT
3. Validate the token via `JwtTokenProvider.validateToken()`
4. If valid, extract the username and load the full `UserDetails` from the database
5. Create a `UsernamePasswordAuthenticationToken` and set it in `SecurityContextHolder`
6. Proceed with the filter chain

After step 5, Spring Security considers the request authenticated. Any downstream code (controllers, services) can access the authenticated user via `SecurityContextHolder.getContext().getAuthentication()`.

### 9.4 CustomUserDetailsService

**File:** `security/CustomUserDetailsService.java`

Bridges the app's `User` entity with Spring Security's `UserDetails` interface:

```java
User user = userRepository.findByUsername(username)...;
return new org.springframework.security.core.userdetails.User(
    user.getUsername(),
    user.getPassword(),
    Collections.singletonList(new SimpleGrantedAuthority(user.getRole().name())));
```

This is used in two places:
- **JwtAuthFilter** — to load the user during token validation
- **AuthenticationManager** — to verify credentials during login

---

## 10. Service Layer (Business Logic)

### 10.1 AuthService

**File:** `service/AuthService.java`

**`register(RegisterRequest)`**
1. Check if username or email already exists → throw `IllegalArgumentException` if so
2. Build a `User` entity with BCrypt-hashed password and `ROLE_USER`
3. Save to database
4. Generate a JWT token
5. Return `AuthResponse` (token + username + role)

**`login(LoginRequest)`**
1. Call `authenticationManager.authenticate()` — this internally:
   - Uses `CustomUserDetailsService` to load the user from DB
   - Compares the provided password (BCrypt) with the stored hash
   - Throws `BadCredentialsException` if they don't match
2. Fetch the user from DB
3. Generate a JWT token
4. Return `AuthResponse`

### 10.2 ExpenseService

**File:** `service/ExpenseService.java`

**`create(ExpenseRequest)`** — Gets the current authenticated user, builds an `Expense` entity, saves it, converts to `ExpenseResponse`.

**`getById(Long id)`** — Finds the expense by ID, checks the current user owns it (or is ADMIN), returns it.

**`getAll(page, size, filters...)`** — Builds a paginated, filtered query:
- If the current user is ADMIN, `filterUser` is set to `null` (sees all expenses)
- If USER, `filterUser` is set to the current user (sees only their own)
- Delegates to `ExpenseRepository.findByUserWithFilters()`

**`update(Long id, ExpenseRequest)`** — Finds expense with access check, updates fields, saves.

**`delete(Long id)`** — Finds expense with access check, deletes.

**Access control logic (`findExpenseWithAccessCheck`):**
```java
if (!expense.getUser().getId().equals(currentUser.getId())
        && currentUser.getRole() != Role.ROLE_ADMIN) {
    throw new UnauthorizedException("Not authorized to access this expense");
}
```
A regular user can only access their own expenses. An admin can access anyone's expenses.

**`getCurrentUser()`** — Reads the username from `SecurityContextHolder` (set by `JwtAuthFilter`) and fetches the `User` entity from the database.

---

## 11. Controller Layer (REST Endpoints)

### 11.1 AuthController

**Base path:** `/api/v1/auth` (public — no authentication required)

| Method | Path        | Description           | Request Body      | Response         | Status |
|--------|-------------|-----------------------|-------------------|------------------|--------|
| POST   | `/register` | Register a new user   | `RegisterRequest` | `AuthResponse`   | 201    |
| POST   | `/login`    | Login, get JWT token  | `LoginRequest`    | `AuthResponse`   | 200    |

### 11.2 ExpenseController

**Base path:** `/api/v1/expenses` (requires JWT token in `Authorization: Bearer <token>` header)

| Method | Path       | Description                         | Request Body     | Response                    | Status |
|--------|------------|-------------------------------------|------------------|-----------------------------|--------|
| POST   | `/`        | Create an expense                   | `ExpenseRequest` | `ExpenseResponse`           | 201    |
| GET    | `/`        | List expenses (paginated + filters) | —                | `PagedResponse<ExpenseResponse>` | 200 |
| GET    | `/{id}`    | Get single expense by ID            | —                | `ExpenseResponse`           | 200    |
| PUT    | `/{id}`    | Update an expense                   | `ExpenseRequest` | `ExpenseResponse`           | 200    |
| DELETE | `/{id}`    | Delete an expense                   | —                | —                           | 204    |

**GET `/` query parameters:**
- `page` (default: 0) — page number
- `size` (default: 10) — page size
- `category` — filter by category (e.g., `FOOD`)
- `startDate`, `endDate` — filter by date range (ISO format: `2025-01-01`)
- `minAmount`, `maxAmount` — filter by amount range

---

## 12. Exception Handling

**File:** `exception/GlobalExceptionHandler.java`

Uses `@RestControllerAdvice` to catch exceptions globally and return consistent JSON error responses.

| Exception                          | HTTP Status        | When it happens                                |
|------------------------------------|--------------------|------------------------------------------------|
| `ResourceNotFoundException`        | 404 Not Found      | Expense/user not found in DB                   |
| `UnauthorizedException`            | 403 Forbidden      | User tries to access another user's expense    |
| `IllegalArgumentException`         | 409 Conflict / 400 | Duplicate username/email, or bad input         |
| `BadCredentialsException`          | 401 Unauthorized   | Wrong password during login                    |
| `MethodArgumentNotValidException`  | 400 Bad Request    | Request body fails Bean Validation             |
| `Exception` (catch-all)            | 500 Internal Error | Any unhandled exception                        |

**Error response format:**
```json
{
  "timestamp": "2025-03-16T10:30:00Z",
  "status": 404,
  "message": "Expense not found with id: 42",
  "path": "/api/v1/expenses/42"
}
```

---

## 13. API Documentation (Swagger/OpenAPI)

**File:** `config/OpenApiConfig.java`

Configures SpringDoc to generate OpenAPI 3 documentation with JWT auth support:

```java
new OpenAPI()
    .info(new Info().title("Expense Tracker REST API").version("1.0"))
    .components(new Components()
        .addSecuritySchemes("bearerAuth",
            new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")));
```

**Access Swagger UI at:** `http://localhost:8080/swagger-ui.html`

From there you can:
1. Call the register/login endpoints to get a token
2. Click "Authorize" and paste the token
3. Test all expense endpoints interactively

---

## 14. Integration Tests

**File:** `test/.../ExpenseIntegrationTest.java`

Uses **Testcontainers** to spin up a real PostgreSQL 15 container during tests (requires Docker).

**Setup:**
1. `@Container` — declares a PostgreSQL container
2. `@DynamicPropertySource` — overrides `spring.datasource.*` properties to point at the container
3. `@BeforeAll` — registers a test user and logs in to get a JWT token

**Test cases:**
| Test | What it verifies |
|------|------------------|
| `createExpense_shouldReturn201` | POST creates an expense and returns 201 |
| `getExpenseById_shouldReturnExpense` | GET by ID returns the correct expense |
| `getAllExpenses_shouldReturnPaginatedResults` | GET with pagination returns correct page metadata |
| `getAllExpenses_withCategoryFilter_shouldReturnFilteredResults` | Category filter works |
| `updateExpense_shouldReturnUpdatedExpense` | PUT updates all fields |
| `deleteExpense_shouldReturn204` | DELETE returns 204 No Content |
| `getExpenseWithoutAuth_shouldReturn401` | Unauthenticated request is rejected |

**Test config** (`application-test.yml`):
```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: create-drop    # Fresh schema per test run
```

---

## 15. Docker Setup

### Dockerfile (Multi-stage build)

**Stage 1 — Build:**
```dockerfile
FROM eclipse-temurin:17-jdk-alpine AS build
COPY pom.xml .
RUN mvn dependency:go-offline      # Cache dependencies
COPY src ./src
RUN mvn package -DskipTests        # Build the JAR
```

**Stage 2 — Runtime:**
```dockerfile
FROM eclipse-temurin:17-jre-alpine
RUN addgroup -S spring && adduser -S spring -G spring   # Non-root user
COPY --from=build /app/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
```

The multi-stage build keeps the final image small (only JRE + app JAR, no Maven/source code).

### docker-compose.yml

Defines two services:

1. **postgres** — PostgreSQL 15 with a health check (`pg_isready`)
2. **app** — The Spring Boot app, depends on postgres being healthy

```bash
docker-compose up --build      # Build and start everything
```

The app overrides the datasource URL to use the Docker network hostname `postgres` instead of `localhost`.

---

## 16. Request Lifecycle - End-to-End Flow

Here's what happens when a user creates an expense:

### Step 1: User registers
```
POST /api/v1/auth/register
Body: { "username": "ashish", "email": "ashish@test.com", "password": "secret123" }
```
→ `AuthController.register()` → `AuthService.register()` → password hashed with BCrypt → saved to `users` table → JWT token generated → returned to client.

### Step 2: User gets a JWT token
The response contains:
```json
{ "token": "eyJhbGciOiJIUzI1NiJ9...", "username": "ashish", "role": "ROLE_USER" }
```

### Step 3: User creates an expense
```
POST /api/v1/expenses
Header: Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
Body: { "amount": 49.99, "category": "FOOD", "description": "Lunch", "date": "2025-03-16" }
```

**What happens internally:**

```
HTTP Request
    │
    ▼
┌──────────────┐
│ JwtAuthFilter │  Extracts "Bearer <token>" from header
│              │  Validates token signature + expiry
│              │  Loads UserDetails from DB
│              │  Sets SecurityContext (user is now authenticated)
└──────┬───────┘
       │
       ▼
┌──────────────────┐
│ SecurityConfig    │  Checks: /api/v1/expenses → requires authentication ✓
│ (authorization)   │  (user is authenticated from filter step)
└──────┬───────────┘
       │
       ▼
┌──────────────────┐
│ Bean Validation   │  Validates @NotNull amount, @NotNull category,
│                   │  @DecimalMin("0.01"), @NotNull date → all pass ✓
└──────┬───────────┘
       │
       ▼
┌──────────────────┐
│ ExpenseController │  Receives validated ExpenseRequest
│ .create()         │  Delegates to ExpenseService.create()
└──────┬───────────┘
       │
       ▼
┌──────────────────┐
│ ExpenseService    │  Gets current user from SecurityContextHolder
│ .create()         │  Builds Expense entity
│                   │  Calls expenseRepository.save()
│                   │  Converts to ExpenseResponse
└──────┬───────────┘
       │
       ▼
┌──────────────────┐
│ ExpenseRepository │  Hibernate generates:
│ .save()           │  INSERT INTO expenses (user_id, amount, category,
│                   │    description, date, created_at) VALUES (?, ?, ?, ?, ?, ?)
└──────┬───────────┘
       │
       ▼
┌──────────────────┐
│ PostgreSQL DB     │  Row inserted into expenses table
└──────────────────┘
       │
       ▼
   HTTP 201 Created
   { "id": 1, "userId": 1, "amount": 49.99, "category": "FOOD", ... }
```

### If something goes wrong:

| Scenario | What catches it | HTTP Response |
|----------|----------------|---------------|
| No/invalid JWT token | `JwtAuthFilter` skips auth → Spring Security rejects | 401 Unauthorized |
| Validation fails (e.g. amount is null) | Bean Validation → `MethodArgumentNotValidException` | 400 Bad Request |
| Expense not found | `ExpenseService` → `ResourceNotFoundException` | 404 Not Found |
| User tries to access another's expense | `ExpenseService` → `UnauthorizedException` | 403 Forbidden |
| Duplicate username at register | `AuthService` → `IllegalArgumentException` | 409 Conflict |
| Wrong password at login | `AuthenticationManager` → `BadCredentialsException` | 401 Unauthorized |

---

## Lombok Annotations Used

Since Lombok is used heavily, here's what each annotation does:

| Annotation | What it generates |
|------------|-------------------|
| `@Data` | `getters`, `setters`, `toString()`, `equals()`, `hashCode()` |
| `@Builder` | Builder pattern (e.g., `User.builder().username("x").build()`) |
| `@NoArgsConstructor` | No-argument constructor (required by JPA) |
| `@AllArgsConstructor` | Constructor with all fields |
| `@RequiredArgsConstructor` | Constructor for `final` fields (used for dependency injection) |

With `@RequiredArgsConstructor` on a service class, Lombok generates a constructor for all `final` fields, and Spring uses it for constructor-based dependency injection — no `@Autowired` needed.

---

## Quick Reference — How to Run

```bash
# Option 1: Docker Compose (easiest)
docker-compose up --build

# Option 2: Local PostgreSQL + Maven
# Ensure PostgreSQL is running on localhost:5432 with database "expense_tracker"
mvn spring-boot:run

# Run tests (requires Docker for Testcontainers)
mvn test
```

**Swagger UI:** http://localhost:8080/swagger-ui.html
