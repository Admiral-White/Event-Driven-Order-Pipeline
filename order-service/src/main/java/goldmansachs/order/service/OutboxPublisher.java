package goldmansachs.order.service;

import goldmansachs.order.domain.OutboxEvent;
import goldmansachs.order.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class OutboxPublisher {
    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private static final String KAFKA_TOPIC = "order-events";
    private static final long POLL_INTERVAL_MS = 5000; // Poll every 5 seconds

    @Scheduled(fixedDelayString = "${outbox.poll-interval:5000}")
    @Transactional
    public void publishPendingEvents() {
        log.debug("Starting to publish pending outbox events");

        try {
            List<OutboxEvent> pendingEvents = outboxRepository.findByStatus(OutboxEvent.EventStatus.PENDING);

            if (pendingEvents.isEmpty()) {
                log.debug("No pending events to publish");
                return;
            }

            log.info("Found {} pending events to publish", pendingEvents.size());

            for (OutboxEvent event : pendingEvents) {
                try {
                    publishEvent(event);
                } catch (Exception e) {
                    log.error("Failed to publish event with id: {}", event.getId(), e);
                    event.setStatus(OutboxEvent.EventStatus.FAILED);
                    event.setErrorMessage(e.getMessage());
                    outboxRepository.save(event);
                }
            }
        } catch (Exception e) {
            log.error("Error in publishPendingEvents", e);
        }
    }

    @Transactional
    public void publishEvent(OutboxEvent event) {
        try {
            String topic = determineTopic(event.getEventType());
            kafkaTemplate.send(topic, event.getAggregateId(), event.getPayload());

            event.setStatus(OutboxEvent.EventStatus.PUBLISHED);
            outboxRepository.save(event);

            log.info("Event published successfully. Event type: {}, Aggregate ID: {}", 
                    event.getEventType(), event.getAggregateId());
        } catch (Exception e) {
            log.error("Failed to publish event: {}", event.getId(), e);
            throw new RuntimeException("Failed to publish event", e);
        }
    }

    private String determineTopic(String eventType) {
        return switch (eventType) {
            case "ORDER_CREATED" -> "order-created-events";
            case "ORDER_STATUS_CHANGED" -> "order-status-changed-events";
            default -> "order-events";
        };
    }

    @Transactional
    public void archiveOldPublishedEvents() {
        log.debug("Starting to archive old published events");

        LocalDateTime cutoffDate = LocalDateTime.now().minusMonths(1);
        List<OutboxEvent> oldEvents = outboxRepository.findByStatusAndCreatedAtBefore(
                OutboxEvent.EventStatus.PUBLISHED, cutoffDate);

        if (!oldEvents.isEmpty()) {
            for (OutboxEvent event : oldEvents) {
                event.setStatus(OutboxEvent.EventStatus.ARCHIVED);
                outboxRepository.save(event);
            }
            log.info("Archived {} old published events", oldEvents.size());
        }
    }
}
