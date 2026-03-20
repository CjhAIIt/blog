package com.example.blog.controller;

import com.example.blog.model.Post;
import com.example.blog.model.PostCategory;
import com.example.blog.model.User;
import com.example.blog.service.CommentService;
import com.example.blog.service.MarkdownService;
import com.example.blog.service.PostService;
import com.example.blog.service.UserService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/posts")
public class PostController {
    private final PostService postService;
    private final UserService userService;
    private final MarkdownService markdownService;
    private final CommentService commentService;

    public PostController(PostService postService, UserService userService, MarkdownService markdownService, CommentService commentService) {
        this.postService = postService;
        this.userService = userService;
        this.markdownService = markdownService;
        this.commentService = commentService;
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
    public String viewPost(@PathVariable Long id, Model model, Principal principal, RedirectAttributes redirectAttributes) {
        Optional<Post> post = postService.findById(id);
        if (post.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "文章不存在");
            return "redirect:/posts";
        }

        Post currentPost = post.get();
        model.addAttribute("post", currentPost);
        model.addAttribute("renderedContent", postService.renderMarkdown(currentPost));
        model.addAttribute("canManage", principal != null && postService.canManage(currentPost, principal.getName()));
        model.addAttribute("comments", commentService.findByPostId(currentPost.getId()));
        model.addAttribute("commentCount", commentService.countByPostId(currentPost.getId()));
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
        model.addAttribute("post", post);
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
        model.addAttribute("selectedCategory", post.get().getCategorySlug());
        return "posts/form";
    }

    @PostMapping
    public String savePost(@ModelAttribute Post post, Principal principal, RedirectAttributes redirectAttributes) {
        if (principal == null) {
            redirectAttributes.addFlashAttribute("error", "请先登录");
            return "redirect:/login";
        }
        if (!StringUtils.hasText(post.getTitle()) || !StringUtils.hasText(post.getContent()) || post.getCategory() == null) {
            redirectAttributes.addFlashAttribute("error", "标题、分类和正文都不能为空");
            return "redirect:/posts/new";
        }

        User author = userService.getByUsername(principal.getName());
        post.setAuthor(author);
        postService.save(post);
        redirectAttributes.addFlashAttribute("message", "Markdown 文章发布成功");
        return "redirect:/posts/" + post.getId();
    }

    @PostMapping("/update/{id}")
    public String updatePost(@PathVariable Long id,
                             @ModelAttribute Post post,
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
        if (!StringUtils.hasText(post.getTitle()) || !StringUtils.hasText(post.getContent()) || post.getCategory() == null) {
            redirectAttributes.addFlashAttribute("error", "标题、分类和正文都不能为空");
            return "redirect:/posts/edit/" + id;
        }

        Post updatedPost = existingPost.get();
        updatedPost.setTitle(post.getTitle().trim());
        updatedPost.setContent(post.getContent());
        updatedPost.setCategory(post.getCategory());
        postService.save(updatedPost);

        redirectAttributes.addFlashAttribute("message", "文章更新成功");
        return "redirect:/posts/" + id;
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

        postService.deleteById(id);
        redirectAttributes.addFlashAttribute("message", "文章删除成功");
        return "redirect:/space";
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
        if (!StringUtils.hasText(content)) {
            redirectAttributes.addFlashAttribute("error", "评论内容不能为空");
            return "redirect:/posts/" + id;
        }
        if (content.trim().length() > 500) {
            redirectAttributes.addFlashAttribute("error", "评论内容不能超过 500 个字符");
            return "redirect:/posts/" + id;
        }

        User author = userService.getByUsername(principal.getName());
        commentService.save(post.get(), author, content);
        redirectAttributes.addFlashAttribute("message", "评论发布成功");
        return "redirect:/posts/" + id + "#comments";
    }
}
