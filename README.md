# Stock Pilot

Inventory and point of sale (POS) REST API for small businesses: products, stock control, sales checkout with discounts, low stock alerts and daily sales reports.

**Stack:** Java 21, Spring Boot 3, Spring Data JPA/Hibernate, PostgreSQL, Flyway, Testcontainers, JUnit 5, Mockito, Maven.

## Architecture (multi-module Maven, ports & adapters)
```
stock-pilot/
├── core/            domain, business rules, ports (in = use cases + DTOs, out = repositories)
├── application/     use case implementations (services), mappers
└── infrastructure/  Spring Boot app: REST controllers, error handling, JPA adapters
```
Dependencies point inward: `infrastructure -> application -> core`. The compiler enforces it, since each layer is its own Maven module.

## Run
Requires Java 21 and Docker. Schema is versioned with Flyway (`infrastructure/src/main/resources/db/migration`).
```
docker compose up -d                      # PostgreSQL on localhost:5432
./mvnw test                               # integration tests use Testcontainers (real PostgreSQL)
./mvnw spring-boot:run -pl infrastructure
```
Connection can be overridden with `DB_URL`, `DB_USER` and `DB_PASSWORD`.

## Endpoints
- `POST /api/products`, `GET /api/products`, `GET /api/products/{id}`, `POST /api/products/{id}/restock`
- `GET /api/products/low-stock`
- `POST /api/sales`
- `GET /api/reports/daily?date=2026-11-05`

## Roadmap
Authentication, returns/refunds, suppliers and purchase orders, more reports.
