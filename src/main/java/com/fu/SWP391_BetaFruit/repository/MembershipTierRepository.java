package com.fu.SWP391_BetaFruit.repository;

import com.fu.SWP391_BetaFruit.entity.MembershipTier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MembershipTierRepository extends JpaRepository<MembershipTier,Integer> {
    MembershipTier findTopByOrderByMinSpentRequiredAsc();
}
