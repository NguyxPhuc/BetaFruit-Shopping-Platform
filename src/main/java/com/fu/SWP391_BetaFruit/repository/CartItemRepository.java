package com.fu.SWP391_BetaFruit.repository;

import com.fu.SWP391_BetaFruit.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Integer> {
    @Query("SELECT COALESCE(SUM(i.quantity), 0) FROM CartItem i WHERE i.cart.customer.userId = :customerId")
    long sumQuantityByCustomerId(@Param("customerId") Integer customerId);

    @Query("SELECT i FROM CartItem i JOIN FETCH i.variant v JOIN FETCH v.product p " +
            "JOIN FETCH p.shop s JOIN FETCH s.owner WHERE i.cart.cartId = :cartId ORDER BY i.cartItemId")
    List<CartItem> findSummaryItems(@Param("cartId") Integer cartId);
    Optional<CartItem> findByCart_CartIdAndVariant_VariantId(Integer cartId, Integer variantId);
    Optional<CartItem> findByCartItemIdAndCart_Customer_UserId(Integer itemId, Integer customerId);
    void deleteByCart_CartId(Integer cartId);
}
