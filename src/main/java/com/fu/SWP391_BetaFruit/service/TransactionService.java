package com.fu.SWP391_BetaFruit.service;

import com.fu.SWP391_BetaFruit.dto.response.TransactionDetailResponse;
import com.fu.SWP391_BetaFruit.dto.response.TransactionResponse;

import java.util.List;

public interface TransactionService {
    List<TransactionResponse> getAllTransactionsByCustomer(Integer customerId) throws Exception;

    TransactionDetailResponse getTransactionDetailsByCustomer(String transactionRef, Integer customerId) throws Exception;
}
