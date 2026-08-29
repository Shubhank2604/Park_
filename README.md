# Parking Management System

A modular Spring Boot backend for parking-lot administration, vehicle entry and exit, billing, authentication, Redis-backed availability, and Kafka events.

## Architecture

The application is currently a **modular monolith**:

- `auth`: JWT authentication and role-based access
- `lot`: parking lots, levels, slots, and cached availability
- `ticket`: vehicle entry and exit
- `billing`: tariffs, invoices, and payments
- `infra`: Redis and Kafka configuration
- `common`: shared DTOs, events, and local demo-data initialization

## Technology

Java 17 · Spring Boot 3.2 · MySQL 8 · Redis 7 · Kafka · Maven · Docker Compose

## Local setup

1. Create a local configuration file:

   ```bash
   cp .env.example .env
   ```

2. Replace every `replace_...` value. Generate a strong JWT secret rather than reusing a password:

   ```bash
   openssl rand -base64 48
   ```

3. Start MySQL, Redis, Kafka, and ZooKeeper:

   ```bash
   docker compose up -d
   ```

4. Run the application:

   ```bash
   mvn spring-boot:run
   ```

Spring loads the local `.env` file as properties. The file is ignored by Git and must never be committed.

## Optional demo data

Demo accounts and sample parking data are disabled by default. To enable them locally, set:

```env
SEED_DATA_ENABLED=true
SEED_ADMIN_PASSWORD=choose_a_local_password
SEED_ATTENDANT_PASSWORD=choose_a_local_password
SEED_USER_PASSWORD=choose_a_local_password
```

The application refuses to seed users when any enabled demo password is empty. Never enable these accounts in a shared or production environment.

## Main APIs

| Capability | Endpoint |
| --- | --- |
| Login | `POST /api/auth/login` |
| Register | `POST /api/auth/register` |
| List parking lots | `GET /api/lots` |
| Check availability | `GET /api/lots/{id}/availability?type=CAR` |
| Vehicle entry | `POST /api/entry` |
| Vehicle exit | `POST /api/exit` |
| Fetch invoice | `GET /api/invoices/{ticketId}` |
| Process payment | `POST /api/pay/{invoiceId}` |

Protected endpoints require `Authorization: Bearer <token>`.

## Current allocation flow

1. Select the first compatible free slot under a database pessimistic write lock.
2. Create the ticket and mark the slot occupied in the same MySQL transaction.
3. Write Kafka events to the outbox in the same transaction.
4. Update Redis only after the database commit succeeds.
5. Relay pending outbox rows to Kafka and mark them published after broker acknowledgement.
6. Periodically rebuild Redis free-slot sets and counters from MySQL, including empty slot types, to repair stale cache state after Redis downtime or missed post-commit updates.

MySQL is the allocation source of truth. Redis is a post-commit availability view; a stale or unavailable cache cannot allocate the same slot twice. A scheduled reconciliation atomically replaces cached free-slot sets and counters from database state. The outbox closes the crash window between committing parking state and sending Kafka events. Failed sends remain pending and retry with bounded exponential backoff.

Billing handles redelivered exit events idempotently: one ticket can create at most one invoice, enforced by both an existence check and a unique database constraint.

## Reliability work planned

The current implementation is an educational system and does not yet guarantee consistency across Redis, MySQL, and Kafka during partial failures. Planned improvements include:

- atomic Redis reservation and expiry;
- outbox retention and dead-letter operations;
- idempotency for future consumers beyond billing;
- Testcontainers integration tests and concurrent load tests; and
- Prometheus and Grafana observability.

Documenting these limitations is intentional: production-grade correctness requires measuring and testing the failure paths, not only the happy path.

## Useful commands

```bash
mvn test
mvn verify
mvn spring-boot:run
docker compose down
```
