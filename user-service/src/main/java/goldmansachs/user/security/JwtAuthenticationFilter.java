package goldmansachs.user.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * JWT Authentication Filter for validating JWT tokens in incoming requests.
 * 
 * This filter:
 * - Extracts JWT token from Authorization header
 * - Validates the token signature and expiration
 * - Extracts user information from token claims
 * - Sets up Spring Security authentication context
 * 
 * @author Michael Inyang
 * @version 1.0
 */
@Component
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    
    /**
     * Do filter internal - executes JWT validation for each request.
     * 
     * @param request the HTTP request
     * @param response the HTTP response
     * @param filterChain the filter chain
     * @throws ServletException if a servlet error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            // Extract JWT token from Authorization header
            String jwt = extractJwtFromRequest(request);
            
            if (StringUtils.hasText(jwt) && jwtTokenProvider.validateToken(jwt)) {
                // Extract user information from token
                Long userId = jwtTokenProvider.extractUserId(jwt);
                String username = jwtTokenProvider.extractUsername(jwt);
                String role = jwtTokenProvider.extractRole(jwt);
                String permissions = jwtTokenProvider.extractPermissions(jwt);
                
                // Create authentication token
                UsernamePasswordAuthenticationToken authentication = 
                        new UsernamePasswordAuthenticationToken(
                                username, null, null
                        );
                authentication.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );
                
                // Set authentication in Security Context
                SecurityContextHolder.getContext().setAuthentication(authentication);
                
                log.debug("JWT token validated for user: {}", username);
                
                // Store user info in request attributes for later use
                request.setAttribute("userId", userId);
                request.setAttribute("username", username);
                request.setAttribute("role", role);
                request.setAttribute("permissions", permissions);
            }
        } catch (Exception e) {
            log.debug("Could not set user authentication in security context", e);
        }
        
        filterChain.doFilter(request, response);
    }
    
    /**
     * Extract JWT token from Authorization header.
     * 
     * Expected format: "Bearer <token>"
     * 
     * @param request the HTTP request
     * @return the JWT token if present, null otherwise
     */
    private String extractJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        
        return null;
    }
}
