package com.fu.SWP391_BetaFruit.service;

import com.fu.SWP391_BetaFruit.entity.DeliveryAssignment;
import java.util.Optional;

public interface AutoDispatchService {

    /**
     * Tự động quét và điều phối Shipper tối ưu nhất cho đơn hàng READY.
     * Trả về Optional chứa DeliveryAssignment nếu gán thành công,
     * hoặc Optional.empty() nếu không tìm thấy Shipper phù hợp (cần Staff can thiệp).
     */
    Optional<DeliveryAssignment> autoDispatchOrder(Integer orderId);
}
