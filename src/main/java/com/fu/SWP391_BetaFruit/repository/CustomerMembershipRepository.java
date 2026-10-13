package com.fu.SWP391_BetaFruit.repository;

import com.fu.SWP391_BetaFruit.entity.CustomerMembership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerMembershipRepository extends JpaRepository<CustomerMembership, Integer> {
    Optional<CustomerMembership> findByUserId(Integer userId);

    boolean existsByTier_TierId(Integer tierId);
}
