package com.fu.SWP391_BetaFruit.entity.converter;

import com.fu.SWP391_BetaFruit.enums.RoleName;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Reads the legacy seller role as the application's canonical shop owner role. */
@Converter
public class RoleNameConverter implements AttributeConverter<RoleName, String> {
    @Override
    public String convertToDatabaseColumn(RoleName role) {
        return role == null ? null : role.name();
    }

    @Override
    public RoleName convertToEntityAttribute(String value) {
        if (value == null) return null;
        return "SELLER".equals(value) ? RoleName.SHOP_OWNER : RoleName.valueOf(value);
    }
}
