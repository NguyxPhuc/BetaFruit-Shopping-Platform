package com.fu.SWP391_BetaFruit.controller;

import com.fu.SWP391_BetaFruit.dto.request.ChangePasswordRequest;
import com.fu.SWP391_BetaFruit.dto.request.CustomerProfileRequest;
import com.fu.SWP391_BetaFruit.dto.response.CustomerProfileResponse;
import com.fu.SWP391_BetaFruit.service.UserService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;


@Controller
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/")
    public String home() {
        return "index";
    }

    @GetMapping("/customer/profile")
    public String getCustomerProfile(Model model, HttpSession session) throws Exception {
        Integer id = (Integer) session.getAttribute("LOGGED_IN_USER_ID");
        try {
            CustomerProfileResponse customerProfileResponse = userService.getCustomerProfile(id);
            model.addAttribute("customerProfileResponse", customerProfileResponse);
            return "customer/profile";
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("errorMessage", e.getMessage());
            return "index";
        }
    }


    @GetMapping("/change-password")
    public String showChangePasswordForm(Model model, HttpSession session) {
        Integer userId = (Integer) session.getAttribute("LOGGED_IN_USER_ID");
        if (userId == null) {
            return "redirect:/auth/login";
        }

        model.addAttribute("changePasswordRequest", new ChangePasswordRequest());
        return "auth/change-password";
    }

    @PostMapping("/change-password")
    public String processChangePassword(@Valid @ModelAttribute("changePasswordRequest") ChangePasswordRequest request,
                                        BindingResult bindingResult,
                                        HttpSession session,
                                        RedirectAttributes redirectAttributes) {
        Integer userId = (Integer) session.getAttribute("LOGGED_IN_USER_ID");
        if (userId == null) {
            return "redirect:/auth/login";
        }
        if (bindingResult.hasErrors()) {
            return "auth/change-password";
        }
        try {
            userService.changePassword(userId, request);
            redirectAttributes.addFlashAttribute("successMessage", "Đổi mật khẩu thành công!");
            return "redirect:/change-password";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/change-password";
        }
    }

    @GetMapping("/customer/profile/edit")
    public String showEditCustomerProfileForm(Model model, HttpSession session, RedirectAttributes redirectAttributes) {
        Integer userId = (Integer) session.getAttribute("LOGGED_IN_USER_ID");
        if (userId == null) {
            return "redirect:/auth/login";
        }
        try {
            CustomerProfileResponse currentProfile = userService.getCustomerProfile(userId);
            CustomerProfileRequest request = new CustomerProfileRequest();
            request.setUsername(currentProfile.getUsername());
            request.setFullName(currentProfile.getFullName());
            request.setEmail(currentProfile.getEmail());
            request.setPhone(currentProfile.getPhone());

            model.addAttribute("customerProfileRequest", request);
            return "customer/edit-profile";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/customer/profile";
        }
    }

    @PostMapping("/customer/profile/edit")
    public String editCustomerProfile(@Valid @ModelAttribute("customerProfileRequest") CustomerProfileRequest request, BindingResult bindingResult, HttpSession session, RedirectAttributes redirectAttributes, Model model){
        Integer userId = (Integer) session.getAttribute("LOGGED_IN_USER_ID");
        if (userId == null) {
            return "redirect:/auth/login";
        }
        if (bindingResult.hasErrors()) {
            model.addAttribute("errorMessage", "Vui lòng kiểm tra lại các thông tin nhập chưa hợp lệ.");
            return "customer/edit-profile";
        }

        try {
            userService.editCustomerProfile(userId, request);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật hồ sơ thành công!");
            return "redirect:/customer/profile";
        } catch (Exception e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "customer/edit-profile";
        }
    }
}
