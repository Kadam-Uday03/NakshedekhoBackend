package com.nakshedekho.repository;

import com.nakshedekho.model.Visionary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VisionaryRepository extends JpaRepository<Visionary, Long> {
    List<Visionary> findByActiveTrue();
}
