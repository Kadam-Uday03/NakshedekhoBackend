package com.nakshedekho.dto;

import lombok.Data;

@Data
public class StageUpdateRequest {
    private Boolean completed;
    private String notes;
    private String fileUrl;
}
