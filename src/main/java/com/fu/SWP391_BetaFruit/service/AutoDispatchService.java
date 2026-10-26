package com.fu.SWP391_BetaFruit.service;

import com.fu.SWP391_BetaFruit.entity.DeliveryAssignment;
import java.util.Optional;

public interface AutoDispatchService {

    /**
     * Tự động quét và điều phối Shipper tối ưu nhất cho đơn hàng READY (Chặng 1: Shop -> Trạm ảo).
     * Trả về Optional chứa DeliveryAssignment nếu gán thành công,
     * hoặc Optional.empty() nếu không tìm thấy Shipper phù hợp (cần Staff can thiệp).
     */
    Optional<DeliveryAssignment> autoDispatchOrder(Integer orderId);

    /**
     * Tự động quét và điều phối Shipper chặng 2 (Trạm ảo -> Khách hàng).
     * Tìm Shipper có khu vực hoạt động trùng với địa chỉ nhận hàng của khách.
     */
    Optional<DeliveryAssignment> autoDispatchLeg2(Integer orderId, Integer currentShipperId);
}
