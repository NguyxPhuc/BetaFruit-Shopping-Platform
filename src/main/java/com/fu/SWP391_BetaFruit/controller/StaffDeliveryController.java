package com.fu.SWP391_BetaFruit.controller;

import com.fu.SWP391_BetaFruit.dto.response.DispatchExceptionOrderResponse;
import com.fu.SWP391_BetaFruit.dto.response.DispatchShipperCandidateResponse;
import com.fu.SWP391_BetaFruit.service.DispatchConfigService;
import com.fu.SWP391_BetaFruit.service.StaffDispatchService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/staff/deliveries")
public class StaffDeliveryController {

    private final StaffDispatchService staffDispatchService;
    private final DispatchConfigService dispatchConfigService;

    @GetMapping("/exceptions")
    public String viewExceptionsQueue(HttpSession session, Model model) {
        Integer staffId = (Integer) session.getAttribute("LOGGED_IN_USER_ID");
        if (staffId == null) {
            return "redirect:/auth/login";
        }

        List<DispatchExceptionOrderResponse> exceptions = staffDispatchService.getExceptionOrders();
        List<DispatchShipperCandidateResponse> shippers = staffDispatchService.getAvailableShippers();

        model.addAttribute("exceptions", exceptions);
        model.addAttribute("shippers", shippers);
        model.addAttribute("dispatchConfig", dispatchConfigService);

        return "operations-staff/dispatch-exceptions";
    }

    @PostMapping("/reassign")
    public String reassignShipper(
            @RequestParam Integer orderId,
            @RequestParam Integer shipperId,
            @RequestParam(defaultValue = "Điều phối cứu viện do sự cố trên đường") String reason,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        Integer staffId = (Integer) session.getAttribute("LOGGED_IN_USER_ID");
        if (staffId == null) {
            return "redirect:/auth/login";
        }

        try {
            staffDispatchService.reassignShipper(orderId, shipperId, staffId, reason);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Đã điều phối và chuyển giao đơn hàng #" + orderId + " cho tài xế mới thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/staff/deliveries/exceptions";
    }

    @PostMapping("/config")
    public String updateDispatchConfig(
            @RequestParam(required = false) Integer maxOngoingOrders,
            @RequestParam(required = false) BigDecimal codCeilingLimit,
            @RequestParam(required = false) Integer pickupTimeoutMinutes,
            @RequestParam(required = false, defaultValue = "false") boolean resetDefaults,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        Integer staffId = (Integer) session.getAttribute("LOGGED_IN_USER_ID");
        if (staffId == null) {
            return "redirect:/auth/login";
        }

        try {
            if (resetDefaults) {
                dispatchConfigService.resetToDefaults();
                redirectAttributes.addFlashAttribute("successMessage",
                        "Đã khôi phục các thông số điều phối vận hành về mặc định!");
            } else {
                if (maxOngoingOrders != null && maxOngoingOrders > 0) {
                    dispatchConfigService.setMaxOngoingOrders(maxOngoingOrders);
                }
                if (codCeilingLimit != null && codCeilingLimit.compareTo(BigDecimal.ZERO) >= 0) {
                    dispatchConfigService.setCodCeilingLimit(codCeilingLimit);
                }
                if (pickupTimeoutMinutes != null && pickupTimeoutMinutes > 0) {
                    dispatchConfigService.setPickupTimeoutMinutes(pickupTimeoutMinutes);
                }
                redirectAttributes.addFlashAttribute("successMessage",
                        "Đã cập nhật các thông số điều phối vận hành thành công!");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi cập nhật cấu hình: " + e.getMessage());
        }

        return "redirect:/staff/deliveries/exceptions";
    }
}
