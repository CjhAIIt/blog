package com.example.blog.repository;

import com.example.blog.model.Plan;
import com.example.blog.model.PlanAccessType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlanRepository extends JpaRepository<Plan, Long> {
    Page<Plan> findByIsPublicTrue(Pageable pageable);

    Page<Plan> findByAccessTypeAndIsPublicTrue(PlanAccessType accessType, Pageable pageable);

    Page<Plan> findByAuthorIdOrderByUpdatedAtDesc(Long authorId, Pageable pageable);

    List<Plan> findByAuthorIdOrderByCreatedAtDesc(Long authorId);

    @Query("""
            SELECT p FROM Plan p
            WHERE p.author.id = :userId OR p.accessType = com.example.blog.model.PlanAccessType.COLLABORATIVE
            ORDER BY p.updatedAt DESC
            """)
    List<Plan> findJoinablePlansForUser(@Param("userId") Long userId);
}
