package com.example.blog.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarkdownServiceHtmlSanitizationTest {
    private final MarkdownService markdownService = new MarkdownService();

    @Test
    void render_allows_basic_html_tags() {
        String html = markdownService.render("Hello <strong>World</strong><div>OK</div>");
        assertTrue(html.contains("<strong>World</strong>"));
        assertTrue(html.contains("<div>OK</div>"));
    }

    @Test
    void render_strips_script_tags() {
        String html = markdownService.render("<script>alert(1)</script><p>safe</p>");
        assertFalse(html.contains("<script"));
        assertTrue(html.contains("<p>safe</p>"));
    }

    @Test
    void render_keeps_relative_image_urls() {
        String html = markdownService.render("<img src=\"/uploads/covers/test.png\" alt=\"x\">");
        assertTrue(html.contains("src=\"/uploads/covers/test.png\""));
    }
}

