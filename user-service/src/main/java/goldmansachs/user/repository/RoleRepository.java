package goldmansachs.user.repository;

import goldmansachs.user.domain.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Role entity providing database access operations.
 * 
 * This repository handles CRUD operations and custom queries for Role entities.
 * Roles are used to group permissions and assign them to users.
 * 
 * @author Michael Inyang
 * @version 1.0
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    
    /**
     * Find a role by name.
     * Role names are unique across the system.
     * 
     * @param name the role name to search for
     * @return Optional containing the role if found, empty otherwise
     */
    Optional<Role> findByName(String name);
    
    /**
     * Find all active roles.
     * Inactive roles should not be assigned to new users.
     * 
     * @return list of all active roles
     */
    @Query("SELECT r FROM Role r WHERE r.active = true")
    List<Role> findAllActive();
    
    /**
     * Find all inactive roles.
     * 
     * @return list of all inactive roles
     */
    @Query("SELECT r FROM Role r WHERE r.active = false")
    List<Role> findAllInactive();
    
    /**
     * Find all roles with a specific permission.
     * Useful for finding which roles have a specific permission.
     * 
     * @param permissionId the permission ID to filter by
     * @return list of roles that have the specified permission
     */
    @Query("SELECT r FROM Role r JOIN r.permissions p WHERE p.id = :permissionId")
    List<Role> findByPermissionId(@Param("permissionId") Long permissionId);
    
    /**
     * Check if a role exists by name.
     * 
     * @param name the role name to check
     * @return true if role exists, false otherwise
     */
    boolean existsByName(String name);
}
