package goldmansachs.order.repository;

import goldmansachs.order.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByOrderNumber(String orderNumber);

    List<Order> findByCustomerId(String customerId);

    List<Order> findByStatus(Order.OrderStatus status);

    List<Order> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
}
