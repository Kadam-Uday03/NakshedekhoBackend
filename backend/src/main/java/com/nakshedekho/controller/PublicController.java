package com.nakshedekho.controller;

import com.nakshedekho.model.ContactEnquiry;
import com.nakshedekho.model.DesignPackage;
import com.nakshedekho.service.ContactEnquiryService;
import com.nakshedekho.service.DesignPackageService;
import com.nakshedekho.service.VisionaryService;
import com.nakshedekho.model.Visionary;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PublicController {

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

    @PostMapping("/contact")
    public ResponseEntity<ContactEnquiry> submitContactForm(@RequestBody ContactEnquiry enquiry) {
        ContactEnquiry saved = enquiryService.createEnquiry(enquiry);
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/visionaries")
    public ResponseEntity<List<Visionary>> getVisionaries() {
        return ResponseEntity.ok(visionaryService.getActiveVisionaries());
    }
}
