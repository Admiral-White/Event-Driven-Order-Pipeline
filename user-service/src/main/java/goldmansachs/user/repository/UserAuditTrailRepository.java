package goldmansachs.user.repository;

import goldmansachs.user.domain.UserAuditTrail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository interface for UserAuditTrail entity providing database access operations.
 * 
 * This repository handles CRUD operations and custom queries for UserAuditTrail entities.
 * Audit trails track all significant user events for security and compliance purposes.
 * 
 * @author Michael Inyang
 * @version 1.0
 */
@Repository
public interface UserAuditTrailRepository extends JpaRepository<UserAuditTrail, Long> {
    
    /**
     * Find all audit trail entries for a specific user.
     * 
     * @param userId the user ID to filter by
     * @return list of all audit trail entries for the user
     */
    List<UserAuditTrail> findByUserId(Long userId);
    
    /**
     * Find all audit trail entries for a specific user, ordered by timestamp descending.
     * 
     * @param userId the user ID to filter by
     * @return list of audit trail entries sorted by most recent first
     */
    @Query("SELECT a FROM UserAuditTrail a WHERE a.userId = :userId ORDER BY a.timestamp DESC")
    List<UserAuditTrail> findByUserIdOrderByTimestampDesc(@Param("userId") Long userId);
    
    /**
     * Find all audit trail entries of a specific action type.
     * 
     * @param action the action type to filter by
     * @return list of all audit trail entries for the specified action
     */
    List<UserAuditTrail> findByAction(String action);
    
    /**
     * Find all audit trail entries for a specific user within a date range.
     * 
     * @param userId the user ID to filter by
     * @param startDate the start of the date range
     * @param endDate the end of the date range
     * @return list of audit trail entries within the specified period
     */
    @Query("SELECT a FROM UserAuditTrail a WHERE a.userId = :userId " +
           "AND a.timestamp BETWEEN :startDate AND :endDate ORDER BY a.timestamp DESC")
    List<UserAuditTrail> findByUserIdAndTimestampBetween(
        @Param("userId") Long userId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
    
    /**
     * Find all audit trail entries for a specific action within a date range.
     * 
     * @param action the action type to filter by
     * @param startDate the start of the date range
     * @param endDate the end of the date range
     * @return list of audit trail entries for the specified action within the period
     */
    @Query("SELECT a FROM UserAuditTrail a WHERE a.action = :action " +
           "AND a.timestamp BETWEEN :startDate AND :endDate ORDER BY a.timestamp DESC")
    List<UserAuditTrail> findByActionAndTimestampBetween(
        @Param("action") String action,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
    
    /**
     * Find all audit trail entries for a specific user and action type.
     * 
     * @param userId the user ID to filter by
     * @param action the action type to filter by
     * @return list of audit trail entries for the specified user and action
     */
    @Query("SELECT a FROM UserAuditTrail a WHERE a.userId = :userId AND a.action = :action " +
           "ORDER BY a.timestamp DESC")
    List<UserAuditTrail> findByUserIdAndAction(
        @Param("userId") Long userId,
        @Param("action") String action
    );
    
    /**
     * Find audit trail entries by IP address.
     * Useful for security analysis and detecting suspicious activities.
     * 
     * @param ipAddress the IP address to search for
     * @return list of audit trail entries from the specified IP address
     */
    List<UserAuditTrail> findByIpAddress(String ipAddress);
    
    /**
     * Count audit trail entries for a specific user.
     * 
     * @param userId the user ID to count for
     * @return the number of audit trail entries for the user
     */
    long countByUserId(Long userId);
}
