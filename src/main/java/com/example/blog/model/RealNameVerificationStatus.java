package com.example.blog.model;

public enum RealNameVerificationStatus {
    PENDING("待审核"),
    APPROVED("已通过"),
    REJECTED("已驳回");

    private final String displayName;

    RealNameVerificationStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
