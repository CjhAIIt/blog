package com.example.blog.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "plans")
public class Plan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "cover_image_url")
    private String coverImageUrl;

    @Column(name = "is_public")
    private boolean isPublic = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "access_type", nullable = false, length = 32)
    private PlanAccessType accessType = PlanAccessType.PRIVATE;

    @Column(name = "expected_count")
    private int expectedCount;

    @Column(name = "status")
    private int status = 0; // 0: in progress, 1: completed, 2: shelved

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Plan() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCoverImageUrl() {
        return coverImageUrl;
    }

    public void setCoverImageUrl(String coverImageUrl) {
        this.coverImageUrl = coverImageUrl;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public void setPublic(boolean isPublic) {
        this.isPublic = isPublic;
    }

    public PlanAccessType getAccessType() {
        if (accessType != null) {
            return accessType;
        }
        return isPublic ? PlanAccessType.COLLABORATIVE : PlanAccessType.PRIVATE;
    }

    public void setAccessType(PlanAccessType accessType) {
        this.accessType = accessType == null ? PlanAccessType.PRIVATE : accessType;
        this.isPublic = this.accessType == PlanAccessType.COLLABORATIVE;
    }

    public String getAccessTypeDisplayName() {
        return getAccessType().getDisplayName();
    }

    public boolean isCollaborative() {
        return getAccessType() == PlanAccessType.COLLABORATIVE;
    }

    public boolean isPrivatePlan() {
        return getAccessType() == PlanAccessType.PRIVATE;
    }

    public int getExpectedCount() {
        return expectedCount;
    }

    public void setExpectedCount(int expectedCount) {
        this.expectedCount = expectedCount;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
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
}
