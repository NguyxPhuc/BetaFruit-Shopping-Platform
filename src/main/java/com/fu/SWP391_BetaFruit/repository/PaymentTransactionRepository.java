package com.fu.SWP391_BetaFruit.repository;

import com.fu.SWP391_BetaFruit.entity.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Integer> {
    Optional<PaymentTransaction> findByTransactionRef(String txnRef);

    List<PaymentTransaction> findByOrderOrderIdOrderByCreatedAtAscTransactionIdAsc(Integer orderId);

    Optional<PaymentTransaction> findByTransactionRefAndOrder_Customer_UserId(String transactionRef,
            Integer orderCustomerUserId);

    List<PaymentTransaction> findByOrder_Customer_UserIdOrderByCreatedAtDesc(Integer customerId);
}
