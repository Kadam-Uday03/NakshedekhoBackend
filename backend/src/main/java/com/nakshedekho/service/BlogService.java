package com.nakshedekho.service;

import com.nakshedekho.dto.BlogPostRequest;
import com.nakshedekho.model.BlogPost;
import com.nakshedekho.model.BlogStatus;
import com.nakshedekho.model.User;
import com.nakshedekho.repository.BlogPostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BlogService {

    private final BlogPostRepository blogPostRepository;

    // ─── PUBLIC ─────────────────────────────────────────────────────────────────

    /** Paginated list of published posts (optionally filtered by category) */
    public Page<BlogPost> getPublishedPosts(int page, int size, String category) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("publishedAt").descending());
        if (category != null && !category.isBlank()) {
            return blogPostRepository.findByStatusAndCategoryOrderByPublishedAtDesc(
                    BlogStatus.PUBLISHED, category, pageable);
        }
        return blogPostRepository.findByStatusOrderByPublishedAtDesc(BlogStatus.PUBLISHED, pageable);
    }

    /** Find a single published post by slug and increment view count */
    public BlogPost getPublishedPostBySlug(String slug) {
        BlogPost post = blogPostRepository.findBySlugAndStatus(slug, BlogStatus.PUBLISHED)
                .orElseThrow(() -> new RuntimeException("Blog post not found: " + slug));
        post.setViewCount(post.getViewCount() == null ? 1 : post.getViewCount() + 1);
        return blogPostRepository.save(post);
    }

    /** Full-text search on published posts */
    public Page<BlogPost> searchPublishedPosts(String query, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("publishedAt").descending());
        return blogPostRepository.searchPublished(query, pageable);
    }

    // ─── AUTHENTICATED (Author / Owner) ─────────────────────────────────────────

    /** Get all posts by the currently logged-in author */
    public List<BlogPost> getMyPosts(Long authorId) {
        return blogPostRepository.findByAuthorIdOrderByCreatedAtDesc(authorId);
    }

    /** Get any post by slug (for owner/author preview of drafts too) */
    public BlogPost getPostBySlug(String slug) {
        return blogPostRepository.findBySlug(slug)
                .orElseThrow(() -> new RuntimeException("Blog post not found: " + slug));
    }

    /** Get any post by ID */
    public BlogPost getPostById(Long id) {
        return blogPostRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Blog post not found: " + id));
    }

    /** Create a new blog post */
    public BlogPost createPost(BlogPostRequest request, User author) {
        BlogPost post = new BlogPost();
        populatePost(post, request);
        post.setAuthor(author);

        if (post.getStatus() == BlogStatus.PUBLISHED && post.getPublishedAt() == null) {
            post.setPublishedAt(LocalDateTime.now());
        }

        return blogPostRepository.save(post);
    }

    /** Update an existing post – only author or OWNER_ADMIN can call this */
    public BlogPost updatePost(Long id, BlogPostRequest request, User currentUser) {
        BlogPost post = getPostById(id);

        // Only OWNER_ADMIN can edit anyone's post; all others must be the author
        boolean isOwner = currentUser.getRole().name().equals("OWNER_ADMIN");
        boolean isAuthor = post.getAuthor().getId().equals(currentUser.getId());

        if (!isOwner && !isAuthor) {
            throw new RuntimeException("You are not allowed to edit this post.");
        }

        // If being published for the first time, stamp publishedAt
        if (request.getStatus() == BlogStatus.PUBLISHED && post.getPublishedAt() == null) {
            post.setPublishedAt(LocalDateTime.now());
        }

        populatePost(post, request);
        return blogPostRepository.save(post);
    }

    /** Delete a post – only author or OWNER_ADMIN can call this */
    public void deletePost(Long id, User currentUser) {
        BlogPost post = getPostById(id);

        boolean isOwner = currentUser.getRole().name().equals("OWNER_ADMIN");
        boolean isAuthor = post.getAuthor().getId().equals(currentUser.getId());

        if (!isOwner && !isAuthor) {
            throw new RuntimeException("You are not allowed to delete this post.");
        }
        blogPostRepository.deleteById(id);
    }

    // ─── OWNER / ADMIN ONLY ──────────────────────────────────────────────────────

    /** Get all posts (any status) – for admin dashboard */
    public List<BlogPost> getAllPosts() {
        return blogPostRepository.findAll(Sort.by("createdAt").descending());
    }

    /** Force-delete any post as owner */
    public void adminDeletePost(Long id) {
        blogPostRepository.deleteById(id);
    }

    // ─── HELPERS ─────────────────────────────────────────────────────────────────

    private void populatePost(BlogPost post, BlogPostRequest req) {
        if (req.getTitle() != null) post.setTitle(req.getTitle());
        if (req.getContent() != null) post.setContent(req.getContent());
        if (req.getExcerpt() != null) post.setExcerpt(req.getExcerpt());
        if (req.getCoverImageUrl() != null) post.setCoverImageUrl(req.getCoverImageUrl());
        if (req.getTags() != null) post.setTags(req.getTags());
        if (req.getCategory() != null) post.setCategory(req.getCategory());
        if (req.getStatus() != null) post.setStatus(req.getStatus());

        // Handle slug
        if (req.getSlug() != null && !req.getSlug().isBlank()) {
            post.setSlug(req.getSlug());
        } else if (req.getTitle() != null && (post.getSlug() == null || post.getSlug().isBlank())) {
            String baseSlug = BlogPost.generateSlug(req.getTitle());
            post.setSlug(ensureUniqueSlug(baseSlug, post.getId()));
        }
    }

    private String ensureUniqueSlug(String baseSlug, Long excludeId) {
        String slug = baseSlug;
        int counter = 1;
        while (blogPostRepository.existsBySlug(slug)) {
            // If the slug belongs to the post being updated, it's fine
            if (excludeId != null) {
                BlogPost existing = blogPostRepository.findBySlug(slug).orElse(null);
                if (existing != null && existing.getId().equals(excludeId)) break;
            }
            slug = baseSlug + "-" + counter++;
        }
        return slug;
    }
}
