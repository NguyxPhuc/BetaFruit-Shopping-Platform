package com.fu.SWP391_BetaFruit.repository;

import com.fu.SWP391_BetaFruit.entity.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import com.fu.SWP391_BetaFruit.enums.PaymentStatus;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction,Integer> {
    Optional<PaymentTransaction> findByTransactionRef(String txnRef);
    @Query("select p.order.orderId from PaymentTransaction p where p.transactionRef = :ref")
    Optional<Integer> findOrderIdByTransactionRef(@Param("ref") String transactionRef);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PaymentTransaction p where p.transactionRef = :ref")
    Optional<PaymentTransaction> findLockedByTransactionRef(@Param("ref") String transactionRef);

    boolean existsByOrderOrderIdAndGatewayAndStatus(Integer orderId, String gateway, PaymentStatus status);
    List<PaymentTransaction> findByOrderOrderIdOrderByCreatedAtAscTransactionIdAsc(Integer orderId);
}
