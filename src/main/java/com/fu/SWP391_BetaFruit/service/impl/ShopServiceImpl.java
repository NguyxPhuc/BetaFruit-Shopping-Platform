package com.fu.SWP391_BetaFruit.service.impl;

import com.fu.SWP391_BetaFruit.dto.request.ShopProfileRequest;
import com.fu.SWP391_BetaFruit.dto.response.ShopProfileResponse;
import com.fu.SWP391_BetaFruit.entity.Shop;
import com.fu.SWP391_BetaFruit.entity.User;
import com.fu.SWP391_BetaFruit.enums.RoleName;
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
                    .anyMatch(r -> r.getRoleName() == RoleName.SHOP_OWNER);
            if (!hasShopOwnerRole) {
                roleRepository.findByRoleName(RoleName.SHOP_OWNER).ifPresent(role -> {
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

    @Override
    public ShopProfileResponse getShopProfile(Integer id) throws Exception {
        ShopProfileResponse shopProfileResponse = new ShopProfileResponse();
        User user = userRepository.findById(id)
                .orElseThrow(() -> new Exception("Lỗi hệ thống: không tìm thấy thông tin người dùng."));

        shopProfileResponse.setEmail(user.getEmail());
        shopProfileResponse.setPhone(user.getPhone());

        Shop shop = shopRepository.findByOwner(user)
                .orElseThrow(() -> new Exception("Lỗi hệ thống: không tìm thấy thông tin shop."));

        shopProfileResponse.setShopName(shop.getShopName());
        shopProfileResponse.setShopDescription(shop.getShopDescription());
        shopProfileResponse.setBankName(shop.getBankName());
        shopProfileResponse.setBankAccountNumber(shop.getBankAccountNumber());
        shopProfileResponse.setAccountName(shop.getAccountName());
        return shopProfileResponse;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void editShopProfile(Integer id, ShopProfileRequest shopProfileRequest) throws Exception {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new Exception("Lỗi hệ thống: không tìm thấy thông tin người dùng."));
        Shop shop = shopRepository.findByOwner(user)
                .orElseThrow(() -> new Exception("Lỗi hệ thống: không tìm thấy thông tin shop."));
        if(!user.getEmail().equalsIgnoreCase(shopProfileRequest.getEmail())) {
            if(userRepository.existsByEmail(shopProfileRequest.getEmail())) {
                throw new Exception("Email đã tồn tại trong hệ thống.");
            }
            user.setEmail(shopProfileRequest.getEmail());
        }
        user.setPhone(shopProfileRequest.getPhone());
        shop.setShopName(shopProfileRequest.getShopName());
        shop.setShopDescription(shopProfileRequest.getShopDescription());
        shop.setBankName(shopProfileRequest.getBankName());
        shop.setBankAccountNumber(shopProfileRequest.getBankAccountNumber());
        shop.setAccountName(shopProfileRequest.getAccountName());
        userRepository.save(user);
        shopRepository.save(shop);
    }
}
