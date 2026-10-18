package com.fu.SWP391_BetaFruit.service.impl;

import com.fu.SWP391_BetaFruit.entity.*;
import com.fu.SWP391_BetaFruit.enums.*;
import com.fu.SWP391_BetaFruit.repository.DeliveryAssignmentRepository;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class ShipperOrderRepositoryTests {
    @Test void assignedOrdersAndOutstandingCashAreScopedAndIndependentOfSettlement() {
        var config = new Configuration()
                .addAnnotatedClass(Order.class).addAnnotatedClass(Shop.class).addAnnotatedClass(Coupon.class)
                .addAnnotatedClass(User.class).addAnnotatedClass(Role.class).addAnnotatedClass(Shipper.class)
                .addAnnotatedClass(DeliveryAssignment.class)
                .setProperty("hibernate.connection.driver_class", "org.h2.Driver")
                .setProperty("hibernate.connection.url", "jdbc:h2:mem:shipper;MODE=MSSQLServer;DB_CLOSE_DELAY=-1")
                .setProperty("hibernate.hbm2ddl.auto", "create-drop")
                .setProperty("hibernate.show_sql", "false");
        try (var factory = config.buildSessionFactory(); var em = factory.createEntityManager()) {
            em.getTransaction().begin();
            User owner = user("owner"); em.persist(owner);
            User user1 = user("shipper1"); em.persist(user1);
            User user2 = user("shipper2"); em.persist(user2);
            Shipper shipper1 = new Shipper(); shipper1.setUser(user1); em.persist(shipper1);
            Shipper shipper2 = new Shipper(); shipper2.setUser(user2); em.persist(shipper2);
            Shop shop = new Shop(); shop.setOwner(owner); shop.setShopName("Fruit"); em.persist(shop);
            Order delivering = add(em, shop, owner, shipper1, PaymentMethod.COD, OrderStatus.DELIVERING, false);
            add(em, shop, owner, shipper1, PaymentMethod.COD, OrderStatus.DELIVERED, false);
            Order settled = add(em, shop, owner, shipper1, PaymentMethod.COD, OrderStatus.SUCCESS, null);
            settled.setIsSettled(true);
            settled.setSettledAt(java.time.LocalDateTime.of(2026, 10, 14, 9, 0));
            add(em, shop, owner, shipper1, PaymentMethod.COD, OrderStatus.DELIVERED, true);
            add(em, shop, owner, shipper1, PaymentMethod.COD, OrderStatus.FAILED, false);
            add(em, shop, owner, shipper1, PaymentMethod.ONLINE, OrderStatus.DELIVERED, false);
            Order other = add(em, shop, owner, shipper2, PaymentMethod.COD, OrderStatus.DELIVERED, false);
            em.flush(); em.clear();

            var repository = new JpaRepositoryFactory(em).getRepository(DeliveryAssignmentRepository.class);
            var service = new ShipperOrderQueryService(repository);
            var summary = service.getCODSummary(user1.getUserId());
            assertEquals(2, summary.pendingOrderCount());
            assertEquals(0, new BigDecimal("230000").compareTo(summary.amountToRemit()));
            var page = service.getOrders(user1.getUserId(), 0, 2);
            assertEquals(6, page.getTotalElements());
            assertEquals(2, page.getContent().size());
            var filtered = service.searchOrders(user1.getUserId(), " owner ", OrderStatus.DELIVERED,
                    PaymentMethod.COD, "totalAmount", "asc", 0, 1);
            assertEquals(2, filtered.getTotalElements());
            assertEquals(1, filtered.getContent().size());
            assertEquals("COD", filtered.getContent().getFirst().paymentMethod());
            assertEquals("DELIVERED", filtered.getContent().getFirst().orderStatus());
            assertTrue(service.searchOrders(user1.getUserId(), other.getOrderId().toString(), null,
                    null, "orderCode", "desc", 0, 10).isEmpty());
            assertTrue(service.searchOrders(user1.getUserId(), "missing customer", null,
                    null, "assignedAt", "desc", 0, 10).isEmpty());
            var dto = service.getOrder(delivering.getOrderId(), user1.getUserId());
            assertEquals(0, new BigDecimal("115000").compareTo(dto.amountToCollect()));
            assertNull(dto.isSettled()); // Original entity leaves unrecorded settlement as null.
            assertThrows(ResponseStatusException.class, () -> service.getOrder(other.getOrderId(), user1.getUserId()));
            var online = page.getContent().stream().filter(o -> o.paymentMethod().equals("ONLINE")).findFirst().orElseThrow();
            assertEquals("NOT_APPLICABLE", online.codRemittanceStatus());
            assertNull(online.isCODRemitted());
            assertEquals(BigDecimal.ZERO, online.amountToCollect());
            assertEquals(0, service.getCODSummary(-1).pendingOrderCount());
            assertEquals(0, BigDecimal.ZERO.compareTo(service.getCODSummary(-1).amountToRemit()));
            em.getTransaction().rollback();
        }
    }

    private User user(String name) {
        User user = new User(); user.setUsername(name); user.setFullName(name);
        user.setEmail(name + "@example.test"); user.setPasswordHash("test"); return user;
    }

    private Order add(jakarta.persistence.EntityManager em, Shop shop, User customer, Shipper shipper,
                      PaymentMethod method, OrderStatus status, Boolean remitted) {
        Order order = new Order(); order.setShop(shop); order.setCustomer(customer);
        order.setSnapshotShippingAddress("Address"); order.setTotalAmount(new BigDecimal("100000"));
        order.setFinalAmount(new BigDecimal("115000")); order.setPaymentMethod(method);
        order.setOrderStatus(status); order.setIsCODRemitted(remitted); em.persist(order);
        DeliveryAssignment assignment = new DeliveryAssignment(); assignment.setOrder(order);
        assignment.setShipper(shipper); assignment.setAssignedBy(customer); em.persist(assignment);
        return order;
    }
}
