package goldmansachs.settlement.repository;

import goldmansachs.settlement.domain.SettlementRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SettlementRepository extends JpaRepository<SettlementRecord, Long> {
    Optional<SettlementRecord> findBySettlementId(String settlementId);

    Optional<SettlementRecord> findByOrderId(String orderId);

    List<SettlementRecord> findByStatus(SettlementRecord.SettlementStatus status);

    List<SettlementRecord> findByCustomerId(String customerId);

    List<SettlementRecord> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

    List<SettlementRecord> findByMerchantId(String merchantId);

    @Query("SELECT s FROM SettlementRecord s WHERE s.status = 'PENDING' ORDER BY s.createdAt ASC")
    List<SettlementRecord> findPendingSettlements();

    @Query("SELECT COUNT(s) FROM SettlementRecord s WHERE s.status = 'COMPLETED' AND s.createdAt >= ?1")
    long countCompletedSince(LocalDateTime dateTime);
}
