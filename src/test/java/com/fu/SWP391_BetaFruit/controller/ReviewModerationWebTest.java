package com.fu.SWP391_BetaFruit.controller;

import com.fu.SWP391_BetaFruit.dto.response.NotificationResponse;
import com.fu.SWP391_BetaFruit.dto.response.ShopProfileResponse;
import com.fu.SWP391_BetaFruit.entity.*;
import com.fu.SWP391_BetaFruit.enums.*;
import com.fu.SWP391_BetaFruit.interceptor.*;
import com.fu.SWP391_BetaFruit.service.*;
import org.junit.jupiter.api.*;
import org.springframework.context.annotation.*;
import org.springframework.mock.web.*;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.*;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ReviewModerationWebTest {
    AnnotationConfigWebApplicationContext context;
    MockMvc mvc;
    ReviewReportService reports;
    NotificationService notifications;
    ReviewReport report;
    Review review;

    @BeforeEach
    void setUp() {
        context = new AnnotationConfigWebApplicationContext();
        context.setServletContext(new MockServletContext());
        context.register(Config.class);
        context.refresh();
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
        reports = context.getBean(ReviewReportService.class);
        notifications = context.getBean(NotificationService.class);
        User customer = new User();
        customer.setFullName("Khách hàng");
        Shop shop = new Shop();
        shop.setShopName("Shop Trái cây");
        OrderItem item = new OrderItem();
        item.setSnapshotProductName("Cam tươi");
        review = new Review();
        review.setReviewId(7);
        review.setCustomer(customer);
        review.setOrderItem(item);
        review.setRating(1);
        review.setComment("Nội dung cần kiểm tra <script>alert(1)</script>");
        review.setVisibilityStatus(ReviewVisibility.VISIBLE);
        review.setCreatedAt(LocalDateTime.of(2026, 11, 3, 10, 0));
        report = new ReviewReport();
        report.setReportId(8);
        report.setReview(review);
        report.setShop(shop);
        report.setStatus(DisputeStatus.PENDING);
        report.setReasonCategory(ReportReason.FALSE_INFORMATION);
        report.setExplanation("Giải trình của cửa hàng.");
        report.setEvidenceUrls("https://example.com/evidence");
        report.setCreatedAt(review.getCreatedAt());
    }

    @AfterEach
    void close() { context.close(); }

    MockHttpSession session(RoleName role) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("LOGGED_IN_USER_ID", 10);
        session.setAttribute("LOGGED_IN_ROLES", List.of(role));
        session.setAttribute("LOGGED_IN_FULLNAME", "Người dùng");
        session.setAttribute("SHOP_APPROVAL_STATUS", ShopApprovalStatus.APPROVED);
        return session;
    }

    Map<String, Object> pageData() {
        Map<String, Object> data = new HashMap<>();
        data.put("reports", List.of(report));
        data.put("currentStatus", "PENDING");
        data.put("currentPage", 1);
        data.put("pageSize", 10);
        data.put("totalPages", 2);
        data.put("totalItems", 11);
        data.put("pendingCount", 1);
        data.put("underReviewCount", 0);
        data.put("approvedCount", 0);
        data.put("rejectedCount", 0);
        data.put("totalCount", 1);
        return data;
    }

    @Test
    void adminPageRendersWorkingActionsAndEscapesReviewContent() throws Exception {
        when(reports.getStaffReportPageData("PENDING", 1, 10)).thenReturn(pageData());
        String html = mvc.perform(get("/admin/reviews").session(session(RoleName.ADMIN)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertTrue(html.contains("/admin/reviews/8/resolve"));
        assertTrue(html.contains("status=UNDER_REVIEW"));
        assertTrue(html.contains("page=2"));
        assertTrue(html.contains("https://example.com/evidence"));
        assertFalse(html.contains("<script>alert(1)</script>"));
        assertFalse(html.contains("/staff/reviews/8/resolve"));
    }

    @Test
    void staffPageKeepsStaffActions() throws Exception {
        when(reports.getStaffReportPageData("PENDING", 1, 10)).thenReturn(pageData());
        String html = mvc.perform(get("/staff/reviews").session(session(RoleName.STAFF)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertTrue(html.contains("/staff/reviews/8/resolve"));
    }

    @Test
    void completedReportHasNoResolutionFormAndLegacyUnsafeLinkIsNotRendered() throws Exception {
        report.setStatus(DisputeStatus.APPROVED_HIDDEN);
        report.setEvidenceUrls("javascript:alert(1)");
        when(reports.getStaffReportPageData("PENDING", 1, 10)).thenReturn(pageData());
        String html = mvc.perform(get("/admin/reviews").session(session(RoleName.ADMIN)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertFalse(html.contains("/admin/reviews/8/resolve"));
        assertFalse(html.contains("javascript:"));
    }

    @Test
    void adminPageRequiresAdminRoleForReadsAndWrites() throws Exception {
        mvc.perform(get("/admin/reviews")).andExpect(redirectedUrl("/auth/login"));
        mvc.perform(get("/admin/reviews").session(session(RoleName.SHOP_OWNER)))
                .andExpect(redirectedUrl("/403"));
        mvc.perform(post("/admin/reviews/8/resolve").param("action", "APPROVE_HIDE")
                .session(session(RoleName.CUSTOMER))).andExpect(redirectedUrl("/403"));
        verifyNoInteractions(reports);
    }

    @Test
    void adminResolvesUsingAuthenticatedIdentity() throws Exception {
        mvc.perform(post("/admin/reviews/8/resolve").session(session(RoleName.ADMIN))
                        .param("action", "APPROVE_HIDE").param("staffNote", "Policy violation"))
                .andExpect(redirectedUrl("/admin/reviews"))
                .andExpect(flash().attributeExists("successMessage"));
        verify(reports).resolveReport(8, "APPROVE_HIDE", "Policy violation", 10);
    }

    @Test
    void shopReviewsRenderReportFormAndLiveNotificationBell() throws Exception {
        Map<String, Object> data = new HashMap<>();
        data.put("reviews", List.of(review));
        data.put("reasons", ReportReason.values());
        data.put("currentPage", 1);
        data.put("pageSize", 10);
        data.put("totalPages", 1);
        data.put("totalItems", 1);
        when(reports.getShopReviewsPageData(10, null, null, 1, 10)).thenReturn(data);
        when(context.getBean(ShopService.class).getShopProfile(10)).thenReturn(new ShopProfileResponse());
        String html = mvc.perform(get("/shop/reviews").session(session(RoleName.SHOP_OWNER)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertTrue(html.contains("/shop/reviews/7/report"));
        assertTrue(html.contains("data-feed-url=\"/notifications/feed\""));
        assertTrue(html.contains("/js/shop-notifications.js"));
        assertFalse(html.contains("<script>alert(1)</script>"));
    }

    @Test
    void reportingUsesSessionOwnerAndRejectsInvalidForm() throws Exception {
        mvc.perform(post("/shop/reviews/7/report").session(session(RoleName.SHOP_OWNER))
                        .param("reasonCategory", "FALSE_INFORMATION").param("explanation", "False review information"))
                .andExpect(redirectedUrl("/shop/reviews")).andExpect(flash().attributeExists("successMessage"));
        verify(reports).createReviewReport(eq(10), eq(7), any());
        clearInvocations(reports);
        mvc.perform(post("/shop/reviews/7/report").session(session(RoleName.SHOP_OWNER))
                        .param("reasonCategory", "FALSE_INFORMATION").param("explanation", "short"))
                .andExpect(flash().attributeExists("errorMessage"));
        verifyNoInteractions(reports);
    }

    @Test
    void notificationFeedRequiresLoginAndOnlyUsesSessionUser() throws Exception {
        mvc.perform(get("/notifications/feed")).andExpect(status().isUnauthorized());
        when(notifications.countUnreadNotifications(10)).thenReturn(1);
        when(notifications.getRecentNotifications(10)).thenReturn(List.of(new NotificationResponse(
                12, "Đơn mới #99", "Chuẩn bị hàng", null, false, "/shop/orders/99")));
        mvc.perform(get("/notifications/feed?userId=999").session(session(RoleName.SHOP_OWNER)))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.unreadCount").value(1))
                .andExpect(jsonPath("$.notifications[0].notificationId").value(12));
        verify(notifications, never()).getRecentNotifications(999);
        verify(notifications, times(1)).countUnreadNotifications(10);
        verify(notifications, never()).getAllNotifications(anyInt());
        verifyNoInteractions(context.getBean(CartService.class));
    }

    @Test
    void readNotificationDoesNotRedirectToExternalSite() throws Exception {
        mvc.perform(post("/notifications/read/12").session(session(RoleName.SHOP_OWNER))
                        .param("redirect", "//evil.example") .header("Referer", "https://evil.example"))
                .andExpect(redirectedUrl("/"));
        verify(notifications).markAsRead(10, 12);
        mvc.perform(post("/notifications/read/12").session(session(RoleName.SHOP_OWNER))
                        .param("redirect", "/shop/orders/99"))
                .andExpect(redirectedUrl("/shop/orders/99"));
    }

    @Configuration
    @EnableWebMvc
    static class Config implements WebMvcConfigurer {
        @Bean ReviewReportService reports() { return mock(ReviewReportService.class); }
        @Bean NotificationService notifications() { return mock(NotificationService.class); }
        @Bean ShopService shops() { return mock(ShopService.class); }
        @Bean ShopAnalyticsService shopAnalyticsService() { return mock(ShopAnalyticsService.class); }
        @Bean AdminReviewController admin() { return new AdminReviewController(reports()); }
        @Bean ShopController shop() { return new ShopController(); }
        @Bean StaffController staff() { return new StaffController(shops(), products(), reports()); }
        @Bean NotificationController notificationController() { return new NotificationController(); }
        @Bean GlobalControllerAdvice advice() { return new GlobalControllerAdvice(); }
        @Bean CartService carts() { return mock(CartService.class); }
        @Bean UserService users() { return mock(UserService.class); }
        @Bean ProductService products() { return mock(ProductService.class); }
        @Bean ThymeleafViewResolver viewResolver() {
            ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
            resolver.setPrefix("templates/");
            resolver.setSuffix(".html");
            resolver.setCharacterEncoding("UTF-8");
            SpringTemplateEngine engine = new SpringTemplateEngine();
            engine.setTemplateResolver(resolver);
            ThymeleafViewResolver views = new ThymeleafViewResolver();
            views.setTemplateEngine(engine);
            views.setCharacterEncoding("UTF-8");
            return views;
        }
        @Override
        public void addInterceptors(InterceptorRegistry registry) {
            registry.addInterceptor(new AdminInterceptor()).addPathPatterns("/admin/**");
            registry.addInterceptor(new StaffInterceptor()).addPathPatterns("/staff/**");
            registry.addInterceptor(new ShopOwnerInterceptor()).addPathPatterns("/shop/**");
        }
    }
}
