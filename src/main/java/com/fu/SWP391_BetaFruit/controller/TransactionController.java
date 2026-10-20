package com.fu.SWP391_BetaFruit.controller;

import com.fu.SWP391_BetaFruit.dto.response.TransactionDetailResponse;
import com.fu.SWP391_BetaFruit.dto.response.TransactionResponse;
import com.fu.SWP391_BetaFruit.service.TransactionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class TransactionController {
    @Autowired
    private TransactionService transactionService;

    @GetMapping("customer/payment/transaction-list")
    public String getAllTransactions(Model model, HttpSession session) {
        Integer customerId = (Integer) session.getAttribute("LOGGED_IN_USER_ID");
        try {
            List<TransactionResponse> transactionResponseList = transactionService
                    .getAllTransactionsByCustomer(customerId);
            model.addAttribute("transactionResponseList", transactionResponseList);
        } catch (Exception e) {
            model.addAttribute("transactionResponseList", null);
        }
        return "customer/transaction-list";
    }

    @GetMapping("customer/payment/transaction-detail/{txn}")
    public String getTransactionDetails(@PathVariable("txn") String transactionRef, Model model, HttpSession session,
            RedirectAttributes redirectAttributes) {
        Integer customerId = (Integer) session.getAttribute("LOGGED_IN_USER_ID");
        try {
            TransactionDetailResponse detailResponse = transactionService
                    .getTransactionDetailsByCustomer(transactionRef, customerId);
            model.addAttribute("detailResponse", detailResponse);
            return "customer/transaction-detail";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/customer/payment/transaction-list";
        }
    }
}
