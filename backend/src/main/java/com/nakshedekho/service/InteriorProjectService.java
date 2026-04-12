package com.nakshedekho.service;

import com.nakshedekho.dto.ProjectUpdateRequest;
import com.nakshedekho.dto.PurchaseRequest;
import com.nakshedekho.dto.StageUpdateRequest;
import com.nakshedekho.model.*;
import com.nakshedekho.repository.InteriorProjectRepository;
import com.nakshedekho.repository.ProjectStageRepository;
import com.nakshedekho.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InteriorProjectService {

    private static final Logger logger = LoggerFactory.getLogger(InteriorProjectService.class);

    private final InteriorProjectRepository projectRepository;
    private final ProjectStageRepository stageRepository;
    private final UserRepository userRepository;
    private final DesignPackageService packageService;
    private final PaymentService paymentService;

    @Transactional
    public InteriorProject createProject(PurchaseRequest request, Long customerId) {
        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        DesignPackage designPackage = packageService.getPackageById(request.getPackageId());

        // Handle subscription purchase
        if (designPackage.getIsSubscription()) {
            // Check if user has an existing active subscription
            boolean hadPreviousSubscription = customer.hasActiveSubscription();
            DesignPackage previousSubscription = customer.getActiveSubscription();

            // Replace old subscription with new one
            customer.setActiveSubscription(designPackage);
            customer.setSubscriptionStartDate(LocalDateTime.now());
            customer.setSubscriptionEndDate(LocalDateTime.now().plusDays(designPackage.getSubscriptionDurationDays()));
            customer.setProjectsUsedUnderSubscription(0); // Reset usage for new subscription

            // Log subscription change for audit purposes
            if (hadPreviousSubscription && previousSubscription != null) {
                logger.info("Subscription changed for customer {} from '{}' to '{}'",
                        customer.getId(), previousSubscription.getName(), designPackage.getName());
            }

            userRepository.save(customer);
        }

        InteriorProject project = new InteriorProject();
        project.setCustomer(customer);
        project.setDesignPackage(designPackage);
        project.setProjectName(request.getProjectName());
        project.setRequirements(request.getRequirements());
        project.setStatus(ProjectStatus.INITIATED);
        project.setProgressPercentage(0);

        project = projectRepository.save(project);

        // Calculate totalPrice
        java.math.BigDecimal basePrice = designPackage.getPrice();
        java.math.BigDecimal finalPrice = basePrice;

        boolean isCustomPrice = request.getComputedPrice() != null;
        if (isCustomPrice) {
            finalPrice = request.getComputedPrice();
        }

        if (!designPackage.getIsSubscription() || isCustomPrice) {
            createDefaultStages(project);

            // Increment usage count if user has an active subscription and this is a
            // service project
            if (customer.hasActiveSubscription()) {
                Integer maxProjects = customer.getActiveSubscription().getMaxProjects();
                if (maxProjects == null || customer.getProjectsUsedUnderSubscription() < maxProjects) {

                    // Only apply discount automatically if it's NOT a custom price
                    // (Custom prices from calculator already include discount if applicable)
                    if (!isCustomPrice) {
                        Integer discountPercent = customer.getActiveSubscription().getServiceDiscountPercentage();
                        if (discountPercent != null && discountPercent > 0) {
                            java.math.BigDecimal discountAmount = basePrice
                                    .multiply(java.math.BigDecimal.valueOf(discountPercent))
                                    .divide(java.math.BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
                            finalPrice = basePrice.subtract(discountAmount);
                        }
                    }

                    // Increment usage count
                    customer.setProjectsUsedUnderSubscription(customer.getProjectsUsedUnderSubscription() + 1);
                    userRepository.save(customer);
                }
            }
        }

        project.setTotalPrice(finalPrice);
        project = projectRepository.save(project);

        // Create advance payment
        // Create advance payment only if amount > 0
        if (request.getAdvanceAmount() != null && request.getAdvanceAmount().compareTo(java.math.BigDecimal.ZERO) > 0) {
            paymentService.createPayment(project, request.getAdvanceAmount(), PaymentType.ADVANCE,
                    request.getTransactionId());
        }

        return project;
    }

    private void createDefaultStages(InteriorProject project) {
        List<String> stageNames = Arrays.asList(
                "Requirement Discussion",
                "Concept Design",
                "3D Design",
                "Final Drawings");

        for (String stageName : stageNames) {
            ProjectStage stage = new ProjectStage();
            stage.setProject(project);
            stage.setStageName(stageName);
            stage.setCompleted(false);
            stageRepository.save(stage);
        }
    }

    public List<InteriorProject> getCustomerProjects(Long customerId) {
        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found"));
        return projectRepository.findByCustomerOrderByCreatedAtDesc(customer)
                .stream()
                .filter(p -> !p.getDesignPackage().getIsSubscription()
                        || (p.getStages() != null && !p.getStages().isEmpty()))
                .toList();
    }

    public List<InteriorProject> getManagerProjects(Long managerId) {
        User manager = userRepository.findById(managerId)
                .orElseThrow(() -> new RuntimeException("Manager not found"));
        return projectRepository.findByManagerOrderByUpdatedAtDesc(manager)
                .stream()
                .filter(p -> !p.getDesignPackage().getIsSubscription())
                .toList();
    }

    public InteriorProject getProjectById(Long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Project not found"));
    }

    public List<InteriorProject> getAllProjects() {
        return projectRepository.findAll().stream()
                .filter(p -> !p.getDesignPackage().getIsSubscription())
                .toList();
    }

    public InteriorProject updateProject(Long id, ProjectUpdateRequest request) {
        InteriorProject project = getProjectById(id);

        if (request.getProgressPercentage() != null) {
            project.setProgressPercentage(request.getProgressPercentage());
        }

        if (request.getStatus() != null) {
            project.setStatus(ProjectStatus.valueOf(request.getStatus()));
        }

        if (request.getEstimatedCompletion() != null) {
            project.setEstimatedCompletion(request.getEstimatedCompletion());
        }

        if (request.getProjectName() != null) {
            project.setProjectName(request.getProjectName());
        }

        if (request.getRequirements() != null) {
            project.setRequirements(request.getRequirements());
        }

        return projectRepository.save(project);
    }

    public InteriorProject updateProject(Long id, InteriorProject project) {
        if (!id.equals(project.getId())) {
            throw new RuntimeException("ID mismatch");
        }
        return projectRepository.save(project);
    }

    public InteriorProject assignManager(Long projectId, Long managerId) {
        InteriorProject project = getProjectById(projectId);
        User manager = userRepository.findById(managerId)
                .orElseThrow(() -> new RuntimeException("Manager not found"));

        if (manager.getRole() != Role.MANAGER_ADMIN) {
            throw new RuntimeException("User is not a manager");
        }

        project.setManager(manager);
        project.setStatus(ProjectStatus.IN_PROGRESS);
        return projectRepository.save(project);
    }

    public List<ProjectStage> getProjectStages(Long projectId) {
        InteriorProject project = getProjectById(projectId);
        return stageRepository.findByProjectOrderByIdAsc(project);
    }

    public ProjectStage getStageById(Long stageId) {
        return stageRepository.findById(stageId)
                .orElseThrow(() -> new RuntimeException("Stage not found: " + stageId));
    }

    public ProjectStage updateStage(Long stageId, StageUpdateRequest request) {
        ProjectStage stage = stageRepository.findById(stageId)
                .orElseThrow(() -> new RuntimeException("Stage not found"));

        if (request.getCompleted() != null) {
            stage.setCompleted(request.getCompleted());
            if (request.getCompleted()) {
                stage.setCompletedAt(LocalDateTime.now());
            }
        }

        if (request.getNotes() != null) {
            stage.setNotes(request.getNotes());
        }

        if (request.getFileUrl() != null) {
            stage.setFileUrl(request.getFileUrl());
        }

        return stageRepository.save(stage);
    }

    @org.springframework.transaction.annotation.Transactional
    public void deleteProject(Long id) {
        projectRepository.deleteById(id);
    }
}
