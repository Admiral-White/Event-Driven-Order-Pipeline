package goldmansachs.user_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * User Service Application - Entry point for the User Service.
 * 
 * This Spring Boot application provides:
 * - User account management
 * - Authentication and authorization with JWT
 * - Role-Based Access Control (RBAC)
 * - User audit trail tracking
 * - Event publishing to Kafka via transactional outbox pattern
 * 
 * Enabled Features:
 * - Component scanning for goldmansachs.user package
 * - Scheduled task execution (for outbox publishing and cleanup)
 * - JPA/Hibernate ORM
 * - Spring Security with JWT
 * - Spring Kafka for event publishing
 * - Flyway database migrations
 * 
 * @author Michael Inyang
 * @version 1.0
 */
@SpringBootApplication
@ComponentScan(basePackages = {"goldmansachs.user"})
@EntityScan(basePackages = {"goldmansachs.user.domain"})
@EnableJpaRepositories(basePackages = {"goldmansachs.user.repository"})
@EnableJpaAuditing
@EnableScheduling
public class UserServiceApplication {
    
    /**
     * Main method - starts the User Service application.
     * 
     * @param args command line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}
