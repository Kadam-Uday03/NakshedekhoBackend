package com.nakshedekho.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.time.LocalDate;

@Data
public class ProjectUpdateRequest {
    @Min(value = 0, message = "Progress must be between 0 and 100")
    @Max(value = 100, message = "Progress must be between 0 and 100")
    private Integer progressPercentage;

    private String status;
    private LocalDate estimatedCompletion;
    private String projectName;
    private String requirements;
}
