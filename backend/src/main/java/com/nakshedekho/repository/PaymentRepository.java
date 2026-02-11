package com.nakshedekho.repository;

import com.nakshedekho.model.InteriorProject;
import com.nakshedekho.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByProject(InteriorProject project);
    List<Payment> findByProjectOrderByCreatedAtDesc(InteriorProject project);
}
