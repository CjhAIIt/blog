package com.example.blog.model;

import jakarta.persistence.*;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Entity
@Table(name = "posts")
public class Post {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private PostCategory category = PostCategory.PROJECT;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 32)
    private PostStatus status = PostStatus.PUBLISHED;

    @Column(name = "cover_image_url")
    private String coverImageUrl;

    @Column(name = "font_key", length = 64)
    private String fontKey = EditorFont.DEFAULT.getKey();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "like_count")
    private Integer likeCount;

    public Post() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.likeCount = 0;
    }

    public Post(String title, String content, PostCategory category, User author) {
        this();
        this.title = title;
        this.content = content;
        this.category = category;
        this.author = author;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public PostCategory getCategory() {
        return category;
    }

    public void setCategory(PostCategory category) {
        this.category = category;
    }

    public String getCategoryDisplayName() {
        return category == null ? "" : category.getDisplayName();
    }

    public String getCategorySlug() {
        return category == null ? "" : category.getSlug();
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content == null ? null : content.replace("\r\n", "\n");
    }

    public PostStatus getStatus() {
        return status;
    }

    public void setStatus(PostStatus status) {
        this.status = status == null ? PostStatus.PUBLISHED : status;
    }

    public boolean isDraft() {
        return resolveStatus() == PostStatus.DRAFT;
    }

    public boolean isPublished() {
        return resolveStatus() == PostStatus.PUBLISHED;
    }

    public String getStatusDisplayName() {
        return resolveStatus().getDisplayName();
    }

    public PostStatus resolveStatus() {
        return status == null ? PostStatus.PUBLISHED : status;
    }

    public String getCoverImageUrl() {
        return coverImageUrl;
    }

    public void setCoverImageUrl(String coverImageUrl) {
        this.coverImageUrl = StringUtils.hasText(coverImageUrl) ? coverImageUrl.trim() : null;
    }

    public String getDisplayCoverUrl() {
        return StringUtils.hasText(coverImageUrl) ? coverImageUrl : "/images/default-cover.svg";
    }

    public String getFontKey() {
        return StringUtils.hasText(fontKey) ? fontKey : EditorFont.DEFAULT.getKey();
    }

    public void setFontKey(String fontKey) {
        this.fontKey = EditorFont.fromKey(fontKey).getKey();
    }

    public EditorFont getEditorFont() {
        return EditorFont.fromKey(fontKey);
    }

    public User getAuthor() {
        return author;
    }

    public void setAuthor(User author) {
        this.author = author;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public int getLikeCount() {
        return likeCount == null ? 0 : likeCount;
    }

    public void setLikeCount(Integer likeCount) {
        this.likeCount = likeCount == null ? 0 : likeCount;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
