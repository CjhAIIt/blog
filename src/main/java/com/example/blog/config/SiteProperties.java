package com.example.blog.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;

@Component
public class SiteProperties {
    private final String sourceRepoUrl;
    private final String uploadDir;

    public SiteProperties(@Value("${app.site.source-repo-url:https://github.com/CjhAIIt/blog}") String sourceRepoUrl,
                          @Value("${app.storage.upload-dir:./uploads}") String uploadDir) {
        this.sourceRepoUrl = sourceRepoUrl;
        this.uploadDir = uploadDir;
    }

    public String getSourceRepoUrl() {
        return sourceRepoUrl;
    }

    public Path getUploadRoot() {
        return Paths.get(uploadDir).toAbsolutePath().normalize();
    }
}
