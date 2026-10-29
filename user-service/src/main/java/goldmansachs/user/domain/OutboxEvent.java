package goldmansachs.user.domain;

import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * OutboxEvent entity implementing the Transactional Outbox Pattern.
 * 
 * This pattern ensures reliable event publishing even in case of failures:
 * - Events are stored in the outbox table atomically with business data
 * - A separate publisher polls and publishes events to Kafka
 * - This prevents message loss and ensures at-least-once delivery
 * 
 * Event flow:
 * 1. Event created with status PENDING
 * 2. OutboxPublisher polls for PENDING events
 * 3. Events are published to Kafka
 * 4. Status changed to PUBLISHED
 * 5. Old published events archived (moved to ARCHIVED after retention period)
 * 
 * @author Michael Inyang
 * @version 1.0
 */
@Entity
@Table(name = "user_outbox_events", indexes = {
    @Index(name = "idx_outbox_status", columnList = "status"),
    @Index(name = "idx_outbox_aggregate_id", columnList = "aggregate_id"),
    @Index(name = "idx_outbox_created_at", columnList = "created_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutboxEvent {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * Type of event that occurred.
     * Examples: USER_CREATED, USER_ACTIVATED, USER_DEACTIVATED, USER_DELETED, etc.
     */
    @Column(nullable = false, length = 50)
    private String eventType;
    
    /**
     * JSON serialized payload containing the event data.
     * Contains all relevant information about the event for downstream consumers.
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;
    
    /**
     * The aggregate ID (user ID) this event is related to.
     * Used for event tracking and correlation.
     */
    @Column(nullable = false, length = 50)
    private String aggregateId;
    
    /**
     * The aggregate type this event is related to.
     * For user service, this is typically "User".
     */
    @Column(nullable = false, length = 50)
    private String aggregateType;
    
    /**
     * Current status of this event in the publishing pipeline.
     * @see EventStatus
     */
    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private EventStatus status;
    
    /**
     * Timestamp when the event was created.
     * Automatically set on entity persistence.
     */
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    /**
     * Timestamp when the event was successfully published to Kafka.
     * NULL indicates the event has not yet been published.
     */
    @Column
    private LocalDateTime publishedAt;
    
    /**
     * Number of retry attempts made to publish this event.
     * Used for tracking failed publish attempts.
     */
    @Column
    @Builder.Default
    private Integer retryCount = 0;
    
    /**
     * Error message from the last failed publish attempt (if any).
     * Helps with debugging failed event publications.
     */
    @Column
    private String lastError;
    
    /**
     * Enum representing possible event statuses in the publishing pipeline.
     */
    public enum EventStatus {
        /** Event pending publication to Kafka */
        PENDING,
        /** Event successfully published to Kafka */
        PUBLISHED,
        /** Event failed to publish after retries */
        FAILED,
        /** Event archived (old published events kept for retention then archived) */
        ARCHIVED
    }
    
    /**
     * JPA lifecycle callback to set timestamps before persistence.
     */
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
