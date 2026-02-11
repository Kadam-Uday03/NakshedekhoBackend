package com.nakshedekho.repository;

import com.nakshedekho.model.InteriorProject;
import com.nakshedekho.model.ProjectStage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectStageRepository extends JpaRepository<ProjectStage, Long> {
    List<ProjectStage> findByProject(InteriorProject project);
    List<ProjectStage> findByProjectOrderByIdAsc(InteriorProject project);
}
