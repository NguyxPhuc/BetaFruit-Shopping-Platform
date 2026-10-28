package com.fu.SWP391_BetaFruit.repository;

import com.fu.SWP391_BetaFruit.entity.PayoutRequest;
import com.fu.SWP391_BetaFruit.entity.Shop;
import com.fu.SWP391_BetaFruit.enums.PayoutStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface PayoutRequestRepository extends JpaRepository<PayoutRequest,Integer> {

    @Query("SELECT COALESCE(SUM(p.requestedAmount), 0) FROM PayoutRequest p WHERE p.shop.shopId = :shopId AND p.status = :status")
    BigDecimal sumAmountByShopAndStatus(@Param("shopId") Integer shopId, @Param("status") PayoutStatus status);

    List<PayoutRequest> findByShop(Shop shop);

    Optional<PayoutRequest> findByRequestIdAndShop(Integer requestId, Shop shop);

    Optional<PayoutRequest> findByRequestId(Integer requestId);

    List<PayoutRequest> findAllByStatus(PayoutStatus status);
}
