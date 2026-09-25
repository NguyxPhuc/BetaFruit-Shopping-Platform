package com.fu.SWP391_BetaFruit.repository;

import com.fu.SWP391_BetaFruit.entity.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Integer> {
    List<PaymentTransaction> findByOrderOrderIdOrderByCreatedAtAscTransactionIdAsc(Integer orderId);
}
