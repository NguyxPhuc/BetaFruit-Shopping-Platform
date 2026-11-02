package com.fu.SWP391_BetaFruit.dto.request;

import com.fu.SWP391_BetaFruit.enums.ReportReason;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReviewReportRequest {

    @NotNull(message = "Vui lòng chọn lý do vi phạm!")
    private ReportReason reasonCategory;

    @NotBlank(message = "Vui lòng nhập lời giải trình chi tiết!")
    @Size(min = 10, max = 1000, message = "Lời giải trình phải có độ dài từ 10 đến 1000 ký tự!")
    private String explanation;

    @Size(max = 2000, message = "Đường dẫn bằng chứng không được vượt quá 2000 ký tự!")
    private String evidenceUrls;
}
