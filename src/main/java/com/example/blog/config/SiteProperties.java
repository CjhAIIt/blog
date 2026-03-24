package com.example.blog.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SiteProperties {
    private final String sourceRepoUrl;

    public SiteProperties(@Value("${app.site.source-repo-url:https://github.com/CjhAIIt/blog}") String sourceRepoUrl) {
        this.sourceRepoUrl = sourceRepoUrl;
    }

    public String getSourceRepoUrl() {
        return sourceRepoUrl;
    }
}
