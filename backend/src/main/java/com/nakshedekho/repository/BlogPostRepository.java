package com.nakshedekho.repository;

import com.nakshedekho.model.BlogPost;
import com.nakshedekho.model.BlogStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BlogPostRepository extends JpaRepository<BlogPost, Long> {

    // Find all published posts (with pagination)
    Page<BlogPost> findByStatusOrderByPublishedAtDesc(BlogStatus status, Pageable pageable);

    // Find by slug (public)
    Optional<BlogPost> findBySlugAndStatus(String slug, BlogStatus status);

    // Find by slug (any status - for author/admin)
    Optional<BlogPost> findBySlug(String slug);

    // Find all posts by a specific author
    List<BlogPost> findByAuthorIdOrderByCreatedAtDesc(Long authorId);

    // Find published posts by category
    Page<BlogPost> findByStatusAndCategoryOrderByPublishedAtDesc(BlogStatus status, String category, Pageable pageable);

    // Search published posts by title or content
    @Query("SELECT b FROM BlogPost b WHERE b.status = 'PUBLISHED' AND " +
           "(LOWER(b.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(b.content) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(b.excerpt) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<BlogPost> searchPublished(@Param("query") String query, Pageable pageable);

    // Check slug uniqueness
    boolean existsBySlug(String slug);

    // Count by author
    long countByAuthorId(Long authorId);
}
