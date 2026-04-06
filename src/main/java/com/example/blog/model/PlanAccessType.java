package com.example.blog.model;

public enum PlanAccessType {
    COLLABORATIVE("collaborative", "共创计划"),
    PRIVATE("private", "私密计划");

    private final String slug;
    private final String displayName;

    PlanAccessType(String slug, String displayName) {
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
