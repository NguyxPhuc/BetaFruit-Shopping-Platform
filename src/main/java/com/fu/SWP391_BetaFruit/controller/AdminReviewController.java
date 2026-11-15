package com.fu.SWP391_BetaFruit.controller;

import com.fu.SWP391_BetaFruit.service.ReviewReportService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/reviews")
public class AdminReviewController {
    private final ReviewReportService reviewReportService;

    @GetMapping
    public String list(@RequestParam(defaultValue = "PENDING") String status,
                       @RequestParam(defaultValue = "1") int page,
                       @RequestParam(defaultValue = "10") int size, Model model) {
        model.addAllAttributes(reviewReportService.getStaffReportPageData(status, page, size));
        model.addAttribute("reportBasePath", "/admin/reviews");
        return "operations-staff/reviews/list";
    }

    @PostMapping("/{reportId}/resolve")
    public String resolve(@PathVariable Integer reportId, @RequestParam String action,
                          @RequestParam(required = false) String staffNote,
                          HttpSession session, RedirectAttributes redirectAttributes) {
        Integer adminId = (Integer) session.getAttribute("LOGGED_IN_USER_ID");
        if (adminId == null) return "redirect:/auth/login";
        try {
            reviewReportService.resolveReport(reportId, action, staffNote, adminId);
            redirectAttributes.addFlashAttribute("successMessage", "Xử lý báo cáo đánh giá thành công!");
        } catch (IllegalArgumentException | IllegalStateException | SecurityException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/reviews";
    }
}
