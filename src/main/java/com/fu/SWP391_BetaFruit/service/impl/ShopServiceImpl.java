package com.fu.SWP391_BetaFruit.service.impl;

import com.fu.SWP391_BetaFruit.entity.Shop;
import com.fu.SWP391_BetaFruit.entity.User;
import com.fu.SWP391_BetaFruit.enums.ShopApprovalStatus;
import com.fu.SWP391_BetaFruit.repository.RoleRepository;
import com.fu.SWP391_BetaFruit.repository.ShopRepository;
import com.fu.SWP391_BetaFruit.repository.UserRepository;
import com.fu.SWP391_BetaFruit.service.ShopService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ShopServiceImpl implements ShopService {

    private final ShopRepository shopRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    /**
     * Retrieves all shops in the system.
     *
     * @return a list of all {@link Shop} entities
     */
    @Override
    public List<Shop> getAllShops() {
        return shopRepository.findAll();
    }

    /**
     * Retrieves shops filtered by their approval status.
     *
     * @param status the approval status to filter by (PENDING, APPROVED, REJECTED). If null, returns all shops.
     * @return a list of {@link Shop} entities matching the specified status
     */
    @Override
    public List<Shop> getShopsByStatus(ShopApprovalStatus status) {
        if (status == null) {
            return shopRepository.findAll();
        }
        return shopRepository.findByApprovalStatus(status);
    }

    /**
     * Searches and filters shops by keyword and approval status.
     *
     * @param keyword the search keyword for shop names or details (can be null or empty)
     * @param status  the approval status filter (can be null)
     * @return a list of {@link Shop} entities matching the search criteria
     */
    @Override
    public List<Shop> searchShops(String keyword, ShopApprovalStatus status) {
        boolean hasKeyword = keyword != null && !keyword.trim().isEmpty();
        if (!hasKeyword && status == null) {
            return getAllShops();
        }
        if (!hasKeyword) {
            return getShopsByStatus(status);
        }
        if (status == null) {
            return shopRepository.searchShops(keyword.trim());
        }
        return shopRepository.searchShopsByStatus(keyword.trim(), status);
    }

    /**
     * Retrieves the details of a shop by its ID.
     *
     * @param shopId the ID of the shop to retrieve
     * @return the {@link Shop} entity
     * @throws IllegalArgumentException if no shop is found with the specified ID
     */
    @Override
    public Shop getShopById(Integer shopId) {
        return shopRepository.findById(shopId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy cửa hàng với mã ID: " + shopId));
    }

    /**
     * Approves a shop and assigns the "ShopOwner" role to the shop's owner if not already present.
     *
     * @param shopId the ID of the shop to approve
     */
    @Override
    @Transactional
    public void approveShop(Integer shopId) {
        Shop shop = getShopById(shopId);

        shop.setApprovalStatus(ShopApprovalStatus.APPROVED);

        User owner = shop.getOwner();
        if (owner != null) {
            boolean hasShopOwnerRole = owner.getRoles() != null && owner.getRoles().stream()
                    .anyMatch(r -> "ShopOwner".equalsIgnoreCase(r.getRoleName()));
            if (!hasShopOwnerRole) {
                roleRepository.findByRoleName("ShopOwner").ifPresent(role -> {
                    if (owner.getRoles() == null) {
                        owner.setRoles(new ArrayList<>());
                    }
                    owner.getRoles().add(role);
                    userRepository.save(owner);
                });
            }
        }

        shopRepository.save(shop);
    }

    /**
     * Rejects a shop's approval request.
     *
     * @param shopId the ID of the shop to reject
     */
    @Override
    @Transactional
    public void rejectShop(Integer shopId) {
        Shop shop = getShopById(shopId);
        shop.setApprovalStatus(ShopApprovalStatus.REJECTED);
        shopRepository.save(shop);
    }

    /**
     * Counts the total number of shops filtered by approval status.
     *
     * @param status the approval status to count
     * @return the count of shops with the specified status
     */
    @Override
    public long countByStatus(ShopApprovalStatus status) {
        return shopRepository.countByApprovalStatus(status);
    }

    /**
     * Counts the total number of shops in the system.
     *
     * @return the total number of shops
     */
    @Override
    public long countTotal() {
        return shopRepository.count();
    }

    /**
     * Aggregates page data required for the Admin shop management view,
     * including filtered shop list, active status filter, search keyword, and status statistics counts.
     *
     * @param keyword   the search keyword
     * @param statusStr the string representation of the approval status filter (ALL, PENDING, APPROVED, REJECTED)
     * @return a map containing shops list, current status string, keyword, and counts for total, pending, approved, and rejected shops
     */
    @Override
    public Map<String, Object> getAdminShopPageData(String keyword, String statusStr) {
        ShopApprovalStatus filterStatus = null;
        if (statusStr != null && !statusStr.trim().isEmpty() && !"ALL".equalsIgnoreCase(statusStr.trim())) {
            try {
                filterStatus = ShopApprovalStatus.valueOf(statusStr.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
            }
        }

        String trimmedKeyword = keyword != null ? keyword.trim() : "";
        List<Shop> shops = searchShops(trimmedKeyword, filterStatus);

        Map<String, Object> data = new HashMap<>();
        data.put("shops", shops);
        data.put("currentStatus", filterStatus != null ? filterStatus.name() : "ALL");
        data.put("keyword", trimmedKeyword);
        data.put("totalCount", countTotal());
        data.put("pendingCount", countByStatus(ShopApprovalStatus.PENDING));
        data.put("approvedCount", countByStatus(ShopApprovalStatus.APPROVED));
        data.put("rejectedCount", countByStatus(ShopApprovalStatus.REJECTED));

        return data;
    }
}
