package com.fu.SWP391_BetaFruit.controller;

import com.fu.SWP391_BetaFruit.dto.request.ProductFilterRequest;
import com.fu.SWP391_BetaFruit.dto.response.CustomerProductDetailResponse;
import com.fu.SWP391_BetaFruit.dto.response.CustomerProductPageResponse;
import com.fu.SWP391_BetaFruit.service.CustomerProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;

@Controller
@RequiredArgsConstructor
public class CustomerProductController {

    private final CustomerProductService customerProductService;

    @GetMapping("/products")
    public String viewProductList(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Integer rating,
            @RequestParam(required = false) String availability,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "6") int size,
            Model model
    ) {
        String effectiveKeyword = (keyword != null && !keyword.trim().isEmpty())
                ? keyword.trim()
                : (search != null ? search.trim() : null);

        ProductFilterRequest filter = ProductFilterRequest.builder()
                .keyword(effectiveKeyword)
                .categoryId(categoryId)
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .rating(rating)
                .availability(availability)
                .sortBy(sortBy)
                .page(page)
                .size(size)
                .build();

        CustomerProductPageResponse pageResponse;
        try {
            pageResponse = customerProductService.getProductsPage(filter);
        } catch (IllegalArgumentException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            filter.setMinPrice(null);
            filter.setMaxPrice(null);
            filter.setRating(null);
            pageResponse = customerProductService.getProductsPage(filter);
        }
        model.addAttribute("pageData", pageResponse);
        model.addAttribute("filter", filter);

        return "customer/products";
    }

    @GetMapping("/products/{productId}")
    public String viewProductDetails(@PathVariable Integer productId, Model model, jakarta.servlet.http.HttpSession session) {
        CustomerProductDetailResponse product = customerProductService.getProductDetail(productId);
        if (product == null) {
            return "redirect:/products";
        }

        model.addAttribute("product", product);
        model.addAttribute("cartToken", CartFormToken.get(session));
        return "customer/product-detail";
    }

    @GetMapping("/search")
    public String searchRedirect(@RequestParam(required = false) String keyword,
                                 @RequestParam(required = false) String q) {
        String query = (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : (q != null ? q.trim() : "");
        return "redirect:" + org.springframework.web.util.UriComponentsBuilder.fromPath("/products")
                .queryParam("keyword", "{keyword}").encode().buildAndExpand(query).toUriString();
    }

    @org.springframework.web.bind.annotation.ExceptionHandler({
            org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class})
    public String invalidFilter(org.springframework.web.servlet.mvc.support.RedirectAttributes redirect) {
        redirect.addFlashAttribute("errorMessage", "Bộ lọc có giá trị không hợp lệ. Vui lòng nhập lại.");
        return "redirect:/products";
    }
}
