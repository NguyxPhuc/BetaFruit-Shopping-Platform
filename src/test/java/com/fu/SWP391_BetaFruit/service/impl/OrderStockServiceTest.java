package com.fu.SWP391_BetaFruit.service.impl;

import com.fu.SWP391_BetaFruit.entity.*;
import com.fu.SWP391_BetaFruit.enums.InventoryTransactionType;
import com.fu.SWP391_BetaFruit.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OrderStockServiceTest {
    OrderItemRepository items = mock(OrderItemRepository.class);
    ProductVariantRepository variants = mock(ProductVariantRepository.class);
    InventoryTransactionRepository transactions = mock(InventoryTransactionRepository.class);
    UserRepository users = mock(UserRepository.class);
    OrderStockService service = new OrderStockService(items, variants, transactions, users);
    Order order = new Order();
    Shop shop = new Shop();

    OrderStockServiceTest() {
        shop.setShopId(10);
        order.setOrderId(1);
        order.setShop(shop);
    }

    ProductVariant variant(int id, int quantity) {
        Product product = new Product();
        product.setShop(shop);
        ProductVariant variant = new ProductVariant();
        variant.setVariantId(id);
        variant.setProduct(product);
        variant.setStockQuantity(quantity);
        when(variants.findForStockUpdate(id)).thenReturn(Optional.of(variant));
        return variant;
    }

    OrderItem line(ProductVariant variant, int quantity) {
        OrderItem item = new OrderItem();
        item.setVariant(variant);
        item.setQuantity(quantity);
        return item;
    }

    @Test void exactStockIsAllowedAndLogged() {
        ProductVariant variant = variant(2, 5);
        when(items.findByOrderOrderIdOrderByOrderItemIdAsc(1)).thenReturn(List.of(line(variant, 5)));
        service.deduct(order, 7);
        assertEquals(0, variant.getStockQuantity());
        verify(transactions).save(argThat(tx -> tx.getTransactionType() == InventoryTransactionType.ORDER_DEDUCT));
    }

    @Test void shortageOnLaterLineDoesNotChangeAnyStock() {
        ProductVariant first = variant(2, 10);
        ProductVariant second = variant(3, 1);
        when(items.findByOrderOrderIdOrderByOrderItemIdAsc(1)).thenReturn(List.of(line(first, 2), line(second, 2)));
        var error = assertThrows(ResponseStatusException.class, () -> service.deduct(order, 7));
        assertTrue(error.getReason().contains("Không đủ tồn kho"));
        assertEquals(10, first.getStockQuantity());
        assertEquals(1, second.getStockQuantity());
        verifyNoInteractions(transactions);
    }

    @Test void repeatedVariantUsesTotalQuantity() {
        ProductVariant variant = variant(2, 5);
        when(items.findByOrderOrderIdOrderByOrderItemIdAsc(1)).thenReturn(List.of(line(variant, 3), line(variant, 3)));
        assertThrows(ResponseStatusException.class, () -> service.deduct(order, 7));
        assertEquals(5, variant.getStockQuantity());
        verifyNoInteractions(transactions);
    }
}
