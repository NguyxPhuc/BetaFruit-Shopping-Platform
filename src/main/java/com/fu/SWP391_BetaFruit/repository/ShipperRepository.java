package com.fu.SWP391_BetaFruit.repository;

import com.fu.SWP391_BetaFruit.entity.Shipper;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ShipperRepository extends JpaRepository<Shipper, Integer> {

    @EntityGraph(attributePaths = {"user"})
    @Query("select s from Shipper s order by s.user.fullName asc")
    List<Shipper> findAllWithUser();

    @EntityGraph(attributePaths = {"user"})
    @Query("select s from Shipper s where s.codDebt > 0 order by s.codDebt desc")
    List<Shipper> findAllWithCodDebt();
}
