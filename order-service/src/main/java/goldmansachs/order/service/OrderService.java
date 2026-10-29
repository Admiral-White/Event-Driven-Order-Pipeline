package goldmansachs.order.service;

import goldmansachs.order.domain.Order;
import goldmansachs.order.domain.OutboxEvent;
import goldmansachs.order.repository.OrderRepository;
import goldmansachs.order.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public Order createOrder(OrderCreateRequest request) {
        log.info("Creating order for customer: {}", request.getCustomerId());

        Order order = Order.builder()
                .orderNumber(generateOrderNumber())
                .customerId(request.getCustomerId())
                .productId(request.getProductId())
                .quantity(request.getQuantity())
                .unitPrice(request.getUnitPrice())
                .totalAmount(request.getUnitPrice().multiply(BigDecimal.valueOf(request.getQuantity())))
                .status(Order.OrderStatus.PENDING)
                .build();

        Order savedOrder = orderRepository.save(order);

        // Create outbox event for order created
        createOutboxEvent(savedOrder, "ORDER_CREATED");

        log.info("Order created successfully with order number: {}", savedOrder.getOrderNumber());
        return savedOrder;
    }

    private String generateOrderNumber() {
        return "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    @Transactional
    public void createOutboxEvent(Order order, String eventType) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("orderId", order.getId());
            payload.put("orderNumber", order.getOrderNumber());
            payload.put("customerId", order.getCustomerId());
            payload.put("productId", order.getProductId());
            payload.put("quantity", order.getQuantity());
            payload.put("totalAmount", order.getTotalAmount());
            payload.put("status", order.getStatus());
            payload.put("createdAt", order.getCreatedAt());

            OutboxEvent event = OutboxEvent.builder()
                    .eventType(eventType)
                    .payload(objectMapper.writeValueAsString(payload))
                    .aggregateId(order.getId().toString())
                    .aggregateType("Order")
                    .status(OutboxEvent.EventStatus.PENDING)
                    .build();

            outboxRepository.save(event);
            log.info("Outbox event created for order: {} with event type: {}", order.getOrderNumber(), eventType);
        } catch (Exception e) {
            log.error("Failed to create outbox event for order: {}", order.getOrderNumber(), e);
            throw new RuntimeException("Failed to create outbox event", e);
        }
    }

    @Transactional(readOnly = true)
    public Order getOrderByNumber(String orderNumber) {
        return orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderNumber));
    }

    @Transactional(readOnly = true)
    public List<Order> getOrdersByCustomer(String customerId) {
        return orderRepository.findByCustomerId(customerId);
    }

    @Transactional
    public Order updateOrderStatus(Long orderId, Order.OrderStatus newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with id: " + orderId));

        order.setStatus(newStatus);
        Order updatedOrder = orderRepository.save(order);

        createOutboxEvent(updatedOrder, "ORDER_STATUS_CHANGED");

        return updatedOrder;
    }

    public static class OrderCreateRequest {
        private String customerId;
        private String productId;
        private Integer quantity;
        private BigDecimal unitPrice;

        public OrderCreateRequest() {}

        public OrderCreateRequest(String customerId, String productId, Integer quantity, BigDecimal unitPrice) {
            this.customerId = customerId;
            this.productId = productId;
            this.quantity = quantity;
            this.unitPrice = unitPrice;
        }

        public String getCustomerId() { return customerId; }
        public void setCustomerId(String customerId) { this.customerId = customerId; }

        public String getProductId() { return productId; }
        public void setProductId(String productId) { this.productId = productId; }

        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }

        public BigDecimal getUnitPrice() { return unitPrice; }
        public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    }

    public static class OrderNotFoundException extends RuntimeException {
        public OrderNotFoundException(String message) {
            super(message);
        }
    }
}
