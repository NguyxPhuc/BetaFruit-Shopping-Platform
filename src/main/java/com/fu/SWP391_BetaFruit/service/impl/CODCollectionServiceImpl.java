package com.fu.SWP391_BetaFruit.service.impl;

import com.fu.SWP391_BetaFruit.entity.*;
import com.fu.SWP391_BetaFruit.enums.NotificationType;
import com.fu.SWP391_BetaFruit.enums.PaymentMethod;
import com.fu.SWP391_BetaFruit.repository.*;
import com.fu.SWP391_BetaFruit.service.CODCollectionService;
import com.fu.SWP391_BetaFruit.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CODCollectionServiceImpl implements CODCollectionService {

    private final CODCollectionRepository codCollectionRepository;
    private final CODDetailRepository codDetailRepository;
    private final DeliveryAssignmentRepository deliveryAssignmentRepository;
    private final ShipperRepository shipperRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Override
    public Page<CODCollection> getCollectionsPage(Integer shipperId, Pageable pageable) {
        return codCollectionRepository.searchCollections(shipperId, pageable);
    }

    @Override
    public List<CODCollection> getCollectionsByShipper(Integer shipperId) {
        return codCollectionRepository.findByShipper_UserIdOrderByCreatedAtDescCollectionIdDesc(shipperId);
    }

    @Override
    public CODCollection getCollectionById(Integer collectionId) {
        return codCollectionRepository.findById(collectionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy phiếu thu COD #" + collectionId));
    }

    @Override
    public List<CODDetail> getCollectionDetails(Integer collectionId) {
        return codDetailRepository.findByCodCollection_CollectionIdOrderByDetailIdAsc(collectionId);
    }

    @Override
    public List<DeliveryAssignment> getUnremittedAssignmentsByShipper(Integer shipperId) {
        return deliveryAssignmentRepository.findUnremittedCodAssignmentsByShipper(shipperId);
    }

    @Override
    public List<Shipper> getShippersWithDebt() {
        return shipperRepository.findAllWithCodDebt();
    }

    @Override
    public List<Shipper> getAllShippers() {
        return shipperRepository.findAllWithUser();
    }

    @Override
    public BigDecimal getShipperDebt(Integer shipperId) {
        return shipperRepository.findById(shipperId)
                .map(Shipper::getCodDebt)
                .orElse(BigDecimal.ZERO);
    }

    @Override
    @Transactional
    public CODCollection remitCodCollection(Integer staffId, Integer shipperId, List<Integer> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vui lòng chọn ít nhất một đơn hàng để lập phiếu nộp COD");
        }

        User staff = userRepository.findById(staffId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy thông tin nhân viên thu tiền"));

        Shipper shipper = shipperRepository.findById(shipperId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy thông tin nhân viên giao hàng"));

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<Order> validOrders = new ArrayList<>();

        for (Integer orderId : orderIds) {
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn hàng #" + orderId));

            if (order.getPaymentMethod() != PaymentMethod.COD) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Đơn hàng #" + orderId + " không phải là đơn COD");
            }

            if (Boolean.TRUE.equals(order.getIsCodRemitted())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Đơn hàng #" + orderId + " đã được nộp tiền COD trước đó");
            }

            BigDecimal codAmount = OrderPaymentRules.codAmount(order);
            totalAmount = totalAmount.add(codAmount);
            validOrders.add(order);
        }

        if (totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tổng số tiền COD nộp phải lớn hơn 0đ");
        }

        // 1. Create CODCollection receipt
        CODCollection collection = new CODCollection();
        collection.setShipper(shipper);
        collection.setCollectedBy(staff);
        collection.setAmountCollected(totalAmount);
        CODCollection savedCollection = codCollectionRepository.save(collection);

        // 2. Create CODDetail line items and mark orders as remitted
        for (Order order : validOrders) {
            BigDecimal amount = OrderPaymentRules.codAmount(order);
            CODDetail detail = new CODDetail();
            detail.setCodCollection(savedCollection);
            detail.setOrder(order);
            detail.setAmount(amount);
            codDetailRepository.save(detail);

            order.setIsCodRemitted(true);
            orderRepository.save(order);
        }

        // 3. Deduct COD debt from shipper balance
        BigDecimal currentDebt = shipper.getCodDebt() != null ? shipper.getCodDebt() : BigDecimal.ZERO;
        BigDecimal newDebt = currentDebt.subtract(totalAmount);
        if (newDebt.compareTo(BigDecimal.ZERO) < 0) {
            newDebt = BigDecimal.ZERO;
        }
        shipper.setCodDebt(newDebt);
        shipperRepository.save(shipper);

        // 4. Bắn thông báo xác nhận nộp tiền COD cho Shipper
        try {
            if (notificationService != null && shipper.getUser() != null) {
                notificationService.createBaseNotification(
                        shipper.getUser(),
                        "💰 Đã xác nhận nộp tiền COD phiếu #" + savedCollection.getCollectionId(),
                        String.format("Nhân viên vận hành %s đã thu %,d ₫ tiền COD. Công nợ COD hiện tại của bạn là %,d ₫.",
                                staff.getFullName() != null ? staff.getFullName() : "Hệ thống",
                                totalAmount.longValue(),
                                newDebt.longValue()),
                        NotificationType.PAYMENT_UPDATE,
                        "/shipper/orders/cod-history"
                );
            }
        } catch (Exception e) {
            log.warn("Không thể gửi thông báo nộp tiền COD cho Shipper #{}: {}", shipperId, e.getMessage());
        }

        return savedCollection;
    }
}
