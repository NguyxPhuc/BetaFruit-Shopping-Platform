package com.fu.SWP391_BetaFruit.service;

import com.fu.SWP391_BetaFruit.config.VnPayConfig;
import com.fu.SWP391_BetaFruit.entity.*;
import com.fu.SWP391_BetaFruit.enums.*;
import com.fu.SWP391_BetaFruit.repository.*;
import com.fu.SWP391_BetaFruit.service.impl.NotificationServiceImpl;
import com.fu.SWP391_BetaFruit.service.impl.PaymentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NewOrderNotificationTest {
    @Mock NotificationRepository notifications;
    @Mock OrderRepository orders;
    @Mock PaymentTransactionRepository transactions;
    @Mock VnPayConfig config;
    @Mock NotificationService notificationService;
    @InjectMocks NotificationServiceImpl service;
    @InjectMocks PaymentServiceImpl paymentService;
    Order order;

    @BeforeEach
    void setUp() {
        User owner = new User();
        owner.setUserId(10);
        Shop shop = new Shop();
        shop.setOwner(owner);
        order = new Order();
        order.setOrderId(99);
        order.setShop(shop);
        order.setOrderStatus(OrderStatus.PENDING);
    }

    @Test
    void newOrderAlertBelongsToShopOwnerAndLinksToOrder() {
        service.notifyNewOrder(order);
        ArgumentCaptor<Notification> captured = ArgumentCaptor.forClass(Notification.class);
        verify(notifications).save(captured.capture());
        Notification saved = captured.getValue();
        assertEquals(10, saved.getUser().getUserId());
        assertEquals("/shop/orders/99", saved.getActionUrl());
        assertEquals(NotificationType.ORDER_STATUS, saved.getNotificationType());
        assertFalse(saved.getIsRead());
        assertFalse(saved.getIsHidden());
    }

    @Test
    void repeatedPaidCallbackDoesNotRecreateExistingAlert() {
        when(notifications.existsByUser_UserIdAndNotificationTypeAndActionUrlAndTitle(
                eq(10), eq(NotificationType.ORDER_STATUS), eq("/shop/orders/99"), anyString())).thenReturn(true);
        service.notifyNewOrder(order);
        verify(notifications, never()).save(any());
    }

    private PaymentTransaction callback(Map<String, String> params) throws Exception {
        PaymentTransaction tx = new PaymentTransaction();
        tx.setOrder(order);
        tx.setStatus(PaymentStatus.PENDING);
        tx.setAmount(new BigDecimal("100000"));
        when(config.verifySignature(params)).thenReturn(true);
        when(transactions.findByTransactionRef("REF")).thenReturn(Optional.of(tx));
        when(orders.findLockedById(99)).thenReturn(Optional.of(order));
        return tx;
    }

    @Test
    void successfulVnpayCallbackNotifiesOnlyOnce() throws Exception {
        var params = Map.of("vnp_TxnRef", "REF", "vnp_Amount", "10000000", "vnp_ResponseCode", "00");
        PaymentTransaction tx = callback(params);
        assertEquals("00", paymentService.processVnpayIpn(params).get("RspCode"));
        assertEquals(PaymentStatus.PAID, tx.getStatus());
        assertEquals("02", paymentService.processVnpayIpn(params).get("RspCode"));
        verify(notificationService, times(1)).notifyNewOrder(order);
    }

    @Test
    void failedPaymentDoesNotTellOwnerToPrepareGoods() throws Exception {
        var params = Map.of("vnp_TxnRef", "REF", "vnp_Amount", "10000000", "vnp_ResponseCode", "24");
        callback(params);
        assertEquals("00", paymentService.processVnpayIpn(params).get("RspCode"));
        verifyNoInteractions(notificationService);
    }

    @Test
    void paymentForCancelledOrderDoesNotRaiseNewOrderAlert() throws Exception {
        var params = Map.of("vnp_TxnRef", "REF", "vnp_Amount", "10000000", "vnp_ResponseCode", "00");
        callback(params);
        order.setOrderStatus(OrderStatus.CANCELLED);
        assertEquals("00", paymentService.processVnpayIpn(params).get("RspCode"));
        verifyNoInteractions(notificationService);
    }
}
