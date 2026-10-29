package goldmansachs.user.domain;

import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * UserAuditTrail entity for tracking user-related events and actions.
 * 
 * This entity maintains an audit trail of all significant user actions including:
 * - Login/logout attempts
 * - Password changes
 * - Role changes
 * - Account activations/deactivations
 * - Account deletions
 * - Permission assignments
 * 
 * This provides a complete audit history for security and compliance purposes.
 * 
 * @author Michael Inyang
 * @version 1.0
 */
@Entity
@Table(name = "user_audit_trail", indexes = {
    @Index(name = "idx_audit_user_id", columnList = "user_id"),
    @Index(name = "idx_audit_action", columnList = "action"),
    @Index(name = "idx_audit_timestamp", columnList = "timestamp")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserAuditTrail {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * Reference to the user this audit entry is for.
     * Foreign key relationship to the users table.
     */
    @Column(nullable = false)
    private Long userId;
    
    /**
     * The action that was performed on the user.
     * Examples: USER_CREATED, LOGIN_ATTEMPT, PASSWORD_CHANGED, ROLE_CHANGED, 
     *          ACCOUNT_ACTIVATED, ACCOUNT_SUSPENDED, ACCOUNT_DELETED, etc.
     */
    @Column(nullable = false, length = 100)
    private String action;
    
    /**
     * Detailed description of what happened.
     * Can include context about the change such as old values, new values, reason, etc.
     */
    @Column(columnDefinition = "TEXT")
    private String details;
    
    /**
     * IP address from which the action was performed (if applicable).
     * Useful for security tracking and detecting suspicious activities.
     */
    @Column(length = 45)
    private String ipAddress;
    
    /**
     * User agent or application from which the action was performed.
     */
    @Column(length = 255)
    private String userAgent;
    
    /**
     * Timestamp when this audit entry was created.
     * Automatically set on entity persistence.
     */
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime timestamp;
    
    /**
     * Enum representing possible user audit actions.
     */
    public enum AuditAction {
        /** User account created */
        USER_CREATED,
        /** User account updated */
        USER_UPDATED,
        /** User account deleted (soft delete) */
        USER_DELETED,
        /** User account activation attempt */
        ACCOUNT_ACTIVATION,
        /** User account deactivation */
        ACCOUNT_DEACTIVATION,
        /** User account suspension */
        ACCOUNT_SUSPENSION,
        /** User account unsuspension */
        ACCOUNT_UNSUSPENSION,
        /** Login attempt successful */
        LOGIN_SUCCESS,
        /** Login attempt failed */
        LOGIN_FAILED,
        /** User logout */
        LOGOUT,
        /** Password change */
        PASSWORD_CHANGED,
        /** Role assignment */
        ROLE_ASSIGNED,
        /** Role removal */
        ROLE_REMOVED,
        /** Permission assignment */
        PERMISSION_ASSIGNED,
        /** Permission removal */
        PERMISSION_REMOVED,
        /** Token generation */
        TOKEN_GENERATED,
        /** Token refresh */
        TOKEN_REFRESHED,
        /** Password reset requested */
        PASSWORD_RESET_REQUESTED,
        /** Password reset completed */
        PASSWORD_RESET_COMPLETED
    }
}
