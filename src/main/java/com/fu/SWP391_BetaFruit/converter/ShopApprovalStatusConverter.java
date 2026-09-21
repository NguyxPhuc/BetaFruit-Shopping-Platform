package com.fu.SWP391_BetaFruit.converter;

import com.fu.SWP391_BetaFruit.enums.ShopApprovalStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ShopApprovalStatusConverter implements AttributeConverter<ShopApprovalStatus, String> {

    @Override
    public String convertToDatabaseColumn(ShopApprovalStatus attribute) {
        if (attribute == null) {
            return null;
        }
        return switch (attribute) {
            case PENDING -> "Pending";
            case APPROVED -> "Approved";
            case REJECTED -> "Rejected";
        };
    }

    @Override
    public ShopApprovalStatus convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return null;
        }
        String clean = dbData.trim();
        if ("Pending".equalsIgnoreCase(clean)) {
            return ShopApprovalStatus.PENDING;
        }
        if ("Approved".equalsIgnoreCase(clean)) {
            return ShopApprovalStatus.APPROVED;
        }
        if ("Rejected".equalsIgnoreCase(clean)) {
            return ShopApprovalStatus.REJECTED;
        }
        for (ShopApprovalStatus status : ShopApprovalStatus.values()) {
            if (status.name().equalsIgnoreCase(clean)) {
                return status;
            }
        }
        return ShopApprovalStatus.PENDING;
    }
}
