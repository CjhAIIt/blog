package com.example.blog.controller;

import com.example.blog.dto.ProfileForm;
import com.example.blog.model.User;
import com.example.blog.service.PersonalBlogExportService;
import com.example.blog.service.PostService;
import com.example.blog.service.UserService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.security.Principal;

@Controller
public class SpaceController {
    private final UserService userService;
    private final PostService postService;
    private final PersonalBlogExportService personalBlogExportService;

    public SpaceController(UserService userService, PostService postService, PersonalBlogExportService personalBlogExportService) {
        this.userService = userService;
        this.postService = postService;
        this.personalBlogExportService = personalBlogExportService;
    }

    @GetMapping("/space")
    public String mySpace(Principal principal) {
        return "redirect:/users/" + UriUtils.encodePathSegment(principal.getName(), StandardCharsets.UTF_8);
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
        if (!model.containsAttribute("profileForm")) {
            User currentUser = userService.getByUsername(principal.getName());
            model.addAttribute("profileForm", buildProfileForm(currentUser));
        }
        model.addAttribute("selectedCategory", "latest");
        return "space/edit";
    }

    @GetMapping("/space/export")
    public String exportPage(Principal principal, Model model) {
        User currentUser = userService.getByUsername(principal.getName());
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("exportPosts", postService.findByAuthorId(currentUser.getId()));
        model.addAttribute("selectedCategory", "latest");
        return "space/export";
    }

    @GetMapping("/space/export/download")
    public ResponseEntity<byte[]> downloadExport(Principal principal) {
        User currentUser = userService.getByUsername(principal.getName());
        PersonalBlogExportService.ExportArchive archive =
                personalBlogExportService.export(currentUser, postService.findByAuthorId(currentUser.getId()));

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(archive.content().length)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(archive.fileName(), StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .body(archive.content());
    }

    @PostMapping("/space/edit")
    public String updateProfile(@ModelAttribute ProfileForm profileForm,
                                Principal principal,
                                RedirectAttributes redirectAttributes) {
        User currentUser = userService.getByUsername(principal.getName());
        String normalizedUsername = userService.normalizeUsername(profileForm.getUsername());
        profileForm.setUsername(normalizedUsername);

        if (!StringUtils.hasText(normalizedUsername) || normalizedUsername.length() < 3 || normalizedUsername.length() > 20) {
            return redirectWithError("用户名长度应在 3 到 20 个字符之间", profileForm, redirectAttributes);
        }
        if (StringUtils.hasText(profileForm.getQq()) && !profileForm.getQq().trim().matches("\\d{5,12}")) {
            return redirectWithError("QQ 号格式不正确", profileForm, redirectAttributes);
        }
        if (StringUtils.hasText(profileForm.getConfirmPassword()) && !StringUtils.hasText(profileForm.getNewPassword())) {
            return redirectWithError("请先输入新密码", profileForm, redirectAttributes);
        }

        boolean usernameChanged = !normalizedUsername.equals(currentUser.getUsername());
        boolean passwordChanged = StringUtils.hasText(profileForm.getNewPassword());
        if ((usernameChanged || passwordChanged) && !StringUtils.hasText(profileForm.getCurrentPassword())) {
            return redirectWithError("修改用户名或密码时，请输入当前密码", profileForm, redirectAttributes);
        }
        if ((usernameChanged || passwordChanged) && !userService.matchesPassword(profileForm.getCurrentPassword(), currentUser.getPassword())) {
            return redirectWithError("当前密码不正确", profileForm, redirectAttributes);
        }
        if (passwordChanged && profileForm.getNewPassword().length() < 8) {
            return redirectWithError("新密码长度至少 8 位", profileForm, redirectAttributes);
        }
        if (passwordChanged && !profileForm.getNewPassword().equals(profileForm.getConfirmPassword())) {
            return redirectWithError("两次输入的新密码不一致", profileForm, redirectAttributes);
        }

        User updatedUser;
        try {
            updatedUser = userService.updateAccountProfile(
                    currentUser,
                    normalizedUsername,
                    profileForm.getNewPassword(),
                    profileForm.getBio(),
                    profileForm.getQq(),
                    profileForm.getGithubUrl(),
                    profileForm.getPersonalBlogUrl()
            );
        } catch (IllegalArgumentException e) {
            return redirectWithError(e.getMessage(), profileForm, redirectAttributes);
        }

        refreshAuthentication(updatedUser);
        redirectAttributes.addFlashAttribute("message", "个人资料已更新");
        return "redirect:/users/" + UriUtils.encodePathSegment(updatedUser.getUsername(), StandardCharsets.UTF_8);
    }

    private ProfileForm buildProfileForm(User currentUser) {
        ProfileForm profileForm = new ProfileForm();
        profileForm.setUsername(currentUser.getUsername());
        profileForm.setBio(currentUser.getBio());
        profileForm.setQq(currentUser.getQq());
        profileForm.setGithubUrl(currentUser.getGithubUrl());
        profileForm.setPersonalBlogUrl(currentUser.getPersonalBlogUrl());
        return profileForm;
    }

    private String redirectWithError(String errorMessage, ProfileForm profileForm, RedirectAttributes redirectAttributes) {
        profileForm.setCurrentPassword(null);
        profileForm.setNewPassword(null);
        profileForm.setConfirmPassword(null);
        redirectAttributes.addFlashAttribute("error", errorMessage);
        redirectAttributes.addFlashAttribute("profileForm", profileForm);
        return "redirect:/space/edit";
    }

    private void refreshAuthentication(User updatedUser) {
        Authentication currentAuthentication = SecurityContextHolder.getContext().getAuthentication();
        if (currentAuthentication == null) {
            return;
        }

        UsernamePasswordAuthenticationToken refreshedAuthentication =
                new UsernamePasswordAuthenticationToken(
                        updatedUser,
                        currentAuthentication.getCredentials(),
                        currentAuthentication.getAuthorities()
                );
        refreshedAuthentication.setDetails(currentAuthentication.getDetails());
        SecurityContextHolder.getContext().setAuthentication(refreshedAuthentication);
    }
}
