package com.nakshedekho.controller;

import com.nakshedekho.dto.BlogPostRequest;
import com.nakshedekho.model.BlogPost;
import com.nakshedekho.model.BlogStatus;
import com.nakshedekho.model.Role;
import com.nakshedekho.model.User;
import com.nakshedekho.security.InputSanitizer;
import com.nakshedekho.service.BlogService;
import com.nakshedekho.service.FileUploadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class BlogController {

    private static final Logger logger = LoggerFactory.getLogger(BlogController.class);

    private final BlogService blogService;
    private final FileUploadService fileUploadService;

    // ══════════════════════════════════════════════════════════════════════════
    // PUBLIC ENDPOINTS  (no auth required)
    // ══════════════════════════════════════════════════════════════════════════

    /** GET /api/public/blogs?page=0&size=10&category=design */
    @GetMapping("/api/public/blogs")
    public ResponseEntity<Page<BlogPost>> getPublishedBlogs(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false)    String category) {
        // Clamp pagination to prevent DoS via huge page sizes
        int safePage = InputSanitizer.clampInt(page, 0, 1000);
        int safeSize = InputSanitizer.clampInt(size, 1, 50);
        String safeCategory = InputSanitizer.sanitizeText(category);
        return ResponseEntity.ok(blogService.getPublishedPosts(safePage, safeSize, safeCategory));
    }

    /** GET /api/public/blogs/{slug} – view a single published post by slug */
    @GetMapping("/api/public/blogs/{slug}")
    public ResponseEntity<BlogPost> getPublishedBlogBySlug(@PathVariable String slug) {
        String safeSlug = InputSanitizer.sanitizeSlug(slug);
        if (safeSlug == null || safeSlug.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(blogService.getPublishedPostBySlug(safeSlug));
    }

    /** GET /api/public/blogs/search?query=interior&page=0&size=10 */
    @GetMapping("/api/public/blogs/search")
    public ResponseEntity<Page<BlogPost>> searchBlogs(
            @RequestParam String query,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {
        // Sanitize search query to strip LIKE wildcards and control characters
        String safeQuery = InputSanitizer.sanitizeSearch(query);
        if (safeQuery == null || safeQuery.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        int safePage = InputSanitizer.clampInt(page, 0, 1000);
        int safeSize = InputSanitizer.clampInt(size, 1, 50);
        return ResponseEntity.ok(blogService.searchPublishedPosts(safeQuery, safePage, safeSize));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // AUTHENTICATED USER ENDPOINTS  (any logged-in: CUSTOMER / MANAGER / OWNER)
    // ══════════════════════════════════════════════════════════════════════════

    /** GET /api/blog/my – get all my blog posts (DRAFT + PUBLISHED) */
    @GetMapping("/api/blog/my")
    public ResponseEntity<List<BlogPost>> getMyBlogs(@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(blogService.getMyPosts(currentUser.getId()));
    }

    /**
     * GET /api/blog/{id} – get a post by ID.
     * IDOR FIX: Non-admin users can only read their own DRAFT posts.
     * Published posts are accessible to all authenticated users (needed for edit preview).
     * Owners/Managers can read all posts.
     */
    @GetMapping("/api/blog/{id}")
    public ResponseEntity<BlogPost> getBlogById(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {

        BlogPost post = blogService.getPostById(id);

        boolean isOwnerOrManager = currentUser.getRole() == Role.OWNER_ADMIN
                || currentUser.getRole() == Role.MANAGER_ADMIN;

        // Non-admin users can only see their own draft posts; published posts are fine
        if (!isOwnerOrManager
                && post.getStatus() == BlogStatus.DRAFT
                && !post.getAuthor().getId().equals(currentUser.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(post);
    }

    /** POST /api/blog – create a new blog post */
    @PostMapping("/api/blog")
    public ResponseEntity<BlogPost> createBlog(
            @Valid @RequestBody BlogPostRequest request,
            @AuthenticationPrincipal User currentUser) {
        sanitizeBlogRequest(request);
        BlogPost created = blogService.createPost(request, currentUser);
        return ResponseEntity.ok(created);
    }

    /**
     * PUT /api/blog/{id} – update a blog post.
     * IDOR FIX: Ownership enforced here AND in BlogService.
     * Only the author or an OWNER_ADMIN can update. MANAGER_ADMIN cannot edit other users' posts.
     */
    @PutMapping("/api/blog/{id}")
    public ResponseEntity<BlogPost> updateBlog(
            @PathVariable Long id,
            @Valid @RequestBody BlogPostRequest request,
            @AuthenticationPrincipal User currentUser) {

        BlogPost post = blogService.getPostById(id);

        boolean isOwner = currentUser.getRole() == Role.OWNER_ADMIN;
        boolean isAuthor = post.getAuthor().getId().equals(currentUser.getId());

        if (!isOwner && !isAuthor) {
            logger.warn("IDOR attempt: user {} tried to update blog {} owned by {}",
                    currentUser.getId(), id, post.getAuthor().getId());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        sanitizeBlogRequest(request);
        BlogPost updated = blogService.updatePost(id, request, currentUser);
        return ResponseEntity.ok(updated);
    }

    /**
     * DELETE /api/blog/{id} – delete a blog post.
     * IDOR FIX: Only the author or an OWNER_ADMIN can delete. MANAGER_ADMIN cannot delete other users' posts.
     */
    @DeleteMapping("/api/blog/{id}")
    public ResponseEntity<Void> deleteBlog(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {

        BlogPost post = blogService.getPostById(id);

        boolean isOwner = currentUser.getRole() == Role.OWNER_ADMIN;
        boolean isAuthor = post.getAuthor().getId().equals(currentUser.getId());

        if (!isOwner && !isAuthor) {
            logger.warn("IDOR attempt: user {} tried to delete blog {} owned by {}",
                    currentUser.getId(), id, post.getAuthor().getId());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        blogService.deletePost(id, currentUser);
        return ResponseEntity.ok().build();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Sanitizes all string fields in a BlogPostRequest before persistence:
     * - Strips control characters / null bytes from text fields
     * - Validates coverImageUrl — rejects javascript:/data: URIs
     * - Sanitizes slug to [a-z0-9-] only
     */
    private void sanitizeBlogRequest(BlogPostRequest request) {
        request.setTitle(InputSanitizer.sanitizeText(request.getTitle()));
        request.setExcerpt(InputSanitizer.sanitizeText(request.getExcerpt()));
        request.setCategory(InputSanitizer.sanitizeText(request.getCategory()));

        // Validate coverImageUrl — reject dangerous URI schemes
        if (request.getCoverImageUrl() != null) {
            String safeUrl = InputSanitizer.sanitizeUrl(request.getCoverImageUrl());
            if (safeUrl == null) {
                logger.warn("Rejected dangerous coverImageUrl in blog request");
                request.setCoverImageUrl(null);
            } else {
                request.setCoverImageUrl(safeUrl);
            }
        }

        // Sanitize slug — only [a-z0-9-]
        if (request.getSlug() != null) {
            request.setSlug(InputSanitizer.sanitizeSlug(request.getSlug()));
        }

        // Sanitize tags
        if (request.getTags() != null) {
            request.setTags(
                request.getTags().stream()
                    .map(InputSanitizer::sanitizeText)
                    .filter(t -> t != null && !t.isBlank())
                    .toList()
            );
        }
    }

    /** POST /api/blog/upload-image – upload cover image or inline image */
    @PostMapping("/api/blog/upload-image")
    public ResponseEntity<Map<String, String>> uploadBlogImage(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal User currentUser) {
        String fileName = fileUploadService.storeFile(file);
        String fileUrl = "/api/files/download/" + fileName;
        Map<String, String> response = new HashMap<>();
        response.put("url", fileUrl);
        return ResponseEntity.ok(response);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // OWNER / ADMIN ENDPOINTS
    // ══════════════════════════════════════════════════════════════════════════

    /** GET /api/owner/blogs – all posts regardless of status (admin dashboard) */
    @GetMapping("/api/owner/blogs")
    public ResponseEntity<List<BlogPost>> getAllBlogsForAdmin() {
        return ResponseEntity.ok(blogService.getAllPosts());
    }

    /** DELETE /api/owner/blogs/{id} – force-delete any post */
    @DeleteMapping("/api/owner/blogs/{id}")
    public ResponseEntity<Void> adminDeleteBlog(@PathVariable Long id) {
        blogService.adminDeletePost(id);
        return ResponseEntity.ok().build();
    }
}
