package com.fu.SWP391_BetaFruit.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping("/403")
    public String accessDenied() {
        return "error/403";
    }

    @GetMapping("/home")
    public String homePage() {
        return "index";
    }

    @GetMapping("/404")
    public String notFound() {
        return "error/404";
    }
}
