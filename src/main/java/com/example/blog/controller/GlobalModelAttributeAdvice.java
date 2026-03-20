package com.example.blog.controller;

import com.example.blog.model.PostCategory;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalModelAttributeAdvice {
    @ModelAttribute("postCategories")
    public PostCategory[] postCategories() {
        return PostCategory.values();
    }
}
