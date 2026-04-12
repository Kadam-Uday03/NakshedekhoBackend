package com.nakshedekho.controller;

import com.nakshedekho.dto.ProjectUpdateRequest;
import com.nakshedekho.dto.PurchaseRequest;
import com.nakshedekho.model.InteriorProject;
import com.nakshedekho.model.Payment;
import com.nakshedekho.model.ProjectStage;
import com.nakshedekho.model.User;
import com.nakshedekho.security.InputSanitizer;
import com.nakshedekho.service.InteriorProjectService;
import com.nakshedekho.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/customer")
@RequiredArgsConstructor
public class CustomerController {

    private final InteriorProjectService projectService;
    private final PaymentService paymentService;

    @Value("${razorpay.key.id}")
    private String razorpayKeyId;

    @GetMapping("/profile")
    public ResponseEntity<User> getProfile(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(user);
    }

    @PostMapping({ "/purchase", "/projects" })
    public ResponseEntity<InteriorProject> purchasePackage(
            @Valid @RequestBody PurchaseRequest request,
            @AuthenticationPrincipal User user) {
        InteriorProject project = projectService.createProject(request, user.getId());
        return ResponseEntity.ok(project);
    }

    @GetMapping("/projects")
    public ResponseEntity<List<InteriorProject>> getMyProjects(@AuthenticationPrincipal User user) {
        List<InteriorProject> projects = projectService.getCustomerProjects(user.getId());
        return ResponseEntity.ok(projects);
    }

    @GetMapping("/projects/{id}")
    public ResponseEntity<Map<String, Object>> getProjectDetails(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        InteriorProject project = projectService.getProjectById(id);

        // Verify the project belongs to the customer
        if (!project.getCustomer().getId().equals(user.getId())) {
            return ResponseEntity.status(403).build();
        }

        List<ProjectStage> stages = projectService.getProjectStages(id);
        List<Payment> payments = paymentService.getProjectPayments(project);
        BigDecimal totalPaid = paymentService.getTotalPaid(project);
        BigDecimal remaining = paymentService.getRemainingAmount(project);

        Map<String, Object> response = new HashMap<>();
        response.put("project", project);
        response.put("stages", stages);
        response.put("payments", payments);
        response.put("totalPaid", totalPaid);
        response.put("remainingAmount", remaining);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/projects/{id}/stages")
    public ResponseEntity<List<ProjectStage>> getProjectStages(@PathVariable Long id,
            @AuthenticationPrincipal User user) {
        InteriorProject project = projectService.getProjectById(id);
        // FIX: Ownership check — customer can only see their own project stages
        if (!project.getCustomer().getId().equals(user.getId())) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(projectService.getProjectStages(id));
    }

    @PutMapping("/projects/{id}")
    public ResponseEntity<InteriorProject> updateProject(
            @PathVariable Long id,
            @Valid @RequestBody ProjectUpdateRequest request,
            @AuthenticationPrincipal User user) {
        InteriorProject project = projectService.getProjectById(id);

        // Verify ownership
        if (!project.getCustomer().getId().equals(user.getId())) {
            return ResponseEntity.status(403).build();
        }

        // Sanitize mutable string fields
        if (request.getProjectName() != null)
            request.setProjectName(InputSanitizer.sanitizeText(request.getProjectName()));
        if (request.getRequirements() != null)
            request.setRequirements(InputSanitizer.sanitizeText(request.getRequirements()));

        InteriorProject updated = projectService.updateProject(id, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/projects/{id}")
    public ResponseEntity<Void> deleteProject(@PathVariable Long id, @AuthenticationPrincipal User user) {
        InteriorProject project = projectService.getProjectById(id);
        if (!project.getCustomer().getId().equals(user.getId())) {
            return ResponseEntity.status(403).build();
        }
        projectService.deleteProject(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/payments/{id}/init")
    public ResponseEntity<Map<String, String>> initiatePayment(@PathVariable Long id,
            @AuthenticationPrincipal User user) {
        Payment payment = paymentService.getPaymentById(id);
        if (!payment.getProject().getCustomer().getId().equals(user.getId())) {
            return ResponseEntity.status(403).build();
        }
        try {
            String orderData = paymentService.createOrder(payment.getAmount());
            org.json.JSONObject orderJson = new org.json.JSONObject(orderData);

            Map<String, String> response = new HashMap<>();
            response.put("orderId", orderJson.getString("id"));
            response.put("key", razorpayKeyId);
            response.put("amount", String.valueOf(payment.getAmount().multiply(new BigDecimal("100")).intValue()));
            response.put("currency", "INR");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/payments/{id}/verify")
    public ResponseEntity<?> verifyPayment(@PathVariable Long id, @RequestBody Map<String, String> data,
            @AuthenticationPrincipal User user) {
        Payment payment = paymentService.getPaymentById(id);
        if (!payment.getProject().getCustomer().getId().equals(user.getId())) {
            return ResponseEntity.status(403).build();
        }

        String orderId = data.get("razorpay_order_id");
        String paymentId = data.get("razorpay_payment_id");
        String signature = data.get("razorpay_signature");

        boolean valid = paymentService.verifyPaymentStrict(orderId, paymentId, signature, payment.getAmount());
        if (valid) {
            paymentService.updatePaymentStatus(id, com.nakshedekho.model.PaymentStatus.PAID, paymentId);
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.badRequest().body("Payment integrity failure or invalid signature.");
        }
    }
}
