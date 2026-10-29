# Service Discovery with Consul

## Overview

This Event-Driven Order Pipeline now implements **Consul-based service discovery** for automatic service registration, health monitoring, and inter-service communication. This document provides comprehensive guidance on Phase 1 implementation and how to use service discovery in your microservices.

## Table of Contents

1. [What Changed](#what-changed)
2. [Architecture](#architecture)
3. [Quick Start](#quick-start)
4. [How It Works](#how-it-works)
5. [Using Service Discovery](#using-service-discovery)
6. [Monitoring & Debugging](#monitoring--debugging)
7. [Best Practices](#best-practices)
8. [Troubleshooting](#troubleshooting)
9. [Next Steps](#next-steps)

---

## What Changed

### Phase 1 Implementation Includes:

#### 1. **Docker Compose Updates**
- Added Consul service (port 8500 for API/UI, port 8600 for DNS)
- All microservices now depend on `consul:service_healthy`
- Each service receives `SPRING_CLOUD_CONSUL_HOST` environment variable

#### 2. **Maven Dependencies (pom.xml)**
- Added Spring Cloud Bill of Materials (BOM) for version management
- Added `spring-cloud-starter-consul-discovery` - service registration/discovery
- Added `spring-cloud-starter-consul-config` - configuration management

#### 3. **Service Configuration (application.yml)**
Updated all four microservices with Consul discovery settings:
- **order-service**
- **settlement-service**
- **reconciliation-service**
- **user-service**

Each service now includes:
```yaml
spring:
  cloud:
    consul:
      host: ${SPRING_CLOUD_CONSUL_HOST:localhost}
      port: 8500
      discovery:
        enabled: true
        instance-id: ${spring.application.name}:${spring.cloud.client.hostname}:${server.port}
        register-health-check: true
        health-check-interval: 10s
        health-check-timeout: 5s
        health-check-path: /actuator/health
        prefer-ip-address: true
```

#### 4. **Service Discovery Configuration Beans**
Created `ServiceDiscoveryConfig.java` in each service:
- Provides LoadBalanced `RestTemplate` bean
- Enables automatic service discovery and load balancing
- Includes comprehensive usage examples in JavaDoc

---

## Architecture

### Service Registration & Discovery Flow

```
┌─────────────────────────────────────────────────────────────┐
│                      Docker Network                         │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  ┌──────────────┐      ┌──────────────┐                    │
│  │   Consul     │      │  PostgreSQL  │                    │
│  │  (8500)      │      │   (5432)     │                    │
│  │              │      │              │                    │
│  │ Service      │      └──────────────┘                    │
│  │ Registry     │                                           │
│  │ Health Checks│      ┌──────────────┐                    │
│  │ DNS Service  │      │   Kafka      │                    │
│  └──────────────┘      │   (9092)     │                    │
│       ▲                 │              │                    │
│       │ registers       └──────────────┘                    │
│       │                                                      │
│  ┌────┴─────────────────────────────────────────────────┐  │
│  │        Microservices (All auto-registered)           │  │
│  ├─────────────────────────────────────────────────────┤  │
│  │                                                      │  │
│  │  ┌──────────────┐    ┌──────────────┐              │  │
│  │  │   Order      │    │  Settlement  │              │  │
│  │  │   Service    │◄──►│   Service    │              │  │
│  │  │   (8080)     │    │   (8081)     │              │  │
│  │  └──────────────┘    └──────────────┘              │  │
│  │         ▲                   ▲                       │  │
│  │         │ register          │ register             │  │
│  │         │                   │                       │  │
│  │  ┌──────────────┐    ┌──────────────┐              │  │
│  │  │ Reconciliation│    │     User     │              │  │
│  │  │   Service    │    │    Service   │              │  │
│  │  │   (8082)     │    │    (8083)    │              │  │
│  │  └──────────────┘    └──────────────┘              │  │
│  │         ▲                   ▲                       │  │
│  │         │ register          │ register             │  │
│  │         └───────────────────┘                       │  │
│  │                                                      │  │
│  └──────────────────────────────────────────────────────┘  │
│                                                              │
│  Event Communication (Kafka Topics):                        │
│  - order-created-events                                     │
│  - order-status-changed-events                              │
│                                                              │
└─────────────────────────────────────────────────────────────┘

Service Discovery Features:
✓ Automatic Registration: Services auto-register on startup
✓ Automatic Deregistration: Services auto-deregister on shutdown
✓ Health Checks: Consul polls /actuator/health every 10 seconds
✓ Load Balancing: Ribbon distributes traffic across instances
✓ Failover: Unhealthy instances are automatically excluded
```

---

## Quick Start

### 1. Start All Services with Docker Compose

```bash
# Start the full stack with Consul
docker-compose up -d

# Verify all services are running
docker-compose ps

# Monitor startup logs
docker-compose logs -f
```

Expected output after startup (30-60 seconds):
- All 5 services (Consul, PostgreSQL, Kafka, Zookeeper, Order/Settlement/Reconciliation/User services) running
- Each microservice registered in Consul

### 2. Access Consul UI

Open your browser and navigate to:
```
http://localhost:8500/ui/
```

You'll see:
- **Services Tab**: All registered services with health status
- **Nodes Tab**: Physical/container nodes
- **Key/Value Tab**: Configuration storage (not yet used)

### 3. Verify Service Registration

```bash
# List all services registered in Consul
curl http://localhost:8500/v1/catalog/services | jq

# Check specific service (e.g., order-service)
curl http://localhost:8500/v1/catalog/service/order-service | jq

# Check health status of order-service instances
curl http://localhost:8500/v1/health/service/order-service | jq
```

### 4. Test Inter-Service Communication

```bash
# Create a test order
curl -X POST http://localhost:8080/api/v1/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "CUST001",
    "productId": "PROD001",
    "quantity": 5,
    "unitPrice": 100.00
  }'

# Services communicate via Kafka events (primary)
# HTTP inter-service calls use service discovery via RestTemplate
```

---

## How It Works

### Service Startup Flow

```
1. Service Container Starts
   ↓
2. Spring Boot Application Initializes
   ↓
3. Spring Cloud Consul Starter Activates
   ↓
4. Service Connects to Consul (host: consul, port: 8500)
   ↓
5. Service Registers Itself with Properties:
   - Service Name: spring.application.name (e.g., "order-service")
   - Instance ID: service-name:hostname:port
   - Tags: []
   - Port: server.port (8080, 8081, 8082, 8083)
   ↓
6. Consul Starts Health Checks
   - Every 10 seconds: HTTP GET /actuator/health
   - Timeout: 5 seconds
   - Status updates in real-time
   ↓
7. Service Ready for Discovery
   - Other services can now discover this service
   - Clients can route traffic via service name
   ↓
Service Shutdown Flow:
8. Service Receives Shutdown Signal
   ↓
9. Spring Cloud Consul Deregisters Service
   ↓
10. Consul Updates Service Catalog
    - Service marked as DOWN
    - No new traffic routed to this instance
    ↓
11. Clients Automatically Reroute Traffic
    - Ribbon detects deregistration
    - Routes to remaining healthy instances
```

### Inter-Service Communication Flow

```
Service A (Order Service) calls Service B (Settlement Service):

1. Code requests: restTemplate.getForObject("http://settlement-service/api/...")
   ↓
2. RestTemplate intercepts request (due to @LoadBalanced)
   ↓
3. Ribbon Load Balancer Activated
   ↓
4. Ribbon Queries Consul: "Give me instances of settlement-service"
   ↓
5. Consul Returns: [
     {
       "ID": "settlement-service:hostname:8081",
       "Service": "settlement-service",
       "Address": "172.20.0.5",
       "Port": 8081,
       "Status": "passing"
     }
   ]
   ↓
6. Ribbon Applies Load Balancing Strategy (round-robin by default)
   ↓
7. Request Sent to: http://172.20.0.5:8081/api/...
   ↓
8. Service B Processes Request and Returns Response
   ↓
9. Response Returned to Service A

If Multiple Instances Exist:
- Ribbon distributes load across all healthy instances
- Failed instances automatically excluded
- New instances automatically included
```

### Health Check Mechanism

```
Every 10 seconds (configurable via health-check-interval):

Consul → Service:/actuator/health

Service Response Examples:

✓ PASSING (Healthy):
{
  "status": "UP",
  "components": {
    "db": {"status": "UP"},
    "kafkaListener": {"status": "UP"},
    "diskSpace": {"status": "UP"}
  }
}

✗ FAILING (Unhealthy):
{
  "status": "DOWN",
  "error": "Database connection failed"
}

Actions:
- Status "UP" → Instance marked as "passing" → Traffic routed here
- Status "DOWN" → Instance marked as "failing" → Traffic NOT routed here
- 3 consecutive failures → Instance removed from service pool
- Recovery → Instance re-added to service pool
```

---

## Using Service Discovery

### 1. Making HTTP Calls Between Services

#### Option A: Service Name Resolution (Recommended)

```java
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.RestTemplate;
import org.springframework.stereotype.Service;

@Service
public class OrderService {
    
    @Autowired
    private RestTemplate discoveryRestTemplate;  // From ServiceDiscoveryConfig
    
    public void processOrder(Long orderId) {
        // Use service name instead of hardcoded URL
        String settlementData = discoveryRestTemplate
            .getForObject(
                "http://settlement-service/api/settlements/{id}",
                String.class,
                orderId
            );
        
        // Service discovery automatically:
        // 1. Resolves "settlement-service" to available instances
        // 2. Load-balances across instances
        // 3. Excludes unhealthy instances
    }
}
```

#### Option B: Direct Service Discovery Query (Advanced)

```java
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.ServiceInstance;

@Service
public class AdvancedService {
    
    @Autowired
    private DiscoveryClient discoveryClient;
    
    public void findService() {
        List<ServiceInstance> instances = discoveryClient
            .getInstances("settlement-service");
        
        for (ServiceInstance instance : instances) {
            System.out.println("Instance: " + instance.getHost() 
                + ":" + instance.getPort());
            System.out.println("Metadata: " + instance.getMetadata());
        }
    }
}
```

### 2. Available Service Names

In your application, use these service names for inter-service HTTP calls:

| Service | Name | Port | Base URL |
|---------|------|------|----------|
| Order | `order-service` | 8080 | `http://order-service:8080` |
| Settlement | `settlement-service` | 8081 | `http://settlement-service:8081` |
| Reconciliation | `reconciliation-service` | 8082 | `http://reconciliation-service:8082` |
| User | `user-service` | 8083 | `http://user-service:8083` |

### 3. Example: Settlement Service Calling Order Service

```java
@Service
@Slf4j
public class SettlementProcessor {
    
    @Autowired
    private RestTemplate discoveryRestTemplate;
    
    public Order getOrderDetails(String orderNumber) {
        try {
            Order order = discoveryRestTemplate
                .getForObject(
                    "http://order-service/api/v1/orders/{orderNumber}",
                    Order.class,
                    orderNumber
                );
            log.info("Retrieved order: {}", orderNumber);
            return order;
        } catch (HttpClientErrorException.NotFound e) {
            log.warn("Order not found: {}", orderNumber);
            return null;
        }
    }
}
```

---

## Monitoring & Debugging

### 1. Consul UI Dashboard

Visit: **http://localhost:8500/ui/**

**Services Tab:**
- See all registered services
- View instance health status
- Check instance metadata
- Monitor registration history

### 2. CLI Commands for Service Discovery

```bash
# List all registered services
curl http://localhost:8500/v1/catalog/services | jq

# Get details of specific service
curl http://localhost:8500/v1/catalog/service/order-service | jq

# Check health of service instances
curl http://localhost:8500/v1/health/service/settlement-service | jq

# Query service by tag (when tags are used)
curl http://localhost:8500/v1/catalog/service/order-service?tag=primary | jq

# Get service metadata
curl http://localhost:8500/v1/kv/services/ | jq
```

### 3. Docker Commands for Service Inspection

```bash
# View Consul logs
docker-compose logs consul

# View service startup logs
docker-compose logs order-service
docker-compose logs settlement-service

# Execute commands inside service container
docker-compose exec order-service sh

# Check service network connectivity
docker-compose exec order-service curl http://consul:8500/v1/health/service/order-service
```

### 4. Service Registration Verification

```bash
# Check if a service is registered
docker-compose exec consul consul catalog services

# View service instances
docker-compose exec consul consul catalog service order-service

# View service health
docker-compose exec consul consul health service order-service
```

### 5. Logs to Watch For

**Successful Registration (look for these in logs):**
```
INFO  o.s.c.c.discovery.ConsulServiceRegistry    : Register service: {
  "id": "order-service:...:8080",
  "name": "order-service",
  "port": 8080
}
INFO  o.s.c.consul.discovery.ConsulDiscoveryClient : Getting instances for service: order-service
```

**Health Check Passing:**
```
INFO  o.s.c.consul.discovery.ConsulHealthIndicator : Consul health check passed
```

**Service Discovery in Action:**
```
DEBUG c.n.loadbalancer.LoadBalancerContext : Using endpoint: [order-service:8080]
DEBUG c.n.loadbalancer.ILoadBalancer : All servers are in UP state
```

---

## Best Practices

### 1. Service Naming Conventions

✓ **DO:**
- Use lowercase service names: `order-service`, `settlement-service`
- Use hyphens to separate words: `user-service` (not `userService`)
- Match service names to `spring.application.name` property
- Keep names concise and descriptive

✗ **DON'T:**
- Use uppercase or mixed case: `OrderService`
- Use underscores: `order_service`
- Use inconsistent naming across services

### 2. Instance Identification

✓ **DO:**
- Use the provided instance-id format: `service-name:hostname:port`
- This uniquely identifies each instance
- Helps with debugging and monitoring

✗ **DON'T:**
- Use generic instance IDs
- Rely on container names as unique identifiers

### 3. Health Check Configuration

✓ **DO:**
- Keep health check intervals reasonable (10s default is good)
- Ensure `/actuator/health` endpoint is accessible
- Monitor health check logs
- Configure appropriate timeouts

✗ **DON'T:**
- Set health check intervals too low (causes high Consul load)
- Disable health checks unless absolutely necessary
- Ignore health check failures

### 4. RestTemplate Usage

✓ **DO:**
```java
// Use service name with @LoadBalanced RestTemplate
@Autowired
private RestTemplate discoveryRestTemplate;

String result = discoveryRestTemplate
    .getForObject("http://settlement-service/api/data", String.class);
```

✗ **DON'T:**
```java
// Don't hardcode service URLs
String result = restTemplate
    .getForObject("http://localhost:8081/api/data", String.class);
```

### 5. Error Handling

```java
@Service
public class ResilientService {
    
    @Autowired
    private RestTemplate discoveryRestTemplate;
    
    public void callService() {
        try {
            String result = discoveryRestTemplate
                .getForObject(
                    "http://settlement-service/api/data",
                    String.class
                );
        } catch (HttpClientErrorException e) {
            log.error("Service error: {}", e.getStatusCode(), e);
            // Handle specific HTTP errors
        } catch (ResourceAccessException e) {
            log.error("Service unavailable: {}", e.getMessage(), e);
            // Handle connection failures (service down)
            // Ribbon will automatically retry with other instances
        } catch (Exception e) {
            log.error("Unexpected error: {}", e.getMessage(), e);
            // Generic error handling
        }
    }
}
```

### 6. Local Development vs Docker

**Local Development (application.yml):**
```yaml
spring:
  cloud:
    consul:
      host: localhost  # Local Consul
      port: 8500
```

**Docker (application.yml):**
```yaml
spring:
  cloud:
    consul:
      host: ${SPRING_CLOUD_CONSUL_HOST:localhost}  # From docker-compose env var
      port: 8500
```

---

## Troubleshooting

### Issue 1: Service Not Appearing in Consul

**Symptoms:**
- Service doesn't show up in Consul UI
- `curl http://localhost:8500/v1/catalog/services` shows empty list

**Diagnosis:**
```bash
# Check service logs
docker-compose logs order-service | grep -i consul

# Verify Consul is running
docker-compose ps | grep consul

# Check network connectivity
docker-compose exec order-service curl http://consul:8500/v1/health/node/consul
```

**Solutions:**
1. **Verify Consul is running:**
   ```bash
   docker-compose up -d consul
   ```

2. **Check spring.cloud.consul.host in application.yml:**
   ```yaml
   spring:
     cloud:
       consul:
         host: consul  # For Docker, not localhost
   ```

3. **Verify SPRING_CLOUD_CONSUL_HOST environment variable:**
   ```bash
   docker-compose logs order-service | grep SPRING_CLOUD_CONSUL_HOST
   ```

4. **Check service startup logs for errors:**
   ```bash
   docker-compose logs order-service | tail -50
   ```

### Issue 2: Health Checks Failing

**Symptoms:**
- Consul shows service status as "failing"
- Service is registered but marked unhealthy

**Diagnosis:**
```bash
# Check health endpoint directly
docker-compose exec order-service curl http://localhost:8080/actuator/health

# Check Consul health check logs
docker-compose logs consul | grep health

# View service health status
curl http://localhost:8500/v1/health/service/order-service | jq '.[] | {ID, Status}'
```

**Solutions:**
1. **Ensure /actuator/health is accessible:**
   ```bash
   docker-compose exec order-service curl http://localhost:8080/actuator/health
   ```

2. **Check application logs for errors:**
   ```bash
   docker-compose logs order-service | grep -i error
   ```

3. **Verify database connectivity:**
   ```bash
   docker-compose logs order-service | grep -i database
   ```

4. **Increase health check timeout if needed:**
   ```yaml
   spring:
     cloud:
       consul:
         discovery:
           health-check-timeout: 10s  # Increase from 5s
   ```

### Issue 3: Service Discovery Not Working

**Symptoms:**
- RestTemplate calls to `http://settlement-service/...` fail
- Error: "UnknownHostException: settlement-service"

**Diagnosis:**
```bash
# Check if service is registered and healthy
curl http://localhost:8500/v1/health/service/settlement-service | jq

# Check logs for Ribbon/load balancer errors
docker-compose logs order-service | grep -i ribbon
```

**Solutions:**
1. **Verify target service is registered and healthy:**
   ```bash
   curl http://localhost:8500/v1/health/service/settlement-service | jq
   ```

2. **Check RestTemplate bean is using @LoadBalanced:**
   ```java
   @Bean
   @LoadBalanced
   public RestTemplate discoveryRestTemplate() {
       return new RestTemplate();
   }
   ```

3. **Verify service name matches registered service:**
   ```bash
   # List all registered services
   curl http://localhost:8500/v1/catalog/services | jq
   ```

4. **Check Spring Cloud Ribbon logs:**
   ```yaml
   logging:
     level:
       com.netflix.loadbalancer: DEBUG
   ```

### Issue 4: Services Can't Find Each Other After Container Restart

**Symptoms:**
- Services work, then after restart they can't discover each other
- Consul UI shows outdated instance information

**Solutions:**
1. **Ensure proper depends_on order in docker-compose.yml:**
   ```yaml
   order-service:
     depends_on:
       consul:
         condition: service_healthy
   ```

2. **Wait for service registration before making calls:**
   - Add startup delay in service startup code
   - Use circuit breakers (Phase 2)
   - Implement retry logic

3. **Clear Docker volumes and restart:**
   ```bash
   docker-compose down -v
   docker-compose up -d
   ```

### Issue 5: Excessive Logging Output

**Symptoms:**
- Logs filled with Consul/Ribbon debug messages
- Difficult to see application logs

**Solution:**
```yaml
logging:
  level:
    org.springframework.cloud.consul: INFO      # Reduce from DEBUG
    com.netflix.loadbalancer: INFO               # Reduce from DEBUG
    goldmansachs: DEBUG                          # Keep app logs at DEBUG
```

---

## Next Steps

### Phase 2: Resilience & Monitoring
- [ ] Add circuit breaker with Resilience4j
- [ ] Implement retry logic for failed calls
- [ ] Add distributed tracing (Jaeger/Zipkin)
- [ ] Enhanced metrics collection

### Phase 3: Advanced Features
- [ ] Multiple service instances (horizontal scaling)
- [ ] Service mesh integration (Istio)
- [ ] Advanced load balancing strategies
- [ ] Consul key-value store for configuration management

### Phase 4: Production Ready
- [ ] Kubernetes migration (replace Consul DNS with K8s DNS)
- [ ] Security hardening (TLS/mTLS)
- [ ] Performance tuning
- [ ] Advanced monitoring and alerting

---

## Configuration Reference

### Consul Discovery Configuration

```yaml
spring:
  cloud:
    consul:
      # Consul server location
      host: localhost              # Consul host
      port: 8500                   # Consul API port
      
      # Service registration
      discovery:
        enabled: true              # Enable service registration
        instance-id: ${spring.application.name}:${spring.cloud.client.hostname}:${server.port}
        register-health-check: true # Register health checks
        health-check-interval: 10s  # Health check frequency
        health-check-timeout: 5s    # Health check timeout
        health-check-path: /actuator/health  # Health check endpoint
        prefer-ip-address: true     # Use IP instead of hostname
        skip-registration: false    # Enable registration
```

### Key Properties

| Property | Default | Description |
|----------|---------|-------------|
| `spring.cloud.consul.host` | `localhost` | Consul server hostname |
| `spring.cloud.consul.port` | `8500` | Consul API port |
| `spring.cloud.consul.discovery.enabled` | `true` | Enable service discovery |
| `spring.cloud.consul.discovery.register-health-check` | `true` | Register health checks |
| `spring.cloud.consul.discovery.health-check-interval` | `10s` | Poll interval for health |
| `spring.cloud.consul.discovery.health-check-timeout` | `5s` | Health check timeout |
| `spring.cloud.consul.discovery.prefer-ip-address` | `true` | Use IP for registration |

---

## Summary

**Phase 1 Implementation Complete:**
✓ Consul integrated with Docker Compose
✓ All services configured for automatic discovery
✓ Health checks configured and operational
✓ LoadBalanced RestTemplate beans created
✓ Comprehensive documentation provided

**You can now:**
✓ Access Consul UI at http://localhost:8500/ui/
✓ Make inter-service HTTP calls using service names
✓ Automatically discover and load-balance across service instances
✓ Monitor service health in real-time
✓ Scale services horizontally with automatic discovery

**Kafka-based event communication remains unchanged and fully operational.**

For questions or issues, refer to the [Troubleshooting](#troubleshooting) section above.
