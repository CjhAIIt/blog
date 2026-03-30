package com.example.blog.model;

public enum NotificationType {
    POST_COMMENT("博客新评论"),
    COMMENT_REPLY("评论新回复");

    private final String label;

    NotificationType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
