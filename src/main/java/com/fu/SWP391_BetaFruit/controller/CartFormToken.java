package com.fu.SWP391_BetaFruit.controller;

import jakarta.servlet.http.HttpSession;
import java.util.UUID;

final class CartFormToken {
    private CartFormToken() {}

    static String get(HttpSession session) {
        synchronized (session) {
            String token = (String) session.getAttribute("CART_FORM_TOKEN");
            if (token == null) {
                token = UUID.randomUUID().toString();
                session.setAttribute("CART_FORM_TOKEN", token);
            }
            return token;
        }
    }

    static void validate(HttpSession session, String token) {
        if (token == null || !token.equals(session.getAttribute("CART_FORM_TOKEN")))
            throw new IllegalArgumentException("Phiên thao tác không hợp lệ. Vui lòng tải lại trang.");
    }
}
