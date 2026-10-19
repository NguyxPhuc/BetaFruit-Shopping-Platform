package com.fu.SWP391_BetaFruit.repository;

import com.fu.SWP391_BetaFruit.entity.*;
import com.fu.SWP391_BetaFruit.entity.Order;
import com.fu.SWP391_BetaFruit.enums.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.hibernate.Hibernate;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.JpaSort;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Real repository queries against an isolated database; no application/production datasource is loaded. */
class OrderRepositoryIntegrationTests {
    private static SessionFactory factory;
    private EntityManager em;

    @BeforeAll static void createDatabase() {
        var config = new Configuration()
                .setProperty("hibernate.connection.driver_class", "org.h2.Driver")
                .setProperty("hibernate.connection.url", "jdbc:h2:mem:merged_repositories;MODE=MSSQLServer")
                .setProperty("hibernate.hbm2ddl.auto", "create-drop")
                .setProperty("hibernate.hbm2ddl.halt_on_error", "true");
        for (Class<?> entity : List.of(Order.class, User.class, Role.class, Shop.class, Coupon.class,
                Shipper.class, DeliveryAssignment.class, CODCollection.class, CODDetail.class,
                OrderStatusHistory.class, PaymentTransaction.class, OrderItem.class,
                Product.class, ProductVariant.class, ProductImage.class, MasterCategory.class)) {
            config.addAnnotatedClass(entity);
        }
        factory = config.buildSessionFactory();
    }

    @AfterAll static void closeDatabase() { if (factory != null) factory.close(); }
    @BeforeEach void begin() { em = factory.createEntityManager(); em.getTransaction().begin(); }
    @AfterEach void rollback() {
        if (em != null) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            em.close();
        }
    }

    private <T> T repo(Class<T> type) { return new JpaRepositoryFactory(em).getRepository(type); }

    @Test void shopQueriesScopeOwnershipFilterPaginateFetchAndLock() {
        User owner = user("owner"), other = user("other"), customer = user("Alice");
        Shop shop = shop(owner), otherShop = shop(other);
        Order first = order(shop, customer, PaymentMethod.COD, OrderStatus.PENDING, false);
        Order second = order(shop, customer, PaymentMethod.VNPAY, OrderStatus.DELIVERED, null);
        order(otherShop, customer, PaymentMethod.COD, OrderStatus.PENDING, false);
        Coupon coupon = new Coupon(); coupon.setCouponCode("SHOP"); coupon.setShop(shop);
        coupon.setDiscountType(DiscountType.values()[0]); coupon.setDiscountValue(BigDecimal.TEN);
        em.persist(coupon); first.setCoupon(coupon); first.setDiscountAmount(BigDecimal.TEN);
        em.flush(); em.clear();
        var orders = repo(OrderRepository.class);
        var page = orders.searchOrdersForShopOwner(owner.getUserId(), "alice", null,
                PageRequest.of(0, 1, Sort.by("orderId")));
        assertEquals(2, page.getTotalElements());
        assertEquals(first.getOrderId(), page.getContent().getFirst().getOrderId());
        assertTrue(Hibernate.isInitialized(page.getContent().getFirst().getCustomer()));
        assertTrue(Hibernate.isInitialized(page.getContent().getFirst().getShop()));
        assertTrue(Hibernate.isInitialized(page.getContent().getFirst().getCoupon()));
        assertEquals(second.getOrderId(), orders.searchOrdersForShopOwner(owner.getUserId(),
                second.getOrderId().toString(), OrderStatus.DELIVERED, PageRequest.of(0, 10))
                .getContent().getFirst().getOrderId());
        assertTrue(orders.searchOrdersForShopOwner(owner.getUserId(), "missing", null, PageRequest.of(0, 10)).isEmpty());
        assertTrue(orders.findDetailForShopOwner(first.getOrderId(), other.getUserId()).isEmpty());
        assertTrue(orders.findDetailForShopOwner(first.getOrderId(), owner.getUserId()).isPresent());
        assertTrue(orders.findByOrderIdAndShopOwnerUserId(first.getOrderId(), other.getUserId()).isEmpty());
        Order locked = orders.findByOrderIdAndShopOwnerUserId(first.getOrderId(), owner.getUserId()).orElseThrow();
        assertEquals(LockModeType.PESSIMISTIC_WRITE, em.getLockMode(locked));
        Order byId = orders.findLockedById(second.getOrderId()).orElseThrow();
        assertEquals(LockModeType.PESSIMISTIC_WRITE, em.getLockMode(byId));
        // Preserve the aliases used by the incoming pricing service for computed sorting.
        var netSort = JpaSort.unsafe(Sort.Direction.ASC,
                "(o.totalAmount - case when fk(c.shop) = s.shopId then coalesce(o.discountAmount, 0) else 0 end - o.platformFee)");
        assertEquals(2, orders.searchOrdersForShopOwner(owner.getUserId(), null, null,
                PageRequest.of(0, 1, netSort)).getTotalElements());
        assertTrue(orders.findByOrderId(first.getOrderId()).isPresent());
        assertEquals(1, orders.countByCoupon_CouponId(coupon.getCouponId()));
        assertEquals(1, orders.countByCoupon_CouponIdAndShop_ShopId(coupon.getCouponId(), shop.getShopId()));
        assertEquals(0, BigDecimal.TEN.compareTo(orders.sumTotalDiscountByCouponId(coupon.getCouponId())));
        assertEquals(0, BigDecimal.TEN.compareTo(orders.sumTotalDiscountByCouponIdAndShopId(coupon.getCouponId(), shop.getShopId())));
        assertEquals(1, orders.findByCoupon_CouponId(coupon.getCouponId(), PageRequest.of(0, 10)).getTotalElements());
        assertTrue(orders.findByCoupon_CouponIdAndShop_ShopId(coupon.getCouponId(), otherShop.getShopId(),
                PageRequest.of(0, 10)).isEmpty());
    }

    @Test void shipperQueriesExcludeOtherShippersRemittedOrdersAndVnpayFromOutstandingCod() {
        User owner = user("owner"), driver = user("driver"), other = user("other");
        Shop shop = shop(owner);
        Shipper shipper = shipper(driver), another = shipper(other);
        Order cash = assigned(shop, owner, shipper, PaymentMethod.COD, OrderStatus.DELIVERED, false);
        assigned(shop, owner, shipper, PaymentMethod.COD, OrderStatus.SUCCESS, null);
        assigned(shop, owner, shipper, PaymentMethod.COD, OrderStatus.DELIVERED, true);
        assigned(shop, owner, shipper, PaymentMethod.COD, OrderStatus.FAILED, false);
        assigned(shop, owner, shipper, PaymentMethod.VNPAY, OrderStatus.DELIVERED, false);
        assigned(shop, owner, another, PaymentMethod.COD, OrderStatus.DELIVERED, false);
        em.flush(); em.clear();
        var assignments = repo(DeliveryAssignmentRepository.class);
        var statuses = List.of(OrderStatus.DELIVERED, OrderStatus.SUCCESS);
        assertEquals(2, assignments.countUnremittedCOD(driver.getUserId(), PaymentMethod.COD, statuses));
        assertEquals(0, new BigDecimal("200").compareTo(
                assignments.sumUnremittedCOD(driver.getUserId(), PaymentMethod.COD, statuses)));
        assertEquals(0, assignments.countUnremittedCOD(-1, PaymentMethod.COD, statuses));
        assertEquals(0, BigDecimal.ZERO.compareTo(assignments.sumUnremittedCOD(-1, PaymentMethod.COD, statuses)));
        assertEquals(5, assignments.findByShipperUserId(driver.getUserId(), PageRequest.of(0, 2)).getTotalElements());
        var filtered = assignments.searchAssignedOrders(driver.getUserId(), "OWNER", OrderStatus.DELIVERED,
                PaymentMethod.COD, PageRequest.of(0, 1));
        assertEquals(2, filtered.getTotalElements());
        assertEquals(1, filtered.getContent().size());
        assertTrue(assignments.findByOrderOrderIdAndShipperUserId(cash.getOrderId(), other.getUserId()).isEmpty());
        assertTrue(assignments.findByOrderOrderIdAndShipperUserId(cash.getOrderId(), driver.getUserId()).isPresent());
        assertTrue(Hibernate.isInitialized(assignments.findByOrderOrderId(cash.getOrderId())
                .orElseThrow().getShipper().getUser()));
        assertTrue(repo(ShipperRepository.class).findById(driver.getUserId()).isPresent());
    }

    @Test void detailRepositoriesUseMainPropertiesAndStableOrdering() {
        User owner = user("owner"); Shop shop = shop(owner);
        Order order = order(shop, owner, PaymentMethod.COD, OrderStatus.DELIVERED, false);
        Shipper shipper = shipper(user("driver"));
        CODCollection collection = new CODCollection(null, shipper, BigDecimal.TEN, owner, null);
        em.persist(collection);
        CODDetail detail = new CODDetail(null, collection, order, BigDecimal.TEN); em.persist(detail);
        MasterCategory category = new MasterCategory(); category.setCategoryName("Fruit"); em.persist(category);
        Product product = new Product(); product.setShop(shop); product.setCategory(category);
        product.setProductName("Apple"); em.persist(product);
        ProductVariant variant = new ProductVariant(); variant.setProduct(product); variant.setVariantName("1kg");
        variant.setOriginalPrice(BigDecimal.TEN); variant.setCostPrice(BigDecimal.ONE); variant.setStockQuantity(5);
        em.persist(variant);
        OrderItem line = new OrderItem(null, order, variant, 1, BigDecimal.TEN, BigDecimal.ONE, "Apple", "1kg");
        em.persist(line);
        OrderItem line2 = new OrderItem(null, order, variant, 2, BigDecimal.TEN, BigDecimal.ONE, "Apple", "1kg");
        em.persist(line2);
        var h1 = new OrderStatusHistory(null, order, owner, OrderStatus.PENDING, OrderStatus.CONFIRMED, "Confirm", null);
        var h2 = new OrderStatusHistory(null, order, owner, OrderStatus.CONFIRMED, OrderStatus.READY, "Ready", null);
        em.persist(h1); em.persist(h2);
        PaymentTransaction p1 = payment(order, "one"), p2 = payment(order, "two");
        em.flush(); em.clear();
        assertEquals(List.of(detail.getDetailId()), repo(CODDetailRepository.class)
                .findByCodCollection_CollectionIdOrderByDetailIdAsc(collection.getCollectionId())
                .stream().map(CODDetail::getDetailId).toList());
        assertEquals(detail.getDetailId(), repo(CODDetailRepository.class).findByOrderOrderId(order.getOrderId())
                .orElseThrow().getDetailId());
        assertTrue(repo(CODDetailRepository.class).findByCodCollection_CollectionIdOrderByDetailIdAsc(-1).isEmpty());
        assertEquals(List.of(line.getOrderItemId(), line2.getOrderItemId()), repo(OrderItemRepository.class)
                .findByOrderOrderIdOrderByOrderItemIdAsc(order.getOrderId()).stream().map(OrderItem::getOrderItemId).toList());
        var history = repo(OrderStatusHistoryRepository.class).findByOrderOrderIdOrderByCreatedAtAscHistoryIdAsc(order.getOrderId());
        assertEquals(List.of(h1.getHistoryId(), h2.getHistoryId()), history.stream().map(OrderStatusHistory::getHistoryId).toList());
        assertTrue(Hibernate.isInitialized(history.getFirst().getUpdatedBy()));
        var payments = repo(PaymentTransactionRepository.class);
        assertEquals(List.of(p1.getTransactionId(), p2.getTransactionId()), payments
                .findByOrderOrderIdOrderByCreatedAtAscTransactionIdAsc(order.getOrderId()).stream()
                .map(PaymentTransaction::getTransactionId).toList());
        assertEquals(p1.getTransactionId(), payments.findByTransactionRef("one").orElseThrow().getTransactionId());
        var locked = repo(ProductVariantRepository.class).findForStockUpdate(variant.getVariantId()).orElseThrow();
        assertEquals(LockModeType.PESSIMISTIC_WRITE, em.getLockMode(locked));
    }

    private User user(String name) {
        var user = new User(); user.setUsername(name); user.setFullName(name);
        user.setEmail(name + "@example.test"); user.setPasswordHash("test"); em.persist(user); return user;
    }
    private Shop shop(User owner) {
        var shop = new Shop(); shop.setOwner(owner); shop.setShopName("Fruit"); em.persist(shop); return shop;
    }
    private Shipper shipper(User user) {
        var shipper = new Shipper(); shipper.setUser(user); em.persist(shipper); return shipper;
    }
    private Order order(Shop shop, User customer, PaymentMethod method, OrderStatus status, Boolean remitted) {
        var order = new Order(); order.setShop(shop); order.setCustomer(customer);
        order.setSnapshotShippingAddress("Address"); order.setTotalAmount(new BigDecimal("100"));
        order.setFinalAmount(new BigDecimal("100")); order.setPlatformFee(BigDecimal.ONE);
        order.setShopNetReceived(new BigDecimal("99")); order.setPaymentMethod(method);
        order.setOrderStatus(status); order.setIsCodRemitted(remitted); em.persist(order); return order;
    }
    private Order assigned(Shop shop, User customer, Shipper shipper, PaymentMethod method, OrderStatus status, Boolean remitted) {
        var order = order(shop, customer, method, status, remitted);
        em.persist(new DeliveryAssignment(null, order, shipper, customer, null)); return order;
    }
    private PaymentTransaction payment(Order order, String reference) {
        var payment = new PaymentTransaction(); payment.setOrder(order); payment.setGateway("VNPAY");
        payment.setTransactionRef(reference); payment.setAmount(BigDecimal.TEN); payment.setStatus(PaymentStatus.PENDING);
        em.persist(payment); return payment;
    }
}
