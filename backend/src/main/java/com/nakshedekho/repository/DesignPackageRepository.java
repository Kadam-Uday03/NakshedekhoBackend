package com.nakshedekho.repository;

import com.nakshedekho.model.DesignPackage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DesignPackageRepository extends JpaRepository<DesignPackage, Long> {
    List<DesignPackage> findByActiveTrue();
}
