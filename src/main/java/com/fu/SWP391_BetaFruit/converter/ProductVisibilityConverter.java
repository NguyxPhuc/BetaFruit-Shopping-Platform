package com.fu.SWP391_BetaFruit.converter;

import com.fu.SWP391_BetaFruit.enums.ProductVisibility;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ProductVisibilityConverter implements AttributeConverter<ProductVisibility, String> {

    @Override
    public String convertToDatabaseColumn(ProductVisibility attribute) {
        if (attribute == null) {
            return null;
        }
        return switch (attribute) {
            case ACTIVE -> "Active";
            case HIDDEN -> "Hidden";
        };
    }

    @Override
    public ProductVisibility convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return null;
        }
        String clean = dbData.trim();
        if ("Active".equalsIgnoreCase(clean)) {
            return ProductVisibility.ACTIVE;
        }
        if ("Hidden".equalsIgnoreCase(clean)) {
            return ProductVisibility.HIDDEN;
        }
        for (ProductVisibility status : ProductVisibility.values()) {
            if (status.name().equalsIgnoreCase(clean)) {
                return status;
            }
        }
        return ProductVisibility.ACTIVE;
    }
}
