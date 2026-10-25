package com.fu.SWP391_BetaFruit.controller;

import com.fu.SWP391_BetaFruit.dto.request.ShopProfileRequest;
import com.fu.SWP391_BetaFruit.dto.request.ShopSetupRequest;
import com.fu.SWP391_BetaFruit.dto.response.ShopProfileResponse;
import com.fu.SWP391_BetaFruit.enums.ShopApprovalStatus;
import com.fu.SWP391_BetaFruit.service.ShopService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
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

    @GetMapping("/setup-profile")
    public String showSetupProfileForm(Model model, HttpSession session) {
        ShopSetupRequest request = new ShopSetupRequest();
        model.addAttribute("shopSetupRequest", request);
        return "shop/setup-profile";
    }

    @PostMapping("/setup-profile")
    public String setUpShopProfile(@Valid @ModelAttribute("shopSetupRequest") ShopSetupRequest request, BindingResult bindingResult, HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        Integer userId = (Integer) session.getAttribute("LOGGED_IN_USER_ID");
        if (bindingResult.hasErrors()) {
            model.addAttribute("errorMessage", "Vui lòng kiểm tra lại các thông tin nhập chưa hợp lệ.");
            return "shop/setup-profile";
        }
        try {
            shopService.setUpProfileShop(userId, request);
            session.setAttribute("SHOP_APPROVAL_STATUS", ShopApprovalStatus.PENDING);
            redirectAttributes.addFlashAttribute("successMessage", "Nộp hồ sơ thành công! Đang chờ sàn phê duyệt.");
            return "redirect:/shop/pending-approval";
        }catch (Exception e){
            model.addAttribute("errorMessage", e.getMessage());
            return "shop/setup-profile";
        }
    }

    @GetMapping("/pending-approval")
    public String showPendingApproval() {
        return "shop/pending-approval";
    }

    @GetMapping("/rejected")
    public String showRejected() {
        return "shop/rejected";
    }
}
