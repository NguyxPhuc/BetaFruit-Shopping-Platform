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

    @Override
    public List<Shop> getAllShops() {
        return shopRepository.findAll();
    }

    @Override
    public List<Shop> getShopsByStatus(ShopApprovalStatus status) {
        if (status == null) {
            return shopRepository.findAll();
        }
        return shopRepository.findByApprovalStatus(status);
    }

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

    @Override
    public Shop getShopById(Integer shopId) {
        return shopRepository.findById(shopId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy cửa hàng với mã ID: " + shopId));
    }

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

    @Override
    @Transactional
    public void rejectShop(Integer shopId) {
        Shop shop = getShopById(shopId);
        shop.setApprovalStatus(ShopApprovalStatus.REJECTED);
        shopRepository.save(shop);
    }

    @Override
    public long countByStatus(ShopApprovalStatus status) {
        return shopRepository.countByApprovalStatus(status);
    }

    @Override
    public long countTotal() {
        return shopRepository.count();
    }

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
