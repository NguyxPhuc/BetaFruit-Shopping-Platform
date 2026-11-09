package com.fu.SWP391_BetaFruit.controller;

import com.fu.SWP391_BetaFruit.dto.request.ShopReviewReplyRequest;
import com.fu.SWP391_BetaFruit.service.ShopReviewService;
import jakarta.servlet.http.HttpSession;
import com.fu.SWP391_BetaFruit.utils.ListPagination;
import org.springframework.util.MultiValueMap;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
@RequestMapping("/shop/reviews")
public class ShopReviewController {
    private final ShopReviewService shopReviewService;

    @PostMapping("/{reviewId}/reply")
    public String reply(@PathVariable Integer reviewId,
                        @Valid @ModelAttribute ShopReviewReplyRequest request,
                        BindingResult bindingResult,
                        @RequestParam(required = false) Integer rating,
                        @RequestParam(required = false) Boolean isReported,
                        @RequestParam MultiValueMap<String, String> paginationParams,
                        HttpSession session,
                        RedirectAttributes redirect) {
        String target = ListPagination.redirect("/shop/reviews", paginationParams, 1, "rating", "isReported");
        if (bindingResult.hasErrors()) {
            redirect.addFlashAttribute("errorMessage", bindingResult.getAllErrors().getFirst().getDefaultMessage());
            return target;
        }

        Integer ownerId = (Integer) session.getAttribute("LOGGED_IN_USER_ID");
        try {
            shopReviewService.saveReply(ownerId, reviewId, request.getReply());
            redirect.addFlashAttribute("successMessage", "Đã lưu phản hồi cho đánh giá.");
        } catch (RuntimeException exception) {
            redirect.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return target;
    }

}
