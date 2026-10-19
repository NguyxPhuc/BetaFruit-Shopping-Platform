package com.fu.SWP391_BetaFruit.config;

import com.fu.SWP391_BetaFruit.utils.VnPayUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.*;

@Component
public class VnPayConfig {
    @Value("${vnpay.tmnCode}")
    private String tmnCode;

    @Value("${vnpay.hashSecret}")
    private String hashSecret;

    @Value("${vnpay.url}")
    private String vnpPayUrl;

    @Value("${vnpay.returnUrl}")
    private String returnUrl;

    // Tạo url gửi đi
    public String buildPaymentUrl(String txnRef, long amount, String ipAddr, String contextPath) throws Exception {
        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", "2.1.0");
        vnp_Params.put("vnp_Command", "pay");
        vnp_Params.put("vnp_TmnCode", tmnCode);
        vnp_Params.put("vnp_Amount", String.valueOf(amount));
        vnp_Params.put("vnp_CurrCode", "VND");
        vnp_Params.put("vnp_TxnRef", txnRef);
        vnp_Params.put("vnp_OrderInfo", "Thanh toan don hang " + txnRef);
        vnp_Params.put("vnp_Locale", "vn");
        String returnPath = returnUrl != null && !returnUrl.isEmpty() ? returnUrl : "/payment/vnpay-return";
        String context = contextPath != null ? contextPath : "";
        vnp_Params.put("vnp_ReturnUrl", context + returnPath);
        vnp_Params.put("vnp_IpAddr", ipAddr != null && !ipAddr.isEmpty() ? ipAddr : "127.0.0.1");

        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String vnpCreateDate = formatter.format(calendar.getTime());
        vnp_Params.put("vnp_CreateDate", vnpCreateDate);

        calendar.add(Calendar.MINUTE, 15);
        vnp_Params.put("vnp_ExpireDate", formatter.format(calendar.getTime()));

        String hashData = VnPayUtils.buildQueryUrl(vnp_Params);
        String vnp_SecureHash = VnPayUtils.hmacSHA512(hashSecret, hashData);
        return vnpPayUrl + "?" + hashData + "&vnp_SecureHash=" + vnp_SecureHash;
    }

    // Hàm kiểm tra chữ kí
    public boolean verifySignature(Map<String, String> fields) throws Exception {
        if (fields == null || fields.isEmpty()) {
            return false;
        }
        Map<String, String> params = new HashMap<>(fields);
        String vnp_SecureHash = params.remove("vnp_SecureHash");
        params.remove("vnp_SecureHashType");
        if (vnp_SecureHash == null || vnp_SecureHash.trim().isEmpty()) {
            return false;
        }
        String hashData = VnPayUtils.buildQueryUrl(params);
        String signValue = VnPayUtils.hmacSHA512(hashSecret, hashData);
        return signValue.equalsIgnoreCase(vnp_SecureHash);
    }
}
