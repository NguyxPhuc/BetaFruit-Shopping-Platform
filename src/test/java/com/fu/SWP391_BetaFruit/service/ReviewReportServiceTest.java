package com.fu.SWP391_BetaFruit.service;

import com.fu.SWP391_BetaFruit.dto.request.ReviewReportRequest;
import com.fu.SWP391_BetaFruit.entity.*;
import com.fu.SWP391_BetaFruit.enums.*;
import com.fu.SWP391_BetaFruit.repository.*;
import com.fu.SWP391_BetaFruit.service.impl.ReviewReportServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewReportServiceTest {
    @Mock ReviewReportRepository reports;
    @Mock ReviewRepository reviews;
    @Mock ShopRepository shops;
    @Mock UserRepository users;
    @InjectMocks ReviewReportServiceImpl service;
    Review review;
    Shop shop;
    ReviewReport report;
    ReviewReportRequest request;

    @BeforeEach
    void setUp() {
        shop = new Shop();
        shop.setShopId(1);
        Product product = new Product();
        product.setShop(shop);
        ProductVariant variant = new ProductVariant();
        variant.setProduct(product);
        OrderItem item = new OrderItem();
        item.setVariant(variant);
        review = new Review();
        review.setReviewId(7);
        review.setOrderItem(item);
        review.setVisibilityStatus(ReviewVisibility.VISIBLE);
        review.setIsReported(false);
        report = new ReviewReport();
        report.setReview(review);
        report.setReportId(8);
        report.setStatus(DisputeStatus.PENDING);
        request = new ReviewReportRequest(ReportReason.FALSE_INFORMATION,
                "  This review contains false information.  ", "https://example.com/evidence");
    }

    private void reporting() {
        when(shops.findByOwner_UserId(10)).thenReturn(Optional.of(shop));
        when(reviews.findLockedById(7)).thenReturn(Optional.of(review));
    }

    private User moderator(RoleName role) {
        User user = new User();
        user.setUserId(20);
        user.setRoles(List.of(new Role(1L, role)));
        when(users.findById(20)).thenReturn(Optional.of(user));
        return user;
    }

    private void resolution() {
        when(reports.findReviewIdByReportId(8)).thenReturn(Optional.of(7));
        when(reviews.findLockedById(7)).thenReturn(Optional.of(review));
        when(reports.findById(8)).thenReturn(Optional.of(report));
    }

    @Test
    void ownerCanReportWithoutImmediatelyHidingReview() {
        reporting();
        service.createReviewReport(10, 7, request);
        ArgumentCaptor<ReviewReport> saved = ArgumentCaptor.forClass(ReviewReport.class);
        verify(reports).save(saved.capture());
        assertSame(shop, saved.getValue().getShop());
        assertEquals(DisputeStatus.PENDING, saved.getValue().getStatus());
        assertEquals(request.getExplanation().trim(), saved.getValue().getExplanation());
        assertTrue(review.getIsReported());
        assertEquals(ReviewVisibility.VISIBLE, review.getVisibilityStatus());
    }

    @Test
    void cannotReportAnotherShopsReview() {
        reporting();
        Shop anotherShop = new Shop();
        anotherShop.setShopId(2);
        review.getOrderItem().getVariant().getProduct().setShop(anotherShop);
        assertThrows(SecurityException.class, () -> service.createReviewReport(10, 7, request));
        verify(reports, never()).save(any());
    }

    @Test
    void cannotCreateDuplicatePendingReport() {
        reporting();
        when(reports.existsByReview_ReviewIdAndStatusIn(eq(7), anyCollection())).thenReturn(true);
        assertThrows(IllegalStateException.class, () -> service.createReviewReport(10, 7, request));
        verify(reports, never()).save(any());
    }

    @Test
    void cannotReportAlreadyHiddenReview() {
        reporting();
        review.setVisibilityStatus(ReviewVisibility.HIDDEN_BY_ADMIN);
        assertThrows(IllegalStateException.class, () -> service.createReviewReport(10, 7, request));
    }

    @Test
    void rejectsExecutableEvidenceUrls() {
        reporting();
        request.setEvidenceUrls("javascript:alert(1)");
        assertThrows(IllegalArgumentException.class, () -> service.createReviewReport(10, 7, request));
        verify(reports, never()).save(any());
    }

    @Test
    void rejectsExplanationContainingOnlyWhitespace() {
        reporting();
        request.setExplanation("                    ");
        assertThrows(IllegalArgumentException.class, () -> service.createReviewReport(10, 7, request));
    }

    @Test
    void adminCanHideReviewAndAuditDecision() {
        User admin = moderator(RoleName.ADMIN);
        resolution();
        service.resolveReport(8, "APPROVE_HIDE", "  Policy violation  ", 20);
        assertEquals(ReviewVisibility.HIDDEN_BY_ADMIN, review.getVisibilityStatus());
        assertEquals(DisputeStatus.APPROVED_HIDDEN, report.getStatus());
        assertSame(admin, report.getProcessedBy());
        assertNotNull(report.getResolvedAt());
        assertEquals("Policy violation", report.getStaffNote());
        verify(reviews).save(review);
        verify(reports).save(report);
    }

    @Test
    void existingStaffCanStillResolveAnUnderReviewReport() {
        moderator(RoleName.STAFF);
        resolution();
        report.setStatus(DisputeStatus.UNDER_REVIEW);
        service.resolveReport(8, "REJECT", null, 20);
        assertEquals(DisputeStatus.REJECTED, report.getStatus());
        assertEquals(ReviewVisibility.VISIBLE, review.getVisibilityStatus());
        assertFalse(review.getIsReported());
    }

    @Test
    void cannotRewriteCompletedDecision() {
        moderator(RoleName.ADMIN);
        resolution();
        report.setStatus(DisputeStatus.APPROVED_HIDDEN);
        review.setVisibilityStatus(ReviewVisibility.HIDDEN_BY_ADMIN);
        assertThrows(IllegalStateException.class, () -> service.resolveReport(8, "REJECT", null, 20));
        assertEquals(DisputeStatus.APPROVED_HIDDEN, report.getStatus());
        verify(reports, never()).save(any());
    }

    @Test
    void shopOwnerCannotResolveReports() {
        moderator(RoleName.SHOP_OWNER);
        assertThrows(SecurityException.class, () -> service.resolveReport(8, "APPROVE_HIDE", null, 20));
        verifyNoInteractions(reports, reviews);
    }

    @Test
    void unknownActionDoesNotSilentlyRejectReport() {
        moderator(RoleName.ADMIN);
        assertThrows(IllegalArgumentException.class, () -> service.resolveReport(8, "TYPO", null, 20));
        verifyNoInteractions(reports, reviews);
    }

    @Test
    void rejectedFilterIncludesWarningsAndClampsPagination() {
        when(reports.findByStatusIn(anyCollection(), any(Pageable.class))).thenReturn(Page.empty());
        var data = service.getStaffReportPageData("REJECTED", -1, 0);
        ArgumentCaptor<Pageable> page = ArgumentCaptor.forClass(Pageable.class);
        verify(reports).findByStatusIn(eq(List.of(DisputeStatus.REJECTED, DisputeStatus.REJECTED_WITH_WARNING)), page.capture());
        assertEquals(0, page.getValue().getPageNumber());
        assertEquals(1, page.getValue().getPageSize());
        assertEquals(1, data.get("currentPage"));
    }
}
