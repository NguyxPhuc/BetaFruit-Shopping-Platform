package com.fu.SWP391_BetaFruit.dto.response;

import com.fu.SWP391_BetaFruit.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PublicShopResponse {
    private Integer shopId;
    private String shopName;
    private String shopDescription;
    private String logoUrl;
    private String bannerUrl;
    private String ownerName;
    private String phone;
    private String email;
    private Long activeProductCount;
    private Double rating;
    private Long reviewCount;
    private String initialLetter;
}
