package com.fu.SWP391_BetaFruit.service;

import com.fu.SWP391_BetaFruit.dto.response.DispatchExceptionOrderResponse;
import com.fu.SWP391_BetaFruit.dto.response.DispatchShipperCandidateResponse;
import com.fu.SWP391_BetaFruit.entity.DeliveryAssignment;

import java.util.List;

public interface StaffDispatchService {

    /**
     * Lấy danh sách các đơn hàng ngoại lệ cần Operations Staff can thiệp:
     * - Chưa gán được tài xế tự động (unassigned)
     * - Có sự cố báo cáo trên đường (incident reported)
     * - Bị quá thời gian chờ lấy hàng > 15 phút (timeout)
     */
    List<DispatchExceptionOrderResponse> getExceptionOrders();

    /**
     * Lấy danh sách tài xế khả dụng kèm tải dở dang và công nợ COD để Staff lựa chọn.
     */
    List<DispatchShipperCandidateResponse> getAvailableShippers();

    /**
     * Thao tác Staff gán thủ công hoặc điều phối tài xế cứu viện.
     */
    DeliveryAssignment reassignShipper(Integer orderId, Integer newShipperId, Integer staffId, String reason);
}
