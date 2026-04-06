package com.example.blog.model;

public enum PostStatus {
    DRAFT("draft", "草稿"),
    SCHEDULED("scheduled", "定时发布"),
    PUBLISHED("published", "已发布");

    private final String slug;
    private final String displayName;

    PostStatus(String slug, String displayName) {
        this.slug = slug;
        this.displayName = displayName;
    }

    public String getSlug() {
        return slug;
    }

    public String getDisplayName() {
        return displayName;
    }
}
