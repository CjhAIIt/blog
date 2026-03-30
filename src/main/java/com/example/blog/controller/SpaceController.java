package com.example.blog.controller;

import com.example.blog.dto.ProfileForm;
import com.example.blog.model.Post;
import com.example.blog.model.User;
import com.example.blog.service.CommentService;
import com.example.blog.service.FileStorageService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.List;

@Controller
public class SpaceController {
    private final UserService userService;
    private final PostService postService;
    private final CommentService commentService;
    private final PersonalBlogExportService personalBlogExportService;
    private final FileStorageService fileStorageService;

    public SpaceController(UserService userService,
                           PostService postService,
                           CommentService commentService,
                           PersonalBlogExportService personalBlogExportService,
                           FileStorageService fileStorageService) {
        this.userService = userService;
        this.postService = postService;
        this.commentService = commentService;
        this.personalBlogExportService = personalBlogExportService;
        this.fileStorageService = fileStorageService;
    }

    @GetMapping("/space")
    public String mySpace(Principal principal) {
        return "redirect:/users/" + UriUtils.encodePathSegment(principal.getName(), StandardCharsets.UTF_8);
    }

    @GetMapping("/users/{username}")
    public String userSpace(@PathVariable String username, Principal principal, Model model) {
        User profileUser = userService.getByUsername(username);
        boolean isOwner = principal != null && principal.getName().equals(profileUser.getUsername());
        List<Post> publishedPosts = postService.findPublishedByAuthorId(profileUser.getId());
        List<Post> drafts = isOwner ? postService.findDraftsByAuthorId(profileUser.getId()) : List.of();
        int totalLikes = publishedPosts.stream().mapToInt(Post::getLikeCount).sum();

        model.addAttribute("profileUser", profileUser);
        model.addAttribute("posts", publishedPosts);
        model.addAttribute("drafts", drafts);
        model.addAttribute("draftCount", drafts.size());
        model.addAttribute("publishedCount", publishedPosts.size());
        model.addAttribute("totalLikeCount", totalLikes);
        model.addAttribute("receivedCommentCount", commentService.countByPostAuthorId(profileUser.getId()));
        model.addAttribute("latestPost", publishedPosts.isEmpty() ? null : publishedPosts.get(0));
        model.addAttribute("isOwner", isOwner);
        model.addAttribute("selectedCategory", "latest");
        return "space/profile";
    }

    @GetMapping("/space/drafts")
    public String draftsPage(Principal principal, Model model) {
        User currentUser = userService.getByUsername(principal.getName());
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("drafts", postService.findDraftsByAuthorId(currentUser.getId()));
        model.addAttribute("selectedCategory", "latest");
        return "space/drafts";
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
        model.addAttribute("exportPosts", postService.findPublishedByAuthorId(currentUser.getId()));
        model.addAttribute("selectedCategory", "latest");
        return "space/export";
    }

    @GetMapping("/space/export/download")
    public ResponseEntity<byte[]> downloadExport(Principal principal) {
        User currentUser = userService.getByUsername(principal.getName());
        PersonalBlogExportService.ExportArchive archive =
                personalBlogExportService.export(currentUser, postService.findPublishedByAuthorId(currentUser.getId()));

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(archive.content().length)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(archive.fileName(), StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .body(archive.content());
    }

    @PostMapping(value = "/space/edit", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String updateProfile(@ModelAttribute ProfileForm profileForm,
                                @RequestParam(value = "avatarFile", required = false) MultipartFile avatarFile,
                                @RequestParam(value = "useDefaultAvatar", defaultValue = "false") boolean useDefaultAvatar,
                                Principal principal,
                                RedirectAttributes redirectAttributes) {
        User currentUser = userService.getByUsername(principal.getName());
        String normalizedUsername = userService.normalizeUsername(profileForm.getUsername());
        profileForm.setUsername(normalizedUsername);
        profileForm.setExistingAvatarUrl(currentUser.getDisplayAvatarUrl());

        if (!StringUtils.hasText(normalizedUsername) || normalizedUsername.length() < 3 || normalizedUsername.length() > 20) {
            return redirectWithError("用户名长度应在 3 到 20 个字符之间", profileForm, redirectAttributes);
        }
        if (StringUtils.hasText(profileForm.getQq()) && !profileForm.getQq().trim().matches("\\d{5,12}")) {
            return redirectWithError("QQ 号格式不正确", profileForm, redirectAttributes);
        }
        if (StringUtils.hasText(profileForm.getRealName()) && !profileForm.getRealName().trim().matches("^[\\u4E00-\\u9FFF]{1,5}$")) {
            return redirectWithError("真实姓名需为 1 到 5 个中文字符", profileForm, redirectAttributes);
        }
        if (StringUtils.hasText(profileForm.getConfirmPassword()) && !StringUtils.hasText(profileForm.getNewPassword())) {
            return redirectWithError("请先输入新密码", profileForm, redirectAttributes);
        }

        boolean usernameChanged = !normalizedUsername.equals(currentUser.getUsername());
        boolean passwordChanged = StringUtils.hasText(profileForm.getNewPassword());
        if ((usernameChanged || passwordChanged) && !StringUtils.hasText(profileForm.getCurrentPassword())) {
            return redirectWithError("修改用户名或密码时，请输入当前密码确认", profileForm, redirectAttributes);
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

        String avatarUrl = currentUser.getAvatarImageUrl();
        try {
            if (useDefaultAvatar) {
                fileStorageService.deleteIfStored(avatarUrl);
                avatarUrl = null;
            } else if (avatarFile != null && !avatarFile.isEmpty()) {
                String newAvatarUrl = fileStorageService.storeAvatar(avatarFile);
                fileStorageService.deleteIfStored(avatarUrl);
                avatarUrl = newAvatarUrl;
            }

            User updatedUser = userService.updateAccountProfile(
                    currentUser,
                    normalizedUsername,
                    profileForm.getNewPassword(),
                    profileForm.getRealName(),
                    profileForm.getBio(),
                    profileForm.getQq(),
                    profileForm.getGithubUrl(),
                    profileForm.getPersonalBlogUrl(),
                    avatarUrl
            );

            refreshAuthentication(updatedUser);
            redirectAttributes.addFlashAttribute("message", "个人资料已更新");
            return "redirect:/users/" + UriUtils.encodePathSegment(updatedUser.getUsername(), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return redirectWithError(e.getMessage(), profileForm, redirectAttributes);
        }
    }

    private ProfileForm buildProfileForm(User currentUser) {
        ProfileForm profileForm = new ProfileForm();
        profileForm.setUsername(currentUser.getUsername());
        profileForm.setRealName(currentUser.getRealName());
        profileForm.setBio(currentUser.getBio());
        profileForm.setQq(currentUser.getQq());
        profileForm.setGithubUrl(currentUser.getGithubUrl());
        profileForm.setPersonalBlogUrl(currentUser.getPersonalBlogUrl());
        profileForm.setExistingAvatarUrl(currentUser.getDisplayAvatarUrl());
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
