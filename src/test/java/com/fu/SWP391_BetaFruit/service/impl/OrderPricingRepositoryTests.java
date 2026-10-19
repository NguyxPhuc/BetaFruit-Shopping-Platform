package com.fu.SWP391_BetaFruit.service.impl;

import com.fu.SWP391_BetaFruit.entity.*;
import com.fu.SWP391_BetaFruit.enums.*;
import com.fu.SWP391_BetaFruit.repository.OrderRepository;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.JpaSort;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;

import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class OrderPricingRepositoryTests {
    @Test void sortsCalculatedReceiptsBeforePaginationAndReadsPlatformCouponWithShopIdZero() {
        var config = new Configuration()
                .addAnnotatedClass(Order.class).addAnnotatedClass(Shop.class)
                .addAnnotatedClass(Coupon.class).addAnnotatedClass(User.class).addAnnotatedClass(Role.class)
                .setProperty("hibernate.connection.driver_class", "org.h2.Driver")
                .setProperty("hibernate.connection.url", "jdbc:h2:mem:pricing;MODE=MSSQLServer;DB_CLOSE_DELAY=-1")
                .setProperty("hibernate.hbm2ddl.auto", "create-drop")
                .setProperty("hibernate.show_sql", "false");
        try (var factory = config.buildSessionFactory(); var em = factory.createEntityManager()) {
            em.getTransaction().begin();
            User owner = new User();
            owner.setUsername("owner");
            owner.setEmail("owner@example.test");
            owner.setPasswordHash("test");
            owner.setFullName("Owner");
            em.persist(owner);
            Shop shop = new Shop();
            shop.setOwner(owner);
            shop.setShopName("Fruit");
            shop.setCommissionRate(null);
            em.persist(shop);
            Coupon platform = new Coupon();
            platform.setCouponCode("ADMIN");
            platform.setDiscountType(DiscountType.values()[0]);
            platform.setDiscountValue(BigDecimal.TEN);
            em.persist(platform);
            Coupon own = new Coupon();
            own.setShop(shop);
            own.setCouponCode("SHOP");
            own.setDiscountType(DiscountType.values()[0]);
            own.setDiscountValue(BigDecimal.TEN);
            em.persist(own);
            Order adminOrder = order(shop, owner, platform, "999999");
            Order shopOrder = order(shop, owner, own, "888888");
            Order noCoupon = order(shop, owner, null, "777777");
            noCoupon.setTotalAmount(new BigDecimal("90.10"));
            noCoupon.setPlatformFee(new BigDecimal("4.51"));
            em.persist(adminOrder);
            em.persist(shopOrder);
            em.persist(noCoupon);
            em.flush();
            // H2-only legacy fixture: an admin coupon has FK value 0 but no Shop 0.
            // Keep the production entity/FK mapping unchanged. No live DB is touched.
            em.createNativeQuery("SET REFERENTIAL_INTEGRITY FALSE").executeUpdate();
            em.createNativeQuery("UPDATE Coupon SET ShopId = 0 WHERE CouponId = :id")
                    .setParameter("id", platform.getCouponId()).executeUpdate();
            em.createNativeQuery("SET REFERENTIAL_INTEGRITY TRUE").executeUpdate();
            em.clear();

            var repository = new JpaRepositoryFactory(em).getRepository(OrderRepository.class);
            var sort = JpaSort.unsafe(Sort.Direction.ASC, "(" + OrderPricing.NET_SORT_EXPRESSION + ")")
                    .and(Sort.by("orderId"));
            var first = repository.searchOrdersForShopOwner(owner.getUserId(), null, null, PageRequest.of(0, 1, sort));
            assertEquals(3, first.getTotalElements());
            assertEquals(shopOrder.getOrderId(), first.getContent().getFirst().getOrderId());
            var second = repository.searchOrdersForShopOwner(owner.getUserId(), null, null, PageRequest.of(1, 1, sort));
            assertEquals(noCoupon.getOrderId(), second.getContent().getFirst().getOrderId());
            var third = repository.searchOrdersForShopOwner(owner.getUserId(), null, null, PageRequest.of(2, 1, sort));
            var loadedAdmin = third.getContent().getFirst();
            assertEquals(adminOrder.getOrderId(), loadedAdmin.getOrderId());
            assertEquals(0, loadedAdmin.getCoupon().getShop().getShopId());
            assertFalse(org.hibernate.Hibernate.isInitialized(loadedAdmin.getCoupon().getShop()));
            assertEquals(new BigDecimal("95.09"), OrderPricing.calculate(loadedAdmin).shopNetReceived());
            assertFalse(org.hibernate.Hibernate.isInitialized(loadedAdmin.getCoupon().getShop()));
            assertEquals(new BigDecimal("85.59"), OrderPricing.calculate(second.getContent().getFirst()).shopNetReceived());
            assertTrue(repository.searchOrdersForShopOwner(-1, null, null, PageRequest.of(0, 10, sort)).isEmpty());
            em.getTransaction().rollback();
        }
    }

    private Order order(Shop shop, User owner, Coupon coupon, String storedNet) {
        Order order = new Order();
        order.setShop(shop);
        order.setCustomer(owner);
        order.setCoupon(coupon);
        order.setSnapshotShippingAddress("Address");
        order.setTotalAmount(new BigDecimal("100.10"));
        order.setDiscountAmount(BigDecimal.TEN);
        order.setPlatformFee(new BigDecimal("5.01"));
        order.setFinalAmount(new BigDecimal("110.10"));
        order.setShopNetReceived(new BigDecimal(storedNet));
        order.setPaymentMethod(PaymentMethod.COD);
        return order;
    }
}
