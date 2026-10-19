package com.fu.SWP391_BetaFruit.repository;

import com.fu.SWP391_BetaFruit.entity.Role;
import com.fu.SWP391_BetaFruit.enums.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    // Compare converted names so legacy SELLER rows also match SHOP_OWNER.
    // The role table contains only the application's small set of system roles.
    default boolean existsByRoleName(RoleName roleName) {
        return findAll().stream().anyMatch(role -> role.getRoleName() == roleName);
    }

    default Optional<Role> findByRoleName(RoleName roleName) {
        return findAll().stream().filter(role -> role.getRoleName() == roleName)
                .min(java.util.Comparator.comparing(Role::getRoleId));
    }
}
