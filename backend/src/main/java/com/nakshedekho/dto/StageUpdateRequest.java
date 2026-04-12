package com.nakshedekho.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class StageUpdateRequest {

    private Boolean completed;

    @Size(max = 1000, message = "Notes must not exceed 1000 characters")
    private String notes;

    // fileUrl should be a server-relative path only (set by backend after upload)
    // It must NOT be an arbitrary external URL — validated further in the service layer
    @Size(max = 500, message = "File URL too long")
    private String fileUrl;
}
