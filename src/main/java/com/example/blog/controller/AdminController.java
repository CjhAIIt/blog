package com.example.blog.controller;

import com.example.blog.model.User;
import com.example.blog.service.UserService;
import com.example.blog.service.ViewModeService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequestMapping("/admin")
public class AdminController {
    private final UserService userService;
    private final ViewModeService viewModeService;

    public AdminController(UserService userService, ViewModeService viewModeService) {
        this.userService = userService;
        this.viewModeService = viewModeService;
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
        redirectAttributes.addFlashAttribute("message", approvedUser.getDisplayName() + " 的实名认证已审核通过");
        return "redirect:/admin/verifications";
    }

    @PostMapping("/verifications/{id}/reject")
    public String rejectVerification(@PathVariable Long id, Principal principal, RedirectAttributes redirectAttributes) {
        User admin = requireAdmin(principal, redirectAttributes);
        if (admin == null) {
            return "redirect:/";
        }

        User rejectedUser = userService.rejectRealNameVerification(id);
        redirectAttributes.addFlashAttribute("message", rejectedUser.getDisplayName() + " 的实名认证已驳回");
        return "redirect:/admin/verifications";
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
