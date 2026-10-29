package goldmansachs.user.repository;

import goldmansachs.user.domain.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository interface for OutboxEvent entity providing database access operations.
 * 
 * This repository handles CRUD operations and custom queries for OutboxEvent entities.
 * OutboxEvents are used to implement the transactional outbox pattern for reliable
 * event publishing to Kafka.
 * 
 * @author Michael Inyang
 * @version 1.0
 */
@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {
    
    /**
     * Find all pending events that need to be published.
     * These are events that haven't been successfully published to Kafka yet.
     * 
     * @return list of all pending outbox events
     */
    @Query("SELECT o FROM OutboxEvent o WHERE o.status = 'PENDING' ORDER BY o.createdAt ASC")
    List<OutboxEvent> findPendingEvents();
    
    /**
     * Find all events with a specific status.
     * 
     * @param status the event status to filter by
     * @return list of events with the specified status
     */
    List<OutboxEvent> findByStatus(OutboxEvent.EventStatus status);
    
    /**
     * Find all published events older than a specific date.
     * Used for archiving old published events.
     * 
     * @param status the event status to filter by
     * @param dateTime the cutoff date/time
     * @return list of published events created before the specified date
     */
    @Query("SELECT o FROM OutboxEvent o WHERE o.status = :status AND o.createdAt < :dateTime")
    List<OutboxEvent> findByStatusAndCreatedAtBefore(
        @Param("status") OutboxEvent.EventStatus status,
        @Param("dateTime") LocalDateTime dateTime
    );
    
    /**
     * Find events by aggregate ID and type.
     * Useful for tracking all events related to a specific user.
     * 
     * @param aggregateId the aggregate ID (user ID)
     * @param aggregateType the aggregate type (usually "User")
     * @return list of events for the specified aggregate
     */
    @Query("SELECT o FROM OutboxEvent o WHERE o.aggregateId = :aggregateId AND o.aggregateType = :aggregateType")
    List<OutboxEvent> findByAggregateIdAndAggregateType(
        @Param("aggregateId") String aggregateId,
        @Param("aggregateType") String aggregateType
    );
    
    /**
     * Find events by event type.
     * 
     * @param eventType the event type to filter by
     * @return list of events of the specified type
     */
    List<OutboxEvent> findByEventType(String eventType);
    
    /**
     * Find failed events that need to be retried.
     * Events that failed to publish are marked as FAILED.
     * 
     * @return list of failed outbox events
     */
    @Query("SELECT o FROM OutboxEvent o WHERE o.status = 'FAILED' ORDER BY o.createdAt ASC")
    List<OutboxEvent> findFailedEvents();
    
    /**
     * Find events created within a specific date range.
     * 
     * @param startDate the start of the date range
     * @param endDate the end of the date range
     * @return list of events created within the specified period
     */
    @Query("SELECT o FROM OutboxEvent o WHERE o.createdAt BETWEEN :startDate AND :endDate")
    List<OutboxEvent> findByCreatedAtBetween(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
    
    /**
     * Count pending events.
     * Useful for monitoring event publishing backlog.
     * 
     * @return the number of pending events
     */
    @Query("SELECT COUNT(o) FROM OutboxEvent o WHERE o.status = 'PENDING'")
    long countPendingEvents();
    
    /**
     * Count failed events.
     * Useful for monitoring events that need attention.
     * 
     * @return the number of failed events
     */
    @Query("SELECT COUNT(o) FROM OutboxEvent o WHERE o.status = 'FAILED'")
    long countFailedEvents();
}
