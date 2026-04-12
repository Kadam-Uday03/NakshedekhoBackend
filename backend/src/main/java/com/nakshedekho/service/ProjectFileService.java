package com.nakshedekho.service;

import com.nakshedekho.model.InteriorProject;
import com.nakshedekho.model.ProjectFile;
import com.nakshedekho.repository.ProjectFileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectFileService {

    private final ProjectFileRepository projectFileRepository;

    @Transactional
    public ProjectFile uploadFile(ProjectFile projectFile) {
        return projectFileRepository.save(projectFile);
    }

    public List<ProjectFile> getProjectFiles(Long projectId) {
        return projectFileRepository.findByProjectIdOrderByUploadedAtDesc(projectId);
    }

    public List<ProjectFile> getProjectFiles(InteriorProject project) {
        return projectFileRepository.findByProjectOrderByUploadedAtDesc(project);
    }

    @Transactional
    public void deleteFile(Long fileId) {
        projectFileRepository.deleteById(fileId);
    }

    public ProjectFile getFileById(Long fileId) {
        return projectFileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("File not found"));
    }

    /**
     * Checks whether a file (identified by its stored filename segment of the URL)
     * belongs to a project that the given user is a customer or assigned manager of.
     * Used by the download endpoint to prevent IDOR access to arbitrary files.
     */
    public boolean isFileAccessibleByUser(String fileName, Long userId) {
        // The stored fileUrl is "/api/files/download/<storedFileName>"
        String urlSuffix = "/api/files/download/" + fileName;
        return projectFileRepository.existsByFileUrlAndProjectCustomerIdOrFileUrlAndProjectManagerId(
                urlSuffix, userId, urlSuffix, userId);
    }
}
