package com.fu.SWP391_BetaFruit.repository;

import com.fu.SWP391_BetaFruit.entity.ReviewReport;
import com.fu.SWP391_BetaFruit.enums.DisputeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewReportRepository extends JpaRepository<ReviewReport, Integer> {

    Page<ReviewReport> findByStatus(DisputeStatus status, Pageable pageable);

    long countByStatus(DisputeStatus status);

    boolean existsByReview_ReviewIdAndStatusIn(Integer reviewId, Collection<DisputeStatus> statuses);

    Optional<ReviewReport> findTopByReview_ReviewIdOrderByCreatedAtDesc(Integer reviewId);

    @Query("SELECT rr FROM ReviewReport rr WHERE rr.shop.shopId = :shopId ORDER BY rr.createdAt DESC")
    Page<ReviewReport> findByShopId(@Param("shopId") Integer shopId, Pageable pageable);
}
