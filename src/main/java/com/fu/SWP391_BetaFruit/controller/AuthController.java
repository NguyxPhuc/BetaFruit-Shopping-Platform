package com.fu.SWP391_BetaFruit.controller;

import com.fu.SWP391_BetaFruit.dto.request.RegisterRequest;
import com.fu.SWP391_BetaFruit.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @GetMapping("/register")
    public String registerForCustomer(Model model){
        model.addAttribute("registerRequest",new RegisterRequest());
        return "register";
    }

    @PostMapping("/register")
    public String registerForCustomer(@Valid @ModelAttribute("registerRequest") RegisterRequest registerRequest, BindingResult bindingResult, Model model){
        if(bindingResult.hasErrors()){
            return "register";
        }
        try{
            authService.registerForCustomer(registerRequest);
        }catch (Exception e){
            model.addAttribute("errorMessage", e.getMessage());
            return "register";
        }
        return "redirect:/auth/login";
    }

}
