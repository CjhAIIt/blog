package com.example.blog.config;

import com.example.blog.model.Post;
import com.example.blog.model.PostCategory;
import com.example.blog.model.PostStatus;
import com.example.blog.model.User;
import com.example.blog.service.CommentService;
import com.example.blog.service.PostService;
import com.example.blog.service.UserService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

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
            if (!existingUser.isEmailVerified()) {
                existingUser.setEmailVerified(true);
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

        if (!userService.findAll().isEmpty()) {
            return;
        }

        User admin = userService.createUser("admin", "admin@example.com", "password");
        admin.setBio("默认管理员账号，用于初始化博客内容。");
        userService.updateProfile(admin, admin.getBio(), "10001", "https://github.com/CjhAIIt", null);

        User user = userService.createUser("user", "user@example.com", "password");
        user.setBio("默认普通用户账号，可用于测试个人空间。");
        userService.updateProfile(user, user.getBio(), "10002", "https://github.com/CjhAIIt", null);

        Post welcomePost = postService.save(new Post(
                "欢迎来到烂柯的博客",
                """
                # 欢迎来到烂柯的博客

                这里现在支持 **Markdown** 写作、实时预览、草稿箱、评论互动和个人主页展示。

                ```java
                System.out.println("Hello Markdown");
                ```
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

        Post embeddedPost = postService.save(new Post(
                "嵌入式学习清单",
                """
                ## 本周计划

                1. 继续补 STM32 外设基础
                2. 整理串口通信笔记
                3. 把实验过程写成文档
                """,
                PostCategory.EMBEDDED,
                user
        ));
        embeddedPost.setLikeCount(5);
        postService.save(embeddedPost);

        commentService.save(welcomePost, user, "这一版已经有 Markdown、草稿箱和评论区了，整体比之前完整很多。");
    }
}
