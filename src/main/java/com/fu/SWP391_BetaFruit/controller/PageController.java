package com.fu.SWP391_BetaFruit.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@lombok.RequiredArgsConstructor
public class PageController {
    private final com.fu.SWP391_BetaFruit.service.CustomerProductService customerProductService;

    @GetMapping("/403")
    public String accessDenied() {
        return "error/403";
    }

    @GetMapping({"/", "/home"})
    public String homePage(org.springframework.ui.Model model) {
        model.addAttribute("featuredProducts", customerProductService.getFeaturedProducts(8));
        model.addAttribute("homeCategories", customerProductService.getAllActiveCategories().stream().limit(6).toList());
        return "public/index";
    }

    @GetMapping("/404")
    public String notFound() {
        return "error/404";
    }
}
