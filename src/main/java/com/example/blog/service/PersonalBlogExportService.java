package com.example.blog.service;

import com.example.blog.config.SiteProperties;
import com.example.blog.model.Post;
import com.example.blog.model.User;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class PersonalBlogExportService {
    private static final DateTimeFormatter DISPLAY_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter FILE_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final MarkdownService markdownService;
    private final ObjectMapper objectMapper;
    private final SiteProperties siteProperties;

    public PersonalBlogExportService(MarkdownService markdownService, ObjectMapper objectMapper, SiteProperties siteProperties) {
        this.markdownService = markdownService;
        this.objectMapper = objectMapper;
        this.siteProperties = siteProperties;
    }

    public ExportArchive export(User user, List<Post> posts) {
        List<ExportPost> exportPosts = buildExportPosts(posts);
        String safeUsername = sanitizeSegment(user.getUsername(), "user");
        String fileName = safeUsername + "-blog-export-" + FILE_DATE.format(LocalDate.now()) + ".zip";

        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
             ZipOutputStream zipOutputStream = new ZipOutputStream(byteArrayOutputStream, StandardCharsets.UTF_8)) {
            addTextEntry(zipOutputStream, "README.md", buildReadme(user, exportPosts));
            addTextEntry(zipOutputStream, "index.html", buildIndexHtml(user, exportPosts));
            addTextEntry(zipOutputStream, "assets/styles.css", buildStyles());
            addTextEntry(zipOutputStream, "metadata/profile.json", buildProfileJson(user, exportPosts));
            addTextEntry(zipOutputStream, "metadata/posts.json", buildPostsJson(user, exportPosts));

            for (ExportPost exportPost : exportPosts) {
                addTextEntry(zipOutputStream, "posts/" + exportPost.slug() + ".html", buildPostHtml(user, exportPost));
                addTextEntry(zipOutputStream, "markdown/" + exportPost.slug() + ".md", buildMarkdownFile(user, exportPost));
            }

            zipOutputStream.finish();
            return new ExportArchive(fileName, byteArrayOutputStream.toByteArray());
        } catch (IOException e) {
            throw new IllegalStateException("生成个人博客导出包失败", e);
        }
    }

    private List<ExportPost> buildExportPosts(List<Post> posts) {
        List<ExportPost> exportPosts = new ArrayList<>();
        Set<String> usedSlugs = new HashSet<>();

        for (Post post : posts) {
            String slug = buildSlug(post, usedSlugs);
            exportPosts.add(new ExportPost(
                    post.getId(),
                    slug,
                    post.getTitle(),
                    post.getCategoryDisplayName(),
                    safeTime(post.getCreatedAt()),
                    safeTime(post.getUpdatedAt()),
                    markdownService.excerpt(post.getContent(), 160),
                    markdownService.render(post.getContent()),
                    post.getContent() == null ? "" : post.getContent()
            ));
        }

        return exportPosts;
    }

    private String buildReadme(User user, List<ExportPost> exportPosts) {
        StringBuilder builder = new StringBuilder();
        builder.append("# ").append(user.getUsername()).append(" 的个人博客导出包\n\n");
        builder.append("这个压缩包用于把实验室博客中的个人内容迁移到自己的 GitHub 仓库。\n\n");
        builder.append("## 包含内容\n\n");
        builder.append("- `index.html`：可直接作为首页发布的静态博客首页\n");
        builder.append("- `posts/`：每篇文章对应的静态 HTML 页面\n");
        builder.append("- `markdown/`：每篇文章的 Markdown 原文，带基础 front matter 元数据\n");
        builder.append("- `metadata/profile.json`：个人资料导出\n");
        builder.append("- `metadata/posts.json`：文章元数据导出\n");
        builder.append("- `assets/styles.css`：静态页面样式\n\n");
        builder.append("## 建议迁移步骤\n\n");
        builder.append("1. 在 GitHub 上创建个人仓库，例如 `")
                .append(sanitizeSegment(user.getUsername(), "yourname"))
                .append(".github.io` 或任意博客仓库\n");
        builder.append("2. 解压后把压缩包内全部文件上传到仓库根目录\n");
        builder.append("3. 打开仓库 `Settings -> Pages`\n");
        builder.append("4. 将发布源设置为 `Deploy from a branch`\n");
        builder.append("5. 选择 `main` 分支和 `/ (root)` 目录，等待 Pages 发布完成\n");
        builder.append("6. 如果后续换用 Hexo、Hugo、VuePress 等系统，可直接复用 `markdown/` 和 `metadata/`\n\n");
        builder.append("## 当前导出信息\n\n");
        builder.append("- 导出用户：").append(user.getUsername()).append('\n');
        builder.append("- 导出文章数：").append(exportPosts.size()).append('\n');
        builder.append("- GitHub 主页：").append(valueOrDefault(user.getGithubUrl(), "未设置")).append('\n');
        builder.append("- 个人博客地址：").append(valueOrDefault(user.getPersonalBlogUrl(), "未设置，可在实验室博客的个人资料页补充后再次导出")).append('\n');
        builder.append("- 实验室博客源码：").append(siteProperties.getSourceRepoUrl()).append('\n');
        builder.append("- 导出时间：").append(DISPLAY_TIME.format(LocalDateTime.now())).append("\n\n");
        builder.append("## 说明\n\n");
        builder.append("- 导出包不包含密码、邮箱、评论和其他用户数据\n");
        builder.append("- 文章内容已经转成静态 HTML，直接上传即可访问\n");
        builder.append("- Markdown 原文会一并保留，保证后续可继续迁移\n");
        return builder.toString();
    }

    private String buildIndexHtml(User user, List<ExportPost> exportPosts) {
        StringBuilder cards = new StringBuilder();
        for (ExportPost exportPost : exportPosts) {
            cards.append("""
                    <article class="card">
                        <div class="card-top">
                            <span class="tag">%s</span>
                            <span class="meta">%s</span>
                        </div>
                        <h2><a href="posts/%s.html">%s</a></h2>
                        <p>%s</p>
                        <div class="card-actions">
                            <a href="posts/%s.html">阅读全文</a>
                            <a href="markdown/%s.md">Markdown</a>
                        </div>
                    </article>
                    """.formatted(
                    escapeHtml(exportPost.categoryDisplayName()),
                    escapeHtml(exportPost.createdAtText()),
                    exportPost.slug(),
                    escapeHtml(exportPost.title()),
                    escapeHtml(exportPost.excerpt()),
                    exportPost.slug(),
                    exportPost.slug()
            ));
        }

        if (cards.isEmpty()) {
            cards.append("""
                    <section class="empty">
                        暂无文章内容，可以回到实验室博客继续写作后重新导出。
                    </section>
                    """);
        }

        return """
                <!DOCTYPE html>
                <html lang="zh-CN">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>%s 的个人博客</title>
                    <link rel="stylesheet" href="assets/styles.css">
                </head>
                <body>
                <div class="shell">
                    <header class="hero">
                        <p class="kicker">Exported from Lab Blog</p>
                        <h1>%s 的个人博客</h1>
                        <p class="lead">%s</p>
                        <div class="actions">
                            %s
                            %s
                            <a class="button secondary" href="%s" target="_blank" rel="noreferrer">项目源码</a>
                        </div>
                        <div class="stats">
                            <span>%d 篇文章</span>
                            <span>导出时间 %s</span>
                        </div>
                    </header>
                    <main class="grid">
                        %s
                    </main>
                </div>
                </body>
                </html>
                """.formatted(
                escapeHtml(user.getUsername()),
                escapeHtml(user.getUsername()),
                escapeHtmlWithBreaks(valueOrDefault(user.getBio(), "这里是从实验室博客导出的个人博客首页，已经可以直接部署到 GitHub Pages。")),
                buildActionLink(user.getGithubUrl(), "GitHub 主页"),
                buildActionLink(user.getPersonalBlogUrl(), "个人博客地址"),
                escapeHtml(siteProperties.getSourceRepoUrl()),
                exportPosts.size(),
                escapeHtml(DISPLAY_TIME.format(LocalDateTime.now())),
                cards
        );
    }

    private String buildPostHtml(User user, ExportPost exportPost) {
        return """
                <!DOCTYPE html>
                <html lang="zh-CN">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>%s - %s 的个人博客</title>
                    <link rel="stylesheet" href="../assets/styles.css">
                </head>
                <body>
                <div class="shell">
                    <article class="article">
                        <a class="back-link" href="../index.html">返回首页</a>
                        <div class="article-meta">
                            <span class="tag">%s</span>
                            <span>%s</span>
                            <span>更新于 %s</span>
                        </div>
                        <h1>%s</h1>
                        <p class="author-line">作者：%s</p>
                        <div class="article-actions">
                            <a href="../markdown/%s.md">下载 Markdown</a>
                            <a href="%s" target="_blank" rel="noreferrer">查看项目源码</a>
                        </div>
                        <div class="markdown-body">
                            %s
                        </div>
                    </article>
                </div>
                </body>
                </html>
                """.formatted(
                escapeHtml(exportPost.title()),
                escapeHtml(user.getUsername()),
                escapeHtml(exportPost.categoryDisplayName()),
                escapeHtml(exportPost.createdAtText()),
                escapeHtml(exportPost.updatedAtText()),
                escapeHtml(exportPost.title()),
                escapeHtml(user.getUsername()),
                exportPost.slug(),
                escapeHtml(siteProperties.getSourceRepoUrl()),
                exportPost.renderedContent()
        );
    }

    private String buildMarkdownFile(User user, ExportPost exportPost) {
        return """
                ---
                title: "%s"
                author: "%s"
                category: "%s"
                createdAt: "%s"
                updatedAt: "%s"
                sourcePostId: %d
                ---

                %s
                """.formatted(
                escapeYaml(exportPost.title()),
                escapeYaml(user.getUsername()),
                escapeYaml(exportPost.categoryDisplayName()),
                exportPost.createdAtText(),
                exportPost.updatedAtText(),
                exportPost.id(),
                exportPost.markdown()
        );
    }

    private String buildProfileJson(User user, List<ExportPost> exportPosts) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("username", user.getUsername());
        payload.put("bio", user.getBio());
        payload.put("githubUrl", user.getGithubUrl());
        payload.put("personalBlogUrl", user.getPersonalBlogUrl());
        payload.put("postCount", exportPosts.size());
        payload.put("sourceRepoUrl", siteProperties.getSourceRepoUrl());
        payload.put("exportedAt", LocalDateTime.now().toString());
        return toPrettyJson(payload);
    }

    private String buildPostsJson(User user, List<ExportPost> exportPosts) {
        List<Map<String, Object>> payload = new ArrayList<>();
        for (ExportPost exportPost : exportPosts) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", exportPost.id());
            item.put("title", exportPost.title());
            item.put("author", user.getUsername());
            item.put("category", exportPost.categoryDisplayName());
            item.put("createdAt", exportPost.createdAtText());
            item.put("updatedAt", exportPost.updatedAtText());
            item.put("excerpt", exportPost.excerpt());
            item.put("htmlPath", "posts/" + exportPost.slug() + ".html");
            item.put("markdownPath", "markdown/" + exportPost.slug() + ".md");
            payload.add(item);
        }
        return toPrettyJson(payload);
    }

    private String buildStyles() {
        return """
                :root {
                    --bg: #f3f6fb;
                    --surface: #ffffff;
                    --text: #111827;
                    --muted: #667085;
                    --line: rgba(15, 23, 42, 0.08);
                    --brand: #0f172a;
                    --brand-alt: #2563eb;
                    --shadow: 0 18px 40px rgba(15, 23, 42, 0.08);
                }

                * {
                    box-sizing: border-box;
                }

                body {
                    margin: 0;
                    background:
                        radial-gradient(circle at top right, rgba(37, 99, 235, 0.08), transparent 18%),
                        linear-gradient(180deg, #ffffff 0%, var(--bg) 100%);
                    color: var(--text);
                    font-family: "Microsoft YaHei UI", "PingFang SC", "Segoe UI", sans-serif;
                }

                a {
                    color: inherit;
                }

                .shell {
                    width: min(1120px, calc(100% - 24px));
                    margin: 0 auto;
                    padding: 32px 0 48px;
                }

                .hero,
                .card,
                .article,
                .empty {
                    border-radius: 24px;
                    background: var(--surface);
                    box-shadow: var(--shadow);
                }

                .hero {
                    padding: 32px;
                    background:
                        radial-gradient(circle at top right, rgba(37, 99, 235, 0.16), transparent 24%),
                        linear-gradient(135deg, rgba(15, 23, 42, 0.98), rgba(37, 99, 235, 0.92));
                    color: #ffffff;
                }

                .kicker {
                    margin: 0;
                    color: rgba(255, 255, 255, 0.72);
                    font-size: 0.88rem;
                    letter-spacing: 0.08em;
                    text-transform: uppercase;
                }

                h1,
                h2 {
                    margin: 0;
                    font-family: "Bahnschrift", "Trebuchet MS", sans-serif;
                }

                .hero h1 {
                    margin-top: 12px;
                    font-size: clamp(2rem, 4vw, 3rem);
                }

                .lead {
                    margin: 14px 0 0;
                    max-width: 760px;
                    color: rgba(255, 255, 255, 0.88);
                    line-height: 1.85;
                }

                .actions,
                .stats,
                .card-actions,
                .article-actions,
                .article-meta {
                    display: flex;
                    flex-wrap: wrap;
                    gap: 10px;
                    align-items: center;
                }

                .actions,
                .stats {
                    margin-top: 18px;
                }

                .button,
                .card-actions a,
                .article-actions a,
                .back-link {
                    display: inline-flex;
                    align-items: center;
                    justify-content: center;
                    min-height: 40px;
                    padding: 0 16px;
                    border-radius: 999px;
                    text-decoration: none;
                }

                .button {
                    background: #ffffff;
                    color: var(--brand);
                    font-weight: 700;
                }

                .button.secondary,
                .card-actions a,
                .article-actions a,
                .back-link {
                    border: 1px solid var(--line);
                    background: #f8fafc;
                    color: inherit;
                }

                .grid {
                    display: grid;
                    grid-template-columns: repeat(3, minmax(0, 1fr));
                    gap: 20px;
                    margin-top: 22px;
                }

                .card,
                .empty {
                    padding: 22px;
                }

                .card-top {
                    display: flex;
                    justify-content: space-between;
                    gap: 10px;
                    margin-bottom: 16px;
                }

                .tag {
                    display: inline-flex;
                    align-items: center;
                    min-height: 30px;
                    padding: 0 12px;
                    border-radius: 999px;
                    background: #eff6ff;
                    color: #1d4ed8;
                    font-size: 0.84rem;
                    font-weight: 700;
                }

                .meta,
                .author-line,
                .stats,
                .card p {
                    color: var(--muted);
                }

                .card h2 {
                    font-size: 1.15rem;
                    line-height: 1.6;
                }

                .card h2 a {
                    text-decoration: none;
                }

                .card p {
                    line-height: 1.85;
                }

                .card-actions a,
                .article-actions a,
                .back-link {
                    color: var(--brand-alt);
                }

                .article {
                    padding: 28px;
                }

                .article h1 {
                    margin-top: 18px;
                    font-size: clamp(2rem, 4vw, 2.8rem);
                }

                .article-actions {
                    margin-top: 18px;
                }

                .markdown-body {
                    margin-top: 28px;
                    line-height: 1.9;
                    color: #334155;
                    font-family: "Georgia", "Source Han Serif SC", serif;
                }

                .markdown-body > :first-child {
                    margin-top: 0;
                }

                .markdown-body h1,
                .markdown-body h2,
                .markdown-body h3,
                .markdown-body h4 {
                    margin: 1.4em 0 0.7em;
                    color: #0f172a;
                    font-family: "Bahnschrift", "Trebuchet MS", sans-serif;
                    line-height: 1.35;
                }

                .markdown-body a {
                    color: var(--brand-alt);
                }

                .markdown-body pre {
                    overflow: auto;
                    padding: 18px 20px;
                    border-radius: 18px;
                    background: #0f172a;
                    color: #e2e8f0;
                }

                .markdown-body code {
                    padding: 0.15em 0.4em;
                    border-radius: 8px;
                    background: #eef2ff;
                    font-size: 0.92em;
                }

                .markdown-body pre code {
                    padding: 0;
                    background: transparent;
                    color: inherit;
                }

                .markdown-body blockquote {
                    margin: 1em 0;
                    padding: 16px 18px;
                    border-left: 4px solid var(--brand-alt);
                    border-radius: 0 14px 14px 0;
                    background: #eff6ff;
                }

                .markdown-body table {
                    width: 100%;
                    border-collapse: collapse;
                    margin: 1.2em 0;
                    overflow: hidden;
                    border-radius: 14px;
                    box-shadow: 0 12px 30px rgba(15, 23, 42, 0.08);
                }

                .markdown-body th,
                .markdown-body td {
                    padding: 12px 14px;
                    border: 1px solid rgba(148, 163, 184, 0.18);
                    text-align: left;
                }

                .markdown-body img {
                    max-width: 100%;
                    border-radius: 18px;
                }

                @media (max-width: 900px) {
                    .grid {
                        grid-template-columns: 1fr;
                    }
                }
                """;
    }

    private void addTextEntry(ZipOutputStream zipOutputStream, String path, String content) throws IOException {
        ZipEntry zipEntry = new ZipEntry(path);
        zipOutputStream.putNextEntry(zipEntry);
        zipOutputStream.write(content.getBytes(StandardCharsets.UTF_8));
        zipOutputStream.closeEntry();
    }

    private String buildSlug(Post post, Set<String> usedSlugs) {
        String normalized = post.getTitle() == null ? "" : post.getTitle().toLowerCase();
        String asciiPart = normalized.replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        String slug = "post-" + post.getId() + (asciiPart.isEmpty() ? "" : "-" + asciiPart);
        while (usedSlugs.contains(slug)) {
            slug = slug + "-copy";
        }
        usedSlugs.add(slug);
        return slug;
    }

    private String sanitizeSegment(String value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String sanitized = value.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        return sanitized.isEmpty() ? fallback : sanitized;
    }

    private String buildActionLink(String url, String label) {
        if (url == null || url.isBlank()) {
            return "";
        }
        return "<a class=\"button secondary\" href=\"" + escapeHtml(url) + "\" target=\"_blank\" rel=\"noreferrer\">" + escapeHtml(label) + "</a>";
    }

    private String safeTime(LocalDateTime time) {
        return time == null ? "" : DISPLAY_TIME.format(time);
    }

    private String valueOrDefault(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value;
    }

    private String escapeHtml(String value) {
        return HtmlUtils.htmlEscape(value == null ? "" : value);
    }

    private String escapeHtmlWithBreaks(String value) {
        return escapeHtml(value).replace("\n", "<br>");
    }

    private String escapeYaml(String value) {
        return value == null ? "" : value.replace("\"", "\\\"");
    }

    private String toPrettyJson(Object value) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("生成导出元数据失败", e);
        }
    }

    public record ExportArchive(String fileName, byte[] content) {
    }

    private record ExportPost(
            Long id,
            String slug,
            String title,
            String categoryDisplayName,
            String createdAtText,
            String updatedAtText,
            String excerpt,
            String renderedContent,
            String markdown
    ) {
    }
}
