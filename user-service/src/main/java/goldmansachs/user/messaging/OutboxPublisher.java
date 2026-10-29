package goldmansachs.user.messaging;

import goldmansachs.user.domain.OutboxEvent;
import goldmansachs.user.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * OutboxPublisher for publishing user events to Kafka.
 * 
 * Implements the Transactional Outbox Pattern:
 * - Polls the outbox_events table for pending events
 * - Publishes events to appropriate Kafka topics
 * - Updates event status to PUBLISHED
 * - Archives old published events
 * 
 * This ensures reliable event publishing even in case of failures.
 * Events are stored atomically with business data and published asynchronously.
 * 
 * @author Michael Inyang
 * @version 1.0
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class OutboxPublisher {
    
    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    
    /**
     * Poll interval for checking pending events (in milliseconds).
     * Injected from application configuration (default: 5000ms).
     */
    @Value("${outbox.poll-interval:5000}")
    private long pollInterval;
    
    /**
     * Retention period for published events before archiving (in days).
     * Injected from application configuration (default: 7 days).
     */
    @Value("${outbox.retention-days:7}")
    private int retentionDays;
    
    /**
     * Maximum number of retries for failed event publishing.
     */
    @Value("${outbox.max-retries:3}")
    private int maxRetries;
    
    /**
     * Scheduled task to poll and publish pending events.
     * Runs at a fixed interval defined by outbox.poll-interval.
     * 
     * This task:
     * 1. Finds all pending events
     * 2. Publishes them to Kafka
     * 3. Updates their status to PUBLISHED
     * 4. Archives old published events
     */
    @Scheduled(fixedDelayString = "${outbox.poll-interval:5000}")
    @Transactional
    public void publishPendingEvents() {
        try {
            List<OutboxEvent> pendingEvents = outboxEventRepository.findPendingEvents();
            
            if (!pendingEvents.isEmpty()) {
                log.debug("Found {} pending events to publish", pendingEvents.size());
                
                for (OutboxEvent event : pendingEvents) {
                    try {
                        publishEvent(event);
                    } catch (Exception e) {
                        log.error("Failed to publish event {}: {}", event.getId(), e.getMessage());
                        handleFailedEvent(event);
                    }
                }
            }
            
            // Archive old published events
            archiveOldPublishedEvents();
            
        } catch (Exception e) {
            log.error("Error in publishPendingEvents task", e);
        }
    }
    
    /**
     * Scheduled task to retry failed events.
     * Runs daily to attempt republishing of failed events.
     */
    @Scheduled(cron = "0 0 1 * * ?") // 1:00 AM daily
    @Transactional
    public void retryFailedEvents() {
        try {
            log.info("Starting retry of failed events");
            
            List<OutboxEvent> failedEvents = outboxEventRepository.findFailedEvents();
            
            for (OutboxEvent event : failedEvents) {
                if (event.getRetryCount() < maxRetries) {
                    try {
                        log.info("Retrying failed event {}, attempt {}/{}", 
                                event.getId(), event.getRetryCount() + 1, maxRetries);
                        publishEvent(event);
                    } catch (Exception e) {
                        event.setRetryCount(event.getRetryCount() + 1);
                        event.setLastError(e.getMessage());
                        outboxEventRepository.save(event);
                        log.warn("Retry attempt {} failed for event {}", 
                                event.getRetryCount(), event.getId());
                    }
                } else {
                    log.error("Event {} exceeded max retries ({}), giving up", 
                            event.getId(), maxRetries);
                }
            }
            
        } catch (Exception e) {
            log.error("Error in retryFailedEvents task", e);
        }
    }
    
    /**
     * Publish an event to Kafka.
     * 
     * @param event the outbox event to publish
     */
    private void publishEvent(OutboxEvent event) {
        log.debug("Publishing event: {}", event.getId());
        
        // Determine the Kafka topic based on event type
        String topic = determineTopic(event.getEventType());
        
        // Send event to Kafka
        kafkaTemplate.send(topic, event.getAggregateId(), event.getPayload());
        
        // Update event status to PUBLISHED
        event.setStatus(OutboxEvent.EventStatus.PUBLISHED);
        event.setPublishedAt(LocalDateTime.now());
        event.setRetryCount(0);
        event.setLastError(null);
        
        outboxEventRepository.save(event);
        
        log.info("Event {} published to topic {}", event.getId(), topic);
    }
    
    /**
     * Handle a failed event publishing attempt.
     * 
     * @param event the event that failed to publish
     */
    private void handleFailedEvent(OutboxEvent event) {
        event.setRetryCount(event.getRetryCount() + 1);
        
        if (event.getRetryCount() >= maxRetries) {
            event.setStatus(OutboxEvent.EventStatus.FAILED);
            log.error("Event {} marked as FAILED after {} retries", event.getId(), maxRetries);
        }
        
        outboxEventRepository.save(event);
    }
    
    /**
     * Archive old published events to free up space.
     * Events older than the retention period are marked as ARCHIVED.
     */
    private void archiveOldPublishedEvents() {
        try {
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(retentionDays);
            List<OutboxEvent> oldPublishedEvents = outboxEventRepository
                    .findByStatusAndCreatedAtBefore(OutboxEvent.EventStatus.PUBLISHED, cutoffDate);
            
            if (!oldPublishedEvents.isEmpty()) {
                log.debug("Archiving {} old published events", oldPublishedEvents.size());
                
                for (OutboxEvent event : oldPublishedEvents) {
                    event.setStatus(OutboxEvent.EventStatus.ARCHIVED);
                }
                
                outboxEventRepository.saveAll(oldPublishedEvents);
                log.info("Archived {} events", oldPublishedEvents.size());
            }
        } catch (Exception e) {
            log.error("Error archiving old events", e);
        }
    }
    
    /**
     * Determine the Kafka topic based on event type.
     * Routes events to the appropriate topic for consumption by interested services.
     * 
     * @param eventType the type of event
     * @return the Kafka topic name
     */
    private String determineTopic(String eventType) {
        return switch (eventType) {
            case "USER_CREATED" -> "user-events";
            case "USER_ACTIVATED" -> "user-events";
            case "USER_DEACTIVATED" -> "user-events";
            case "USER_DELETED" -> "user-events";
            case "ROLE_ASSIGNED" -> "user-events";
            case "ROLE_REMOVED" -> "user-events";
            default -> "user-events";
        };
    }
    
    /**
     * Get the count of pending events.
     * Useful for monitoring event publishing backlog.
     * 
     * @return the number of pending events
     */
    public long getPendingEventCount() {
        return outboxEventRepository.countPendingEvents();
    }
    
    /**
     * Get the count of failed events.
     * Useful for monitoring events that need attention.
     * 
     * @return the number of failed events
     */
    public long getFailedEventCount() {
        return outboxEventRepository.countFailedEvents();
    }
}
