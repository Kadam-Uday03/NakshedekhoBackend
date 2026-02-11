package com.nakshedekho.service;

import com.nakshedekho.model.ContactEnquiry;
import com.nakshedekho.repository.ContactEnquiryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ContactEnquiryService {

    private final ContactEnquiryRepository enquiryRepository;

    public ContactEnquiry createEnquiry(ContactEnquiry enquiry) {
        return enquiryRepository.save(enquiry);
    }

    public List<ContactEnquiry> getAllEnquiries() {
        return enquiryRepository.findAllByOrderBySubmittedAtDesc();
    }

    public List<ContactEnquiry> getUncontactedEnquiries() {
        return enquiryRepository.findByContactedFalseOrderBySubmittedAtDesc();
    }

    public ContactEnquiry markAsContacted(Long id) {
        ContactEnquiry enquiry = enquiryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Enquiry not found"));
        enquiry.setContacted(true);
        return enquiryRepository.save(enquiry);
    }
}
