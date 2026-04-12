package com.nakshedekho.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

@Data
public class ProjectUpdateRequest {

    @Min(value = 0, message = "Progress must be between 0 and 100")
    @Max(value = 100, message = "Progress must be between 0 and 100")
    private Integer progressPercentage;

    // Only allow known enum values — prevents arbitrary strings from reaching
    // ProjectStatus.valueOf() which would throw an unhandled IllegalArgumentException
    @Pattern(
        regexp = "^(INITIATED|IN_PROGRESS|COMPLETED|ON_HOLD|CANCELLED)?$",
        message = "Invalid status value"
    )
    private String status;

    // Date must not be more than 10 years in the future
    @Future(message = "Estimated completion date must be in the future")
    private LocalDate estimatedCompletion;

    @Size(min = 2, max = 150, message = "Project name must be between 2 and 150 characters")
    private String projectName;

    @Size(max = 2000, message = "Requirements must not exceed 2000 characters")
    private String requirements;
}
