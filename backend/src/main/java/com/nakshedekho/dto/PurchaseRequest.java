package com.nakshedekho.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PurchaseRequest {
    @NotNull(message = "Package ID is required")
    private Long packageId;

    @NotBlank(message = "Project name is required")
    private String projectName;

    private String requirements;

    private BigDecimal advanceAmount;

    private String transactionId;

    private BigDecimal computedPrice;
}
