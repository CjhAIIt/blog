package com.example.blog.controller;

import com.example.blog.dto.ProfileForm;
import com.example.blog.model.User;
import com.example.blog.service.PostService;
import com.example.blog.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
public class SpaceController {
    private final UserService userService;
    private final PostService postService;

    public SpaceController(UserService userService, PostService postService) {
        this.userService = userService;
        this.postService = postService;
    }

    @GetMapping("/space")
    public String mySpace(Principal principal) {
        return "redirect:/users/" + principal.getName();
    }

    @GetMapping("/users/{username}")
    public String userSpace(@PathVariable String username, Principal principal, Model model) {
        User profileUser = userService.getByUsername(username);
        boolean isOwner = principal != null && principal.getName().equals(profileUser.getUsername());
        model.addAttribute("profileUser", profileUser);
        model.addAttribute("posts", postService.findByAuthorId(profileUser.getId()));
        model.addAttribute("isOwner", isOwner);
        model.addAttribute("selectedCategory", "latest");
        return "space/profile";
    }

    @GetMapping("/space/edit")
    public String editProfile(Principal principal, Model model) {
        User currentUser = userService.getByUsername(principal.getName());
        ProfileForm profileForm = new ProfileForm();
        profileForm.setBio(currentUser.getBio());
        profileForm.setQq(currentUser.getQq());
        profileForm.setGithubUrl(currentUser.getGithubUrl());
        model.addAttribute("profileForm", profileForm);
        model.addAttribute("selectedCategory", "latest");
        return "space/edit";
    }

    @PostMapping("/space/edit")
    public String updateProfile(@ModelAttribute ProfileForm profileForm,
                                Principal principal,
                                RedirectAttributes redirectAttributes) {
        if (StringUtils.hasText(profileForm.getQq()) && !profileForm.getQq().trim().matches("\\d{5,12}")) {
            redirectAttributes.addFlashAttribute("error", "QQ 号格式不正确");
            return "redirect:/space/edit";
        }

        User currentUser = userService.getByUsername(principal.getName());
        userService.updateProfile(currentUser, profileForm.getBio(), profileForm.getQq(), profileForm.getGithubUrl());
        redirectAttributes.addFlashAttribute("message", "个人资料已更新");
        return "redirect:/space";
    }
}
