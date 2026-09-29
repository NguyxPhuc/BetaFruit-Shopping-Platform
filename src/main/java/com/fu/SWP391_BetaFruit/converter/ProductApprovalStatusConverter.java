package com.fu.SWP391_BetaFruit.converter;

import com.fu.SWP391_BetaFruit.enums.ProductApprovalStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ProductApprovalStatusConverter implements AttributeConverter<ProductApprovalStatus, String> {

    @Override
    public String convertToDatabaseColumn(ProductApprovalStatus attribute) {
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
    public ProductApprovalStatus convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return null;
        }
        String clean = dbData.trim();
        if ("Pending".equalsIgnoreCase(clean)) {
            return ProductApprovalStatus.PENDING;
        }
        if ("Approved".equalsIgnoreCase(clean)) {
            return ProductApprovalStatus.APPROVED;
        }
        if ("Rejected".equalsIgnoreCase(clean)) {
            return ProductApprovalStatus.REJECTED;
        }
        for (ProductApprovalStatus status : ProductApprovalStatus.values()) {
            if (status.name().equalsIgnoreCase(clean)) {
                return status;
            }
        }
        return ProductApprovalStatus.PENDING;
    }
}
