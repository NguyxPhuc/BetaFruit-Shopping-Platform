package com.fu.SWP391_BetaFruit.strategy;

import com.fu.SWP391_BetaFruit.enums.PaymentMethod;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class PaymentStrategyFactory {

    private Map<PaymentMethod, PaymentStrategy> paymentStrategyMap;

    @Autowired
    public PaymentStrategyFactory(List<PaymentStrategy> strategyList) {
        this.paymentStrategyMap = new HashMap<>();
        for (PaymentStrategy strategy : strategyList) {
            PaymentMethod key = strategy.getPaymentMethod();
            this.paymentStrategyMap.put(key, strategy);
        }
    }

    public PaymentStrategy getStrategy(PaymentMethod method) throws Exception {
        PaymentStrategy strategy = paymentStrategyMap.get(method);
        if (strategy == null) {
            throw new Exception("Hệ thống chưa hỗ trợ phương thức thanh toán: " + method);
        }
        return strategy;
    }
}
