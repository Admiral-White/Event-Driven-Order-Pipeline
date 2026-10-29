package goldmansachs.gateway.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import reactor.core.publisher.Mono;

/**
 * API Gateway Configuration
 *
 * ============================================================================
 * Overview:
 * ============================================================================
 * This configuration class defines custom beans and settings for the API
 * Gateway. It handles:
 * - CORS (Cross-Origin Resource Sharing) configuration
 * - Rate limiting key resolution
 * - Custom filters and interceptors (future)
 * - Gateway-wide settings and policies
 *
 * ============================================================================
 * CORS (Cross-Origin Resource Sharing):
 * ============================================================================
 * CORS allows web applications running at one origin to access resources
 * from a server at a different origin. This is essential for:
 *
 * Scenarios:
 * - Frontend (example.com) calling backend (api.example.com)
 * - Web app on port 3000 calling gateway on port 8000
 * - Third-party mobile apps accessing the gateway
 *
 * Configuration:
 * - Allowed Origins: Which domains can make requests
 * - Allowed Methods: Which HTTP methods are permitted (GET, POST, etc.)
 * - Allowed Headers: Which request headers are allowed
 * - Exposed Headers: Which response headers are accessible to clients
 * - Allow Credentials: Whether to include cookies/auth headers
 * - Max Age: How long browsers can cache CORS preflight responses
 *
 * Request Flow:
 * 1. Browser sends OPTIONS preflight request
 *    OPTIONS /api/v1/orders HTTP/1.1
 *    Origin: http://localhost:3000
 *    Access-Control-Request-Method: POST
 *
 * 2. Gateway responds with CORS headers
 *    Access-Control-Allow-Origin: http://localhost:3000
 *    Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS
 *    Access-Control-Allow-Headers: Content-Type, Authorization
 *    Access-Control-Max-Age: 3600
 *
 * 3. Browser receives response and permits actual request
 *    POST /api/v1/orders HTTP/1.1
 *    (Request proceeds)
 *
 * ============================================================================
 * Rate Limiting (Future Enhancement):
 * ============================================================================
 * Rate limiting prevents abuse by restricting the number of requests:
 *
 * Current Configuration:
 * - All routes limited to 100 requests per second (normal traffic)
 * - Burst capacity: 200 requests (handles spikes)
 * - Key resolver: Uses request path (route-based limiting)
 *
 * Future Enhancement Options:
 * - Per-user limiting: Restrict based on API key/user ID
 * - Per-IP limiting: Restrict based on client IP address
 * - Dynamic limiting: Adjust limits based on service load
 * - Different limits for different routes
 *
 * Example (per-user limiting):
 * Key Resolver: IP + User ID
 * Limit: 1000 requests per hour per user
 *
 * ============================================================================
 * Custom Filters (Future Enhancement):
 * ============================================================================
 * Filters can be added to:
 * - Transform requests (modify headers, path, body)
 * - Transform responses (compress, add headers)
 * - Implement cross-cutting concerns
 * - Validate requests before routing
 * - Add tracing/correlation IDs
 * - Implement circuit breakers
 *
 * Example (future):
 * - Add X-Request-ID header for distributed tracing
 * - Add X-Forwarded-For header for client IP
 * - Compress responses to reduce bandwidth
 * - Validate JWT tokens before routing
 *
 * ============================================================================
 *
 * @author Michael Inyang
 * @version 1.0.0
 */
@Configuration
public class GatewayConfig {

    private static final Logger log = LoggerFactory.getLogger(GatewayConfig.class);

    /**
     * Configures CORS (Cross-Origin Resource Sharing) for the API Gateway.
     *
     * CORS allows web applications to make requests to the gateway from
     * different origins (domains). This is essential for:
     * - Web frontends running on different ports/domains
     * - Third-party applications accessing the API
     * - Mobile apps using web technologies (React Native, etc.)
     *
     * Configuration Details:
     * ─────────────────────
     * Allowed Origins:
     * - http://localhost:3000 (local frontend development)
     * - http://localhost:4200 (Angular development)
     * - http://localhost:8081 (Vue development)
     * - Add production domains as needed
     *
     * Allowed Methods:
     * - GET: Retrieve resources
     * - POST: Create resources
     * - PUT: Replace resources
     * - DELETE: Delete resources
     * - PATCH: Partial update
     * - OPTIONS: CORS preflight
     *
     * Allowed Headers:
     * - Content-Type: Request body format
     * - Authorization: Bearer tokens, API keys
     * - Accept: Response format preference
     * - Cache-Control: Caching directives
     * - Pragma: Legacy cache control
     *
     * Exposed Headers (accessible to JavaScript):
     * - Content-Type: Response body format
     * - X-Request-ID: For request tracing
     * - X-Total-Count: For pagination
     * - X-RateLimit-*: For rate limit info
     *
     * Credentials:
     * - allow: true allows Authorization headers in requests
     * - Requires matching origin (not wildcard *)
     *
     * Max Age:
     * - 3600 seconds (1 hour)
     * - Browser caches preflight response for 1 hour
     * - Reduces preflight requests for repeated calls
     *
     * Usage:
     * ──────
     * Frontend code calling the gateway:
     *
     *   fetch('http://localhost:8000/api/v1/orders', {
     *     method: 'POST',
     *     headers: {
     *       'Content-Type': 'application/json',
     *       'Authorization': 'Bearer eyJhbGciOiJIUzI1NiIs...'
     *     },
     *     body: JSON.stringify({
     *       customerId: 'CUST001',
     *       productId: 'PROD001',
     *       quantity: 5,
     *       unitPrice: 100.00
     *     })
     *   })
     *   .then(response => response.json())
     *   .then(data => console.log(data))
     *   .catch(error => console.error('Error:', error));
     *
     * Troubleshooting CORS Issues:
     * ────────────────────────────
     * Error: "Access to XMLHttpRequest blocked by CORS policy"
     * Solution: Check if origin is in allowedOrigins list
     *
     * Error: "Preflight request fails (405 Method Not Allowed)"
     * Solution: Ensure OPTIONS method is in allowedMethods
     *
     * Error: "Authorization header blocked"
     * Solution: Add Authorization to allowedHeaders
     *           Set allow credentials to true
     *
     * @return CorsWebFilter configured with CORS settings
     */
    @Bean
    public CorsWebFilter corsWebFilter() {
        log.info("Configuring CORS for API Gateway");

        // Create CORS configuration
        CorsConfiguration corsConfiguration = new CorsConfiguration();

        // Set allowed origins (modify for production)
        // Development: Allow localhost on common frontend ports
        corsConfiguration.addAllowedOrigin("http://localhost:3000");      // React
        corsConfiguration.addAllowedOrigin("http://localhost:4200");      // Angular
        corsConfiguration.addAllowedOrigin("http://localhost:8081");      // Vue
        corsConfiguration.addAllowedOrigin("http://localhost:8888");      // Other development
        corsConfiguration.addAllowedOrigin("http://127.0.0.1:3000");      // Loopback
        corsConfiguration.addAllowedOrigin("http://127.0.0.1:4200");
        corsConfiguration.addAllowedOrigin("http://127.0.0.1:8081");

        // TODO: Production - Replace with actual domain
        // corsConfiguration.addAllowedOrigin("https://example.com");
        // corsConfiguration.addAllowedOrigin("https://app.example.com");

        // Set allowed HTTP methods
        corsConfiguration.addAllowedMethod("GET");      // Retrieve resources
        corsConfiguration.addAllowedMethod("POST");     // Create resources
        corsConfiguration.addAllowedMethod("PUT");      // Replace resources
        corsConfiguration.addAllowedMethod("DELETE");   // Delete resources
        corsConfiguration.addAllowedMethod("PATCH");    // Partial update
        corsConfiguration.addAllowedMethod("OPTIONS");  // CORS preflight

        // Set allowed request headers
        corsConfiguration.addAllowedHeader("*");        // Allow all headers (more permissive)
        // More restrictive alternative:
        // corsConfiguration.addAllowedHeader("Content-Type");
        // corsConfiguration.addAllowedHeader("Authorization");
        // corsConfiguration.addAllowedHeader("Accept");

        // Set headers exposed to client JavaScript
        corsConfiguration.addExposedHeader("Content-Type");
        corsConfiguration.addExposedHeader("X-Request-ID");         // Tracing
        corsConfiguration.addExposedHeader("X-Total-Count");        // Pagination
        corsConfiguration.addExposedHeader("X-RateLimit-Limit");    // Rate limiting info
        corsConfiguration.addExposedHeader("X-RateLimit-Remaining");
        corsConfiguration.addExposedHeader("X-RateLimit-Reset");

        // Allow credentials (cookies, authorization headers)
        // Note: Cannot use wildcard (*) for origins when credentials are allowed
        corsConfiguration.setAllowCredentials(true);

        // Max age for preflight cache (in seconds)
        // Browser caches preflight response to reduce requests
        corsConfiguration.setMaxAge(3600L);  // 1 hour

        // Apply configuration to all paths
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfiguration);

        log.debug("CORS configuration applied: allowed origins={}, allowed methods={}, max age=3600s",
                corsConfiguration.getAllowedOrigins(), corsConfiguration.getAllowedMethods());

        return new CorsWebFilter(source);
    }

    /**
     * Path-based key resolver for rate limiting.
     *
     * This key resolver extracts the API path from each request and uses it
     * as the rate limiting key. This means:
     * - Each route has independent rate limits
     * - All requests to /api/v1/orders/** count against the same limit
     * - Requests to different routes don't interfere with each other
     *
     * Example:
     * ────────
     * Request 1: POST /api/v1/orders/create → Key: /api/v1/orders/**
     * Request 2: POST /api/v1/orders/create → Key: /api/v1/orders/** (same limit)
     * Request 3: GET /api/v1/settlements/123 → Key: /api/v1/settlements/** (different limit)
     *
     * Rate Limit Configuration (from application.yml):
     * ─────────────────────────────────────────────────
     * - Replenish Rate: 100 requests/second (steady-state throughput)
     * - Burst Capacity: 200 requests (allows bursts)
     *
     * Token Bucket Algorithm:
     * ───────────────────────
     * 1. Bucket starts with 200 tokens (burst capacity)
     * 2. For each request, consume 1 token
     * 3. Bucket replenishes 100 tokens/second (rate limit)
     * 4. If bucket is empty, request is rejected (429 Too Many Requests)
     *
     * Example Timeline:
     * ──────────────────
     * Time 0.0s: Bucket has 200 tokens (max)
     * Time 0.1s: 10 requests processed, 190 tokens remaining
     * Time 0.2s: 10 requests processed, 180 tokens remaining
     * Time 1.0s: Bucket replenished to 200 tokens (100 replenished - 10 used)
     *
     * Future Enhancement:
     * ───────────────────
     * Current: Path-based limiting
     * Future: Per-user/per-API-key limiting
     *
     * Example (per-user):
     *   String userId = request.getHeader("X-User-ID");
     *   return Mono.just(userId);
     *
     * This would allow different rate limits for different users:
     * - Free tier: 100 requests/hour
     * - Premium tier: 10,000 requests/hour
     * - Enterprise: Unlimited
     *
     * @return Mono containing the rate limit key for the request
     */
    @Bean
    public KeyResolver pathKeyResolver() {
        log.info("Configuring path-based rate limiting key resolver");

        return exchange -> {
            // Extract request path from exchange
            String path = exchange.getRequest().getPath().value();

            log.debug("Rate limiting key resolved for path: {}", path);

            // Return the path as the rate limiting key
            // Mono.just() wraps the value in a reactive Mono
            return Mono.just(path);
        };
    }

    /**
     * Future: Additional gateway configuration hooks
     *
     * Placeholder for future enhancements:
     * - Custom request/response logging filters
     * - Request correlation ID propagation
     * - Distributed tracing setup
     * - Custom exception handling
     * - Request validation
     * - Response compression configuration
     * - Circuit breaker configuration
     * - Timeout management
     *
     * Examples (to be implemented):
     *
     * 1. Add X-Request-ID header to all requests:
     *    @Bean
     *    public RouteFilter requestIdFilter() { ... }
     *
     * 2. Add distributed tracing:
     *    @Bean
     *    public TracingFilter tracingFilter() { ... }
     *
     * 3. Custom error handling:
     *    @Bean
     *    public ErrorHandler customErrorHandler() { ... }
     */
}
