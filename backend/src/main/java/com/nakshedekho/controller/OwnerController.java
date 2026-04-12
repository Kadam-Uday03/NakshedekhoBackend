package com.nakshedekho.controller;

import com.nakshedekho.model.*;
import com.nakshedekho.repository.UserRepository;
import com.nakshedekho.service.*;
import com.nakshedekho.dto.RegisterRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/owner")
@RequiredArgsConstructor
public class OwnerController {

    private static final Logger logger = LoggerFactory.getLogger(OwnerController.class);

    private final DesignPackageService packageService;
    private final InteriorProjectService projectService;
    private final ContactEnquiryService enquiryService;
    private final PaymentService paymentService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final VisionaryService visionaryService;
    private final FileUploadService fileUploadService;

    // ─── Package Management ───────────────────────────────────────────────────

    @GetMapping("/packages")
    public ResponseEntity<List<DesignPackage>> getAllPackages() {
        return ResponseEntity.ok(packageService.getAllPackages());
    }

    @PostMapping("/packages")
    public ResponseEntity<DesignPackage> createPackage(@RequestBody DesignPackage designPackage) {
        DesignPackage created = packageService.createPackage(designPackage);
        return ResponseEntity.ok(created);
    }

    @PutMapping("/packages/{id}")
    public ResponseEntity<DesignPackage> updatePackage(
            @PathVariable Long id,
            @RequestBody DesignPackage designPackage) {
        DesignPackage updated = packageService.updatePackage(id, designPackage);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/packages/{id}")
    public ResponseEntity<Void> deletePackage(@PathVariable Long id) {
        packageService.deletePackage(id);
        return ResponseEntity.ok().build();
    }

    // ─── Project Management ───────────────────────────────────────────────────

    @GetMapping("/projects")
    public ResponseEntity<List<InteriorProject>> getAllProjects() {
        return ResponseEntity.ok(projectService.getAllProjects());
    }

    @GetMapping("/projects/{id}")
    public ResponseEntity<InteriorProject> getProjectById(@PathVariable Long id) {
        return ResponseEntity.ok(projectService.getProjectById(id));
    }

    @PutMapping("/projects/{projectId}/assign/{managerId}")
    public ResponseEntity<InteriorProject> assignManager(
            @PathVariable Long projectId,
            @PathVariable Long managerId) {
        // InteriorProjectService already validates that managerId belongs to a MANAGER_ADMIN
        InteriorProject updated = projectService.assignManager(projectId, managerId);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/projects/{id}")
    public ResponseEntity<Void> deleteProject(@PathVariable Long id) {
        projectService.deleteProject(id);
        return ResponseEntity.ok().build();
    }

    // ─── Manager Management ───────────────────────────────────────────────────

    @GetMapping("/managers")
    public ResponseEntity<List<User>> getAllManagers() {
        List<User> managers = userRepository.findByRole(Role.MANAGER_ADMIN);
        return ResponseEntity.ok(managers);
    }

    /**
     * Create a new manager account.
     * Applies @Valid so RegisterRequest password/email constraints are enforced.
     * Explicitly sets role, verified, and active so seeded values are never accidentally inherited.
     */
    @PostMapping("/managers")
    public ResponseEntity<?> createManager(@Valid @RequestBody RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "Email already exists"));
        }

        User user = new User();
        user.setEmail(request.getEmail().trim().toLowerCase());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName());
        user.setPhone(request.getPhone());
        user.setRole(Role.MANAGER_ADMIN);   // Force role — never trust request body
        user.setActive(true);
        user.setVerified(true);
        user.setCreatedAt(LocalDateTime.now());

        User saved = userRepository.save(user);
        logger.info("Manager account created by owner: {}", saved.getEmail());
        return ResponseEntity.ok(saved);
    }

    /**
     * Delete a manager account.
     * IDOR FIX: Previously any Long ID could be passed in, including customer or owner IDs.
     * Now validates the target user exists AND has MANAGER_ADMIN role before deletion.
     */
    @DeleteMapping("/managers/{id}")
    @Transactional
    public ResponseEntity<?> deleteManager(@PathVariable Long id) {
        User target = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Prevent accidentally deleting a customer or owner account via this endpoint
        if (target.getRole() != Role.MANAGER_ADMIN) {
            logger.warn("Owner attempted to delete non-manager account {} (role: {})",
                    id, target.getRole());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Target user is not a manager"));
        }

        // Unassign this manager from any projects before deletion
        List<InteriorProject> projects = projectService.getAllProjects().stream()
                .filter(p -> p.getManager() != null && p.getManager().getId().equals(id))
                .toList();

        for (InteriorProject project : projects) {
            project.setManager(null);
            projectService.updateProject(project.getId(), project);
        }

        userRepository.deleteById(id);
        logger.info("Manager account {} deleted", id);
        return ResponseEntity.ok().build();
    }

    // ─── Customer Management ──────────────────────────────────────────────────

    @GetMapping("/customers")
    public ResponseEntity<List<User>> getAllCustomers() {
        List<User> customers = userRepository.findByRole(Role.CUSTOMER);
        return ResponseEntity.ok(customers);
    }

    /**
     * Delete a customer account.
     * IDOR FIX: Validates that the target is a CUSTOMER before deletion to prevent
     * accidentally deleting a manager or owner account.
     */
    @Transactional
    @DeleteMapping("/customers/{id}")
    public ResponseEntity<?> deleteCustomer(@PathVariable Long id) {
        User target = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (target.getRole() != Role.CUSTOMER) {
            logger.warn("Owner attempted to delete non-customer account {} (role: {})",
                    id, target.getRole());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Target user is not a customer"));
        }

        userRepository.deleteById(id);
        logger.info("Customer account {} deleted", id);
        return ResponseEntity.ok().build();
    }

    // ─── Contact Enquiries ────────────────────────────────────────────────────

    @GetMapping("/enquiries")
    public ResponseEntity<List<ContactEnquiry>> getAllEnquiries() {
        return ResponseEntity.ok(enquiryService.getAllEnquiries());
    }

    @PutMapping("/enquiries/{id}/contacted")
    public ResponseEntity<ContactEnquiry> markAsContacted(@PathVariable Long id) {
        ContactEnquiry updated = enquiryService.markAsContacted(id);
        return ResponseEntity.ok(updated);
    }

    // ─── Payments ─────────────────────────────────────────────────────────────

    @GetMapping("/payments")
    public ResponseEntity<List<Payment>> getAllPayments() {
        return ResponseEntity.ok(paymentService.getAllPayments());
    }

    // ─── Visionary Management ─────────────────────────────────────────────────

    @GetMapping("/visionaries")
    public ResponseEntity<List<Visionary>> getAllVisionaries() {
        return ResponseEntity.ok(visionaryService.getAllVisionaries());
    }

    @PostMapping("/visionaries")
    public ResponseEntity<Visionary> createVisionary(@RequestBody Visionary visionary) {
        return ResponseEntity.ok(visionaryService.createVisionary(visionary));
    }

    @PutMapping("/visionaries/{id}")
    public ResponseEntity<Visionary> updateVisionary(@PathVariable Long id, @RequestBody Visionary visionary) {
        return ResponseEntity.ok(visionaryService.updateVisionary(id, visionary));
    }

    @DeleteMapping("/visionaries/{id}")
    public ResponseEntity<Void> deleteVisionary(@PathVariable Long id) {
        visionaryService.deleteVisionary(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/visionaries/upload")
    public ResponseEntity<Map<String, String>> uploadVisionaryImage(
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        String fileName = fileUploadService.storeFile(file);
        String fileUrl = "/api/files/download/" + fileName;
        Map<String, String> response = new HashMap<>();
        response.put("url", fileUrl);
        return ResponseEntity.ok(response);
    }

    // ─── Analytics ────────────────────────────────────────────────────────────

    @GetMapping("/analytics")
    public ResponseEntity<Map<String, Object>> getAnalytics() {
        List<InteriorProject> allProjects = projectService.getAllProjects();
        List<User> managers = userRepository.findByRole(Role.MANAGER_ADMIN);
        List<User> customers = userRepository.findByRole(Role.CUSTOMER);
        List<DesignPackage> packages = packageService.getAllPackages();

        Map<String, Object> analytics = new HashMap<>();
        analytics.put("totalProjects", allProjects.size());
        analytics.put("totalManagers", managers.size());
        analytics.put("totalCustomers", customers.size());
        analytics.put("totalPackages", packages.size());
        analytics.put("activeProjects", allProjects.stream()
                .filter(p -> p.getStatus() == ProjectStatus.IN_PROGRESS).count());
        analytics.put("completedProjects", allProjects.stream()
                .filter(p -> p.getStatus() == ProjectStatus.COMPLETED).count());
        analytics.put("totalRevenue", paymentService.getAllPayments().stream()
                .filter(p -> p.getStatus() == PaymentStatus.PAID)
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        return ResponseEntity.ok(analytics);
    }
}
