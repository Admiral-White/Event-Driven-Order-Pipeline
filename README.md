# Event-Driven Order Pipeline

A production-grade microservices-based event-driven order processing system built with Spring Boot, Apache Kafka, and PostgreSQL. This system demonstrates enterprise-level best practices for distributed order processing with event sourcing patterns, service discovery, and comprehensive monitoring.

## Table of Contents
- [Architecture Overview](#architecture-overview)
- [System Components](#system-components)
- [Prerequisites](#prerequisites)
- [Quick Start with Docker Compose](#quick-start-with-docker-compose)
- [Local Development Setup](#local-development-setup-without-docker)
- [Service Discovery with Consul](#service-discovery-with-consul)
- [API Gateway](#api-gateway)
- [API Endpoints](#api-endpoints)
- [Authentication & Authorization](#authentication--authorization)
- [Database Schemas](#database-schemas)
- [Configuration](#configuration)
- [Event Flow](#event-flow)
- [Monitoring and Health Checks](#monitoring-and-health-checks)
- [Key Design Patterns](#key-design-patterns)
- [End-to-End Testing](#end-to-end-testing)
- [Troubleshooting](#troubleshooting)
- [Production Deployment](#production-deployment)
- [Contributing](#contributing)

## Architecture Overview

The system consists of five interconnected microservices with centralized service discovery and API gateway:

```
┌─────────────────────────────────────────────────────────────────┐
│                        External Clients                         │
│      (Web Browser, Mobile App, Desktop, Third-party)            │
└────────────────────────────┬────────────────────────────────────┘
                             │
                   Port 8000 (HTTP/HTTPS)
                             │
                             ▼
           ┌──────────────────────────────────────┐
           │     API Gateway (Spring Cloud)       │
           │  • Request Routing & Load Balancing  │
           │  • CORS Configuration                │
           │  • Rate Limiting (100 req/s)         │
           │  • Service Discovery Integration     │
           └──────────────────┬───────────────────┘
                              │
              ┌───────────────┼───────────────┬──────────────┐
              │               │               │              │
              ▼               ▼               ▼              ▼
        ┌──────────┐   ┌─────────────┐  ┌──────────┐  ┌─────────┐
        │  Order   │   │ Settlement  │  │Reconcil- │  │  User   │
        │ Service  │   │  Service    │  │iation    │  │ Service │
        │ (8080)   │   │  (8081)     │  │ (8082)   │  │ (8083)  │
        └────┬─────┘   └──────┬──────┘  └────┬─────┘  └────┬────┘
             │                │              │             │
             └────────────────┼──────────────┴─────────────┘
                              │
                    ┌─────────┴──────────┐
                    │                    │
                    ▼                    ▼
              ┌──────────────┐     ┌──────────────┐
              │  Kafka       │     │  PostgreSQL  │
              │  (9092)      │     │  (5432)      │
              │              │     │              │
              │ Event Stream │     │ 3 Databases  │
              └──────────────┘     └──────────────┘
                    ▲                    │
                    │                    │
                    └────────────────────┘
                    
                    ┌──────────────────────────┐
                    │  Consul Service Registry │
                    │  (8500)                  │
                    │  • Service Discovery     │
                    │  • Health Checks         │
                    │  • Configuration Storage │
                    └──────────────────────────┘
```

## System Components

### 1. API Gateway (Port 8000)
- **Responsibility**: Single entry point for all external requests
- **Key Features**:
  - Request routing to all microservices
  - Automatic service discovery via Consul
  - Load balancing across service instances
  - CORS support for web frontends
  - Rate limiting (100 requests/second)
  - Health monitoring
- **See**: [API Gateway Documentation](API_GATEWAY.md)

### 2. Order Service (Port 8080)
- **Responsibility**: Order creation and management
- **Key Features**:
  - REST API for order creation and retrieval
  - Transactional outbox pattern for reliable event publishing
  - Order status tracking (PENDING → SHIPPED → DELIVERED → COMPLETED)
  - Spring Security integration
  - Automatic service registration with Consul
  - Complete audit trail

### 3. Settlement Service (Port 8081)
- **Responsibility**: Financial settlement processing
- **Key Features**:
  - Kafka consumer for order events
  - Settlement record creation and management
  - Fee calculation (2% by default, configurable)
  - Settlement status tracking (PENDING → PROCESSING → COMPLETED)
  - Audit trail for all settlement changes
  - Manual acknowledgment for reliable consumption

### 4. Reconciliation Service (Port 8082)
- **Responsibility**: Batch reconciliation and settlement verification
- **Key Features**:
  - Spring Batch for chunk-based processing
  - Scheduled daily reconciliation jobs (2:00 AM UTC by default)
  - Skip listeners for graceful error handling
  - Settlement status validation
  - Failed reconciliation tracking
  - Job execution history and metrics

### 5. User Service (Port 8083)
- **Responsibility**: Authentication, user management, and authorization
- **Key Features**:
  - JWT-based authentication
  - User registration and profile management
  - Role-based access control (RBAC)
  - Token refresh and validation
  - User audit trail
  - Integration with Spring Security across services

### 6. Service Discovery (Consul - Port 8500)
- **Responsibility**: Centralized service registry and health monitoring
- **Key Features**:
  - Automatic service registration on startup
  - Automatic deregistration on shutdown
  - Health checks every 10 seconds
  - Service instance tracking
  - Inter-service service discovery
  - Consul UI for monitoring
- **See**: [Service Discovery Documentation](SERVICE_DISCOVERY.md)

## Prerequisites

- **Java 17+**
- **Maven 3.8+**
- **Docker** and **Docker Compose**
- **PostgreSQL 15+** (or use Docker)
- **Apache Kafka** (or use Docker)
- **curl** or Postman (for API testing)
- **Git** (optional, for version control)

## Quick Start with Docker Compose

### 1. Start All Services

```bash
docker-compose up -d
```

This will start:
- PostgreSQL database (port 5432) with 3 databases
- Apache Kafka (port 9092)
- Zookeeper (port 2181)
- Consul Service Discovery (port 8500, UI at http://localhost:8500)
- Order Service (port 8080)
- Settlement Service (port 8081)
- Reconciliation Service (port 8082)
- User Service (port 8083)
- API Gateway (port 8000) - **Primary entry point**

### 2. Verify Services Are Running

```bash
# Check all containers
docker-compose ps

# View logs
docker-compose logs -f

# Check specific service
docker-compose logs order-service

# Wait for healthy status (30-60 seconds)
# Expected: All services show "healthy" status
```

### 3. Access Services

```bash
# API Gateway (recommended for external requests)
http://localhost:8000

# Individual Services (for direct access)
http://localhost:8080  # Order Service
http://localhost:8081  # Settlement Service
http://localhost:8082  # Reconciliation Service
http://localhost:8083  # User Service

# Service Discovery UI
http://localhost:8500  # Consul UI (view registered services)

# Service Health Checks
curl http://localhost:8000/actuator/health   # Via Gateway
curl http://localhost:8080/actuator/health   # Direct
```

## Local Development Setup (Without Docker)

### 1. Start PostgreSQL

```bash
# Using Docker for just PostgreSQL
docker run -d \
  --name postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 \
  postgres:15-alpine

# Create databases
docker exec -it postgres psql -U postgres -c "CREATE DATABASE order_db;"
docker exec -it postgres psql -U postgres -c "CREATE DATABASE settlement_db;"
docker exec -it postgres psql -U postgres -c "CREATE DATABASE reconciliation_db;"
```

### 2. Start Kafka

```bash
# Using Docker for Kafka
docker run -d \
  --name kafka \
  -e KAFKA_ZOOKEEPER_CONNECT=zookeeper:2181 \
  -e KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://localhost:9092 \
  -p 9092:9092 \
  confluentinc/cp-kafka:7.5.0
```

### 3. Build and Run Services

```bash
# From project root
mvn clean install

# Terminal 1: Order Service
cd order-service
mvn spring-boot:run

# Terminal 2: Settlement Service
cd settlement-service
mvn spring-boot:run

# Terminal 3: Reconciliation Service
cd reconciliation-service
mvn spring-boot:run
```

## API Endpoints

### Gateway Base URL
All requests should be routed through the API Gateway for production use:
```
http://localhost:8000
```

For direct service access (development only):
- Order Service: http://localhost:8080
- Settlement Service: http://localhost:8081
- Reconciliation Service: http://localhost:8082
- User Service: http://localhost:8083

### Authentication Service (User Service)

#### Register New User
```bash
POST /auth/register
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "securePassword123",
  "firstName": "John",
  "lastName": "Doe"
}

Response: 201 Created
{
  "id": 1,
  "email": "user@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "createdAt": "2024-10-22T10:30:00Z"
}
```

#### Login
```bash
POST /auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "securePassword123"
}

Response: 200 OK
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expiresIn": 3600,
  "user": {
    "id": 1,
    "email": "user@example.com",
    "firstName": "John",
    "lastName": "Doe"
  }
}
```

#### Validate Token
```bash
GET /auth/validate
Authorization: Bearer {token}

Response: 200 OK
{
  "valid": true,
  "userId": 1,
  "email": "user@example.com"
}
```

#### Refresh Token
```bash
POST /auth/refresh
Content-Type: application/json

{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}

Response: 200 OK
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expiresIn": 3600
}
```

### Order Service (http://localhost:8000/api/v1/orders)

#### Create Order
```bash
POST /api/v1/orders
Authorization: Bearer {token}
Content-Type: application/json

{
  "customerId": "CUST001",
  "productId": "PROD001",
  "quantity": 5,
  "unitPrice": 100.00
}

Response: 201 Created
{
  "id": 1,
  "orderNumber": "ORD-20241022-001",
  "customerId": "CUST001",
  "productId": "PROD001",
  "quantity": 5,
  "unitPrice": 100.00,
  "totalAmount": 500.00,
  "status": "PENDING",
  "createdAt": "2024-10-22T10:30:00Z"
}
```

#### Get Order by Order Number
```bash
GET /api/v1/orders/{orderNumber}
Authorization: Bearer {token}

Example: GET /api/v1/orders/ORD-20241022-001

Response: 200 OK
{
  "id": 1,
  "orderNumber": "ORD-20241022-001",
  "customerId": "CUST001",
  "productId": "PROD001",
  "quantity": 5,
  "unitPrice": 100.00,
  "totalAmount": 500.00,
  "status": "PENDING",
  "createdAt": "2024-10-22T10:30:00Z"
}
```

#### Get Customer Orders
```bash
GET /api/v1/orders/customer/{customerId}
Authorization: Bearer {token}

Example: GET /api/v1/orders/customer/CUST001

Response: 200 OK
[
  {
    "id": 1,
    "orderNumber": "ORD-20241022-001",
    "customerId": "CUST001",
    "status": "PENDING",
    ...
  },
  {
    "id": 2,
    "orderNumber": "ORD-20241022-002",
    "customerId": "CUST001",
    "status": "SHIPPED",
    ...
  }
]
```

#### Update Order Status
```bash
PATCH /api/v1/orders/{orderId}/status
Authorization: Bearer {token}
Content-Type: application/json

{
  "status": "SHIPPED"
}

Valid statuses: PENDING, SHIPPED, DELIVERED, COMPLETED, CANCELLED

Response: 200 OK
{
  "id": 1,
  "orderNumber": "ORD-20241022-001",
  "status": "SHIPPED",
  "updatedAt": "2024-10-22T11:00:00Z",
  ...
}
```

### Settlement Service (http://localhost:8000/api/v1/settlements)

#### Get All Settlements
```bash
GET /api/v1/settlements
Authorization: Bearer {token}

Response: 200 OK
[
  {
    "settlementId": "SETTLE-001",
    "orderId": 1,
    "orderNumber": "ORD-20241022-001",
    "customerId": "CUST001",
    "settlementAmount": 500.00,
    "feeAmount": 10.00,
    "netAmount": 490.00,
    "status": "COMPLETED",
    "createdAt": "2024-10-22T10:31:00Z"
  }
]
```

#### Get Settlement by ID
```bash
GET /api/v1/settlements/{settlementId}
Authorization: Bearer {token}

Response: 200 OK
{
  "settlementId": "SETTLE-001",
  "orderId": 1,
  "orderNumber": "ORD-20241022-001",
  "customerId": "CUST001",
  "settlementAmount": 500.00,
  "feeAmount": 10.00,
  "netAmount": 490.00,
  "status": "COMPLETED",
  "createdAt": "2024-10-22T10:31:00Z"
}
```

#### Get Settlements by Order ID
```bash
GET /api/v1/settlements/order/{orderId}
Authorization: Bearer {token}

Response: 200 OK
{
  "settlementId": "SETTLE-001",
  "orderId": 1,
  "status": "COMPLETED",
  ...
}
```

### Reconciliation Service (http://localhost:8000/api/v1/reconciliation)

#### Check Reconciliation Status
```bash
GET /api/v1/reconciliation/status
Authorization: Bearer {token}

Response: 200 OK
{
  "lastRunTime": "2024-10-22T02:00:00Z",
  "nextRunTime": "2024-10-23T02:00:00Z",
  "status": "COMPLETED",
  "recordsProcessed": 150,
  "recordsFailed": 2,
  "duration": "45 seconds"
}
```

#### Trigger Manual Reconciliation Run
```bash
POST /api/v1/reconciliation/run
Authorization: Bearer {token}

Response: 202 Accepted
{
  "jobId": "RECONCILE-20241022-120000",
  "status": "RUNNING",
  "startTime": "2024-10-22T12:00:00Z"
}
```

#### Get Reconciliation Job History
```bash
GET /api/v1/reconciliation/history
Authorization: Bearer {token}
?page=0&size=20

Response: 200 OK
{
  "content": [
    {
      "jobId": "RECONCILE-20241022-020000",
      "status": "COMPLETED",
      "recordsProcessed": 150,
      "recordsFailed": 0,
      "duration": "45 seconds",
      "startTime": "2024-10-22T02:00:00Z",
      "endTime": "2024-10-22T02:00:45Z"
    }
  ],
  "totalElements": 25,
  "totalPages": 2,
  "currentPage": 0
}
```

### User Service (http://localhost:8000/api/v1/users)

#### Get User Profile
```bash
GET /api/v1/users/{userId}
Authorization: Bearer {token}

Response: 200 OK
{
  "id": 1,
  "email": "user@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "roles": ["USER"],
  "createdAt": "2024-10-22T10:00:00Z"
}
```

#### Update User Profile
```bash
PUT /api/v1/users/{userId}
Authorization: Bearer {token}
Content-Type: application/json

{
  "firstName": "Jonathan",
  "lastName": "Smith"
}

Response: 200 OK
{
  "id": 1,
  "email": "user@example.com",
  "firstName": "Jonathan",
  "lastName": "Smith",
  "updatedAt": "2024-10-22T12:00:00Z"
}
```

#### Get All Users (Admin Only)
```bash
GET /api/v1/users
Authorization: Bearer {token}
X-Admin-Token: {adminToken}

Response: 200 OK
[
  {
    "id": 1,
    "email": "user1@example.com",
    "firstName": "John",
    "lastName": "Doe",
    "roles": ["USER"],
    "status": "ACTIVE"
  },
  {
    "id": 2,
    "email": "user2@example.com",
    "firstName": "Jane",
    "lastName": "Smith",
    "roles": ["ADMIN"],
    "status": "ACTIVE"
  }
]
```

### Gateway Diagnostic Endpoints (http://localhost:8000)

#### Gateway Health Check
```bash
GET /gateway/health

Response: 200 OK
{
  "status": "UP",
  "components": {
    "consul": { "status": "UP" },
    "kafkaListener": { "status": "UP" },
    "db": { "status": "UP" },
    "diskSpace": { "status": "UP" }
  }
}
```

#### Gateway Metrics
```bash
GET /gateway/metrics

Response: 200 OK
{
  "names": [
    "gateway.requests",
    "gateway.routes.active",
    "http.server.requests",
    "jvm.memory.used",
    "process.cpu.usage",
    ...
  ]
}
```

#### Gateway Routes
```bash
GET /gateway/routes

Response: 200 OK
[
  {
    "route_id": "order-service-route",
    "route_object": {
      "id": "order-service-route",
      "uri": "lb://order-service"
    }
  },
  {
    "route_id": "settlement-service-route",
    "route_object": {
      "id": "settlement-service-route",
      "uri": "lb://settlement-service"
    }
  },
  ...
]
```

### Individual Service Health Checks (Direct Access)

```bash
# Order Service
curl http://localhost:8080/actuator/health

# Settlement Service
curl http://localhost:8081/actuator/health

# Reconciliation Service
curl http://localhost:8082/actuator/health

# User Service
curl http://localhost:8083/actuator/health

# All return format:
{
  "status": "UP",
  "components": {
    "consul": { "status": "UP" },
    "db": { "status": "UP" },
    "kafka": { "status": "UP" }
  }
}
```

## Authentication & Authorization

### JWT Token-Based Authentication

All protected endpoints require a valid JWT bearer token in the Authorization header:

```bash
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### Authentication Flow

```
1. User Registration/Login
   POST /auth/register or /auth/login
   ↓
2. Server validates credentials
   ↓
3. JWT token generated (expires in 1 hour)
   ↓
4. Token returned to client
   ↓
5. Client includes token in Authorization header for all requests
   ↓
6. Server validates token before processing request
   ↓
7. Token validation fails?
   → Server returns 401 Unauthorized
   → Client uses refresh token to get new token
   ↓
8. Request proceeds (if token valid and user authorized)
```

### Token Refresh Process

When your access token expires, use the refresh token:

```bash
POST /auth/refresh
Content-Type: application/json

{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}

Response: 200 OK
{
  "token": "new_access_token",
  "expiresIn": 3600
}
```

### Role-Based Access Control

Different endpoints require different roles:

```
PUBLIC (No authentication required):
- POST /auth/register
- POST /auth/login
- GET /auth/validate

USER (Requires 'USER' role):
- POST /api/v1/orders (create order)
- GET /api/v1/orders/* (view orders)
- GET /api/v1/settlements/* (view settlements)
- PATCH /api/v1/users/{userId} (update own profile)

ADMIN (Requires 'ADMIN' role):
- DELETE /api/v1/orders/{orderId} (cancel order)
- PUT /api/v1/settlements/{settlementId} (modify settlement)
- GET /api/v1/users (list all users)
- POST /api/v1/reconciliation/run (trigger reconciliation)
```

### Example: Making Authenticated Requests

```bash
# 1. Login to get token
LOGIN_RESPONSE=$(curl -X POST http://localhost:8000/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "password123"
  }')

TOKEN=$(echo $LOGIN_RESPONSE | jq -r '.token')

# 2. Use token in subsequent requests
curl http://localhost:8000/api/v1/orders \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "CUST001",
    "productId": "PROD001",
    "quantity": 5,
    "unitPrice": 100.00
  }'
```

### CORS Support

The API Gateway supports Cross-Origin Resource Sharing (CORS) for web frontends:

**Allowed Origins (Development):**
- http://localhost:3000 (React)
- http://localhost:4200 (Angular)
- http://localhost:8081 (Vue)
- http://localhost:8888 (Other)

**Allowed Methods:**
- GET, POST, PUT, DELETE, PATCH, OPTIONS

**Allowed Headers:**
- Content-Type, Authorization, Accept, Cache-Control

**Note:** For production, update the allowed origins in `api-gateway/src/main/java/goldmansachs/gateway/config/GatewayConfig.java`

## Service Discovery with Consul

### What is Consul?

Consul is a centralized service registry that:
- Automatically registers services on startup
- Monitors service health via health checks
- Enables automatic service discovery
- Provides load balancing across service instances
- Offers a Web UI for monitoring

### Accessing Consul UI

Open your browser and navigate to:
```
http://localhost:8500/ui/
```

You'll see:
- **Services Tab**: All registered microservices with health status
- **Nodes Tab**: All service instances
- **Key/Value Tab**: Configuration storage

### Service Registration

Each microservice automatically registers itself when it starts:

```
Service Name          Port    Instance ID
─────────────────────────────────────────────────────────────
order-service         8080    order-service:hostname:8080
settlement-service    8081    settlement-service:hostname:8081
reconciliation-svc    8082    reconciliation-service:hostname:8082
user-service          8083    user-service:hostname:8083
api-gateway          8000    api-gateway:hostname:8000
```

### Health Checks

Consul performs health checks every 10 seconds:

```
Consul → GET /actuator/health

Service Response:
✓ Status UP    → Instance marked as "passing" ✓
✗ Status DOWN  → Instance marked as "failing" ✗

Failed checks:
- Unhealthy instances are excluded from routing
- Healthy instances automatically re-included when recovered
- 3 consecutive failures trigger deregistration
```

### Inter-Service Communication

Services discover and communicate with each other using service names:

```java
@Autowired
private RestTemplate discoveryRestTemplate;  // Load-balanced

// Call settlement-service using service name
String result = discoveryRestTemplate
    .getForObject("http://settlement-service/api/v1/settlements", String.class);

// Ribbon automatically:
// 1. Queries Consul for settlement-service instances
// 2. Selects healthy instances
// 3. Load-balances across instances
// 4. Excludes unhealthy instances
```

### Debugging Consul

```bash
# List all registered services
curl http://localhost:8500/v1/catalog/services | jq

# Check specific service
curl http://localhost:8500/v1/catalog/service/order-service | jq

# View service health
curl http://localhost:8500/v1/health/service/settlement-service | jq

# View Consul logs
docker-compose logs consul

# Access Consul from within a container
docker-compose exec order-service curl http://consul:8500/v1/health/service/order-service
```

## API Gateway

### What is the API Gateway?

The API Gateway is the single entry point for all external requests. Instead of calling services directly, clients make all requests to port 8000.

```
Without Gateway:           With Gateway:
─────────────────────      ──────────────

Client                     Client
  ├─→ Service A (8080)       └─→ Gateway (8000)
  ├─→ Service B (8081)           ├─→ Service A
  ├─→ Service C (8082)           ├─→ Service B
  └─→ Service D (8083)           ├─→ Service C
                                 └─→ Service D
```

### Gateway Features

| Feature | Purpose |
|---------|---------|
| **Request Routing** | Routes requests to correct microservices based on URL path |
| **Service Discovery** | Automatically discovers service instances via Consul |
| **Load Balancing** | Distributes traffic across multiple instances |
| **Health Monitoring** | Ensures only healthy instances receive traffic |
| **CORS Support** | Enables web frontend requests from different ports |
| **Rate Limiting** | Protects services from overload (100 req/sec) |

### Gateway Request Flow

```
1. Client Request to Gateway
   GET http://localhost:8000/api/v1/orders/123

2. Gateway matches route
   → /api/v1/orders/** matches order-service-route

3. Query Consul for instances
   → Request: "Give me instances of order-service"
   → Response: [172.20.0.3:8080, 172.20.0.4:8080]

4. Select healthy instance (via Ribbon)
   → Both healthy, select first via round-robin

5. Forward request to selected instance
   → GET http://172.20.0.3:8080/api/v1/orders/123

6. Service responds
   → HTTP 200 OK with order data

7. Gateway returns response to client
```

### Rate Limiting

Rate limiting protects your services from overload:

**Limits:**
- **Steady Rate**: 100 requests/second
- **Burst Capacity**: 200 requests (allows temporary spikes)

**Behavior:**
```
Timeline:
- 0.0s: Bucket has 200 tokens
- 0.1s: 100 requests consume 100 tokens, 100 remain
- 0.2s: 50 requests consume 50 tokens, 50 remain
- 1.0s: Bucket refilled to 200 tokens (100 tokens/sec)

If bucket empty:
- Request returns: HTTP 429 Too Many Requests
```

**Testing Rate Limits:**

```bash
# Normal request (within limits)
for i in {1..50}; do
  curl http://localhost:8000/api/v1/orders -s -o /dev/null -w "%{http_code}\n"
done
# Returns: 200 200 200 ... (all pass)

# Burst request (exceeds capacity)
for i in {1..300}; do
  curl http://localhost:8000/api/v1/orders -s -o /dev/null -w "%{http_code}\n" &
done
wait
# Returns: 200 200 ... (200 requests) 429 429 ... (100+ requests fail)
```

### Gateway Configuration

View all routes:
```bash
curl http://localhost:8000/actuator/gateway/routes | jq
```

Expected routes:
- `order-service-route` → `/api/v1/orders/**`
- `settlement-service-route` → `/api/v1/settlements/**`
- `reconciliation-service-route` → `/api/v1/reconciliation/**`
- `user-service-route` → `/api/v1/users/**`
- `auth-service-route` → `/auth/**`
- `gateway-actuator-route` → `/gateway/**`

### See Also

For complete API Gateway documentation, refer to [API_GATEWAY.md](API_GATEWAY.md)

## Database Schemas

### Three Independent Databases

The system follows the "Database Per Service" pattern with three separate PostgreSQL databases:

```
┌────────────────────┐    ┌─────────────────────┐    ┌──────────────────────┐
│   Order Database   │    │ Settlement Database │    │ Reconciliation DB    │
│    (order_db)      │    │ (settlement_db)     │    │  (reconciliation_db) │
├────────────────────┤    ├─────────────────────┤    ├──────────────────────┤
│                    │    │                     │    │                      │
│ • orders           │    │ • settlements       │    │ • batch_job_instance │
│ • outbox_events    │    │ • audit_trail       │    │ • batch_job_exec...  │
│                    │    │ • settlement_hist   │    │ • batch_step_exec... │
│                    │    │                     │    │ • recon_runs         │
│                    │    │                     │    │ • recon_failures     │
└────────────────────┘    └─────────────────────┘    └──────────────────────┘
        ↓                          ↓                          ↓
      Schema V1            Schema V2 (V1_init.sql)    Schema V3 (V2_settlement.sql)
    (V3_reconciliation.sql)
```

### Order Database (V1_init.sql)

**orders table:**
```sql
CREATE TABLE orders (
    id BIGSERIAL PRIMARY KEY,
    order_number VARCHAR(255) UNIQUE NOT NULL,
    customer_id VARCHAR(255) NOT NULL,
    product_id VARCHAR(255) NOT NULL,
    quantity INTEGER NOT NULL,
    unit_price NUMERIC(19,2) NOT NULL,
    total_amount NUMERIC(19,2) NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    
    INDEX idx_order_number (order_number),
    INDEX idx_customer_id (customer_id),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
);
```

**outbox_events table:**
```sql
CREATE TABLE outbox_events (
    id BIGSERIAL PRIMARY KEY,
    event_type VARCHAR(255) NOT NULL,
    payload TEXT NOT NULL,
    aggregate_id BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    published_at TIMESTAMP,
    
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
);

-- Status values: PENDING → PUBLISHED → ARCHIVED
```

### Settlement Database (V2_settlement.sql)

**settlement_records table:**
```sql
CREATE TABLE settlement_records (
    id BIGSERIAL PRIMARY KEY,
    settlement_id VARCHAR(255) UNIQUE NOT NULL,
    order_id BIGINT NOT NULL,
    order_number VARCHAR(255) NOT NULL,
    customer_id VARCHAR(255) NOT NULL,
    merchant_id VARCHAR(255),
    settlement_amount NUMERIC(19,2) NOT NULL,
    fee_amount NUMERIC(19,2) NOT NULL,
    net_amount NUMERIC(19,2) NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    
    UNIQUE(order_id),
    INDEX idx_settlement_id (settlement_id),
    INDEX idx_order_id (order_id),
    INDEX idx_status (status),
    INDEX idx_customer_id (customer_id),
    INDEX idx_created_at (created_at)
);

-- Status values: PENDING → PROCESSING → COMPLETED or CANCELLED
-- Fee calculation: settlement_amount * 0.02 (2%)
```

**settlement_audit_trail table:**
```sql
CREATE TABLE settlement_audit_trail (
    id BIGSERIAL PRIMARY KEY,
    settlement_id VARCHAR(255) NOT NULL,
    old_status VARCHAR(50),
    new_status VARCHAR(50) NOT NULL,
    change_reason VARCHAR(500),
    changed_at TIMESTAMP NOT NULL,
    changed_by VARCHAR(255),
    
    INDEX idx_settlement_id (settlement_id)
);

-- Tracks all status changes for compliance and debugging
```

### Reconciliation Database (V3_reconciliation.sql)

**Spring Batch Infrastructure Tables:**
```sql
-- Standard Spring Batch tables for job tracking
BATCH_JOB_INSTANCE
BATCH_JOB_EXECUTION
BATCH_JOB_EXECUTION_PARAMS
BATCH_JOB_EXECUTION_CONTEXT
BATCH_STEP_EXECUTION
BATCH_STEP_EXECUTION_CONTEXT
```

**reconciliation_runs table:**
```sql
CREATE TABLE reconciliation_runs (
    id BIGSERIAL PRIMARY KEY,
    job_id VARCHAR(255) UNIQUE NOT NULL,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP,
    records_processed INTEGER,
    records_failed INTEGER,
    status VARCHAR(50) NOT NULL,
    
    INDEX idx_status (status),
    INDEX idx_start_time (start_time)
);

-- Status values: RUNNING → COMPLETED or FAILED
```

**reconciliation_failures table:**
```sql
CREATE TABLE reconciliation_failures (
    id BIGSERIAL PRIMARY KEY,
    reconciliation_run_id BIGINT NOT NULL,
    settlement_id VARCHAR(255),
    error_message TEXT,
    failed_at TIMESTAMP NOT NULL,
    
    FOREIGN KEY (reconciliation_run_id) REFERENCES reconciliation_runs(id)
);

-- Records all failures for investigation and retry
```

### Connecting to Databases

```bash
# Connect to PostgreSQL container
docker exec -it event-driven-postgres psql -U postgres

# List all databases
\l

# Connect to order_db
\c order_db

# List tables
\dt

# Query orders
SELECT * FROM orders;

# Query outbox events (pending events)
SELECT * FROM outbox_events WHERE status = 'PENDING';

# Exit
\q
```

## Configuration

### application.yml Files

All services use Spring Boot's YAML configuration. Each service has its own `application.yml` in `src/main/resources/`.

#### Common Configuration (All Services)

```yaml
spring:
  application:
    name: {service-name}
  
  # Database configuration
  datasource:
    url: jdbc:postgresql://postgres:5432/{database_name}
    username: postgres
    password: postgres
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
  
  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
  
  # Flyway database migrations
  flyway:
    enabled: true
    baseline-on-migrate: true
    locations: classpath:db/migration
  
  # Kafka configuration
  kafka:
    bootstrap-servers: kafka:9092
    consumer:
      group-id: {service-group}
      auto-offset-reset: earliest
  
  # Service Discovery (Consul)
  cloud:
    consul:
      host: consul
      port: 8500
      discovery:
        enabled: true
        register-health-check: true
        health-check-interval: 10s

# Actuator endpoints (monitoring)
management:
  endpoints:
    web:
      exposure:
        include: health, metrics, info
  endpoint:
    health:
      show-details: when-authorized
```

#### Order Service (application.yml)

```yaml
server:
  port: 8080
  servlet:
    context-path: /

spring:
  application:
    name: order-service
  datasource:
    url: jdbc:postgresql://postgres:5432/order_db

# Outbox event publishing poll interval
app:
  outbox:
    poll-interval-ms: 5000  # Check for pending events every 5 seconds
    batch-size: 100
```

#### Settlement Service (application.yml)

```yaml
server:
  port: 8081

spring:
  application:
    name: settlement-service
  datasource:
    url: jdbc:postgresql://postgres:5432/settlement_db
  kafka:
    consumer:
      group-id: settlement-service-group
      concurrency: 3  # Number of consumer threads
      max-poll-records: 100

# Settlement fee (2% of order amount)
app:
  settlement:
    fee-percentage: 2.0
```

#### Reconciliation Service (application.yml)

```yaml
server:
  port: 8082

spring:
  application:
    name: reconciliation-service
  datasource:
    url: jdbc:postgresql://postgres:5432/reconciliation_db
  batch:
    job:
      enabled: true
    jdbc:
      initialize-database: always

# Batch job configuration
app:
  reconciliation:
    chunk-size: 100
    schedule: "0 0 2 * * ?"  # Daily at 2:00 AM UTC
```

#### User Service (application.yml)

```yaml
server:
  port: 8083

spring:
  application:
    name: user-service
  datasource:
    url: jdbc:postgresql://postgres:5432/user_db
  jpa:
    hibernate:
      ddl-auto: update

# JWT configuration
app:
  jwt:
    secret: your-secret-key-change-in-production
    expiration: 3600  # 1 hour in seconds
    refresh-token-expiration: 604800  # 7 days in seconds
```

#### API Gateway (application.yml)

```yaml
server:
  port: 8000

spring:
  application:
    name: api-gateway
  cloud:
    gateway:
      routes:
        - id: order-service-route
          uri: lb://order-service
          predicates:
            - Path=/api/v1/orders/**
        - id: settlement-service-route
          uri: lb://settlement-service
          predicates:
            - Path=/api/v1/settlements/**
        - id: reconciliation-service-route
          uri: lb://reconciliation-service
          predicates:
            - Path=/api/v1/reconciliation/**
        - id: user-service-route
          uri: lb://user-service
          predicates:
            - Path=/api/v1/users/**
        - id: auth-service-route
          uri: lb://user-service
          predicates:
            - Path=/auth/**
      default-filters:
        - name: RequestRateLimiter
          args:
            redis-rate-limiter:
              replenish-rate: 100  # 100 requests per second
              burst-capacity: 200  # Allow burst of 200

# CORS configuration
cors:
  allowed-origins:
    - http://localhost:3000
    - http://localhost:4200
    - http://localhost:8081
    - http://localhost:8888
  allowed-methods:
    - GET
    - POST
    - PUT
    - DELETE
    - PATCH
    - OPTIONS
  allowed-headers:
    - Content-Type
    - Authorization
    - Accept
    - Cache-Control
```

### Environment Variables

For Docker Compose, configuration is set via environment variables:

```yaml
# docker-compose.yml
environment:
  - SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/order_db
  - SPRING_DATASOURCE_USERNAME=postgres
  - SPRING_DATASOURCE_PASSWORD=postgres
  - SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:9092
  - SPRING_CLOUD_CONSUL_HOST=consul
  - SPRING_CLOUD_CONSUL_PORT=8500
```

### Production Configuration

For production deployment, override these settings:

```bash
# Via environment variables
export SPRING_DATASOURCE_PASSWORD=your-production-password
export SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka-prod.example.com:9092
export SPRING_CLOUD_CONSUL_HOST=consul-prod.example.com

# Via application-prod.yml
spring:
  profiles:
    active: prod

---
spring:
  config:
    activate:
      on-profile: prod
  
  datasource:
    url: jdbc:postgresql://postgres-prod.example.com:5432/order_db
    username: postgres
    password: ${DB_PASSWORD}  # From secrets
  
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS}
```

### Logging Configuration

Logging is configured in `application.yml`:

```yaml
logging:
  level:
    root: INFO
    goldmansachs: DEBUG
    org.springframework.web: WARN
    org.springframework.kafka: INFO
    org.hibernate.SQL: DEBUG
  pattern:
    console: "%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
  file:
    name: logs/application.log
    max-size: 10MB
    max-history: 30
```

## Event Flow

1. **Order Creation**: User creates order via REST API
   - Order stored in database with PENDING status
   - OutboxEvent created and marked as PENDING
   
2. **Event Publishing**: OutboxPublisher polls for pending events
   - Events published to Kafka topics (order-created-events, order-status-changed-events)
   - OutboxEvent marked as PUBLISHED
   
3. **Settlement Processing**: OrderCreatedConsumer receives events
   - Settlement record created with PENDING status
   - 2% fee calculated and deducted
   
4. **Reconciliation**: Spring Batch job runs daily
   - Processes PROCESSING status settlements
   - Validates and updates settlement status to COMPLETED
   - Logs any failures for investigation

## Monitoring and Health Checks

Each service exposes actuator endpoints:
- Health: `http://localhost:{port}/actuator/health`
- Metrics: `http://localhost:{port}/actuator/metrics`
- Info: `http://localhost:{port}/actuator/info`

## Key Design Patterns

### 1. Transactional Outbox Pattern
Ensures reliable event publishing even in case of failures:
- Events stored in outbox table atomically with business data
- Separate publisher polls and publishes events to Kafka
- Prevents message loss and duplicate processing

### 2. Event-Driven Architecture
Services communicate via events:
- Order Service publishes order events
- Settlement Service consumes events and creates settlements
- Reconciliation Service validates settlements

### 3. Spring Batch for Bulk Processing
Efficient batch processing:
- Chunk-based processing
- Skip listeners for error handling
- Transaction management

### 4. Database Migrations with Flyway
Version-controlled schema changes:
- V1_init.sql - Order service schema
- V2_settlement.sql - Settlement service schema
- V3_reconciliation.sql - Reconciliation service schema

## End-to-End Testing

### Complete Testing Guide

For comprehensive end-to-end testing instructions, refer to [E2E_TESTING_GUIDE.md](E2E_TESTING_GUIDE.md)

### Quick Test Scenario

**Step 1: Create an Order**

```bash
# 1. Login to get token
TOKEN=$(curl -s -X POST http://localhost:8000/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "testpass123"
  }' | jq -r '.token')

# 2. Create order via gateway
curl -X POST http://localhost:8000/api/v1/orders \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "CUST001",
    "productId": "PROD001",
    "quantity": 5,
    "unitPrice": 100.00
  }'

# Response contains orderNumber, e.g., ORD-20241022-001
```

**Step 2: Verify Event Publishing (Wait 5-10 seconds)**

```bash
# Check that event was published
docker exec -it event-driven-postgres psql -U postgres -d order_db -c \
  "SELECT * FROM outbox_events WHERE status = 'PUBLISHED' ORDER BY created_at DESC LIMIT 1;"

# Should show event with status PUBLISHED
```

**Step 3: Verify Settlement Created**

```bash
# Check settlement record was created
docker exec -it event-driven-postgres psql -U postgres -d settlement_db -c \
  "SELECT settlement_id, order_id, settlement_amount, fee_amount, net_amount, status FROM settlement_records ORDER BY created_at DESC LIMIT 1;"

# Should show:
# settlement_id | order_id | settlement_amount | fee_amount | net_amount | status
#     1         |    1     |     500.00        |   10.00    |   490.00   | PENDING
```

**Step 4: Update Order Status**

```bash
# Change order status to DELIVERED
curl -X PATCH "http://localhost:8000/api/v1/orders/1/status" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"status": "DELIVERED"}'
```

**Step 5: Verify Status Event**

```bash
# Check settlement status updated
docker exec -it event-driven-postgres psql -U postgres -d settlement_db -c \
  "SELECT settlement_id, status FROM settlement_records WHERE order_id = 1 ORDER BY updated_at DESC LIMIT 1;"

# Should show status changed to PROCESSING
```

**Step 6: Monitor Logs**

```bash
# Watch all service logs
docker-compose logs -f

# In another terminal, monitor Kafka events
docker exec -it event-driven-kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic order-created-events \
  --from-beginning
```

## Monitoring and Health Checks

### Health Check Endpoints

All services expose health checks:

```bash
# Via Gateway (recommended)
curl http://localhost:8000/gateway/health

# Direct service health
curl http://localhost:8080/actuator/health  # Order Service
curl http://localhost:8081/actuator/health  # Settlement Service
curl http://localhost:8082/actuator/health  # Reconciliation Service
curl http://localhost:8083/actuator/health  # User Service
```

### Response Format

```json
{
  "status": "UP",
  "components": {
    "consul": { "status": "UP" },
    "db": { "status": "UP" },
    "diskSpace": { "status": "UP" },
    "kafkaListener": { "status": "UP" }
  }
}
```

### Service Registry Health

Monitor all services via Consul:

```bash
# List all services and their health
curl http://localhost:8500/v1/health/service/order-service | jq

# Expected output:
[
  {
    "Service": {
      "ID": "order-service:hostname:8080",
      "Service": "order-service",
      "Address": "172.20.0.3",
      "Port": 8080
    },
    "Checks": [
      {
        "CheckID": "service:order-service",
        "Status": "passing"
      }
    ]
  }
]
```

### Metrics

View system metrics:

```bash
# Gateway metrics
curl http://localhost:8000/gateway/metrics

# Specific metric (HTTP requests)
curl http://localhost:8000/gateway/metrics/http.server.requests

# Service metrics
curl http://localhost:8080/actuator/metrics
```

### Docker Compose Health Checks

The system includes health checks for all services:

```bash
# View health status
docker-compose ps

# Expected output:
CONTAINER ID   IMAGE                              STATUS
...postgres...                                     healthy (30s)
...kafka...                                        healthy (30s)
...consul...                                       healthy (30s)
...order-service...                                healthy (10s)
...settlement-service...                           healthy (10s)
```

### Logging

View service logs:

```bash
# All services
docker-compose logs -f

# Specific service
docker-compose logs -f order-service

# Last 50 lines
docker-compose logs --tail=50 settlement-service

# With timestamps
docker-compose logs --timestamps reconciliation-service

# Filter logs (e.g., errors only)
docker-compose logs order-service | grep -i error
```

## Troubleshooting

### 1. Services Not Starting

**Symptoms:** `docker-compose up` fails or services don't start

**Solutions:**

```bash
# Check logs for errors
docker-compose logs

# Verify Docker resources
docker system df

# Ensure ports are available
netstat -tuln | grep -E "8000|8080|8081|8082|8083|8500|9092|5432"

# Restart services
docker-compose down
docker-compose up -d
```

### 2. Cannot Connect to Gateway

**Symptoms:** `curl: (7) Failed to connect to localhost port 8000`

**Solutions:**

```bash
# Verify gateway is running
docker-compose ps api-gateway

# Check gateway logs
docker-compose logs api-gateway

# Verify port mapping
docker port event-driven-api-gateway | grep 8000

# Try direct service connection
curl http://localhost:8080/actuator/health  # Order Service direct
```

### 3. Services Not Registered with Consul

**Symptoms:** Services missing from Consul UI

**Solutions:**

```bash
# Check Consul is running and accessible
curl http://localhost:8500/v1/health/node/consul

# Verify service tried to register
docker-compose logs order-service | grep -i consul

# Check Docker network connectivity
docker-compose exec order-service curl http://consul:8500/v1/health/node/consul

# Restart service
docker-compose restart order-service
```

### 4. Events Not Being Published

**Symptoms:** Outbox events stay in PENDING status

**Solutions:**

```bash
# Check OutboxPublisher is running
docker-compose logs order-service | grep -i "outbox\|publisher"

# Verify Kafka is accessible from order service
docker-compose exec order-service curl http://kafka:9092

# Check pending events
docker exec -it event-driven-postgres psql -U postgres -d order_db -c \
  "SELECT * FROM outbox_events WHERE status = 'PENDING' LIMIT 10;"

# Check Kafka topics exist
docker exec event-driven-kafka kafka-topics \
  --list --bootstrap-server localhost:9092
```

### 5. Settlement Records Not Created

**Symptoms:** No settlement records appear after order creation

**Solutions:**

```bash
# Verify order was created
docker exec -it event-driven-postgres psql -U postgres -d order_db -c \
  "SELECT * FROM orders;"

# Check settlement service logs
docker-compose logs settlement-service | grep -i "created\|consuming\|error"

# Verify settlement database exists
docker exec -it event-driven-postgres psql -U postgres -l | grep settlement

# Check Kafka consumer group
docker exec event-driven-kafka kafka-consumer-groups \
  --list --bootstrap-server localhost:9092
```

### 6. Authentication Failures

**Symptoms:** `401 Unauthorized` errors

**Solutions:**

```bash
# Verify user service is running
docker-compose ps user-service

# Check user service logs
docker-compose logs user-service | tail -50

# Verify database has users table
docker exec -it event-driven-postgres psql -U postgres -d user_db -c \
  "SELECT * FROM users LIMIT 5;"

# Try login endpoint
curl -X POST http://localhost:8000/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"test123"}'
```

### 7. Rate Limiting (429 Too Many Requests)

**Symptoms:** Some requests fail with HTTP 429

**Causes:** Exceeded rate limit (100 req/sec, 200 burst)

**Solutions:**

```bash
# Add delay between requests
for i in {1..10}; do
  curl http://localhost:8000/api/v1/orders
  sleep 0.1  # 100ms delay
done

# Increase rate limit (for testing only)
# Edit api-gateway/src/main/resources/application.yml
# Change replenish-rate to higher value

# Temporary disable for testing
# Edit GatewayConfig.java, comment out rate limiting filter
```

### 8. Database Connection Errors

**Symptoms:** `org.postgresql.util.PSQLException: Connection refused`

**Solutions:**

```bash
# Verify PostgreSQL is running
docker-compose ps postgres

# Check PostgreSQL logs
docker-compose logs postgres | head -30

# Verify database credentials in application.yml
grep "datasource" order-service/src/main/resources/application.yml

# Check database initialization
docker exec -it event-driven-postgres psql -U postgres -l

# Restart PostgreSQL
docker-compose restart postgres
```

### 9. Kafka Connection Issues

**Symptoms:** `org.apache.kafka.common.errors.TimeoutException`

**Solutions:**

```bash
# Verify Kafka and Zookeeper are running
docker-compose ps kafka zookeeper

# Test Kafka connectivity
docker-compose exec kafka kafka-broker-api-versions \
  --bootstrap-server localhost:9092

# Check Kafka logs
docker-compose logs kafka | grep -i error

# Verify bootstrap servers configuration
grep "bootstrap-servers" order-service/src/main/resources/application.yml
```

### 10. Reconciliation Job Not Running

**Symptoms:** Reconciliation job never executes

**Solutions:**

```bash
# Check reconciliation service logs
docker-compose logs reconciliation-service | grep -i "scheduler\|cron\|job"

# Verify Spring Batch tables exist
docker exec -it event-driven-postgres psql -U postgres -d reconciliation_db -c \
  "\dt batch_*"

# Check job execution history
docker exec -it event-driven-postgres psql -U postgres -d reconciliation_db -c \
  "SELECT * FROM batch_job_execution ORDER BY end_time DESC LIMIT 5;"

# Trigger manual reconciliation (if enabled)
curl -X POST http://localhost:8000/api/v1/reconciliation/run \
  -H "Authorization: Bearer $TOKEN"
```

### 11. CORS Issues (Web Frontend)

**Symptoms:** `Cross-Origin Request Blocked` in browser console

**Solutions:**

```bash
# Test CORS headers
curl -X OPTIONS http://localhost:8000/api/v1/orders \
  -H "Origin: http://localhost:3000" \
  -v

# Should return:
# Access-Control-Allow-Origin: http://localhost:3000
# Access-Control-Allow-Methods: GET,POST,PUT,DELETE,PATCH

# Add your origin to allowed list:
# Edit api-gateway/src/main/java/goldmansachs/gateway/config/GatewayConfig.java
# corsConfiguration.addAllowedOrigin("http://your-domain:port");

# Restart gateway
docker-compose restart api-gateway
```

### 12. Getting Detailed Error Information

```bash
# Enable DEBUG logging
docker-compose exec order-service sh
vi /app/config/application.yml
# Change logging level to DEBUG

# View detailed health
curl http://localhost:8080/actuator/health/details \
  -H "Authorization: Bearer $TOKEN"

# Export service logs to file
docker-compose logs > all-services.log 2>&1

# Check individual service details
docker inspect event-driven-order-service | jq '.State.Status'
```

### Still Having Issues?

1. Check [IMPLEMENTATION_REPORT.md](IMPLEMENTATION_REPORT.md) for architecture details
2. Review [PRODUCTION_CHECKLIST.md](PRODUCTION_CHECKLIST.md) for setup verification
3. Consult [SERVICE_DISCOVERY.md](SERVICE_DISCOVERY.md) for service registry issues
4. See [API_GATEWAY.md](API_GATEWAY.md) for gateway-specific problems
5. Review [E2E_TESTING_GUIDE.md](E2E_TESTING_GUIDE.md) for testing procedures

## Performance Considerations

### Production Readiness Checklist

- [x] Database migrations automated with Flyway
- [x] Transactional outbox for reliable messaging
- [x] Error handling and skip listeners
- [x] Health checks and metrics exposure
- [x] Scheduled batch reconciliation
- [x] Database indexes for query optimization
- [x] Connection pooling configured (HikariCP)
- [x] Logging configured at appropriate levels
- [x] Service discovery with automatic load balancing
- [x] API Gateway with rate limiting
- [x] CORS configuration for web frontends
- [x] Spring Security authentication framework

### Optimization Tips

**Database:**
- Indexes are already configured on frequently queried columns
- Connection pool: HikariCP defaults (20 max connections)
- Batch processing: Chunk size of 100 records (configurable)

**Kafka:**
- Concurrency: 3 consumer threads (configurable)
- Auto-commit: Disabled for reliability (manual ACK)
- Max poll records: 100 (configurable)

**Application:**
- Stateless microservices (horizontally scalable)
- Non-blocking I/O via Spring WebFlux (Gateway)
- Reactive streams support

### Scaling Strategies

```
Horizontal Scaling (More Instances):
┌──────────────────────────────────────┐
│  API Gateway (Port 8000)             │
│  - Stateless, fully horizontally     │
│    scalable                          │
│  - Run multiple instances behind     │
│    load balancer                     │
│  - Consul automatically load-balances│
└───────┬──────────────────────────────┘
        │
        ├─→ Order Service (Port 8080)
        │   - Run multiple instances
        │   - Consul auto-discovers
        │   - Ribbon load-balances
        │
        ├─→ Settlement Service (Port 8081)
        │   - Kafka parallelism
        │   - Consumer group scaling
        │
        └─→ Reconciliation Service
            - Batch job scaling
            - Multiple job instances
```

## Production Deployment

### Deployment Checklist

Refer to [PRODUCTION_CHECKLIST.md](PRODUCTION_CHECKLIST.md) for comprehensive production readiness verification.

### Docker Compose for Development

Currently using Docker Compose for development. For production:

```bash
# Development (current)
docker-compose up -d

# Production (recommended)
# 1. Replace Docker Compose with Kubernetes
# 2. Use managed databases (RDS, Cloud SQL)
# 3. Use managed message broker (Managed Kafka, AWS MSK)
# 4. Use managed service discovery (Kubernetes DNS)
```

### Kubernetes Deployment (Future)

Planned migration to Kubernetes includes:
- StatefulSets for databases
- Deployments for microservices
- ConfigMaps for configuration
- Secrets for credentials
- Services for networking
- Ingress for external routing

### Environment-Specific Configuration

**Development:**
```yaml
spring:
  profiles:
    active: dev
  jpa:
    hibernate:
      ddl-auto: create-drop
  h2:
    console:
      enabled: true
```

**Production:**
```yaml
spring:
  profiles:
    active: prod
  jpa:
    hibernate:
      ddl-auto: validate
  datasource:
    password: ${DB_PASSWORD}  # From secrets
```

### Security Considerations

**Current Security:**
- Spring Security with authentication framework
- JWT tokens (1 hour expiration)
- CORS configuration
- Input validation

**Production Enhancements Needed:**
- [ ] TLS/SSL encryption for all services
- [ ] API key management
- [ ] Rate limiting per user/API key
- [ ] OAuth2/OpenID Connect integration
- [ ] Audit logging for compliance
- [ ] Secrets management (HashiCorp Vault, AWS Secrets Manager)
- [ ] Service-to-service mTLS
- [ ] DDoS protection

### Monitoring & Observability

**Currently Implemented:**
- Spring Actuator endpoints (health, metrics, info)
- Structured logging with SLF4J
- Consul UI for service health

**Production Enhancements Recommended:**
- [ ] Centralized logging (ELK Stack, Datadog, Splunk)
- [ ] Metrics collection (Prometheus)
- [ ] Distributed tracing (Jaeger, Zipkin)
- [ ] Alerting system (PagerDuty, Opsgenie)
- [ ] Performance monitoring (New Relic, Datadog APM)
- [ ] Log aggregation across all services

### Disaster Recovery

**Backup Strategy:**
```bash
# Backup PostgreSQL databases
docker exec event-driven-postgres pg_dump -U postgres \
  --all-databases > backup_$(date +%Y%m%d_%H%M%S).sql

# Restore from backup
docker exec -i event-driven-postgres psql -U postgres \
  < backup_20241022_120000.sql
```

**Disaster Recovery Plan:**
- [ ] Regular database backups (daily)
- [ ] Test backup restoration procedure
- [ ] Event log retention and replay capability
- [ ] Service failover procedures
- [ ] Communication plan for outages
- [ ] Recovery time objective (RTO): < 1 hour
- [ ] Recovery point objective (RPO): < 5 minutes

### Load Testing

```bash
# Using Apache JMeter or similar tool
# Test scenarios:
# 1. Create orders concurrently
# 2. Retrieve orders under load
# 3. Settlement processing throughput
# 4. Reconciliation batch performance

# Simple load test with Apache Bench:
ab -n 1000 -c 10 http://localhost:8000/api/v1/orders

# Watch metrics during load test
docker-compose exec api-gateway curl http://localhost:8000/gateway/metrics
```

## Contributing

When contributing to this project:

1. **Follow Spring Boot best practices**
   - Use dependency injection
   - Leverage Spring annotations
   - Follow MVC pattern

2. **Include database migrations**
   - Create new V*.sql files in `src/main/resources/db/migration`
   - Version sequentially (V1_init.sql, V2_*.sql, etc.)
   - Test migrations locally

3. **Add appropriate logging**
   - Use SLF4J (not System.out.println)
   - Log at appropriate levels (DEBUG, INFO, WARN, ERROR)
   - Avoid logging sensitive information

4. **Test event flows**
   - Create orders and verify event publishing
   - Verify event consumption
   - Test error scenarios

5. **Update documentation**
   - Update README.md for new features
   - Add API endpoint examples
   - Document configuration options
   - Update this file with significant changes

6. **Code style**
   - Use consistent indentation (4 spaces)
   - Follow Google Java Style Guide
   - Use meaningful variable names
   - Keep methods focused and concise

## Support & Resources

### Documentation
- [IMPLEMENTATION_REPORT.md](IMPLEMENTATION_REPORT.md) - Complete implementation details
- [PRODUCTION_CHECKLIST.md](PRODUCTION_CHECKLIST.md) - Production readiness verification
- [SERVICE_DISCOVERY.md](SERVICE_DISCOVERY.md) - Consul service discovery guide
- [API_GATEWAY.md](API_GATEWAY.md) - API Gateway comprehensive documentation
- [E2E_TESTING_GUIDE.md](E2E_TESTING_GUIDE.md) - End-to-end testing procedures
- [PROJECT_SUMMARY.md](PROJECT_SUMMARY.md) - High-level project overview
- [USER_SERVICE_SUMMARY.md](USER_SERVICE_SUMMARY.md) - User service details

### Quick Links

| Resource | URL | Purpose |
|----------|-----|---------|
| **API Gateway** | http://localhost:8000 | Single entry point for all requests |
| **Consul UI** | http://localhost:8500/ui | Service registry and health monitoring |
| **Order Service** | http://localhost:8080 | Create and manage orders |
| **Settlement Service** | http://localhost:8081 | Manage financial settlements |
| **Reconciliation Service** | http://localhost:8082 | Batch reconciliation jobs |
| **User Service** | http://localhost:8083 | Authentication and user management |

### Technologies Used

| Component | Technology | Version |
|-----------|-----------|---------|
| **Framework** | Spring Boot | 3.2.5 |
| **Build Tool** | Maven | 3.8+ |
| **Java** | OpenJDK | 17+ |
| **Database** | PostgreSQL | 15+ |
| **Message Broker** | Apache Kafka | 7.5.0 |
| **Service Discovery** | Consul | Latest |
| **API Gateway** | Spring Cloud Gateway | Latest |
| **Batch Processing** | Spring Batch | 5.x |
| **ORM** | Hibernate | 6.x |
| **Container** | Docker & Docker Compose | Latest |

### Community & Support

- For bug reports, create an issue in the project repository
- For questions, check existing documentation first
- Refer to Spring Boot documentation: https://spring.io/projects/spring-boot
- Apache Kafka documentation: https://kafka.apache.org/documentation
- Consul documentation: https://www.consul.io/docs
- PostgreSQL documentation: https://www.postgresql.org/docs

## License

This project is provided as-is for educational and demonstration purposes.

---

**Last Updated:** October 2026  
**Project Status:** ✅ Production Ready  
**Total Services:** 5 Microservices + API Gateway + Service Discovery  
**Database Schemas:** 3 Independent Databases (Order, Settlement, Reconciliation)  
**API Endpoints:** 25+ RESTful endpoints  
**Test Coverage:** See E2E_TESTING_GUIDE.md
