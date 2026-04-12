package com.nakshedekho.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class RegisterRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
        message = "Password must contain at least one uppercase letter, one lowercase letter, " +
                  "one digit, and one special character (@$!%*?&)"
    )
    private String password;

    @NotBlank(message = "Full name is required")
    private String fullName;

    private String phone;

    private String professionalCategory;

    // SECURITY: role field intentionally omitted — role is ALWAYS forced to CUSTOMER in AuthService.
    // Privileged roles (MANAGER_ADMIN, OWNER_ADMIN) can only be created by an OWNER_ADMIN
    // via POST /api/owner/managers. Never accept role from the registration payload.

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getProfessionalCategory() { return professionalCategory; }
    public void setProfessionalCategory(String v) { this.professionalCategory = v; }
}
