package goldmansachs.user.domain;

import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Role entity representing a system role for role-based access control (RBAC).
 * 
 * Roles are associated with permissions to provide flexible authorization.
 * Each user has exactly one role, and each role can have multiple permissions.
 * 
 * @author Michael Inyang
 * @version 1.0
 */
@Entity
@Table(name = "roles", indexes = {
    @Index(name = "idx_role_name", columnList = "name", unique = true)
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * Unique name of the role.
     * Examples: ADMIN, USER, MERCHANT, etc.
     */
    @Column(nullable = false, unique = true, length = 50)
    private String name;
    
    /**
     * Human-readable description of the role's purpose.
     */
    @Column(length = 255)
    private String description;
    
    /**
     * Many-to-Many relationship with Permission.
     * A role can have multiple permissions, and a permission can belong to multiple roles.
     */
    @ManyToMany(fetch = FetchType.EAGER, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
        name = "role_permissions",
        joinColumns = @JoinColumn(name = "role_id"),
        inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    @Builder.Default
    private Set<Permission> permissions = new HashSet<>();
    
    /**
     * Timestamp when the role was created.
     * Automatically set on entity persistence.
     */
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    /**
     * Whether this role is active and available for assignment.
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;
    
    /**
     * Adds a permission to this role.
     * 
     * @param permission the permission to add
     */
    public void addPermission(Permission permission) {
        if (permissions == null) {
            permissions = new HashSet<>();
        }
        permissions.add(permission);
    }
    
    /**
     * Removes a permission from this role.
     * 
     * @param permission the permission to remove
     */
    public void removePermission(Permission permission) {
        if (permissions != null) {
            permissions.remove(permission);
        }
    }
    
    /**
     * Checks if this role has a specific permission.
     * 
     * @param permissionName the name of the permission to check
     * @return true if the role has the permission, false otherwise
     */
    public boolean hasPermission(String permissionName) {
        if (permissions == null) {
            return false;
        }
        return permissions.stream()
                .anyMatch(p -> p.getName().equals(permissionName));
    }
}
