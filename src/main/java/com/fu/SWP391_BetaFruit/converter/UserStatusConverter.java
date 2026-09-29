package com.fu.SWP391_BetaFruit.converter;

import com.fu.SWP391_BetaFruit.enums.UserStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class UserStatusConverter implements AttributeConverter<UserStatus, String> {

    @Override
    public String convertToDatabaseColumn(UserStatus attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute == UserStatus.ACTIVE ? "Active" : "Deactivated";
    }

    @Override
    public UserStatus convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return null;
        }
        String clean = dbData.trim();
        if ("Active".equalsIgnoreCase(clean)) {
            return UserStatus.ACTIVE;
        }
        if ("Deactivated".equalsIgnoreCase(clean)) {
            return UserStatus.DEACTIVATED;
        }
        for (UserStatus status : UserStatus.values()) {
            if (status.name().equalsIgnoreCase(clean)) {
                return status;
            }
        }
        return UserStatus.ACTIVE;
    }
}
