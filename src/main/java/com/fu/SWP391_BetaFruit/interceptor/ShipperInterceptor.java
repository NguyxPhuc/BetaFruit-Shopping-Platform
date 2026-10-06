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
        HttpSession session = request.getSession();

        if(session.getAttribute("LOGGED_IN_USER_ID") == null){
            response.sendRedirect("/auth/login");
            return false;
        }

        List<RoleName> roles = (List<RoleName>) session.getAttribute("LOGGED_IN_ROLES");
        if (roles != null && roles.contains(RoleName.DELIVERY_STAFF)) {
            return true;
        }

        response.sendRedirect("/403");
        return false;
    }
}
