# User Service

A comprehensive user management and authentication service with JWT-based authorization, role-based access control (RBAC), and audit trail tracking.

## Overview

The User Service provides:
- **User Account Management**: Create, read, update, and delete user accounts
- **JWT Authentication**: Secure token-based authentication
- **Role-Based Access Control (RBAC)**: Flexible permission and role management
- **Audit Trail**: Complete tracking of all user-related actions
- **Event Publishing**: Kafka integration for user lifecycle events
- **Password Security**: Strong password validation with 12-char minimum, uppercase, lowercase, digits, and special characters

## Architecture

### Port
- **8083** - User Service API

### Key Components

#### Domain Model
- **User**: User accounts with soft delete support
- **Role**: User roles with permission associations
- **Permission**: Fine-grained permissions for RBAC
- **UserAuditTrail**: Audit trail for user actions
- **OutboxEvent**: Transactional outbox for reliable event publishing

#### Services
- **AuthService**: Registration, login, token management
- **UserService**: User CRUD, status management, audit logging
- **RoleService**: Role and permission management
- **PermissionService**: Permission CRUD operations

#### Controllers
- **AuthController**: Authentication endpoints (register, login, logout, refresh)
- **UserController**: User management endpoints
- **RoleController**: Role and permission management endpoints

### Security
- **JWT Tokens**: Stateless authentication
- **BCrypt Password Encoding**: Strength 12
- **CORS**: Configured for cross-origin requests
- **CSRF**: Disabled (stateless JWT architecture)

### Event Publishing
- **Kafka Integration**: User events published to Kafka topics
- **Transactional Outbox Pattern**: Ensures reliable event delivery
- **Automatic Retry**: Failed events are retried with exponential backoff
- **Event Archival**: Old published events are archived for retention

## API Endpoints

### Authentication Endpoints

#### Register User
```bash
POST /api/v1/auth/register
Content-Type: application/json

{
  "username": "john.doe",
  "email": "john@example.com",
  "password": "SecurePass123!@#",
  "firstName": "John",
  "lastName": "Doe"
}

Response:
{
  "userId": 1,
  "username": "john.doe",
  "email": "john@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "role": "USER",
  "permissions": "users.read,orders.create",
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 86400
}
```

#### Login
```bash
POST /api/v1/auth/login
Content-Type: application/json

{
  "username": "john.doe",
  "password": "SecurePass123!@#"
}
```

#### Logout
```bash
POST /api/v1/auth/logout
Authorization: Bearer <accessToken>
Content-Type: application/json

{
  "userId": 1,
  "token": "<accessToken>"
}
```

#### Refresh Token
```bash
POST /api/v1/auth/refresh
Content-Type: application/json

{
  "userId": 1,
  "username": "john.doe",
  "roleName": "USER"
}
```

### User Management Endpoints

#### Get All Active Users
```bash
GET /api/v1/users
Authorization: Bearer <accessToken>
```

#### Get User by ID
```bash
GET /api/v1/users/{userId}
Authorization: Bearer <accessToken>
```

#### Get User by Username
```bash
GET /api/v1/users/username/{username}
Authorization: Bearer <accessToken>
```

#### Update User
```bash
PATCH /api/v1/users/{userId}
Authorization: Bearer <accessToken>
Content-Type: application/json

{
  "firstName": "John",
  "lastName": "Doe",
  "email": "john.doe@example.com"
}
```

#### Activate User
```bash
POST /api/v1/users/{userId}/activate
Authorization: Bearer <accessToken>
```

#### Deactivate User
```bash
POST /api/v1/users/{userId}/deactivate
Authorization: Bearer <accessToken>
```

#### Suspend User
```bash
POST /api/v1/users/{userId}/suspend
Authorization: Bearer <accessToken>
Content-Type: application/json

{
  "reason": "Account violation"
}
```

#### Unsuspend User
```bash
POST /api/v1/users/{userId}/unsuspend
Authorization: Bearer <accessToken>
```

#### Delete User (Soft Delete)
```bash
DELETE /api/v1/users/{userId}
Authorization: Bearer <accessToken>
Content-Type: application/json

{
  "reason": "User requested deletion"
}
```

#### Get User Audit Trail
```bash
GET /api/v1/users/{userId}/audit-trail
Authorization: Bearer <accessToken>
```

#### Get User Audit Trail by Date Range
```bash
GET /api/v1/users/{userId}/audit-trail-range?startDate=2024-01-01T00:00:00&endDate=2024-12-31T23:59:59
Authorization: Bearer <accessToken>
```

#### Get Active User Count
```bash
GET /api/v1/users/count/active
Authorization: Bearer <accessToken>
```

### Role Management Endpoints

#### Create Role
```bash
POST /api/v1/roles
Authorization: Bearer <accessToken>
Content-Type: application/json

{
  "name": "SUPERVISOR",
  "description": "Supervisor with limited admin capabilities"
}
```

#### Get All Active Roles
```bash
GET /api/v1/roles
Authorization: Bearer <accessToken>
```

#### Get Role by ID
```bash
GET /api/v1/roles/{roleId}
Authorization: Bearer <accessToken>
```

#### Update Role
```bash
PATCH /api/v1/roles/{roleId}
Authorization: Bearer <accessToken>
Content-Type: application/json

{
  "description": "Updated role description"
}
```

#### Activate Role
```bash
POST /api/v1/roles/{roleId}/activate
Authorization: Bearer <accessToken>
```

#### Deactivate Role
```bash
POST /api/v1/roles/{roleId}/deactivate
Authorization: Bearer <accessToken>
```

#### Add Permission to Role
```bash
POST /api/v1/roles/{roleId}/permissions/{permissionId}
Authorization: Bearer <accessToken>
```

#### Remove Permission from Role
```bash
DELETE /api/v1/roles/{roleId}/permissions/{permissionId}
Authorization: Bearer <accessToken>
```

#### Get Role Permissions
```bash
GET /api/v1/roles/{roleId}/permissions
Authorization: Bearer <accessToken>
```

### Permission Management Endpoints

#### Create Permission
```bash
POST /api/v1/permissions
Authorization: Bearer <accessToken>
Content-Type: application/json

{
  "name": "reports.generate",
  "description": "Generate system reports",
  "resource": "reports",
  "action": "generate"
}
```

#### Get All Active Permissions
```bash
GET /api/v1/permissions
Authorization: Bearer <accessToken>
```

#### Get Permission by ID
```bash
GET /api/v1/permissions/{permissionId}
Authorization: Bearer <accessToken>
```

#### Get Permissions by Resource
```bash
GET /api/v1/permissions/resource/{resource}
Authorization: Bearer <accessToken>
```

## Password Policy

Passwords must meet the following requirements:
- **Minimum 12 characters**
- **At least one uppercase letter** (A-Z)
- **At least one lowercase letter** (a-z)
- **At least one digit** (0-9)
- **At least one special character** (!@#$%^&*()-_+=[]{}|;:,.<>?)

Example valid password: `SecurePass123!@#`

## JWT Token Structure

### Access Token
- **Subject (sub)**: User ID
- **Claims**:
  - `userId`: User ID
  - `username`: Username
  - `role`: Role name
  - `permissions`: Comma-separated list of permission names
- **Expiration**: 24 hours (configurable)

### Refresh Token
- **Subject (sub)**: User ID
- **Claims**:
  - `userId`: User ID
  - `username`: Username
  - `tokenType`: "REFRESH"
- **Expiration**: 7 days

## Configuration

### Environment Variables

```bash
# Database
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/user_db
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=postgres

# Kafka
SPRING_KAFKA_BOOTSTRAP_SERVERS=localhost:9092

# JWT
JWT_SECRET_KEY=your-super-secret-key-change-this-in-production
JWT_EXPIRATION_MS=86400000

# Outbox
OUTBOX_POLL_INTERVAL=5000
OUTBOX_RETENTION_DAYS=7
OUTBOX_MAX_RETRIES=3
```

## Database Schema

### Tables
- **users**: User accounts (with soft delete)
- **roles**: User roles
- **permissions**: Fine-grained permissions
- **role_permissions**: Role-permission associations
- **user_audit_trail**: Audit trail entries
- **user_outbox_events**: Outbox events for Kafka publishing

### Indexes
Optimized indexes for:
- Username and email lookups
- User status queries
- Role and permission queries
- Audit trail searches
- Outbox event publishing queries

## Event Publishing

### Kafka Topics
- **user-events**: All user lifecycle events

### Event Types
- `USER_CREATED`: User account created
- `USER_ACTIVATED`: User account activated
- `USER_DEACTIVATED`: User account deactivated
- `USER_DELETED`: User account deleted (soft delete)
- `ROLE_ASSIGNED`: Role assigned to user
- `ROLE_REMOVED`: Role removed from user

### Transactional Outbox
Events are stored in the outbox table and published asynchronously:
1. Event created with status `PENDING`
2. OutboxPublisher polls for pending events
3. Events published to Kafka
4. Status changed to `PUBLISHED`
5. Old events archived after retention period

## Audit Trail

All significant user actions are logged:
- User creation, updates, deletion
- Login attempts (success/failure)
- Password changes
- Role assignments
- Account status changes
- Token generation and refresh

Each audit entry includes:
- User ID
- Action type
- Detailed description
- IP address (if available)
- User agent (if available)
- Timestamp

## Security Best Practices

1. **Change JWT Secret**: Always change `JWT_SECRET_KEY` in production
2. **Use HTTPS**: Always use HTTPS in production
3. **Token Rotation**: Refresh tokens regularly
4. **CORS Configuration**: Restrict CORS origins in production
5. **Rate Limiting**: Implement rate limiting on authentication endpoints
6. **Monitoring**: Monitor failed login attempts and suspicious activities

## Local Development

### Prerequisites
- Java 17+
- Maven 3.8+
- PostgreSQL 15+
- Apache Kafka

### Build
```bash
cd user-service
mvn clean package
```

### Run
```bash
# Using Maven
mvn spring-boot:run

# Using JAR
java -jar target/user-service-1.0.0.jar
```

### Test Endpoints
```bash
# Register user
curl -X POST http://localhost:8083/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username":"test.user",
    "email":"test@example.com",
    "password":"TestPass123!@#",
    "firstName":"Test",
    "lastName":"User"
  }'

# Login
curl -X POST http://localhost:8083/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username":"test.user",
    "password":"TestPass123!@#"
  }'

# Get user (requires access token)
curl -X GET http://localhost:8083/api/v1/users/1 \
  -H "Authorization: Bearer <accessToken>"
```

## Docker Deployment

### Build Docker Image
```bash
docker build -t user-service:1.0 .
```

### Run with Docker Compose
```bash
docker-compose up user-service
```

## Troubleshooting

### User not found during login
- Verify user exists in database
- Check username spelling and case sensitivity

### JWT validation errors
- Verify JWT_SECRET_KEY is configured correctly
- Check token expiration
- Ensure token format is "Bearer <token>"

### Database connection errors
- Verify PostgreSQL is running
- Check database URL and credentials
- Ensure user_db database exists

### Kafka connection errors
- Verify Kafka is running
- Check bootstrap server configuration
- Ensure Kafka topics exist

## Health Check

```bash
curl http://localhost:8083/actuator/health
```

## Metrics

```bash
curl http://localhost:8083/actuator/metrics
```

## Contributing

Follow the existing code patterns:
- Use Lombok for boilerplate reduction
- Add comprehensive documentation
- Log at appropriate levels (DEBUG, INFO, WARN, ERROR)
- Follow naming conventions
- Implement proper exception handling

## License

This project is part of the Event-Driven Order Pipeline system.
