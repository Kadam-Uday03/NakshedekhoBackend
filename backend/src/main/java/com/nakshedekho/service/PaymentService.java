package com.nakshedekho.service;

import com.nakshedekho.model.InteriorProject;
import com.nakshedekho.model.Payment;
import com.nakshedekho.model.PaymentStatus;
import com.nakshedekho.model.PaymentType;
import com.nakshedekho.repository.PaymentRepository;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;

    @Value("${razorpay.key.id}")
    private String razorpayKeyId;

    @Value("${razorpay.key.secret}")
    private String razorpayKeySecret;

    private RazorpayClient razorpayClient;

    public void initRazorpay() throws RazorpayException {
        // Lazy initialization or check if null
        if (this.razorpayClient == null) {
            this.razorpayClient = new RazorpayClient(razorpayKeyId, razorpayKeySecret);
        }
    }

    public String createOrder(BigDecimal amount) throws RazorpayException {
        initRazorpay();
        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", amount.multiply(new BigDecimal("100")).intValue()); // Amount in paise
        orderRequest.put("currency", "INR");
        orderRequest.put("receipt", "txn_" + System.currentTimeMillis());

        Order order = razorpayClient.orders.create(orderRequest);
        return order.toString();
    }

    public boolean verifyPaymentSignature(String orderId, String paymentId, String signature) {
        try {
            String payload = orderId + "|" + paymentId;
            String generatedSignature = calculateHmacSHA256(payload, razorpayKeySecret);
            return java.security.MessageDigest.isEqual(generatedSignature.getBytes(), signature.getBytes());
        } catch (Exception e) {
            return false;
        }
    }

    public boolean verifyPaymentStrict(String orderId, String paymentId, String signature, BigDecimal expectedAmount) {
        try {
            // 1. Verify Signature first
            if (!verifyPaymentSignature(orderId, paymentId, signature)) {
                return false;
            }

            // 2. Fetch Order from Razorpay to verify actual paid amount
            initRazorpay();
            Order order = razorpayClient.orders.fetch(orderId);
            
            // Razorpay amounts are in paise
            BigDecimal actualPaidAmount = new BigDecimal(order.get("amount").toString()).divide(new BigDecimal("100"));
            
            // 3. Ensure they paid the full amount expected
            return actualPaidAmount.compareTo(expectedAmount) >= 0;
        } catch (Exception e) {
            return false;
        }
    }

    private String calculateHmacSHA256(String data, String secret) throws Exception {
        javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
        javax.crypto.spec.SecretKeySpec secretKeySpec = new javax.crypto.spec.SecretKeySpec(
                secret.getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(secretKeySpec);
        byte[] hash = mac.doFinal(data.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return bytesToHex(hash);
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }

    public Payment createPayment(InteriorProject project, BigDecimal amount, PaymentType paymentType,
            String transactionId) {
        Payment payment = new Payment();
        payment.setProject(project);
        payment.setAmount(amount);
        payment.setPaymentType(paymentType);
        payment.setStatus(PaymentStatus.PAID);
        payment.setPaymentDate(LocalDateTime.now());
        payment.setTransactionRef(transactionId != null ? transactionId : "TXN-" + System.currentTimeMillis());

        return paymentRepository.save(payment);
    }

    public Payment createPaymentRequest(InteriorProject project, BigDecimal amount, String description) {
        Payment payment = new Payment();
        payment.setProject(project);
        payment.setAmount(amount);
        payment.setPaymentType(PaymentType.INSTALLMENT);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setDescription(description);
        payment.setCreatedAt(LocalDateTime.now());
        return paymentRepository.save(payment);
    }

    // Fallback for existing calls
    public Payment createPayment(InteriorProject project, BigDecimal amount, PaymentType paymentType) {
        return createPayment(project, amount, paymentType, null);
    }

    public List<Payment> getProjectPayments(InteriorProject project) {
        return paymentRepository.findByProjectOrderByCreatedAtDesc(project);
    }

    public BigDecimal getTotalPaid(InteriorProject project) {
        return getProjectPayments(project).stream()
                .filter(p -> p.getStatus() == PaymentStatus.PAID)
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public List<Payment> getAllPayments() {
        return paymentRepository.findAll();
    }

    public BigDecimal getRemainingAmount(InteriorProject project) {
        BigDecimal totalPrice = project.getTotalPrice() != null ? project.getTotalPrice()
                : project.getDesignPackage().getPrice();
        BigDecimal totalPaid = getTotalPaid(project);
        return totalPrice.subtract(totalPaid);
    }

    public Payment getPaymentById(Long id) {
        return paymentRepository.findById(id).orElseThrow(() -> new RuntimeException("Payment not found"));
    }

    public Payment updatePaymentStatus(Long id, PaymentStatus status, String transactionRef) {
        Payment payment = getPaymentById(id);
        payment.setStatus(status);
        if (transactionRef != null) {
            payment.setTransactionRef(transactionRef);
        }
        if (status == PaymentStatus.PAID) {
            payment.setPaymentDate(LocalDateTime.now());
        }
        return paymentRepository.save(payment);
    }
}
