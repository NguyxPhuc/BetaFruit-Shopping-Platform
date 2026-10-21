package com.fu.SWP391_BetaFruit.controller;

import com.fu.SWP391_BetaFruit.dto.response.customer.*;
import com.fu.SWP391_BetaFruit.entity.Address;
import org.junit.jupiter.api.Test;
import org.thymeleaf.context.Context;
import org.thymeleaf.context.IExpressionContext;
import org.thymeleaf.linkbuilder.ILinkBuilder;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.FileTemplateResolver;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class CheckoutTemplateTest {
    @Test void rendersCheckoutEmptyAddressStateReceiptAndCartUsingRealTemplates() throws Exception {
        FileTemplateResolver resolver = new FileTemplateResolver();
        resolver.setPrefix("src/main/resources/templates/"); resolver.setSuffix(".html");
        resolver.setCharacterEncoding("UTF-8"); resolver.setTemplateMode("HTML");
        SpringTemplateEngine engine = new SpringTemplateEngine(); engine.setTemplateResolver(resolver);
        // Standalone render without a servlet container; links still retain path/query parameters.
        engine.setLinkBuilder(new ILinkBuilder() {
            public String getName() { return "preview"; }
            public Integer getOrder() { return 1; }
            public String buildLink(IExpressionContext context, String base, Map<String, Object> params) {
                String result = base;
                List<String> query = new ArrayList<>();
                for (var entry : (params == null ? Map.<String, Object>of() : params).entrySet()) {
                    String token = "{" + entry.getKey() + "}";
                    if (result.contains(token)) result = result.replace(token, String.valueOf(entry.getValue()));
                    else query.add(entry.getKey() + "=" + entry.getValue());
                }
                return result + (query.isEmpty() ? "" : "?" + String.join("&", query));
            }
        });
        Context context = new Context(Locale.forLanguageTag("vi-VN"));
        Map<String, Object> session = new HashMap<>();
        session.put("LOGGED_IN_USER_ID", null);
        context.setVariable("session", session);
        context.setVariable("cartCount", 3);
        context.setVariable("checkoutToken", "preview-only");
        var item = new CartItemResponse(1, 1, "Táo Envy New Zealand", "Hộp 1 kg", "Vườn trái cây An Nhiên",
                2, 10, new BigDecimal("129000"), new BigDecimal("258000"), true, null);
        var otherItem = new CartItemResponse(2, 2, "Cam vàng mọng nước", "Túi 1 kg", "Nông trại Xanh",
                1, 15, new BigDecimal("89000"), new BigDecimal("89000"), true, null);
        var shops = List.of(new CheckoutResponse.ShopGroup(1, item.shopName(), List.of(item), item.subtotal(), BigDecimal.ZERO, item.subtotal()),
                new CheckoutResponse.ShopGroup(2, otherItem.shopName(), List.of(otherItem), otherItem.subtotal(), BigDecimal.ZERO, otherItem.subtotal()));
        context.setVariable("checkout", new CheckoutResponse(shops, List.of(1, 2), 3,
                new BigDecimal("347000"), BigDecimal.ZERO, new BigDecimal("347000")));
        Address address = new Address(); address.setAddressId(1); address.setIsDefault(true);
        address.setReceiverName("Nguyễn Minh Anh"); address.setReceiverPhone("0901234567");
        address.setAddressLine("24 Nguyễn Huệ"); address.setWard("Phường Bến Nghé");
        address.setDistrict("Quận 1"); address.setCity("TP. Hồ Chí Minh");
        context.setVariable("addresses", List.of(address));
        String html = engine.process("customer/checkout", context);
        assertTrue(html.contains("Táo Envy")); assertTrue(html.contains("347.000đ"));
        assertTrue(html.contains("name=\"addressId\"")); assertFalse(html.contains("th:text"));
        Path preview = Path.of("target/checkout-preview"); Files.createDirectories(preview);
        Files.writeString(preview.resolve("checkout.html"), html);
        context.setVariable("addresses", List.of());
        String empty = engine.process("customer/checkout", context);
        assertTrue(empty.contains("Bạn chưa có địa chỉ")); assertTrue(empty.contains("disabled=\"disabled\""));
        Files.writeString(preview.resolve("checkout-empty.html"), empty);
        context.setVariable("orders", List.of(new CheckoutResponse.PlacedOrder(101, item.shopName(), item.subtotal())));
        String receipt = engine.process("customer/checkout-success", context);
        assertTrue(receipt.contains("Đơn hàng #101")); assertTrue(receipt.contains("Chờ xác nhận"));
        Files.writeString(preview.resolve("success.html"), receipt);
        context.setVariable("cart", new CartSummaryResponse(List.of(item), 2, item.subtotal(), true));
        context.setVariable("cartToken", "preview-only");
        String cart = engine.process("customer/cart", context);
        assertTrue(cart.contains("form=\"checkoutSelectionForm\""));
        assertTrue(cart.contains("/customer/checkout"));
    }
}
