package com.fu.SWP391_BetaFruit.interceptor;

import com.fu.SWP391_BetaFruit.enums.RoleName;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.List;

@Component
public class ShipperInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        HttpSession session = request.getSession(false);

        if (session == null || !(session.getAttribute("LOGGED_IN_USER_ID") instanceof Integer)) {
            reject(request, response, HttpServletResponse.SC_UNAUTHORIZED, "/auth/login");
            return false;
        }

        Object rawRoles = session.getAttribute("LOGGED_IN_ROLES");
        if (rawRoles instanceof List<?> roles && roles.contains(RoleName.DELIVERY_STAFF)) {
            return true;
        }

        reject(request, response, HttpServletResponse.SC_FORBIDDEN, "/403");
        return false;
    }
    private void reject(HttpServletRequest request, HttpServletResponse response, int status, String page) throws java.io.IOException {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        if (path.matches("/(shop|shipper)/orders(?:\\.html)?/api(?:/.*)?")) {
            response.sendError(status);
        } else {
            response.sendRedirect(request.getContextPath() + page);
        }
    }
}
