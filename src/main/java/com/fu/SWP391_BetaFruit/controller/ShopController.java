package com.fu.SWP391_BetaFruit.controller;

import com.fu.SWP391_BetaFruit.dto.request.ReviewReportRequest;
import com.fu.SWP391_BetaFruit.dto.request.ShopProfileRequest;
import com.fu.SWP391_BetaFruit.dto.response.ShopProfileResponse;
import com.fu.SWP391_BetaFruit.service.ReviewReportService;
import com.fu.SWP391_BetaFruit.service.ShopService;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/shop")
public class ShopController {

    @Autowired
    private ShopService shopService;

    @GetMapping({"", "/", "/dashboard"})
    public String getShopDashboard(Model model, HttpSession session) {
        Integer id = (Integer) session.getAttribute("LOGGED_IN_USER_ID");
        if (id == null) {
            return "redirect:/auth/login";
        }
        try {
            ShopProfileResponse shopProfileResponse = shopService.getShopProfile(id);
            model.addAttribute("shopProfileResponse", shopProfileResponse);
            return "shop/dashboard";
        } catch (Exception e) {
            return "shop/dashboard";
        }
    }

    @GetMapping("/profile")
    public String getShopProfile(Model model, HttpSession session) throws Exception {
        Integer id = (Integer) session.getAttribute("LOGGED_IN_USER_ID");
        if (id == null) {
            return "redirect:/auth/login";
        }
        try {
            ShopProfileResponse shopProfileResponse = shopService.getShopProfile(id);
            model.addAttribute("shopProfileResponse", shopProfileResponse);
            return "shop/profile";
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("errorMessage", e.getMessage());
            return "index";
        }
    }

    @GetMapping("/profile/edit")
    public String showEditShopProfileForm(Model model, HttpSession session, RedirectAttributes redirectAttributes)  {
        Integer userId = (Integer) session.getAttribute("LOGGED_IN_USER_ID");
        if (userId == null) {
            return "redirect:/auth/login";
        }
        try {
            ShopProfileResponse shopProfileResponse = shopService.getShopProfile(userId);
            ShopProfileRequest shopProfileRequest = new ShopProfileRequest();
            shopProfileRequest.setShopName(shopProfileResponse.getShopName());
            shopProfileRequest.setShopDescription(shopProfileResponse.getShopDescription());
            shopProfileRequest.setEmail(shopProfileResponse.getEmail());
            shopProfileRequest.setPhone(shopProfileResponse.getPhone());
            shopProfileRequest.setAccountName(shopProfileResponse.getAccountName());
            shopProfileRequest.setBankName(shopProfileResponse.getBankName());
            shopProfileRequest.setBankAccountNumber(shopProfileResponse.getBankAccountNumber());

            model.addAttribute("shopProfileRequest", shopProfileRequest);
            return "shop/edit-profile";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/shop/profile";
        }
    }

    @PostMapping("/profile/edit")
    public String editShopProfile(@Valid @ModelAttribute("shopProfileRequest") ShopProfileRequest shopProfileRequest, BindingResult bindingResult, HttpSession session, RedirectAttributes redirectAttributes,Model model){
        Integer userId = (Integer) session.getAttribute("LOGGED_IN_USER_ID");
        if (userId == null) {
            return "redirect:/auth/login";
        }
        if (bindingResult.hasErrors()) {
            model.addAttribute("errorMessage", "Vui lòng kiểm tra lại các thông tin nhập chưa hợp lệ.");
            return "shop/edit-profile";
        }
        try{
            shopService.editShopProfile(userId, shopProfileRequest);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật hồ sơ cửa hàng thành công!");
            return "redirect:/shop/profile";
        } catch (Exception e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "shop/edit-profile";
        }
    }

    // =========================================================================
    // ============= FE-10.2: KHIẾU NẠI ĐÁNH GIÁ (REVIEW DISPUTE) ==============
    // =========================================================================

    @Autowired
    private ReviewReportService reviewReportService;

    /**
     * FE-10.2a: Hiển thị danh sách đánh giá của các sản phẩm thuộc gian hàng của Shop.
     * Cho phép lọc theo số sao, trạng thái đã báo cáo và phân trang.
     */
    @GetMapping("/reviews")
    public String getShopReviews(@RequestParam(value = "page", defaultValue = "1") int page,
                                 @RequestParam(value = "size", defaultValue = "10") int size,
                                 @RequestParam(value = "rating", required = false) Integer rating,
                                 @RequestParam(value = "isReported", required = false) Boolean isReported,
                                 HttpSession session,
                                 Model model) {
        Integer userId = (Integer) session.getAttribute("LOGGED_IN_USER_ID");
        if (userId == null) {
            return "redirect:/auth/login";
        }
        try {
            ShopProfileResponse shopProfileResponse = shopService.getShopProfile(userId);
            model.addAttribute("shopProfileResponse", shopProfileResponse);
            model.addAllAttributes(reviewReportService.getShopReviewsPageData(userId, rating, isReported, page, size));
            return "shop/reviews/list";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Không thể tải danh sách đánh giá: " + e.getMessage());
            return "shop/dashboard";
        }
    }

    /**
     * FE-10.2b: Tiếp nhận đơn khiếu nại đánh giá vi phạm từ Shop gửi lên Staff.
     */
    @PostMapping("/reviews/{reviewId}/report")
    public String reportInappropriateReview(@PathVariable("reviewId") Integer reviewId,
                                            @Valid @ModelAttribute("reportRequest") ReviewReportRequest reportRequest,
                                            BindingResult bindingResult,
                                            HttpSession session,
                                            RedirectAttributes redirectAttributes) {
        Integer userId = (Integer) session.getAttribute("LOGGED_IN_USER_ID");
        if (userId == null) {
            return "redirect:/auth/login";
        }
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", bindingResult.getAllErrors().get(0).getDefaultMessage());
            return "redirect:/shop/reviews";
        }
        try {
            reviewReportService.createReviewReport(userId, reviewId, reportRequest);
            redirectAttributes.addFlashAttribute("successMessage", "Gửi khiếu nại thành công! Yêu cầu đang được nhân viên vận hành (Staff) xem xét.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/shop/reviews";
    }
}
