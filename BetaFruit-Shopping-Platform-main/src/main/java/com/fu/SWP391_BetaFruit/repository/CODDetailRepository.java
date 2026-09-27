package com.fu.SWP391_BetaFruit.repository;

import com.fu.SWP391_BetaFruit.entity.CODDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CODDetailRepository extends JpaRepository<CODDetail, Integer> {
    List<CODDetail> findByCollectionCollectionIdOrderByDetailIdAsc(Integer collectionId);

    Optional<CODDetail> findByOrderOrderId(Integer orderId);
}
