package com.fu.SWP391_BetaFruit.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserTierStatResponse {
    private Integer tierId;         // Tier ID hoặc -1 nếu là tài khoản chưa phân hạng / nội bộ
    private String tierName;        // Tên hạng (Đồng, Bạc, Vàng, Kim Cương, Nội bộ / Chưa có)
    private long count;             // Số lượng người dùng thuộc nhóm này
    private String iconClass;       // CSS icon Bootstrap (bi-circle-fill, bi-award-fill, bi-gem-fill, v.v.)
    private String badgeClass;      // CSS class định danh kiểu dáng
    private String cardClass;       // CSS class thẻ lọc tương tác
    private BigDecimal minSpent;    // Yêu cầu chi tiêu tối thiểu
}
