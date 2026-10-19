package com.fu.SWP391_BetaFruit.service.impl;

import com.fu.SWP391_BetaFruit.service.EmailService;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EmailServiceImpl implements EmailService {
    @Autowired
    private JavaMailSender mailSender;

    @Override
    public void sendWelcomeEmail(String toEmail, String fullName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject("Chào mừng bạn đến với BetaFruit!");

            String htmlContent = "<div style='font-family: Arial, sans-serif; padding: 20px; color: #333;'>"
                    + "<h2 style='color: #4CAF50;'>Xin chào " + fullName + ",</h2>"
                    + "<p>Chúc mừng bạn đã đăng ký tài khoản thành công tại <strong>BetaFruit</strong> - Nền tảng mua sắm trái cây sạch số 1.</p>"
                    + "<p>Hãy đăng nhập ngay để bắt đầu trải nghiệm mua sắm hoặc bán hàng cùng chúng tôi nhé!</p>"
                    + "<a href='http://localhost:8080/auth/login' style='display: inline-block; padding: 10px 20px; margin-top: 15px; background-color: #4CAF50; color: white; text-decoration: none; border-radius: 5px;'>Đăng nhập ngay</a>"
                    + "<p style='margin-top: 30px; font-size: 12px; color: #777;'>Nếu bạn không thực hiện đăng ký này, vui lòng bỏ qua email này.</p>"
                    + "</div>";

            helper.setText(htmlContent, true);

            mailSender.send(message);
        } catch (Exception e) {
            log.error("Lỗi khi gửi email", e);
        }
    }
}
