package com.nakshedekho.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public void sendEmail(String to, String subject, String body) throws jakarta.mail.MessagingException {
        log.info("📧 Attempting to send email to: {}", to);
        log.debug("From: {}, Subject: {}", fromEmail, subject);

        jakarta.mail.internet.MimeMessage mimeMessage = mailSender.createMimeMessage();
        org.springframework.mail.javamail.MimeMessageHelper helper = new org.springframework.mail.javamail.MimeMessageHelper(
                mimeMessage, "utf-8");

        try {
            helper.setText(body, true); // true indicates HTML
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setFrom(fromEmail);

            log.debug("🔄 Sending email via SMTP...");
            mailSender.send(mimeMessage);
            log.info("✅ Email successfully sent to: {}", to);

        } catch (jakarta.mail.AuthenticationFailedException e) {
            log.error("❌ AUTHENTICATION FAILED: Gmail rejected the username/password");
            log.error("📋 Troubleshooting steps:");
            log.error("   1. Check if 2-Step Verification is enabled on your Gmail account");
            log.error("   2. Generate a new App Password at: https://myaccount.google.com/apppasswords");
            log.error("   3. Update spring.mail.password in application.properties");
            log.error("   4. Make sure you're using the 16-character app password (no spaces)");
            log.error("   5. Check for security alerts at: https://myaccount.google.com/notifications");
            throw new jakarta.mail.MessagingException(
                    "Gmail Authentication Failed. Please check your App Password. Error: " + e.getMessage(), e);

        } catch (jakarta.mail.MessagingException e) {
            log.error("❌ Failed to send email to {}: {}", to, e.getMessage());

            // Provide specific error guidance
            if (e.getMessage().contains("Could not connect")) {
                log.error("🔌 Connection Error: Unable to connect to Gmail SMTP server");
                log.error("   - Check your internet connection");
                log.error("   - Verify firewall/antivirus is not blocking port 587");
                log.error("   - Try using port 465 with SSL instead");
            } else if (e.getMessage().contains("530")) {
                log.error("🔐 STARTTLS Error: SMTP server requires STARTTLS");
                log.error("   - Verify spring.mail.properties.mail.smtp.starttls.enable=true");
            } else if (e.getMessage().contains("535")) {
                log.error("🔑 Credentials Error: Username or password not accepted");
                log.error("   - Generate a new Gmail App Password");
                log.error("   - Ensure 2-Step Verification is enabled");
            }

            throw e; // Re-throw to be handled by caller

        } catch (org.springframework.mail.MailException e) {
            log.error("❌ SMTP Mail Exception for {}: {}", to, e.getMessage());
            log.error("Full error: ", e);
            throw new jakarta.mail.MessagingException("SMTP Error: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("❌ Unexpected error while sending email: {}", e.getMessage());
            log.error("Full error: ", e);
            throw new jakarta.mail.MessagingException("Unexpected error: " + e.getMessage(), e);
        }
    }

}
