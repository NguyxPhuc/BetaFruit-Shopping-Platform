package com.fu.SWP391_BetaFruit.service.impl;

import com.fu.SWP391_BetaFruit.dto.request.PayoutCreateRequest;
import com.fu.SWP391_BetaFruit.dto.request.ProcessPayoutRequest;
import com.fu.SWP391_BetaFruit.dto.response.PayoutDetailResponse;
import com.fu.SWP391_BetaFruit.dto.response.PayoutResponse;
import com.fu.SWP391_BetaFruit.dto.response.ShopBalanceResponse;
import com.fu.SWP391_BetaFruit.entity.PayoutRequest;
import com.fu.SWP391_BetaFruit.entity.Shop;
import com.fu.SWP391_BetaFruit.entity.User;
import com.fu.SWP391_BetaFruit.enums.NotificationType;
import com.fu.SWP391_BetaFruit.enums.PayoutStatus;
import com.fu.SWP391_BetaFruit.repository.PayoutRequestRepository;
import com.fu.SWP391_BetaFruit.repository.ShopRepository;
import com.fu.SWP391_BetaFruit.repository.UserRepository;
import com.fu.SWP391_BetaFruit.service.NotificationService;
import com.fu.SWP391_BetaFruit.service.PayoutService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class PayoutServiceImpl implements PayoutService {
    @Autowired
    private PayoutRequestRepository payoutRequestRepository;
    @Autowired
    private ShopRepository shopRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private NotificationService notificationService;

    @Override
    @Transactional(readOnly = true)
    public ShopBalanceResponse getShopBalance(Integer userId) throws Exception {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new Exception("Lỗi hệ thống: Không tìm thấy thông tin người dùng."));
        Shop shop = shopRepository.findByOwner(user)
                .orElseThrow(() -> new Exception("Lỗi hệ thống: Không tìm thấy thông tin cửa hàng."));
        BigDecimal balance = shop.getAvailableBalance() != null ? shop.getAvailableBalance() : BigDecimal.ZERO;
        BigDecimal pendingPayoutAmount = payoutRequestRepository.sumAmountByShopAndStatus(shop.getShopId(), PayoutStatus.PENDING);
        BigDecimal totalWithdrawnAmount = payoutRequestRepository.sumAmountByShopAndStatus(shop.getShopId(), PayoutStatus.COMPLETED);
        ShopBalanceResponse shopBalanceResponse = new ShopBalanceResponse();
        shopBalanceResponse.setAvailableBalance(balance);
        shopBalanceResponse.setAccountName(shop.getAccountName());
        shopBalanceResponse.setBankAccountNumber(shop.getBankAccountNumber());
        shopBalanceResponse.setBankName(shop.getBankName());
        shopBalanceResponse.setPendingPayoutAmount(pendingPayoutAmount);
        shopBalanceResponse.setTotalWithdrawnAmount(totalWithdrawnAmount);
        return shopBalanceResponse;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void requestPayout(Integer userId, PayoutCreateRequest request) throws Exception {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new Exception("Lỗi hệ thống: Không tìm thấy thông tin người dùng."));
        Shop shop = shopRepository.findByOwner(user)
                .orElseThrow(() -> new Exception("Lỗi hệ thống: Không tìm thấy thông tin cửa hàng."));
        BigDecimal balance = shop.getAvailableBalance() != null ? shop.getAvailableBalance() : BigDecimal.ZERO;
        BigDecimal amount = request.getRequestedAmount();
        if (balance.compareTo(amount) < 0) {
            throw new Exception("Số dư khả dụng (" + balance.toString() + ") không đủ để thực hiện rút " + amount.toString());
        }
        shop.setAvailableBalance(balance.subtract(amount));
        shopRepository.save(shop);

        PayoutRequest payout = new PayoutRequest();
        payout.setShop(shop);
        payout.setRequestedAmount(amount);
        payout.setBankName(request.getBankName());
        payout.setBankAccountNumber(request.getBankAccountNumber());
        payout.setAccountName(request.getAccountName());
        payout.setStatus(PayoutStatus.PENDING);
        payoutRequestRepository.save(payout);

        notificationService.createBaseNotification(user, "Yêu cầu rút tiền thành công", "Bạn đã gửi yêu cầu rút " + amount.toString() + " về " + request.getBankName() + ". Vui lòng chờ sàn phê duyệt trong 24h làm việc.", NotificationType.PAYMENT_UPDATE, "/shop/payouts");
    }

    private PayoutResponse toPayoutResponse(PayoutRequest payoutRequest) {
        PayoutResponse payoutResponse = new PayoutResponse();
        payoutResponse.setRequestId(payoutRequest.getRequestId());
        payoutResponse.setRequestedAmount(payoutRequest.getRequestedAmount());
        payoutResponse.setStatus(payoutRequest.getStatus());
        payoutResponse.setCreatedAt(payoutRequest.getCreatedAt());
        return payoutResponse;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PayoutResponse> getAllPayoutRequestsByShop(Integer userId) throws Exception {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new Exception("Lỗi hệ thống: Không tìm thấy thông tin người dùng."));
        Shop shop = shopRepository.findByOwner(user)
                .orElseThrow(() -> new Exception("Lỗi hệ thống: Không tìm thấy thông tin cửa hàng."));
        List<PayoutRequest> payoutRequests = payoutRequestRepository.findByShop(shop);
        List<PayoutResponse> payoutResponses = new ArrayList<>();
        for (PayoutRequest payoutRequest : payoutRequests) {
             payoutResponses.add(toPayoutResponse(payoutRequest));
        }
        return payoutResponses;
    }

    @Override
    @Transactional(readOnly = true)
    public PayoutDetailResponse getPayoutDetailByShop(Integer userId, Integer requestId) throws Exception {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new Exception("Lỗi hệ thống: Không tìm thấy thông tin người dùng."));
        Shop shop = shopRepository.findByOwner(user)
                .orElseThrow(() -> new Exception("Lỗi hệ thống: Không tìm thấy thông tin cửa hàng."));
        PayoutRequest payoutRequest = payoutRequestRepository.findByRequestIdAndShop(requestId, shop).orElseThrow(() -> new Exception("Không tìm thấy yêu cầu rút tiền."));
        PayoutDetailResponse payoutDetailResponse = new PayoutDetailResponse();
        payoutDetailResponse.setAccountName(payoutRequest.getAccountName());
        payoutDetailResponse.setBankName(payoutRequest.getBankName());
        payoutDetailResponse.setBankAccountNumber(payoutRequest.getBankAccountNumber());
        payoutDetailResponse.setRequestedAmount(payoutRequest.getRequestedAmount());
        payoutDetailResponse.setStatus(payoutRequest.getStatus());
        payoutDetailResponse.setCreatedAt(payoutRequest.getCreatedAt());
        payoutDetailResponse.setProcessesBy(payoutRequest.getProcessedBy() != null ? payoutRequest.getProcessedBy().getUsername() : "Chưa xử lý");
        return payoutDetailResponse;
    }



    @Override
    @Transactional(readOnly = true)
    public List<PayoutResponse> getAllPayoutRequests(Integer id) throws Exception {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new Exception("Lỗi hệ thống: Không tìm thấy thông tin nhân viên."));
        List<PayoutRequest> payoutRequests = payoutRequestRepository.findAll();
        List<PayoutResponse> payoutResponses = new ArrayList<>();
        for (PayoutRequest payoutRequest : payoutRequests) {
            payoutResponses.add(toPayoutResponse(payoutRequest));
        }
        return payoutResponses;
    }

    @Override
    @Transactional(readOnly = true)
    public PayoutDetailResponse getPayoutDetail(Integer requestId, Integer userId) throws Exception {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new Exception("Lỗi hệ thống: Không tìm thấy thông tin nhân viên."));
        PayoutRequest payoutRequest = payoutRequestRepository.findByRequestId(requestId).orElseThrow(() -> new Exception("Không tìm thấy yêu cầu rút tiền."));
        PayoutDetailResponse payoutDetailResponse = new PayoutDetailResponse();
        payoutDetailResponse.setAccountName(payoutRequest.getAccountName());
        payoutDetailResponse.setBankName(payoutRequest.getBankName());
        payoutDetailResponse.setBankAccountNumber(payoutRequest.getBankAccountNumber());
        payoutDetailResponse.setRequestedAmount(payoutRequest.getRequestedAmount());
        payoutDetailResponse.setStatus(payoutRequest.getStatus());
        payoutDetailResponse.setCreatedAt(payoutRequest.getCreatedAt());
        payoutDetailResponse.setProcessesBy(payoutRequest.getProcessedBy() != null ? payoutRequest.getProcessedBy().getUsername() : "Chưa xử lý");
        return payoutDetailResponse;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PayoutResponse> getPayoutRequestsByStatus(PayoutStatus status) {
        List<PayoutRequest> payoutRequests = payoutRequestRepository.findAllByStatus(status);
        List<PayoutResponse> payoutResponses = new ArrayList<>();
        for (PayoutRequest payoutRequest : payoutRequests) {
            payoutResponses.add(toPayoutResponse(payoutRequest));
        }
        return payoutResponses;
    }

    @Override
    @Transactional(rollbackFor =  Exception.class)
    public void processPayout(Integer requestId, Integer userId, ProcessPayoutRequest processPayoutRequest) throws Exception {
        PayoutRequest payoutRequest = payoutRequestRepository.findByRequestId(requestId).orElseThrow(() -> new Exception("Không tìm thấy yêu cầu rút tiền."));
        if (payoutRequest.getStatus() != PayoutStatus.PENDING) {
            throw new Exception("Yêu cầu rút tiền này đã được xử lý trước đó!");
        }
        User staff = userRepository.findById(userId)
                .orElseThrow(() -> new Exception("Không xác định được nhân viên xử lý"));
        payoutRequest.setProcessedBy(staff);
        payoutRequest.setStatus(processPayoutRequest.getPayoutStatus());
        payoutRequest.setUpdatedAt(LocalDateTime.now());
        Shop shop = payoutRequest.getShop();
        if (processPayoutRequest.getPayoutStatus() == PayoutStatus.COMPLETED) {
            payoutRequestRepository.save(payoutRequest);
            notificationService.createBaseNotification(
                    shop.getOwner(),
                    "Rút tiền thành công!",
                    "Yêu cầu rút " + payoutRequest.getRequestedAmount().toString() + " của bạn đã được sàn duyệt và giải ngân vào tài khoản " + payoutRequest.getBankAccountNumber() + " (" + payoutRequest.getBankName() + ").",
                    NotificationType.PAYMENT_UPDATE,
                    "/shop/payouts"
            );
        } else if (processPayoutRequest.getPayoutStatus() == PayoutStatus.REJECTED) {
            BigDecimal currentBalance = shop.getAvailableBalance() != null ? shop.getAvailableBalance() : BigDecimal.ZERO;
            shop.setAvailableBalance(currentBalance.add(payoutRequest.getRequestedAmount()));
            shopRepository.save(shop);
            payoutRequestRepository.save(payoutRequest);
            String reason = (processPayoutRequest.getNote() != null && !processPayoutRequest.getNote().isBlank()) ? "Lý do: " + processPayoutRequest.getNote() : "Thông tin ngân hàng không hợp lệ.";
            notificationService.createBaseNotification(
                    shop.getOwner(),
                    "Yêu cầu rút tiền bị từ chối",
                    "Yêu cầu rút " + payoutRequest.getRequestedAmount().toString()  + " bị từ chối. Số tiền đã được hoàn trả lại ví của bạn. " + reason,
                    NotificationType.PAYMENT_UPDATE,
                    "/shop/payouts"
            );
        }
    }
}
