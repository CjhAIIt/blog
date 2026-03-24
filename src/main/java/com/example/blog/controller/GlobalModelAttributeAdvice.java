package com.example.blog.controller;

import com.example.blog.config.SiteProperties;
import com.example.blog.model.EditorFont;
import com.example.blog.model.PostCategory;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalModelAttributeAdvice {
    private final SiteProperties siteProperties;

    public GlobalModelAttributeAdvice(SiteProperties siteProperties) {
        this.siteProperties = siteProperties;
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
    public void siteAttributes(Model model) {
        model.addAttribute("sourceRepoUrl", siteProperties.getSourceRepoUrl());
    }
}
