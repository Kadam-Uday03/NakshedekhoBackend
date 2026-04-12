package com.nakshedekho.controller;

import com.nakshedekho.dto.ProjectUpdateRequest;
import com.nakshedekho.dto.StageUpdateRequest;
import com.nakshedekho.model.DesignPackage;
import com.nakshedekho.model.InteriorProject;
import com.nakshedekho.model.ProjectStage;
import com.nakshedekho.model.User;
import com.nakshedekho.security.InputSanitizer;
import com.nakshedekho.service.DesignPackageService;
import com.nakshedekho.service.InteriorProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import com.nakshedekho.service.PaymentService;

@RestController
@RequestMapping("/api/manager")
@RequiredArgsConstructor
public class ManagerController {

    private final InteriorProjectService projectService;
    private final DesignPackageService packageService;
    private final PaymentService paymentService;

    // Package Management (READ ONLY - managers can view packages but not modify them)
    @GetMapping("/packages")
    public ResponseEntity<List<DesignPackage>> getAllPackages() {
        return ResponseEntity.ok(packageService.getAllPackages());
    }

    @GetMapping("/projects")
    public ResponseEntity<List<InteriorProject>> getAssignedProjects(@AuthenticationPrincipal User user) {
        List<InteriorProject> projects = projectService.getManagerProjects(user.getId());
        return ResponseEntity.ok(projects);
    }

    @GetMapping("/projects/{id}")
    public ResponseEntity<InteriorProject> getProjectDetails(@PathVariable Long id,
            @AuthenticationPrincipal User user) {
        InteriorProject project = projectService.getProjectById(id);

        // Verify the project is assigned to this manager
        if (project.getManager() == null || !project.getManager().getId().equals(user.getId())) {
            return ResponseEntity.status(403).build();
        }

        return ResponseEntity.ok(project);
    }

    @PutMapping("/projects/{id}")
    public ResponseEntity<InteriorProject> updateProject(
            @PathVariable Long id,
            @Valid @RequestBody ProjectUpdateRequest request,
            @AuthenticationPrincipal User user) {
        InteriorProject project = projectService.getProjectById(id);

        if (project.getManager() == null || !project.getManager().getId().equals(user.getId())) {
            return ResponseEntity.status(403).build();
        }

        InteriorProject updated = projectService.updateProject(id, request);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/projects/{id}/stages")
    public ResponseEntity<List<ProjectStage>> getProjectStages(@PathVariable Long id,
            @AuthenticationPrincipal User user) {
        InteriorProject project = projectService.getProjectById(id);
        // FIX: Ownership check — manager can only view stages of their own projects
        if (project.getManager() == null || !project.getManager().getId().equals(user.getId())) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(projectService.getProjectStages(id));
    }

    @PutMapping("/stages/{stageId}")
    public ResponseEntity<ProjectStage> updateStage(
            @PathVariable Long stageId,
            @Valid @RequestBody StageUpdateRequest request,
            @AuthenticationPrincipal User user) {
        ProjectStage stage = projectService.getStageById(stageId);
        if (stage.getProject().getManager() == null ||
                !stage.getProject().getManager().getId().equals(user.getId())) {
            return ResponseEntity.status(403).build();
        }

        // Sanitize free-text fields before persistence
        if (request.getNotes() != null)
            request.setNotes(InputSanitizer.sanitizeText(request.getNotes()));
        if (request.getFileUrl() != null) {
            String safeUrl = InputSanitizer.sanitizeUrl(request.getFileUrl());
            request.setFileUrl(safeUrl); // null if URL was dangerous
        }

        ProjectStage updated = projectService.updateStage(stageId, request);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/payments/request")
    public ResponseEntity<?> requestPayment(@Valid @RequestBody com.nakshedekho.dto.PaymentRequestDTO request,
            @AuthenticationPrincipal User user) {
        try {
            InteriorProject project = projectService.getProjectById(request.getProjectId());

            // Verify manager ownership
            if (project.getManager() == null || !project.getManager().getId().equals(user.getId())) {
                return ResponseEntity.status(403).body("You are not authorized to request payment for this project");
            }

            paymentService.createPaymentRequest(project, request.getAmount(), request.getDescription());
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/projects/{id}/payments")
    public ResponseEntity<List<com.nakshedekho.model.Payment>> getProjectPayments(@PathVariable Long id,
            @AuthenticationPrincipal User user) {
        InteriorProject project = projectService.getProjectById(id);
        if (project.getManager() == null || !project.getManager().getId().equals(user.getId())) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(paymentService.getProjectPayments(project));
    }
}
