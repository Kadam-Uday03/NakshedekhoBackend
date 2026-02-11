package com.nakshedekho.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

@Service
public class SmsService {

    @Value("${sms.provider.api-key:}")
    private String apiKey;

    @Value("${sms.enabled:false}")
    private boolean isSmsEnabled;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    public void sendSms(String phoneNumber, String message) {
        if (!isSmsEnabled) {
            System.out.println("SMS Disabled. Message to " + phoneNumber + ": " + message);
            // Throwing exception so UI knows to show Dev Mode OTP if real SMS is not set up
            throw new RuntimeException("SMS Disabled in Config. Dev Mode OTP (Log).");
        }

        if (apiKey == null || apiKey.isEmpty()) {
            throw new RuntimeException("SMS API Key missing. Please configure 'sms.provider.api-key'.");
        }

        // Fast2SMS Implementation Example (Quickest for Indian numbers)
        // You can swap this with Twilio or any other provider logic
        sendFast2Sms(phoneNumber, message);
    }

    private void sendFast2Sms(String phoneNumber, String message) {
        try {
            // Fast2SMS API URL (Bulk V2)
            String url = "https://www.fast2sms.com/dev/bulkV2?authorization=" + apiKey +
                    "&route=q&message=" + java.net.URLEncoder.encode(message, "UTF-8") +
                    "&flash=0&numbers=" + phoneNumber;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .GET()
                    .build();

            CompletableFuture<HttpResponse<String>> response = httpClient.sendAsync(request,
                    HttpResponse.BodyHandlers.ofString());

            response.thenAccept(res -> {
                System.out.println("SMS Response: " + res.body());
                if (res.statusCode() != 200) {
                    System.err.println("Failed to send SMS: " + res.body());
                }
            }).join(); // Wait for completion for synchronous usage in auth flow

        } catch (Exception e) {
            System.err.println("Error sending SMS: " + e.getMessage());
            throw new RuntimeException("Failed to send SMS via Gateway");
        }
    }
}
