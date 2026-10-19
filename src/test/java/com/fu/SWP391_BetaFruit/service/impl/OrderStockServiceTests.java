package com.fu.SWP391_BetaFruit.service.impl;

import com.fu.SWP391_BetaFruit.entity.*;
import com.fu.SWP391_BetaFruit.enums.InventoryTransactionType;
import com.fu.SWP391_BetaFruit.repository.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OrderStockServiceTests {
    private final OrderItemRepository items = mock(OrderItemRepository.class);
    private final ProductVariantRepository variants = mock(ProductVariantRepository.class);
    private final InventoryTransactionRepository transactions = mock(InventoryTransactionRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final OrderStockService service = new OrderStockService(items, variants, transactions, users);
    private final Shop shop = new Shop();
    private final Order order = new Order();

    OrderStockServiceTests() {
        shop.setShopId(2);
        order.setShop(shop);
        order.setOrderId(12);
    }

    private ProductVariant variant(int id, int stock) {
        Product product = new Product();
        product.setShop(shop);
        ProductVariant variant = new ProductVariant();
        variant.setVariantId(id);
        variant.setProduct(product);
        variant.setStockQuantity(stock);
        when(variants.findForStockUpdate(id)).thenReturn(Optional.of(variant));
        return variant;
    }

    private OrderItem item(ProductVariant variant, int quantity) {
        OrderItem item = new OrderItem();
        item.setVariant(variant);
        item.setQuantity(quantity);
        return item;
    }

    @Test void aggregatesDuplicateVariantsAndLogsExactDeductionAndRestoration() {
        var variant = variant(1, 10);
        when(items.findByOrderOrderIdOrderByOrderItemIdAsc(12))
                .thenReturn(List.of(item(variant, 2), item(variant, 3)));
        service.deduct(order, 7);
        assertEquals(5, variant.getStockQuantity());
        service.restore(order, 7);
        assertEquals(10, variant.getStockQuantity());
        var logs = ArgumentCaptor.forClass(InventoryTransaction.class);
        verify(transactions, times(2)).save(logs.capture());
        var deduction = logs.getAllValues().get(0);
        assertEquals(InventoryTransactionType.ORDER_DEDUCT, deduction.getTransactionType());
        assertEquals(-5, deduction.getQuantityChanged());
        assertEquals(10, deduction.getPreviousStock());
        assertEquals(5, deduction.getNewStock());
        var refund = logs.getAllValues().get(1);
        assertEquals(InventoryTransactionType.CANCEL_REFUND, refund.getTransactionType());
        assertEquals(5, refund.getQuantityChanged());
        assertEquals(5, refund.getPreviousStock());
        assertEquals(10, refund.getNewStock());
    }

    @Test void shortageInLaterVariantLeavesAllStockUntouchedAndLocksInIdOrder() {
        var first = variant(1, 10);
        var second = variant(2, 1);
        when(items.findByOrderOrderIdOrderByOrderItemIdAsc(12))
                .thenReturn(List.of(item(second, 2), item(first, 3)));
        var error = assertThrows(ResponseStatusException.class, () -> service.deduct(order, 7));
        assertEquals(409, error.getStatusCode().value());
        assertEquals(10, first.getStockQuantity());
        assertEquals(1, second.getStockQuantity());
        var sequence = inOrder(variants);
        sequence.verify(variants).findForStockUpdate(1);
        sequence.verify(variants).findForStockUpdate(2);
        verifyNoInteractions(transactions);
    }

    @Test void exactStockCanBeSoldOut() {
        var variant = variant(1, 3);
        when(items.findByOrderOrderIdOrderByOrderItemIdAsc(12)).thenReturn(List.of(item(variant, 3)));
        service.deduct(order, 7);
        assertEquals(0, variant.getStockQuantity());
    }

    @Test void invalidQuantityEmptyOrderAndForeignShopCannotChangeStock() {
        var variant = variant(1, 10);
        when(items.findByOrderOrderIdOrderByOrderItemIdAsc(12)).thenReturn(List.of());
        assertThrows(ResponseStatusException.class, () -> service.deduct(order, 7));
        when(items.findByOrderOrderIdOrderByOrderItemIdAsc(12)).thenReturn(List.of(item(variant, 0)));
        assertThrows(ResponseStatusException.class, () -> service.deduct(order, 7));
        Shop otherShop = new Shop();
        otherShop.setShopId(99);
        variant.getProduct().setShop(otherShop);
        when(items.findByOrderOrderIdOrderByOrderItemIdAsc(12)).thenReturn(List.of(item(variant, 2)));
        assertThrows(ResponseStatusException.class, () -> service.deduct(order, 7));
        assertEquals(10, variant.getStockQuantity());
        verifyNoInteractions(transactions);
    }

    @Test void restorationOverflowDoesNotWrapStockToNegative() {
        var variant = variant(1, Integer.MAX_VALUE);
        when(items.findByOrderOrderIdOrderByOrderItemIdAsc(12)).thenReturn(List.of(item(variant, 1)));
        assertThrows(ResponseStatusException.class, () -> service.restore(order, 7));
        assertEquals(Integer.MAX_VALUE, variant.getStockQuantity());
        verifyNoInteractions(transactions);
    }
}
