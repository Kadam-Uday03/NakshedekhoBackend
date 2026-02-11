package com.nakshedekho.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class PaymentRequestDTO {
    private Long projectId;
    private BigDecimal amount;
    private String description;
}
