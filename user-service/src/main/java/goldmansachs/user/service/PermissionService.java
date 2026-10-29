package goldmansachs.user.service;

import goldmansachs.user.domain.Permission;
import goldmansachs.user.repository.PermissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * PermissionService for managing system permissions.
 * 
 * This service handles:
 * - Permission CRUD operations
 * - Permission retrieval by resource and action
 * - Permission activation/deactivation
 * 
 * Permissions are the fine-grained access controls used in RBAC.
 * 
 * @author Michael Inyang
 * @version 1.0
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PermissionService {
    
    private final PermissionRepository permissionRepository;
    
    /**
     * Create a new permission.
     * 
     * Permission names use dot notation: resource.action
     * Example: orders.create, orders.view, users.manage
     * 
     * @param name the unique name of the permission (resource.action)
     * @param description a description of the permission
     * @param resource the resource category
     * @param action the action allowed on the resource
     * @return the created permission
     * @throws PermissionAlreadyExistsException if permission name already exists
     */
    @Transactional
    public Permission createPermission(String name, String description, String resource, String action) {
        log.info("Creating new permission: {}", name);
        
        // Check if permission already exists
        if (permissionRepository.existsByName(name)) {
            log.warn("Permission already exists: {}", name);
            throw new PermissionAlreadyExistsException("Permission already exists: " + name);
        }
        
        Permission permission = Permission.builder()
                .name(name)
                .description(description)
                .resource(resource)
                .action(action)
                .active(true)
                .build();
        
        Permission createdPermission = permissionRepository.save(permission);
        log.info("Permission created successfully: {}", name);
        return createdPermission;
    }
    
    /**
     * Get a permission by ID.
     * 
     * @param permissionId the permission ID
     * @return Optional containing the permission if found
     */
    @Transactional(readOnly = true)
    public Optional<Permission> getPermissionById(Long permissionId) {
        log.debug("Retrieving permission with ID: {}", permissionId);
        return permissionRepository.findById(permissionId);
    }
    
    /**
     * Get a permission by name.
     * 
     * @param name the permission name
     * @return Optional containing the permission if found
     */
    @Transactional(readOnly = true)
    public Optional<Permission> getPermissionByName(String name) {
        log.debug("Retrieving permission by name: {}", name);
        return permissionRepository.findByName(name);
    }
    
    /**
     * Get a permission by resource and action.
     * 
     * @param resource the resource name
     * @param action the action name
     * @return Optional containing the permission if found
     */
    @Transactional(readOnly = true)
    public Optional<Permission> getPermissionByResourceAndAction(String resource, String action) {
        log.debug("Retrieving permission for resource: {}, action: {}", resource, action);
        return permissionRepository.findByResourceAndAction(resource, action);
    }
    
    /**
     * Get all active permissions.
     * 
     * @return list of all active permissions
     */
    @Transactional(readOnly = true)
    public List<Permission> getAllActivePermissions() {
        log.debug("Retrieving all active permissions");
        return permissionRepository.findAllActive();
    }
    
    /**
     * Get all inactive permissions.
     * 
     * @return list of all inactive permissions
     */
    @Transactional(readOnly = true)
    public List<Permission> getAllInactivePermissions() {
        log.debug("Retrieving all inactive permissions");
        return permissionRepository.findAllInactive();
    }
    
    /**
     * Get all permissions for a specific resource.
     * 
     * @param resource the resource name
     * @return list of permissions for the resource
     */
    @Transactional(readOnly = true)
    public List<Permission> getPermissionsByResource(String resource) {
        log.debug("Retrieving permissions for resource: {}", resource);
        return permissionRepository.findByResource(resource);
    }
    
    /**
     * Update permission information.
     * 
     * @param permissionId the ID of the permission to update
     * @param description the new description
     * @return the updated permission
     * @throws PermissionNotFoundException if permission is not found
     */
    @Transactional
    public Permission updatePermission(Long permissionId, String description) {
        log.info("Updating permission: {}", permissionId);
        
        Permission permission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new PermissionNotFoundException("Permission not found: " + permissionId));
        
        permission.setDescription(description);
        Permission updatedPermission = permissionRepository.save(permission);
        
        log.info("Permission updated successfully: {}", permissionId);
        return updatedPermission;
    }
    
    /**
     * Activate a permission.
     * 
     * @param permissionId the ID of the permission to activate
     * @return the activated permission
     * @throws PermissionNotFoundException if permission is not found
     */
    @Transactional
    public Permission activatePermission(Long permissionId) {
        log.info("Activating permission: {}", permissionId);
        
        Permission permission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new PermissionNotFoundException("Permission not found: " + permissionId));
        
        permission.setActive(true);
        Permission activatedPermission = permissionRepository.save(permission);
        
        log.info("Permission activated successfully: {}", permissionId);
        return activatedPermission;
    }
    
    /**
     * Deactivate a permission.
     * 
     * @param permissionId the ID of the permission to deactivate
     * @return the deactivated permission
     * @throws PermissionNotFoundException if permission is not found
     */
    @Transactional
    public Permission deactivatePermission(Long permissionId) {
        log.info("Deactivating permission: {}", permissionId);
        
        Permission permission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new PermissionNotFoundException("Permission not found: " + permissionId));
        
        permission.setActive(false);
        Permission deactivatedPermission = permissionRepository.save(permission);
        
        log.info("Permission deactivated successfully: {}", permissionId);
        return deactivatedPermission;
    }
    
    // Custom Exceptions
    
    /**
     * Exception thrown when permission already exists.
     */
    public static class PermissionAlreadyExistsException extends RuntimeException {
        public PermissionAlreadyExistsException(String message) {
            super(message);
        }
    }
    
    /**
     * Exception thrown when permission is not found.
     */
    public static class PermissionNotFoundException extends RuntimeException {
        public PermissionNotFoundException(String message) {
            super(message);
        }
    }
}
