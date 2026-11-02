package com.fu.SWP391_BetaFruit.controller;

import com.fu.SWP391_BetaFruit.dto.response.DispatchExceptionOrderResponse;
import com.fu.SWP391_BetaFruit.dto.response.DispatchShipperCandidateResponse;
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

import java.util.List;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/staff/deliveries")
public class StaffDeliveryController {

    private final StaffDispatchService staffDispatchService;

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
}
