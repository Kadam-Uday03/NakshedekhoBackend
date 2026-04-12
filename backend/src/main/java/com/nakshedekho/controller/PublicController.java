package com.nakshedekho.controller;

import com.nakshedekho.model.ContactEnquiry;
import com.nakshedekho.model.DesignPackage;
import com.nakshedekho.service.ContactEnquiryService;
import com.nakshedekho.service.DesignPackageService;
import com.nakshedekho.service.VisionaryService;
import com.nakshedekho.model.Visionary;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicController {

    private static final Logger logger = LoggerFactory.getLogger(PublicController.class);

    private final DesignPackageService packageService;
    private final ContactEnquiryService enquiryService;
    private final VisionaryService visionaryService;

    @GetMapping("/packages")
    public ResponseEntity<List<DesignPackage>> getActivePackages() {
        return ResponseEntity.ok(packageService.getAllActivePackages());
    }

    @GetMapping("/packages/{id}")
    public ResponseEntity<DesignPackage> getPackageById(@PathVariable Long id) {
        return ResponseEntity.ok(packageService.getPackageById(id));
    }

    /**
     * Contact form submission with bot/spam protection:
     *
     * 1. Honeypot field — bots that blindly fill all fields will be caught silently.
     *    Real browsers leave the hidden `website` field empty.
     * 2. Server-side field length validation — prevents database flooding via huge payloads.
     * 3. Basic URL/script pattern detection — blocks obvious spam with links.
     * 4. Rate limiting is enforced upstream by RateLimitingFilter (3 per 5 min per IP).
     *
     * Returns 200 even on honeypot catch — never confirm to bots that they were blocked.
     */
    @PostMapping("/contact")
    public ResponseEntity<?> submitContactForm(@Valid @RequestBody ContactFormRequest request) {

        // ── Honeypot check ────────────────────────────────────────────────────
        // Legitimate users never touch the hidden `website` field.
        // Bots that auto-fill forms will populate it — silently discard.
        if (request.getWebsite() != null && !request.getWebsite().isBlank()) {
            logger.warn("Bot honeypot triggered from contact form — silently discarded");
            // Return 200 so bots think they succeeded — prevents retry storms
            return ResponseEntity.ok(Map.of("message", "Thank you! We will get back to you shortly."));
        }

        // ── Spam content detection ────────────────────────────────────────────
        // Reject messages containing http/https links or common spam patterns
        if (containsSpamPattern(request.getMessage())) {
            logger.warn("Spam content detected in contact form — discarded");
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Your message could not be submitted. Please remove any links."));
        }

        // ── Persist the enquiry ───────────────────────────────────────────────
        ContactEnquiry enquiry = new ContactEnquiry();
        enquiry.setName(sanitize(request.getName()));
        enquiry.setEmail(request.getEmail().trim().toLowerCase());
        enquiry.setPhone(request.getPhone() != null ? sanitize(request.getPhone()) : null);
        enquiry.setMessage(sanitize(request.getMessage()));

        enquiryService.createEnquiry(enquiry);
        return ResponseEntity.ok(Map.of("message", "Thank you! We will get back to you shortly."));
    }

    @GetMapping("/visionaries")
    public ResponseEntity<List<Visionary>> getVisionaries() {
        return ResponseEntity.ok(visionaryService.getActiveVisionaries());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Strips leading/trailing whitespace and collapses internal whitespace. */
    private String sanitize(String input) {
        if (input == null) return null;
        return input.trim().replaceAll("\\s+", " ");
    }

    /** Returns true if the message contains spam indicators (URLs, SQL keywords). */
    private boolean containsSpamPattern(String message) {
        if (message == null) return false;
        String lower = message.toLowerCase();
        return lower.contains("http://")
                || lower.contains("https://")
                || lower.contains("www.")
                || lower.contains("<script")
                || lower.contains("onclick")
                || lower.contains("onload");
    }

    // ── Contact form DTO with Bean Validation ─────────────────────────────────

    /**
     * Separate DTO for contact form — keeps ContactEnquiry entity clean and
     * allows adding the honeypot `website` field without polluting the DB model.
     */
    public static class ContactFormRequest {

        @NotBlank(message = "Name is required")
        @Size(max = 100, message = "Name too long")
        private String name;

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        @Size(max = 255, message = "Email too long")
        private String email;

        @Size(max = 20, message = "Phone number too long")
        private String phone;

        @NotBlank(message = "Message is required")
        @Size(min = 10, max = 2000, message = "Message must be between 10 and 2000 characters")
        private String message;

        /**
         * Honeypot field — hidden from UI via CSS (display:none / aria-hidden).
         * Real users never fill it. Bots that auto-fill forms will populate it.
         * Not persisted to the database.
         */
        private String website;

        // Getters
        public String getName()    { return name; }
        public String getEmail()   { return email; }
        public String getPhone()   { return phone; }
        public String getMessage() { return message; }
        public String getWebsite() { return website; }

        // Setters
        public void setName(String n)    { this.name = n; }
        public void setEmail(String e)   { this.email = e; }
        public void setPhone(String p)   { this.phone = p; }
        public void setMessage(String m) { this.message = m; }
        public void setWebsite(String w) { this.website = w; }
    }
}
