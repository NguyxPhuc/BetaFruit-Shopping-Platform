package com.fu.SWP391_BetaFruit.repository;

import com.fu.SWP391_BetaFruit.entity.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction,Integer> {
    Optional<PaymentTransaction> findByTransactionRef(String txnRef);
}
