package com.fu.SWP391_BetaFruit.controller;

import com.fu.SWP391_BetaFruit.dto.request.ShopReviewReplyRequest;
import com.fu.SWP391_BetaFruit.service.ShopReviewService;
import jakarta.servlet.http.HttpSession;
import com.fu.SWP391_BetaFruit.exception.GlobalExceptionHandler.InvalidPagination;
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

    /**
     * Tác dụng: Nhận phản hồi đã kiểm tra đầu vào, gọi ShopReviewService.saveReply rồi chuyển về danh sách,
     * giữ trang và bộ lọc cùng thông báo kết quả.
     * Màn hình / luồng: Màn shop/reviews/list; form phản hồi gửi đến /shop/reviews/{reviewId}/reply.
     */
    @PostMapping("/{reviewId}/reply")
    public String reply(@PathVariable Integer reviewId,
                        @Valid @ModelAttribute ShopReviewReplyRequest request,
                        BindingResult bindingResult,
                        @RequestParam(required = false) Integer rating,
                        @RequestParam(required = false) Boolean isReported,
                        @RequestParam MultiValueMap<String, String> paginationParams,
                        HttpSession session,
                        RedirectAttributes redirect) {
        String target = paginationRedirect("/shop/reviews", paginationParams, 1, "rating", "isReported");
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

    /**
     * Tác dụng: Đọc page/size từ request, yêu cầu mỗi tham số xuất hiện một lần và là số nguyên trong giới
     * hạn; thiếu thì dùng mặc định, sai thì tạo InvalidPagination.
     * Màn hình / luồng: Hàm nội bộ phục vụ danh sách đánh giá shop sau khi phản hồi.
     */
    private static int paginationNumber(MultiValueMap<String, String> params, String key, int fallback, int min, int max) {
        var values = params.get(key);
        if (values == null) return fallback;
        if (values.size() != 1) throw new InvalidPagination("Tham số " + key + " chỉ được xuất hiện một lần.");
        String raw = values.getFirst();
        if (raw == null || !raw.matches("[0-9]+")) throw invalid(key, min, max);
        try {
            int value = Integer.parseInt(raw);
            if (value < min || value > max) throw invalid(key, min, max);
            return value;
        } catch (NumberFormatException ex) {
            throw invalid(key, min, max);
        }
    }

    /**
     * Tác dụng: Tạo lỗi InvalidPagination kèm khoảng giá trị hợp lệ; paginationNumber gọi khi số trang/kích
     * thước trang sai.
     * Màn hình / luồng: Hàm nội bộ phục vụ danh sách đánh giá shop sau khi phản hồi.
     */
    private static InvalidPagination invalid(String key, int min, int max) {
        return new InvalidPagination("Tham số " + key + " phải là số nguyên từ " + min + " đến " + max + ". Hãy sửa giá trị đã nhập.");
    }

    /**
     * Tác dụng: Tạo URL redirect giữ page, size và các bộ lọc được truyền vào sau thao tác; dùng
     * paginationNumber để kiểm tra tham số trước khi dựng URL.
     * Màn hình / luồng: Hàm nội bộ phục vụ danh sách đánh giá shop sau khi phản hồi.
     */
    private String paginationRedirect(String path, MultiValueMap<String, String> params, int firstPage, String... filters) {
        int page = paginationNumber(params, "page", firstPage, firstPage, Integer.MAX_VALUE);
        int size = paginationNumber(params, "size", 10, 1, 100);
        var uri = org.springframework.web.util.UriComponentsBuilder.fromPath(path)
                .queryParam("page", page).queryParam("size", size);
        for (String filter : filters) {
            String value = params.getFirst(filter);
            if (value != null && !value.isEmpty()) uri.queryParam(filter, value);
        }
        return "redirect:" + uri.build().encode().toUriString();
    }
}
