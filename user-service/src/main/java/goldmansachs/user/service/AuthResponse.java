package goldmansachs.user.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AuthResponse DTO for authentication responses.
 * 
 * Contains user information and JWT tokens returned after successful
 * registration or login.
 * 
 * @author Michael Inyang
 * @version 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {
    
    /**
     * The user ID.
     */
    private Long userId;
    
    /**
     * The username.
     */
    private String username;
    
    /**
     * The email address.
     */
    private String email;
    
    /**
     * The first name.
     */
    private String firstName;
    
    /**
     * The last name.
     */
    private String lastName;
    
    /**
     * The user's role name.
     */
    private String role;
    
    /**
     * Comma-separated list of permissions for the user.
     */
    private String permissions;
    
    /**
     * The JWT access token.
     * Use this token for subsequent API requests.
     */
    private String accessToken;
    
    /**
     * The JWT refresh token.
     * Use this to obtain a new access token when it expires.
     */
    private String refreshToken;
    
    /**
     * The token type (typically "Bearer").
     */
    private String tokenType;
    
    /**
     * Token expiration time in seconds.
     */
    private Long expiresIn;
}
