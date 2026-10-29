# API Gateway Implementation Guide

## Overview

The API Gateway is the single entry point for all external client requests to the Event-Driven Order Pipeline microservices. Built with **Spring Cloud Gateway** and integrated with **Consul service discovery**, it provides intelligent request routing, load balancing, and service health monitoring.

## Table of Contents

1. [What Is the API Gateway?](#what-is-the-api-gateway)
2. [Architecture](#architecture)
3. [Quick Start](#quick-start)
4. [Routes & Routing](#routes--routing)
5. [Service Discovery Integration](#service-discovery-integration)
6. [CORS Configuration](#cors-configuration)
7. [Rate Limiting](#rate-limiting)
8. [Monitoring & Debugging](#monitoring--debugging)
9. [Common Use Cases](#common-use-cases)
10. [Troubleshooting](#troubleshooting)
11. [Future Enhancements](#future-enhancements)

---

## What Is the API Gateway?

### Purpose

The API Gateway eliminates the need for external clients to know about individual microservices. Instead of making direct requests to services on ports 8080-8083, clients make all requests to a single gateway endpoint on port 8000.

### Key Responsibilities

1. **Request Routing**: Route incoming requests to appropriate microservices based on URL paths
2. **Service Discovery**: Query Consul to find available service instances dynamically
3. **Load Balancing**: Distribute requests across multiple instances of a service
4. **Health Monitoring**: Ensure only healthy service instances receive traffic
5. **CORS Handling**: Enable cross-origin requests from web applications
6. **Centralized Control**: Single point for adding authentication, rate limiting, etc.

### Why Use an API Gateway?

| Without Gateway | With Gateway |
|---|---|
| Clients hardcode service URLs | Clients use single gateway URL |
| Clients handle load balancing | Gateway handles load balancing |
| Clients need service discovery logic | Gateway abstracts service discovery |
| Difficult to add cross-cutting concerns | Easy to add authentication, rate limiting |
| CORS issues for web frontends | Centralized CORS configuration |

---

## Architecture

### System Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                        External Clients                         │
│                                                                 │
│  Web Browser (React, Angular, Vue)                             │
│  Mobile App (iOS, Android)                                     │
│  Desktop Application                                           │
│  Third-party Integration                                       │
└────────────────────────────┬────────────────────────────────────┘
                             │
                   HTTP/HTTPS port 8000
                             │
                             ▼
           ┌──────────────────────────────────────┐
           │     Spring Cloud Gateway             │
           │     (api-gateway service)            │
           │                                      │
           │  Port: 8000                          │
           │  - Request Routing                   │
           │  - Service Discovery Integration    │
           │  - Load Balancing                    │
           │  - Health Checks                     │
           │  - CORS Support                      │
           └──────────────────┬───────────────────┘
                              │
              ┌───────────────┼───────────────┬──────────────┐
              │               │               │              │
              ▼               ▼               ▼              ▼
        ┌──────────┐   ┌─────────────┐  ┌──────────┐  ┌─────────┐
        │  Order   │   │ Settlement  │  │Reconcil- │  │  User   │
        │ Service  │   │  Service    │  │iation    │  │ Service │
        │ (8080)   │   │  (8081)     │  │(8082)    │  │ (8083)  │
        └──────────┘   └─────────────┘  └──────────┘  └─────────┘
              │               │               │              │
              └───────────────┼───────────────┴──────────────┘
                              │
                    Service Registry Consul
                    (Service Discovery)
                    Port: 8500
                    - Service Registration
                    - Health Checks
                    - Service Lookup
```

### Request Flow

```
1. Client Request
   └─> GET http://localhost:8000/api/v1/orders/123

2. Gateway Receives Request
   └─> Matches against routes in application.yml
       └─> Route: Path=/api/v1/orders/** → order-service

3. Ribbon Load Balancer Activated
   └─> Query Consul: "Give me instances of order-service"
       └─> Consul Response: 
           [
             {
               "ID": "order-service:hostname:8080",
               "Address": "172.20.0.3",
               "Port": 8080,
               "Status": "passing"
             }
           ]

4. Route to Selected Instance
   └─> Send: GET http://172.20.0.3:8080/api/v1/orders/123

5. Service Processes Request
   └─> Order Service handles request
       └─> Returns response (200 OK, JSON body, etc.)

6. Gateway Receives Response
   └─> Apply response filters (add headers, etc.)
       └─> Return response to client

7. Client Receives Response
   └─> HTTP 200 OK with order data
```

### Technology Stack

| Component | Technology | Purpose |
|-----------|-----------|---------|
| Gateway Framework | Spring Cloud Gateway | Non-blocking, reactive API gateway |
| Service Discovery | Consul | Service registration & discovery |
| Load Balancer | Ribbon | Client-side load balancing |
| HTTP Framework | Spring WebFlux | Reactive web framework |
| Runtime | Java 17 | Language runtime |
| Container | Docker | Containerization |

---

## Quick Start

### 1. Start the Complete Stack

```bash
# From project root directory
docker-compose up -d

# Wait for all services to be healthy (30-60 seconds)
docker-compose ps

# Expected output:
# NAME                                STATUS
# event-driven-api-gateway            Up (healthy)
# event-driven-order-service          Up (healthy)
# event-driven-settlement-service     Up (healthy)
# event-driven-reconciliation-service Up (healthy)
# event-driven-user-service           Up (healthy)
# event-driven-consul                 Up (healthy)
# event-driven-kafka                  Up (healthy)
# event-driven-postgres               Up (healthy)
```

### 2. Verify Gateway Is Running

```bash
# Check gateway health
curl http://localhost:8000/actuator/health

# Expected response:
# {"status":"UP","components":{"consul":{"status":"UP"},...}}
```

### 3. View Available Routes

```bash
# See all configured routes in gateway
curl http://localhost:8000/actuator/gateway/routes | jq

# Expected output (abbreviated):
# [
#   {
#     "route_id": "order-service-route",
#     "route_object": {"id":"order-service-route",...},
#     "order": 0
#   },
#   ...
# ]
```

### 4. Test Routing

```bash
# Create an order through the gateway
curl -X POST http://localhost:8000/api/v1/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "CUST001",
    "productId": "PROD001",
    "quantity": 5,
    "unitPrice": 100.00
  }'

# Response (routed to order-service):
# {
#   "id": 1,
#   "orderNumber": "ORD-2024-001",
#   "customerId": "CUST001",
#   "status": "PENDING",
#   ...
# }
```

### 5. Access Consul UI

Open browser: **http://localhost:8500/ui/**

You'll see:
- All registered services (including api-gateway)
- Service health status
- Service instances with IP addresses
- Health check history

---

## Routes & Routing

### Configured Routes

The gateway is pre-configured with 6 routes to handle different request patterns:

#### Route 1: Order Service

```yaml
Route ID: order-service-route
Pattern: /api/v1/orders/**
Destination: lb://order-service (port 8080)
Methods: ALL (GET, POST, PUT, DELETE, etc.)

Examples:
- POST   http://localhost:8000/api/v1/orders
  ↓ Routes to ↓
  POST   http://order-service:8080/api/v1/orders

- GET    http://localhost:8000/api/v1/orders/123
  ↓ Routes to ↓
  GET    http://order-service:8080/api/v1/orders/123

- PATCH  http://localhost:8000/api/v1/orders/123/status?status=SHIPPED
  ↓ Routes to ↓
  PATCH  http://order-service:8080/api/v1/orders/123/status?status=SHIPPED
```

#### Route 2: Settlement Service

```yaml
Route ID: settlement-service-route
Pattern: /api/v1/settlements/**
Destination: lb://settlement-service (port 8081)

Examples:
- GET    http://localhost:8000/api/v1/settlements
- GET    http://localhost:8000/api/v1/settlements/123
```

#### Route 3: Reconciliation Service

```yaml
Route ID: reconciliation-service-route
Pattern: /api/v1/reconciliation/**
Destination: lb://reconciliation-service (port 8082)

Examples:
- GET    http://localhost:8000/api/v1/reconciliation/status
- POST   http://localhost:8000/api/v1/reconciliation/run
```

#### Route 4: User Service (API)

```yaml
Route ID: user-service-route
Pattern: /api/v1/users/**
Destination: lb://user-service (port 8083)

Examples:
- GET    http://localhost:8000/api/v1/users
- GET    http://localhost:8000/api/v1/users/123
- PUT    http://localhost:8000/api/v1/users/123
- DELETE http://localhost:8000/api/v1/users/123
```

#### Route 5: User Service (Authentication)

```yaml
Route ID: auth-service-route
Pattern: /auth/**
Destination: lb://user-service (port 8083)

Examples:
- POST   http://localhost:8000/auth/login
- POST   http://localhost:8000/auth/logout
- POST   http://localhost:8000/auth/refresh
- GET    http://localhost:8000/auth/validate
```

#### Route 6: Gateway Actuator (Diagnostics)

```yaml
Route ID: gateway-actuator-route
Patterns: /gateway/health, /gateway/metrics, /gateway/info
Destination: Gateway itself (http://localhost:8000)

Examples:
- GET    http://localhost:8000/gateway/health
  → Rewritten to http://localhost:8000/actuator/health
- GET    http://localhost:8000/gateway/metrics
  → Rewritten to http://localhost:8000/actuator/metrics
```

### How Routing Works

1. **Request arrives at gateway** on port 8000
2. **Route matching**: Gateway checks request path against all routes
3. **Predicate matching**: First matching route is selected
4. **Filter chain**: Request filters are applied
5. **Service resolution**: Ribbon queries Consul for service instances
6. **Load balancing**: Ribbon selects an instance (round-robin)
7. **Request forwarding**: Request sent to selected instance
8. **Response filters**: Response filters are applied
9. **Response returned**: Response sent back to client

### Adding New Routes

To add a new route, edit `api-gateway/src/main/resources/application.yml`:

```yaml
spring:
  cloud:
    gateway:
      routes:
        # Add new route here
        - id: my-new-service-route
          uri: lb://my-new-service
          predicates:
            - Path=/api/v1/my-service/**
          filters:
            - name: PreserveHostHeader
```

Then restart the gateway:
```bash
docker-compose restart api-gateway
```

---

## Service Discovery Integration

### How Gateway Discovers Services

```
1. Gateway Starts
   ↓
2. Connects to Consul (SPRING_CLOUD_CONSUL_HOST: consul)
   ↓
3. Registers itself as "api-gateway" service
   ↓
4. Watches for other services registering
   ↓
5. When client makes request to /api/v1/orders/**
   ↓
6. Gateway queries Consul: "Give me instances of order-service"
   ↓
7. Consul returns: [
     {
       "ID": "order-service:hostname:8080",
       "Address": "172.20.0.3",
       "Port": 8080,
       "Status": "passing"
     }
   ]
   ↓
8. Ribbon load balances across returned instances
   ↓
9. Request routed to selected instance
```

### Service Health Monitoring

```
Consul continuously monitors service health:

Every 10 seconds:
  Consul → GET http://service:port/actuator/health
  
Service Response Examples:

✓ Healthy (Status: UP)
  └─ Consul marks as "passing"
  └─ Ribbon routes traffic here

✗ Unhealthy (Status: DOWN)
  └─ Consul marks as "failing"
  └─ Ribbon excludes from routing
  └─ Clients get 503 Service Unavailable if no healthy instances
```

### Multiple Service Instances

When multiple instances are available:

```
Request Flow (Multiple Instances):

Client Request
  ↓
Gateway queries Consul: "Give me instances of order-service"
  ↓
Consul returns:
  Instance 1: 172.20.0.3:8080 (healthy)
  Instance 2: 172.20.0.4:8080 (healthy)
  Instance 3: 172.20.0.5:8080 (unhealthy - excluded)
  ↓
Ribbon load balances across healthy instances:
  Request 1 → Instance 1
  Request 2 → Instance 2
  Request 3 → Instance 1 (round-robin)
  Request 4 → Instance 2
  ...
```

### Configuration

```yaml
spring:
  cloud:
    consul:
      # Consul connection
      host: consul          # For Docker: "consul", for local: "localhost"
      port: 8500
      
      discovery:
        # Service registration
        enabled: true       # Enable service discovery
        instance-id: api-gateway:hostname:8000
        
        # Health checks
        register-health-check: true
        health-check-interval: 10s
        health-check-timeout: 5s
        health-check-path: /actuator/health
```

---

## CORS Configuration

### What Is CORS?

CORS (Cross-Origin Resource Sharing) allows web applications running on one domain to make requests to APIs on a different domain.

### Why It Matters

```
Scenario 1: Web Frontend on Port 3000
┌──────────────────────────┐
│ Frontend (React)         │
│ http://localhost:3000    │
└──────────────────────────┘
           │
           │ Requests to:
           ▼
┌──────────────────────────┐
│ API Gateway              │
│ http://localhost:8000    │  ← Different port!
└──────────────────────────┘

Without CORS: Browser blocks request (same-origin policy)
With CORS: Browser allows request (CORS configured)

Scenario 2: Mobile App
Mobile app uses HTTP client to call API
CORS doesn't apply (CORS is browser security feature)
```

### Configured Origins

The gateway allows requests from:

```
Development:
- http://localhost:3000      (React development)
- http://localhost:4200      (Angular development)
- http://localhost:8081      (Vue development)
- http://localhost:8888      (Other development)

Future: Production domains
- https://app.example.com
- https://example.com
```

### Allowed Methods

```
GET     - Retrieve resources
POST    - Create resources
PUT     - Replace resources
DELETE  - Delete resources
PATCH   - Partial updates
OPTIONS - CORS preflight
```

### Allowed Headers

```
Request Headers:
- Content-Type     (JSON, form data, etc.)
- Authorization    (Bearer tokens, API keys)
- Accept          (Response format)
- Cache-Control   (Cache directives)

Response Headers (exposed to client):
- Content-Type
- X-Request-ID         (Tracing)
- X-Total-Count        (Pagination)
- X-RateLimit-Limit    (Rate limit info)
- X-RateLimit-Remaining
- X-RateLimit-Reset
```

### CORS Flow

```
1. Browser detects cross-origin request
   ↓
2. Browser sends OPTIONS preflight request
   OPTIONS /api/v1/orders
   Origin: http://localhost:3000
   Access-Control-Request-Method: POST
   ↓
3. Gateway responds with CORS headers
   Access-Control-Allow-Origin: http://localhost:3000
   Access-Control-Allow-Methods: GET, POST, PUT, DELETE, PATCH
   Access-Control-Allow-Headers: Content-Type, Authorization
   Access-Control-Max-Age: 3600
   ↓
4. Browser caches response for 1 hour
   ↓
5. Browser allows actual request
   POST /api/v1/orders
   [Request proceeds]
```

### Testing CORS

```bash
# Test CORS with curl
curl -X OPTIONS http://localhost:8000/api/v1/orders \
  -H "Origin: http://localhost:3000" \
  -H "Access-Control-Request-Method: POST" \
  -v

# Look for these headers in response:
# < Access-Control-Allow-Origin: http://localhost:3000
# < Access-Control-Allow-Methods: GET,POST,PUT,DELETE,PATCH,OPTIONS
# < Access-Control-Max-Age: 3600
```

### Configuring CORS for Production

Edit `api-gateway/src/main/java/goldmansachs/gateway/config/GatewayConfig.java`:

```java
CorsConfiguration corsConfiguration = new CorsConfiguration();

// Add production origins
corsConfiguration.addAllowedOrigin("https://example.com");
corsConfiguration.addAllowedOrigin("https://app.example.com");

// Remove development origins for production
// corsConfiguration.addAllowedOrigin("http://localhost:3000");
```

---

## Rate Limiting

### Purpose

Rate limiting protects services from overload by restricting the number of requests allowed per time period.

### Current Configuration

```
All routes are rate limited to:
- Replenish Rate: 100 requests/second (steady-state throughput)
- Burst Capacity: 200 requests (allows temporary spikes)

Example Timeline:
- Time 0s: Bucket has 200 tokens
- Time 0.1s: 10 requests, 190 tokens remaining
- Time 1s: Bucket refilled to 200 tokens
```

### Token Bucket Algorithm

```
How it works:

1. Each route has a bucket of tokens
   Capacity: 200 tokens (burst capacity)

2. Each request consumes 1 token

3. Bucket refills at a steady rate
   Replenish Rate: 100 tokens/second

4. If bucket is empty, request is rejected
   Status Code: 429 (Too Many Requests)

Example:
- Time 0.0s: 200 tokens (full)
- 200 requests at 0.1s: 0 tokens
- Time 0.1s to 1.0s: Replenished with 90 tokens (100/sec * 0.9s)
- Result: Can handle 100 requests from 0.1s to 1.0s
```

### Testing Rate Limiting

```bash
# Send 300 requests rapidly (exceeds 200 burst capacity)
for i in {1..300}; do
  curl http://localhost:8000/api/v1/orders 2>/dev/null
done

# First 200 succeed (within burst capacity)
# Requests 201-300 fail with 429 Too Many Requests

# After 1 second (bucket refilled):
# Can make 100 more requests
```

### Future Enhancements

Current: Path-based rate limiting (all users share the same limit)

Planned:
```
Per-User Rate Limiting:
- Different limits for different users
- API key based limits
- Tier-based limits (free, premium, enterprise)

Example:
- Free users: 100 requests/hour
- Premium users: 10,000 requests/hour
- Enterprise: Unlimited
```

---

## Monitoring & Debugging

### Gateway Health Endpoints

```bash
# Overall health status
GET http://localhost:8000/actuator/health

# Detailed health status (requires authorization)
GET http://localhost:8000/actuator/health/detailed

# Component health (database, Kafka, Consul, etc.)
# Response includes status of all backends
```

### Gateway Metrics

```bash
# View all available metrics
GET http://localhost:8000/actuator/metrics

# View specific metric (HTTP requests)
GET http://localhost:8000/actuator/metrics/http.server.requests

# View gateway-specific metrics
- gateway.requests
- gateway.routes
- gateway.requests.total
```

### Gateway Routes

```bash
# List all configured routes
GET http://localhost:8000/actuator/gateway/routes

# Response (abbreviated):
[
  {
    "route_id": "order-service-route",
    "route_object": {
      "id": "order-service-route",
      "uri": "lb://order-service",
      "predicates": [...],
      "filters": [...]
    },
    "order": 0
  },
  ...
]
```

### Consul UI

Access: **http://localhost:8500/ui/**

**Services Tab:**
- See all registered services
- View instance status
- Monitor health checks
- See service metadata

**Key/Value Tab:**
- View configuration stored in Consul
- Used for future config management

### Viewing Logs

```bash
# View gateway logs
docker-compose logs api-gateway

# Follow logs in real-time
docker-compose logs -f api-gateway

# View logs for specific timeframe
docker-compose logs api-gateway --tail=100

# View logs with timestamps
docker-compose logs api-gateway --timestamps
```

### Debugging Routing Issues

```bash
# Test routing with verbose output
curl -v http://localhost:8000/api/v1/orders

# Check response headers
# Look for:
# - X-Gateway-Header: API-Gateway-1.0
# - X-Forwarded-*: Original request info

# Trace request through logs
# Look for:
# - Route selection
# - Service discovery query
# - Request forwarding
# - Response filtering
```

### Common Log Messages

```
✓ Successful startup:
  "Gateway Started Successfully"
  "Service registered with Consul"

✓ Successful routing:
  "Routing to: lb://order-service"
  "Selected instance: 172.20.0.3:8080"

✗ Issues:
  "Cannot connect to Consul"
  "Service not found in Consul"
  "All instances unhealthy"
  "Request timed out"
```

---

## Common Use Cases

### 1. Simple REST API Call

```javascript
// Frontend JavaScript code
const response = await fetch('http://localhost:8000/api/v1/orders', {
  method: 'GET',
  headers: {
    'Content-Type': 'application/json'
  }
});

const orders = await response.json();
console.log(orders);

// Gateway routes to order-service
// Response: [{ id: 1, orderNumber: "ORD-001", ... }, ...]
```

### 2. Create Order

```bash
curl -X POST http://localhost:8000/api/v1/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "CUST001",
    "productId": "PROD001",
    "quantity": 5,
    "unitPrice": 100.00
  }'

# Gateway routes to order-service
# Response: { id: 1, orderNumber: "ORD-001", status: "PENDING", ... }
```

### 3. Get Settlement Details

```bash
curl http://localhost:8000/api/v1/settlements/123

# Gateway routes to settlement-service
# Response: { id: 123, amount: 450.00, status: "COMPLETED", ... }
```

### 4. Authenticate and Get User

```javascript
// Login
const loginResponse = await fetch('http://localhost:8000/auth/login', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ email: 'user@example.com', password: 'pass123' })
});

const { token } = await loginResponse.json();

// Get user with token
const userResponse = await fetch('http://localhost:8000/api/v1/users/me', {
  headers: { 'Authorization': `Bearer ${token}` }
});

const user = await userResponse.json();

// Gateway routes /auth/* to user-service
// Gateway routes /api/v1/users/* to user-service
```

### 5. Access Gateway Diagnostics

```bash
# Check gateway health
curl http://localhost:8000/gateway/health

# View all routes
curl http://localhost:8000/gateway/metrics

# Monitor metrics
curl http://localhost:8000/gateway/info
```

---

## Troubleshooting

### Issue 1: "Cannot Connect to Gateway"

**Error:**
```
curl: (7) Failed to connect to localhost port 8000: Connection refused
```

**Solutions:**

1. **Verify gateway is running:**
   ```bash
   docker-compose ps api-gateway
   ```
   Expected: "Up"

2. **Start gateway if not running:**
   ```bash
   docker-compose up -d api-gateway
   ```

3. **Check gateway logs for errors:**
   ```bash
   docker-compose logs api-gateway
   ```

4. **Verify port 8000 is not in use:**
   ```bash
   # Windows: netstat -ano | findstr :8000
   # Mac/Linux: lsof -i :8000
   ```

### Issue 2: "502 Bad Gateway - Service Unavailable"

**Error:**
```
HTTP 502: Bad Gateway
"Cannot find a matching service in Consul"
```

**Solutions:**

1. **Verify backend service is running:**
   ```bash
   docker-compose ps order-service
   ```
   Expected: "Up (healthy)"

2. **Check service is registered with Consul:**
   ```bash
   curl http://localhost:8500/v1/catalog/services | jq
   ```
   Expected: order-service in list

3. **Verify service health:**
   ```bash
   curl http://localhost:8500/v1/health/service/order-service | jq
   ```
   Expected: "Status": "passing"

4. **Check Consul connectivity:**
   ```bash
   docker-compose exec api-gateway curl http://consul:8500/v1/health/node/consul
   ```

### Issue 3: "CORS Policy: No 'Access-Control-Allow-Origin' Header"

**Error:**
```
Cross-Origin Request Blocked: The Same Origin Policy disallows
reading the remote resource at http://localhost:8000/api/v1/orders
```

**Solutions:**

1. **Verify frontend origin is allowed:**
   - Check `GatewayConfig.java` for allowed origins
   - Add your origin if missing

2. **Test CORS preflight:**
   ```bash
   curl -X OPTIONS http://localhost:8000/api/v1/orders \
     -H "Origin: http://localhost:3000" \
     -v
   ```
   Expected: `Access-Control-Allow-Origin: http://localhost:3000`

3. **Verify credentials are included:**
   ```javascript
   fetch('http://localhost:8000/api/v1/orders', {
     credentials: 'include',  // Include cookies/auth headers
     headers: {
       'Content-Type': 'application/json',
       'Authorization': 'Bearer token...'
     }
   });
   ```

### Issue 4: "429 Too Many Requests"

**Error:**
```
HTTP 429: Too Many Requests
"Rate limit exceeded"
```

**Explanation:**
You've exceeded the rate limit (100 requests/second, 200 burst).

**Solutions:**

1. **Wait for bucket to refill:**
   - Bucket refills at 100 requests/second
   - After 1 second, can make 100 more requests

2. **Reduce request frequency:**
   ```javascript
   // Add delay between requests
   for (let i = 0; i < 10; i++) {
     await fetch('http://localhost:8000/api/v1/orders');
     await new Promise(resolve => setTimeout(resolve, 100)); // 100ms delay
   }
   ```

3. **For load testing:**
   - Temporarily increase rate limit in `application.yml`
   - Or disable rate limiting for testing

### Issue 5: "Service Route Not Found"

**Error:**
```
HTTP 404: Not Found
"No route matched for this path"
```

**Solutions:**

1. **Verify route path is correct:**
   ```bash
   # Correct path
   GET /api/v1/orders

   # Incorrect paths
   GET /orders            ✗ (missing /api/v1/)
   GET /api/orders        ✗ (missing /v1/)
   GET /api/v1/order      ✗ (should be /orders)
   ```

2. **Check all available routes:**
   ```bash
   curl http://localhost:8000/actuator/gateway/routes | jq '.[] | .route_id'
   ```

3. **Ensure service is accessible:**
   - Route to service directly
   - Check service health

### Issue 6: "Connection Timeout"

**Error:**
```
curl: (28) Operation timed out after timeout milliseconds
```

**Solutions:**

1. **Verify backend service responds:**
   ```bash
   # Direct call to service
   docker-compose exec order-service curl http://localhost:8080/actuator/health
   ```

2. **Increase timeout settings:**
   ```yaml
   # In application.yml
   gateway:
     request-timeout: 60000  # 60 seconds
   ```

3. **Check service logs for errors:**
   ```bash
   docker-compose logs order-service | grep -i error
   ```

4. **Verify database connectivity:**
   ```bash
   docker-compose exec order-service curl http://localhost:8080/actuator/health/details
   ```

---

## Future Enhancements

### Phase 2: Authentication & Authorization

```yaml
Planned Features:
- JWT token validation for all routes
- Per-route authentication requirements
- Role-based access control (RBAC)
- API key authentication

Implementation:
- Custom authentication filter
- JWT token validation
- Authorization header processing
- Rate limiting per user/API key
```

### Phase 3: Observability

```yaml
Planned Features:
- Distributed request tracing (Jaeger/Zipkin)
- Request correlation IDs
- Metrics collection and export (Prometheus)
- Centralized logging (ELK Stack)
- Request/response logging

Implementation:
- Add X-Request-ID header to all requests
- Integration with observability platforms
- Custom metrics for gateway operations
- Log aggregation setup
```

### Phase 4: Advanced Features

```yaml
Planned Features:
- API versioning support
- Canary deployments (gradual rollout)
- Circuit breakers for resilience
- Request/response caching
- Request transformation
- Webhook support

Implementation:
- Version-aware routing
- Gradual traffic shifting
- Resilience4j integration
- Response caching
- Header/body transformation
- Event-driven webhook system
```

### Phase 5: Production Hardening

```yaml
Planned Features:
- TLS/SSL encryption
- Rate limiting per user
- Request validation
- Response compression
- Service contract validation

Implementation:
- HTTPS configuration
- Advanced rate limiting
- Schema validation
- Compression filters
- Contract testing
```

---

## Summary

The API Gateway provides a single entry point for all external client requests, with:

✅ **Automatic service discovery** via Consul  
✅ **Intelligent request routing** based on URL paths  
✅ **Load balancing** across multiple service instances  
✅ **Health monitoring** of backend services  
✅ **CORS support** for web frontends  
✅ **Rate limiting** for protecting services  
✅ **Easy monitoring** via health and metrics endpoints  
✅ **Non-blocking, reactive** architecture for high performance  

**For questions or issues, refer to the [Troubleshooting](#troubleshooting) section or check the project documentation.**
