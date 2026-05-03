package com.example.blog.controller;

import com.example.blog.model.Post;
import com.example.blog.model.PostCategory;
import com.example.blog.model.RankingPeriod;
import com.example.blog.model.User;
import com.example.blog.service.PostService;
import com.example.blog.service.UserService;
import com.example.blog.service.ViewModeService;
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
    private final ViewModeService viewModeService;

    public MainController(PostService postService, UserService userService, ViewModeService viewModeService) {
        this.postService = postService;
        this.userService = userService;
        this.viewModeService = viewModeService;
    }

    @GetMapping("/")
    public String home(@RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "latest") String category,
                       Model model) {
        Pageable pageable = PageRequest.of(page, 6);
        Optional<PostCategory> selectedCategory = PostCategory.fromSlug(category);
        Page<Post> postsPage = selectedCategory
                .map(postCategory -> postService.findByCategory(postCategory, pageable))
                .orElseGet(() -> postService.findAll(pageable));

        model.addAttribute("posts", postsPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", postsPage.getTotalPages());
        model.addAttribute("totalItems", postsPage.getTotalElements());
        model.addAttribute("selectedCategory", selectedCategory.map(PostCategory::getSlug).orElse("latest"));
        model.addAttribute("featuredPosts", postService.findLikeLeaderboard(RankingPeriod.MONTH, 3));
        model.addAttribute("pinnedPosts", postService.findPinnedPosts());
        model.addAttribute("topFeaturedPosts", postService.findFeaturedPosts(5));
        return view("index");
    }

    @GetMapping("/leaderboards")
    public String leaderboards(@RequestParam(defaultValue = "week") String rankPeriod, Model model) {
        RankingPeriod selectedRankingPeriod = RankingPeriod.fromSlug(rankPeriod);
        int contributionLimit = Math.toIntExact(Math.min(Integer.MAX_VALUE, Math.max(10, userService.countUsers())));
        model.addAttribute("selectedRankingPeriod", selectedRankingPeriod);
        model.addAttribute("rankingPeriods", RankingPeriod.values());
        model.addAttribute("contributionLeaderboard", postService.findContributionLeaderboard(selectedRankingPeriod, contributionLimit));
        model.addAttribute("likeLeaderboard", postService.findLikeLeaderboard(selectedRankingPeriod, 10));
        return view("leaderboards");
    }

    @GetMapping("/about")
    public String about(Model model) {
        model.addAttribute("selectedCategory", "latest");
        return view("about");
    }

    @GetMapping("/login")
    public String login() {
        return view("login");
    }

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("user", new User());
        return view("register");
    }

    @PostMapping("/register")
    public String registerUser(@ModelAttribute("user") User user,
                               @RequestParam("confirmPassword") String confirmPassword,
                               RedirectAttributes redirectAttributes,
                               Model model) {
        validateRegistration(user, confirmPassword, model);
        if (model.containsAttribute("error")) {
            return view("register");
        }

        try {
            userService.registerUser(user.getUsername(), user.getEmail(), user.getPassword(), user.getRealName());
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "注册成功。真实姓名现在是选填项，不填写也可以直接发文；如果你填写了实名信息，后台会继续审核。"
            );
            return "redirect:/login";
        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            return view("register");
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
        return view("search");
    }

    private String view(String name) {
        return viewModeService.view(name);
    }

    private void validateRegistration(User user, String confirmPassword, Model model) {
        if (!StringUtils.hasText(user.getUsername()) || user.getUsername().trim().length() < 3 || user.getUsername().trim().length() > 20) {
            model.addAttribute("error", "用户名长度应在 3 到 20 个字符之间");
            return;
        }
        if (StringUtils.hasText(user.getRealName())) {
            try {
                userService.normalizeRealName(user.getRealName());
            } catch (IllegalArgumentException e) {
                model.addAttribute("error", e.getMessage());
                return;
            }
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
