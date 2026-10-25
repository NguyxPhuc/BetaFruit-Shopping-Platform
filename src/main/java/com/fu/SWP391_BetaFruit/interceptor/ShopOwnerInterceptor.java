package com.fu.SWP391_BetaFruit.interceptor;

import com.fu.SWP391_BetaFruit.enums.RoleName;
import com.fu.SWP391_BetaFruit.enums.ShopApprovalStatus;
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
        List<RoleName> roles = (List<RoleName>) session.getAttribute("LOGGED_IN_ROLES");
        if (roles == null || !roles.contains(RoleName.SHOP_OWNER)) {
            response.sendRedirect(request.getContextPath() + "/403");
            return false;
        }
        String path = request.getServletPath();
        ShopApprovalStatus status = (ShopApprovalStatus) session.getAttribute("SHOP_APPROVAL_STATUS");
        if (status == null || status == ShopApprovalStatus.DRAFT) {
            if (path.startsWith("/shop/setup-profile")) {
                return true;
            }
            response.sendRedirect(request.getContextPath() + "/shop/setup-profile");
            return false;
        }
        if (status == ShopApprovalStatus.PENDING) {
            if (path.startsWith("/shop/pending-approval")) {
                return true;
            }
            response.sendRedirect(request.getContextPath() + "/shop/pending-approval");
            return false;
        }
        if (status == ShopApprovalStatus.REJECTED) {
            if (path.startsWith("/shop/rejected") || path.startsWith("/shop/setup-profile")) {
                return true;
            }
            response.sendRedirect(request.getContextPath() + "/shop/rejected");
            return false;
        }
        if (status == ShopApprovalStatus.APPROVED) {
            if (path.startsWith("/shop/setup-profile") || path.startsWith("/shop/pending-approval") || path.startsWith("/shop/rejected")) {
                response.sendRedirect(request.getContextPath() + "/shop/dashboard");
                return false;
            }
            return true;
        }
        response.sendRedirect(request.getContextPath() + "/403");
        return false;
    }
}
