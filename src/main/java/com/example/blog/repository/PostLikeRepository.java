package com.example.blog.repository;

import com.example.blog.model.PostLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PostLikeRepository extends JpaRepository<PostLike, Long> {
    boolean existsByUserIdAndPostId(Long userId, Long postId);

    long countByPostId(Long postId);

    void deleteByPostId(Long postId);
}
