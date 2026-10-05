package com.fu.SWP391_BetaFruit.repository;

import com.fu.SWP391_BetaFruit.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification,Integer> {
    // 1. Đếm số thông báo CHƯA ĐỌC và CHƯA BỊ ẨN (Dùng cho chấm đỏ)
    int countByUser_UserIdAndIsReadFalseAndIsHiddenFalse(Integer userId);

    // 2. Lấy 5 thông báo mới nhất CHƯA BỊ ẨN (Dùng cho menu thả xuống)
    List<Notification> findTop5ByUser_UserIdAndIsHiddenFalseOrderByCreatedAtDesc(Integer userId);

    // 3. Lấy TẤT CẢ thông báo CHƯA BỊ ẨN (Dùng cho trang "Xem tất cả")
    List<Notification> findByUser_UserIdAndIsHiddenFalseOrderByCreatedAtDesc(Integer userId);
}
