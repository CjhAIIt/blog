package com.example.blog.service;

import com.example.blog.config.SiteProperties;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.Normalizer;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class FileStorageService {
    private static final Charset GB18030 = Charset.forName("GB18030");
    private static final Set<String> IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "gif");
    private static final Set<String> MARKDOWN_EXTENSIONS = Set.of("md", "markdown", "txt");

    private final Path uploadRoot;

    public FileStorageService(SiteProperties siteProperties) {
        this.uploadRoot = siteProperties.getUploadRoot();
    }

    public String storeAvatar(MultipartFile file) {
        return storeImage(file, "avatars");
    }

    public String storeCover(MultipartFile file) {
        return storeImage(file, "covers");
    }

    public String loadMarkdownContent(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请选择要导入的 Markdown 文件");
        }

        String extension = extensionOf(file.getOriginalFilename());
        if (!MARKDOWN_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("仅支持导入 .md、.markdown 或 .txt 文件");
        }

        try {
            return decodeMarkdownBytes(file.getBytes());
        } catch (IOException e) {
            throw new IllegalArgumentException("读取 Markdown 文件失败，请稍后重试", e);
        }
    }

    public void deleteIfStored(String fileUrl) {
        if (!StringUtils.hasText(fileUrl) || !fileUrl.startsWith("/uploads/")) {
            return;
        }

        String relativePath = fileUrl.substring("/uploads/".length()).replace('/', java.io.File.separatorChar);
        Path target = uploadRoot.resolve(relativePath).normalize();
        if (!target.startsWith(uploadRoot)) {
            return;
        }

        try {
            Files.deleteIfExists(target);
        } catch (IOException ignored) {
        }
    }

    private String storeImage(MultipartFile file, String folder) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        String contentType = file.getContentType();
        String extension = extensionOf(file.getOriginalFilename());
        if (!IMAGE_EXTENSIONS.contains(extension) || contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("仅支持上传 JPG、PNG、WEBP 或 GIF 图片");
        }

        try {
            Path directory = uploadRoot.resolve(folder);
            Files.createDirectories(directory);

            String safeName = slugifyFilename(file.getOriginalFilename());
            String fileName = UUID.randomUUID() + "-" + safeName;
            Path target = directory.resolve(fileName).normalize();
            if (!target.startsWith(directory)) {
                throw new IllegalArgumentException("文件路径不合法");
            }

            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return "/uploads/" + folder + "/" + fileName;
        } catch (IOException e) {
            throw new IllegalArgumentException("文件上传失败，请稍后重试", e);
        }
    }

    private String extensionOf(String filename) {
        if (!StringUtils.hasText(filename) || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).trim().toLowerCase(Locale.ROOT);
    }

    private String slugifyFilename(String originalFilename) {
        String normalized = Normalizer.normalize(
                StringUtils.hasText(originalFilename) ? originalFilename : "upload.file",
                Normalizer.Form.NFKD
        ).replaceAll("\\p{M}", "");
        String cleaned = normalized.replaceAll("[^A-Za-z0-9._-]", "-").replaceAll("-{2,}", "-");
        if (!cleaned.contains(".")) {
            cleaned = cleaned + ".bin";
        }
        return cleaned.toLowerCase(Locale.ROOT);
    }

    private String decodeMarkdownBytes(byte[] bytes) {
        if (bytes.length >= 3
                && bytes[0] == (byte) 0xEF
                && bytes[1] == (byte) 0xBB
                && bytes[2] == (byte) 0xBF) {
            return stripBom(new String(bytes, StandardCharsets.UTF_8));
        }
        if (bytes.length >= 2 && bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xFE) {
            return stripBom(new String(bytes, StandardCharsets.UTF_16LE));
        }
        if (bytes.length >= 2 && bytes[0] == (byte) 0xFE && bytes[1] == (byte) 0xFF) {
            return stripBom(new String(bytes, StandardCharsets.UTF_16BE));
        }

        String utf8 = tryDecode(bytes, StandardCharsets.UTF_8);
        if (utf8 != null) {
            return stripBom(utf8);
        }

        String gb18030 = tryDecode(bytes, GB18030);
        if (gb18030 != null) {
            return stripBom(gb18030);
        }

        return stripBom(new String(bytes, StandardCharsets.UTF_8));
    }

    private String tryDecode(byte[] bytes, Charset charset) {
        CharsetDecoder decoder = charset.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);
        try {
            return decoder.decode(ByteBuffer.wrap(bytes)).toString();
        } catch (CharacterCodingException e) {
            return null;
        }
    }

    private String stripBom(String value) {
        if (value != null && !value.isEmpty() && value.charAt(0) == '\uFEFF') {
            return value.substring(1);
        }
        return value;
    }
}
