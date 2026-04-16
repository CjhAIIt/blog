package com.example.blog.model;

import java.util.Arrays;

public enum PlanStatus {
    IN_PROGRESS("进行中"),
    COMPLETED("已完成"),
    SHELVED("已搁置");

    private final String displayName;

    PlanStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static PlanStatus fromValue(String value) {
        if (value == null || value.isBlank()) {
            return IN_PROGRESS;
        }
        String normalized = value.trim();
        if ("0".equals(normalized)) {
            return IN_PROGRESS;
        }
        if ("1".equals(normalized)) {
            return COMPLETED;
        }
        if ("2".equals(normalized)) {
            return SHELVED;
        }
        return Arrays.stream(values())
                .filter(status -> status.name().equalsIgnoreCase(normalized))
                .findFirst()
                .orElse(IN_PROGRESS);
    }
}
