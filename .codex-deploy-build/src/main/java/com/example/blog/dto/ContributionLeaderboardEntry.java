package com.example.blog.dto;

public class ContributionLeaderboardEntry {
    private final Long userId;
    private final String username;
    private final long postCount;

    public ContributionLeaderboardEntry(Long userId, String username, long postCount) {
        this.userId = userId;
        this.username = username;
        this.postCount = postCount;
    }

    public Long getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public long getPostCount() {
        return postCount;
    }
}
