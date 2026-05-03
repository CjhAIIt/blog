package com.example.blog.dto;

public class ContributionLeaderboardEntry {
    private final Long userId;
    private final String username;
    private final long postCount;
    private final long totalPostCount;

    public ContributionLeaderboardEntry(Long userId, String username, long postCount) {
        this(userId, username, postCount, postCount);
    }

    public ContributionLeaderboardEntry(Long userId, String username, long postCount, long totalPostCount) {
        this.userId = userId;
        this.username = username;
        this.postCount = postCount;
        this.totalPostCount = totalPostCount;
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

    public long getTotalPostCount() {
        return totalPostCount;
    }
}
