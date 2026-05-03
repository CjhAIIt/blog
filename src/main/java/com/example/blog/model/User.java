package com.example.blog.model;

import com.example.blog.service.SensitiveFieldCrypto;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_users_verification_queue", columnList = "real_name_verification_status, real_name_verification_submitted_at, created_at"),
        @Index(name = "idx_users_role_created", columnList = "role, created_at")
})
public class User implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified;

    @Column(name = "real_name", length = 16)
    private String realName;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Column(name = "qq_encrypted")
    private String qqEncrypted;

    @Column(name = "github_url_encrypted")
    private String githubUrlEncrypted;

    @Column(name = "personal_blog_url")
    private String personalBlogUrl;

    @Column(name = "avatar_image_url")
    private String avatarImageUrl;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "password_updated_at")
    private LocalDateTime passwordUpdatedAt;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "role", nullable = false, length = 16)
    private UserRole role = UserRole.USER;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "real_name_verification_status", nullable = false, length = 16)
    private RealNameVerificationStatus realNameVerificationStatus = RealNameVerificationStatus.PENDING;

    @Column(name = "real_name_verification_submitted_at")
    private LocalDateTime realNameVerificationSubmittedAt;

    @Column(name = "real_name_verification_reviewed_at")
    private LocalDateTime realNameVerificationReviewedAt;

    @OneToMany(mappedBy = "author", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Post> posts;

    public User() {
        this.createdAt = LocalDateTime.now();
        this.passwordUpdatedAt = LocalDateTime.now();
        this.emailVerified = true;
    }

    public User(String username, String password, String email) {
        this();
        this.username = username;
        this.password = password;
        this.email = email;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
        this.passwordUpdatedAt = LocalDateTime.now();
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    public String getRealName() {
        return realName;
    }

    public void setRealName(String realName) {
        this.realName = StringUtils.hasText(realName) ? realName.trim() : null;
    }

    public boolean hasRealName() {
        return StringUtils.hasText(realName);
    }

    public String getDisplayName() {
        return hasRealName() ? realName : username;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getQq() {
        return SensitiveFieldCrypto.decrypt(qqEncrypted);
    }

    public void setQq(String qq) {
        this.qqEncrypted = SensitiveFieldCrypto.encrypt(qq);
    }

    public String getGithubUrl() {
        return SensitiveFieldCrypto.decrypt(githubUrlEncrypted);
    }

    public void setGithubUrl(String githubUrl) {
        this.githubUrlEncrypted = SensitiveFieldCrypto.encrypt(githubUrl);
    }

    public String getPersonalBlogUrl() {
        return personalBlogUrl;
    }

    public void setPersonalBlogUrl(String personalBlogUrl) {
        this.personalBlogUrl = personalBlogUrl;
    }

    public String getAvatarImageUrl() {
        return avatarImageUrl;
    }

    public void setAvatarImageUrl(String avatarImageUrl) {
        this.avatarImageUrl = StringUtils.hasText(avatarImageUrl) ? avatarImageUrl.trim() : null;
    }

    public String getDisplayAvatarUrl() {
        return StringUtils.hasText(avatarImageUrl) ? avatarImageUrl : "/images/default-avatar.svg";
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getPasswordUpdatedAt() {
        return passwordUpdatedAt;
    }

    public void setPasswordUpdatedAt(LocalDateTime passwordUpdatedAt) {
        this.passwordUpdatedAt = passwordUpdatedAt;
    }

    public List<Post> getPosts() {
        return posts;
    }

    public void setPosts(List<Post> posts) {
        this.posts = posts;
    }

    public UserRole getRole() {
        return role == null ? UserRole.USER : role;
    }

    public void setRole(UserRole role) {
        this.role = role == null ? UserRole.USER : role;
    }

    public boolean isAdmin() {
        return getRole() == UserRole.ADMIN;
    }

    public RealNameVerificationStatus getRealNameVerificationStatus() {
        if (realNameVerificationStatus != null) {
            return realNameVerificationStatus;
        }
        return isAdmin() ? RealNameVerificationStatus.APPROVED : RealNameVerificationStatus.PENDING;
    }

    public void setRealNameVerificationStatus(RealNameVerificationStatus realNameVerificationStatus) {
        this.realNameVerificationStatus = realNameVerificationStatus == null
                ? RealNameVerificationStatus.PENDING
                : realNameVerificationStatus;
    }

    public LocalDateTime getRealNameVerificationSubmittedAt() {
        return realNameVerificationSubmittedAt;
    }

    public void setRealNameVerificationSubmittedAt(LocalDateTime realNameVerificationSubmittedAt) {
        this.realNameVerificationSubmittedAt = realNameVerificationSubmittedAt;
    }

    public LocalDateTime getRealNameVerificationReviewedAt() {
        return realNameVerificationReviewedAt;
    }

    public void setRealNameVerificationReviewedAt(LocalDateTime realNameVerificationReviewedAt) {
        this.realNameVerificationReviewedAt = realNameVerificationReviewedAt;
    }

    public boolean isRealNameVerified() {
        return hasRealName() && getRealNameVerificationStatus() == RealNameVerificationStatus.APPROVED;
    }

    public boolean isRealNameVerificationPending() {
        return hasRealName() && getRealNameVerificationStatus() == RealNameVerificationStatus.PENDING;
    }

    public boolean isRealNameVerificationRejected() {
        return hasRealName() && getRealNameVerificationStatus() == RealNameVerificationStatus.REJECTED;
    }

    public boolean canPublishPosts() {
        return true;
    }

    public String getRealNameVerificationStatusDisplayName() {
        if (!hasRealName() && !isAdmin()) {
            return "未提交";
        }
        return getRealNameVerificationStatus().getDisplayName();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (isAdmin()) {
            return List.of(
                    new SimpleGrantedAuthority("ROLE_USER"),
                    new SimpleGrantedAuthority("ROLE_ADMIN")
            );
        }
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
