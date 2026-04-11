package com.example.blog.controller;

import com.example.blog.config.SiteProperties;
import com.example.blog.model.EditorFont;
import com.example.blog.model.PostCategory;
import com.example.blog.model.User;
import com.example.blog.service.NotificationService;
import com.example.blog.service.ViewModeService;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalModelAttributeAdvice {
    private final SiteProperties siteProperties;
    private final NotificationService notificationService;
    private final ViewModeService viewModeService;

    public GlobalModelAttributeAdvice(SiteProperties siteProperties,
                                      NotificationService notificationService,
                                      ViewModeService viewModeService) {
        this.siteProperties = siteProperties;
        this.notificationService = notificationService;
        this.viewModeService = viewModeService;
    }

    @ModelAttribute("postCategories")
    public PostCategory[] postCategories() {
        return PostCategory.values();
    }

    @ModelAttribute("editorFonts")
    public EditorFont[] editorFonts() {
        return EditorFont.values();
    }

    @ModelAttribute
    public void siteAttributes(Model model, Authentication authentication) {
        model.addAttribute("sourceRepoUrl", siteProperties.getSourceRepoUrl());
        long notificationUnreadCount = 0;
        User viewer = null;
        if (authentication != null && authentication.getPrincipal() instanceof User currentUser && currentUser.getId() != null) {
            notificationUnreadCount = notificationService.countUnreadByRecipientId(currentUser.getId());
            viewer = currentUser;
        }
        model.addAttribute("notificationUnreadCount", notificationUnreadCount);
        model.addAttribute("viewer", viewer);
        model.addAttribute("isMobileClient", viewModeService.isMobileClient());
    }
}
