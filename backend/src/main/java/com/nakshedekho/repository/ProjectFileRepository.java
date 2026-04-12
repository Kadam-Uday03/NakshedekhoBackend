package com.nakshedekho.repository;

import com.nakshedekho.model.InteriorProject;
import com.nakshedekho.model.ProjectFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectFileRepository extends JpaRepository<ProjectFile, Long> {

    List<ProjectFile> findByProjectOrderByUploadedAtDesc(InteriorProject project);

    List<ProjectFile> findByProjectIdOrderByUploadedAtDesc(Long projectId);

    /**
     * IDOR guard for file downloads.
     * Returns true if a record with the given fileUrl exists AND
     * the linked project belongs to the customer OR has this user assigned as manager.
     */
    @Query("SELECT COUNT(f) > 0 FROM ProjectFile f " +
           "WHERE (f.fileUrl = :url1 AND f.project.customer.id = :customerId) " +
           "   OR (f.fileUrl = :url2 AND f.project.manager.id = :managerId)")
    boolean existsByFileUrlAndProjectCustomerIdOrFileUrlAndProjectManagerId(
            @Param("url1") String url1,
            @Param("customerId") Long customerId,
            @Param("url2") String url2,
            @Param("managerId") Long managerId);
}
