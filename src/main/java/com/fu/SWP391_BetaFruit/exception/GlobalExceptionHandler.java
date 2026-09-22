package com.fu.SWP391_BetaFruit.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public String handleException(Exception ex, HttpServletRequest request, RedirectAttributes redirectAttributes) {
        String message = ex.getMessage();
        if (message == null || message.trim().isEmpty()) {
            message = "Đã xảy ra lỗi trong quá trình xử lý! Vui lòng thử lại sau.";
        }
        redirectAttributes.addFlashAttribute("errorMessage", message);

        String referer = request.getHeader("Referer");
        if (referer != null && !referer.trim().isEmpty()) {
            return "redirect:" + referer;
        }
        return "redirect:/";
    }
}
