package com.nakshedekho.controller;

import com.nakshedekho.model.InteriorProject;
import com.nakshedekho.model.ProjectFile;
import com.nakshedekho.model.User;
import com.nakshedekho.service.FileUploadService;
import com.nakshedekho.service.InteriorProjectService;
import com.nakshedekho.service.ProjectFileService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.List;

@RestController
@RequestMapping("/api/files")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class ProjectFileController {

    private final ProjectFileService projectFileService;
    private final InteriorProjectService projectService;
    private final FileUploadService fileUploadService;

    @PostMapping("/upload")
    public ResponseEntity<ProjectFile> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("projectId") Long projectId,
            @RequestParam(value = "description", required = false) String description,
            @AuthenticationPrincipal User user) {

        InteriorProject project = projectService.getProjectById(projectId);

        // Verify user owns this project or is a manager/owner
        boolean hasAccess = project.getCustomer().getId().equals(user.getId()) ||
                (project.getManager() != null && project.getManager().getId().equals(user.getId())) ||
                user.getRole().name().equals("OWNER_ADMIN");

        if (!hasAccess) {
            return ResponseEntity.status(403).build();
        }

        String storedFileName = fileUploadService.storeFile(file);
        String fileUrl = "/api/files/download/" + storedFileName;

        ProjectFile projectFile = new ProjectFile();
        projectFile.setProject(project);
        projectFile.setFileName(file.getOriginalFilename());
        projectFile.setFileUrl(fileUrl);
        projectFile.setFileType(file.getContentType());
        projectFile.setFileSize(file.getSize());
        projectFile.setDescription(description != null ? description : "Uploaded by " + user.getFullName());
        projectFile.setUploadedBy(user.getFullName());

        ProjectFile savedFile = projectFileService.uploadFile(projectFile);
        return ResponseEntity.ok(savedFile);
    }

    @GetMapping("/download/{fileName:.+}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileName) {
        try {
            Path filePath = fileUploadService.getFilePath(fileName);
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists()) {
                String contentType = "application/octet-stream";
                String lowercaseFileName = fileName.toLowerCase();

                if (lowercaseFileName.endsWith(".jpg") || lowercaseFileName.endsWith(".jpeg")) {
                    contentType = "image/jpeg";
                } else if (lowercaseFileName.endsWith(".png")) {
                    contentType = "image/png";
                } else if (lowercaseFileName.endsWith(".gif")) {
                    contentType = "image/gif";
                } else if (lowercaseFileName.endsWith(".pdf")) {
                    contentType = "application/pdf";
                }

                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<ProjectFile>> getProjectFiles(
            @PathVariable Long projectId,
            @AuthenticationPrincipal User user) {

        InteriorProject project = projectService.getProjectById(projectId);

        // Verify user has access (customer, manager, or owner)
        boolean hasAccess = project.getCustomer().getId().equals(user.getId()) ||
                (project.getManager() != null && project.getManager().getId().equals(user.getId())) ||
                user.getRole().name().equals("OWNER_ADMIN");

        if (!hasAccess) {
            return ResponseEntity.status(403).build();
        }

        List<ProjectFile> files = projectFileService.getProjectFiles(projectId);
        return ResponseEntity.ok(files);
    }

    @DeleteMapping("/{fileId}")
    public ResponseEntity<Void> deleteFile(
            @PathVariable Long fileId,
            @AuthenticationPrincipal User user) {

        ProjectFile file = projectFileService.getFileById(fileId);
        InteriorProject project = file.getProject();

        // Customer can delete their own files, or manager/owner
        boolean canDelete = project.getCustomer().getId().equals(user.getId()) ||
                (project.getManager() != null && project.getManager().getId().equals(user.getId())) ||
                user.getRole().name().equals("OWNER_ADMIN");

        if (!canDelete) {
            return ResponseEntity.status(403).build();
        }

        // Try to delete physical file if possible
        try {
            String fileUrl = file.getFileUrl();
            if (fileUrl != null && fileUrl.contains("/download/")) {
                String physicalName = fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
                fileUploadService.deleteFile(physicalName);
            }
        } catch (Exception e) {
            // Log error but continue deleting record
            System.err.println("Failed to delete physical file: " + e.getMessage());
        }

        projectFileService.deleteFile(fileId);
        return ResponseEntity.ok().build();
    }
}
