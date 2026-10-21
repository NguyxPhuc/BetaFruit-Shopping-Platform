package com.fu.SWP391_BetaFruit.service.impl;

import com.fu.SWP391_BetaFruit.dto.response.TransactionDetailResponse;
import com.fu.SWP391_BetaFruit.dto.response.TransactionResponse;
import com.fu.SWP391_BetaFruit.entity.Order;
import com.fu.SWP391_BetaFruit.entity.PaymentTransaction;
import com.fu.SWP391_BetaFruit.entity.User;
import com.fu.SWP391_BetaFruit.repository.PaymentTransactionRepository;
import com.fu.SWP391_BetaFruit.repository.UserRepository;
import com.fu.SWP391_BetaFruit.service.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class TransactionServiceImpl implements TransactionService {

    @Autowired
    private PaymentTransactionRepository transactionRepository;

    @Autowired
    private UserRepository userRepository;

    private TransactionResponse toTransactionResponse(PaymentTransaction paymentTransaction) {
        TransactionResponse transactionResponse = new TransactionResponse();
        transactionResponse.setTransactionRef(paymentTransaction.getTransactionRef());
        transactionResponse.setAmount(paymentTransaction.getAmount());
        transactionResponse.setStatus(paymentTransaction.getStatus().name());
        transactionResponse.setCreatedAt(paymentTransaction.getCreatedAt());
        return transactionResponse;
    }

    @Override
    public List<TransactionResponse> getAllTransactionsByCustomer(Integer customerId) throws Exception {
        if (customerId == null) {
            return new ArrayList<>();
        }
        User user = userRepository.findById(customerId)
                .orElseThrow(() -> new Exception("Không tìm thấy người dùng trong hệ thống."));
        List<PaymentTransaction> paymentTransactionList = transactionRepository
                .findByOrder_Customer_UserIdOrderByCreatedAtDesc(customerId);
        List<TransactionResponse> transactionResponseList = new ArrayList<>();
        for (PaymentTransaction paymentTransaction : paymentTransactionList) {
            transactionResponseList.add(toTransactionResponse(paymentTransaction));
        }
        return transactionResponseList;
    }

    private TransactionDetailResponse toTransactionDetailResponse(PaymentTransaction paymentTransaction) {
        TransactionDetailResponse transactionDetailResponse = new TransactionDetailResponse();
        transactionDetailResponse.setTransactionRef(paymentTransaction.getTransactionRef());
        transactionDetailResponse.setAmount(paymentTransaction.getAmount());
        transactionDetailResponse.setStatus(paymentTransaction.getStatus().name());
        transactionDetailResponse.setCreatedAt(paymentTransaction.getCreatedAt());
        transactionDetailResponse.setGateway(paymentTransaction.getGateway());
        transactionDetailResponse.setCustomerName(paymentTransaction.getOrder().getCustomer().getUsername());
        transactionDetailResponse.setOrderId(paymentTransaction.getOrder().getOrderId());
        transactionDetailResponse.setOrderStatus(paymentTransaction.getOrder().getOrderStatus().name());
        transactionDetailResponse
                .setShippingAddress(paymentTransaction.getOrder().getSnapshotShippingAddress() == null ? ""
                        : paymentTransaction.getOrder().getSnapshotShippingAddress());
        transactionDetailResponse.setOrderCode(String.format("DH%06d", paymentTransaction.getOrder().getOrderId()));
        return transactionDetailResponse;
    }

    @Override
    public TransactionDetailResponse getTransactionDetailsByCustomer(String transactionRef, Integer customerId)
            throws Exception {
        User user = userRepository.findById(customerId)
                .orElseThrow(() -> new Exception("Không tìm thấy người dùng trong hệ thống."));
        PaymentTransaction paymentTransaction = transactionRepository
                .findByTransactionRefAndOrder_Customer_UserId(transactionRef, customerId)
                .orElseThrow(() -> new Exception("Lỗi: Không tìm thấy giao dịch của bạn."));
        Order order = paymentTransaction.getOrder();
        if (order == null) {
            throw new Exception("Lỗi không tìm thấy đơn hàng của giao dịch này.");
        }
        return toTransactionDetailResponse(paymentTransaction);
    }
}
