package com.fu.SWP391_BetaFruit.repository;

import com.fu.SWP391_BetaFruit.entity.User;
import com.fu.SWP391_BetaFruit.enums.RoleName;
import com.fu.SWP391_BetaFruit.enums.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.userId = :id")
    Optional<User> findCartCustomerForUpdate(@Param("id") Integer id);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    @Query(value = "SELECT DISTINCT u FROM User u LEFT JOIN u.roles r LEFT JOIN u.customerMembership cm WHERE " +
            "(:status IS NULL OR u.status = :status) AND " +
            "(:roleId IS NULL OR r.roleId = :roleId) AND " +
            "(:tierId IS NULL OR (:tierId = -1 AND (cm IS NULL OR cm.tier IS NULL)) OR (cm.tier.tierId = :tierId)) AND " +
            "(:keyword IS NULL OR :keyword = '' OR " +
            "LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "(u.phone IS NOT NULL AND LOWER(u.phone) LIKE LOWER(CONCAT('%', :keyword, '%'))))",
            countQuery = "SELECT COUNT(DISTINCT u) FROM User u LEFT JOIN u.roles r LEFT JOIN u.customerMembership cm WHERE " +
                    "(:status IS NULL OR u.status = :status) AND " +
                    "(:roleId IS NULL OR r.roleId = :roleId) AND " +
                    "(:tierId IS NULL OR (:tierId = -1 AND (cm IS NULL OR cm.tier IS NULL)) OR (cm.tier.tierId = :tierId)) AND " +
                    "(:keyword IS NULL OR :keyword = '' OR " +
                    "LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                    "LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                    "LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                    "(u.phone IS NOT NULL AND LOWER(u.phone) LIKE LOWER(CONCAT('%', :keyword, '%'))))")
    Page<User> filterUsers(@Param("keyword") String keyword,
            @Param("roleId") Long roleId,
            @Param("status") UserStatus status,
            @Param("tierId") Integer tierId,
            Pageable pageable);

    default Page<User> filterUsers(String keyword, Long roleId, UserStatus status, Pageable pageable) {
        return filterUsers(keyword, roleId, status, null, pageable);
    }

    long countByStatus(UserStatus status);

    @Query("SELECT COUNT(DISTINCT u) FROM User u JOIN u.roles r WHERE r.roleName = :roleName")
    long countByRoleName(@Param("roleName") RoleName roleName);

    @Query("SELECT DISTINCT u FROM User u JOIN u.roles r WHERE r.roleName = :roleName AND u.status = :status")
    List<User> findUsersByRoleNameAndStatus(@Param("roleName") RoleName roleName, @Param("status") UserStatus status);
}
