package com.example.blog.controller;

import com.example.blog.model.EditorFont;
import com.example.blog.model.Plan;
import com.example.blog.model.PlanAccessType;
import com.example.blog.model.Post;
import com.example.blog.model.PostCategory;
import com.example.blog.model.User;
import com.example.blog.service.CommentService;
import com.example.blog.service.FileStorageService;
import com.example.blog.service.MarkdownService;
import com.example.blog.service.PlanService;
import com.example.blog.service.PostService;
import com.example.blog.service.UserService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Controller
@RequestMapping("/posts")
public class PostController {
    private static final int MAX_TITLE_LENGTH = 200;

    private final PostService postService;
    private final UserService userService;
    private final MarkdownService markdownService;
    private final CommentService commentService;
    private final FileStorageService fileStorageService;
    private final PlanService planService;

    public PostController(PostService postService,
                          UserService userService,
                          MarkdownService markdownService,
                          CommentService commentService,
                          FileStorageService fileStorageService,
                          PlanService planService) {
        this.postService = postService;
        this.userService = userService;
        this.markdownService = markdownService;
        this.commentService = commentService;
        this.fileStorageService = fileStorageService;
        this.planService = planService;
    }

    @GetMapping
    public String listPosts(@RequestParam(defaultValue = "latest") String category, Model model) {
        Optional<PostCategory> selectedCategory = PostCategory.fromSlug(category);
        List<Post> posts = selectedCategory.map(postService::findByCategory).orElseGet(postService::findAll);
        model.addAttribute("posts", posts);
        model.addAttribute("selectedCategory", selectedCategory.map(PostCategory::getSlug).orElse("latest"));
        return "posts/list";
    }

    @GetMapping("/{id}")
    public String viewPost(@PathVariable Long id, Model model, Principal principal, RedirectAttributes redirectAttributes) {
        User currentUser = principal == null ? null : userService.getByUsername(principal.getName());
        Optional<Post> post = currentUser == null ? postService.findVisibleById(id) : postService.findById(id);
        if (post.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "文章不存在");
            return "redirect:/posts";
        }

        Post currentPost = post.get();
        boolean canManage = postService.canManage(currentPost, currentUser);
        if (currentPost.isHiddenFromPublic() && !canManage) {
            redirectAttributes.addFlashAttribute("error", "你无权查看这篇未公开文章");
            return "redirect:/posts";
        }

        model.addAttribute("post", currentPost);
        model.addAttribute("renderedContent", postService.renderMarkdown(currentPost));
        model.addAttribute("canManage", canManage);
        model.addAttribute("isDraftPreview", currentPost.isHiddenFromPublic());
        model.addAttribute("comments", currentPost.isPublished() ? commentService.findByPostId(currentPost.getId()) : List.of());
        model.addAttribute("commentCount", currentPost.isPublished() ? commentService.countByPostId(currentPost.getId()) : 0);
        model.addAttribute("likedByCurrentUser", postService.hasUserLikedPost(id, currentUser));
        model.addAttribute("currentUsername", currentUser == null ? null : currentUser.getUsername());
        model.addAttribute("currentUserAdmin", currentUser != null && currentUser.isAdmin());
        model.addAttribute("selectedCategory", currentPost.getCategorySlug());
        model.addAttribute("showPlanContext", currentPost.getPlan() != null
                && (currentPost.getPlan().isPublic() || planService.canJoin(currentPost.getPlan(), currentUser)));
        return "posts/view";
    }

    @GetMapping("/new")
    public String newPostForm(@RequestParam(value = "planId", required = false) Long planId,
                              Model model,
                              Principal principal,
                              RedirectAttributes redirectAttributes) {
        if (principal == null) {
            redirectAttributes.addFlashAttribute("error", "请先登录后再写博客");
            return "redirect:/login";
        }

        User currentUser = userService.getByUsername(principal.getName());
        if (!userService.canWritePosts(currentUser)) {
            redirectAttributes.addFlashAttribute("error", userService.getPostPermissionMessage(currentUser));
            return "redirect:/space/edit";
        }

        Post post = new Post();
        post.setCategory(PostCategory.PROJECT);
        post.setFontKey(EditorFont.DEFAULT.getKey());
        if (planId != null) {
            preselectPlan(post, planId, currentUser);
        }
        model.addAttribute("post", post);
        model.addAttribute("editorLocalDraftKey", "new-" + UUID.randomUUID());
        model.addAttribute("selectedCategory", "latest");
        populatePlanOptions(model, currentUser);
        return "posts/form";
    }

    @GetMapping("/edit/{id}")
    public String editPostForm(@PathVariable Long id, Model model, Principal principal, RedirectAttributes redirectAttributes) {
        if (principal == null) {
            redirectAttributes.addFlashAttribute("error", "请先登录后再编辑文章");
            return "redirect:/login";
        }

        User currentUser = userService.getByUsername(principal.getName());
        if (!userService.canWritePosts(currentUser)) {
            redirectAttributes.addFlashAttribute("error", userService.getPostPermissionMessage(currentUser));
            return "redirect:/space/edit";
        }

        Optional<Post> post = postService.findById(id);
        if (post.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "文章不存在");
            return "redirect:/posts";
        }
        if (!postService.canManage(post.get(), currentUser)) {
            redirectAttributes.addFlashAttribute("error", "你没有权限编辑这篇文章");
            return "redirect:/posts/" + id;
        }

        model.addAttribute("post", post.get());
        model.addAttribute("editorLocalDraftKey", "post-" + id);
        model.addAttribute("selectedCategory", post.get().getCategorySlug());
        populatePlanOptions(model, currentUser);
        return "posts/form";
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String savePost(@ModelAttribute Post post,
                           @RequestParam(defaultValue = "publish") String action,
                           @RequestParam(value = "scheduledPublishAt", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime scheduledPublishAt,
                           @RequestParam(value = "coverFile", required = false) MultipartFile coverFile,
                           @RequestParam(value = "useDefaultCover", defaultValue = "false") boolean useDefaultCover,
                           @RequestParam(value = "planSelection", defaultValue = "none") String planSelection,
                           @RequestParam(value = "existingPlanId", required = false) Long existingPlanId,
                           @RequestParam(value = "newPlanName", required = false) String newPlanName,
                           @RequestParam(value = "newPlanAccessType", required = false) PlanAccessType newPlanAccessType,
                           @RequestParam(value = "planOrder", required = false) Integer planOrder,
                           Principal principal,
                           RedirectAttributes redirectAttributes) {
        if (principal == null) {
            redirectAttributes.addFlashAttribute("error", "请先登录");
            return "redirect:/login";
        }

        User currentUser = userService.getByUsername(principal.getName());
        if (!userService.canWritePosts(currentUser)) {
            redirectAttributes.addFlashAttribute("error", userService.getPostPermissionMessage(currentUser));
            return "redirect:/space/edit";
        }

        post.setAuthor(currentUser);
        post.setCategory(post.getCategory() == null ? PostCategory.PROJECT : post.getCategory());

        try {
            validateTitle(post.getTitle());
            applyCover(post, null, coverFile, useDefaultCover);
            processPlanSelection(post, planSelection, existingPlanId, newPlanName, newPlanAccessType, planOrder, currentUser);
            if (isDraftAction(action)) {
                Post draft = postService.saveDraft(post);
                redirectAttributes.addFlashAttribute("message", "草稿已保存到草稿箱");
                return "redirect:/posts/edit/" + draft.getId();
            }
            if (!isPublishable(post)) {
                redirectAttributes.addFlashAttribute("error", "发布文章时，标题、分类和正文不能为空");
                return "redirect:/posts/new";
            }
            if (isScheduleAction(action)) {
                Post scheduledPost = postService.schedule(post, false, scheduledPublishAt);
                redirectAttributes.addFlashAttribute("message", "文章已进入定时发布队列");
                return "redirect:/posts/" + scheduledPost.getId();
            }

            Post publishedPost = postService.publish(post, false);
            redirectAttributes.addFlashAttribute("message", "文章已发布");
            return "redirect:/posts/" + publishedPost.getId();
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/posts/new";
        } catch (DataIntegrityViolationException e) {
            redirectAttributes.addFlashAttribute("error", "保存失败：数据库字段容量不足，请重启应用完成结构升级后重试");
            return "redirect:/posts/new";
        }
    }

    @PostMapping(value = "/update/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String updatePost(@PathVariable Long id,
                             @ModelAttribute Post post,
                             @RequestParam(defaultValue = "publish") String action,
                             @RequestParam(value = "scheduledPublishAt", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime scheduledPublishAt,
                             @RequestParam(value = "coverFile", required = false) MultipartFile coverFile,
                             @RequestParam(value = "useDefaultCover", defaultValue = "false") boolean useDefaultCover,
                             @RequestParam(value = "planSelection", defaultValue = "none") String planSelection,
                             @RequestParam(value = "existingPlanId", required = false) Long existingPlanId,
                             @RequestParam(value = "newPlanName", required = false) String newPlanName,
                             @RequestParam(value = "newPlanAccessType", required = false) PlanAccessType newPlanAccessType,
                             @RequestParam(value = "planOrder", required = false) Integer planOrder,
                             Principal principal,
                             RedirectAttributes redirectAttributes) {
        if (principal == null) {
            redirectAttributes.addFlashAttribute("error", "请先登录");
            return "redirect:/login";
        }

        User currentUser = userService.getByUsername(principal.getName());
        if (!userService.canWritePosts(currentUser)) {
            redirectAttributes.addFlashAttribute("error", userService.getPostPermissionMessage(currentUser));
            return "redirect:/space/edit";
        }

        Optional<Post> existingPost = postService.findById(id);
        if (existingPost.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "文章不存在");
            return "redirect:/posts";
        }
        if (!postService.canManage(existingPost.get(), currentUser)) {
            redirectAttributes.addFlashAttribute("error", "你没有权限修改这篇文章");
            return "redirect:/posts/" + id;
        }

        Post editablePost = existingPost.get();
        boolean wasDraft = editablePost.isDraft() || editablePost.isScheduled();
        editablePost.setTitle(post.getTitle());
        editablePost.setContent(post.getContent());
        editablePost.setCategory(post.getCategory() == null ? editablePost.getCategory() : post.getCategory());
        editablePost.setFontKey(post.getFontKey());

        try {
            validateTitle(editablePost.getTitle());
            applyCover(editablePost, editablePost.getCoverImageUrl(), coverFile, useDefaultCover);
            processPlanSelection(editablePost, planSelection, existingPlanId, newPlanName, newPlanAccessType, planOrder, currentUser);
            if (isDraftAction(action)) {
                Post draft = postService.saveDraft(editablePost);
                redirectAttributes.addFlashAttribute("message", "草稿已更新");
                return "redirect:/posts/edit/" + draft.getId();
            }
            if (!isPublishable(editablePost)) {
                redirectAttributes.addFlashAttribute("error", "发布文章时，标题、分类和正文不能为空");
                return "redirect:/posts/edit/" + id;
            }
            if (isScheduleAction(action)) {
                Post scheduledPost = postService.schedule(editablePost, wasDraft, scheduledPublishAt);
                redirectAttributes.addFlashAttribute("message", "文章定时发布设置已更新");
                return "redirect:/posts/" + scheduledPost.getId();
            }

            Post publishedPost = postService.publish(editablePost, wasDraft);
            redirectAttributes.addFlashAttribute("message", wasDraft ? "草稿已发布" : "文章内容已更新");
            return "redirect:/posts/" + publishedPost.getId();
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/posts/edit/" + id;
        } catch (DataIntegrityViolationException e) {
            redirectAttributes.addFlashAttribute("error", "保存失败：数据库字段容量不足，请重启应用完成结构升级后重试");
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

        User currentUser = userService.getByUsername(principal.getName());
        if (!userService.canWritePosts(currentUser)) {
            redirectAttributes.addFlashAttribute("error", userService.getPostPermissionMessage(currentUser));
            return "redirect:/space/edit";
        }

        try {
            String markdownContent = fileStorageService.loadMarkdownContent(markdownFile);
            Post importedDraft = postService.createImportedDraft(currentUser, category, markdownFile.getOriginalFilename(), markdownContent);
            redirectAttributes.addFlashAttribute("message", "Markdown 文件已导入草稿箱，请继续补充封面和排版");
            return "redirect:/posts/edit/" + importedDraft.getId();
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/space/drafts";
        } catch (DataIntegrityViolationException e) {
            redirectAttributes.addFlashAttribute("error", "导入失败：数据库内容字段容量不足，请重启应用完成结构升级后重试");
            return "redirect:/space/drafts";
        }
    }

    @PostMapping("/delete/{id}")
    public String deletePost(@PathVariable Long id, Principal principal, RedirectAttributes redirectAttributes) {
        if (principal == null) {
            redirectAttributes.addFlashAttribute("error", "请先登录");
            return "redirect:/login";
        }

        User currentUser = userService.getByUsername(principal.getName());
        Optional<Post> existingPost = postService.findById(id);
        if (existingPost.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "文章不存在");
            return "redirect:/posts";
        }
        if (!postService.canManage(existingPost.get(), currentUser)) {
            redirectAttributes.addFlashAttribute("error", "你没有权限删除这篇文章");
            return "redirect:/posts/" + id;
        }

        fileStorageService.deleteIfStored(existingPost.get().getCoverImageUrl());
        boolean wasHidden = existingPost.get().isHiddenFromPublic();
        postService.deleteById(id);
        redirectAttributes.addFlashAttribute("message", wasHidden ? "未公开文章已删除" : "文章已删除");
        return wasHidden ? "redirect:/space/drafts" : "redirect:/space";
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
                             @RequestParam(value = "parentCommentId", required = false) Long parentCommentId,
                             Principal principal,
                             RedirectAttributes redirectAttributes) {
        if (principal == null) {
            redirectAttributes.addFlashAttribute("error", "请先登录后再发表评论");
            return "redirect:/login";
        }
        Optional<Post> post = postService.findById(id);
        if (post.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "文章不存在");
            return "redirect:/posts";
        }
        if (!post.get().isPublished()) {
            redirectAttributes.addFlashAttribute("error", "未公开文章暂不支持评论");
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

        try {
            commentService.save(post.get(), userService.getByUsername(principal.getName()), content, parentCommentId);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/posts/" + id + "#comments";
        }
        redirectAttributes.addFlashAttribute("message", parentCommentId == null ? "评论已发布" : "回复已发布");
        return "redirect:/posts/" + id + "#comments";
    }

    @PostMapping("/{postId}/comments/delete/{commentId}")
    public String deleteComment(@PathVariable Long postId, @PathVariable Long commentId, Principal principal, RedirectAttributes redirectAttributes) {
        if (principal == null) {
            redirectAttributes.addFlashAttribute("error", "请先登录");
            return "redirect:/login";
        }

        User currentUser = userService.getByUsername(principal.getName());
        Optional<Post> post = postService.findById(postId);
        if (post.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "文章不存在");
            return "redirect:/posts";
        }
        Optional<com.example.blog.model.Comment> comment = commentService.findByIdAndPostId(commentId, postId);
        if (comment.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "评论不存在");
            return "redirect:/posts/" + postId + "#comments";
        }
        if (!commentService.canManage(comment.get(), currentUser)) {
            redirectAttributes.addFlashAttribute("error", "你没有权限删除这条评论");
            return "redirect:/posts/" + postId + "#comments";
        }

        commentService.delete(comment.get());
        redirectAttributes.addFlashAttribute("message", "评论已删除");
        return "redirect:/posts/" + postId + "#comments";
    }

    @PostMapping("/{id}/like")
    public String likePost(@PathVariable Long id, Principal principal, RedirectAttributes redirectAttributes) {
        if (principal == null) {
            redirectAttributes.addFlashAttribute("error", "请先登录后再点赞");
            return "redirect:/login";
        }

        Optional<Post> post = postService.findById(id);
        if (post.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "文章不存在");
            return "redirect:/posts";
        }
        if (!post.get().isPublished()) {
            redirectAttributes.addFlashAttribute("error", "未公开文章暂不支持点赞");
            return "redirect:/posts/" + id;
        }

        try {
            Post likedPost = postService.likePost(id, userService.getByUsername(principal.getName()));
            redirectAttributes.addFlashAttribute("message", "点赞成功，当前点赞数为 " + likedPost.getLikeCount());
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/posts/" + id;
    }

    private boolean isDraftAction(String action) {
        return "draft".equalsIgnoreCase(action);
    }

    private boolean isScheduleAction(String action) {
        return "schedule".equalsIgnoreCase(action);
    }

    private boolean isPublishable(Post post) {
        return StringUtils.hasText(post.getTitle()) && StringUtils.hasText(post.getContent()) && post.getCategory() != null;
    }

    private void validateTitle(String title) {
        if (!StringUtils.hasText(title)) {
            return;
        }
        if (title.trim().length() > MAX_TITLE_LENGTH) {
            throw new IllegalArgumentException("标题不能超过 " + MAX_TITLE_LENGTH + " 个字符");
        }
    }

    private void applyCover(Post target, String originalCoverUrl, MultipartFile coverFile, boolean useDefaultCover) {
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

    private void processPlanSelection(Post post,
                                      String planSelection,
                                      Long existingPlanId,
                                      String newPlanName,
                                      PlanAccessType newPlanAccessType,
                                      Integer planOrder,
                                      User author) {
        if ("none".equals(planSelection)) {
            post.setPlan(null);
            post.setPlanOrder(null);
        } else if ("existing".equals(planSelection) && existingPlanId != null) {
            Plan plan = planService.findById(existingPlanId)
                    .orElseThrow(() -> new IllegalArgumentException("选中的计划不存在"));
            if (!planService.canJoin(plan, author)) {
                throw new IllegalArgumentException("你不能将文章加入这个计划");
            }
            post.setPlan(plan);
            post.setPlanOrder(resolvePlanOrder(plan.getId(), planOrder, post.getId()));
        } else if ("new".equals(planSelection) && StringUtils.hasText(newPlanName)) {
            Plan newPlan = new Plan();
            newPlan.setName(newPlanName.trim());
            newPlan.setAuthor(author);
            newPlan.setAccessType(newPlanAccessType);
            newPlan.setExpectedCount(0);
            newPlan = planService.save(newPlan);
            post.setPlan(newPlan);
            post.setPlanOrder(planOrder != null ? planOrder : 1);
        }
    }

    private void populatePlanOptions(Model model, User currentUser) {
        model.addAttribute("userPlans", planService.getJoinablePlans(currentUser.getId()));
        model.addAttribute("planAccessTypes", PlanAccessType.values());
    }

    private void preselectPlan(Post post, Long planId, User currentUser) {
        planService.findById(planId)
                .filter(plan -> planService.canJoin(plan, currentUser))
                .ifPresent(plan -> {
                    post.setPlan(plan);
                    post.setPlanOrder(resolvePlanOrder(plan.getId(), null, null));
                });
    }

    private Integer resolvePlanOrder(Long planId, Integer requestedPlanOrder, Long currentPostId) {
        if (requestedPlanOrder != null && requestedPlanOrder > 0) {
            return requestedPlanOrder;
        }
        int nextPlanOrder = 1;
        for (Post planPost : postService.findByPlanId(planId)) {
            if (currentPostId != null && currentPostId.equals(planPost.getId())) {
                continue;
            }
            int currentOrder = planPost.getPlanOrder() == null ? 0 : planPost.getPlanOrder();
            if (currentOrder >= nextPlanOrder) {
                nextPlanOrder = currentOrder + 1;
            }
        }
        return nextPlanOrder;
    }
}
