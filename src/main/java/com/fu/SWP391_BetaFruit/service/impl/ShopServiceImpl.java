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
import java.util.List;

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

        // Tự động cấp vai trò ShopOwner cho chủ cửa hàng nếu chưa có
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
}
