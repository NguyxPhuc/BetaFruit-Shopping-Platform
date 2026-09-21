package com.fu.SWP391_BetaFruit.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ErrorPageController {

    @GetMapping("/403")
    public String accessDenied() {
        return "error/403";
    }
}
