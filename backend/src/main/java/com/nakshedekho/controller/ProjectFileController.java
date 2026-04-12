package com.nakshedekho.controller;

import com.nakshedekho.model.InteriorProject;
import com.nakshedekho.model.ProjectFile;
import com.nakshedekho.model.Role;
import com.nakshedekho.model.User;
import com.nakshedekho.service.FileUploadService;
import com.nakshedekho.service.InteriorProjectService;
import com.nakshedekho.service.ProjectFileService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.List;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class ProjectFileController {

    private static final Logger logger = LoggerFactory.getLogger(ProjectFileController.class);

    private final ProjectFileService projectFileService;
    private final InteriorProjectService projectService;
    private final FileUploadService fileUploadService;

    // ─── Upload ────────────────────────────────────────────────────────────────

    @PostMapping("/upload")
    public ResponseEntity<ProjectFile> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("projectId") Long projectId,
            @RequestParam(value = "description", required = false) String description,
            @AuthenticationPrincipal User user) {

        InteriorProject project = projectService.getProjectById(projectId);

        if (!hasProjectAccess(user, project)) {
            logger.warn("IDOR attempt: user {} tried to upload to project {} (owner: {})",
                    user.getId(), projectId, project.getCustomer().getId());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
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

    // ─── Download ──────────────────────────────────────────────────────────────

    /**
     * GET /api/files/download/{fileName}
     * IDOR FIX: Previously this endpoint was fully unauthenticated — any URL-guesser
     * could download any project file. Now the caller must be authenticated AND must
     * own or be assigned to a project that contains a file with this name.
     */
    @GetMapping("/download/{fileName:.+}")
    public ResponseEntity<Resource> downloadFile(
            @PathVariable String fileName,
            @AuthenticationPrincipal User user) {

        // Owners can skip the per-project lookup
        if (user.getRole() != Role.OWNER_ADMIN) {
            // Verify the requested file belongs to a project that this user has access to
            boolean authorised = projectFileService.isFileAccessibleByUser(fileName, user.getId());
            if (!authorised) {
                logger.warn("IDOR attempt: user {} tried to download file '{}' without project access",
                        user.getId(), fileName);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        }

        try {
            Path filePath = fileUploadService.getFilePath(fileName);
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists()) {
                return ResponseEntity.notFound().build();
            }

            String contentType = resolveContentType(fileName.toLowerCase());
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);

        } catch (Exception e) {
            logger.error("File download failed for '{}'", fileName, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // ─── List files for a project ──────────────────────────────────────────────

    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<ProjectFile>> getProjectFiles(
            @PathVariable Long projectId,
            @AuthenticationPrincipal User user) {

        InteriorProject project = projectService.getProjectById(projectId);

        if (!hasProjectAccess(user, project)) {
            logger.warn("IDOR attempt: user {} tried to list files of project {} (owner: {})",
                    user.getId(), projectId, project.getCustomer().getId());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        List<ProjectFile> files = projectFileService.getProjectFiles(projectId);
        return ResponseEntity.ok(files);
    }

    // ─── Delete ────────────────────────────────────────────────────────────────

    @DeleteMapping("/{fileId}")
    public ResponseEntity<Void> deleteFile(
            @PathVariable Long fileId,
            @AuthenticationPrincipal User user) {

        ProjectFile file = projectFileService.getFileById(fileId);
        InteriorProject project = file.getProject();

        if (!hasProjectAccess(user, project)) {
            logger.warn("IDOR attempt: user {} tried to delete file {} from project {} (owner: {})",
                    user.getId(), fileId, project.getId(), project.getCustomer().getId());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        // Delete the physical file from disk
        try {
            String fileUrl = file.getFileUrl();
            if (fileUrl != null && fileUrl.contains("/download/")) {
                String physicalName = fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
                fileUploadService.deleteFile(physicalName);
            }
        } catch (Exception e) {
            logger.error("Failed to delete physical file for record {}: {}", fileId, e.getMessage());
        }

        projectFileService.deleteFile(fileId);
        return ResponseEntity.ok().build();
    }

    // ─── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Returns true if the user is authorised to access files belonging to this project:
     * - OWNER_ADMIN: always
     * - MANAGER_ADMIN: only if assigned to this project
     * - CUSTOMER: only if they are the project owner
     */
    private boolean hasProjectAccess(User user, InteriorProject project) {
        return switch (user.getRole()) {
            case OWNER_ADMIN -> true;
            case MANAGER_ADMIN -> project.getManager() != null
                    && project.getManager().getId().equals(user.getId());
            case CUSTOMER -> project.getCustomer().getId().equals(user.getId());
        };
    }

    private String resolveContentType(String lowerName) {
        if (lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg")) return "image/jpeg";
        if (lowerName.endsWith(".png"))  return "image/png";
        if (lowerName.endsWith(".gif"))  return "image/gif";
        if (lowerName.endsWith(".pdf"))  return "application/pdf";
        return "application/octet-stream";
    }
}
