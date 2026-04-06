package com.example.blog.repository;

import com.example.blog.model.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByPostIdOrderByCreatedAtAsc(Long postId);

    long countByPostId(Long postId);

    @Query("SELECT COUNT(c) FROM Comment c WHERE c.post.author.id = :authorId AND c.post.status = com.example.blog.model.PostStatus.PUBLISHED")
    long countByPostAuthorId(@Param("authorId") Long authorId);

    Optional<Comment> findByIdAndPostId(Long id, Long postId);

    void deleteByIdAndPostId(Long id, Long postId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    void deleteByPostId(Long postId);
}
