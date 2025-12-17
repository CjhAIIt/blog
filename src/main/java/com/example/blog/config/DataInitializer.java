package com.example.blog.config;

import com.example.blog.model.Post;
import com.example.blog.model.User;
import com.example.blog.service.PostService;
import com.example.blog.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private PostService postService;
    
    @Override
    public void run(String... args) throws Exception {
        if (userService.findAll().isEmpty()) {
            User admin = new User("admin", "password", "admin@example.com");
            userService.save(admin);
            
            User user = new User("user", "password", "user@example.com");
            userService.save(user);
            
            Post post1 = new Post(
                "欢迎来到我的博客",
                "这是我的第一篇博客文章。在这里我将分享我的想法和经验。",
                admin
            );
            postService.save(post1);
            
            Post post2 = new Post(
                "Spring Boot 入门",
                "Spring Boot 是一个用于创建独立的、生产级的 Spring 应用程序的框架。",
                admin
            );
            postService.save(post2);
            
            Post post3 = new Post(
                "Java 编程技巧",
                "在这篇文章中，我将分享一些实用的 Java 编程技巧。",
                user
            );
            postService.save(post3);
        }
    }
}