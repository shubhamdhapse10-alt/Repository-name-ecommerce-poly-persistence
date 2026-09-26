# E-commerce Order Management — Polyglot Persistence (PostgreSQL + MongoDB)

A practice project built to demonstrate **purposeful** use of two databases in one
Spring Boot application, aimed at Java backend interview prep.

## Why two databases (the core design decision)

| Data | Store | Why |
|---|---|---|
| Users, Orders, Order Items, Payments, Inventory | **PostgreSQL** | Needs ACID transactions, foreign keys, and row-level locking. Placing an order must atomically deduct stock, create the order, and record payment — all or nothing. |
| Product Catalog | **MongoDB** | Products across categories have wildly different attributes (phone: RAM/storage; shirt: size/color). A document model avoids a huge sparse relational table or an EAV pattern. |
| Reviews & Replies | **MongoDB** | Naturally nested, variable-depth data (a review with N replies) — one document read instead of a join. |
| Activity Logs | **MongoDB** | High-volume, append-only, no relational integrity needed. Written **asynchronously** so a slow/unavailable Mongo never blocks or fails an already-committed Postgres order — a small, honest example of eventual consistency between independent stores. |

## Tech stack
- Java 17, Spring Boot 3.3
- Spring Data JPA + PostgreSQL + Flyway (versioned schema migrations)
- Spring Data MongoDB
- Optimistic locking (`@Version`) on `Inventory` to prevent overselling under concurrent orders
- Testcontainers — integration tests run against **real** Postgres + Mongo containers, not mocks
- springdoc-openapi (Swagger UI)
- Docker Compose for local Postgres + Mongo + Mongo Express (UI)

## Project structure
```
src/main/java/com/example/ecommerce/
  entity/        JPA entities (Postgres): User, Order, OrderItem, Payment, Inventory
  document/      Mongo documents: Product, Review, ActivityLog
  repository/jpa/    Spring Data JPA repositories
  repository/mongo/  Spring Data MongoDB repositories
  service/       OrderService (the core transactional flow), ProductService, ReviewService
  controller/    REST controllers
  dto/           Request/response DTOs
  config/        AsyncConfig (enables @Async for activity logging)
src/main/resources/
  application.yml
  db/migration/V1__init_schema.sql   Flyway migration for Postgres schema
src/test/java/...  Testcontainers integration test for the order flow
```

## Running locally

```bash
# 1. Start Postgres + Mongo
docker compose up -d

# 2. Run the app (Flyway migrates Postgres automatically on startup)
./mvnw spring-boot:run

# 3. Swagger UI
open http://localhost:8080/swagger-ui.html

# 4. Mongo Express (view Mongo data visually)
open http://localhost:8081
```

## Try it out

```bash
# Create a product (writes to Mongo + creates an Inventory row in Postgres)
curl -X POST "http://localhost:8080/api/products?initialStock=10" \
  -H "Content-Type: application/json" \
  -d '{"name":"Wireless Mouse","category":"electronics","price":19.99,"attributes":{"color":"black","wireless":true}}'

# Register a user directly via Postgres (or add an AuthController — left as an exercise)

# Place an order (Postgres transaction: deduct stock, create order + payment)
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{"userId":1,"paymentMethod":"CARD","items":[{"productId":"<mongo-product-id>","quantity":2}]}'

# Add a review (Mongo)
curl -X POST http://localhost:8080/api/reviews \
  -H "Content-Type: application/json" \
  -d '{"productId":"<mongo-product-id>","userId":1,"userName":"Test","rating":5,"comment":"Great mouse"}'
```

## Run the integration tests
```bash
./mvnw test
```
This spins up real Postgres and Mongo containers via Testcontainers and verifies:
- A successful order deducts stock and creates the order + payment atomically.
- An order exceeding available stock throws and the **entire transaction rolls back**
  (stock is left unchanged) — proving the ACID guarantee actually holds.

## Where to go next (good "what would you add" answers for interviews)
1. **Spring Security + JWT** for real authentication/authorization.
2. **Kafka/RabbitMQ**: publish an `OrderPlaced` event from Postgres instead of the
   direct `@Async` Mongo write — decouples the write path further and adds
   at-least-once delivery semantics you can discuss (idempotency, outbox pattern).
3. **Transactional outbox pattern**: to guarantee the Mongo activity log is
   eventually written even if the app crashes right after the Postgres commit
   (currently, a crash between steps 3 and 4 in `OrderService` would silently
   drop the activity log — a good thing to point out you're aware of).
4. **Redis** cache in front of `ProductService.getProduct()` for hot products.
5. **Pagination + Mongo compound indexes** on `products` (category + price) and
   discuss index strategy differences vs. Postgres B-tree indexes.
6. **Pessimistic locking** (`findWithLockByProductId`, already stubbed in
   `InventoryRepository`) as an alternative to optimistic locking under very
   high contention on a single popular product — be ready to explain the
   trade-off (throughput vs. retry storms).

## Interview talking points this project gives you
- "Why Postgres here and Mongo there?" → answered by design, not an afterthought.
- "How do you keep two databases consistent?" → snapshotting (order items store
  product name/price at order time) + async writes + the outbox pattern as the
  next step you'd add for stronger guarantees.
- "How do you test code that touches two databases?" → Testcontainers, not mocks.
- "How do you prevent overselling under concurrency?" → optimistic locking via
  `@Version`, with pessimistic locking as a documented alternative.
