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

1. Read the cached free-slot count.
2. Pop a candidate free slot from Redis.
3. Fall back to the database when the cache has no candidate.
4. create the ticket and mark the slot occupied in the database.
5. Update cached ticket and slot state.
6. Publish entry and slot events to Kafka.

## Reliability work planned

The current implementation is an educational system and does not yet guarantee consistency across Redis, MySQL, and Kafka during partial failures. Planned improvements include:

- atomic Redis reservation and expiry;
- database locking or a unique active-reservation constraint;
- compensation when a database transaction fails after cache reservation;
- a transactional outbox for Kafka events;
- idempotent event consumers;
- Testcontainers integration tests and concurrent load tests; and
- Prometheus and Grafana observability.

Documenting these limitations is intentional: production-grade correctness requires measuring and testing the failure paths, not only the happy path.

## Useful commands

```bash
mvn test
mvn spring-boot:run
docker compose down
```
