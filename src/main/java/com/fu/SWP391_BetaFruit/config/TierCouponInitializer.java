package com.fu.SWP391_BetaFruit.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Tự động kiểm tra và khởi tạo bảng trung gian TierCoupon
 * cùng dữ liệu mẫu ban đầu theo mô hình Shopee Rewards Voucher Bundle.
 */
@Component
@Order(10)
public class TierCouponInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(TierCouponInitializer.class);

    private final DataSource dataSource;

    public TierCouponInitializer(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(String... args) {
        if (dataSource == null) {
            return;
        }

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            // 1. Tạo bảng TierCoupon nếu chưa tồn tại
            String createTableSql = "IF OBJECT_ID('TierCoupon', 'U') IS NULL " +
                    "BEGIN " +
                    "    CREATE TABLE TierCoupon ( " +
                    "        Id INT IDENTITY(1,1) PRIMARY KEY, " +
                    "        TierId INT NOT NULL, " +
                    "        CouponId INT NOT NULL, " +
                    "        CONSTRAINT FK_TierCoupon_Tier FOREIGN KEY (TierId) REFERENCES MembershipTier(TierId) ON DELETE CASCADE, " +
                    "        CONSTRAINT FK_TierCoupon_Coupon FOREIGN KEY (CouponId) REFERENCES Coupon(CouponId) ON DELETE CASCADE, " +
                    "        CONSTRAINT UQ_Tier_Coupon UNIQUE (TierId, CouponId) " +
                    "    ); " +
                    "END";
            stmt.execute(createTableSql);
            log.info("TierCouponInitializer: Verified/Created table TierCoupon successfully.");

            // 2. Kiểm tra nếu bảng chưa có dữ liệu thì nạp dữ liệu mẫu cho các Hạng
            boolean isEmpty = true;
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM TierCoupon")) {
                if (rs.next()) {
                    isEmpty = (rs.getInt(1) == 0);
                }
            }

            if (isEmpty) {
                // Gán mã sàn cho từng hạng nếu mã tồn tại
                String seedSql = "INSERT INTO TierCoupon (TierId, CouponId) " +
                        "SELECT t.TierId, c.CouponId " +
                        "FROM MembershipTier t " +
                        "CROSS JOIN Coupon c " +
                        "WHERE c.ShopId IS NULL " +
                        "  AND ( " +
                        "       (t.TierName LIKE N'%Bạc%' AND c.CouponCode IN ('SILVER_VIP', 'BETAFRUIT10')) " +
                        "    OR (t.TierName LIKE N'%Vàng%' AND c.CouponCode IN ('GOLD_VIP', 'BETAFRUIT10')) " +
                        "    OR (t.TierName LIKE N'%Kim Cương%' AND c.CouponCode IN ('DIAMOND_VIP', 'FREESHIP50', 'BETAFRUIT10')) " +
                        "  ) " +
                        "  AND NOT EXISTS ( " +
                        "      SELECT 1 FROM TierCoupon tc WHERE tc.TierId = t.TierId AND tc.CouponId = c.CouponId " +
                        "  );";
                int rows = stmt.executeUpdate(seedSql);
                log.info("TierCouponInitializer: Seeded {} default Tier-Coupon bundle associations.", rows);
            }

        } catch (Exception e) {
            log.warn("TierCouponInitializer: Table check/initialization notice (safe to ignore if using mock db): {}", e.getMessage());
        }
    }
}
