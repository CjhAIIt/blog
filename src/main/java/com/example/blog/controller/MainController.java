package com.example.blog.controller;

import com.example.blog.model.Post;
import com.example.blog.model.User;
import com.example.blog.service.PostService;
import com.example.blog.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class MainController {
    
    @Autowired
    private PostService postService;
    
    @Autowired
    private UserService userService;
    
    @GetMapping("/")
    public String home(@RequestParam(defaultValue = "0") int page, Model model) {
        Pageable pageable = PageRequest.of(page, 5); // 每页显示5篇文章
        Page<Post> postsPage = postService.findAll(pageable);
        model.addAttribute("posts", postsPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", postsPage.getTotalPages());
        model.addAttribute("totalItems", postsPage.getTotalElements());
        return "index";
    }
    
    @GetMapping("/about")
    public String about() {
        return "about";
    }
    
    @GetMapping("/login")
    public String login() {
        return "login";
    }
    
    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("user", new User());
        return "register";
    }
    
    @PostMapping("/register")
    public String registerUser(@ModelAttribute("user") User user, 
                              BindingResult result, 
                              @RequestParam("confirmPassword") String confirmPassword,
                              RedirectAttributes redirectAttributes) {
        // 手动验证用户名长度
        if (user.getUsername() == null || user.getUsername().trim().length() < 3 || user.getUsername().trim().length() > 20) {
            result.rejectValue("username", "error.user", "用户名长度应在3-20个字符之间");
        }
        
        // 手动验证密码长度
        if (user.getPassword() == null || user.getPassword().length() < 6) {
            result.rejectValue("password", "error.user", "密码长度至少6个字符");
        }
        
        // 手动验证邮箱格式
        if (user.getEmail() == null || !user.getEmail().contains("@") || !user.getEmail().contains(".")) {
            result.rejectValue("email", "error.user", "请输入有效的电子邮箱地址");
        }
        
        // 验证密码确认
        if (!user.getPassword().equals(confirmPassword)) {
            result.rejectValue("password", "error.user", "密码不匹配");
        }
        
        // 检查用户名是否已存在
        if (userService.existsByUsername(user.getUsername())) {
            result.rejectValue("username", "error.user", "用户名已存在");
        }
        
        // 检查邮箱是否已存在
        if (userService.existsByEmail(user.getEmail())) {
            result.rejectValue("email", "error.user", "邮箱已被使用");
        }
        
        if (result.hasErrors()) {
            return "register";
        }
        
        // 保存用户
        try {
            User registeredUser = userService.registerUser(user.getUsername(), user.getEmail(), user.getPassword());
            // 注册成功，可以在这里对返回的用户对象进行额外处理
        } catch (RuntimeException e) {
            result.rejectValue("username", "error.user", e.getMessage());
            return "register";
        }
        
        // 添加成功消息
        redirectAttributes.addFlashAttribute("successMessage", "注册成功！请登录。");
        
        return "redirect:/login";
    }
    
    @GetMapping("/search")
    public String search(@RequestParam String keyword, 
                        @RequestParam(defaultValue = "0") int page, 
                        Model model) {
        Pageable pageable = PageRequest.of(page, 5); // 每页显示5篇文章
        Page<Post> postsPage = postService.findByKeyword(keyword, pageable);
        model.addAttribute("posts", postsPage.getContent());
        model.addAttribute("keyword", keyword);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", postsPage.getTotalPages());
        model.addAttribute("totalItems", postsPage.getTotalElements());
        return "search";
    }
}