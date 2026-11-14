package com.fu.SWP391_BetaFruit.controller;

import com.fu.SWP391_BetaFruit.dto.request.CustomerReviewRequest;
import com.fu.SWP391_BetaFruit.service.CustomerReviewService;
import com.fu.SWP391_BetaFruit.service.CustomerOrderService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
@RequestMapping("/customer/reviews")
public class CustomerReviewController {
    private final CustomerReviewService customerReviewService;
    private final CustomerOrderService customerOrderService;

    @GetMapping("/orders/{orderId}")
    public String orderReviews(@PathVariable Integer orderId, HttpSession session, Model model) {
        Integer customerId = customerId(session);
        if (customerId == null) return "redirect:/auth/login";
        model.addAttribute("order", customerOrderService.getOrderForReview(orderId, customerId));
        return "customer/order-reviews";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute CustomerReviewRequest request, BindingResult binding,
                         HttpSession session, RedirectAttributes redirect) {
        if (binding.hasErrors()) {
            redirect.addFlashAttribute("reviewDraft", request);
            redirect.addFlashAttribute("reviewError", "Rating phải từ 1 đến 5; nội dung tối đa 5.000 ký tự.");
            return orderRedirect(request.orderId());
        }
        try {
            customerReviewService.createReview(customerId(session), request);
            redirect.addFlashAttribute("reviewSuccess", "Đã gửi đánh giá sản phẩm.");
        } catch (ResponseStatusException exception) {
            redirect.addFlashAttribute("reviewDraft", request);
            redirect.addFlashAttribute("reviewError", exception.getReason());
        }
        return orderRedirect(request.orderId());
    }

    @PostMapping("/{reviewId}/edit")
    public String update(@PathVariable Integer reviewId, @Valid @ModelAttribute CustomerReviewRequest request,
                         BindingResult binding, HttpSession session, RedirectAttributes redirect) {
        if (binding.hasErrors()) {
            redirect.addFlashAttribute("reviewDraft", request);
            redirect.addFlashAttribute("reviewError", "Rating phải từ 1 đến 5; nội dung tối đa 5.000 ký tự.");
            return orderRedirect(request.orderId());
        }
        try {
            customerReviewService.updateReview(customerId(session), reviewId, request);
            redirect.addFlashAttribute("reviewSuccess", "Đã cập nhật đánh giá sản phẩm.");
        } catch (ResponseStatusException exception) {
            redirect.addFlashAttribute("reviewDraft", request);
            redirect.addFlashAttribute("reviewError", exception.getReason());
        }
        return orderRedirect(request.orderId());
    }

    @PostMapping("/{reviewId}/hide")
    public String hide(@PathVariable Integer reviewId, @RequestParam Integer orderId,
                       HttpSession session, RedirectAttributes redirect) {
        try {
            customerReviewService.hideReview(customerId(session), reviewId);
            redirect.addFlashAttribute("reviewSuccess", "Đã ẩn đánh giá của bạn.");
        } catch (ResponseStatusException exception) {
            redirect.addFlashAttribute("reviewError", exception.getReason());
        }
        return orderRedirect(orderId);
    }

    private Integer customerId(HttpSession session) {
        return (Integer) session.getAttribute("LOGGED_IN_USER_ID");
    }

    private String orderRedirect(Integer orderId) {
        return orderId == null ? "redirect:/orders" : "redirect:/customer/reviews/orders/" + orderId;
    }
}
