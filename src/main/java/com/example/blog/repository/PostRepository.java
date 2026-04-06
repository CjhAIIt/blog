package com.example.blog.repository;

import com.example.blog.dto.ContributionLeaderboardEntry;
import com.example.blog.model.Post;
import com.example.blog.model.PostCategory;
import com.example.blog.model.PostStatus;
import com.example.blog.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PostRepository extends JpaRepository<Post, Long>, JpaSpecificationExecutor<Post> {
    List<Post> findByAuthorIdAndStatusOrderByCreatedAtDesc(Long authorId, PostStatus status);

    Page<Post> findByAuthorIdAndStatusOrderByCreatedAtDesc(Long authorId, PostStatus status, Pageable pageable);

    List<Post> findByAuthorIdAndStatusOrderByUpdatedAtDesc(Long authorId, PostStatus status);

    List<Post> findByAuthorOrderByCreatedAtDesc(User author);

    List<Post> findByCategoryAndStatusOrderByCreatedAtDesc(PostCategory category, PostStatus status);

    Page<Post> findByCategoryAndStatusOrderByCreatedAtDesc(PostCategory category, PostStatus status, Pageable pageable);

    List<Post> findByStatusOrderByCreatedAtDesc(PostStatus status);

    Page<Post> findByStatusOrderByCreatedAtDesc(PostStatus status, Pageable pageable);

    List<Post> findByStatusAndScheduledPublishAtLessThanEqualOrderByScheduledPublishAtAsc(PostStatus status, LocalDateTime scheduledPublishAt);

    @Query("""
            SELECT p FROM Post p
            WHERE p.status = com.example.blog.model.PostStatus.PUBLISHED
            AND (p.title LIKE %:keyword% OR p.content LIKE %:keyword%)
            ORDER BY p.createdAt DESC
            """)
    List<Post> findByKeyword(@Param("keyword") String keyword);

    @Query("""
            SELECT p FROM Post p
            WHERE p.status = com.example.blog.model.PostStatus.PUBLISHED
            AND (p.title LIKE %:keyword% OR p.content LIKE %:keyword%)
            ORDER BY p.createdAt DESC
            """)
    Page<Post> findByKeyword(@Param("keyword") String keyword, Pageable pageable);

    @Query("""
            SELECT new com.example.blog.dto.ContributionLeaderboardEntry(
                p.author.id,
                p.author.username,
                COUNT(p.id)
            )
            FROM Post p
            WHERE p.status = com.example.blog.model.PostStatus.PUBLISHED
            AND p.createdAt >= :startAt
            GROUP BY p.author.id, p.author.username
            ORDER BY COUNT(p.id) DESC, MAX(p.createdAt) DESC
            """)
    List<ContributionLeaderboardEntry> findContributionLeaderboard(@Param("startAt") LocalDateTime startAt, Pageable pageable);

    @Query("""
            SELECT p FROM Post p
            WHERE p.status = com.example.blog.model.PostStatus.PUBLISHED
            AND p.createdAt >= :startAt
            ORDER BY COALESCE(p.likeCount, 0) DESC, p.createdAt DESC
            """)
    List<Post> findTopByLikesSince(@Param("startAt") LocalDateTime startAt, Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Post p SET p.likeCount = COALESCE(p.likeCount, 0) + 1 WHERE p.id = :id")
    int incrementLikeCount(@Param("id") Long id);

    @Query("""
            SELECT p FROM Post p
            WHERE p.id = :id
            AND (p.status = com.example.blog.model.PostStatus.PUBLISHED
                 OR (p.status = com.example.blog.model.PostStatus.SCHEDULED AND p.scheduledPublishAt <= :now))
            """)
    Optional<Post> findPublicById(@Param("id") Long id, @Param("now") LocalDateTime now);
    List<Post> findByPlanIdOrderByPlanOrderAsc(Long planId);

    List<Post> findByPlanIdAndStatusOrderByPlanOrderAsc(Long planId, PostStatus status);

    int countByPlanIdAndStatus(Long planId, PostStatus status);
}
