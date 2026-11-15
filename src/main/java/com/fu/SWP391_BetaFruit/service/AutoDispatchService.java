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
     * Tự động tìm tài xế thay thế khả dụng (loại trừ tài xế hiện tại) cho đơn hàng quá hạn (Smart Handover).
     * Nếu tìm được tài xế mới: thực hiện chuyển giao và trả về Optional chứa DeliveryAssignment mới.
     * Nếu không tìm được ai khác: trả về Optional.empty() để giữ nguyên tài xế hiện tại.
     */
    Optional<DeliveryAssignment> autoReassignOrder(Integer orderId, Integer currentShipperId, String reason);
}
