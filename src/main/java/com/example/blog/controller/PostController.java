package com.example.blog.controller;

import com.example.blog.model.Post;
import com.example.blog.model.User;
import com.example.blog.service.PostService;
import com.example.blog.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/posts")
public class PostController {
    
    @Autowired
    private PostService postService;
    
    @Autowired
    private UserService userService;
    
    @GetMapping
    public String listPosts(Model model) {
        List<Post> posts = postService.findAll();
        model.addAttribute("posts", posts);
        return "posts/list";
    }
    
    @GetMapping("/{id}")
    public String viewPost(@PathVariable Long id, Model model) {
        Optional<Post> post = postService.findById(id);
        if (post.isPresent()) {
            model.addAttribute("post", post.get());
            return "posts/view";
        }
        return "redirect:/posts";
    }
    
    @GetMapping("/new")
    public String newPostForm(Model model) {
        model.addAttribute("post", new Post());
        return "posts/form";
    }
    
    @GetMapping("/edit/{id}")
    public String editPostForm(@PathVariable Long id, Model model, Principal principal) {
        Optional<Post> post = postService.findById(id);
        if (post.isPresent()) {
            model.addAttribute("post", post.get());
            return "posts/form";
        }
        return "redirect:/posts";
    }
    
    @PostMapping
    public String savePost(@ModelAttribute Post post, Principal principal, RedirectAttributes redirectAttributes) {
        if (principal != null) {
            Optional<User> author = userService.findByUsername(principal.getName());
            if (author.isPresent()) {
                post.setAuthor(author.get());
                postService.save(post);
                redirectAttributes.addFlashAttribute("message", "文章发布成功！");
                return "redirect:/posts/" + post.getId();
            }
        }
        redirectAttributes.addFlashAttribute("error", "请先登录");
        return "redirect:/login";
    }
    
    @PostMapping("/update/{id}")
    public String updatePost(@PathVariable Long id, @ModelAttribute Post post, RedirectAttributes redirectAttributes) {
        Optional<Post> existingPost = postService.findById(id);
        if (existingPost.isPresent()) {
            Post updatedPost = existingPost.get();
            updatedPost.setTitle(post.getTitle());
            updatedPost.setContent(post.getContent());
            postService.save(updatedPost);
            redirectAttributes.addFlashAttribute("message", "文章更新成功！");
            return "redirect:/posts/" + id;
        }
        return "redirect:/posts";
    }
    
    @PostMapping("/delete/{id}")
    public String deletePost(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        postService.deleteById(id);
        redirectAttributes.addFlashAttribute("message", "文章删除成功！");
        return "redirect:/posts";
    }
    
    @GetMapping("/search")
    public String searchPosts(@RequestParam String keyword, Model model) {
        List<Post> posts = postService.findByKeyword(keyword);
        model.addAttribute("posts", posts);
        model.addAttribute("keyword", keyword);
        return "posts/list";
    }
}