package com.fu.SWP391_BetaFruit.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class ImportNoteDetailResponse {
    private Integer importId;
    private String importCode;
    private String note;
    private BigDecimal totalCost;
    private LocalDateTime importDate;
    private List<ImportNoteItemResponse> items;
}
