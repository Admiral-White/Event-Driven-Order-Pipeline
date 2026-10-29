package goldmansachs.user.web;

import goldmansachs.user.service.AuthService;
import goldmansachs.user.service.AuthResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * AuthController for handling user authentication endpoints.
 * 
 * This controller provides endpoints for:
 * - User registration
 * - User login
 * - User logout
 * - Token refresh
 * 
 * All requests are validated and errors are handled with appropriate HTTP status codes.
 * 
 * @author Michael Inyang
 * @version 1.0
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {
    
    private final AuthService authService;
    
    /**
     * Register a new user.
     * 
     * Endpoint: POST /api/v1/auth/register
     * 
     * @param registerRequest the registration request containing user details
     * @return ResponseEntity containing AuthResponse with JWT tokens
     * 
     * @throws AuthService.InvalidPasswordException if password doesn't meet requirements
     * @throws AuthService.UsernameAlreadyExistsException if username is already taken
     * @throws AuthService.EmailAlreadyExistsException if email is already taken
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest registerRequest) {
        log.info("Registration request for username: {}", registerRequest.getUsername());
        
        // Validate request
        if (registerRequest.getUsername() == null || registerRequest.getUsername().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Username is required"));
        }
        if (registerRequest.getEmail() == null || registerRequest.getEmail().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Email is required"));
        }
        if (registerRequest.getPassword() == null || registerRequest.getPassword().isEmpty()) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Password is required"));
        }
        
        try {
            AuthResponse authResponse = authService.registerUser(
                    registerRequest.getUsername(),
                    registerRequest.getEmail(),
                    registerRequest.getPassword(),
                    registerRequest.getFirstName(),
                    registerRequest.getLastName()
            );
            
            log.info("User registered successfully: {}", registerRequest.getUsername());
            return ResponseEntity.status(HttpStatus.CREATED).body(authResponse);
            
        } catch (AuthService.InvalidPasswordException e) {
            log.warn("Registration failed - invalid password: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (AuthService.UsernameAlreadyExistsException e) {
            log.warn("Registration failed - username already exists: {}", registerRequest.getUsername());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(e.getMessage()));
        } catch (AuthService.EmailAlreadyExistsException e) {
            log.warn("Registration failed - email already exists: {}", registerRequest.getEmail());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Registration error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Registration failed: " + e.getMessage()));
        }
    }
    
    /**
     * Authenticate a user and obtain JWT tokens.
     * 
     * Endpoint: POST /api/v1/auth/login
     * 
     * @param loginRequest the login request containing username and password
     * @return ResponseEntity containing AuthResponse with JWT tokens
     * 
     * @throws AuthService.UserNotFoundException if user doesn't exist
     * @throws AuthService.InvalidCredentialsException if password is incorrect
     * @throws AuthService.AccountNotActiveException if account is suspended or deleted
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        log.info("Login request for username: {}", loginRequest.getUsername());
        
        // Validate request
        if (loginRequest.getUsername() == null || loginRequest.getUsername().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Username is required"));
        }
        if (loginRequest.getPassword() == null || loginRequest.getPassword().isEmpty()) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Password is required"));
        }
        
        try {
            AuthResponse authResponse = authService.login(
                    loginRequest.getUsername(),
                    loginRequest.getPassword()
            );
            
            log.info("User logged in successfully: {}", loginRequest.getUsername());
            return ResponseEntity.ok(authResponse);
            
        } catch (AuthService.UserNotFoundException e) {
            log.warn("Login failed - user not found: {}", loginRequest.getUsername());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Invalid credentials"));
        } catch (AuthService.InvalidCredentialsException e) {
            log.warn("Login failed - invalid credentials for user: {}", loginRequest.getUsername());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Invalid credentials"));
        } catch (AuthService.AccountNotActiveException e) {
            log.warn("Login failed - account not active: {}", loginRequest.getUsername());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Login error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Login failed: " + e.getMessage()));
        }
    }
    
    /**
     * Logout a user.
     * 
     * Endpoint: POST /api/v1/auth/logout
     * 
     * @param logoutRequest the logout request containing user ID
     * @return ResponseEntity with logout confirmation
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody LogoutRequest logoutRequest) {
        log.info("Logout request for user ID: {}", logoutRequest.getUserId());
        
        try {
            authService.logout(logoutRequest.getUserId(), logoutRequest.getToken());
            log.info("User logged out successfully: {}", logoutRequest.getUserId());
            return ResponseEntity.ok(new SuccessResponse("Logged out successfully"));
            
        } catch (Exception e) {
            log.error("Logout error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Logout failed: " + e.getMessage()));
        }
    }
    
    /**
     * Refresh a user's JWT token.
     * 
     * Endpoint: POST /api/v1/auth/refresh
     * 
     * @param refreshRequest the refresh request containing user details
     * @return ResponseEntity containing AuthResponse with new JWT tokens
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestBody RefreshTokenRequest refreshRequest) {
        log.debug("Token refresh request for user ID: {}", refreshRequest.getUserId());
        
        try {
            AuthResponse authResponse = authService.refreshToken(
                    refreshRequest.getUserId(),
                    refreshRequest.getUsername(),
                    refreshRequest.getRoleName()
            );
            
            log.debug("Token refreshed successfully for user: {}", refreshRequest.getUserId());
            return ResponseEntity.ok(authResponse);
            
        } catch (AuthService.UserNotFoundException e) {
            log.warn("Token refresh failed - user not found: {}", refreshRequest.getUserId());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Token refresh error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Token refresh failed: " + e.getMessage()));
        }
    }
    
    /**
     * Request for user registration.
     */
    public static class RegisterRequest {
        public String username;
        public String email;
        public String password;
        public String firstName;
        public String lastName;
        
        // Getters and setters
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        
        public String getFirstName() { return firstName; }
        public void setFirstName(String firstName) { this.firstName = firstName; }
        
        public String getLastName() { return lastName; }
        public void setLastName(String lastName) { this.lastName = lastName; }
    }
    
    /**
     * Request for user login.
     */
    public static class LoginRequest {
        public String username;
        public String password;
        
        // Getters and setters
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }
    
    /**
     * Request for user logout.
     */
    public static class LogoutRequest {
        public Long userId;
        public String token;
        
        // Getters and setters
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        
        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }
    }
    
    /**
     * Request for token refresh.
     */
    public static class RefreshTokenRequest {
        public Long userId;
        public String username;
        public String roleName;
        
        // Getters and setters
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        
        public String getRoleName() { return roleName; }
        public void setRoleName(String roleName) { this.roleName = roleName; }
    }
    
    /**
     * Generic error response.
     */
    public static class ErrorResponse {
        public String message;
        
        public ErrorResponse(String message) {
            this.message = message;
        }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
    
    /**
     * Generic success response.
     */
    public static class SuccessResponse {
        public String message;
        
        public SuccessResponse(String message) {
            this.message = message;
        }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
}
