package com.example.blog.controller;

import com.example.blog.model.Post;
import com.example.blog.model.User;
import com.example.blog.service.PostService;
import com.example.blog.service.UserService;
import com.example.blog.service.ViewModeService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
public class AdminController {
    private final UserService userService;
    private final PostService postService;
    private final ViewModeService viewModeService;

    public AdminController(UserService userService, PostService postService, ViewModeService viewModeService) {
        this.userService = userService;
        this.postService = postService;
        this.viewModeService = viewModeService;
    }

    @GetMapping("/users")
    public String users(@RequestParam(defaultValue = "") String keyword,
                        Model model,
                        Principal principal,
                        RedirectAttributes redirectAttributes) {
        User admin = requireAdmin(principal, redirectAttributes);
        if (admin == null) {
            return "redirect:/";
        }

        List<User> allUsers = userService.findAll();
        if (keyword != null && !keyword.isBlank()) {
            String lowerKeyword = keyword.trim().toLowerCase();
            allUsers = allUsers.stream()
                    .filter(u -> u.getUsername().toLowerCase().contains(lowerKeyword)
                            || u.getEmail().toLowerCase().contains(lowerKeyword)
                            || (u.getRealName() != null && u.getRealName().toLowerCase().contains(lowerKeyword)))
                    .collect(Collectors.toList());
        }

        // Sort: admins first, then by published post count descending
        allUsers.sort(Comparator.<User, Boolean>comparing(u -> !u.isAdmin())
                .thenComparing(Comparator.<User, Long>comparing(u -> postService.countPublishedByAuthorId(u.getId())).reversed()));

        List<UserInfo> userInfos = allUsers.stream()
                .map(u -> new UserInfo(
                        u,
                        postService.countPublishedByAuthorId(u.getId()),
                        postService.countDraftsByAuthorId(u.getId())
                ))
                .collect(Collectors.toList());
        long activeUserCount = userInfos.stream().filter(info -> info.getPublishedCount() > 0).count();
        long totalPublishedCount = userInfos.stream().mapToLong(UserInfo::getPublishedCount).sum();

        model.addAttribute("currentUser", admin);
        model.addAttribute("userInfos", userInfos);
        model.addAttribute("totalUsers", userInfos.size());
        model.addAttribute("activeUserCount", activeUserCount);
        model.addAttribute("totalPublishedCount", totalPublishedCount);
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedCategory", "latest");
        return view("admin/users");
    }

    public static class UserInfo {
        private final User user;
        private final long publishedCount;
        private final long draftCount;

        public UserInfo(User user, long publishedCount, long draftCount) {
            this.user = user;
            this.publishedCount = publishedCount;
            this.draftCount = draftCount;
        }

        public User getUser() { return user; }
        public long getPublishedCount() { return publishedCount; }
        public long getDraftCount() { return draftCount; }
    }

    @PostMapping("/users/{id}/password")
    public String resetUserPassword(@PathVariable Long id,
                                    @RequestParam String newPassword,
                                    @RequestParam String confirmPassword,
                                    Principal principal,
                                    RedirectAttributes redirectAttributes) {
        User admin = requireAdmin(principal, redirectAttributes);
        if (admin == null) {
            return "redirect:/";
        }

        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("error", "两次输入的新密码不一致");
            return "redirect:/admin/users";
        }

        try {
            User user = userService.resetPassword(id, newPassword);
            redirectAttributes.addFlashAttribute("message", "已为 @" + user.getUsername() + " 重置密码");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @GetMapping("/verifications")
    public String verifications(Model model, Principal principal, RedirectAttributes redirectAttributes) {
        User admin = requireAdmin(principal, redirectAttributes);
        if (admin == null) {
            return "redirect:/";
        }

        model.addAttribute("currentUser", admin);
        model.addAttribute("pendingUsers", userService.findPendingRealNameVerifications());
        model.addAttribute("reviewedUsers", userService.findReviewedRealNameVerifications());
        model.addAttribute("selectedCategory", "latest");
        return view("admin/verifications");
    }

    @PostMapping("/verifications/{id}/approve")
    public String approveVerification(@PathVariable Long id, Principal principal, RedirectAttributes redirectAttributes) {
        User admin = requireAdmin(principal, redirectAttributes);
        if (admin == null) {
            return "redirect:/";
        }

        User approvedUser = userService.approveRealNameVerification(id);
        redirectAttributes.addFlashAttribute("message", approvedUser.getDisplayName() + " 的实名资料已审核通过");
        return "redirect:/admin/verifications";
    }

    @PostMapping("/verifications/{id}/reject")
    public String rejectVerification(@PathVariable Long id, Principal principal, RedirectAttributes redirectAttributes) {
        User admin = requireAdmin(principal, redirectAttributes);
        if (admin == null) {
            return "redirect:/";
        }

        User rejectedUser = userService.rejectRealNameVerification(id);
        redirectAttributes.addFlashAttribute("message", rejectedUser.getDisplayName() + " 的实名资料已驳回");
        return "redirect:/admin/verifications";
    }

    @GetMapping("/posts")
    public String adminPosts(Model model, Principal principal, RedirectAttributes redirectAttributes) {
        User admin = requireAdmin(principal, redirectAttributes);
        if (admin == null) {
            return "redirect:/";
        }
        model.addAttribute("currentUser", admin);
        model.addAttribute("allPosts", postService.findAllIncludingDrafts());
        model.addAttribute("selectedCategory", "latest");
        return view("admin/posts");
    }

    @PostMapping("/posts/{id}/pin")
    public String togglePin(@PathVariable Long id, Principal principal, RedirectAttributes redirectAttributes) {
        User admin = requireAdmin(principal, redirectAttributes);
        if (admin == null) {
            return "redirect:/";
        }
        try {
            Post post = postService.findById(id).orElseThrow(() -> new RuntimeException("文章不存在"));
            postService.setPinned(id, !post.isPinned());
            redirectAttributes.addFlashAttribute("message", post.isPinned() ? "已取消置顶" : "已置顶");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/posts";
    }

    @PostMapping("/posts/{id}/feature")
    public String toggleFeature(@PathVariable Long id, Principal principal, RedirectAttributes redirectAttributes) {
        User admin = requireAdmin(principal, redirectAttributes);
        if (admin == null) {
            return "redirect:/";
        }
        try {
            Post post = postService.findById(id).orElseThrow(() -> new RuntimeException("文章不存在"));
            postService.setFeatured(id, !post.isFeatured());
            redirectAttributes.addFlashAttribute("message", post.isFeatured() ? "已取消精选" : "已设为精选");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/posts";
    }

    private User requireAdmin(Principal principal, RedirectAttributes redirectAttributes) {
        if (principal == null) {
            redirectAttributes.addFlashAttribute("error", "请先登录管理员账号");
            return null;
        }

        User currentUser = userService.getByUsername(principal.getName());
        if (!currentUser.isAdmin()) {
            redirectAttributes.addFlashAttribute("error", "只有管理员可以访问该页面");
            return null;
        }
        return currentUser;
    }

    private String view(String name) {
        return viewModeService.view(name);
    }
}
