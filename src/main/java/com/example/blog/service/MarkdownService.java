package com.example.blog.service;

import org.commonmark.ext.autolink.AutolinkExtension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.ext.heading.anchor.HeadingAnchorExtension;
import org.commonmark.Extension;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.commonmark.renderer.text.TextContentRenderer;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class MarkdownService {
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
        return htmlRenderer.render(parse(markdown));
    }

    public String toPlainText(String markdown) {
        if (!StringUtils.hasText(markdown)) {
            return "";
        }
        return textRenderer.render(parse(markdown)).trim();
    }

    public String excerpt(String markdown, int maxLength) {
        String plainText = toPlainText(markdown);
        if (plainText.length() <= maxLength) {
            return plainText;
        }
        return plainText.substring(0, Math.max(0, maxLength - 1)) + "…";
    }

    private Node parse(String markdown) {
        return parser.parse(markdown);
    }
}
