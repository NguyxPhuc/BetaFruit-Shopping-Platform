package com.fu.SWP391_BetaFruit.controller;

import com.fu.SWP391_BetaFruit.dto.response.PaymentResultResponse;
import com.fu.SWP391_BetaFruit.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

@Controller
public class PaymentController {
    @Autowired
    private PaymentService paymentService;

    @GetMapping( "/payment/vnpay-ipn")
    @ResponseBody
    public Map<String, String> handleVnPayIpn(@RequestParam Map<String, String> params) {
        return paymentService.processVnpayIpn(params);
    }

    @GetMapping( "/payment/vnpay-return")
    public String handleVnPayReturn(@RequestParam Map<String, String> params, Model model) {
        PaymentResultResponse resultResponse = paymentService.processVnpayReturn(params);
        model.addAttribute("result", resultResponse);
        if (resultResponse.isSuccess()) {
            return "payment/success";
        } else {
            return "payment/fail";  
        }
    }
}
