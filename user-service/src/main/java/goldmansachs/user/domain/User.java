package goldmansachs.user.domain;

import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * User entity representing a system user.
 * 
 * This entity stores user account information including authentication credentials,
 * personal details, and account status. It uses soft delete (deleted_at field) to
 * maintain data integrity and audit trails.
 * 
 * @author Michael Inyang
 * @version 1.0
 */
@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_username", columnList = "username", unique = true),
    @Index(name = "idx_email", columnList = "email", unique = true),
    @Index(name = "idx_status", columnList = "status")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * Unique username for login purposes.
     * Must be unique across all users in the system.
     */
    @Column(nullable = false, unique = true, length = 50)
    private String username;
    
    /**
     * Email address of the user.
     * Must be unique and used for communication.
     */
    @Column(nullable = false, unique = true, length = 100)
    private String email;
    
    /**
     * Hashed password using bcrypt algorithm.
     * Never stored in plain text for security purposes.
     */
    @Column(nullable = false)
    private String passwordHash;
    
    /**
     * User's first name.
     */
    @Column(length = 100)
    private String firstName;
    
    /**
     * User's last name.
     */
    @Column(length = 100)
    private String lastName;
    
    /**
     * Current status of the user account.
     * @see UserStatus
     */
    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private UserStatus status;
    
    /**
     * Many-to-One relationship with Role.
     * Each user has exactly one role for RBAC.
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;
    
    /**
     * Timestamp when the user account was created.
     * Automatically set on entity persistence.
     */
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    /**
     * Timestamp when the user account was last updated.
     * Automatically updated on entity modifications.
     */
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    /**
     * Timestamp when the user account was deleted (soft delete).
     * NULL indicates an active account. Non-NULL indicates deletion.
     */
    @Column
    private LocalDateTime deletedAt;
    
    /**
     * Reason for account deletion (soft delete).
     * Optional field used to track why an account was deleted.
     */
    @Column(length = 255)
    private String deletionReason;
    
    /**
     * Enum representing possible user account statuses.
     */
    public enum UserStatus {
        /** User account is active and can login */
        ACTIVE,
        /** User account is inactive and cannot login */
        INACTIVE,
        /** User account is suspended and cannot login */
        SUSPENDED,
        /** User account has been deleted (soft delete) */
        DELETED
    }
    
    /**
     * JPA lifecycle callback to set timestamps before persistence.
     * Ensures createdAt and updatedAt are properly initialized.
     */
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
    }
    
    /**
     * JPA lifecycle callback to update timestamp before entity update.
     * Ensures updatedAt is always current when changes are persisted.
     */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
