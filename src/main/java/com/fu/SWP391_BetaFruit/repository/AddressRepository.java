package com.fu.SWP391_BetaFruit.repository;

import com.fu.SWP391_BetaFruit.entity.Address;
import com.fu.SWP391_BetaFruit.enums.AddressType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface AddressRepository extends JpaRepository<Address,Integer> {
    int countByUser_UserIdAndAddressType(Integer userId, AddressType addressType);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("Update Address a Set a.isDefault = false Where a.user.userId = :userId And a.addressType = :type And a.isDefault = true")
    void clearDefaultAddress(@Param("userId") Integer userId, @Param("type") AddressType type);

    List<Address> getAddressesByUser_UserIdAndIsActiveAndAddressTypeIn(Integer userUserId, Boolean isActive, Collection<AddressType> addressTypes);

    Optional<Address> findByUser_UserIdAndAddressIdAndIsActive(Integer userId, Integer addressId, Boolean isActive);

    List<Address> findByUser_UserIdAndAddressTypeAndIsActiveTrue(Integer userId, AddressType addressType);
}
