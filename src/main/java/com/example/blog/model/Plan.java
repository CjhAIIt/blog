package com.example.blog.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Entity
@Table(name = "plans", indexes = {
        @Index(name = "idx_plans_public_updated", columnList = "is_public, updated_at"),
        @Index(name = "idx_plans_access_public_updated", columnList = "access_type, is_public, updated_at"),
        @Index(name = "idx_plans_author_updated", columnList = "author_id, updated_at")
})
public class Plan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "cover_image_url", length = 512)
    private String coverImageUrl;

    @Column(name = "is_public")
    private boolean isPublic = true;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "access_type", nullable = false, length = 32)
    private PlanAccessType accessType = PlanAccessType.PRIVATE;

    @Column(name = "expected_count")
    private int expectedCount;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "status", nullable = false, length = 16)
    private PlanStatus status = PlanStatus.IN_PROGRESS;

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
        this.name = StringUtils.hasText(name) ? name.trim() : null;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = StringUtils.hasText(description) ? description.trim() : null;
    }

    public String getCoverImageUrl() {
        return coverImageUrl;
    }

    public void setCoverImageUrl(String coverImageUrl) {
        this.coverImageUrl = StringUtils.hasText(coverImageUrl) ? coverImageUrl.trim() : null;
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
        return Math.max(0, expectedCount);
    }

    public void setExpectedCount(int expectedCount) {
        this.expectedCount = Math.max(0, expectedCount);
    }

    public PlanStatus getStatus() {
        return status == null ? PlanStatus.IN_PROGRESS : status;
    }

    public void setStatus(PlanStatus status) {
        this.status = status == null ? PlanStatus.IN_PROGRESS : status;
    }

    public String getStatusDisplayName() {
        return getStatus().getDisplayName();
    }

    public boolean isCompleted() {
        return getStatus() == PlanStatus.COMPLETED;
    }

    public boolean isShelved() {
        return getStatus() == PlanStatus.SHELVED;
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

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
