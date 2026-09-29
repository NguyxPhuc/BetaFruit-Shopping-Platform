package com.fu.SWP391_BetaFruit.controller;

import com.fu.SWP391_BetaFruit.dto.request.LoginRequest;
import com.fu.SWP391_BetaFruit.dto.request.RegisterRequest;
import com.fu.SWP391_BetaFruit.dto.response.LoginResponse;
import com.fu.SWP391_BetaFruit.service.AuthService;
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

import java.util.List;

@Controller
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @GetMapping("/register")
    public String registerForCustomer(Model model){
        model.addAttribute("registerRequest",new RegisterRequest());
        return "auth/register-customer";
    }

    @PostMapping("/register")
    public String registerForCustomer(@Valid @ModelAttribute("registerRequest") RegisterRequest registerRequest, BindingResult bindingResult, Model model){
        if(bindingResult.hasErrors()){
            return "auth/register-customer";
        }
        try{
            authService.registerForCustomer(registerRequest);
        }catch (Exception e){
            e.printStackTrace();
            model.addAttribute("errorMessage", e.getMessage());
            return "auth/register-customer";
        }
        return "redirect:/auth/login";
    }

    @GetMapping("/register-shopowner")
    public String registerForShopOwner(Model model){
        model.addAttribute("registerRequest",new RegisterRequest());
        return "auth/register-shopowner";
    }

    @PostMapping("/register-shopowner")
    public String registerForShopOwner(@Valid @ModelAttribute("registerRequest") RegisterRequest registerRequest, BindingResult bindingResult, Model model){
        if(bindingResult.hasErrors()){
            return "auth/register-shopowner";
        }
        try{
            authService.registerForShopOwner(registerRequest);
        }catch (Exception e){
            e.printStackTrace();
            model.addAttribute("errorMessage", e.getMessage());
            return "auth/register-shopowner";
        }
        return "redirect:/auth/login";
    }

    @GetMapping("/login")
    public String login(Model model){
        model.addAttribute("loginRequest",new LoginRequest());
        return "auth/login";
    }

    @PostMapping("/login")
    public String login(@Valid  @ModelAttribute("loginRequest") LoginRequest loginRequest, BindingResult bindingResult, HttpSession session, Model model){
        if(bindingResult.hasErrors()){
            return "auth/login";
        }
        try{
            LoginResponse loginResponse = authService.login(loginRequest);
            List<String> roles = loginResponse.getRoles();

            session.setAttribute("LOGGED_IN_USER_ID", loginResponse.getUserId());
            session.setAttribute("LOGGED_IN_USERNAME", loginResponse.getUsername());
            session.setAttribute("LOGGED_IN_FULLNAME", loginResponse.getFullName());
            session.setAttribute("LOGGED_IN_ROLES", loginResponse.getRoles());
            if (roles.contains("Admin")) {
                return "redirect:/admin/dashboard";
            } else {
                return "redirect:/home";
            }
        }catch (Exception e){
            e.printStackTrace();
            model.addAttribute("errorMessage", e.getMessage());
            return "auth/login";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpSession session){
        session.invalidate();
        return "redirect:/auth/login";
    }
}
