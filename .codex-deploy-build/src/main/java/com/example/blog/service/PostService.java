package com.example.blog.service;

import com.example.blog.dto.ContributionLeaderboardEntry;
import com.example.blog.model.EditorFont;
import com.example.blog.model.Post;
import com.example.blog.model.PostCategory;
import com.example.blog.model.PostLike;
import com.example.blog.model.PostStatus;
import com.example.blog.model.RankingPeriod;
import com.example.blog.model.User;
import com.example.blog.repository.PostLikeRepository;
import com.example.blog.repository.PostRepository;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class PostService {
    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;
    private final MarkdownService markdownService;
    private final CommentService commentService;
    private final NotificationService notificationService;

    public PostService(PostRepository postRepository,
                       PostLikeRepository postLikeRepository,
                       MarkdownService markdownService,
                       CommentService commentService,
                       NotificationService notificationService) {
        this.postRepository = postRepository;
        this.postLikeRepository = postLikeRepository;
        this.markdownService = markdownService;
        this.commentService = commentService;
        this.notificationService = notificationService;
    }

    public List<Post> findAllIncludingDrafts() {
        publishScheduledPosts();
        return postRepository.findAll();
    }

    public List<Post> findAll() {
        publishScheduledPosts();
        return postRepository.findByStatusOrderByCreatedAtDesc(PostStatus.PUBLISHED);
    }

    public Page<Post> findAll(Pageable pageable) {
        publishScheduledPosts();
        return postRepository.findByStatusOrderByCreatedAtDesc(PostStatus.PUBLISHED, pageable);
    }

    public List<Post> findByCategory(PostCategory category) {
        publishScheduledPosts();
        return postRepository.findByCategoryAndStatusOrderByCreatedAtDesc(category, PostStatus.PUBLISHED);
    }

    public Page<Post> findByCategory(PostCategory category, Pageable pageable) {
        publishScheduledPosts();
        return postRepository.findByCategoryAndStatusOrderByCreatedAtDesc(category, PostStatus.PUBLISHED, pageable);
    }

    public List<Post> findByPlanId(Long planId) {
        return postRepository.findByPlanIdOrderByPlanOrderAsc(planId);
    }

    public List<Post> findPublishedByPlanId(Long planId) {
        return postRepository.findByPlanIdAndStatusOrderByPlanOrderAsc(planId, PostStatus.PUBLISHED);
    }

    public Optional<Post> findById(Long id) {
        publishScheduledPosts();
        return postRepository.findById(id);
    }

    public Optional<Post> findVisibleById(Long id) {
        publishScheduledPosts();
        return postRepository.findPublicById(id, LocalDateTime.now());
    }

    public List<Post> findPublishedByAuthorId(Long authorId) {
        publishScheduledPosts();
        return postRepository.findByAuthorIdAndStatusOrderByCreatedAtDesc(authorId, PostStatus.PUBLISHED);
    }

    public Page<Post> findPublishedByAuthorId(Long authorId, Pageable pageable) {
        publishScheduledPosts();
        return postRepository.findByAuthorIdAndStatusOrderByCreatedAtDesc(authorId, PostStatus.PUBLISHED, pageable);
    }

    public List<Post> findDraftsByAuthorId(Long authorId) {
        return postRepository.findByAuthorIdAndStatusOrderByUpdatedAtDesc(authorId, PostStatus.DRAFT);
    }

    public long countDraftsByAuthorId(Long authorId) {
        return findDraftsByAuthorId(authorId).size();
    }

    public List<Post> findByKeyword(String keyword) {
        publishScheduledPosts();
        return postRepository.findByKeyword(keyword);
    }

    public Page<Post> findByKeyword(String keyword, Pageable pageable) {
        publishScheduledPosts();
        return postRepository.findByKeyword(keyword, pageable);
    }

    public Post save(Post post) {
        if (post.getCreatedAt() == null) {
            post.setCreatedAt(LocalDateTime.now());
        }
        post.setUpdatedAt(LocalDateTime.now());
        if (post.getLikeCount() < 0) {
            post.setLikeCount(0);
        }
        if (!StringUtils.hasText(post.getFontKey())) {
            post.setFontKey(EditorFont.DEFAULT.getKey());
        }
        return postRepository.save(post);
    }

    public Post saveDraft(Post post) {
        post.setStatus(PostStatus.DRAFT);
        post.setScheduledPublishAt(null);
        if (StringUtils.hasText(post.getTitle())) {
            post.setTitle(post.getTitle().trim());
        } else {
            post.setTitle("未命名草稿");
        }
        return save(post);
    }

    public Post publish(Post post, boolean wasDraft) {
        post.setStatus(PostStatus.PUBLISHED);
        post.setTitle(post.getTitle().trim());
        post.setScheduledPublishAt(null);
        if (wasDraft) {
            post.setCreatedAt(LocalDateTime.now());
        }
        return save(post);
    }

    public Post schedule(Post post, boolean wasDraft, LocalDateTime scheduledPublishAt) {
        if (scheduledPublishAt == null || !scheduledPublishAt.isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("定时发布时间必须晚于当前时间");
        }
        post.setStatus(PostStatus.SCHEDULED);
        post.setTitle(post.getTitle().trim());
        post.setScheduledPublishAt(scheduledPublishAt);
        if (wasDraft || post.getCreatedAt() == null || post.getCreatedAt().isBefore(scheduledPublishAt)) {
            post.setCreatedAt(scheduledPublishAt);
        }
        return save(post);
    }

    @Transactional
    public Post likePost(Long id, User user) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("请先登录后再点赞");
        }
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("文章不存在"));
        if (!post.isPublished()) {
            throw new IllegalArgumentException("只有已发布文章可以点赞");
        }
        if (postLikeRepository.existsByUserIdAndPostId(user.getId(), id)) {
            throw new IllegalArgumentException("你已经给这篇文章点过赞了");
        }

        postLikeRepository.save(new PostLike(user, post));
        post.setLikeCount(Math.toIntExact(postLikeRepository.countByPostId(id)));
        return postRepository.save(post);
    }

    public boolean hasUserLikedPost(Long postId, User user) {
        return user != null
                && user.getId() != null
                && postId != null
                && postLikeRepository.existsByUserIdAndPostId(user.getId(), postId);
    }

    public List<ContributionLeaderboardEntry> findContributionLeaderboard(RankingPeriod period, int limit) {
        publishScheduledPosts();
        Pageable pageable = PageRequest.of(0, Math.max(1, limit));
        return postRepository.findContributionLeaderboard(period.startAt(), pageable);
    }

    public List<Post> findLikeLeaderboard(RankingPeriod period, int limit) {
        publishScheduledPosts();
        Pageable pageable = PageRequest.of(0, Math.max(1, limit));
        return postRepository.findTopByLikesSince(period.startAt(), pageable);
    }

    @Transactional
    public void deleteById(Long id) {
        notificationService.deleteByPostId(id);
        commentService.deleteByPostId(id);
        postLikeRepository.deleteByPostId(id);
        postRepository.deleteById(id);
    }

    public boolean canManage(Post post, User user) {
        if (post == null || user == null) {
            return false;
        }
        if (user.isAdmin()) {
            return true;
        }
        return post.getAuthor() != null
                && user.getUsername() != null
                && user.getUsername().equals(post.getAuthor().getUsername());
    }

    public String renderMarkdown(Post post) {
        return markdownService.render(post == null ? null : post.getContent());
    }

    public String excerpt(Post post, int maxLength) {
        return markdownService.excerpt(post == null ? null : post.getContent(), maxLength);
    }

    public Post createImportedDraft(User author, PostCategory category, String originalFileName, String markdownContent) {
        Post post = new Post();
        post.setAuthor(author);
        post.setCategory(category == null ? PostCategory.PROJECT : category);
        post.setTitle(markdownService.guessTitle(markdownContent, markdownService.guessTitleFromFilename(originalFileName)));
        post.setContent(markdownService.normalizeImportedContent(markdownContent));
        post.setFontKey(EditorFont.DEFAULT.getKey());
        return saveDraft(post);
    }

    @Transactional
    @Scheduled(cron = "0 * * * * *")
    public void publishScheduledPosts() {
        LocalDateTime now = LocalDateTime.now();
        List<Post> duePosts = postRepository.findByStatusAndScheduledPublishAtLessThanEqualOrderByScheduledPublishAtAsc(
                PostStatus.SCHEDULED,
                now
        );
        for (Post post : duePosts) {
            post.setStatus(PostStatus.PUBLISHED);
            if (post.getScheduledPublishAt() != null) {
                post.setCreatedAt(post.getScheduledPublishAt());
            }
            post.setScheduledPublishAt(null);
            save(post);
        }
    }
}
