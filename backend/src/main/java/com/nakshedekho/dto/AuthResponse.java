package com.nakshedekho.dto;

public class AuthResponse {
    private String token;
    private String email;
    private String fullName;
    private String role;
    private Long userId;
    private Boolean hasActiveSubscription;
    private Integer serviceDiscountPercentage;
    private String subscriptionTier;

    public AuthResponse() {
    }

    public AuthResponse(String token, String email, String fullName, String role, Long userId,
            Boolean hasActiveSubscription, Integer serviceDiscountPercentage, String subscriptionTier) {
        this.token = token;
        this.email = email;
        this.fullName = fullName;
        this.role = role;
        this.userId = userId;
        this.hasActiveSubscription = hasActiveSubscription;
        this.serviceDiscountPercentage = serviceDiscountPercentage;
        this.subscriptionTier = subscriptionTier;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Boolean getHasActiveSubscription() {
        return hasActiveSubscription;
    }

    public void setHasActiveSubscription(Boolean hasActiveSubscription) {
        this.hasActiveSubscription = hasActiveSubscription;
    }

    public Integer getServiceDiscountPercentage() {
        return serviceDiscountPercentage;
    }

    public void setServiceDiscountPercentage(Integer serviceDiscountPercentage) {
        this.serviceDiscountPercentage = serviceDiscountPercentage;
    }

    public String getSubscriptionTier() {
        return subscriptionTier;
    }

    public void setSubscriptionTier(String subscriptionTier) {
        this.subscriptionTier = subscriptionTier;
    }
}
