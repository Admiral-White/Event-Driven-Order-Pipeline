package goldmansachs.settlement.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Service Discovery Configuration for Inter-Service Communication
 *
 * This configuration enables service-to-service HTTP communication using Spring Cloud's
 * load-balanced RestTemplate. Services register with Consul and are automatically discovered.
 *
 * Key Features:
 * - Automatic service registration with Consul on startup
 * - Service deregistration on graceful shutdown
 * - Health checks ensure only healthy instances receive traffic
 * - Load balancing across multiple instances of a service
 * - Ribbon integration for client-side load balancing
 *
 * Usage Example:
 *   @Autowired
 *   private RestTemplate restTemplate;
 *
 *   // Instead of: http://localhost:8080/api/orders
 *   // Use service name registered in Consul:
 *   String response = restTemplate.getForObject(
 *       "http://order-service/api/orders",
 *       String.class
 *   );
 *
 * How It Works:
 * 1. RestTemplate is injected with @LoadBalanced annotation
 * 2. When a request is made to "http://service-name/...", Ribbon intercepts it
 * 3. Ribbon queries Consul for available instances of "service-name"
 * 4. Consul returns healthy instances and their addresses
 * 5. Ribbon load-balances the request across available instances
 * 6. If a service goes down, Consul marks it as unhealthy
 * 7. Ribbon stops routing traffic to unhealthy instances
 *
 * Benefits:
 * - No hardcoded service URLs (e.g., localhost:8080)
 * - Services can scale horizontally (multiple instances)
 * - Automatic failover to healthy instances
 * - Consistent discovery pattern across all microservices
 * - Works seamlessly in containerized and cloud environments
 *
 * @author Michael Inyang
 * @version 1.0.0
 */
@Configuration
@Slf4j
public class ServiceDiscoveryConfig {

    /**
     * Creates a LoadBalanced RestTemplate bean for inter-service communication.
     *
     * The @LoadBalanced annotation integrates Spring Cloud's Ribbon with RestTemplate,
     * enabling automatic service discovery and client-side load balancing via Consul.
     *
     * This RestTemplate should be used for all HTTP calls to other microservices that
     * are registered in Consul. It will automatically resolve service names to available
     * instances and handle load balancing.
     *
     * Configuration:
     * - Connection timeout: managed by RestTemplate defaults (60 seconds)
     * - Read timeout: managed by RestTemplate defaults (60 seconds)
     * - Retry policy: can be configured via spring.cloud.consul properties
     * - Load balancing strategy: round-robin (default)
     *
     * Notes:
     * - This RestTemplate is NOT suitable for external third-party API calls
     * - For external APIs, create a separate non-LoadBalanced RestTemplate
     * - Consul health checks ensure only healthy instances are used
     *
     * @return LoadBalanced RestTemplate configured for service discovery
     */
    @Bean
    @LoadBalanced
    public RestTemplate discoveryRestTemplate() {
        log.info("Initializing LoadBalanced RestTemplate for Consul service discovery");
        return new RestTemplate();
    }
}
