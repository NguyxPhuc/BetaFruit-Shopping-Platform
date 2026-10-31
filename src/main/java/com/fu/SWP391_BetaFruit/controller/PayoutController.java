package com.fu.SWP391_BetaFruit.controller;

import com.fu.SWP391_BetaFruit.dto.request.PayoutCreateRequest;
import com.fu.SWP391_BetaFruit.dto.request.ProcessPayoutRequest;
import com.fu.SWP391_BetaFruit.dto.response.PayoutDetailResponse;
import com.fu.SWP391_BetaFruit.dto.response.PayoutResponse;
import com.fu.SWP391_BetaFruit.dto.response.ShopBalanceResponse;
import com.fu.SWP391_BetaFruit.service.PayoutService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class PayoutController {
    @Autowired
    private PayoutService payoutService;

    @GetMapping("/shop/balance")
    public String getBalanceShop(Model model, HttpSession session) {
        Integer id = (Integer) session.getAttribute("LOGGED_IN_USER_ID");
        try {
            ShopBalanceResponse response = payoutService.getShopBalance(id);
            model.addAttribute("shopBalanceResponse", response);
        }catch (Exception e){
            model.addAttribute("errorMessage", e.getMessage());
        }
        return "shop/balance";
    }

    @GetMapping("/shop/payout-request")
    public String getAllPayoutRequestByShop(Model model, HttpSession session) {
        Integer id = (Integer) session.getAttribute("LOGGED_IN_USER_ID");
        try {
            List<PayoutResponse> payoutResponses = payoutService.getAllPayoutRequestsByShop(id);
            model.addAttribute("payoutResponses", payoutResponses);
        }catch (Exception e){
            model.addAttribute("errorMessage", e.getMessage());
        }
        return "shop/payout-request";
    }

    @GetMapping("/shop/payout-request/{id}")
    public String getPayoutRequestDetailByShop(@PathVariable("id") Integer requestId, Model model, HttpSession session) {
        Integer id = (Integer) session.getAttribute("LOGGED_IN_USER_ID");
        try {
            PayoutDetailResponse detailResponse = payoutService.getPayoutDetailByShop(id, requestId);
            model.addAttribute("detailResponse", detailResponse);
        }catch (Exception e){
            model.addAttribute("errorMessage", e.getMessage());
        }
        return "shop/payout-request-detail";
    }

    @GetMapping("/staff/payout-request")
    public String getAllPayoutRequest(Model model, HttpSession session) {
        Integer id = (Integer) session.getAttribute("LOGGED_IN_USER_ID");
        try {
            List<PayoutResponse> payoutResponses = payoutService.getAllPayoutRequests(id);
            model.addAttribute("payoutResponses", payoutResponses);
        }catch (Exception e){
            model.addAttribute("errorMessage", e.getMessage());
        }
        return "staff/payout-request";
    }

    @GetMapping("/staff/payout-request/{id}")
    public String getPayoutRequestDetail(@PathVariable("id") Integer requestId, Model model, HttpSession session) {
        Integer id = (Integer) session.getAttribute("LOGGED_IN_USER_ID");
        try {
            PayoutDetailResponse detailResponse = payoutService.getPayoutDetail(requestId, id);
            model.addAttribute("detailResponse", detailResponse);
            model.addAttribute("requestId", requestId);
        }catch (Exception e){
            model.addAttribute("errorMessage", e.getMessage());
        }
        return "staff/payout-request-detail";
    }

    @PostMapping("/staff/payout-request/{id}/process")
    public String handleProcessPayout(@PathVariable("id") Integer requestId, @ModelAttribute ProcessPayoutRequest req, HttpSession session, RedirectAttributes redirectAttributes) {
        Integer userId = (Integer) session.getAttribute("LOGGED_IN_USER_ID");
        try {
            payoutService.processPayout(requestId, userId, req);
            redirectAttributes.addFlashAttribute("successMessage", "Xử lý yêu cầu rút tiền #" + requestId + " thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/staff/payout-request";
    }

    @GetMapping("/shop/payout-request-form")
    public String showFormPayoutRequest(Model model) {
        PayoutCreateRequest req = new PayoutCreateRequest();
        model.addAttribute("payoutCreateRequest", req);
        return "shop/payout-request-form";
    }

    @PostMapping("/shop/payout-request-form")
    public String handleRequestPayout(@Valid @ModelAttribute("payoutCreateRequest") PayoutCreateRequest request, BindingResult bindingResult, HttpSession session,RedirectAttributes redirectAttributes, Model model) {
        Integer userId = (Integer) session.getAttribute("LOGGED_IN_USER_ID");
        if (bindingResult.hasErrors()) {
            // Trả về view, Thymeleaf tự bind message lỗi từ DTO qua th:errors
            return "shop/payout-request-form";
        }
        try {
            payoutService.requestPayout(userId, request);
            redirectAttributes.addFlashAttribute("successMessage", "Gửi yêu cầu rút tiền thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/shop/payout-request";
    }
}
