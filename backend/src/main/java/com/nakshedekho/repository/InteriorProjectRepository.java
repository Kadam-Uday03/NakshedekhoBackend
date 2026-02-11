package com.nakshedekho.repository;

import com.nakshedekho.model.InteriorProject;
import com.nakshedekho.model.ProjectStatus;
import com.nakshedekho.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InteriorProjectRepository extends JpaRepository<InteriorProject, Long> {
    List<InteriorProject> findByCustomer(User customer);
    List<InteriorProject> findByManager(User manager);
    List<InteriorProject> findByStatus(ProjectStatus status);
    List<InteriorProject> findByCustomerOrderByCreatedAtDesc(User customer);
    List<InteriorProject> findByManagerOrderByUpdatedAtDesc(User manager);
}
