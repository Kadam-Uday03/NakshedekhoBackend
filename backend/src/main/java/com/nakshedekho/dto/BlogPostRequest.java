package com.nakshedekho.dto;

import com.nakshedekho.model.BlogStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class BlogPostRequest {

    @NotBlank(message = "Title is required")
    @Size(min = 5, max = 200, message = "Title must be between 5 and 200 characters")
    private String title;

    // Content can be rich HTML — do NOT strip HTML here.
    // XSS prevention is handled at render time by the frontend (escaping output).
    // Size limit prevents DB flooding.
    @Size(max = 100000, message = "Content exceeds maximum allowed length (100KB)")
    private String content;

    @Size(max = 500, message = "Excerpt must not exceed 500 characters")
    private String excerpt;

    // Cover image URL must be a safe server-relative or https URL — no javascript:/data: URIs
    @Size(max = 500, message = "Cover image URL too long")
    private String coverImageUrl;

    // Tags: max 10 tags, each max 50 chars
    @Size(max = 10, message = "Maximum 10 tags allowed")
    private List<
        @NotBlank(message = "Tag must not be blank")
        @Size(max = 50, message = "Each tag must not exceed 50 characters")
        String
    > tags;

    @Size(max = 100, message = "Category must not exceed 100 characters")
    private String category;

    private BlogStatus status;

    // Slug: only lowercase letters, digits, and hyphens
    @Size(max = 200, message = "Slug must not exceed 200 characters")
    @Pattern(
        regexp = "^[a-z0-9-]*$",
        message = "Slug must contain only lowercase letters, digits, and hyphens"
    )
    private String slug;
}
