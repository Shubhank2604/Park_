# Parking Management System

A comprehensive parking management system built with Spring Boot, featuring microservices architecture, Redis caching, Kafka event streaming, and JWT authentication.

## Architecture Overview

The system is designed as a modular monolith that can be easily split into microservices:

- **Auth Module**: User management and JWT authentication
- **Lot Module**: Parking lot, level, and slot management
- **Ticket Module**: Entry/exit flow and slot allocation
- **Billing Module**: Tariff calculation and invoice generation
- **Common Module**: Shared DTOs, events, and utilities
- **Infra Module**: Redis, Kafka, and database configurations

## Technology Stack

- **Backend**: Spring Boot 3.2.0, Java 17
- **Database**: MySQL 8
- **Cache**: Redis 7
- **Message Queue**: Apache Kafka with Zookeeper
- **Security**: Spring Security with JWT
- **Build Tool**: Maven

## Prerequisites

- Java 17
- Docker Desktop
- IntelliJ IDEA (recommended)

## Quick Start

### 1. Start Infrastructure Services

```bash
docker-compose up -d
```

This will start:
- MySQL on port 3306
- Redis on port 6379
- Kafka on port 9092
- Zookeeper on port 2181

### 2. Run the Application

```bash
mvn spring-boot:run
```

The application will start on port 8080.

### 3. Default Users

The system initializes with these default users:

- **Admin**: `admin` / `admin123`
- **Attendant**: `attendant1` / `attendant123`
- **User**: `user1` / `user123`

## API Endpoints

### Authentication

```bash
# Login
POST /api/auth/login
{
  "username": "admin",
  "password": "admin123"
}

# Register new user
POST /api/auth/register
{
  "username": "newuser",
  "password": "password123"
}
```

### Parking Lot Management (Admin only)

```bash
# Create parking lot
POST /api/lots
{
  "name": "Mall Parking",
  "address": "456 Shopping Ave"
}

# Get all lots
GET /api/lots

# Get specific lot
GET /api/lots/{id}

# Create level
POST /api/lots/{id}/levels
{
  "levelNo": 1
}

# Create slots
POST /api/levels/{id}/slots
{
  "type": "CAR",
  "codes": ["A1", "A2", "A3"]
}

# Check availability
GET /api/lots/{id}/availability?type=CAR
```

### Entry/Exit Operations (Attendant/Admin)

```bash
# Vehicle entry
POST /api/entry
{
  "lotId": 1,
  "plateNo": "ABC123",
  "vehicleType": "CAR"
}

# Vehicle exit
POST /api/exit
{
  "identifier": "ABC123"  // or ticket ID
}
```

### Billing

```bash
# Get invoices for ticket
GET /api/invoices/{ticketId}

# Process payment
POST /api/pay/{invoiceId}
```

## Testing the System

### 1. Login as Admin

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

Save the JWT token from the response.

### 2. Check Available Slots

```bash
curl -X GET "http://localhost:8080/api/lots/1/availability?type=CAR" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### 3. Process Vehicle Entry

```bash
curl -X POST http://localhost:8080/api/entry \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{"lotId":1,"plateNo":"XYZ789","vehicleType":"CAR"}'
```

### 4. Process Vehicle Exit

```bash
curl -X POST http://localhost:8080/api/exit \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{"identifier":"XYZ789"}'
```

### 5. Check Generated Invoice

```bash
curl -X GET http://localhost:8080/api/invoices/{ticketId} \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

## Key Features

### Slot Allocation Algorithm

1. **Redis-First Approach**: Checks Redis cache for free slots
2. **Database Fallback**: Queries MySQL if Redis cache is empty
3. **Atomic Operations**: Uses transactions to ensure consistency
4. **Real-time Counters**: Maintains free slot counts in Redis

### Event-Driven Architecture

The system publishes events for:
- `parking.entry`: Vehicle entered
- `parking.exit`: Vehicle exited
- `slot.updated`: Slot status changed
- `billing.invoice.created`: Invoice generated
- `payment.completed`: Payment processed

### Tariff Calculation

Default pricing structure:
- **Base**: $5.00 for first 60 minutes
- **Additional**: $2.00 per 30-minute increment
- **EV Surcharge**: $1.00 additional for electric vehicles

### Security

- JWT-based authentication
- Role-based access control (ADMIN, ATTENDANT, USER)
- Password encryption with BCrypt

## Redis Data Structure

```
slot:{slotId} → HASH {status, type, levelId, lotId}
lot:{lotId}:free:{type} → INTEGER counter
lot:{lotId}:freeSlots:{type} → SET of slot IDs
vehicle:openTicket:{plate} → ticketId
ticket:summary:{ticketId} → HASH {slotId, lotId, entryTime, plate}
```

## Kafka Topics

- `parking.entry`
- `parking.exit`
- `slot.updated`
- `billing.invoice.created`
- `payment.completed`

## Database Schema

### Core Tables
- `parking_lots`: Parking lot information
- `levels`: Parking levels within lots
- `slots`: Individual parking slots
- `vehicles`: Vehicle information
- `tickets`: Parking tickets
- `invoices`: Billing invoices
- `users`: System users
- `tariff_rules`: Pricing configuration

## Future Enhancements

1. **Microservices Split**: Separate into Auth, Parking, and Billing services
2. **API Gateway**: Add Zuul or Spring Cloud Gateway
3. **Monitoring**: Integrate with Prometheus and Grafana
4. **Notifications**: Add SMS/email notifications
5. **Mobile App**: React Native mobile application
6. **Analytics**: Parking usage analytics and reporting

## Troubleshooting

### Common Issues

1. **Port Conflicts**: Ensure ports 3306, 6379, 9092, 2181 are available
2. **Docker Issues**: Run `docker-compose down` and `docker-compose up -d` to restart
3. **Database Connection**: Wait for MySQL to fully start before running the application
4. **Kafka Topics**: Topics are auto-created, but you can manually create them if needed

### Logs

Check application logs for detailed error messages:
```bash
tail -f logs/application.log
```

## Development

### Project Structure

```
src/main/java/com/park/
├── auth/           # Authentication and security
├── lot/            # Parking lot management
├── ticket/         # Entry/exit operations
├── billing/        # Billing and invoicing
├── vehicle/        # Vehicle management
├── common/         # Shared DTOs and events
└── infra/          # Infrastructure configuration
```

### Adding New Features

1. Create entity classes in appropriate module
2. Add repository interfaces
3. Implement service layer
4. Create REST controllers
5. Add security configuration if needed
6. Update data initialization if required

## License

This project is for educational purposes. Feel free to use and modify as needed.
