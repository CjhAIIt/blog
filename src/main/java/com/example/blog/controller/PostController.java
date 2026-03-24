package com.example.blog.controller;

import com.example.blog.model.EditorFont;
import com.example.blog.model.Post;
import com.example.blog.model.PostCategory;
import com.example.blog.service.CommentService;
import com.example.blog.service.FileStorageService;
import com.example.blog.service.MarkdownService;
import com.example.blog.service.PostService;
import com.example.blog.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Controller
@RequestMapping("/posts")
public class PostController {
    private static final String LIKED_POST_IDS_SESSION_KEY = "likedPostIds";

    private final PostService postService;
    private final UserService userService;
    private final MarkdownService markdownService;
    private final CommentService commentService;
    private final FileStorageService fileStorageService;

    public PostController(PostService postService,
                          UserService userService,
                          MarkdownService markdownService,
                          CommentService commentService,
                          FileStorageService fileStorageService) {
        this.postService = postService;
        this.userService = userService;
        this.markdownService = markdownService;
        this.commentService = commentService;
        this.fileStorageService = fileStorageService;
    }

    @GetMapping
    public String listPosts(@RequestParam(defaultValue = "latest") String category, Model model) {
        Optional<PostCategory> selectedCategory = PostCategory.fromSlug(category);
        List<Post> posts = selectedCategory
                .map(postService::findByCategory)
                .orElseGet(postService::findAll);
        model.addAttribute("posts", posts);
        model.addAttribute("selectedCategory", selectedCategory.map(PostCategory::getSlug).orElse("latest"));
        return "posts/list";
    }

    @GetMapping("/{id}")
    public String viewPost(@PathVariable Long id,
                           Model model,
                           Principal principal,
                           RedirectAttributes redirectAttributes,
                           HttpSession session) {
        Optional<Post> post = postService.findById(id);
        if (post.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "文章不存在");
            return "redirect:/posts";
        }

        Post currentPost = post.get();
        boolean canManage = principal != null && postService.canManage(currentPost, principal.getName());
        if (currentPost.isDraft() && !canManage) {
            redirectAttributes.addFlashAttribute("error", "草稿仅作者本人可见");
            return "redirect:/posts";
        }

        model.addAttribute("post", currentPost);
        model.addAttribute("renderedContent", postService.renderMarkdown(currentPost));
        model.addAttribute("canManage", canManage);
        model.addAttribute("isDraftPreview", currentPost.isDraft());
        model.addAttribute("comments", currentPost.isDraft() ? List.of() : commentService.findByPostId(currentPost.getId()));
        model.addAttribute("commentCount", currentPost.isDraft() ? 0 : commentService.countByPostId(currentPost.getId()));
        model.addAttribute("likedByCurrentSession", getLikedPostIds(session).contains(id));
        model.addAttribute("selectedCategory", currentPost.getCategorySlug());
        return "posts/view";
    }

    @GetMapping("/new")
    public String newPostForm(Model model, Principal principal, RedirectAttributes redirectAttributes) {
        if (principal == null) {
            redirectAttributes.addFlashAttribute("error", "请先登录后再写文章");
            return "redirect:/login";
        }
        Post post = new Post();
        post.setCategory(PostCategory.PROJECT);
        post.setFontKey(EditorFont.DEFAULT.getKey());
        model.addAttribute("post", post);
        model.addAttribute("editorLocalDraftKey", "new-" + UUID.randomUUID());
        model.addAttribute("selectedCategory", "latest");
        return "posts/form";
    }

    @GetMapping("/edit/{id}")
    public String editPostForm(@PathVariable Long id, Model model, Principal principal, RedirectAttributes redirectAttributes) {
        if (principal == null) {
            redirectAttributes.addFlashAttribute("error", "请先登录后再编辑文章");
            return "redirect:/login";
        }

        Optional<Post> post = postService.findById(id);
        if (post.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "文章不存在");
            return "redirect:/posts";
        }
        if (!postService.canManage(post.get(), principal.getName())) {
            redirectAttributes.addFlashAttribute("error", "你没有权限编辑这篇文章");
            return "redirect:/posts/" + id;
        }

        model.addAttribute("post", post.get());
        model.addAttribute("editorLocalDraftKey", "post-" + id);
        model.addAttribute("selectedCategory", post.get().getCategorySlug());
        return "posts/form";
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String savePost(@ModelAttribute Post post,
                           @RequestParam(defaultValue = "publish") String action,
                           @RequestParam(value = "coverFile", required = false) MultipartFile coverFile,
                           @RequestParam(value = "useDefaultCover", defaultValue = "false") boolean useDefaultCover,
                           Principal principal,
                           RedirectAttributes redirectAttributes) {
        if (principal == null) {
            redirectAttributes.addFlashAttribute("error", "请先登录");
            return "redirect:/login";
        }

        post.setAuthor(userService.getByUsername(principal.getName()));
        post.setCategory(post.getCategory() == null ? PostCategory.PROJECT : post.getCategory());

        try {
            applyCover(post, null, coverFile, useDefaultCover);
            if (isDraftAction(action)) {
                Post draft = postService.saveDraft(post);
                redirectAttributes.addFlashAttribute("message", "草稿已保存到草稿箱");
                return "redirect:/posts/edit/" + draft.getId();
            }

            if (!isPublishable(post)) {
                redirectAttributes.addFlashAttribute("error", "发布文章时，标题、分类和正文不能为空");
                return "redirect:/posts/new";
            }

            Post publishedPost = postService.publish(post, false);
            redirectAttributes.addFlashAttribute("message", "文章已发布");
            return "redirect:/posts/" + publishedPost.getId();
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/posts/new";
        }
    }

    @PostMapping(value = "/update/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String updatePost(@PathVariable Long id,
                             @ModelAttribute Post post,
                             @RequestParam(defaultValue = "publish") String action,
                             @RequestParam(value = "coverFile", required = false) MultipartFile coverFile,
                             @RequestParam(value = "useDefaultCover", defaultValue = "false") boolean useDefaultCover,
                             Principal principal,
                             RedirectAttributes redirectAttributes) {
        if (principal == null) {
            redirectAttributes.addFlashAttribute("error", "请先登录");
            return "redirect:/login";
        }

        Optional<Post> existingPost = postService.findById(id);
        if (existingPost.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "文章不存在");
            return "redirect:/posts";
        }
        if (!postService.canManage(existingPost.get(), principal.getName())) {
            redirectAttributes.addFlashAttribute("error", "你没有权限修改这篇文章");
            return "redirect:/posts/" + id;
        }

        Post editablePost = existingPost.get();
        boolean wasDraft = editablePost.isDraft();
        editablePost.setTitle(post.getTitle());
        editablePost.setContent(post.getContent());
        editablePost.setCategory(post.getCategory() == null ? editablePost.getCategory() : post.getCategory());
        editablePost.setFontKey(post.getFontKey());

        try {
            applyCover(editablePost, existingPost.get().getCoverImageUrl(), coverFile, useDefaultCover);
            if (isDraftAction(action)) {
                Post draft = postService.saveDraft(editablePost);
                redirectAttributes.addFlashAttribute("message", "草稿已更新");
                return "redirect:/posts/edit/" + draft.getId();
            }

            if (!isPublishable(editablePost)) {
                redirectAttributes.addFlashAttribute("error", "发布文章时，标题、分类和正文不能为空");
                return "redirect:/posts/edit/" + id;
            }

            Post publishedPost = postService.publish(editablePost, wasDraft);
            redirectAttributes.addFlashAttribute("message", wasDraft ? "草稿已发布" : "文章内容已更新");
            return "redirect:/posts/" + publishedPost.getId();
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/posts/edit/" + id;
        }
    }

    @PostMapping("/import")
    public String importMarkdown(@RequestParam("markdownFile") MultipartFile markdownFile,
                                 @RequestParam(value = "category", required = false) PostCategory category,
                                 Principal principal,
                                 RedirectAttributes redirectAttributes) {
        if (principal == null) {
            redirectAttributes.addFlashAttribute("error", "请先登录");
            return "redirect:/login";
        }

        try {
            String markdownContent = fileStorageService.loadMarkdownContent(markdownFile);
            Post importedDraft = postService.createImportedDraft(
                    userService.getByUsername(principal.getName()),
                    category,
                    markdownFile.getOriginalFilename(),
                    markdownContent
            );
            redirectAttributes.addFlashAttribute("message", "Markdown 文件已导入草稿箱，请继续补充封面和排版");
            return "redirect:/posts/edit/" + importedDraft.getId();
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/space/drafts";
        } catch (DataIntegrityViolationException e) {
            redirectAttributes.addFlashAttribute("error", "导入失败：当前数据库字段容量不足，请重启应用完成内容字段升级后重试");
            return "redirect:/space/drafts";
        }
    }

    @PostMapping("/delete/{id}")
    public String deletePost(@PathVariable Long id, Principal principal, RedirectAttributes redirectAttributes) {
        if (principal == null) {
            redirectAttributes.addFlashAttribute("error", "请先登录");
            return "redirect:/login";
        }

        Optional<Post> existingPost = postService.findById(id);
        if (existingPost.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "文章不存在");
            return "redirect:/posts";
        }
        if (!postService.canManage(existingPost.get(), principal.getName())) {
            redirectAttributes.addFlashAttribute("error", "你没有权限删除这篇文章");
            return "redirect:/posts/" + id;
        }

        fileStorageService.deleteIfStored(existingPost.get().getCoverImageUrl());
        boolean wasDraft = existingPost.get().isDraft();
        postService.deleteById(id);
        redirectAttributes.addFlashAttribute("message", wasDraft ? "草稿已删除" : "文章已删除");
        return wasDraft ? "redirect:/space/drafts" : "redirect:/space";
    }

    @GetMapping("/search")
    public String searchPosts(@RequestParam String keyword, Model model) {
        List<Post> posts = postService.findByKeyword(keyword);
        model.addAttribute("posts", posts);
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedCategory", "latest");
        return "posts/list";
    }

    @PostMapping(value = "/preview", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public ResponseEntity<String> previewMarkdown(@RequestParam String content) {
        return ResponseEntity.ok(markdownService.render(content));
    }

    @PostMapping("/{id}/comments")
    public String addComment(@PathVariable Long id,
                             @RequestParam String content,
                             Principal principal,
                             RedirectAttributes redirectAttributes) {
        Optional<Post> post = postService.findById(id);
        if (post.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "文章不存在");
            return "redirect:/posts";
        }
        if (post.get().isDraft()) {
            redirectAttributes.addFlashAttribute("error", "草稿暂不支持评论");
            return "redirect:/posts/" + id;
        }
        if (!StringUtils.hasText(content)) {
            redirectAttributes.addFlashAttribute("error", "评论内容不能为空");
            return "redirect:/posts/" + id;
        }
        if (content.trim().length() > 500) {
            redirectAttributes.addFlashAttribute("error", "评论内容不能超过 500 个字符");
            return "redirect:/posts/" + id;
        }

        commentService.save(post.get(), userService.getByUsername(principal.getName()), content);
        redirectAttributes.addFlashAttribute("message", "评论已发布");
        return "redirect:/posts/" + id + "#comments";
    }

    @PostMapping("/{id}/like")
    public String likePost(@PathVariable Long id,
                           HttpSession session,
                           RedirectAttributes redirectAttributes) {
        Optional<Post> post = postService.findById(id);
        if (post.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "文章不存在");
            return "redirect:/posts";
        }
        if (post.get().isDraft()) {
            redirectAttributes.addFlashAttribute("error", "草稿暂不支持点赞");
            return "redirect:/posts/" + id;
        }

        Set<Long> likedPostIds = getLikedPostIds(session);
        if (likedPostIds.contains(id)) {
            redirectAttributes.addFlashAttribute("error", "你已经给这篇文章点过赞了");
            return "redirect:/posts/" + id;
        }

        Post likedPost = postService.likePost(id);
        likedPostIds.add(id);
        session.setAttribute(LIKED_POST_IDS_SESSION_KEY, likedPostIds);
        redirectAttributes.addFlashAttribute("message", "点赞成功，当前点赞数为 " + likedPost.getLikeCount());
        return "redirect:/posts/" + id;
    }

    private boolean isDraftAction(String action) {
        return "draft".equalsIgnoreCase(action);
    }

    private boolean isPublishable(Post post) {
        return StringUtils.hasText(post.getTitle())
                && StringUtils.hasText(post.getContent())
                && post.getCategory() != null;
    }

    private void applyCover(Post target,
                            String originalCoverUrl,
                            MultipartFile coverFile,
                            boolean useDefaultCover) {
        String currentCoverUrl = StringUtils.hasText(originalCoverUrl) ? originalCoverUrl : target.getCoverImageUrl();
        if (useDefaultCover) {
            fileStorageService.deleteIfStored(currentCoverUrl);
            target.setCoverImageUrl(null);
            return;
        }
        if (coverFile == null || coverFile.isEmpty()) {
            return;
        }

        String newCoverUrl = fileStorageService.storeCover(coverFile);
        fileStorageService.deleteIfStored(currentCoverUrl);
        target.setCoverImageUrl(newCoverUrl);
    }

    private Set<Long> getLikedPostIds(HttpSession session) {
        Object likedPostIds = session.getAttribute(LIKED_POST_IDS_SESSION_KEY);
        if (likedPostIds instanceof Set<?> likedSet) {
            Set<Long> normalizedIds = new HashSet<>();
            for (Object value : likedSet) {
                if (value instanceof Number numberValue) {
                    normalizedIds.add(numberValue.longValue());
                }
            }
            return normalizedIds;
        }
        return new HashSet<>();
    }
}
