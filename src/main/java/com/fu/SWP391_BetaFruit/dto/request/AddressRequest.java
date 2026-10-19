package com.fu.SWP391_BetaFruit.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AddressRequest {

    @NotBlank(message = "Vui lòng nhập tên người nhận")
    @Size(max = 100, message = "Tên người nhận không được vượt quá 100 ký tự")
    private String receiverName;

    @NotBlank(message = "Vui lòng nhập số điện thoại")
    @Pattern(regexp = "^(0[3|5|7|8|9])+([0-9]{8})$", message = "Số điện thoại không hợp lệ (Phải là số Việt Nam gồm 10 chữ số)")
    private String receiverPhone;

    @NotBlank(message = "Vui lòng nhập địa chỉ chi tiết (Số nhà, tên đường...)")
    @Size(max = 255, message = "Địa chỉ chi tiết không được vượt quá 255 ký tự")
    private String addressLine;

    @NotBlank(message = "Vui lòng chọn Tỉnh/Thành phố")
    private String city;

    @NotBlank(message = "Vui lòng chọn Quận/Huyện")
    private String district;

    @NotBlank(message = "Vui lòng chọn Phường/Xã")
    private String ward;

    private Boolean isDefault;

    private String addressType;
}