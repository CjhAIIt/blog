package com.example.blog.service;

import org.commonmark.Extension;
import org.commonmark.ext.autolink.AutolinkExtension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.ext.heading.anchor.HeadingAnchorExtension;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.commonmark.renderer.text.TextContentRenderer;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class MarkdownService {
    private static final Pattern FIRST_H1_PATTERN = Pattern.compile("(?m)^#\\s+(.+?)\\s*$");

    private final Parser parser;
    private final HtmlRenderer htmlRenderer;
    private final TextContentRenderer textRenderer;

    public MarkdownService() {
        List<Extension> extensions = List.of(
                TablesExtension.create(),
                AutolinkExtension.create(),
                HeadingAnchorExtension.create()
        );
        this.parser = Parser.builder().extensions(extensions).build();
        this.htmlRenderer = HtmlRenderer.builder()
                .extensions(extensions)
                .escapeHtml(true)
                .sanitizeUrls(true)
                .build();
        this.textRenderer = TextContentRenderer.builder().build();
    }

    public String render(String markdown) {
        if (!StringUtils.hasText(markdown)) {
            return "";
        }
        return htmlRenderer.render(parse(stripBom(markdown)));
    }

    public String toPlainText(String markdown) {
        if (!StringUtils.hasText(markdown)) {
            return "";
        }
        return textRenderer.render(parse(stripBom(markdown))).trim();
    }

    public String excerpt(String markdown, int maxLength) {
        String plainText = toPlainText(markdown);
        if (plainText.length() <= maxLength) {
            return plainText;
        }
        return plainText.substring(0, Math.max(0, maxLength - 3)) + "...";
    }

    public String guessTitle(String markdown, String fallbackTitle) {
        return firstHeading(markdown)
                .orElseGet(() -> StringUtils.hasText(fallbackTitle) ? fallbackTitle.trim() : "未命名导入文章");
    }

    public String guessTitleFromFilename(String originalFilename) {
        if (!StringUtils.hasText(originalFilename)) {
            return "未命名导入文章";
        }
        int dotIndex = originalFilename.lastIndexOf('.');
        String value = dotIndex > 0 ? originalFilename.substring(0, dotIndex) : originalFilename;
        value = value.replace('_', ' ').replace('-', ' ').trim();
        return StringUtils.hasText(value) ? value : "未命名导入文章";
    }

    public String normalizeImportedContent(String markdown) {
        if (!StringUtils.hasText(markdown)) {
            return "";
        }

        String normalized = stripBom(markdown).replace("\r\n", "\n").trim();
        Optional<String> heading = firstHeading(normalized);
        if (heading.isEmpty()) {
            return normalized;
        }

        String titleLine = "# " + heading.get();
        if (!normalized.startsWith(titleLine)) {
            return normalized;
        }

        String stripped = normalized.substring(titleLine.length()).stripLeading();
        return stripped.isBlank() ? normalized : stripped;
    }

    private Node parse(String markdown) {
        return parser.parse(markdown);
    }

    private Optional<String> firstHeading(String markdown) {
        if (!StringUtils.hasText(markdown)) {
            return Optional.empty();
        }
        Matcher matcher = FIRST_H1_PATTERN.matcher(stripBom(markdown).replace("\r\n", "\n"));
        if (matcher.find()) {
            return Optional.of(matcher.group(1).trim());
        }
        return Optional.empty();
    }

    private String stripBom(String markdown) {
        if (markdown != null && !markdown.isEmpty() && markdown.charAt(0) == '\uFEFF') {
            return markdown.substring(1);
        }
        return markdown;
    }
}
