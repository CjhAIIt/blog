package com.example.blog.service;

import com.example.blog.dto.ContributionLeaderboardEntry;
import com.example.blog.model.EditorFont;
import com.example.blog.model.Post;
import com.example.blog.model.PostCategory;
import com.example.blog.model.PostStatus;
import com.example.blog.model.RankingPeriod;
import com.example.blog.model.User;
import com.example.blog.repository.PostRepository;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class PostService {
    private final PostRepository postRepository;
    private final MarkdownService markdownService;

    public PostService(PostRepository postRepository, MarkdownService markdownService) {
        this.postRepository = postRepository;
        this.markdownService = markdownService;
    }

    public List<Post> findAllIncludingDrafts() {
        return postRepository.findAll();
    }

    public List<Post> findAll() {
        return postRepository.findByStatusOrderByCreatedAtDesc(PostStatus.PUBLISHED);
    }

    public Page<Post> findAll(Pageable pageable) {
        return postRepository.findByStatusOrderByCreatedAtDesc(PostStatus.PUBLISHED, pageable);
    }

    public List<Post> findByCategory(PostCategory category) {
        return postRepository.findByCategoryAndStatusOrderByCreatedAtDesc(category, PostStatus.PUBLISHED);
    }

    public Page<Post> findByCategory(PostCategory category, Pageable pageable) {
        return postRepository.findByCategoryAndStatusOrderByCreatedAtDesc(category, PostStatus.PUBLISHED, pageable);
    }

    public Optional<Post> findById(Long id) {
        return postRepository.findById(id);
    }

    public List<Post> findPublishedByAuthorId(Long authorId) {
        return postRepository.findByAuthorIdAndStatusOrderByCreatedAtDesc(authorId, PostStatus.PUBLISHED);
    }

    public Page<Post> findPublishedByAuthorId(Long authorId, Pageable pageable) {
        return postRepository.findByAuthorIdAndStatusOrderByCreatedAtDesc(authorId, PostStatus.PUBLISHED, pageable);
    }

    public List<Post> findDraftsByAuthorId(Long authorId) {
        return postRepository.findByAuthorIdAndStatusOrderByUpdatedAtDesc(authorId, PostStatus.DRAFT);
    }

    public long countDraftsByAuthorId(Long authorId) {
        return findDraftsByAuthorId(authorId).size();
    }

    public List<Post> findByKeyword(String keyword) {
        return postRepository.findByKeyword(keyword);
    }

    public Page<Post> findByKeyword(String keyword, Pageable pageable) {
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
        if (wasDraft) {
            post.setCreatedAt(LocalDateTime.now());
        }
        return save(post);
    }

    @Transactional
    public Post likePost(Long id) {
        if (postRepository.incrementLikeCount(id) == 0) {
            throw new IllegalArgumentException("文章不存在");
        }
        return postRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("文章不存在"));
    }

    public List<ContributionLeaderboardEntry> findContributionLeaderboard(RankingPeriod period, int limit) {
        Pageable pageable = PageRequest.of(0, Math.max(1, limit));
        return postRepository.findContributionLeaderboard(period.startAt(), pageable);
    }

    public List<Post> findLikeLeaderboard(RankingPeriod period, int limit) {
        Pageable pageable = PageRequest.of(0, Math.max(1, limit));
        return postRepository.findTopByLikesSince(period.startAt(), pageable);
    }

    public void deleteById(Long id) {
        postRepository.deleteById(id);
    }

    public boolean canManage(Post post, String username) {
        return post != null
                && post.getAuthor() != null
                && username != null
                && username.equals(post.getAuthor().getUsername());
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
}
