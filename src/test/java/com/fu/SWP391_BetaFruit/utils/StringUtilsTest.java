package com.fu.SWP391_BetaFruit.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Kiểm thử tiện ích xử lý chuỗi StringUtils")
class StringUtilsTest {

    @Test
    @DisplayName("sanitizeKeyword trả về null khi đầu vào là null")
    void sanitizeKeyword_WhenNull_ShouldReturnNull() {
        assertNull(StringUtils.sanitizeKeyword(null));
    }

    @Test
    @DisplayName("sanitizeKeyword trả về null khi đầu vào là chuỗi rỗng hoặc chỉ có khoảng trắng")
    void sanitizeKeyword_WhenEmptyOrWhitespace_ShouldReturnNull() {
        assertNull(StringUtils.sanitizeKeyword(""));
        assertNull(StringUtils.sanitizeKeyword("   "));
        assertNull(StringUtils.sanitizeKeyword("\t\n"));
    }

    @Test
    @DisplayName("sanitizeKeyword cắt bỏ khoảng trắng thừa đầu cuối của chuỗi hợp lệ")
    void sanitizeKeyword_WhenValid_ShouldTrimWhitespace() {
        assertEquals("Trái cây tươi", StringUtils.sanitizeKeyword("  Trái cây tươi  "));
    }

    @Test
    @DisplayName("sanitizeKeyword cắt ngắn chuỗi xuống 100 ký tự nếu vượt quá 100 ký tự")
    void sanitizeKeyword_WhenLengthExceeds100_ShouldTruncateTo100Chars() {
        String input = "a".repeat(120);
        String result = StringUtils.sanitizeKeyword(input);
        assertNotNull(result);
        assertEquals(100, result.length());
        assertEquals("a".repeat(100), result);
    }
}
