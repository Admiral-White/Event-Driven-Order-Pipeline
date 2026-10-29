package goldmansachs.user.repository;

import goldmansachs.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for User entity providing database access operations.
 * 
 * This repository handles all CRUD operations and custom queries for User entities.
 * It extends JpaRepository to inherit standard CRUD functionality and adds custom
 * query methods for user lookups by username, email, status, etc.
 * 
 * @author Michael Inyang
 * @version 1.0
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    /**
     * Find a user by username.
     * Username is unique across the system.
     * 
     * @param username the username to search for
     * @return Optional containing the user if found, empty otherwise
     */
    Optional<User> findByUsername(String username);
    
    /**
     * Find a user by email address.
     * Email is unique across the system.
     * 
     * @param email the email to search for
     * @return Optional containing the user if found, empty otherwise
     */
    Optional<User> findByEmail(String email);
    
    /**
     * Find all active users (excluding soft-deleted users).
     * 
     * @return list of all non-deleted users
     */
    @Query("SELECT u FROM User u WHERE u.deletedAt IS NULL")
    List<User> findAllActive();
    
    /**
     * Find all users with a specific status (excluding soft-deleted users).
     * 
     * @param status the user status to filter by
     * @return list of users with the specified status
     */
    @Query("SELECT u FROM User u WHERE u.status = :status AND u.deletedAt IS NULL")
    List<User> findByStatus(@Param("status") User.UserStatus status);
    
    /**
     * Find all users with a specific role (excluding soft-deleted users).
     * 
     * @param roleId the role ID to filter by
     * @return list of users with the specified role
     */
    @Query("SELECT u FROM User u WHERE u.role.id = :roleId AND u.deletedAt IS NULL")
    List<User> findByRoleId(@Param("roleId") Long roleId);
    
    /**
     * Find users created within a specific date range (excluding soft-deleted users).
     * 
     * @param startDate the start of the date range
     * @param endDate the end of the date range
     * @return list of users created within the specified period
     */
    @Query("SELECT u FROM User u WHERE u.createdAt BETWEEN :startDate AND :endDate AND u.deletedAt IS NULL")
    List<User> findByCreatedAtBetween(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
    
    /**
     * Find all deleted users (soft-deleted).
     * Useful for data cleanup and audit purposes.
     * 
     * @return list of all soft-deleted users
     */
    @Query("SELECT u FROM User u WHERE u.deletedAt IS NOT NULL")
    List<User> findAllDeleted();
    
    /**
     * Find deleted users within a specific date range.
     * Useful for cleanup and retention policies.
     * 
     * @param startDate the start of the date range
     * @param endDate the end of the date range
     * @return list of users deleted within the specified period
     */
    @Query("SELECT u FROM User u WHERE u.deletedAt BETWEEN :startDate AND :endDate")
    List<User> findDeletedBetween(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
    
    /**
     * Check if a username exists (excluding soft-deleted users).
     * Used for validation during user registration.
     * 
     * @param username the username to check
     * @return true if username exists and is not deleted, false otherwise
     */
    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM User u " +
           "WHERE u.username = :username AND u.deletedAt IS NULL")
    boolean existsByUsernameAndNotDeleted(@Param("username") String username);
    
    /**
     * Check if an email exists (excluding soft-deleted users).
     * Used for validation during user registration.
     * 
     * @param email the email to check
     * @return true if email exists and is not deleted, false otherwise
     */
    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM User u " +
           "WHERE u.email = :email AND u.deletedAt IS NULL")
    boolean existsByEmailAndNotDeleted(@Param("email") String email);
    
    /**
     * Count all active users.
     * Useful for analytics and monitoring.
     * 
     * @return the number of active users
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.deletedAt IS NULL")
    long countActiveUsers();
}
