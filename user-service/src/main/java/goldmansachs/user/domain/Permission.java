package goldmansachs.user.domain;

import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Permission entity representing a system permission for fine-grained access control.
 * 
 * Permissions are assigned to roles, which are then assigned to users.
 * This provides flexible role-based access control (RBAC).
 * 
 * @author Michael Inyang
 * @version 1.0
 */
@Entity
@Table(name = "permissions", indexes = {
    @Index(name = "idx_permission_name", columnList = "name", unique = true)
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Permission {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * Unique name of the permission.
     * Uses dot notation: resource.action
     * Examples: orders.create, orders.view, users.manage, etc.
     */
    @Column(nullable = false, unique = true, length = 100)
    private String name;
    
    /**
     * Human-readable description of what this permission allows.
     */
    @Column(length = 255)
    private String description;
    
    /**
     * Resource category this permission applies to.
     * Examples: orders, users, settlements, etc.
     */
    @Column(length = 50)
    private String resource;
    
    /**
     * Action this permission allows on the resource.
     * Examples: create, read, update, delete, manage, etc.
     */
    @Column(length = 50)
    private String action;
    
    /**
     * Timestamp when the permission was created.
     * Automatically set on entity persistence.
     */
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    /**
     * Whether this permission is currently active.
     * Inactive permissions are not assigned to new roles.
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;
}
