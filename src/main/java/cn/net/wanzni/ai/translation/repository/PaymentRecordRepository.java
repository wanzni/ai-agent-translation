package cn.net.wanzni.ai.translation.repository;

import cn.net.wanzni.ai.translation.entity.PaymentRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRecordRepository extends JpaRepository<PaymentRecord, Long> {
    List<PaymentRecord> findByOrderIdOrderByCreatedAtDesc(Long orderId);
    Optional<PaymentRecord> findTopByPaymentNo(String paymentNo);
}