package goldmansachs.user.service;

import goldmansachs.user.domain.Role;
import goldmansachs.user.domain.Permission;
import goldmansachs.user.repository.RoleRepository;
import goldmansachs.user.repository.PermissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * RoleService for managing roles and permissions.
 * 
 * This service handles:
 * - Role CRUD operations
 * - Permission management
 * - Flexible Role-Based Access Control (RBAC)
 * - Role activation/deactivation
 * 
 * Provides flexible permission assignment to roles for fine-grained access control.
 * 
 * @author Michael Inyang
 * @version 1.0
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RoleService {
    
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    
    /**
     * Create a new role with permissions.
     * 
     * @param name the unique name of the role
     * @param description a description of the role
     * @return the created role
     * @throws RoleAlreadyExistsException if role name already exists
     */
    @Transactional
    public Role createRole(String name, String description) {
        log.info("Creating new role: {}", name);
        
        // Check if role already exists
        if (roleRepository.existsByName(name)) {
            log.warn("Role already exists: {}", name);
            throw new RoleAlreadyExistsException("Role already exists: " + name);
        }
        
        Role role = Role.builder()
                .name(name)
                .description(description)
                .active(true)
                .build();
        
        Role createdRole = roleRepository.save(role);
        log.info("Role created successfully: {}", name);
        return createdRole;
    }
    
    /**
     * Get a role by ID.
     * 
     * @param roleId the role ID
     * @return Optional containing the role if found
     */
    @Transactional(readOnly = true)
    public Optional<Role> getRoleById(Long roleId) {
        log.debug("Retrieving role with ID: {}", roleId);
        return roleRepository.findById(roleId);
    }
    
    /**
     * Get a role by name.
     * 
     * @param name the role name
     * @return Optional containing the role if found
     */
    @Transactional(readOnly = true)
    public Optional<Role> getRoleByName(String name) {
        log.debug("Retrieving role by name: {}", name);
        return roleRepository.findByName(name);
    }
    
    /**
     * Get all active roles.
     * 
     * @return list of all active roles
     */
    @Transactional(readOnly = true)
    public List<Role> getAllActiveRoles() {
        log.debug("Retrieving all active roles");
        return roleRepository.findAllActive();
    }
    
    /**
     * Get all inactive roles.
     * 
     * @return list of all inactive roles
     */
    @Transactional(readOnly = true)
    public List<Role> getAllInactiveRoles() {
        log.debug("Retrieving all inactive roles");
        return roleRepository.findAllInactive();
    }
    
    /**
     * Update role information.
     * 
     * @param roleId the ID of the role to update
     * @param description the new description
     * @return the updated role
     * @throws RoleNotFoundException if role is not found
     */
    @Transactional
    public Role updateRole(Long roleId, String description) {
        log.info("Updating role: {}", roleId);
        
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RoleNotFoundException("Role not found: " + roleId));
        
        role.setDescription(description);
        Role updatedRole = roleRepository.save(role);
        
        log.info("Role updated successfully: {}", roleId);
        return updatedRole;
    }
    
    /**
     * Activate a role.
     * 
     * @param roleId the ID of the role to activate
     * @return the activated role
     * @throws RoleNotFoundException if role is not found
     */
    @Transactional
    public Role activateRole(Long roleId) {
        log.info("Activating role: {}", roleId);
        
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RoleNotFoundException("Role not found: " + roleId));
        
        role.setActive(true);
        Role activatedRole = roleRepository.save(role);
        
        log.info("Role activated successfully: {}", roleId);
        return activatedRole;
    }
    
    /**
     * Deactivate a role.
     * 
     * @param roleId the ID of the role to deactivate
     * @return the deactivated role
     * @throws RoleNotFoundException if role is not found
     */
    @Transactional
    public Role deactivateRole(Long roleId) {
        log.info("Deactivating role: {}", roleId);
        
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RoleNotFoundException("Role not found: " + roleId));
        
        role.setActive(false);
        Role deactivatedRole = roleRepository.save(role);
        
        log.info("Role deactivated successfully: {}", roleId);
        return deactivatedRole;
    }
    
    /**
     * Add a permission to a role.
     * 
     * @param roleId the ID of the role
     * @param permissionId the ID of the permission to add
     * @return the updated role
     * @throws RoleNotFoundException if role is not found
     * @throws PermissionNotFoundException if permission is not found
     */
    @Transactional
    public Role addPermissionToRole(Long roleId, Long permissionId) {
        log.info("Adding permission {} to role {}", permissionId, roleId);
        
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RoleNotFoundException("Role not found: " + roleId));
        
        Permission permission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new PermissionNotFoundException("Permission not found: " + permissionId));
        
        role.addPermission(permission);
        Role updatedRole = roleRepository.save(role);
        
        log.info("Permission added to role successfully");
        return updatedRole;
    }
    
    /**
     * Remove a permission from a role.
     * 
     * @param roleId the ID of the role
     * @param permissionId the ID of the permission to remove
     * @return the updated role
     * @throws RoleNotFoundException if role is not found
     * @throws PermissionNotFoundException if permission is not found
     */
    @Transactional
    public Role removePermissionFromRole(Long roleId, Long permissionId) {
        log.info("Removing permission {} from role {}", permissionId, roleId);
        
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RoleNotFoundException("Role not found: " + roleId));
        
        Permission permission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new PermissionNotFoundException("Permission not found: " + permissionId));
        
        role.removePermission(permission);
        Role updatedRole = roleRepository.save(role);
        
        log.info("Permission removed from role successfully");
        return updatedRole;
    }
    
    /**
     * Add multiple permissions to a role.
     * 
     * @param roleId the ID of the role
     * @param permissionIds set of permission IDs to add
     * @return the updated role
     * @throws RoleNotFoundException if role is not found
     */
    @Transactional
    public Role addPermissionsToRole(Long roleId, Set<Long> permissionIds) {
        log.info("Adding {} permissions to role {}", permissionIds.size(), roleId);
        
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RoleNotFoundException("Role not found: " + roleId));
        
        for (Long permissionId : permissionIds) {
            Permission permission = permissionRepository.findById(permissionId)
                    .orElseThrow(() -> new PermissionNotFoundException("Permission not found: " + permissionId));
            role.addPermission(permission);
        }
        
        Role updatedRole = roleRepository.save(role);
        log.info("Permissions added to role successfully");
        return updatedRole;
    }
    
    /**
     * Remove multiple permissions from a role.
     * 
     * @param roleId the ID of the role
     * @param permissionIds set of permission IDs to remove
     * @return the updated role
     * @throws RoleNotFoundException if role is not found
     */
    @Transactional
    public Role removePermissionsFromRole(Long roleId, Set<Long> permissionIds) {
        log.info("Removing {} permissions from role {}", permissionIds.size(), roleId);
        
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RoleNotFoundException("Role not found: " + roleId));
        
        for (Long permissionId : permissionIds) {
            Permission permission = permissionRepository.findById(permissionId)
                    .orElseThrow(() -> new PermissionNotFoundException("Permission not found: " + permissionId));
            role.removePermission(permission);
        }
        
        Role updatedRole = roleRepository.save(role);
        log.info("Permissions removed from role successfully");
        return updatedRole;
    }
    
    /**
     * Get all permissions for a role.
     * 
     * @param roleId the ID of the role
     * @return set of permissions for the role
     * @throws RoleNotFoundException if role is not found
     */
    @Transactional(readOnly = true)
    public Set<Permission> getRolePermissions(Long roleId) {
        log.debug("Retrieving permissions for role: {}", roleId);
        
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RoleNotFoundException("Role not found: " + roleId));
        
        return role.getPermissions();
    }
    
    /**
     * Check if a role has a specific permission.
     * 
     * @param roleId the ID of the role
     * @param permissionName the name of the permission to check
     * @return true if the role has the permission, false otherwise
     * @throws RoleNotFoundException if role is not found
     */
    @Transactional(readOnly = true)
    public boolean roleHasPermission(Long roleId, String permissionName) {
        log.debug("Checking if role {} has permission: {}", roleId, permissionName);
        
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RoleNotFoundException("Role not found: " + roleId));
        
        return role.hasPermission(permissionName);
    }
    
    // Custom Exceptions
    
    /**
     * Exception thrown when role already exists.
     */
    public static class RoleAlreadyExistsException extends RuntimeException {
        public RoleAlreadyExistsException(String message) {
            super(message);
        }
    }
    
    /**
     * Exception thrown when role is not found.
     */
    public static class RoleNotFoundException extends RuntimeException {
        public RoleNotFoundException(String message) {
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
