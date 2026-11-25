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

    /**
     * Tác dụng: Mở trang đánh giá theo đơn: lấy ID khách và gọi getOrderForReview để tải các sản phẩm được
     * đánh giá cùng review đã có.
     * Màn hình / luồng: Màn customer/order-reviews; URL gốc /customer/reviews, gọi CustomerOrderService và
     * CustomerReviewService.
     */
    @GetMapping("/orders/{orderId}")
    public String orderReviews(@PathVariable Integer orderId, HttpSession session, Model model) {
        Integer customerId = customerId(session);
        if (customerId == null) return "redirect:/auth/login";
        model.addAttribute("order", customerOrderService.getOrderForReview(orderId, customerId));
        return "customer/order-reviews";
    }

    /**
     * Tác dụng: Nhận form đánh giá mới, xử lý lỗi kiểm tra đầu vào rồi gọi createReview; giữ bản nháp khi
     * lỗi và chuyển lại trang đánh giá.
     * Màn hình / luồng: Màn customer/order-reviews; URL gốc /customer/reviews, gọi CustomerOrderService và
     * CustomerReviewService.
     */
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

    /**
     * Tác dụng: Nhận form sửa đánh giá, xử lý lỗi đầu vào rồi gọi updateReview; trả thông báo và bản nháp
     * nếu sửa không thành công.
     * Màn hình / luồng: Màn customer/order-reviews; URL gốc /customer/reviews, gọi CustomerOrderService và
     * CustomerReviewService.
     */
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

    /**
     * Tác dụng: Nhận nút Ẩn đánh giá, gọi hideReview với ID khách hiện tại rồi chuyển lại trang đánh giá
     * kèm kết quả.
     * Màn hình / luồng: Màn customer/order-reviews; URL gốc /customer/reviews, gọi CustomerOrderService và
     * CustomerReviewService.
     */
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

    /**
     * Tác dụng: Đọc ID khách đăng nhập từ session để các thao tác đánh giá kiểm tra đúng chủ sở hữu.
     * Màn hình / luồng: Màn customer/order-reviews; URL gốc /customer/reviews, gọi CustomerOrderService và
     * CustomerReviewService.
     */
    private Integer customerId(HttpSession session) {
        return (Integer) session.getAttribute("LOGGED_IN_USER_ID");
    }

    /**
     * Tác dụng: Tạo URL quay lại trang đánh giá của đơn sau gửi/sửa/ẩn; nếu thiếu orderId thì về danh sách
     * đơn khách.
     * Màn hình / luồng: Màn customer/order-reviews; URL gốc /customer/reviews, gọi CustomerOrderService và
     * CustomerReviewService.
     */
    private String orderRedirect(Integer orderId) {
        return orderId == null ? "redirect:/orders" : "redirect:/customer/reviews/orders/" + orderId;
    }
}
