package goldmansachs.user.repository;

import goldmansachs.user.domain.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Permission entity providing database access operations.
 * 
 * This repository handles CRUD operations and custom queries for Permission entities.
 * Permissions are fine-grained access controls that are grouped into roles.
 * 
 * @author Michael Inyang
 * @version 1.0
 */
@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {
    
    /**
     * Find a permission by name.
     * Permission names are unique across the system and use dot notation (resource.action).
     * 
     * @param name the permission name to search for
     * @return Optional containing the permission if found, empty otherwise
     */
    Optional<Permission> findByName(String name);
    
    /**
     * Find all active permissions.
     * Inactive permissions should not be assigned to new roles.
     * 
     * @return list of all active permissions
     */
    @Query("SELECT p FROM Permission p WHERE p.active = true")
    List<Permission> findAllActive();
    
    /**
     * Find all inactive permissions.
     * 
     * @return list of all inactive permissions
     */
    @Query("SELECT p FROM Permission p WHERE p.active = false")
    List<Permission> findAllInactive();
    
    /**
     * Find all permissions for a specific resource.
     * Useful for finding all available actions on a resource.
     * 
     * @param resource the resource name to filter by
     * @return list of permissions for the specified resource
     */
    @Query("SELECT p FROM Permission p WHERE p.resource = :resource AND p.active = true")
    List<Permission> findByResource(@Param("resource") String resource);
    
    /**
     * Find a permission by resource and action.
     * This is useful for checking if a specific resource.action permission exists.
     * 
     * @param resource the resource name
     * @param action the action name
     * @return Optional containing the permission if found, empty otherwise
     */
    @Query("SELECT p FROM Permission p WHERE p.resource = :resource AND p.action = :action")
    Optional<Permission> findByResourceAndAction(
        @Param("resource") String resource,
        @Param("action") String action
    );
    
    /**
     * Check if a permission exists by name.
     * 
     * @param name the permission name to check
     * @return true if permission exists, false otherwise
     */
    boolean existsByName(String name);
}
