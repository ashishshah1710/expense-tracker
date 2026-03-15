# Expense Tracker REST API

A production-quality REST API for expense tracking with JWT authentication, role-based access control, and paginated queries with filtering.

## Tech Stack

- **Java 17**
- **Spring Boot 3.2.x**
- **Spring Security** with JWT
- **Spring Data JPA**
- **PostgreSQL 15**
- **Docker** & **Testcontainers**
- **Swagger/OpenAPI** (springdoc)

## Features

- JWT authentication (register, login)
- Role-based access control (ROLE_USER, ROLE_ADMIN)
- Full CRUD for expenses
- Paginated expense queries with filtering (category, date range, amount range)
- Users can only access their own expenses; admins can access all
- Comprehensive integration tests with Testcontainers
- OpenAPI/Swagger documentation

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/auth/register` | Register a new user |
| POST | `/api/v1/auth/login` | Login and get JWT token |
| POST | `/api/v1/expenses` | Create an expense |
| GET | `/api/v1/expenses` | Get all expenses (paginated, filterable) |
| GET | `/api/v1/expenses/{id}` | Get expense by ID |
| PUT | `/api/v1/expenses/{id}` | Update an expense |
| DELETE | `/api/v1/expenses/{id}` | Delete an expense |

### Query Parameters for GET /api/v1/expenses

| Parameter | Type | Description |
|-----------|------|-------------|
| page | int | Page number (default: 0) |
| size | int | Page size (default: 10) |
| category | enum | Filter by category (FOOD, TRANSPORT, HOUSING, etc.) |
| startDate | date | Filter expenses from this date |
| endDate | date | Filter expenses until this date |
| minAmount | decimal | Minimum amount filter |
| maxAmount | decimal | Maximum amount filter |

## How to Run

### Prerequisites

- Java 17+
- Maven 3.8+
- Docker (optional, for docker-compose)

### Local Development (with Docker PostgreSQL)

```bash
# Start PostgreSQL
docker-compose up -d postgres

# Run the application
mvn spring-boot:run
```

### Full Stack with Docker Compose

```bash
docker-compose up --build
```

The API will be available at `http://localhost:8080`.

### Swagger UI

After starting the application, access the API documentation at:

- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8080/v3/api-docs

Use the "Authorize" button in Swagger UI to add your JWT token (obtained from `/api/v1/auth/login`).

## Running Tests

```bash
mvn test
```

Integration tests use Testcontainers to spin up a PostgreSQL container automatically. **Docker must be running** for tests to pass.

## Project Structure

```
src/main/java/com/ashish/expensetracker/
├── config/          # OpenAPI configuration
├── controller/      # REST controllers
├── dto/             # Request/response DTOs
├── exception/       # Exception handling
├── model/           # JPA entities
├── repository/      # Data access
├── security/        # JWT, filters, security config
└── service/         # Business logic
```
