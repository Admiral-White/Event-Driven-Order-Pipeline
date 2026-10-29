package goldmansachs.gateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * API Gateway Application - Main Entry Point
 *
 * ============================================================================
 * Overview:
 * ============================================================================
 * The API Gateway is the single entry point for all external client requests
 * to the Event-Driven Order Pipeline microservices. It leverages Spring Cloud
 * Gateway and Consul service discovery to route requests to appropriate
 * backend services dynamically.
 *
 * ============================================================================
 * Key Responsibilities:
 * ============================================================================
 * 1. REQUEST ROUTING
 *    - Route external client requests to appropriate microservices
 *    - Use path-based routing rules defined in application.yml
 *    - Examples:
 *      * /api/v1/orders/** → order-service
 *      * /api/v1/settlements/** → settlement-service
 *      * /auth/** → user-service
 *
 * 2. SERVICE DISCOVERY INTEGRATION
 *    - Query Consul for available service instances
 *    - Dynamically update routes as services come online/offline
 *    - No hardcoded service URLs or IP addresses
 *
 * 3. LOAD BALANCING
 *    - Distribute requests across multiple instances of a service
 *    - Automatically exclude unhealthy instances
 *    - Ribbon-based client-side load balancing
 *
 * 4. HEALTH MONITORING
 *    - Register itself with Consul as a discoverable service
 *    - Expose health endpoints for monitoring
 *    - Monitor backend service health via Consul
 *
 * 5. REQUEST/RESPONSE PROCESSING
 *    - Apply filters to requests before routing
 *    - Transform headers (preserve host, add tracing headers)
 *    - Potential future: authentication, rate limiting, compression
 *
 * ============================================================================
 * Architecture:
 * ============================================================================
 * External Clients (Web/Mobile/Desktop)
 *          │
 *          ▼
 *    [API Gateway] ← port 8000 (single entry point)
 *          │
 *     ┌────┼────┬────────┬──────────┐
 *     │    │    │        │          │
 *     ▼    ▼    ▼        ▼          ▼
 *  Order  Settlement  User    Reconciliation
 *  Service Service   Service  Service
 *  (8080) (8081)    (8083)   (8082)
 *     │    │         │         │
 *     └────┼─────┬───┘         │
 *          │     │             │
 *          ▼     ▼             ▼
 *      PostgreSQL ← ← ← → Kafka Events
 *
 * Service Discovery:
 * - Gateway and all services register with Consul (port 8500)
 * - Gateway queries Consul to find service instances
 * - Consul provides health status for load balancing
 * - Services auto-deregister on shutdown
 *
 * ============================================================================
 * Non-Blocking Architecture:
 * ============================================================================
 * Spring Cloud Gateway uses Spring WebFlux (reactive framework) providing:
 *
 * Benefits:
 * - Handles high concurrency with fewer threads
 * - Better resource utilization (CPU, memory)
 * - Lower latency for I/O-bound operations
 * - Can handle thousands of concurrent requests
 *
 * Implications:
 * - Use non-blocking libraries for custom filters
 * - Avoid blocking operations in custom code
 * - RestTemplate calls should use WebClient (future enhancement)
 *
 * ============================================================================
 * Route Configuration:
 * ============================================================================
 * Routes are defined in application.yml with three main components:
 *
 * 1. PREDICATES: Conditions to match incoming requests
 *    - Path: /api/v1/orders/**
 *    - Method: GET, POST, etc.
 *    - Host: specific domain
 *    Example: Match requests to /api/v1/orders/* → route to order-service
 *
 * 2. URI: Destination service using load-balanced discovery
 *    - Format: lb://service-name
 *    - Example: lb://order-service
 *    - Ribbon resolves service name to actual instance address via Consul
 *
 * 3. FILTERS: Transformations applied to requests/responses
 *    - PreserveHostHeader: Keep original host header
 *    - AddResponseHeader: Add custom response headers
 *    - RequestRateLimiter: Rate limiting (future)
 *    - Rewrite paths: Modify request paths before routing
 *
 * ============================================================================
 * Request Flow:
 * ============================================================================
 * 1. Client sends request to gateway
 *    GET http://localhost:8000/api/v1/orders/123
 *
 * 2. Gateway matches request against routes in application.yml
 *    Matches: Path=/api/v1/orders/** → order-service-route
 *
 * 3. Gateway applies filters to the request
 *    - Preserve host header
 *    - Add tracing headers (future)
 *
 * 4. Ribbon (load balancer) queries Consul
 *    "Give me healthy instances of order-service"
 *
 * 5. Consul returns available instances
 *    [
 *      { "host": "172.20.0.3", "port": 8080, "status": "passing" }
 *    ]
 *
 * 6. Ribbon selects an instance (round-robin)
 *    Selected: 172.20.0.3:8080
 *
 * 7. Gateway routes request to selected instance
 *    GET http://172.20.0.3:8080/api/v1/orders/123
 *
 * 8. Backend service processes request and returns response
 *
 * 9. Gateway applies response filters
 *    - Add X-Gateway-Header: API-Gateway-1.0
 *    - Pass through response status/headers/body
 *
 * 10. Gateway returns response to client
 *
 * ============================================================================
 * Deployment:
 * ============================================================================
 * Local Development:
 * - Runs on port 8000
 * - Connects to Consul on localhost:8500
 * - Routes to services on localhost:8080-8083
 *
 * Docker Compose:
 * - Runs in container on port 8000
 * - Connects to Consul container via network
 * - Routes to service containers via internal network
 *
 * Production (Kubernetes):
 * - Runs in pod on port 8000
 * - Can be replaced with Kubernetes Ingress
 * - Or use as internal service mesh gateway
 *
 * ============================================================================
 * Startup Sequence:
 * ============================================================================
 * 1. Spring Boot initializes
 * 2. Spring Cloud Consul integration starts
 * 3. Gateway connects to Consul at configured host:port
 * 4. Gateway registers itself with Consul
 *    - Service name: "api-gateway"
 *    - Port: 8000
 *    - Health check: GET /actuator/health
 * 5. Gateway loads routes from application.yml
 * 6. Ribbon initializes load balancer
 * 7. Gateway is ready to receive client requests
 * 8. Consul monitors gateway health continuously
 *
 * ============================================================================
 * Monitoring & Troubleshooting:
 * ============================================================================
 * Gateway Health:
 *   GET http://localhost:8000/actuator/health
 *
 * Gateway Routes:
 *   GET http://localhost:8000/actuator/gateway/routes
 *
 * Gateway Metrics:
 *   GET http://localhost:8000/actuator/metrics
 *
 * Consul UI (view all services):
 *   http://localhost:8500/ui/
 *
 * Logs:
 *   docker-compose logs api-gateway
 *
 * Test routing:
 *   curl http://localhost:8000/api/v1/orders (routes to order-service)
 *
 * ============================================================================
 * Future Enhancements:
 * ============================================================================
 * Phase 2: Authentication & Security
 * - JWT token validation for all routes
 * - Rate limiting per user/API key
 * - CORS configuration
 * - Request/response transformation
 *
 * Phase 3: Observability
 * - Distributed tracing (Jaeger/Zipkin)
 * - Request correlation IDs
 * - Metrics collection and export
 * - Centralized logging
 *
 * Phase 4: Advanced Features
 * - API versioning support
 * - Canary deployments
 * - Circuit breakers for backend services
 * - Request caching
 *
 * ============================================================================
 *
 * @author Michael Inyang
 * @version 1.0.0
 * @see org.springframework.cloud.gateway.config.GatewayAutoConfiguration
 * @see org.springframework.cloud.client.discovery.EnableDiscoveryClient
 */
@SpringBootApplication
@EnableDiscoveryClient
public class ApiGatewayApplication {

    private static final Logger log = LoggerFactory.getLogger(ApiGatewayApplication.class);

    /**
     * Main entry point for the API Gateway application.
     *
     * Initializes and starts the Spring Boot application with:
     * - Spring Cloud Gateway for request routing
     * - Consul discovery client for service discovery
     * - Spring Actuator for health monitoring
     *
     * Execution Flow:
     * 1. SpringApplication.run() initializes Spring context
     * 2. Spring Cloud Consul integration starts
     * 3. Gateway registers itself with Consul
     * 4. Routes from application.yml are loaded
     * 5. Gateway listens for incoming client requests on port 8000
     * 6. All services are discovered from Consul and made available for routing
     *
     * @param args Command line arguments (typically empty in Spring Boot)
     */
    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
        log.info("════════════════════════════════════════════════════════════════");
        log.info("API Gateway Started Successfully");
        log.info("════════════════════════════════════════════════════════════════");
        log.info("Gateway is listening on port 8000");
        log.info("Consul Discovery: http://localhost:8500/ui/");
        log.info("Gateway Health: http://localhost:8000/actuator/health");
        log.info("Gateway Routes: http://localhost:8000/actuator/gateway/routes");
        log.info("════════════════════════════════════════════════════════════════");
        log.info("Available Routes:");
        log.info("  • /api/v1/orders/** → order-service (8080)");
        log.info("  • /api/v1/settlements/** → settlement-service (8081)");
        log.info("  • /api/v1/reconciliation/** → reconciliation-service (8082)");
        log.info("  • /api/v1/users/** → user-service (8083)");
        log.info("  • /auth/** → user-service (8083)");
        log.info("════════════════════════════════════════════════════════════════");
    }
}
