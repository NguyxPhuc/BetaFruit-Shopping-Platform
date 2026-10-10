package com.fu.SWP391_BetaFruit.controller;

import com.fu.SWP391_BetaFruit.dto.response.CustomerMembershipResponse;
import com.fu.SWP391_BetaFruit.service.MembershipTierService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class MembershipController {

    private final MembershipTierService membershipTierService;

    /**
     * Màn hình Bảng điều khiển Hội viên dành cho Khách hàng (FE-09.1, FE-09.2)
     * Xem Tổng chi tiêu tích lũy (BR-LY-01), Tiến độ lên hạng kế tiếp (BR-LY-02),
     * Cấp bậc và Đặc quyền hội viên (BR-LY-03).
     */
    @GetMapping("/membership")
    public String viewMembership(HttpSession session, Model model) {
        Integer loggedInUserId = (Integer) session.getAttribute("LOGGED_IN_USER_ID");
        if (loggedInUserId == null) {
            return "redirect:/auth/login";
        }

        try {
            CustomerMembershipResponse membershipData = membershipTierService.getCustomerMembershipData(loggedInUserId);
            model.addAttribute("membership", membershipData);
            return "customer/membership";
        } catch (Exception e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "redirect:/profile";
        }
    }
}
