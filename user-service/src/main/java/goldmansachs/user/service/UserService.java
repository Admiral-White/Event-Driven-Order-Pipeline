package goldmansachs.user.service;

import goldmansachs.user.domain.User;
import goldmansachs.user.domain.UserAuditTrail;
import goldmansachs.user.repository.UserRepository;
import goldmansachs.user.repository.UserAuditTrailRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * UserService for managing user accounts and operations.
 * 
 * This service handles:
 * - User CRUD operations
 * - User activation, deactivation, suspension
 * - User status management
 * - Comprehensive audit trail logging
 * 
 * All operations are transactional and logged to the audit trail.
 * 
 * @author Michael Inyang
 * @version 1.0
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {
    
    private final UserRepository userRepository;
    private final UserAuditTrailRepository auditTrailRepository;
    
    /**
     * Get a user by ID.
     * 
     * @param userId the user ID to retrieve
     * @return Optional containing the user if found
     */
    @Transactional(readOnly = true)
    public Optional<User> getUserById(Long userId) {
        log.debug("Retrieving user with ID: {}", userId);
        Optional<User> user = userRepository.findById(userId);
        user.ifPresent(u -> log.debug("User found: {}", u.getUsername()));
        return user;
    }
    
    /**
     * Get a user by username.
     * 
     * @param username the username to retrieve
     * @return Optional containing the user if found
     */
    @Transactional(readOnly = true)
    public Optional<User> getUserByUsername(String username) {
        log.debug("Retrieving user by username: {}", username);
        Optional<User> user = userRepository.findByUsername(username);
        if (user.isEmpty()) {
            log.debug("User not found: {}", username);
        }
        return user;
    }
    
    /**
     * Get a user by email.
     * 
     * @param email the email to retrieve
     * @return Optional containing the user if found
     */
    @Transactional(readOnly = true)
    public Optional<User> getUserByEmail(String email) {
        log.debug("Retrieving user by email: {}", email);
        Optional<User> user = userRepository.findByEmail(email);
        if (user.isEmpty()) {
            log.debug("User not found with email: {}", email);
        }
        return user;
    }
    
    /**
     * Get all active users.
     * 
     * @return list of all active (non-deleted) users
     */
    @Transactional(readOnly = true)
    public List<User> getAllActiveUsers() {
        log.debug("Retrieving all active users");
        return userRepository.findAllActive();
    }
    
    /**
     * Get all users with a specific status.
     * 
     * @param status the user status to filter by
     * @return list of users with the specified status
     */
    @Transactional(readOnly = true)
    public List<User> getUsersByStatus(User.UserStatus status) {
        log.debug("Retrieving users with status: {}", status);
        return userRepository.findByStatus(status);
    }
    
    /**
     * Get all users with a specific role.
     * 
     * @param roleId the role ID to filter by
     * @return list of users with the specified role
     */
    @Transactional(readOnly = true)
    public List<User> getUsersByRole(Long roleId) {
        log.debug("Retrieving users with role ID: {}", roleId);
        return userRepository.findByRoleId(roleId);
    }
    
    /**
     * Update user information.
     * 
     * @param userId the ID of the user to update
     * @param firstName the new first name
     * @param lastName the new last name
     * @param email the new email address
     * @return the updated user
     * @throws UserNotFoundException if user is not found
     */
    @Transactional
    public User updateUser(Long userId, String firstName, String lastName, String email) {
        log.info("Updating user: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));
        
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(email);
        
        User updatedUser = userRepository.save(user);
        
        logAuditTrail(userId, UserAuditTrail.AuditAction.USER_UPDATED.name(),
                "User information updated: firstName=" + firstName + ", lastName=" + lastName + ", email=" + email);
        
        log.info("User updated successfully: {}", userId);
        return updatedUser;
    }
    
    /**
     * Activate a user account.
     * 
     * @param userId the ID of the user to activate
     * @return the activated user
     * @throws UserNotFoundException if user is not found
     */
    @Transactional
    public User activateUser(Long userId) {
        log.info("Activating user: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));
        
        user.setStatus(User.UserStatus.ACTIVE);
        User activatedUser = userRepository.save(user);
        
        logAuditTrail(userId, UserAuditTrail.AuditAction.ACCOUNT_ACTIVATION.name(),
                "User account activated");
        
        log.info("User activated successfully: {}", userId);
        return activatedUser;
    }
    
    /**
     * Deactivate a user account.
     * 
     * @param userId the ID of the user to deactivate
     * @return the deactivated user
     * @throws UserNotFoundException if user is not found
     */
    @Transactional
    public User deactivateUser(Long userId) {
        log.info("Deactivating user: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));
        
        user.setStatus(User.UserStatus.INACTIVE);
        User deactivatedUser = userRepository.save(user);
        
        logAuditTrail(userId, UserAuditTrail.AuditAction.ACCOUNT_DEACTIVATION.name(),
                "User account deactivated");
        
        log.info("User deactivated successfully: {}", userId);
        return deactivatedUser;
    }
    
    /**
     * Suspend a user account.
     * 
     * @param userId the ID of the user to suspend
     * @param reason the reason for suspension
     * @return the suspended user
     * @throws UserNotFoundException if user is not found
     */
    @Transactional
    public User suspendUser(Long userId, String reason) {
        log.info("Suspending user: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));
        
        user.setStatus(User.UserStatus.SUSPENDED);
        User suspendedUser = userRepository.save(user);
        
        logAuditTrail(userId, UserAuditTrail.AuditAction.ACCOUNT_SUSPENSION.name(),
                "User account suspended. Reason: " + reason);
        
        log.info("User suspended successfully: {}", userId);
        return suspendedUser;
    }
    
    /**
     * Unsuspend a user account.
     * 
     * @param userId the ID of the user to unsuspend
     * @return the unsuspended user
     * @throws UserNotFoundException if user is not found
     */
    @Transactional
    public User unsuspendUser(Long userId) {
        log.info("Unsuspending user: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));
        
        user.setStatus(User.UserStatus.ACTIVE);
        User unsuspendedUser = userRepository.save(user);
        
        logAuditTrail(userId, UserAuditTrail.AuditAction.ACCOUNT_UNSUSPENSION.name(),
                "User account unsuspended");
        
        log.info("User unsuspended successfully: {}", userId);
        return unsuspendedUser;
    }
    
    /**
     * Soft delete a user account.
     * The user record is not physically deleted but marked as deleted with a timestamp.
     * 
     * @param userId the ID of the user to delete
     * @param reason the reason for deletion
     * @return the deleted user
     * @throws UserNotFoundException if user is not found
     */
    @Transactional
    public User deleteUser(Long userId, String reason) {
        log.info("Deleting user (soft delete): {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));
        
        user.setStatus(User.UserStatus.DELETED);
        user.setDeletedAt(LocalDateTime.now());
        user.setDeletionReason(reason);
        
        User deletedUser = userRepository.save(user);
        
        logAuditTrail(userId, UserAuditTrail.AuditAction.USER_DELETED.name(),
                "User account deleted (soft delete). Reason: " + reason);
        
        log.info("User deleted successfully (soft delete): {}", userId);
        return deletedUser;
    }
    
    /**
     * Check if a username is available (not taken and not deleted).
     * 
     * @param username the username to check
     * @return true if username is available, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean isUsernameAvailable(String username) {
        return !userRepository.existsByUsernameAndNotDeleted(username);
    }
    
    /**
     * Check if an email is available (not taken and not deleted).
     * 
     * @param email the email to check
     * @return true if email is available, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean isEmailAvailable(String email) {
        return !userRepository.existsByEmailAndNotDeleted(email);
    }
    
    /**
     * Get the count of active users.
     * 
     * @return the number of active users
     */
    @Transactional(readOnly = true)
    public long getActiveUserCount() {
        return userRepository.countActiveUsers();
    }
    
    /**
     * Get audit trail for a specific user.
     * 
     * @param userId the user ID to get audit trail for
     * @return list of audit trail entries for the user
     */
    @Transactional(readOnly = true)
    public List<UserAuditTrail> getUserAuditTrail(Long userId) {
        log.debug("Retrieving audit trail for user: {}", userId);
        return auditTrailRepository.findByUserIdOrderByTimestampDesc(userId);
    }
    
    /**
     * Get audit trail for a specific user within a date range.
     * 
     * @param userId the user ID to get audit trail for
     * @param startDate the start of the date range
     * @param endDate the end of the date range
     * @return list of audit trail entries within the date range
     */
    @Transactional(readOnly = true)
    public List<UserAuditTrail> getUserAuditTrail(Long userId, LocalDateTime startDate, LocalDateTime endDate) {
        log.debug("Retrieving audit trail for user {} between {} and {}", userId, startDate, endDate);
        return auditTrailRepository.findByUserIdAndTimestampBetween(userId, startDate, endDate);
    }
    
    /**
     * Log an audit trail entry.
     * This method should be called for all significant user actions.
     * 
     * @param userId the user ID to log for
     * @param action the action that occurred
     * @param details detailed information about the action
     */
    @Transactional
    public void logAuditTrail(Long userId, String action, String details) {
        UserAuditTrail auditEntry = UserAuditTrail.builder()
                .userId(userId)
                .action(action)
                .details(details)
                .timestamp(LocalDateTime.now())
                .build();
        
        auditTrailRepository.save(auditEntry);
        log.debug("Audit trail logged for user {}: {} - {}", userId, action, details);
    }
    
    /**
     * Log an audit trail entry with IP address.
     * 
     * @param userId the user ID to log for
     * @param action the action that occurred
     * @param details detailed information about the action
     * @param ipAddress the IP address from which the action was performed
     * @param userAgent the user agent/application from which the action was performed
     */
    @Transactional
    public void logAuditTrail(Long userId, String action, String details, String ipAddress, String userAgent) {
        UserAuditTrail auditEntry = UserAuditTrail.builder()
                .userId(userId)
                .action(action)
                .details(details)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .timestamp(LocalDateTime.now())
                .build();
        
        auditTrailRepository.save(auditEntry);
        log.debug("Audit trail logged for user {}: {} - {} from IP: {}", userId, action, details, ipAddress);
    }
    
    /**
     * Custom exception for user not found scenarios.
     */
    public static class UserNotFoundException extends RuntimeException {
        public UserNotFoundException(String message) {
            super(message);
        }
    }
}
