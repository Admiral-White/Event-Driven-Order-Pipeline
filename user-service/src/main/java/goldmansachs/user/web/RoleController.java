package goldmansachs.user.web;

import goldmansachs.user.domain.Role;
import goldmansachs.user.domain.Permission;
import goldmansachs.user.service.RoleService;
import goldmansachs.user.service.PermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * RoleController for handling role and permission management endpoints.
 * 
 * This controller provides endpoints for:
 * - Creating and managing roles
 * - Managing role permissions
 * - Managing permissions
 * - Flexible RBAC operations
 * 
 * All endpoints require authentication and appropriate permissions.
 * 
 * @author Michael Inyang
 * @version 1.0
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
public class RoleController {
    
    private final RoleService roleService;
    private final PermissionService permissionService;
    
    // ==================== Role Endpoints ====================
    
    /**
     * Create a new role.
     * 
     * Endpoint: POST /api/v1/roles
     * 
     * @param createRoleRequest the role creation request
     * @return ResponseEntity containing the created role
     */
    @PostMapping("/roles")
    public ResponseEntity<?> createRole(@RequestBody CreateRoleRequest createRoleRequest) {
        log.info("Creating new role: {}", createRoleRequest.getName());
        
        try {
            Role role = roleService.createRole(
                    createRoleRequest.getName(),
                    createRoleRequest.getDescription()
            );
            
            log.info("Role created successfully: {}", createRoleRequest.getName());
            return ResponseEntity.status(HttpStatus.CREATED).body(role);
        } catch (RoleService.RoleAlreadyExistsException e) {
            log.warn("Role creation failed - role already exists: {}", createRoleRequest.getName());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Error creating role", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Failed to create role: " + e.getMessage()));
        }
    }
    
    /**
     * Get all active roles.
     * 
     * Endpoint: GET /api/v1/roles
     * 
     * @return ResponseEntity containing list of all active roles
     */
    @GetMapping("/roles")
    public ResponseEntity<?> getAllActiveRoles() {
        log.debug("Fetching all active roles");
        
        try {
            List<Role> roles = roleService.getAllActiveRoles();
            log.debug("Retrieved {} active roles", roles.size());
            return ResponseEntity.ok(roles);
        } catch (Exception e) {
            log.error("Error fetching roles", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Failed to fetch roles: " + e.getMessage()));
        }
    }
    
    /**
     * Get a role by ID.
     * 
     * Endpoint: GET /api/v1/roles/{roleId}
     * 
     * @param roleId the role ID
     * @return ResponseEntity containing the role
     */
    @GetMapping("/roles/{roleId}")
    public ResponseEntity<?> getRoleById(@PathVariable Long roleId) {
        log.debug("Fetching role: {}", roleId);
        
        try {
            Optional<Role> role = roleService.getRoleById(roleId);
            
            if (role.isEmpty()) {
                log.warn("Role not found: {}", roleId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ErrorResponse("Role not found"));
            }
            
            return ResponseEntity.ok(role.get());
        } catch (Exception e) {
            log.error("Error fetching role", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Failed to fetch role: " + e.getMessage()));
        }
    }
    
    /**
     * Update role information.
     * 
     * Endpoint: PATCH /api/v1/roles/{roleId}
     * 
     * @param roleId the role ID to update
     * @param updateRoleRequest the update request
     * @return ResponseEntity containing the updated role
     */
    @PatchMapping("/roles/{roleId}")
    public ResponseEntity<?> updateRole(
            @PathVariable Long roleId,
            @RequestBody UpdateRoleRequest updateRoleRequest) {
        log.info("Updating role: {}", roleId);
        
        try {
            Role updatedRole = roleService.updateRole(roleId, updateRoleRequest.getDescription());
            log.info("Role updated successfully: {}", roleId);
            return ResponseEntity.ok(updatedRole);
        } catch (RoleService.RoleNotFoundException e) {
            log.warn("Role not found: {}", roleId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Error updating role", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Failed to update role: " + e.getMessage()));
        }
    }
    
    /**
     * Activate a role.
     * 
     * Endpoint: POST /api/v1/roles/{roleId}/activate
     * 
     * @param roleId the role ID to activate
     * @return ResponseEntity containing the activated role
     */
    @PostMapping("/roles/{roleId}/activate")
    public ResponseEntity<?> activateRole(@PathVariable Long roleId) {
        log.info("Activating role: {}", roleId);
        
        try {
            Role activatedRole = roleService.activateRole(roleId);
            log.info("Role activated successfully: {}", roleId);
            return ResponseEntity.ok(activatedRole);
        } catch (RoleService.RoleNotFoundException e) {
            log.warn("Role not found: {}", roleId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Error activating role", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Failed to activate role: " + e.getMessage()));
        }
    }
    
    /**
     * Deactivate a role.
     * 
     * Endpoint: POST /api/v1/roles/{roleId}/deactivate
     * 
     * @param roleId the role ID to deactivate
     * @return ResponseEntity containing the deactivated role
     */
    @PostMapping("/roles/{roleId}/deactivate")
    public ResponseEntity<?> deactivateRole(@PathVariable Long roleId) {
        log.info("Deactivating role: {}", roleId);
        
        try {
            Role deactivatedRole = roleService.deactivateRole(roleId);
            log.info("Role deactivated successfully: {}", roleId);
            return ResponseEntity.ok(deactivatedRole);
        } catch (RoleService.RoleNotFoundException e) {
            log.warn("Role not found: {}", roleId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Error deactivating role", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Failed to deactivate role: " + e.getMessage()));
        }
    }
    
    // ==================== Role Permission Endpoints ====================
    
    /**
     * Add a permission to a role.
     * 
     * Endpoint: POST /api/v1/roles/{roleId}/permissions/{permissionId}
     * 
     * @param roleId the role ID
     * @param permissionId the permission ID to add
     * @return ResponseEntity containing the updated role
     */
    @PostMapping("/roles/{roleId}/permissions/{permissionId}")
    public ResponseEntity<?> addPermissionToRole(
            @PathVariable Long roleId,
            @PathVariable Long permissionId) {
        log.info("Adding permission {} to role {}", permissionId, roleId);
        
        try {
            Role updatedRole = roleService.addPermissionToRole(roleId, permissionId);
            log.info("Permission added to role successfully");
            return ResponseEntity.ok(updatedRole);
        } catch (RoleService.RoleNotFoundException e) {
            log.warn("Role not found: {}", roleId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse(e.getMessage()));
        } catch (RoleService.PermissionNotFoundException e) {
            log.warn("Permission not found: {}", permissionId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Error adding permission to role", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Failed to add permission: " + e.getMessage()));
        }
    }
    
    /**
     * Remove a permission from a role.
     * 
     * Endpoint: DELETE /api/v1/roles/{roleId}/permissions/{permissionId}
     * 
     * @param roleId the role ID
     * @param permissionId the permission ID to remove
     * @return ResponseEntity containing the updated role
     */
    @DeleteMapping("/roles/{roleId}/permissions/{permissionId}")
    public ResponseEntity<?> removePermissionFromRole(
            @PathVariable Long roleId,
            @PathVariable Long permissionId) {
        log.info("Removing permission {} from role {}", permissionId, roleId);
        
        try {
            Role updatedRole = roleService.removePermissionFromRole(roleId, permissionId);
            log.info("Permission removed from role successfully");
            return ResponseEntity.ok(updatedRole);
        } catch (RoleService.RoleNotFoundException e) {
            log.warn("Role not found: {}", roleId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse(e.getMessage()));
        } catch (RoleService.PermissionNotFoundException e) {
            log.warn("Permission not found: {}", permissionId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Error removing permission from role", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Failed to remove permission: " + e.getMessage()));
        }
    }
    
    /**
     * Get all permissions for a role.
     * 
     * Endpoint: GET /api/v1/roles/{roleId}/permissions
     * 
     * @param roleId the role ID
     * @return ResponseEntity containing the list of permissions
     */
    @GetMapping("/roles/{roleId}/permissions")
    public ResponseEntity<?> getRolePermissions(@PathVariable Long roleId) {
        log.debug("Fetching permissions for role: {}", roleId);
        
        try {
            Set<Permission> permissions = roleService.getRolePermissions(roleId);
            return ResponseEntity.ok(permissions);
        } catch (RoleService.RoleNotFoundException e) {
            log.warn("Role not found: {}", roleId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Error fetching role permissions", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Failed to fetch permissions: " + e.getMessage()));
        }
    }
    
    // ==================== Permission Endpoints ====================
    
    /**
     * Create a new permission.
     * 
     * Endpoint: POST /api/v1/permissions
     * 
     * @param createPermissionRequest the permission creation request
     * @return ResponseEntity containing the created permission
     */
    @PostMapping("/permissions")
    public ResponseEntity<?> createPermission(@RequestBody CreatePermissionRequest createPermissionRequest) {
        log.info("Creating new permission: {}", createPermissionRequest.getName());
        
        try {
            Permission permission = permissionService.createPermission(
                    createPermissionRequest.getName(),
                    createPermissionRequest.getDescription(),
                    createPermissionRequest.getResource(),
                    createPermissionRequest.getAction()
            );
            
            log.info("Permission created successfully: {}", createPermissionRequest.getName());
            return ResponseEntity.status(HttpStatus.CREATED).body(permission);
        } catch (PermissionService.PermissionAlreadyExistsException e) {
            log.warn("Permission creation failed - permission already exists: {}", createPermissionRequest.getName());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Error creating permission", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Failed to create permission: " + e.getMessage()));
        }
    }
    
    /**
     * Get all active permissions.
     * 
     * Endpoint: GET /api/v1/permissions
     * 
     * @return ResponseEntity containing list of all active permissions
     */
    @GetMapping("/permissions")
    public ResponseEntity<?> getAllActivePermissions() {
        log.debug("Fetching all active permissions");
        
        try {
            List<Permission> permissions = permissionService.getAllActivePermissions();
            log.debug("Retrieved {} active permissions", permissions.size());
            return ResponseEntity.ok(permissions);
        } catch (Exception e) {
            log.error("Error fetching permissions", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Failed to fetch permissions: " + e.getMessage()));
        }
    }
    
    /**
     * Get a permission by ID.
     * 
     * Endpoint: GET /api/v1/permissions/{permissionId}
     * 
     * @param permissionId the permission ID
     * @return ResponseEntity containing the permission
     */
    @GetMapping("/permissions/{permissionId}")
    public ResponseEntity<?> getPermissionById(@PathVariable Long permissionId) {
        log.debug("Fetching permission: {}", permissionId);
        
        try {
            Optional<Permission> permission = permissionService.getPermissionById(permissionId);
            
            if (permission.isEmpty()) {
                log.warn("Permission not found: {}", permissionId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ErrorResponse("Permission not found"));
            }
            
            return ResponseEntity.ok(permission.get());
        } catch (Exception e) {
            log.error("Error fetching permission", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Failed to fetch permission: " + e.getMessage()));
        }
    }
    
    /**
     * Get permissions by resource.
     * 
     * Endpoint: GET /api/v1/permissions/resource/{resource}
     * 
     * @param resource the resource name
     * @return ResponseEntity containing list of permissions for the resource
     */
    @GetMapping("/permissions/resource/{resource}")
    public ResponseEntity<?> getPermissionsByResource(@PathVariable String resource) {
        log.debug("Fetching permissions for resource: {}", resource);
        
        try {
            List<Permission> permissions = permissionService.getPermissionsByResource(resource);
            return ResponseEntity.ok(permissions);
        } catch (Exception e) {
            log.error("Error fetching permissions by resource", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Failed to fetch permissions: " + e.getMessage()));
        }
    }
    
    // ==================== Request/Response Classes ====================
    
    /**
     * Request for creating a role.
     */
    public static class CreateRoleRequest {
        public String name;
        public String description;
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }
    
    /**
     * Request for updating a role.
     */
    public static class UpdateRoleRequest {
        public String description;
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }
    
    /**
     * Request for creating a permission.
     */
    public static class CreatePermissionRequest {
        public String name;
        public String description;
        public String resource;
        public String action;
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public String getResource() { return resource; }
        public void setResource(String resource) { this.resource = resource; }
        
        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }
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
}
