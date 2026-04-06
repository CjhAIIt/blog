package com.example.blog.service;

import com.example.blog.config.SiteProperties;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MarkdownImportCompatibilityTest {
    private final FileStorageService fileStorageService =
            new FileStorageService(new SiteProperties("https://example.com/repo", "./uploads"));
    private final MarkdownService markdownService = new MarkdownService();

    @Test
    void loadMarkdownContentStripsUtf8Bom() {
        byte[] body = "# 标题\n\n正文".getBytes(StandardCharsets.UTF_8);
        byte[] bytes = new byte[body.length + 3];
        bytes[0] = (byte) 0xEF;
        bytes[1] = (byte) 0xBB;
        bytes[2] = (byte) 0xBF;
        System.arraycopy(body, 0, bytes, 3, body.length);

        MockMultipartFile file = new MockMultipartFile(
                "markdownFile",
                "post.md",
                "text/markdown",
                bytes
        );

        assertEquals("# 标题\n\n正文", fileStorageService.loadMarkdownContent(file));
    }

    @Test
    void loadMarkdownContentFallsBackToGb18030() {
        Charset gb18030 = Charset.forName("GB18030");
        MockMultipartFile file = new MockMultipartFile(
                "markdownFile",
                "post.md",
                "text/markdown",
                "# 标题\n\n中文内容".getBytes(gb18030)
        );

        assertEquals("# 标题\n\n中文内容", fileStorageService.loadMarkdownContent(file));
    }

    @Test
    void normalizeImportedContentHandlesBomBeforeFirstHeading() {
        String markdown = "\uFEFF# 标题\r\n\r\n正文内容";

        assertEquals("标题", markdownService.guessTitle(markdown, "fallback"));
        assertEquals("正文内容", markdownService.normalizeImportedContent(markdown));
    }
}
