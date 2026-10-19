package com.fu.SWP391_BetaFruit;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.mock.web.MockHttpSession;
import com.fu.SWP391_BetaFruit.entity.*;
import com.fu.SWP391_BetaFruit.entity.Order;
import com.fu.SWP391_BetaFruit.enums.*;
import com.fu.SWP391_BetaFruit.service.impl.DeliveryAssignmentService;
import com.fu.SWP391_BetaFruit.service.PaymentService;
import com.fu.SWP391_BetaFruit.utils.VnPayUtils;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.config.location=classpath:/application-test.properties")
@AutoConfigureMockMvc
@Transactional
class Swp391BetaFruitApplicationTests {

    @Autowired MockMvc mvc;
    @Autowired EntityManager em;
    @Autowired DeliveryAssignmentService assignments;
    @Autowired PaymentService payments;

	@Test
	void contextLoads() {
	}

    @Test void codAndVnpayOrdersPassThroughRealShopAndShipperLayers() throws Exception {
        User owner = user("owner"), customer = user("customer"), driver = user("driver");
        Shop shop = new Shop(); shop.setOwner(owner); shop.setShopName("Fruit shop");
        shop.setCommissionRate(new BigDecimal("5")); shop.setAvailableBalance(BigDecimal.ZERO); em.persist(shop);
        Shipper shipper = new Shipper(); shipper.setUser(driver); em.persist(shipper);
        MasterCategory category = new MasterCategory(); category.setCategoryName("Fruit"); em.persist(category);
        Product product = new Product(); product.setShop(shop); product.setCategory(category);
        product.setProductName("Apple"); em.persist(product);
        ProductVariant variant = new ProductVariant(); variant.setProduct(product); variant.setVariantName("1kg");
        variant.setOriginalPrice(new BigDecimal("100")); variant.setCostPrice(new BigDecimal("60"));
        variant.setStockQuantity(10); em.persist(variant);
        var ownerSession = session(owner, RoleName.SHOP_OWNER);
        var driverSession = session(driver, RoleName.DELIVERY_STAFF);
        for (PaymentMethod method : PaymentMethod.values()) {
            Order order = new Order(); order.setShop(shop); order.setCustomer(customer);
            order.setPaymentMethod(method); order.setOrderStatus(OrderStatus.PENDING);
            order.setSnapshotShippingAddress("Test address"); order.setTotalAmount(new BigDecimal("100"));
            order.setFinalAmount(new BigDecimal("115")); order.setShippingFee(new BigDecimal("15"));
            order.setPlatformFee(new BigDecimal("5")); order.setShopNetReceived(new BigDecimal("95"));
            em.persist(order);
            em.persist(new OrderItem(null, order, variant, 1, new BigDecimal("100"), new BigDecimal("60"), "Apple", "1kg"));
            em.flush();
            String shopUrl = "/shop/orders/" + order.getOrderId();
            String shipperUrl = "/shipper/orders/" + order.getOrderId();
            mvc.perform(get(shopUrl).session(ownerSession)).andExpect(status().isOk()).andExpect(view().name("shop/order-detail"));
            if (method == PaymentMethod.VNPAY) {
                var transaction = new PaymentTransaction(); transaction.setOrder(order);
                transaction.setGateway("VNPAY"); transaction.setTransactionRef("TEST_VNPAY");
                transaction.setAmount(new BigDecimal("115")); transaction.setStatus(PaymentStatus.PENDING);
                em.persist(transaction); em.flush();
                var params = new java.util.HashMap<String, String>();
                params.put("vnp_TxnRef", "TEST_VNPAY"); params.put("vnp_Amount", "11500");
                params.put("vnp_ResponseCode", "00");
                params.put("vnp_SecureHash", VnPayUtils.hmacSHA512("test-only-signature-key", VnPayUtils.buildQueryUrl(params)));
                int stockBeforeCallback = variant.getStockQuantity();
                assertEquals("00", payments.processVnpayIpn(params).get("RspCode"));
                assertEquals(PaymentStatus.PAID, transaction.getStatus());
                assertEquals(OrderStatus.CONFIRMED, order.getOrderStatus());
                assertEquals(stockBeforeCallback - 1, variant.getStockQuantity());
                assertEquals("02", payments.processVnpayIpn(params).get("RspCode"));
                assertTrue(payments.processVnpayReturn(params).isSuccess());
                assertEquals(stockBeforeCallback - 1, variant.getStockQuantity());
            } else {
                mvc.perform(post(shopUrl + "/confirm").session(ownerSession).param("ownerId", "-1"))
                        .andExpect(redirectedUrl("/shop/orders"));
            }
            for (String action : List.of("preparing", "ready")) {
                mvc.perform(post(shopUrl + "/" + action).session(ownerSession).param("ownerId", "-1"))
                        .andExpect(redirectedUrl("/shop/orders"));
            }
            assignments.assign(order.getOrderId(), driver.getUserId(), owner.getUserId());
            mvc.perform(get(shipperUrl).session(driverSession)).andExpect(status().isOk()).andExpect(view().name("shipper/order-detail"));
            mvc.perform(post(shipperUrl + "/picked-up").session(driverSession)).andExpect(redirectedUrl(shipperUrl));
            mvc.perform(post(shipperUrl + "/delivering").session(driverSession)).andExpect(redirectedUrl(shipperUrl));
            mvc.perform(post(shipperUrl + "/delivered").session(driverSession).param("codCollected", "true"))
                    .andExpect(redirectedUrl(shipperUrl));
            assertEquals(OrderStatus.DELIVERED, order.getOrderStatus());
            assertEquals(0, new BigDecimal("95").compareTo(order.getShopNetReceived()));
            assertFalse(order.getIsSettled());
            assertNull(order.getSettledAt());
            if (method == PaymentMethod.COD) assertFalse(order.getIsCodRemitted());
            else assertNull(order.getIsCodRemitted());
        }
        assertEquals(8, variant.getStockQuantity());
        assertEquals(0, BigDecimal.ZERO.compareTo(shop.getAvailableBalance()));
        mvc.perform(get("/shipper/orders/api/cod-summary").session(driverSession))
                .andExpect(status().isOk()).andExpect(jsonPath("$.pendingOrderCount").value(1))
                .andExpect(jsonPath("$.amountToRemit").value(115));
    }

    private User user(String name) {
        User user = new User(); user.setUsername(name); user.setFullName(name);
        user.setEmail(name + "@example.test"); user.setPasswordHash("test-only"); em.persist(user); return user;
    }

    private MockHttpSession session(User user, RoleName role) {
        var session = new MockHttpSession(); session.setAttribute("LOGGED_IN_USER_ID", user.getUserId());
        session.setAttribute("LOGGED_IN_FULLNAME", user.getFullName());
        session.setAttribute("LOGGED_IN_ROLES", List.of(role)); return session;
    }

}
