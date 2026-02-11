package com.nakshedekho;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

import jakarta.mail.internet.MimeMessage;

@SpringBootTest
public class EmailConfigTest {

    @Autowired
    private JavaMailSender mailSender;

    @Test
    public void testEmailConnection() {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setFrom("kadamuday2003@gmail.com");
            helper.setTo("kadamuday2003@gmail.com");
            helper.setSubject("Test Email - NaksheDekho");
            helper.setText("This is a test email to verify SMTP configuration.", true);

            mailSender.send(message);
            System.out.println("✅ Email sent successfully! SMTP configuration is working.");
        } catch (Exception e) {
            System.err.println("❌ Email sending failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
