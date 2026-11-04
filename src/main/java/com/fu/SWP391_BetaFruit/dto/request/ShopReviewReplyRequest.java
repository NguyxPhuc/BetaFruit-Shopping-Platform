package com.fu.SWP391_BetaFruit.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ShopReviewReplyRequest {
    @NotBlank(message = "Vui lòng nhập nội dung phản hồi.")
    @Size(max = 5000, message = "Phản hồi tối đa 5.000 ký tự.")
    private String reply;
}
