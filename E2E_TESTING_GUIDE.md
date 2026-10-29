# End-to-End Testing Guide for Event-Driven Order Pipeline

## Overview
This guide walks you through running end-to-end tests of the microservices architecture. The pipeline consists of:
- **API Gateway** (Port 8000) - Routes requests to microservices
- **Order Service** (Port 8080) - Creates orders, publishes events
- **Settlement Service** (Port 8081) - Consumes orders, creates settlement records
- **Reconciliation Service** (Port 8082) - Batch reconciliation jobs
- **User Service** (Port 8083) - Authentication and user management
- **Kafka** (Port 9092) - Event streaming
- **PostgreSQL** (Port 5432) - Data persistence
- **Consul** (Port 8500) - Service discovery

## Prerequisites
- Docker & Docker Compose installed
- Maven installed (for building)
- `curl` or Postman for API testing
- Git Bash or PowerShell (Windows)

## Step 1: Build All Services

From the workspace root directory, run:

```bash
mvn clean package -DskipTests
```

This builds all modules:
- `api-gateway`
- `order-service`
- `settlement-service`
- `reconciliation-service`
- `user-service`

**Expected Output:**
```
[INFO] Building jar: .../api-gateway/target/api-gateway-1.0.0-SNAPSHOT.jar
[INFO] Building jar: .../order-service/target/order-service-0.0.1-SNAPSHOT.jar
[INFO] Building jar: .../settlement-service/target/settlement-service-0.0.1-SNAPSHOT.jar
[INFO] Building jar: .../reconciliation-service/target/reconciliation-service-0.0.1-SNAPSHOT.jar
[INFO] Building jar: .../user-service/target/user-service-0.0.1-SNAPSHOT.jar
```

## Step 2: Start Infrastructure with Docker Compose

Navigate to workspace root and start all services:

```bash
docker-compose up -d
```

This will start:
1. **PostgreSQL** (initialization happens first)
2. **Zookeeper** (required by Kafka)
3. **Kafka** (depends on Zookeeper)
4. **Consul** (Service Discovery UI)
5. **Order Service** (depends on all infrastructure)
6. **Settlement Service** (depends on all infrastructure)
7. **Reconciliation Service** (depends on all infrastructure)
8. **User Service** (depends on all infrastructure)
9. **API Gateway** (depends on all infrastructure)

**Wait for services to be healthy:**
```bash
docker-compose ps
```

Look for `healthy` status in the output. Wait 30-60 seconds for all services to be ready.

**Expected healthy status:**
```
CONTAINER ID   IMAGE                              STATUS              PORTS
...
...postgres...                                     healthy (30s)
...kafka...                                        healthy (30s)
...consul...                                       healthy (30s)
...order-service...                                healthy (10s)
...settlement-service...                           healthy (10s)
...reconciliation-service...                       healthy (10s)
...user-service...                                 healthy (10s)
...api-gateway...                                  healthy (10s)
```

## Step 3: Verify Services Are Running

Check service endpoints:

```bash
# Check Order Service health
curl http://localhost:8080/actuator/health

# Check Settlement Service health
curl http://localhost:8081/actuator/health

# Check Reconciliation Service health
curl http://localhost:8082/actuator/health

# Check User Service health
curl http://localhost:8083/actuator/health

# Check API Gateway health
curl http://localhost:8000/actuator/health

# Check Consul UI (open in browser)
# http://localhost:8500
```

## Step 4: Run End-to-End Test Scenarios

### Scenario 1: Create an Order and Verify Event Flow

**4.1a: Create an Order**

```bash
curl -X POST http://localhost:8080/api/v1/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "CUST001",
    "productId": "PROD001",
    "quantity": 5,
    "unitPrice": 100.00
  }'
```

**Expected Response:**
```json
{
  "id": 1,
  "orderNumber": "ORD-20241022-001",
  "customerId": "CUST001",
  "productId": "PROD001",
  "quantity": 5,
  "unitPrice": 100.00,
  "totalAmount": 500.00,
  "status": "PENDING",
  "createdAt": "2026-10-22T10:30:00Z"
}
```

**Note the `orderNumber` from the response (e.g., `ORD-20241022-001`)**

**4.1b: Wait for Event Publishing (5-15 seconds)**

The order service publishes events via the Outbox pattern every 5 seconds.

**4.1c: Verify Outbox Events**

Connect to the order_db database:
```bash
docker exec -it event-driven-postgres psql -U postgres -d order_db
```

Run this query:
```sql
SELECT * FROM outbox_events ORDER BY created_at DESC LIMIT 5;
```

**Expected Output:**
```
 id | event_type      | payload                                  | aggregate_id | status    | created_at
 1  | ORDER_CREATED   | {"orderId": 1, "orderNumber": "ORD..."  | 1            | PUBLISHED | 2026-10-22...
```

**4.1d: Verify Settlement Record Created**

Connect to the settlement_db database:
```bash
docker exec -it event-driven-postgres psql -U postgres -d settlement_db
```

Run this query:
```sql
SELECT * FROM settlement_records ORDER BY created_at DESC LIMIT 1;
```

**Expected Output:**
```
 settlement_id | order_id | order_number    | customer_id | settlement_amount | fee_amount | net_amount | status  | created_at
 ...           | 1        | ORD-20241022... | CUST001     | 500.00            | 10.00      | 490.00     | PENDING | 2026-10-22...
```

**Verification:** Fee should be 2% of order amount (500.00 * 0.02 = 10.00)

---

### Scenario 2: Update Order Status and Verify Status Change Event

**4.2a: Update Order Status to DELIVERED**

Replace `{orderId}` with the ID from scenario 4.1:

```bash
curl -X PATCH http://localhost:8080/api/v1/orders/1/status \
  -H "Content-Type: application/json" \
  -d 'status=DELIVERED'
```

**Expected Response:**
```json
{
  "id": 1,
  "orderNumber": "ORD-20241022-001",
  "status": "DELIVERED",
  "customerId": "CUST001",
  ...
}
```

**4.2b: Wait for Event to be Consumed (5-15 seconds)**

**4.2c: Verify Settlement Status Updated**

```bash
docker exec -it event-driven-postgres psql -U postgres -d settlement_db
```

Run this query:
```sql
SELECT settlement_id, order_id, status FROM settlement_records 
WHERE order_id = 1 
ORDER BY updated_at DESC LIMIT 1;
```

**Expected Output:**
```
 settlement_id | order_id | status
 ...           | 1        | PROCESSING
```

---

### Scenario 3: Test Order Cancellation and Settlement Cancellation

**4.3a: Cancel an Order**

```bash
curl -X PATCH http://localhost:8080/api/v1/orders/1/status \
  -d 'status=CANCELLED'
```

**4.3b: Verify Settlement is Cancelled**

```bash
docker exec -it event-driven-postgres psql -U postgres -d settlement_db
```

Run:
```sql
SELECT settlement_id, order_id, status FROM settlement_records 
WHERE order_id = 1 
ORDER BY updated_at DESC LIMIT 1;
```

**Expected Output:**
```
 settlement_id | order_id | status
 ...           | 1        | CANCELLED
```

---

### Scenario 4: Retrieve Order via API

**4.4a: Get Order by Order Number**

Replace `ORD-20241022-001` with actual order number:

```bash
curl http://localhost:8080/api/v1/orders/ORD-20241022-001
```

**Expected Response:**
```json
{
  "id": 1,
  "orderNumber": "ORD-20241022-001",
  "customerId": "CUST001",
  "status": "CANCELLED",
  ...
}
```

**4.4b: Get All Orders for a Customer**

```bash
curl http://localhost:8080/api/v1/orders/customer/CUST001
```

**Expected Response:**
```json
[
  {
    "id": 1,
    "orderNumber": "ORD-20241022-001",
    "customerId": "CUST001",
    ...
  }
]
```

---

### Scenario 5: Monitor Reconciliation Service

**5.1: Trigger Reconciliation Job (Optional)**

Reconciliation runs automatically on schedule (default: 2 AM UTC). To trigger manually:

```bash
curl -X POST http://localhost:8082/api/v1/reconciliation/run
```

**5.2: Check Reconciliation Status**

```bash
curl http://localhost:8082/api/v1/reconciliation/status
```

---

## Step 5: Monitor Logs

**Watch all service logs:**

```bash
docker-compose logs -f
```

**Watch specific service:**

```bash
docker-compose logs -f order-service
docker-compose logs -f settlement-service
docker-compose logs -f kafka
```

**Key log patterns to look for:**

- **Order Service:**
  ```
  Creating order for customer: CUST001
  Order created successfully with order number: ORD-20241022-001
  Starting to publish pending outbox events
  Event published successfully. Event type: ORDER_CREATED
  ```

- **Settlement Service:**
  ```
  Received message from topic: order-created-events
  Settlement record created for order: ORD-20241022-001
  Settlement status updated to PROCESSING for order
  ```

---

## Step 6: Verify Event Flow via Kafka

**List Kafka topics:**

```bash
docker exec event-driven-kafka kafka-topics --list --bootstrap-server localhost:9092
```

**Expected topics:**
```
order-created-events
order-status-changed-events
order-events
```

**Consume messages from a topic (in real-time):**

```bash
docker exec event-driven-kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic order-created-events \
  --from-beginning
```

---

## Step 7: View Data via Database

**Connect to PostgreSQL:**

```bash
docker exec -it event-driven-postgres psql -U postgres
```

**List all databases:**
```sql
\l
```

**Connect to order_db:**
```sql
\c order_db
```

**View orders:**
```sql
SELECT * FROM orders;
```

**View outbox events:**
```sql
SELECT id, event_type, status FROM outbox_events ORDER BY created_at DESC;
```

**Connect to settlement_db:**
```sql
\c settlement_db
```

**View settlement records:**
```sql
SELECT settlement_id, order_id, settlement_amount, fee_amount, net_amount, status 
FROM settlement_records;
```

---

## Step 8: Consul Service Discovery

**Open Consul UI:**
```
http://localhost:8500
```

You should see:
- **Services:** order-service, settlement-service, reconciliation-service, user-service, api-gateway
- **Nodes:** All healthy
- **Health checks:** All passing

---

## Troubleshooting

### Services Not Starting
1. Check logs: `docker-compose logs`
2. Verify ports are not in use
3. Ensure Docker has enough memory (at least 4GB recommended)

### Events Not Being Published
1. Wait 5-10 seconds (Outbox polls every 5 seconds)
2. Check outbox_events table: `SELECT * FROM outbox_events ORDER BY created_at DESC LIMIT 10;`
3. Check service logs for errors

### Settlement Records Not Created
1. Verify order was created first
2. Wait 5-15 seconds for event consumption
3. Check settlement_db exists: `\l`
4. Check Kafka topics: `docker exec event-driven-kafka kafka-topics --list --bootstrap-server localhost:9092`

### Database Connection Issues
1. Verify PostgreSQL is running: `docker-compose ps | grep postgres`
2. Check database initialization: `docker-compose logs postgres | head -50`
3. Verify credentials in application.yml (default: postgres/postgres)

---

## Cleanup

**Stop all services:**

```bash
docker-compose down
```

**Stop and remove volumes (reset data):**

```bash
docker-compose down -v
```

**Remove and rebuild Docker images:**

```bash
docker-compose down --remove-orphans
docker-compose build --no-cache
docker-compose up -d
```

---

## Test Summary Checklist

- [ ] All services started and healthy
- [ ] Created order successfully
- [ ] Order published to Kafka (outbox table shows PUBLISHED)
- [ ] Settlement record created with correct fee calculation
- [ ] Updated order status to DELIVERED
- [ ] Settlement status updated to PROCESSING
- [ ] Cancelled order
- [ ] Settlement status updated to CANCELLED
- [ ] Retrieved order by number via API
- [ ] Retrieved all orders for customer
- [ ] Verified Kafka topics and messages
- [ ] Verified database data persistence

---

## Performance Expectations

- **Order Creation:** < 100ms
- **Event Publishing (Outbox):** 5-10 seconds (polling interval)
- **Event Consumption:** < 1 second after publication
- **Settlement Record Creation:** < 500ms after event consumption
- **Full E2E (order creation → settlement created):** 5-15 seconds

