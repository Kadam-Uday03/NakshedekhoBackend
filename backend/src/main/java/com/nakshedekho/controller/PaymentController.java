package com.nakshedekho.controller;

import com.nakshedekho.service.PaymentService;
import com.razorpay.RazorpayException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/payment")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @Value("${razorpay.key.id}")
    private String razorpayKeyId;

    @PostMapping(value = "/create-order", produces = "application/json")
    public ResponseEntity<String> createOrder(@RequestBody Map<String, Object> data) {
        try {
            BigDecimal amount = new BigDecimal(data.get("amount").toString());
            String order = paymentService.createOrder(amount);
            return ResponseEntity.ok(order);
        } catch (RazorpayException e) {
            String errorJson = String.format("{\"error\": \"Error creating order: %s\"}",
                    e.getMessage().replace("\"", "\\\""));
            return ResponseEntity.badRequest().body(errorJson);
        } catch (Exception e) {
            String errorJson = String.format("{\"error\": \"Error: %s\"}",
                    e.getMessage().replace("\"", "\\\""));
            return ResponseEntity.badRequest().body(errorJson);
        }
    }

    @PostMapping("/verify-payment")
    public ResponseEntity<Map<String, Object>> verifyPayment(@RequestBody Map<String, String> paymentData) {
        try {
            boolean isValid = paymentService.verifyPaymentSignature(
                    paymentData.get("razorpay_order_id"),
                    paymentData.get("razorpay_payment_id"),
                    paymentData.get("razorpay_signature"));

            if (isValid) {
                return ResponseEntity.ok(Map.of("status", "success", "verified", true));
            } else {
                return ResponseEntity.badRequest()
                        .body(Map.of("status", "failed", "verified", false, "error", "Invalid signature"));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("status", "error", "verified", false, "error", e.getMessage()));
        }
    }

    @GetMapping("/key")
    public ResponseEntity<Map<String, String>> getKey() {
        return ResponseEntity.ok(Map.of("key", razorpayKeyId));
    }
}
