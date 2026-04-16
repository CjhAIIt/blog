package com.example.blog.config;

import com.example.blog.model.Post;
import com.example.blog.model.PostCategory;
import com.example.blog.model.PostStatus;
import com.example.blog.model.RealNameVerificationStatus;
import com.example.blog.model.User;
import com.example.blog.model.UserRole;
import com.example.blog.service.CommentService;
import com.example.blog.service.PostService;
import com.example.blog.service.UserService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class DataInitializer implements CommandLineRunner {
    private final UserService userService;
    private final PostService postService;
    private final CommentService commentService;

    public DataInitializer(UserService userService, PostService postService, CommentService commentService) {
        this.userService = userService;
        this.postService = postService;
        this.commentService = commentService;
    }

    @Override
    public void run(String... args) {
        userService.findAll().forEach(existingUser -> {
            boolean changed = false;
            if (!existingUser.isEmailVerified()) {
                existingUser.setEmailVerified(true);
                changed = true;
            }
            if ("admin".equalsIgnoreCase(existingUser.getUsername()) && !existingUser.isAdmin()) {
                existingUser.setRole(UserRole.ADMIN);
                changed = true;
            }
            if (existingUser.isAdmin()
                    && existingUser.getRealNameVerificationStatus() != RealNameVerificationStatus.APPROVED) {
                approveLegacyVerification(existingUser);
                changed = true;
            }
            if (shouldInitializeLegacyVerification(existingUser)
                    && (existingUser.getRealNameVerificationStatus() == null
                    || (existingUser.getRealNameVerificationStatus() == RealNameVerificationStatus.PENDING
                    && existingUser.getRealNameVerificationSubmittedAt() == null))) {
                approveLegacyVerification(existingUser);
                changed = true;
            }
            if (changed) {
                userService.save(existingUser);
            }
        });

        postService.findAllIncludingDrafts().forEach(existingPost -> {
            boolean changed = false;
            if (existingPost.getCategory() == null) {
                existingPost.setCategory(PostCategory.PROJECT);
                changed = true;
            }
            if (existingPost.getStatus() == null) {
                existingPost.setStatus(PostStatus.PUBLISHED);
                changed = true;
            }
            if (changed) {
                postService.save(existingPost);
            }
        });

        User admin = userService.findByUsername("admin")
                .map(existing -> {
                    boolean changed = false;
                    if (!existing.isAdmin()) {
                        existing.setRole(UserRole.ADMIN);
                        changed = true;
                    }
                    if (existing.getRealNameVerificationStatus() != RealNameVerificationStatus.APPROVED) {
                        approveLegacyVerification(existing);
                        changed = true;
                    }
                    return changed ? userService.save(existing) : existing;
                })
                .orElseGet(() -> userService.createUser(
                        "admin",
                        "admin@example.com",
                        "password",
                        UserRole.ADMIN,
                        RealNameVerificationStatus.APPROVED
                ));
        if (admin.getBio() == null) {
            admin.setBio("默认管理员账号，可管理所有文章、评论和实名认证审核。");
            userService.updateProfile(admin, "管理员", admin.getBio(), "10001", "https://github.com/CjhAIIt", null);
        }

        if (userService.findAll().size() > 1 || !postService.findAllIncludingDrafts().isEmpty()) {
            return;
        }

        User user = userService.createUser(
                "user",
                "user@example.com",
                "password",
                UserRole.USER,
                RealNameVerificationStatus.APPROVED
        );
        user.setBio("默认普通用户账号，用于测试个人空间。");
        userService.updateProfile(user, "测试用户", user.getBio(), "10002", "https://github.com/CjhAIIt", null);

        Post welcomePost = postService.save(new Post(
                "欢迎来到博客系统",
                """
                # 欢迎来到博客系统

                这里现在支持 **Markdown** 写作、实时预览、草稿箱、评论互动和个人主页展示。
                """,
                PostCategory.PROJECT,
                admin
        ));
        welcomePost.setLikeCount(12);
        postService.save(welcomePost);

        Post springBootPost = postService.save(new Post(
                "Spring Boot 入门记录",
                """
                ## 为什么选择 Spring Boot

                - 快速搭建项目
                - 配置简单
                - 适合个人博客系统
                """,
                PostCategory.FRONTEND_BACKEND,
                admin
        ));
        springBootPost.setLikeCount(8);
        postService.save(springBootPost);

        Post algorithmPost = postService.save(new Post(
                "算法刷题周记",
                """
                ## 本周计划

                1. 继续整理二叉树题型
                2. 复盘动态规划模板
                3. 记录复杂度分析思路
                """,
                PostCategory.ALGORITHM,
                user
        ));
        algorithmPost.setLikeCount(5);
        postService.save(algorithmPost);

        commentService.save(welcomePost, user, "这一版已经有 Markdown、草稿箱和评论区了，整体完整很多。");
    }

    private void approveLegacyVerification(User user) {
        LocalDateTime referenceTime = user.getCreatedAt() == null ? LocalDateTime.now() : user.getCreatedAt();
        user.setRealNameVerificationStatus(RealNameVerificationStatus.APPROVED);
        if (user.getRealNameVerificationSubmittedAt() == null) {
            user.setRealNameVerificationSubmittedAt(referenceTime);
        }
        if (user.getRealNameVerificationReviewedAt() == null) {
            user.setRealNameVerificationReviewedAt(referenceTime);
        }
    }

    private boolean shouldInitializeLegacyVerification(User user) {
        return user != null && (user.isAdmin() || user.hasRealName());
    }
}
