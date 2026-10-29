package goldmansachs.order.repository;

import goldmansachs.order.domain.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OutboxRepository extends JpaRepository<OutboxEvent, Long> {
    List<OutboxEvent> findByStatus(OutboxEvent.EventStatus status);

    List<OutboxEvent> findByStatusAndCreatedAtBefore(OutboxEvent.EventStatus status, LocalDateTime dateTime);

    @Query("SELECT o FROM OutboxEvent o WHERE o.status = 'PENDING' ORDER BY o.createdAt ASC")
    List<OutboxEvent> findPendingEvents();

    List<OutboxEvent> findByAggregateIdAndAggregateType(String aggregateId, String aggregateType);
}
