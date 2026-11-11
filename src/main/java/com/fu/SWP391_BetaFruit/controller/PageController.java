package com.fu.SWP391_BetaFruit.controller;

import com.fu.SWP391_BetaFruit.dto.response.PublicShopResponse;
import com.fu.SWP391_BetaFruit.service.CustomerProductService;
import com.fu.SWP391_BetaFruit.service.ShopService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class PageController {

    private final CustomerProductService customerProductService;
    private final ShopService shopService;

    @GetMapping("/403")
    public String accessDenied() {
        return "error/403";
    }

    @GetMapping({"/", "/home"})
    public String homePage(Model model) {
        model.addAttribute("featuredProducts", customerProductService.getFeaturedProducts(8));
        model.addAttribute("homeCategories", customerProductService.getAllActiveCategories().stream().limit(6).toList());
        return "public/index";
    }

    @GetMapping("/404")
    public String notFound() {
        return "error/404";
    }

    /**
     * Hiển thị trang danh sách các gian hàng công khai (Public Shops).
     * Ánh xạ các đường dẫn: /public/shop, /public/shops, /shops
     */
    @GetMapping({"/public/shop", "/public/shops", "/shops"})
    public String getAllPublicShops(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "9") int size,
            Model model
    ) {
        int pageIndex = Math.max(0, page - 1);
        int pageSize = size > 0 ? size : 9;
        Pageable pageable = PageRequest.of(pageIndex, pageSize);

        Page<PublicShopResponse> shopPage = shopService.getAllPublicShops(pageable);

        model.addAttribute("shopPage", shopPage);
        model.addAttribute("shops", shopPage.getContent());
        model.addAttribute("totalShops", shopPage.getTotalElements());

        return "public/shop";
    }
}
