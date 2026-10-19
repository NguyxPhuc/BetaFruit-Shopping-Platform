package com.fu.SWP391_BetaFruit.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ImportNoteItemResponse {
    private Integer detailId;
    private Integer variantId;
    private String productName;
    private String variantName;
    private Integer quantity;
    private BigDecimal importPrice;
    private BigDecimal lineTotal;
}
