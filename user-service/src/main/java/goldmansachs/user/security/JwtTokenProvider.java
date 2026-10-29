package goldmansachs.user.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT Token Provider for generating and validating JSON Web Tokens.
 * 
 * This component handles all JWT operations including:
 * - Token generation with custom claims
 * - Token validation
 * - Token expiration management
 * - Extracting claims from tokens
 * 
 * JWT tokens are used for stateless authentication in the User Service.
 * Token structure: Header.Payload.Signature
 * 
 * @author Michael Inyang
 * @version 1.0
 */
@Component
@Slf4j
public class JwtTokenProvider {
    
    /**
     * JWT secret key for signing tokens.
     * Injected from application configuration.
     * Must be at least 256 bits (32 characters) for HS256.
     */
    @Value("${jwt.secret-key:your-secret-key-change-this-in-production-at-least-256-bits}")
    private String secretKey;
    
    /**
     * JWT token expiration time in milliseconds.
     * Injected from application configuration (default: 24 hours).
     */
    @Value("${jwt.expiration-ms:86400000}")
    private long jwtExpirationMs;
    
    /**
     * Cache for the SecretKey to avoid recreating it on every operation.
     */
    private SecretKey key;
    
    /**
     * Gets the SecretKey, creating it if necessary.
     * 
     * @return the SecretKey used for token signing
     */
    private SecretKey getSigningKey() {
        if (key == null) {
            key = Keys.hmacShaKeyFor(secretKey.getBytes());
        }
        return key;
    }
    
    /**
     * Generate a JWT token for a user.
     * 
     * The token includes:
     * - User ID as the subject (sub)
     * - User username
     * - User role
     * - User permissions (from role)
     * - Token issue time
     * - Token expiration time
     * 
     * @param userId the user ID to include in the token
     * @param username the username to include in the token
     * @param roleName the role name to include in the token
     * @param permissions comma-separated list of permission names
     * @return the generated JWT token
     */
    public String generateToken(Long userId, String username, String roleName, String permissions) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        claims.put("role", roleName);
        claims.put("permissions", permissions);
        
        return createToken(claims, userId.toString());
    }
    
    /**
     * Generate a refresh token for a user.
     * Refresh tokens have a longer expiration time than access tokens.
     * 
     * @param userId the user ID to include in the token
     * @param username the username to include in the token
     * @return the generated refresh JWT token
     */
    public String generateRefreshToken(Long userId, String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        claims.put("tokenType", "REFRESH");
        
        Date now = new Date();
        // Refresh token expires in 7 days
        Date expiryDate = new Date(now.getTime() + (jwtExpirationMs * 7));
        
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(userId.toString())
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }
    
    /**
     * Creates a JWT token with the given claims and subject.
     * 
     * @param claims the claims to include in the token
     * @param subject the subject (user ID) for the token
     * @return the generated JWT token
     */
    private String createToken(Map<String, Object> claims, String subject) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);
        
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }
    
    /**
     * Extract the user ID from a JWT token.
     * 
     * @param token the JWT token to extract from
     * @return the user ID from the token
     * @throws JwtException if the token is invalid or expired
     */
    public Long extractUserId(String token) {
        try {
            Claims claims = extractAllClaims(token);
            Object userIdObj = claims.get("userId");
            if (userIdObj instanceof Integer) {
                return ((Integer) userIdObj).longValue();
            } else if (userIdObj instanceof Long) {
                return (Long) userIdObj;
            } else if (userIdObj instanceof String) {
                return Long.parseLong((String) userIdObj);
            }
            return null;
        } catch (JwtException e) {
            log.error("Failed to extract user ID from token", e);
            throw e;
        }
    }
    
    /**
     * Extract the username from a JWT token.
     * 
     * @param token the JWT token to extract from
     * @return the username from the token
     * @throws JwtException if the token is invalid or expired
     */
    public String extractUsername(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return (String) claims.get("username");
        } catch (JwtException e) {
            log.error("Failed to extract username from token", e);
            throw e;
        }
    }
    
    /**
     * Extract the role name from a JWT token.
     * 
     * @param token the JWT token to extract from
     * @return the role name from the token
     * @throws JwtException if the token is invalid or expired
     */
    public String extractRole(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return (String) claims.get("role");
        } catch (JwtException e) {
            log.error("Failed to extract role from token", e);
            throw e;
        }
    }
    
    /**
     * Extract permissions from a JWT token.
     * 
     * @param token the JWT token to extract from
     * @return comma-separated list of permissions from the token
     * @throws JwtException if the token is invalid or expired
     */
    public String extractPermissions(String token) {
        try {
            Claims claims = extractAllClaims(token);
            Object permsObj = claims.get("permissions");
            return permsObj != null ? permsObj.toString() : "";
        } catch (JwtException e) {
            log.error("Failed to extract permissions from token", e);
            throw e;
        }
    }
    
    /**
     * Validate a JWT token.
     * Checks signature, expiration, and other standard claims.
     * 
     * @param token the JWT token to validate
     * @return true if the token is valid, false otherwise
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (SecurityException e) {
            log.error("Invalid JWT signature: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.error("Expired JWT token: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.error("Unsupported JWT token: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("JWT claims string is empty: {}", e.getMessage());
        } catch (JwtException e) {
            log.error("JWT validation error: {}", e.getMessage());
        }
        return false;
    }
    
    /**
     * Check if a JWT token is expired.
     * 
     * @param token the JWT token to check
     * @return true if the token is expired, false otherwise
     */
    public boolean isTokenExpired(String token) {
        try {
            Date expiration = extractAllClaims(token).getExpiration();
            return expiration.before(new Date());
        } catch (JwtException e) {
            log.error("Failed to check token expiration", e);
            return true;
        }
    }
    
    /**
     * Extract all claims from a JWT token.
     * 
     * @param token the JWT token to extract claims from
     * @return the Claims object containing all claims from the token
     * @throws JwtException if the token is invalid or expired
     */
    private Claims extractAllClaims(String token) {
        try {
            return Jwts.parser()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException e) {
            log.error("Failed to extract claims from token", e);
            throw e;
        }
    }
    
    /**
     * Get the token expiration time in milliseconds.
     * 
     * @return the JWT expiration time in milliseconds
     */
    public long getJwtExpirationMs() {
        return jwtExpirationMs;
    }
}
