package com.fu.SWP391_BetaFruit.service.impl;

import com.fu.SWP391_BetaFruit.dto.request.ShopProfileRequest;
import com.fu.SWP391_BetaFruit.dto.request.ShopSetupRequest;
import com.fu.SWP391_BetaFruit.dto.response.ShopProfileResponse;
import com.fu.SWP391_BetaFruit.entity.Shop;
import com.fu.SWP391_BetaFruit.entity.User;
import com.fu.SWP391_BetaFruit.enums.RoleName;
import com.fu.SWP391_BetaFruit.enums.ShopApprovalStatus;
import com.fu.SWP391_BetaFruit.enums.UserStatus;
import com.fu.SWP391_BetaFruit.repository.RoleRepository;
import com.fu.SWP391_BetaFruit.repository.ShopRepository;
import com.fu.SWP391_BetaFruit.repository.UserRepository;
import com.fu.SWP391_BetaFruit.service.ShopService;
import com.fu.SWP391_BetaFruit.utils.StringUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ShopServiceImpl implements ShopService {

    private static final Set<String> ALLOWED_SHOP_SORT_FIELDS = Set.of(
            "shopId", "shopName", "approvalStatus", "commissionRate"
    );

    private final ShopRepository shopRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

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
     * Updates the platform commission rate for a specific shop.
     *
     * @param shopId         the ID of the shop to update
     * @param commissionRate the new commission rate percentage (0.00 to 100.00)
     * @throws IllegalArgumentException if the commission rate is null or out of range [0, 100]
     */
    @Override
    @Transactional
    public void updateCommissionRate(Integer shopId, BigDecimal commissionRate) {
        if (commissionRate == null || commissionRate.compareTo(BigDecimal.ZERO) < 0 || commissionRate.compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException("Tỷ lệ chiết khấu phí sàn phải nằm trong khoảng từ 0% đến 100%!");
        }
        Shop shop = getShopById(shopId);
        shop.setCommissionRate(commissionRate);
        shopRepository.save(shop);
    }

    /**
     * Toggles the visibility/active status of a shop by updating its owner's account status.
     * When DEACTIVATED, products from this shop are automatically hidden from the homepage and customer catalog.
     *
     * @param shopId the ID of the shop to toggle
     */
    @Override
    @Transactional
    public void toggleShopVisibility(Integer shopId) {
        Shop shop = getShopById(shopId);
        User owner = shop.getOwner();
        if (owner != null) {
            UserStatus currentStatus = owner.getStatus();
            if (currentStatus == UserStatus.ACTIVE) {
                owner.setStatus(UserStatus.DEACTIVATED);
            } else {
                owner.setStatus(UserStatus.ACTIVE);
            }
            userRepository.save(owner);
        }
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
    public Map<String, Object> getAdminShopPageData(String keyword, String statusStr, int page, int size, String sortBy, String sortDir) {
        int pageIndex = Math.max(0, page - 1);
        int pageSize = size > 0 ? size : 5;

        String safeSortBy = (sortBy != null && ALLOWED_SHOP_SORT_FIELDS.contains(sortBy.trim()))
                ? sortBy.trim() : "shopId";
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(pageIndex, pageSize, Sort.by(direction, safeSortBy));

        ShopApprovalStatus filterStatus = null;
        if (statusStr != null && !statusStr.trim().isEmpty() && !"ALL".equalsIgnoreCase(statusStr.trim())) {
            try {
                filterStatus = ShopApprovalStatus.valueOf(statusStr.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
            }
        }

        String cleanKeyword = StringUtils.sanitizeKeyword(keyword);
        String trimmedKeyword = cleanKeyword != null ? cleanKeyword : "";
        String safeSortDir = "desc".equalsIgnoreCase(sortDir) ? "desc" : "asc";

        Page<Shop> shopPage = shopRepository.searchShopsForAdmin(trimmedKeyword, filterStatus, pageable);

        Map<String, Object> data = new HashMap<>();
        data.put("shops", shopPage.getContent());
        data.put("pageData", shopPage);
        data.put("currentStatus", filterStatus != null ? filterStatus.name() : "ALL");
        data.put("keyword", trimmedKeyword);
        data.put("sortBy", safeSortBy);
        data.put("sortDir", safeSortDir);
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void editShopProfile(Integer userId, ShopProfileRequest shopProfileRequest) throws Exception {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new Exception("Lỗi hệ thống: Không tìm thấy thông tin người dùng."));
        Shop shop = shopRepository.findByOwner(user)
                .orElseThrow(() -> new Exception("Lỗi hệ thống: Không tìm thấy thông tin cửa hàng."));
        if (!user.getEmail().trim().equalsIgnoreCase(shopProfileRequest.getEmail())) {
            if (userRepository.existsByEmail(shopProfileRequest.getEmail())) {
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



    @Override
    public Map<String, Object> getAdminCommissionPageData(String keyword, String ownerStatus, String commissionTier, int page, int size, String sortBy, String sortDir) {
        int pageIndex = Math.max(0, page - 1);
        int pageSize = size > 0 ? size : 5;

        String safeSortBy = (sortBy != null && ALLOWED_SHOP_SORT_FIELDS.contains(sortBy.trim()))
                ? sortBy.trim() : "shopId";
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(pageIndex, pageSize, Sort.by(direction, safeSortBy));

        String cleanKeyword = StringUtils.sanitizeKeyword(keyword);
        String trimmedKeyword = cleanKeyword != null ? cleanKeyword : "";

        UserStatus targetOwnerStatus = null;
        if (ownerStatus != null && !ownerStatus.trim().isEmpty() && !"ALL".equalsIgnoreCase(ownerStatus.trim())) {
            try {
                targetOwnerStatus = UserStatus.valueOf(ownerStatus.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
            }
        }

        String safeOwnerStatus = (ownerStatus != null && !ownerStatus.trim().isEmpty()) ? ownerStatus.trim().toUpperCase() : "ALL";
        String safeCommissionTier = (commissionTier != null && !commissionTier.trim().isEmpty()) ? commissionTier.trim().toUpperCase() : "ALL";
        String safeSortDir = "desc".equalsIgnoreCase(sortDir) ? "desc" : "asc";

        Page<Shop> shopPage = shopRepository.searchApprovedShopsForCommission(trimmedKeyword, targetOwnerStatus, safeCommissionTier, pageable);

        long approvedTotal = countByStatus(ShopApprovalStatus.APPROVED);
        long activeCount = shopRepository.countApprovedShopsByOwnerStatus(UserStatus.ACTIVE);
        long deactivatedCount = shopRepository.countApprovedShopsByOwnerStatus(UserStatus.DEACTIVATED);

        Map<String, Object> data = new HashMap<>();
        data.put("shops", shopPage.getContent());
        data.put("pageData", shopPage);
        data.put("keyword", trimmedKeyword);
        data.put("ownerStatus", safeOwnerStatus);
        data.put("commissionTier", safeCommissionTier);
        data.put("sortBy", safeSortBy);
        data.put("sortDir", safeSortDir);
        data.put("approvedTotal", approvedTotal);
        data.put("activeCount", activeCount);
        data.put("deactivatedCount", deactivatedCount);
        return data;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setUpProfileShop(Integer userId, ShopSetupRequest request) throws Exception {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new Exception("Lỗi hệ thống: Không tìm thấy thông tin người dùng."));
        Shop shop = shopRepository.findByOwner(user)
                .orElseThrow(() -> new Exception("Lỗi hệ thống: Không tìm thấy thông tin cửa hàng."));
        if (shop.getApprovalStatus() == ShopApprovalStatus.APPROVED) {
            throw new Exception("Cửa hàng của bạn đã được phê duyệt, không thể thiết lập lại hồ sơ ban đầu.");
        }
        shop.setShopName(request.getShopName().trim());
        shop.setShopDescription(request.getShopDescription() != null ?  request.getShopDescription().trim() : null);
        shop.setBankName(request.getBankName().trim());
        shop.setBankAccountNumber(request.getBankAccountNumber().trim());
        shop.setAccountName(request.getAccountName().toUpperCase().trim());
        shop.setApprovalStatus(ShopApprovalStatus.PENDING);
        shopRepository.save(shop);
    }
}
