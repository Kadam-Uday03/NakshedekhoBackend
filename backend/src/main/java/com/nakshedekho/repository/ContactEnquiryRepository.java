package com.nakshedekho.repository;

import com.nakshedekho.model.ContactEnquiry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContactEnquiryRepository extends JpaRepository<ContactEnquiry, Long> {
    List<ContactEnquiry> findByContactedFalseOrderBySubmittedAtDesc();
    List<ContactEnquiry> findAllByOrderBySubmittedAtDesc();
}
