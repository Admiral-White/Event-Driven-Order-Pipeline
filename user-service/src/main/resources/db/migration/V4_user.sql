/**
 * User Service Database Migration
 * Version: 4
 * Description: Complete user management schema with RBAC, audit trail, and event outbox
 * 
 * Tables created:
 * - users: User accounts with soft delete support
 * - roles: User roles for RBAC
 * - permissions: Fine-grained permissions
 * - role_permissions: Many-to-many mapping between roles and permissions
 * - user_audit_trail: Audit trail for user actions
 * - user_outbox_events: Transactional outbox for Kafka event publishing
 */

-- ==================== Users Table ====================
/**
 * Users table - stores user account information
 * Implements soft delete pattern using deleted_at timestamp
 */
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    
    -- User credentials (unique)
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    
    -- User profile information
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    
    -- Account status
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, INACTIVE, SUSPENDED, DELETED
    
    -- Foreign key to roles table (one role per user)
    role_id BIGINT NOT NULL,
    
    -- Timestamps
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Soft delete fields
    deleted_at TIMESTAMP,
    deletion_reason VARCHAR(255),
    
    -- Constraints
    CONSTRAINT fk_user_role FOREIGN KEY (role_id) REFERENCES roles(id)
);

-- Indexes on users table for query optimization
CREATE INDEX idx_users_username ON users(username) WHERE deleted_at IS NULL;
CREATE INDEX idx_users_email ON users(email) WHERE deleted_at IS NULL;
CREATE INDEX idx_users_status ON users(status) WHERE deleted_at IS NULL;
CREATE INDEX idx_users_role_id ON users(role_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_users_created_at ON users(created_at) WHERE deleted_at IS NULL;
CREATE INDEX idx_users_deleted_at ON users(deleted_at) WHERE deleted_at IS NOT NULL;

-- ==================== Roles Table ====================
/**
 * Roles table - stores user roles for RBAC
 */
CREATE TABLE IF NOT EXISTS roles (
    id BIGSERIAL PRIMARY KEY,
    
    -- Role information
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),
    
    -- Active flag
    active BOOLEAN NOT NULL DEFAULT true,
    
    -- Timestamp
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Index on roles name for lookups
CREATE INDEX idx_roles_name ON roles(name);
CREATE INDEX idx_roles_active ON roles(active);

-- ==================== Permissions Table ====================
/**
 * Permissions table - stores fine-grained permissions for RBAC
 * Permission names follow dot notation: resource.action
 * Example: orders.create, orders.view, users.manage, etc.
 */
CREATE TABLE IF NOT EXISTS permissions (
    id BIGSERIAL PRIMARY KEY,
    
    -- Permission information
    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255),
    
    -- Resource and action decomposition
    resource VARCHAR(50),
    action VARCHAR(50),
    
    -- Active flag
    active BOOLEAN NOT NULL DEFAULT true,
    
    -- Timestamp
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes on permissions table
CREATE INDEX idx_permissions_name ON permissions(name);
CREATE INDEX idx_permissions_resource ON permissions(resource);
CREATE INDEX idx_permissions_resource_action ON permissions(resource, action);
CREATE INDEX idx_permissions_active ON permissions(active);

-- ==================== Role Permissions Junction Table ====================
/**
 * Role Permissions table - many-to-many mapping between roles and permissions
 */
CREATE TABLE IF NOT EXISTS role_permissions (
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    
    PRIMARY KEY (role_id, permission_id),
    
    CONSTRAINT fk_role_permissions_role FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE,
    CONSTRAINT fk_role_permissions_permission FOREIGN KEY (permission_id) REFERENCES permissions(id) ON DELETE CASCADE
);

-- Index for efficient permission lookups by role
CREATE INDEX idx_role_permissions_permission ON role_permissions(permission_id);

-- ==================== User Audit Trail Table ====================
/**
 * User Audit Trail table - tracks all user-related events and actions
 * Used for security, compliance, and debugging purposes
 */
CREATE TABLE IF NOT EXISTS user_audit_trail (
    id BIGSERIAL PRIMARY KEY,
    
    -- Reference to user
    user_id BIGINT NOT NULL,
    
    -- Action information
    action VARCHAR(100) NOT NULL,
    details TEXT,
    
    -- Request information
    ip_address VARCHAR(45),
    user_agent VARCHAR(255),
    
    -- Timestamp
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for audit trail queries
CREATE INDEX idx_audit_user_id ON user_audit_trail(user_id);
CREATE INDEX idx_audit_action ON user_audit_trail(action);
CREATE INDEX idx_audit_timestamp ON user_audit_trail(timestamp);
CREATE INDEX idx_audit_user_id_timestamp ON user_audit_trail(user_id, timestamp DESC);

-- ==================== Outbox Events Table ====================
/**
 * User Outbox Events table - implements transactional outbox pattern
 * Events are stored atomically with business data and published asynchronously to Kafka
 * 
 * Status lifecycle: PENDING -> PUBLISHED -> ARCHIVED (after retention period)
 *                  PENDING -> FAILED (if max retries exceeded)
 */
CREATE TABLE IF NOT EXISTS user_outbox_events (
    id BIGSERIAL PRIMARY KEY,
    
    -- Event information
    event_type VARCHAR(50) NOT NULL,
    payload TEXT NOT NULL,
    
    -- Aggregate reference
    aggregate_id VARCHAR(50) NOT NULL,
    aggregate_type VARCHAR(50) NOT NULL,
    
    -- Event status
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, PUBLISHED, FAILED, ARCHIVED
    
    -- Publishing information
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at TIMESTAMP,
    
    -- Retry information
    retry_count INTEGER DEFAULT 0,
    last_error VARCHAR(255)
);

-- Indexes for outbox event queries
CREATE INDEX idx_outbox_status ON user_outbox_events(status);
CREATE INDEX idx_outbox_aggregate_id ON user_outbox_events(aggregate_id);
CREATE INDEX idx_outbox_created_at ON user_outbox_events(created_at);
CREATE INDEX idx_outbox_pending ON user_outbox_events(status, created_at) WHERE status = 'PENDING';
CREATE INDEX idx_outbox_failed ON user_outbox_events(status, created_at) WHERE status = 'FAILED';

-- ==================== Default Roles ====================
/**
 * Insert default system roles
 */
INSERT INTO roles (name, description, active)
VALUES
    ('ADMIN', 'Administrator with full system access', true),
    ('USER', 'Regular user with limited access', true),
    ('MERCHANT', 'Merchant user for order processing', true)
ON CONFLICT DO NOTHING;

-- ==================== Default Permissions ====================
/**
 * Insert default system permissions
 * Permission naming convention: resource.action
 */
INSERT INTO permissions (name, description, resource, action, active)
VALUES
    -- User Management Permissions
    ('users.create', 'Create new users', 'users', 'create', true),
    ('users.read', 'View user information', 'users', 'read', true),
    ('users.update', 'Update user information', 'users', 'update', true),
    ('users.delete', 'Delete users', 'users', 'delete', true),
    ('users.manage', 'Manage all user operations', 'users', 'manage', true),
    
    -- Role Management Permissions
    ('roles.create', 'Create new roles', 'roles', 'create', true),
    ('roles.read', 'View roles', 'roles', 'read', true),
    ('roles.update', 'Update roles', 'roles', 'update', true),
    ('roles.delete', 'Delete roles', 'roles', 'delete', true),
    
    -- Permission Management Permissions
    ('permissions.create', 'Create new permissions', 'permissions', 'create', true),
    ('permissions.read', 'View permissions', 'permissions', 'read', true),
    ('permissions.update', 'Update permissions', 'permissions', 'update', true),
    ('permissions.delete', 'Delete permissions', 'permissions', 'delete', true),
    
    -- Order Management Permissions
    ('orders.create', 'Create orders', 'orders', 'create', true),
    ('orders.read', 'View orders', 'orders', 'read', true),
    ('orders.update', 'Update orders', 'orders', 'update', true),
    ('orders.delete', 'Delete orders', 'orders', 'delete', true),
    
    -- Settlement Management Permissions
    ('settlements.read', 'View settlements', 'settlements', 'read', true),
    ('settlements.manage', 'Manage settlements', 'settlements', 'manage', true),
    
    -- Audit Permissions
    ('audit.read', 'View audit trails', 'audit', 'read', true),
    
    -- System Permissions
    ('system.admin', 'System administration', 'system', 'admin', true)
ON CONFLICT DO NOTHING;

-- ==================== Assign Permissions to Roles ====================
/**
 * Assign default permissions to default roles
 */

-- ADMIN role gets all permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p WHERE r.name = 'ADMIN'
ON CONFLICT DO NOTHING;

-- USER role gets basic permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p 
WHERE r.name = 'USER' AND p.name IN (
    'users.read', 'orders.create', 'orders.read', 'settlements.read'
)
ON CONFLICT DO NOTHING;

-- MERCHANT role gets merchant-specific permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p 
WHERE r.name = 'MERCHANT' AND p.name IN (
    'orders.create', 'orders.read', 'orders.update', 'settlements.read'
)
ON CONFLICT DO NOTHING;
