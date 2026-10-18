package com.fu.SWP391_BetaFruit.service.impl;

import com.fu.SWP391_BetaFruit.entity.*;
import com.fu.SWP391_BetaFruit.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OrderPricingTests {
    private Order order(Integer couponShopId) {
        Shop shop = new Shop();
        shop.setShopId(2);
        // Explicitly test database rows with no shop-specific commission; new Shop()
        // retains the original entity's 5% default.
        shop.setCommissionRate(null);
        Order order = new Order();
        order.setOrderId(12);
        order.setShop(shop);
        order.setCustomer(new User());
        order.setTotalAmount(new BigDecimal("100000"));
        order.setPlatformFee(new BigDecimal("5000"));
        order.setDiscountAmount(new BigDecimal("10000"));
        order.setShippingFee(new BigDecimal("20000"));
        order.setFinalAmount(new BigDecimal("110000"));
        order.setShopNetReceived(new BigDecimal("1")); // stale stored value must not drive the UI
        Coupon coupon = new Coupon();
        if (couponShopId != null) {
            Shop couponShop = new Shop();
            couponShop.setShopId(couponShopId);
            coupon.setShop(couponShop);
        }
        coupon.setCouponCode("SAVE10");
        order.setCoupon(coupon);
        return order;
    }

    @Test void platformCouponIsFundedByAdminAndShippingIsExcluded() {
        var result = OrderPricing.calculate(order(0));
        assertEquals("PLATFORM", result.couponSource());
        assertEquals("PLATFORM", result.commissionSource());
        assertEquals(new BigDecimal("5000.00"), result.platformFee());
        assertEquals(BigDecimal.ZERO, result.shopDiscountAmount());
        assertEquals(new BigDecimal("95000.00"), result.shopNetReceived());
        assertEquals(result, OrderPricing.calculate(order(null)));
    }

    @Test void shopCouponIsDeductedAndCommissionUsesPriceBeforeVoucher() {
        var result = OrderPricing.calculate(order(2));
        assertEquals("SHOP", result.couponSource());
        assertEquals(new BigDecimal("10000"), result.shopDiscountAmount());
        assertEquals(new BigDecimal("5000.00"), result.platformFee());
        assertEquals(new BigDecimal("85000.00"), result.shopNetReceived());
    }

    @Test void shopRateIncludingZeroOverridesPlatformDefault() {
        var order = order(2);
        order.getShop().setCommissionRate(new BigDecimal("8"));
        assertEquals(new BigDecimal("82000.00"), OrderPricing.calculate(order).shopNetReceived());
        order.getShop().setCommissionRate(BigDecimal.ZERO);
        var result = OrderPricing.calculate(order);
        assertEquals("SHOP", result.commissionSource());
        assertEquals(new BigDecimal("90000.00"), result.shopNetReceived());
    }

    @Test void noCouponDoesNotChargeAnUnrelatedDiscountAndNullDiscountIsZero() {
        var order = order(2);
        order.setDiscountAmount(null);
        assertEquals(new BigDecimal("95000.00"), OrderPricing.calculate(order).shopNetReceived());
        order.setCoupon(null);
        order.setDiscountAmount(new BigDecimal("10000"));
        var result = OrderPricing.calculate(order);
        assertEquals("NONE", result.couponSource());
        assertEquals(new BigDecimal("95000.00"), result.shopNetReceived());
    }

    @Test void rejectsCouponOwnedByAnotherShopAndInvalidRate() {
        assertThrows(ResponseStatusException.class, () -> OrderPricing.calculate(order(3)));
        assertThrows(ResponseStatusException.class, () -> OrderPricing.calculate(order(-1)));
        var order = order(0);
        for (var rate : List.of(new BigDecimal("101"), new BigDecimal("-1"))) {
            order.getShop().setCommissionRate(rate);
            assertThrows(ResponseStatusException.class, () -> OrderPricing.calculate(order));
        }
        order.getShop().setCommissionRate(null);
        order.setPlatformFee(null);
        assertThrows(ResponseStatusException.class, () -> OrderPricing.calculate(order));
    }

    @Test void roundsFeeHalfUpAndPreservesNegativeNetReceipt() {
        var order = order(2);
        order.getShop().setCommissionRate(new BigDecimal("5"));
        order.setTotalAmount(new BigDecimal("100.10"));
        order.setDiscountAmount(new BigDecimal("100.10"));
        var result = OrderPricing.calculate(order);
        assertEquals(new BigDecimal("5.01"), result.platformFee());
        assertEquals(new BigDecimal("-5.01"), result.shopNetReceived());
    }

    @Test void listAndDetailUseSamePricingWithoutChangingStoredAmounts() {
        var orders = mock(OrderRepository.class);
        var order = order(2);
        var service = new ShopOrderServiceImpl(orders, mock(OrderStatusHistoryRepository.class),
                mock(UserRepository.class), mock(OrderItemRepository.class),
                mock(DeliveryAssignmentRepository.class), mock(PaymentTransactionRepository.class),
                mock(OrderStockService.class));
        when(orders.searchOrdersForShopOwner(eq(7), isNull(), isNull(), any()))
                .thenReturn(new PageImpl<>(List.of(order)));
        when(orders.findDetailForShopOwner(12, 7)).thenReturn(Optional.of(order));
        var list = service.getOrdersForShopOwner(7, null, null, "createdAt", "desc", 0, 10);
        var detail = service.getOrderDetailForShopOwner(12, 7);
        assertEquals(list.getContent().getFirst(), detail.summary());
        assertEquals(new BigDecimal("85000.00"), detail.summary().receivedAmount());
        assertEquals(new BigDecimal("1"), order.getShopNetReceived());
        assertEquals(new BigDecimal("5000"), order.getPlatformFee());
    }

    @Test void existingOrderWithMonetaryPlatformFeeLoadsWithoutTreatingItAsPercent() {
        var order = order(0);
        order.setTotalAmount(new BigDecimal("400000.00"));
        order.setDiscountAmount(new BigDecimal("20000.00"));
        order.setPlatformFee(new BigDecimal("20000.00"));
        var result = OrderPricing.calculate(order);
        assertEquals(new BigDecimal("20000.00"), result.platformFee());
        assertEquals(0, new BigDecimal("5").compareTo(result.commissionRate()));
        assertEquals(new BigDecimal("380000.00"), result.shopNetReceived());
    }

    @Test void smallStoredFeesAreAlsoCurrencyNotPercentAndZeroTotalIsSupported() {
        var order = order(0);
        order.setTotalAmount(new BigDecimal("1000"));
        order.setPlatformFee(new BigDecimal("50"));
        var result = OrderPricing.calculate(order);
        assertEquals(new BigDecimal("950.00"), result.shopNetReceived());
        assertEquals(0, new BigDecimal("5").compareTo(result.commissionRate()));
        order.setTotalAmount(BigDecimal.ZERO);
        order.setPlatformFee(BigDecimal.ZERO);
        assertEquals(new BigDecimal("0.00"), OrderPricing.calculate(order).shopNetReceived());
        assertEquals(BigDecimal.ZERO, OrderPricing.calculate(order).commissionRate());
    }

    @Test void invalidShopCouponBlocksConfirmationBeforeStockIsDeducted() {
        var orders = mock(OrderRepository.class);
        var stock = mock(OrderStockService.class);
        var history = mock(OrderStatusHistoryRepository.class);
        var order = order(3);
        order.setOrderStatus(com.fu.SWP391_BetaFruit.enums.OrderStatus.PENDING);
        when(orders.findByOrderIdAndShopOwnerUserId(12, 7)).thenReturn(Optional.of(order));
        var service = new ShopOrderServiceImpl(orders, history, mock(UserRepository.class),
                mock(OrderItemRepository.class), mock(DeliveryAssignmentRepository.class),
                mock(PaymentTransactionRepository.class), stock);
        assertThrows(ResponseStatusException.class, () -> service.confirmOrder(12, 7));
        verifyNoInteractions(stock, history);
    }
}
