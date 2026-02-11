package com.nakshedekho.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "design_packages")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DesignPackage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToMany(mappedBy = "designPackage", cascade = CascadeType.ALL, orphanRemoval = true)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private java.util.List<InteriorProject> projects;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private Integer areaSqft;

    @Column(columnDefinition = "TEXT")
    private String designScope;

    private Integer timelineDays;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    private Boolean active = true;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "discount_percentage")
    private Integer discountPercentage;

    @Column(name = "offer_description")
    private String offerDescription;

    @Column(name = "offer_valid_until")
    private LocalDateTime offerValidUntil;

    // Subscription-based fields
    @Column(name = "is_subscription")
    private Boolean isSubscription = false;

    @Column(name = "subscription_duration_days")
    private Integer subscriptionDurationDays = 365; // Default 1 year

    @Column(name = "max_projects")
    private Integer maxProjects; // Null = unlimited

    @Column(name = "max_revisions")
    private Integer maxRevisions; // Revisions per project

    @Column(name = "priority_support")
    private Boolean prioritySupport = false;

    @Column(name = "includes_3d_visualization")
    private Boolean includes3dVisualization = false;

    @Column(name = "includes_site_visits")
    private Integer includesSiteVisits = 0;

    @Column(name = "package_tier")
    private String packageTier; // BASIC, STANDARD, PREMIUM, ENTERPRISE

    @Column(name = "service_discount_percentage")
    private Integer serviceDiscountPercentage = 0;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (active == null) {
            active = true;
        }
        if (isSubscription == null) {
            isSubscription = false;
        }
        if (prioritySupport == null) {
            prioritySupport = false;
        }
        if (includes3dVisualization == null) {
            includes3dVisualization = false;
        }
    }
}
