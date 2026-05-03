package com.example.blog.dto;

import java.time.LocalDateTime;

public class SpaceStyleVersionDto {
    private Long id;
    private String versionName;
    private LocalDateTime createdAt;
    private SpaceStyleDto style;

    public SpaceStyleVersionDto(Long id, String versionName, LocalDateTime createdAt, SpaceStyleDto style) {
        this.id = id;
        this.versionName = versionName;
        this.createdAt = createdAt;
        this.style = style;
    }

    public Long getId() {
        return id;
    }

    public String getVersionName() {
        return versionName;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public SpaceStyleDto getStyle() {
        return style;
    }
}
