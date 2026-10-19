package com.fu.SWP391_BetaFruit;

import com.fu.SWP391_BetaFruit.entity.*;
import com.fu.SWP391_BetaFruit.entity.Order;
import com.fu.SWP391_BetaFruit.enums.*;
import com.fu.SWP391_BetaFruit.service.PaymentService;
import com.fu.SWP391_BetaFruit.service.ShopOrderService;
import com.fu.SWP391_BetaFruit.utils.VnPayUtils;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

/** Callbacks run outside a test transaction, so commit/rollback and competing requests are real. */
@SpringBootTest(properties = "spring.config.location=classpath:/application-test.properties")
@AutoConfigureMockMvc
class VnpayPaymentIntegrationTests {
    @Autowired PaymentService payments;
    @Autowired ShopOrderService shop;
    @Autowired TransactionTemplate transactions;
    @Autowired EntityManager em;
    private Fixture fixture;
    private record Fixture(Integer orderId, Integer variantId, Integer ownerId, String reference) { }
    private record State(int stock, OrderStatus status, PaymentStatus payment, long deductions, long history) { }

    @BeforeEach void seedCommittedFixture() {
        fixture = transactions.execute(tx -> {
            String suffix = UUID.randomUUID().toString();
            User owner = user("owner-" + suffix), customer = user("customer-" + suffix);
            Shop store = new Shop(); store.setOwner(owner); store.setShopName("Test Fruit");
            store.setCommissionRate(new BigDecimal("5")); em.persist(store);
            MasterCategory category = new MasterCategory(); category.setCategoryName("Fruit-" + suffix); em.persist(category);
            Product product = new Product(); product.setShop(store); product.setCategory(category);
            product.setProductName("Apple"); em.persist(product);
            ProductVariant variant = new ProductVariant(); variant.setProduct(product); variant.setVariantName("1kg");
            variant.setOriginalPrice(new BigDecimal("100")); variant.setCostPrice(new BigDecimal("60"));
            variant.setStockQuantity(10); em.persist(variant);
            Order order = new Order(); order.setShop(store); order.setCustomer(customer);
            order.setPaymentMethod(PaymentMethod.VNPAY); order.setOrderStatus(OrderStatus.PENDING);
            order.setSnapshotShippingAddress("Test address"); order.setTotalAmount(new BigDecimal("100"));
            order.setFinalAmount(new BigDecimal("115")); order.setShippingFee(new BigDecimal("15"));
            order.setPlatformFee(new BigDecimal("5")); order.setShopNetReceived(new BigDecimal("95"));
            em.persist(order);
            em.persist(new OrderItem(null, order, variant, 1, new BigDecimal("100"), new BigDecimal("60"), "Apple", "1kg"));
            String reference = "TEST-" + suffix;
            persistPayment(order, reference);
            return new Fixture(order.getOrderId(), variant.getVariantId(), owner.getUserId(), reference);
        });
    }

    @Test void ipnThenReturnAndRepeatedConfirmationDeductOnlyOnce() throws Exception {
        var params = signed("11500", "00");
        assertEquals("00", payments.processVnpayIpn(params).get("RspCode"));
        assertPaidOnce();
        assertTrue(payments.processVnpayReturn(params).isSuccess());
        assertEquals("02", payments.processVnpayIpn(params).get("RspCode"));
        shop.confirmOrder(fixture.orderId(), fixture.ownerId());
        assertPaidOnce();
    }

    @Test void returnBeforeIpnCommitsStockPaymentAndHistoryTogether() throws Exception {
        var params = signed("11500", "00");
        var result = payments.processVnpayReturn(params);
        assertTrue(result.isSuccess()); assertEquals(fixture.orderId(), result.getOrderId());
        assertPaidOnce();
        assertEquals("02", payments.processVnpayIpn(params).get("RspCode"));
        assertTrue(payments.processVnpayReturn(params).isSuccess());
        assertPaidOnce();
    }

    @Test void invalidSignatureAndWrongAmountsNeverChangeStockOrPayment() throws Exception {
        var badSignature = signed("11500", "00"); badSignature.put("vnp_SecureHash", "invalid");
        assertEquals("97", payments.processVnpayIpn(badSignature).get("RspCode"));
        assertFalse(payments.processVnpayReturn(badSignature).isSuccess());
        for (String amount : List.of("11501", "11499", "invalid")) {
            var params = signed(amount, "00");
            assertEquals("04", payments.processVnpayIpn(params).get("RspCode"));
            assertFalse(payments.processVnpayReturn(params).isSuccess());
        }
        assertEquals(new State(10, OrderStatus.PENDING, PaymentStatus.PENDING, 0, 0), state());
    }

    @Test void failedGatewayResultDoesNotConfirmOrDeduct() throws Exception {
        var params = signed("11500", "24");
        assertEquals("00", payments.processVnpayIpn(params).get("RspCode"));
        assertFalse(payments.processVnpayReturn(params).isSuccess());
        assertEquals(new State(10, OrderStatus.PENDING, PaymentStatus.FAILED, 0, 0), state());
    }

    @Test void shortageRollsBackAndTheSameSuccessfulPaymentCanBeRetried() throws Exception {
        setStock(0);
        var params = signed("11500", "00");
        assertEquals("99", payments.processVnpayIpn(params).get("RspCode"));
        assertFalse(payments.processVnpayReturn(params).isSuccess());
        assertEquals(new State(0, OrderStatus.PENDING, PaymentStatus.PENDING, 0, 0), state());
        setStock(10);
        assertEquals("00", payments.processVnpayIpn(params).get("RspCode"));
        assertPaidOnce();
    }

    @Test void cancellingPaidOrderRestoresExactlyTheDeductedStockOnce() throws Exception {
        payments.processVnpayIpn(signed("11500", "00"));
        assertPaidOnce();
        shop.cancelOrder(fixture.orderId(), fixture.ownerId());
        assertEquals(10, state().stock());
        assertEquals(OrderStatus.CANCELLED, state().status());
        assertThrows(ResponseStatusException.class, () -> shop.cancelOrder(fixture.orderId(), fixture.ownerId()));
        assertEquals(10, state().stock());
    }

    @Test void shopConfirmedFirstIsNotDeductedAgainOrMovedBackwards() throws Exception {
        shop.confirmOrder(fixture.orderId(), fixture.ownerId());
        shop.startPreparingOrder(fixture.orderId(), fixture.ownerId());
        assertEquals("00", payments.processVnpayIpn(signed("11500", "00")).get("RspCode"));
        State state = state(); assertEquals(9, state.stock()); assertEquals(1, state.deductions());
        assertEquals(OrderStatus.PREPARING, state.status()); assertEquals(PaymentStatus.PAID, state.payment());
    }

    @Test void cancelledPendingOrderIsNotResurrectedByLatePayment() throws Exception {
        shop.cancelOrder(fixture.orderId(), fixture.ownerId());
        assertEquals("99", payments.processVnpayIpn(signed("11500", "00")).get("RspCode"));
        assertEquals(OrderStatus.CANCELLED, state().status());
        assertEquals(PaymentStatus.PENDING, state().payment()); assertEquals(10, state().stock());
    }

    @Test void concurrentIpnAndReturnStillDeductOnce() throws Exception {
        var params = signed("11500", "00");
        try (var executor = Executors.newFixedThreadPool(2)) {
            var start = new CountDownLatch(1);
            var ipn = executor.submit(() -> { start.await(); return payments.processVnpayIpn(params); });
            var browserReturn = executor.submit(() -> { start.await(); return payments.processVnpayReturn(params); });
            start.countDown();
            assertTrue(List.of("00", "02").contains(ipn.get(30, TimeUnit.SECONDS).get("RspCode")));
            assertTrue(browserReturn.get(30, TimeUnit.SECONDS).isSuccess());
        }
        assertPaidOnce();
    }

    @Test void differentPaymentReferencesForSameOrderDoNotDeductTwice() throws Exception {
        payments.processVnpayIpn(signed("11500", "00"));
        String anotherRef = fixture.reference() + "-second";
        transactions.executeWithoutResult(tx -> persistPayment(em.find(Order.class, fixture.orderId()), anotherRef));
        var params = signed(anotherRef, "11500", "00");
        assertEquals("00", payments.processVnpayIpn(params).get("RspCode"));
        assertPaidOnce();
    }

    private void assertPaidOnce() {
        assertEquals(new State(9, OrderStatus.CONFIRMED, PaymentStatus.PAID, 1, 1), state());
    }
    private State state() {
        return transactions.execute(tx -> {
            Order order = em.find(Order.class, fixture.orderId());
            PaymentTransaction payment = em.createQuery("select p from PaymentTransaction p where p.transactionRef = :ref", PaymentTransaction.class)
                    .setParameter("ref", fixture.reference()).getSingleResult();
            long deductions = em.createQuery("select count(i) from InventoryTransaction i where i.variant.variantId = :id and i.transactionType = :type", Long.class)
                    .setParameter("id", fixture.variantId()).setParameter("type", InventoryTransactionType.ORDER_DEDUCT).getSingleResult();
            long history = em.createQuery("select count(h) from OrderStatusHistory h where h.order.orderId = :id", Long.class)
                    .setParameter("id", fixture.orderId()).getSingleResult();
            return new State(em.find(ProductVariant.class, fixture.variantId()).getStockQuantity(), order.getOrderStatus(), payment.getStatus(), deductions, history);
        });
    }
    private void setStock(int stock) {
        transactions.executeWithoutResult(tx -> em.find(ProductVariant.class, fixture.variantId()).setStockQuantity(stock));
    }
    private Map<String, String> signed(String amount, String code) throws Exception { return signed(fixture.reference(), amount, code); }
    private Map<String, String> signed(String reference, String amount, String code) throws Exception {
        var params = new HashMap<String, String>(); params.put("vnp_TxnRef", reference);
        params.put("vnp_Amount", amount); params.put("vnp_ResponseCode", code);
        params.put("vnp_SecureHash", VnPayUtils.hmacSHA512("test-only-signature-key", VnPayUtils.buildQueryUrl(params)));
        return params;
    }
    private User user(String name) {
        var user = new User(); user.setUsername(name.substring(0, Math.min(50, name.length()))); user.setFullName(name);
        user.setEmail(name + "@example.test"); user.setPasswordHash("test-only"); em.persist(user); return user;
    }
    private void persistPayment(Order order, String reference) {
        var payment = new PaymentTransaction(); payment.setOrder(order); payment.setGateway("VNPAY");
        payment.setTransactionRef(reference); payment.setAmount(new BigDecimal("115")); payment.setStatus(PaymentStatus.PENDING); em.persist(payment);
    }
}
