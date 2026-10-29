package goldmansachs.settlement.messaging;

import goldmansachs.settlement.domain.SettlementRecord;
import goldmansachs.settlement.repository.SettlementRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderCreatedConsumer {
    private final SettlementRepository settlementRepository;
    private final ObjectMapper objectMapper;
    private static final BigDecimal SETTLEMENT_FEE_PERCENTAGE = new BigDecimal("0.02"); // 2% fee

    @KafkaListener(topics = "order-created-events", groupId = "${spring.kafka.consumer.group-id:settlement-service-group}")
    @Transactional
    public void consumeOrderCreatedEvent(
            @Payload String message,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            Acknowledgment acknowledgment) {
        try {
            log.info("Received message from topic: {}", topic);

            Map<String, Object> orderData = objectMapper.readValue(message, HashMap.class);

            String orderId = orderData.get("orderId").toString();
            String orderNumber = orderData.get("orderNumber").toString();
            String customerId = orderData.get("customerId").toString();

            BigDecimal totalAmount = new BigDecimal(orderData.get("totalAmount").toString());
            BigDecimal feeAmount = totalAmount.multiply(SETTLEMENT_FEE_PERCENTAGE);
            BigDecimal netAmount = totalAmount.subtract(feeAmount);

            SettlementRecord settlement = SettlementRecord.builder()
                    .settlementId(UUID.randomUUID().toString())
                    .orderId(orderId)
                    .orderNumber(orderNumber)
                    .customerId(customerId)
                    .settlementAmount(totalAmount)
                    .feeAmount(feeAmount)
                    .netAmount(netAmount)
                    .status(SettlementRecord.SettlementStatus.PENDING)
                    .paymentMethod("CARD") // Default payment method
                    .merchantId("MERCHANT_001") // Default merchant ID
                    .notes("Auto-created from order: " + orderNumber)
                    .build();

            settlementRepository.save(settlement);

            log.info("Settlement record created for order: {} with settlement ID: {}", 
                    orderNumber, settlement.getSettlementId());

            // Acknowledge the message after successful processing
            if (acknowledgment != null) {
                acknowledgment.acknowledge();
            }
        } catch (Exception e) {
            log.error("Error processing order created event: {}", message, e);
            // In case of error, we don't acknowledge, allowing Kafka to retry
        }
    }

    @KafkaListener(topics = "order-status-changed-events", groupId = "${spring.kafka.consumer.group-id:settlement-service-group}")
    @Transactional
    public void consumeOrderStatusChangedEvent(
            @Payload String message,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            Acknowledgment acknowledgment) {
        try {
            log.info("Received order status changed event from topic: {}", topic);

            Map<String, Object> orderData = objectMapper.readValue(message, HashMap.class);
            String orderId = orderData.get("orderId").toString();
            String newStatus = orderData.get("status").toString();

            // Find existing settlement for this order
            SettlementRecord settlement = settlementRepository.findByOrderId(orderId)
                    .orElse(null);

            if (settlement != null) {
                // Update settlement status based on order status
                if ("DELIVERED".equals(newStatus)) {
                    settlement.setStatus(SettlementRecord.SettlementStatus.PROCESSING);
                    log.info("Settlement status updated to PROCESSING for order: {}", orderId);
                } else if ("CANCELLED".equals(newStatus)) {
                    settlement.setStatus(SettlementRecord.SettlementStatus.CANCELLED);
                    log.info("Settlement cancelled for order: {}", orderId);
                }

                settlementRepository.save(settlement);
            }

            if (acknowledgment != null) {
                acknowledgment.acknowledge();
            }
        } catch (Exception e) {
            log.error("Error processing order status changed event: {}", message, e);
        }
    }
}
