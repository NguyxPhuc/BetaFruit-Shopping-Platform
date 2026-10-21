package com.fu.SWP391_BetaFruit.interceptor;

import com.fu.SWP391_BetaFruit.enums.RoleName;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.List;

@Component
public class ShopOwnerInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        HttpSession session = request.getSession(false);

        if (session == null || !(session.getAttribute("LOGGED_IN_USER_ID") instanceof Integer)) {
            response.sendRedirect(request.getContextPath() + "/auth/login");
            return false;
        }
        Object rawRoles = session.getAttribute("LOGGED_IN_ROLES");
        if (rawRoles instanceof List<?> roles && roles.contains(RoleName.SHOP_OWNER)) {
            return true;
        }
        response.sendRedirect(request.getContextPath() + "/403");
        return false;
    }
}
