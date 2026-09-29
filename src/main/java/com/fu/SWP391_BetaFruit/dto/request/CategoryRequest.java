package com.fu.SWP391_BetaFruit.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CategoryRequest {
    private Integer categoryId;
    private String categoryName;
    private Boolean isActive;
}
