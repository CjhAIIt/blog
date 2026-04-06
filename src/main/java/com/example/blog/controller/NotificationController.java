package com.example.blog.controller;

import com.example.blog.model.User;
import com.example.blog.service.NotificationService;
import com.example.blog.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
public class NotificationController {
    private final NotificationService notificationService;
    private final UserService userService;

    public NotificationController(NotificationService notificationService, UserService userService) {
        this.notificationService = notificationService;
        this.userService = userService;
    }

    @GetMapping("/notifications")
    public String notificationCenter(Principal principal, Model model, RedirectAttributes redirectAttributes) {
        if (principal == null) {
            redirectAttributes.addFlashAttribute("error", "请先登录后查看消息中心");
            return "redirect:/login";
        }

        User currentUser = userService.getByUsername(principal.getName());
        model.addAttribute("notifications", notificationService.findByRecipient(currentUser));
        model.addAttribute("unreadNotificationCount", notificationService.countUnreadByRecipientId(currentUser.getId()));
        model.addAttribute("selectedCategory", "latest");
        return "space/notifications";
    }

    @GetMapping("/notifications/{id}")
    public String openNotification(@PathVariable Long id, Principal principal, RedirectAttributes redirectAttributes) {
        if (principal == null) {
            redirectAttributes.addFlashAttribute("error", "请先登录后查看消息");
            return "redirect:/login";
        }

        User currentUser = userService.getByUsername(principal.getName());
        try {
            return "redirect:" + notificationService.openNotification(id, currentUser);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/notifications";
        }
    }

    @PostMapping("/notifications/read-all")
    public String markAllNotificationsRead(Principal principal, RedirectAttributes redirectAttributes) {
        if (principal == null) {
            redirectAttributes.addFlashAttribute("error", "请先登录后查看消息中心");
            return "redirect:/login";
        }

        User currentUser = userService.getByUsername(principal.getName());
        notificationService.markAllAsRead(currentUser);
        redirectAttributes.addFlashAttribute("message", "全部消息已标记为已读");
        return "redirect:/notifications";
    }
}
