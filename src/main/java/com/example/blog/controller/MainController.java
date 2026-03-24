package com.example.blog.controller;

import com.example.blog.model.RankingPeriod;
import com.example.blog.model.Post;
import com.example.blog.model.PostCategory;
import com.example.blog.model.User;
import com.example.blog.service.PostService;
import com.example.blog.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
public class MainController {
    private final PostService postService;
    private final UserService userService;

    public MainController(PostService postService, UserService userService) {
        this.postService = postService;
        this.userService = userService;
    }

    @GetMapping("/")
    public String home(@RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "latest") String category,
                       @RequestParam(defaultValue = "week") String rankPeriod,
                       Model model) {
        Pageable pageable = PageRequest.of(page, 6);
        Optional<PostCategory> selectedCategory = PostCategory.fromSlug(category);
        RankingPeriod selectedRankingPeriod = RankingPeriod.fromSlug(rankPeriod);
        Page<Post> postsPage = selectedCategory
                .map(postCategory -> postService.findByCategory(postCategory, pageable))
                .orElseGet(() -> postService.findAll(pageable));

        model.addAttribute("posts", postsPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", postsPage.getTotalPages());
        model.addAttribute("totalItems", postsPage.getTotalElements());
        model.addAttribute("selectedCategory", selectedCategory.map(PostCategory::getSlug).orElse("latest"));
        model.addAttribute("selectedRankingPeriod", selectedRankingPeriod);
        model.addAttribute("rankingPeriods", RankingPeriod.values());
        model.addAttribute("contributionLeaderboard", postService.findContributionLeaderboard(selectedRankingPeriod, 8));
        model.addAttribute("likeLeaderboard", postService.findLikeLeaderboard(selectedRankingPeriod, 8));
        return "index";
    }

    @GetMapping("/about")
    public String about(Model model) {
        model.addAttribute("selectedCategory", "latest");
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
                               @RequestParam("confirmPassword") String confirmPassword,
                               RedirectAttributes redirectAttributes,
                               Model model) {
        validateRegistration(user, confirmPassword, model);
        if (model.containsAttribute("error")) {
            return "register";
        }

        try {
            userService.registerUser(user.getUsername(), user.getEmail(), user.getPassword());
            redirectAttributes.addFlashAttribute("successMessage", "注册成功，现在可以直接登录。");
            return "redirect:/login";
        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            return "register";
        }
    }

    @GetMapping("/search")
    public String search(@RequestParam String keyword,
                         @RequestParam(defaultValue = "0") int page,
                         Model model) {
        Pageable pageable = PageRequest.of(page, 6);
        Page<Post> postsPage = postService.findByKeyword(keyword, pageable);
        model.addAttribute("posts", postsPage.getContent());
        model.addAttribute("keyword", keyword);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", postsPage.getTotalPages());
        model.addAttribute("totalItems", postsPage.getTotalElements());
        model.addAttribute("selectedCategory", "latest");
        return "search";
    }

    private void validateRegistration(User user, String confirmPassword, Model model) {
        if (!StringUtils.hasText(user.getUsername()) || user.getUsername().trim().length() < 3 || user.getUsername().trim().length() > 20) {
            model.addAttribute("error", "用户名长度应在 3 到 20 个字符之间");
            return;
        }
        if (!StringUtils.hasText(user.getPassword()) || user.getPassword().length() < 8) {
            model.addAttribute("error", "密码长度至少 8 位");
            return;
        }
        if (!StringUtils.hasText(user.getEmail()) || !user.getEmail().contains("@") || !user.getEmail().contains(".")) {
            model.addAttribute("error", "请输入有效的邮箱地址");
            return;
        }
        if (!user.getPassword().equals(confirmPassword)) {
            model.addAttribute("error", "两次输入的密码不一致");
        }
    }
}
