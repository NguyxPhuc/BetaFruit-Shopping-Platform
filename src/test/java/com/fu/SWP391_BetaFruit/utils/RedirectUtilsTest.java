package com.fu.SWP391_BetaFruit.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Kiểm thử tiện ích tạo URL chuyển hướng RedirectUtils")
class RedirectUtilsTest {

    @Test
    @DisplayName("Tạo URL cơ bản chỉ với số trang hợp lệ")
    void buildUserRedirectUrl_WithOnlyPage_ShouldReturnBaseUrlWithPage() {
        String url = RedirectUtils.buildUserRedirectUrl(2, null, null, null);
        assertEquals("redirect:/admin/users?page=2", url);
    }

    @Test
    @DisplayName("Tạo URL với số trang nhỏ hơn 1 -> Tự động đưa về page=1")
    void buildUserRedirectUrl_WithNegativePage_ShouldFallbackToPage1() {
        String url = RedirectUtils.buildUserRedirectUrl(-5, null, null, null);
        assertEquals("redirect:/admin/users?page=1", url);
    }

    @Test
    @DisplayName("Tạo URL với đầy đủ các bộ lọc keyword, roleId và status")
    void buildUserRedirectUrl_WithAllFilters_ShouldReturnFullUrl() {
        String url = RedirectUtils.buildUserRedirectUrl(3, "nguyen", 2L, "ACTIVE");
        assertEquals("redirect:/admin/users?page=3&keyword=nguyen&roleId=2&status=ACTIVE", url);
    }

    @Test
    @DisplayName("Tự động mã hóa URL khi từ khóa có dấu cách hoặc ký tự tiếng Việt")
    void buildUserRedirectUrl_WithSpecialCharacters_ShouldEncodeProperly() {
        String url = RedirectUtils.buildUserRedirectUrl(1, "Hoa quả", 1L, "ACTIVE");
        assertTrue(url.contains("keyword=Hoa%20qu%E1%BA%A3"));
    }

    @Test
    @DisplayName("Bỏ qua status khi status là ALL hoặc rỗng")
    void buildUserRedirectUrl_WithAllStatus_ShouldIgnoreStatusParam() {
        String url1 = RedirectUtils.buildUserRedirectUrl(1, null, null, "ALL");
        String url2 = RedirectUtils.buildUserRedirectUrl(1, null, null, "   ");
        assertEquals("redirect:/admin/users?page=1", url1);
        assertEquals("redirect:/admin/users?page=1", url2);
    }

    @Test
    @DisplayName("Bỏ qua roleId khi roleId <= 0")
    void buildUserRedirectUrl_WithInvalidRoleId_ShouldIgnoreRoleId() {
        String url = RedirectUtils.buildUserRedirectUrl(1, null, 0L, null);
        assertEquals("redirect:/admin/users?page=1", url);
    }
}
