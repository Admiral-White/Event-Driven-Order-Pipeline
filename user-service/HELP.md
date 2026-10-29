# User Service - Help & Quick Reference

## Quick Start (60 seconds)

### 1. Build the Service
```bash
cd user-service
mvn clean package -DskipTests
```

### 2. Start with Docker Compose
```bash
docker-compose up user-service
```

### 3. Test Registration
```bash
curl -X POST http://localhost:8083/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username":"admin",
    "email":"admin@example.com",
    "password":"AdminPass123!@#",
    "firstName":"Admin",
    "lastName":"User"
  }'
```

### 4. Login and Get Token
```bash
curl -X POST http://localhost:8083/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username":"admin",
    "password":"AdminPass123!@#"
  }'
```

## Feature Highlights

### 🔐 JWT Authentication
- Stateless token-based authentication
- 24-hour token expiration (configurable)
- Automatic token refresh capability

### 🔑 Strong Password Policy
- Minimum 12 characters
- Uppercase, lowercase, digits, special characters required
- Example: `SecurePass123!@#`

### 👥 Role-Based Access Control
- Pre-configured roles: ADMIN, USER, MERCHANT
- Flexible permission assignment
- Supports resource.action pattern (e.g., orders.create)

### 📋 Audit Trail
- Complete tracking of all user actions
- IP address and user agent logging
- Timestamp and action details

### 🎯 Event Publishing
- Kafka integration for user lifecycle events
- Transactional outbox pattern for reliability
- Automatic retry with exponential backoff

### 🗑️ Soft Delete
- User data never physically deleted
- Maintains audit trail and data integrity
- Can be restored if needed

## Common Tasks

### User Management

#### Create New User (Registration)
```bash
POST /api/v1/auth/register
```

#### Get User Information
```bash
GET /api/v1/users/{userId}
Authorization: Bearer <token>
```

#### Update User Profile
```bash
PATCH /api/v1/users/{userId}
Authorization: Bearer <token>
Content-Type: application/json

{
  "firstName": "New Name",
  "lastName": "New Last",
  "email": "newemail@example.com"
}
```

#### Activate/Deactivate Account
```bash
POST /api/v1/users/{userId}/activate
POST /api/v1/users/{userId}/deactivate
Authorization: Bearer <token>
```

#### Suspend Account
```bash
POST /api/v1/users/{userId}/suspend
Authorization: Bearer <token>
Content-Type: application/json

{
  "reason": "Suspicious activity detected"
}
```

### Role & Permission Management

#### Create New Role
```bash
POST /api/v1/roles
Authorization: Bearer <token>
Content-Type: application/json

{
  "name": "SUPERVISOR",
  "description": "Supervisor role with limited admin access"
}
```

#### Add Permission to Role
```bash
POST /api/v1/roles/{roleId}/permissions/{permissionId}
Authorization: Bearer <token>
```

#### Get Role Permissions
```bash
GET /api/v1/roles/{roleId}/permissions
Authorization: Bearer <token>
```

#### Create New Permission
```bash
POST /api/v1/permissions
Authorization: Bearer <token>
Content-Type: application/json

{
  "name": "reports.create",
  "description": "Create system reports",
  "resource": "reports",
  "action": "create"
}
```

### Audit & Monitoring

#### View User Audit Trail
```bash
GET /api/v1/users/{userId}/audit-trail
Authorization: Bearer <token>
```

#### View Audit Trail for Date Range
```bash
GET /api/v1/users/{userId}/audit-trail-range?startDate=2024-01-01T00:00:00&endDate=2024-12-31T23:59:59
Authorization: Bearer <token>
```

#### Get Active User Count
```bash
GET /api/v1/users/count/active
Authorization: Bearer <token>
```

## Configuration Reference

### Database Configuration
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/user_db
    username: postgres
    password: postgres
```

### JWT Configuration
```yaml
jwt:
  secret-key: your-super-secret-key-at-least-256-bits
  expiration-ms: 86400000  # 24 hours
```

### Outbox Configuration
```yaml
outbox:
  poll-interval: 5000      # 5 seconds
  retention-days: 7        # 7 days
  max-retries: 3           # 3 retry attempts
```

### Server Configuration
```yaml
server:
  port: 8083
  servlet:
    context-path: /
```

## Error Codes & Solutions

### 400 Bad Request
- Invalid request format
- Missing required fields
- Invalid email format
- Weak password

**Solution**: Check request JSON and field values

### 401 Unauthorized
- Invalid credentials
- Missing or invalid token
- Token expired

**Solution**: 
- Verify username and password
- Add valid Authorization header with Bearer token
- Refresh token if expired

### 403 Forbidden
- Insufficient permissions
- Account suspended or deleted

**Solution**:
- Check user role and permissions
- Verify account status
- Contact admin if suspended

### 404 Not Found
- User, role, or permission doesn't exist

**Solution**: 
- Verify ID exists
- List available resources
- Check for typos

### 409 Conflict
- Username or email already exists
- Role or permission already exists

**Solution**:
- Use different username/email
- Choose different role/permission name

### 500 Internal Server Error
- Database connection failed
- Kafka unavailable
- Unexpected error

**Solution**:
- Check logs: `docker-compose logs user-service`
- Verify database is running
- Verify Kafka is running
- Restart service

## Database

### Connect to Database
```bash
# Using psql
psql -h localhost -U postgres -d user_db

# View tables
\dt

# View users
SELECT * FROM users WHERE deleted_at IS NULL;

# View roles
SELECT * FROM roles;

# View permissions
SELECT * FROM permissions;

# View audit trail
SELECT * FROM user_audit_trail ORDER BY timestamp DESC;

# View pending events
SELECT * FROM user_outbox_events WHERE status = 'PENDING';
```

### Database Reset
```bash
# Drop and recreate database
psql -h localhost -U postgres -c "DROP DATABASE user_db;"
psql -h localhost -U postgres -c "CREATE DATABASE user_db;"

# Migrations will run automatically on startup
```

## Logs

### View Service Logs
```bash
# Docker Compose
docker-compose logs user-service -f

# Local (Spring Boot)
# Logs written to: logs/user-service.log
tail -f logs/user-service.log
```

### Log Levels
- **DEBUG**: Detailed information for debugging (JWT validation, queries)
- **INFO**: General information (user registration, login)
- **WARN**: Warning messages (retry failures)
- **ERROR**: Error messages (exceptions, connection failures)

### Enable Debug Logging
```yaml
logging:
  level:
    goldmansachs.user: DEBUG
    org.springframework.security: DEBUG
```

## Performance Tuning

### Database Connection Pool
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20        # Adjust based on load
      minimum-idle: 5
      connection-timeout: 20000
      idle-timeout: 300000
```

### Kafka Producer Batching
```yaml
spring:
  kafka:
    producer:
      batch.size: 16384            # Increase for higher throughput
      linger.ms: 10                # Wait for batching
```

### Outbox Polling
```yaml
outbox:
  poll-interval: 5000              # Decrease for lower latency
```

## Security Checklist

- [ ] Change JWT_SECRET_KEY in production
- [ ] Use HTTPS in production
- [ ] Configure CORS for specific origins
- [ ] Enable rate limiting on auth endpoints
- [ ] Monitor failed login attempts
- [ ] Regularly review audit trails
- [ ] Implement token rotation
- [ ] Use strong database passwords
- [ ] Enable database encryption at rest
- [ ] Implement WAF rules

## Monitoring & Health

### Health Check
```bash
curl http://localhost:8083/actuator/health
```

### Metrics
```bash
curl http://localhost:8083/actuator/metrics
```

### Application Info
```bash
curl http://localhost:8083/actuator/info
```

## Integration with Other Services

### Order Service Integration
User Service provides authentication for Order Service:
1. User registers/logs in with User Service
2. Receives JWT token
3. Includes token in requests to Order Service
4. Order Service validates token with User Service JWT

### Settlement Service Integration
User events can trigger settlement operations:
1. User role determines order limits
2. Settlement Service receives user context from events
3. Applies role-based settlement rules

### Reconciliation Service Integration
Audit trails used for reconciliation:
1. Track user actions that affect settlements
2. Verify settlement changes against audit trail
3. Generate compliance reports

## Development Tips

### Running Tests
```bash
mvn test
```

### Running Specific Test
```bash
mvn test -Dtest=UserServiceTest
```

### Clean Build
```bash
mvn clean install
```

### Skip Tests
```bash
mvn clean package -DskipTests
```

### Debug Mode
```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--debug"
```

## Useful Links

- Spring Security: https://spring.io/projects/spring-security
- JWT (JJWT): https://github.com/jwtk/jjwt
- Kafka: https://kafka.apache.org/
- PostgreSQL: https://www.postgresql.org/
- Spring Data JPA: https://spring.io/projects/spring-data-jpa
- Flyway: https://flywaydb.org/

## Support & Troubleshooting

### Common Issues

#### "user_db database does not exist"
```bash
# Solution: Run init script
./init-databases.sh

# Or create manually
psql -U postgres -c "CREATE DATABASE user_db;"
```

#### "Failed to connect to Kafka"
```bash
# Solution: Check Kafka is running
docker-compose ps

# Or start Kafka
docker-compose up kafka -d
```

#### "JWT secret key not configured"
```bash
# Solution: Set environment variable
export JWT_SECRET_KEY="your-secret-key-at-least-256-bits"

# Or configure in application.yml
```

#### "Connection pool exhausted"
```bash
# Solution: Increase pool size in application.yml
spring:
  datasource:
    hikari:
      maximum-pool-size: 30
```

## Getting Help

1. Check logs: `docker-compose logs user-service`
2. Review configuration: `application.yml`
3. Test endpoints: Use provided curl examples
4. Check database: Query tables directly
5. Review this document: Search for your issue
