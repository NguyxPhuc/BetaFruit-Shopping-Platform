package com.fu.SWP391_BetaFruit.dto.request;

import com.fu.SWP391_BetaFruit.utils.RegexUtils;
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
    @Pattern(regexp = RegexUtils.NAME_REGEX, message = "Tên người nhận không được chứa số hoặc ký tự đặc biệt")
    private String receiverName;

    @NotBlank(message = "Vui lòng nhập số điện thoại")
    @Pattern(regexp = RegexUtils.PHONE_REGEX, message = "Số điện thoại phải gồm đúng 10 chữ số và bắt đầu bằng các đầu số 03, 05, 07, 08 hoặc 09 (Ví dụ: 0912345678)")
    private String receiverPhone;

    @NotBlank(message = "Vui lòng nhập địa chỉ chi tiết (Số nhà, tên đường...)")
    @Size(max = 255, message = "Địa chỉ chi tiết không được vượt quá 255 ký tự")
    private String addressLine;

    @NotBlank(message = "Vui lòng chọn hoặc nhập Tỉnh/Thành phố")
    @Size(max = 100, message = "Tên Tỉnh/Thành phố không được vượt quá 100 ký tự")
    @Pattern(regexp = RegexUtils.LOCATION_REGEX, message = "Tỉnh/Thành phố không được chứa ký tự đặc biệt không hợp lệ")
    private String city;

    @NotBlank(message = "Vui lòng chọn hoặc nhập Quận/Huyện")
    @Size(max = 100, message = "Tên Quận/Huyện không được vượt quá 100 ký tự")
    @Pattern(regexp = RegexUtils.LOCATION_REGEX, message = "Quận/Huyện không được chứa ký tự đặc biệt không hợp lệ")
    private String district;

    @NotBlank(message = "Vui lòng chọn hoặc nhập Phường/Xã")
    @Size(max = 100, message = "Tên Phường/Xã không được vượt quá 100 ký tự")
    @Pattern(regexp = RegexUtils.LOCATION_REGEX, message = "Phường/Xã không được chứa ký tự đặc biệt không hợp lệ")
    private String ward;

    private Boolean isDefault;

    private String addressType;
}