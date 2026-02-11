package com.nakshedekho.service;

import com.nakshedekho.model.DesignPackage;
import com.nakshedekho.model.User;
import com.nakshedekho.repository.DesignPackageRepository;
import com.nakshedekho.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DesignPackageService {

    private final DesignPackageRepository packageRepository;
    private final UserRepository userRepository;

    public List<DesignPackage> getAllActivePackages() {
        return packageRepository.findByActiveTrue();
    }

    public List<DesignPackage> getAllPackages() {
        return packageRepository.findAll();
    }

    public DesignPackage getPackageById(Long id) {
        return packageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Package not found"));
    }

    public DesignPackage createPackage(DesignPackage designPackage) {
        return packageRepository.save(designPackage);
    }

    public DesignPackage updatePackage(Long id, DesignPackage updatedPackage) {
        DesignPackage existingPackage = getPackageById(id);

        // Basic Fields
        if (updatedPackage.getName() != null)
            existingPackage.setName(updatedPackage.getName());
        if (updatedPackage.getDescription() != null)
            existingPackage.setDescription(updatedPackage.getDescription());
        if (updatedPackage.getAreaSqft() != null)
            existingPackage.setAreaSqft(updatedPackage.getAreaSqft());
        if (updatedPackage.getDesignScope() != null)
            existingPackage.setDesignScope(updatedPackage.getDesignScope());
        if (updatedPackage.getTimelineDays() != null)
            existingPackage.setTimelineDays(updatedPackage.getTimelineDays());
        if (updatedPackage.getPrice() != null)
            existingPackage.setPrice(updatedPackage.getPrice());
        if (updatedPackage.getActive() != null)
            existingPackage.setActive(updatedPackage.getActive());
        if (updatedPackage.getImageUrl() != null)
            existingPackage.setImageUrl(updatedPackage.getImageUrl());

        // Offer Fields
        if (updatedPackage.getDiscountPercentage() != null)
            existingPackage.setDiscountPercentage(updatedPackage.getDiscountPercentage());
        if (updatedPackage.getOfferDescription() != null)
            existingPackage.setOfferDescription(updatedPackage.getOfferDescription());
        if (updatedPackage.getOfferValidUntil() != null)
            existingPackage.setOfferValidUntil(updatedPackage.getOfferValidUntil());

        // Subscription Fields
        if (updatedPackage.getIsSubscription() != null)
            existingPackage.setIsSubscription(updatedPackage.getIsSubscription());
        if (updatedPackage.getPackageTier() != null)
            existingPackage.setPackageTier(updatedPackage.getPackageTier());
        if (updatedPackage.getMaxProjects() != null)
            existingPackage.setMaxProjects(updatedPackage.getMaxProjects());
        if (updatedPackage.getMaxRevisions() != null)
            existingPackage.setMaxRevisions(updatedPackage.getMaxRevisions());
        if (updatedPackage.getIncludesSiteVisits() != null)
            existingPackage.setIncludesSiteVisits(updatedPackage.getIncludesSiteVisits());
        if (updatedPackage.getIncludes3dVisualization() != null)
            existingPackage.setIncludes3dVisualization(updatedPackage.getIncludes3dVisualization());
        if (updatedPackage.getPrioritySupport() != null)
            existingPackage.setPrioritySupport(updatedPackage.getPrioritySupport());
        if (updatedPackage.getSubscriptionDurationDays() != null)
            existingPackage.setSubscriptionDurationDays(updatedPackage.getSubscriptionDurationDays());
        if (updatedPackage.getServiceDiscountPercentage() != null)
            existingPackage.setServiceDiscountPercentage(updatedPackage.getServiceDiscountPercentage());

        return packageRepository.save(existingPackage);
    }

    @org.springframework.transaction.annotation.Transactional
    public void deletePackage(Long id) {
        DesignPackage pkg = getPackageById(id);

        // 1. Unassign from users who have this as active subscription
        List<User> users = userRepository.findByActiveSubscription_Id(id);
        for (User user : users) {
            user.setActiveSubscription(null);
            user.setSubscriptionStartDate(null);
            user.setSubscriptionEndDate(null);
            userRepository.save(user);
        }

        // 2. Cascade delete is handled by @OneToMany(cascade = CascadeType.ALL)
        // for projects, but if we want to be safe and avoid issues with
        // DB constraints not playing nice with Hibernate's order of operations:
        packageRepository.delete(pkg);
    }
}
