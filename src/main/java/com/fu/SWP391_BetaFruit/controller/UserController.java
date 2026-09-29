package com.fu.SWP391_BetaFruit.controller;

import com.fu.SWP391_BetaFruit.dto.response.CustomerProfileResponse;
import com.fu.SWP391_BetaFruit.dto.response.ShopProfileResponse;
import com.fu.SWP391_BetaFruit.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;


@Controller
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/")
    public String home() {
        return "index";
    }

    @GetMapping("/profile")
    public String getCustomerProfile(Model model, HttpSession session) throws Exception {
        Integer id = (Integer) session.getAttribute("LOGGED_IN_USER_ID");
        if (id == null) {
            return "redirect:/auth/login";
        }
        try {
            CustomerProfileResponse customerProfileResponse = userService.getCustomerProfile(id);
            model.addAttribute("customerProfileResponse", customerProfileResponse);
            return "customer/customer-profile";
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("errorMessage", e.getMessage());
            return "index";
        }
    }

    @GetMapping("/shop/profile")
    public String getShopProfile(Model model, HttpSession session) throws Exception {
        Integer id = (Integer) session.getAttribute("LOGGED_IN_USER_ID");
        if (id == null) {
            return "redirect:/auth/login";
        }
        try {
            ShopProfileResponse shopProfileResponse = userService.getShopProfile(id);
            model.addAttribute("shopProfileResponse", shopProfileResponse);
            return "shop/shop-profile";
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("errorMessage", e.getMessage());
            return "index";
        }
    }
}
