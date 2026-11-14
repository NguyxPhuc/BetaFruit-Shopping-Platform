package com.fu.SWP391_BetaFruit.utils;

import org.springframework.web.util.UriComponentsBuilder;

public final class RedirectUtils {

    private RedirectUtils() {
        // Prevent instantiation
    }

    /**
     * Builds a safe redirect URL for the Admin user management page,
     * preserving pagination and active filter parameters with proper URL encoding.
     *
     * @param page    the current page number (1-based)
     * @param keyword the search keyword (optional)
     * @param roleId  the filtered role ID (optional)
     * @param status  the filtered user status (optional)
     * @return the encoded redirect URL string prefixed with "redirect:"
     */
    public static String buildUserRedirectUrl(int page, String keyword, Long roleId, String status) {
        return buildUserRedirectUrl(page, keyword, roleId, status, null);
    }

    /**
     * Builds a safe redirect URL for the Admin user management page with tier filtering,
     * preserving pagination and active filter parameters with proper URL encoding.
     *
     * @param page    the current page number (1-based)
     * @param keyword the search keyword (optional)
     * @param roleId  the filtered role ID (optional)
     * @param status  the filtered user status (optional)
     * @param tierId  the filtered tier ID (optional, e.g. > 0 or -1 for unranked)
     * @return the encoded redirect URL string prefixed with "redirect:"
     */
    public static String buildUserRedirectUrl(int page, String keyword, Long roleId, String status, Integer tierId) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/admin/users")
                .queryParam("page", Math.max(1, page));

        String cleanKeyword = StringUtils.sanitizeKeyword(keyword);
        if (cleanKeyword != null) {
            builder.queryParam("keyword", cleanKeyword);
        }
        if (roleId != null && roleId > 0) {
            builder.queryParam("roleId", roleId);
        }
        if (status != null && !status.trim().isEmpty() && !"ALL".equalsIgnoreCase(status.trim())) {
            builder.queryParam("status", status.trim());
        }
        if (tierId != null && (tierId > 0 || tierId == -1)) {
            builder.queryParam("tierId", tierId);
        }

        return "redirect:" + builder.build().encode().toUriString();
    }
}
