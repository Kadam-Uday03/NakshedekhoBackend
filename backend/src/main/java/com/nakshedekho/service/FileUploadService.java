package com.nakshedekho.service;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
public class FileUploadService {

    private final Path fileStorageLocation;

    // SECURITY: Only allow safe file types — blocks .exe, .php, .jsp, .sh, .html etc.
    private static final Set<String> ALLOWED_EXTENSIONS = new HashSet<>(Arrays.asList(
            ".jpg", ".jpeg", ".png", ".gif", ".webp",
            ".pdf", ".doc", ".docx",
            ".dwg", ".dxf", ".skp"
    ));

    public FileUploadService() {
        this.fileStorageLocation = Paths.get("uploads").toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            throw new RuntimeException("Could not create the directory where the uploaded files will be stored.", ex);
        }
    }

    public String storeFile(MultipartFile file) {
        // ── Size validation ────────────────────────────────────────────────────
        // 20MB max per file — prevents DoS via large upload payloads
        final long MAX_FILE_SIZE = 20 * 1024 * 1024L;
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new RuntimeException("File too large. Maximum allowed size is 20MB.");
        }

        if (file.isEmpty()) {
            throw new RuntimeException("Cannot upload an empty file.");
        }

        String originalFileName = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
        String fileExtension = "";

        try {
            if (originalFileName.contains(".")) {
                fileExtension = originalFileName.substring(originalFileName.lastIndexOf(".")).toLowerCase();
            }

            // ── Extension whitelist ────────────────────────────────────────────
            if (!ALLOWED_EXTENSIONS.contains(fileExtension)) {
                throw new RuntimeException("File type not allowed: '" + fileExtension +
                        "'. Allowed: jpg, jpeg, png, gif, webp, pdf, doc, docx, dwg, dxf, skp");
            }

            // ── UUID filename (prevents path traversal via filename) ───────────
            String fileName = UUID.randomUUID().toString() + fileExtension;

            Path targetLocation = this.fileStorageLocation.resolve(fileName).normalize();

            // ── Canonical path check — ensures target is inside upload directory ─
            // Prevents path traversal even if normalize() is bypassed somehow
            if (!targetLocation.toFile().getCanonicalPath()
                    .startsWith(this.fileStorageLocation.toFile().getCanonicalPath())) {
                throw new RuntimeException("Path traversal attack detected — upload rejected.");
            }

            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            return fileName;

        } catch (IOException ex) {
            throw new RuntimeException("Could not store file. Please try again.", ex);
        }
    }

    public Path getFilePath(String fileName) {
        return this.fileStorageLocation.resolve(fileName).normalize();
    }

    public void deleteFile(String fileName) {
        try {
            Path filePath = this.fileStorageLocation.resolve(fileName).normalize();
            Files.deleteIfExists(filePath);
        } catch (IOException ex) {
            throw new RuntimeException("Could not delete file " + fileName, ex);
        }
    }
}
