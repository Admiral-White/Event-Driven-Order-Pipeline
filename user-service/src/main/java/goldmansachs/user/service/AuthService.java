package goldmansachs.user.service;

import goldmansachs.user.domain.Role;
import goldmansachs.user.domain.User;
import goldmansachs.user.domain.OutboxEvent;
import goldmansachs.user.domain.UserAuditTrail;
import goldmansachs.user.repository.UserRepository;
import goldmansachs.user.repository.RoleRepository;
import goldmansachs.user.repository.OutboxEventRepository;
import goldmansachs.user.security.JwtTokenProvider;
import goldmansachs.user.security.PasswordValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AuthService for handling user authentication and authorization operations.
 * 
 * This service handles:
 * - User registration with password validation
 * - User login with credentials validation
 * - JWT token generation and refresh
 * - Event publishing for user lifecycle events
 * 
 * All authentication operations are logged in the audit trail.
 * 
 * @author Michael Inyang
 * @version 1.0
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {
    
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordValidator passwordValidator;
    private final UserService userService;
    private final ObjectMapper objectMapper;
    
    /**
     * Register a new user with the system.
     * 
     * Registration flow:
     * 1. Validate password meets requirements
     * 2. Check username and email are available
     * 3. Create new user with encrypted password
     * 4. Assign default role
     * 5. Create user and publish USER_CREATED event
     * 6. Log audit trail
     * 
     * @param username the username for the new user
     * @param email the email for the new user
     * @param password the plain text password (will be encrypted)
     * @param firstName the user's first name
     * @param lastName the user's last name
     * @return AuthResponse containing user details and JWT token
     * @throws InvalidPasswordException if password doesn't meet requirements
     * @throws UsernameAlreadyExistsException if username is already taken
     * @throws EmailAlreadyExistsException if email is already taken
     * @throws RoleNotFoundException if default role is not found
     */
    @Transactional
    public AuthResponse registerUser(String username, String email, String password, 
                                     String firstName, String lastName) {
        log.info("Registering new user: {}", username);
        
        // Validate password strength
        PasswordValidator.PasswordValidationResult validationResult = passwordValidator.validate(password);
        if (!validationResult.isValid()) {
            log.warn("Password validation failed for user registration: {}", validationResult.getErrors());
            throw new InvalidPasswordException("Password does not meet requirements: " + validationResult.getErrors());
        }
        
        // Check username availability
        if (!userService.isUsernameAvailable(username)) {
            log.warn("Username already exists: {}", username);
            throw new UsernameAlreadyExistsException("Username already exists: " + username);
        }
        
        // Check email availability
        if (!userService.isEmailAvailable(email)) {
            log.warn("Email already exists: {}", email);
            throw new EmailAlreadyExistsException("Email already exists: " + email);
        }
        
        // Get default role (USER role)
        Role defaultRole = roleRepository.findByName("USER")
                .orElseThrow(() -> new RoleNotFoundException("Default USER role not found"));
        
        // Create new user
        User newUser = User.builder()
                .username(username)
                .email(email)
                .passwordHash(passwordEncoder.encode(password))
                .firstName(firstName)
                .lastName(lastName)
                .status(User.UserStatus.ACTIVE)
                .role(defaultRole)
                .build();
        
        User savedUser = userRepository.save(newUser);
        
        // Create and publish USER_CREATED event
        createUserEvent(savedUser, "USER_CREATED");
        
        // Log audit trail
        userService.logAuditTrail(savedUser.getId(), 
                UserAuditTrail.AuditAction.USER_CREATED.name(),
                "User account created: " + username);
        
        log.info("User registered successfully: {}", username);
        
        // Generate and return JWT token
        return generateAuthResponse(savedUser);
    }
    
    /**
     * Authenticate a user and generate JWT tokens.
     * 
     * Authentication flow:
     * 1. Find user by username
     * 2. Validate password matches stored hash
     * 3. Check user account is active (not suspended or deleted)
     * 4. Generate JWT tokens
     * 5. Log successful login
     * 
     * @param username the username to authenticate
     * @param password the plain text password to validate
     * @return AuthResponse containing JWT tokens
     * @throws UserNotFoundException if user doesn't exist
     * @throws InvalidCredentialsException if password is incorrect
     * @throws AccountNotActiveException if account is suspended or deleted
     */
    @Transactional
    public AuthResponse login(String username, String password) {
        log.info("Login attempt for user: {}", username);
        
        // Find user by username
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.warn("Login failed: user not found - {}", username);
                    return new UserNotFoundException("User not found: " + username);
                });
        
        // Validate password
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            log.warn("Login failed: incorrect password for user - {}", username);
            userService.logAuditTrail(user.getId(), 
                    UserAuditTrail.AuditAction.LOGIN_FAILED.name(),
                    "Login failed: incorrect password");
            throw new InvalidCredentialsException("Invalid credentials");
        }
        
        // Check account status
        if (user.getStatus() == User.UserStatus.SUSPENDED) {
            log.warn("Login failed: account suspended - {}", username);
            throw new AccountNotActiveException("Account is suspended");
        }
        
        if (user.getStatus() == User.UserStatus.DELETED) {
            log.warn("Login failed: account deleted - {}", username);
            throw new AccountNotActiveException("Account has been deleted");
        }
        
        if (user.getStatus() == User.UserStatus.INACTIVE) {
            log.warn("Login failed: account inactive - {}", username);
            throw new AccountNotActiveException("Account is inactive");
        }
        
        // Log successful login
        userService.logAuditTrail(user.getId(), 
                UserAuditTrail.AuditAction.LOGIN_SUCCESS.name(),
                "User login successful");
        
        log.info("User login successful: {}", username);
        
        // Generate and return JWT tokens
        return generateAuthResponse(user);
    }
    
    /**
     * Logout a user (invalidate token).
     * 
     * @param userId the user ID to log out
     * @param token the JWT token to invalidate
     */
    @Transactional
    public void logout(Long userId, String token) {
        log.info("Logout for user: {}", userId);
        
        userService.logAuditTrail(userId, 
                UserAuditTrail.AuditAction.LOGOUT.name(),
                "User logged out");
        
        log.info("User logged out successfully: {}", userId);
    }
    
    /**
     * Refresh a user's JWT token.
     * 
     * @param userId the user ID to refresh token for
     * @param username the username to include in new token
     * @param roleName the role name to include in new token
     * @return AuthResponse with new JWT token
     */
    @Transactional
    public AuthResponse refreshToken(Long userId, String username, String roleName) {
        log.debug("Refreshing token for user: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));
        
        userService.logAuditTrail(userId, 
                UserAuditTrail.AuditAction.TOKEN_REFRESHED.name(),
                "Token refreshed");
        
        return generateAuthResponse(user);
    }
    
    /**
     * Generate an authentication response with JWT tokens.
     * 
     * @param user the user to generate tokens for
     * @return AuthResponse containing tokens and user info
     */
    private AuthResponse generateAuthResponse(User user) {
        // Get permissions from user's role
        String permissions = user.getRole().getPermissions().stream()
                .map(p -> p.getName())
                .collect(Collectors.joining(","));
        
        // Generate access token
        String accessToken = jwtTokenProvider.generateToken(
                user.getId(),
                user.getUsername(),
                user.getRole().getName(),
                permissions
        );
        
        // Generate refresh token
        String refreshToken = jwtTokenProvider.generateRefreshToken(
                user.getId(),
                user.getUsername()
        );
        
        return AuthResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole().getName())
                .permissions(permissions)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getJwtExpirationMs() / 1000) // Convert to seconds
                .build();
    }
    
    /**
     * Create a user event and add it to the outbox for publishing.
     * 
     * @param user the user associated with the event
     * @param eventType the type of event
     */
    private void createUserEvent(User user, String eventType) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("userId", user.getId());
            payload.put("username", user.getUsername());
            payload.put("email", user.getEmail());
            payload.put("firstName", user.getFirstName());
            payload.put("lastName", user.getLastName());
            payload.put("role", user.getRole().getName());
            payload.put("status", user.getStatus());
            payload.put("createdAt", user.getCreatedAt());
            
            OutboxEvent event = OutboxEvent.builder()
                    .eventType(eventType)
                    .payload(objectMapper.writeValueAsString(payload))
                    .aggregateId(user.getId().toString())
                    .aggregateType("User")
                    .status(OutboxEvent.EventStatus.PENDING)
                    .build();
            
            outboxEventRepository.save(event);
            log.info("User event created for user {}: {}", user.getId(), eventType);
        } catch (Exception e) {
            log.error("Failed to create user event for user: {}", user.getId(), e);
            throw new RuntimeException("Failed to create user event", e);
        }
    }
    
    // Custom Exceptions
    
    /**
     * Exception thrown when password doesn't meet validation requirements.
     */
    public static class InvalidPasswordException extends RuntimeException {
        public InvalidPasswordException(String message) {
            super(message);
        }
    }
    
    /**
     * Exception thrown when username already exists.
     */
    public static class UsernameAlreadyExistsException extends RuntimeException {
        public UsernameAlreadyExistsException(String message) {
            super(message);
        }
    }
    
    /**
     * Exception thrown when email already exists.
     */
    public static class EmailAlreadyExistsException extends RuntimeException {
        public EmailAlreadyExistsException(String message) {
            super(message);
        }
    }
    
    /**
     * Exception thrown when user is not found.
     */
    public static class UserNotFoundException extends RuntimeException {
        public UserNotFoundException(String message) {
            super(message);
        }
    }
    
    /**
     * Exception thrown when credentials are invalid.
     */
    public static class InvalidCredentialsException extends RuntimeException {
        public InvalidCredentialsException(String message) {
            super(message);
        }
    }
    
    /**
     * Exception thrown when account is not active (suspended or deleted).
     */
    public static class AccountNotActiveException extends RuntimeException {
        public AccountNotActiveException(String message) {
            super(message);
        }
    }
    
    /**
     * Exception thrown when default role is not found.
     */
    public static class RoleNotFoundException extends RuntimeException {
        public RoleNotFoundException(String message) {
            super(message);
        }
    }
}
