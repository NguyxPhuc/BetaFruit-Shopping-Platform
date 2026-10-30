package com.fu.SWP391_BetaFruit.repository;

import com.fu.SWP391_BetaFruit.entity.CODCollection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CODCollectionRepository extends JpaRepository<CODCollection, Integer> {

    @EntityGraph(attributePaths = {"shipper.user", "collectedBy"})
    Page<CODCollection> findAllByOrderByCreatedAtDescCollectionIdDesc(Pageable pageable);

    @EntityGraph(attributePaths = {"shipper.user", "collectedBy"})
    List<CODCollection> findByShipper_UserIdOrderByCreatedAtDescCollectionIdDesc(Integer shipperId);

    @EntityGraph(attributePaths = {"shipper.user", "collectedBy"})
    @Query("""
            select c from CODCollection c
            where (:shipperId is null or c.shipper.userId = :shipperId)
            order by c.createdAt desc, c.collectionId desc
            """)
    Page<CODCollection> searchCollections(@Param("shipperId") Integer shipperId, Pageable pageable);
}
