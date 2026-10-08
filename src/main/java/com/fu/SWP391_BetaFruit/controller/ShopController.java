package com.fu.SWP391_BetaFruit.controller;

import com.fu.SWP391_BetaFruit.dto.response.ShopProfileResponse;
import com.fu.SWP391_BetaFruit.service.ShopService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/shop")
public class ShopController {

    @Autowired
    private ShopService shopService;

    @GetMapping("/profile")
    public String getShopProfile(Model model, HttpSession session) throws Exception {
        Integer id = (Integer) session.getAttribute("LOGGED_IN_USER_ID");
        if (id == null) {
            return "redirect:/auth/login";
        }
        try {
            ShopProfileResponse shopProfileResponse = shopService.getShopProfile(id);
            model.addAttribute("shopProfileResponse", shopProfileResponse);
            return "shop/shop-profile";
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("errorMessage", e.getMessage());
            return "index";
        }
    }


}
