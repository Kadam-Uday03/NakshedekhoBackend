package com.nakshedekho.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PurchaseRequest {

    @NotNull(message = "Package ID is required")
    @Positive(message = "Package ID must be a positive number")
    private Long packageId;

    @NotBlank(message = "Project name is required")
    @Size(min = 2, max = 150, message = "Project name must be between 2 and 150 characters")
    @Pattern(
        regexp = "^[\\p{L}\\p{N}\\s.,\\-'()]+$",
        message = "Project name contains invalid characters"
    )
    private String projectName;

    @Size(max = 2000, message = "Requirements must not exceed 2000 characters")
    private String requirements;

    @DecimalMin(value = "0.0", inclusive = true, message = "Advance amount cannot be negative")
    @DecimalMax(value = "10000000.00", message = "Advance amount too large")
    @Digits(integer = 8, fraction = 2, message = "Invalid amount format")
    private BigDecimal advanceAmount;

    @Size(max = 100, message = "Transaction ID too long")
    @Pattern(
        regexp = "^[a-zA-Z0-9_\\-]*$",
        message = "Transaction ID contains invalid characters"
    )
    private String transactionId;

    @DecimalMin(value = "0.0", inclusive = true, message = "Computed price cannot be negative")
    @DecimalMax(value = "10000000.00", message = "Computed price too large")
    @Digits(integer = 8, fraction = 2, message = "Invalid price format")
    private BigDecimal computedPrice;
}
