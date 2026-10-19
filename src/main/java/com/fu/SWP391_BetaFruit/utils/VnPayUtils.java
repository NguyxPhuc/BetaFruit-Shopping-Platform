package com.fu.SWP391_BetaFruit.utils;

import com.fu.SWP391_BetaFruit.enums.PaymentMethod;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class VnPayUtils {
    private static final String ALGORITHM = "HmacSHA512";

    private VnPayUtils() {
    }

    public static String hmacSHA512(final String key, final String data) {
        try{
            if (key == null || data == null) {
                throw new NullPointerException("Key hoặc Data null.");
            }
            final Mac hmac512 = Mac.getInstance(ALGORITHM);
            byte[] hmacKeyBytes = key.getBytes();
            final SecretKeySpec secretKey = new SecretKeySpec(hmacKeyBytes, ALGORITHM);
            hmac512.init(secretKey);
            byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);
            byte[] result = hmac512.doFinal(dataBytes);
            StringBuilder sb = new StringBuilder(2 * result.length);
            for (byte b : result) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        }catch(Exception e){
            return "";
        }
    }

    public static String buildQueryUrl(Map<String, String> params) throws Exception {
        List<String> fieldNames = new ArrayList<>(params.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        for (String fieldName : fieldNames) {
            String fieldValue = params.get(fieldName);
            if (fieldValue != null && !fieldValue.isEmpty()) {
                try {
                    if (hashData.length() > 0) {
                        hashData.append('&');
                    }
                    hashData.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII.toString()));
                    hashData.append('=');
                    hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                } catch (Exception e) {
                    throw new Exception("Lỗi mã hóa URL tham số VNPay", e);
                }
            }
        }
        return hashData.toString();
    }

    public static String generateTransactionRef(PaymentMethod paymentMethod, Integer orderId){
        String gatewayName = paymentMethod.name();
        String randomStringUUID = UUID.randomUUID().toString().substring(0, 8);
        String txnRef = gatewayName + "_" + orderId + "_" + randomStringUUID;
        return txnRef;
    }

    public static String translateVnpayResponseCode(String code){
        if (code == null || code.isEmpty()){
            return "Lỗi không xác định.";
        }
        switch (code){
            case "07":
                return "Trừ tiền thành công nhưng giao dịch bị nghi ngờ bất thường.";
            case "09":
                return "Thẻ hoặc tài khoản chưa đăng ký dịch vụ InternetBanking tại ngân hàng.";
            case "10":
                return "Xác thực thông tin thẻ/tài khoản không đúng quá 3 lần.";
            case "11":
                return "Đã hết hạn chờ thanh toán. Xin vui lòng thực hiện lại giao dịch.";
            case "12":
                return "Thẻ hoặc tài khoản ngân hàng của quý khách hiện đang bị khóa.";
            case "13":
                return "Quý khách đã nhập sai mật khẩu xác thực OTP.";
            case "24":
                return "Giao dịch đã được hủy theo yêu cầu của quý khách.";
            case "51":
                return "Số dư tài khoản ngân hàng không đủ để thực hiện thanh toán.";
            case "65":
                return "Tài khoản của quý khách đã vượt quá hạn mức giao dịch trong ngày.";
            case "75":
                return "Tài khoản của quý khách đã vượt quá hạn mức giao dịch trong ngày.";
            case "79":
                return "Quý khách nhập sai mật khẩu thanh toán quá số lần quy định.";
            default:
                return "Giao dịch không thành công." + code;
        }
    }

    public static String formatVnPayDate(String raw){
        if (raw == null || raw.trim().length() < 14) {
            return LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy"));
        }
        try {
            DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
            LocalDateTime dateTime = LocalDateTime.parse(raw, inputFormatter);
            DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy");
            return dateTime.format(outputFormatter);
        }catch (Exception e){
            return raw;
        }
    }
}
