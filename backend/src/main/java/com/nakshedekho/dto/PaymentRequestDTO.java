package com.nakshedekho.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PaymentRequestDTO {

    @NotNull(message = "Project ID is required")
    @Positive(message = "Project ID must be a positive number")
    private Long projectId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "1.00", message = "Payment amount must be at least ₹1")
    @DecimalMax(value = "10000000.00", message = "Payment amount too large")
    @Digits(integer = 8, fraction = 2, message = "Invalid amount — max 2 decimal places")
    private BigDecimal amount;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;
}
