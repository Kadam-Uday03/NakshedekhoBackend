package com.nakshedekho.controller;

import com.nakshedekho.model.*;
import com.nakshedekho.repository.UserRepository;
import com.nakshedekho.service.*;
import com.nakshedekho.dto.RegisterRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/owner")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class OwnerController {

    private final DesignPackageService packageService;
    private final InteriorProjectService projectService;
    private final ContactEnquiryService enquiryService;
    private final PaymentService paymentService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final VisionaryService visionaryService;
    private final FileUploadService fileUploadService;

    // Package Management
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

    // Project Management
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
        InteriorProject updated = projectService.assignManager(projectId, managerId);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/projects/{id}")
    public ResponseEntity<Void> deleteProject(@PathVariable Long id) {
        projectService.deleteProject(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/managers")
    public ResponseEntity<List<User>> getAllManagers() {
        List<User> managers = userRepository.findByRole(Role.MANAGER_ADMIN);
        return ResponseEntity.ok(managers);
    }

    @PostMapping("/managers")
    public ResponseEntity<User> createManager(@RequestBody RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName());
        user.setPhone(request.getPhone());
        user.setRole(Role.MANAGER_ADMIN);

        User saved = userRepository.save(user);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/managers/{id}")
    @Transactional
    public ResponseEntity<Void> deleteManager(@PathVariable Long id) {
        // Find projects assigned to this manager and unassign them
        List<InteriorProject> projects = projectService.getAllProjects().stream()
                .filter(p -> p.getManager() != null && p.getManager().getId().equals(id))
                .toList();

        for (InteriorProject project : projects) {
            project.setManager(null);
            projectService.updateProject(project.getId(), project);
        }

        userRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }

    // Customer Management
    @GetMapping("/customers")
    public ResponseEntity<List<User>> getAllCustomers() {
        List<User> customers = userRepository.findByRole(Role.CUSTOMER);
        return ResponseEntity.ok(customers);
    }

    @Transactional
    @DeleteMapping("/customers/{id}")
    public ResponseEntity<Void> deleteCustomer(@PathVariable Long id) {
        userRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }

    // Contact Enquiries
    @GetMapping("/enquiries")
    public ResponseEntity<List<ContactEnquiry>> getAllEnquiries() {
        return ResponseEntity.ok(enquiryService.getAllEnquiries());
    }

    @PutMapping("/enquiries/{id}/contacted")
    public ResponseEntity<ContactEnquiry> markAsContacted(@PathVariable Long id) {
        ContactEnquiry updated = enquiryService.markAsContacted(id);
        return ResponseEntity.ok(updated);
    }

    // Payments
    @GetMapping("/payments")
    public ResponseEntity<List<Payment>> getAllPayments() {
        return ResponseEntity.ok(paymentService.getAllPayments());
    }

    // Visionary Management
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

    // Analytics
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
