package goldmansachs.user.web;

import goldmansachs.user.domain.User;
import goldmansachs.user.domain.UserAuditTrail;
import goldmansachs.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * UserController for handling user management endpoints.
 * 
 * This controller provides endpoints for:
 * - Getting user information
 * - Updating user details
 * - Managing user account status (activate, deactivate, suspend)
 * - Viewing user audit trails
 * 
 * All endpoints require authentication via JWT token.
 * 
 * @author Michael Inyang
 * @version 1.0
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {
    
    private final UserService userService;
    
    /**
     * Get all active users.
     * 
     * Endpoint: GET /api/v1/users
     * 
     * @return ResponseEntity containing list of all active users
     */
    @GetMapping
    public ResponseEntity<?> getAllActiveUsers() {
        log.debug("Fetching all active users");
        
        try {
            List<User> users = userService.getAllActiveUsers();
            log.debug("Retrieved {} active users", users.size());
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            log.error("Error fetching active users", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Failed to fetch users: " + e.getMessage()));
        }
    }
    
    /**
     * Get a user by ID.
     * 
     * Endpoint: GET /api/v1/users/{userId}
     * 
     * @param userId the user ID to retrieve
     * @return ResponseEntity containing user details
     */
    @GetMapping("/{userId}")
    public ResponseEntity<?> getUserById(@PathVariable Long userId) {
        log.debug("Fetching user: {}", userId);
        
        try {
            Optional<User> user = userService.getUserById(userId);
            
            if (user.isEmpty()) {
                log.warn("User not found: {}", userId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ErrorResponse("User not found"));
            }
            
            log.debug("User retrieved: {}", userId);
            return ResponseEntity.ok(user.get());
        } catch (Exception e) {
            log.error("Error fetching user", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Failed to fetch user: " + e.getMessage()));
        }
    }
    
    /**
     * Get a user by username.
     * 
     * Endpoint: GET /api/v1/users/username/{username}
     * 
     * @param username the username to retrieve
     * @return ResponseEntity containing user details
     */
    @GetMapping("/username/{username}")
    public ResponseEntity<?> getUserByUsername(@PathVariable String username) {
        log.debug("Fetching user by username: {}", username);
        
        try {
            Optional<User> user = userService.getUserByUsername(username);
            
            if (user.isEmpty()) {
                log.warn("User not found: {}", username);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ErrorResponse("User not found"));
            }
            
            log.debug("User retrieved: {}", username);
            return ResponseEntity.ok(user.get());
        } catch (Exception e) {
            log.error("Error fetching user", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Failed to fetch user: " + e.getMessage()));
        }
    }
    
    /**
     * Update user information.
     * 
     * Endpoint: PATCH /api/v1/users/{userId}
     * 
     * @param userId the user ID to update
     * @param updateRequest the update request containing new user details
     * @return ResponseEntity containing updated user
     */
    @PatchMapping("/{userId}")
    public ResponseEntity<?> updateUser(
            @PathVariable Long userId,
            @RequestBody UpdateUserRequest updateRequest) {
        log.info("Updating user: {}", userId);
        
        try {
            User updatedUser = userService.updateUser(
                    userId,
                    updateRequest.getFirstName(),
                    updateRequest.getLastName(),
                    updateRequest.getEmail()
            );
            
            log.info("User updated successfully: {}", userId);
            return ResponseEntity.ok(updatedUser);
        } catch (UserService.UserNotFoundException e) {
            log.warn("User not found: {}", userId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Error updating user", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Failed to update user: " + e.getMessage()));
        }
    }
    
    /**
     * Activate a user account.
     * 
     * Endpoint: POST /api/v1/users/{userId}/activate
     * 
     * @param userId the user ID to activate
     * @return ResponseEntity containing activated user
     */
    @PostMapping("/{userId}/activate")
    public ResponseEntity<?> activateUser(@PathVariable Long userId) {
        log.info("Activating user: {}", userId);
        
        try {
            User activatedUser = userService.activateUser(userId);
            log.info("User activated successfully: {}", userId);
            return ResponseEntity.ok(activatedUser);
        } catch (UserService.UserNotFoundException e) {
            log.warn("User not found: {}", userId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Error activating user", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Failed to activate user: " + e.getMessage()));
        }
    }
    
    /**
     * Deactivate a user account.
     * 
     * Endpoint: POST /api/v1/users/{userId}/deactivate
     * 
     * @param userId the user ID to deactivate
     * @return ResponseEntity containing deactivated user
     */
    @PostMapping("/{userId}/deactivate")
    public ResponseEntity<?> deactivateUser(@PathVariable Long userId) {
        log.info("Deactivating user: {}", userId);
        
        try {
            User deactivatedUser = userService.deactivateUser(userId);
            log.info("User deactivated successfully: {}", userId);
            return ResponseEntity.ok(deactivatedUser);
        } catch (UserService.UserNotFoundException e) {
            log.warn("User not found: {}", userId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Error deactivating user", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Failed to deactivate user: " + e.getMessage()));
        }
    }
    
    /**
     * Suspend a user account.
     * 
     * Endpoint: POST /api/v1/users/{userId}/suspend
     * 
     * @param userId the user ID to suspend
     * @param suspendRequest the suspend request containing reason
     * @return ResponseEntity containing suspended user
     */
    @PostMapping("/{userId}/suspend")
    public ResponseEntity<?> suspendUser(
            @PathVariable Long userId,
            @RequestBody SuspendRequest suspendRequest) {
        log.info("Suspending user: {}", userId);
        
        try {
            User suspendedUser = userService.suspendUser(userId, suspendRequest.getReason());
            log.info("User suspended successfully: {}", userId);
            return ResponseEntity.ok(suspendedUser);
        } catch (UserService.UserNotFoundException e) {
            log.warn("User not found: {}", userId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Error suspending user", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Failed to suspend user: " + e.getMessage()));
        }
    }
    
    /**
     * Unsuspend a user account.
     * 
     * Endpoint: POST /api/v1/users/{userId}/unsuspend
     * 
     * @param userId the user ID to unsuspend
     * @return ResponseEntity containing unsuspended user
     */
    @PostMapping("/{userId}/unsuspend")
    public ResponseEntity<?> unsuspendUser(@PathVariable Long userId) {
        log.info("Unsuspending user: {}", userId);
        
        try {
            User unsuspendedUser = userService.unsuspendUser(userId);
            log.info("User unsuspended successfully: {}", userId);
            return ResponseEntity.ok(unsuspendedUser);
        } catch (UserService.UserNotFoundException e) {
            log.warn("User not found: {}", userId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Error unsuspending user", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Failed to unsuspend user: " + e.getMessage()));
        }
    }
    
    /**
     * Delete a user account (soft delete).
     * 
     * Endpoint: DELETE /api/v1/users/{userId}
     * 
     * @param userId the user ID to delete
     * @param deleteRequest the delete request containing reason
     * @return ResponseEntity with deletion confirmation
     */
    @DeleteMapping("/{userId}")
    public ResponseEntity<?> deleteUser(
            @PathVariable Long userId,
            @RequestBody DeleteRequest deleteRequest) {
        log.info("Deleting user: {}", userId);
        
        try {
            userService.deleteUser(userId, deleteRequest.getReason());
            log.info("User deleted successfully (soft delete): {}", userId);
            return ResponseEntity.ok(new SuccessResponse("User deleted successfully"));
        } catch (UserService.UserNotFoundException e) {
            log.warn("User not found: {}", userId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Error deleting user", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Failed to delete user: " + e.getMessage()));
        }
    }
    
    /**
     * Get user audit trail.
     * 
     * Endpoint: GET /api/v1/users/{userId}/audit-trail
     * 
     * @param userId the user ID to get audit trail for
     * @return ResponseEntity containing audit trail entries
     */
    @GetMapping("/{userId}/audit-trail")
    public ResponseEntity<?> getUserAuditTrail(@PathVariable Long userId) {
        log.debug("Fetching audit trail for user: {}", userId);
        
        try {
            List<UserAuditTrail> auditTrail = userService.getUserAuditTrail(userId);
            log.debug("Retrieved {} audit trail entries for user: {}", auditTrail.size(), userId);
            return ResponseEntity.ok(auditTrail);
        } catch (Exception e) {
            log.error("Error fetching audit trail", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Failed to fetch audit trail: " + e.getMessage()));
        }
    }
    
    /**
     * Get user audit trail within a date range.
     * 
     * Endpoint: GET /api/v1/users/{userId}/audit-trail?startDate=&endDate=
     * 
     * @param userId the user ID to get audit trail for
     * @param startDate the start date (ISO format)
     * @param endDate the end date (ISO format)
     * @return ResponseEntity containing audit trail entries
     */
    @GetMapping("/{userId}/audit-trail-range")
    public ResponseEntity<?> getUserAuditTrailByDateRange(
            @PathVariable Long userId,
            @RequestParam String startDate,
            @RequestParam String endDate) {
        log.debug("Fetching audit trail for user {} between {} and {}", userId, startDate, endDate);
        
        try {
            LocalDateTime start = LocalDateTime.parse(startDate);
            LocalDateTime end = LocalDateTime.parse(endDate);
            
            List<UserAuditTrail> auditTrail = userService.getUserAuditTrail(userId, start, end);
            log.debug("Retrieved {} audit trail entries for user {} in date range", auditTrail.size(), userId);
            return ResponseEntity.ok(auditTrail);
        } catch (Exception e) {
            log.error("Error fetching audit trail by date range", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Failed to fetch audit trail: " + e.getMessage()));
        }
    }
    
    /**
     * Get the count of active users.
     * 
     * Endpoint: GET /api/v1/users/count/active
     * 
     * @return ResponseEntity containing user count
     */
    @GetMapping("/count/active")
    public ResponseEntity<?> getActiveUserCount() {
        log.debug("Fetching active user count");
        
        try {
            long count = userService.getActiveUserCount();
            return ResponseEntity.ok(new UserCountResponse(count));
        } catch (Exception e) {
            log.error("Error fetching user count", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Failed to fetch user count: " + e.getMessage()));
        }
    }
    
    /**
     * Request for updating user information.
     */
    public static class UpdateUserRequest {
        public String firstName;
        public String lastName;
        public String email;
        
        public String getFirstName() { return firstName; }
        public void setFirstName(String firstName) { this.firstName = firstName; }
        
        public String getLastName() { return lastName; }
        public void setLastName(String lastName) { this.lastName = lastName; }
        
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }
    
    /**
     * Request for suspending a user.
     */
    public static class SuspendRequest {
        public String reason;
        
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }
    
    /**
     * Request for deleting a user.
     */
    public static class DeleteRequest {
        public String reason;
        
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }
    
    /**
     * Response containing user count.
     */
    public static class UserCountResponse {
        public long count;
        
        public UserCountResponse(long count) {
            this.count = count;
        }
        
        public long getCount() { return count; }
        public void setCount(long count) { this.count = count; }
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
