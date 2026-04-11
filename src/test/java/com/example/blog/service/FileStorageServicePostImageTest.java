package com.example.blog.service;

import com.example.blog.config.SiteProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class FileStorageServicePostImageTest {
    @TempDir
    Path tempDir;

    @Test
    void storePostImage_savesFileIntoPostsFolder() throws IOException {
        FileStorageService fileStorageService = new FileStorageService(
                new SiteProperties("https://example.com/repo", tempDir.toString())
        );
        MockMultipartFile file = new MockMultipartFile(
                "imageFile",
                "diagram.png",
                "image/png",
                "demo image".getBytes()
        );

        String storedUrl = fileStorageService.storePostImage(file);

        assertTrue(storedUrl.startsWith("/uploads/posts/"));
        Path storedFile = tempDir.resolve(storedUrl.substring("/uploads/".length()).replace('/', File.separatorChar));
        assertTrue(Files.exists(storedFile));
    }

    @Test
    void storePostImage_acceptsJfifEvenWithoutImageContentType() throws IOException {
        FileStorageService fileStorageService = new FileStorageService(
                new SiteProperties("https://example.com/repo", tempDir.toString())
        );
        MockMultipartFile file = new MockMultipartFile(
                "imageFile",
                "camera-export.jfif",
                "application/octet-stream",
                "demo image".getBytes()
        );

        String storedUrl = fileStorageService.storePostImage(file);

        assertTrue(storedUrl.startsWith("/uploads/posts/"));
        Path storedFile = tempDir.resolve(storedUrl.substring("/uploads/".length()).replace('/', File.separatorChar));
        assertTrue(Files.exists(storedFile));
    }
}
