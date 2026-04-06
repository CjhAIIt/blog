package com.example.blog.service;

import com.example.blog.model.Plan;
import com.example.blog.model.PlanAccessType;
import com.example.blog.model.PostStatus;
import com.example.blog.model.User;
import com.example.blog.repository.PlanRepository;
import com.example.blog.repository.PostRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class PlanService {
    private final PlanRepository planRepository;
    private final PostRepository postRepository;

    public PlanService(PlanRepository planRepository, PostRepository postRepository) {
        this.planRepository = planRepository;
        this.postRepository = postRepository;
    }

    public Page<Plan> getPublicPlans(Pageable pageable) {
        return planRepository.findByIsPublicTrue(pageable);
    }

    public Page<Plan> getCollaborativePlans(Pageable pageable) {
        return planRepository.findByAccessTypeAndIsPublicTrue(PlanAccessType.COLLABORATIVE, pageable);
    }

    public Page<Plan> getUserPlans(Long authorId, Pageable pageable) {
        return planRepository.findByAuthorIdOrderByUpdatedAtDesc(authorId, pageable);
    }

    public List<Plan> getUserPlans(Long authorId) {
        return planRepository.findByAuthorIdOrderByCreatedAtDesc(authorId);
    }

    public List<Plan> getJoinablePlans(Long userId) {
        return planRepository.findJoinablePlansForUser(userId);
    }

    public Plan save(Plan plan) {
        normalizePlan(plan);
        plan.setUpdatedAt(LocalDateTime.now());
        return planRepository.save(plan);
    }

    public Optional<Plan> findById(Long id) {
        return planRepository.findById(id);
    }

    public Optional<Plan> findAccessibleById(Long id, User user) {
        return findById(id).filter(plan -> plan.isPublic() || canManage(plan, user));
    }

    public int getPublishedPostCount(Long planId) {
        return postRepository.countByPlanIdAndStatus(planId, PostStatus.PUBLISHED);
    }

    public boolean canManage(Plan plan, User user) {
        if (plan == null || user == null) {
            return false;
        }
        if (user.isAdmin()) {
            return true;
        }
        return plan.getAuthor() != null
                && plan.getAuthor().getId() != null
                && plan.getAuthor().getId().equals(user.getId());
    }

    public boolean canJoin(Plan plan, User user) {
        if (plan == null || user == null) {
            return false;
        }
        return canManage(plan, user) || plan.isCollaborative();
    }

    private void normalizePlan(Plan plan) {
        if (plan == null) {
            return;
        }
        plan.setAccessType(plan.getAccessType());
    }
}
