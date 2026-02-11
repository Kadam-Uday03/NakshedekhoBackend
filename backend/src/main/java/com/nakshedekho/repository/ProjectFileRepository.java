package com.nakshedekho.repository;

import com.nakshedekho.model.InteriorProject;
import com.nakshedekho.model.ProjectFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectFileRepository extends JpaRepository<ProjectFile, Long> {
    List<ProjectFile> findByProjectOrderByUploadedAtDesc(InteriorProject project);

    List<ProjectFile> findByProjectIdOrderByUploadedAtDesc(Long projectId);
}
