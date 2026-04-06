package com.example.blog.model;

import java.util.Arrays;
import java.util.Optional;

public enum PostCategory {
    FRONTEND_BACKEND("frontend-backend", "前后端"),
    EMBEDDED("embedded", "嵌入式"),
    BIG_DATA("big-data", "大数据"),
    PROJECT("project", "项目"),
    TALK("talk", "杂谈"),
    ALGORITHM("algorithm", "算法");

    private final String slug;
    private final String displayName;

    PostCategory(String slug, String displayName) {
        this.slug = slug;
        this.displayName = displayName;
    }

    public String getSlug() {
        return slug;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static Optional<PostCategory> fromSlug(String slug) {
        return Arrays.stream(values())
                .filter(category -> category.slug.equalsIgnoreCase(slug))
                .findFirst();
    }
}
