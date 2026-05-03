package com.example.blog.service;

import com.example.blog.dto.SpaceStyleDto;
import com.example.blog.dto.SpaceStyleVersionDto;
import com.example.blog.model.SpaceStyle;
import com.example.blog.model.SpaceStyleVersion;
import com.example.blog.model.User;
import com.example.blog.repository.SpaceStyleRepository;
import com.example.blog.repository.SpaceStyleVersionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class SpaceStyleService {
    private static final Pattern COLOR_PATTERN = Pattern.compile("^#[0-9a-fA-F]{6}$");
    private static final int MAX_PROFILE_LENGTH = 600;
    private static final int MAX_SIGNATURE_LENGTH = 120;
    private static final int MAX_TAG_COUNT = 10;
    private static final int MAX_TAG_LENGTH = 20;
    private static final DateTimeFormatter VERSION_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final Map<String, String> FONT_OPTIONS = Map.of(
            "system", "Inter, system-ui, sans-serif",
            "serif", "Georgia, 'Times New Roman', serif",
            "mono", "'SFMono-Regular', Consolas, 'Liberation Mono', monospace",
            "rounded", "'Trebuchet MS', 'Arial Rounded MT Bold', system-ui, sans-serif"
    );
    private static final Set<String> TEMPLATE_IDS = Set.of("clean", "ink", "garden", "sunset", "ocean");

    private final SpaceStyleRepository styleRepository;
    private final SpaceStyleVersionRepository versionRepository;
    private final ObjectMapper objectMapper;

    public SpaceStyleService(SpaceStyleRepository styleRepository,
                             SpaceStyleVersionRepository versionRepository,
                             ObjectMapper objectMapper) {
        this.styleRepository = styleRepository;
        this.versionRepository = versionRepository;
        this.objectMapper = objectMapper;
    }

    public SpaceStyleDto getPublicStyle(User user) {
        return styleRepository.findByUserId(user.getId())
                .map(this::toDto)
                .orElseGet(() -> defaultStyle(user));
    }

    @Transactional
    public SpaceStyleDto save(User user, SpaceStyleDto request) {
        SpaceStyle style = styleRepository.findByUserId(user.getId()).orElseGet(() -> createStyle(user));
        if (style.getId() != null) {
            saveVersion(user, "保存前版本 " + LocalDateTime.now().format(VERSION_TIME_FORMATTER), toDto(style));
        }
        apply(style, sanitize(request, user));
        style.setUpdatedAt(LocalDateTime.now());
        return toDto(styleRepository.save(style));
    }

    @Transactional
    public SpaceStyleDto reset(User user) {
        SpaceStyle style = styleRepository.findByUserId(user.getId()).orElseGet(() -> createStyle(user));
        if (style.getId() != null) {
            saveVersion(user, "恢复默认前 " + LocalDateTime.now().format(VERSION_TIME_FORMATTER), toDto(style));
        }
        apply(style, defaultStyle(user));
        style.setUpdatedAt(LocalDateTime.now());
        return toDto(styleRepository.save(style));
    }

    public List<SpaceStyleVersionDto> listVersions(User user) {
        return versionRepository.findTop12ByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(version -> new SpaceStyleVersionDto(
                        version.getId(),
                        version.getVersionName(),
                        version.getCreatedAt(),
                        readSnapshot(version.getStyleSnapshot(), user)
                ))
                .toList();
    }

    @Transactional
    public SpaceStyleDto restore(User user, Long versionId) {
        SpaceStyleVersion version = versionRepository.findByIdAndUserId(versionId, user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Style version not found"));
        SpaceStyle style = styleRepository.findByUserId(user.getId()).orElseGet(() -> createStyle(user));
        if (style.getId() != null) {
            saveVersion(user, "恢复历史前 " + LocalDateTime.now().format(VERSION_TIME_FORMATTER), toDto(style));
        }
        apply(style, sanitize(readSnapshot(version.getStyleSnapshot(), user), user));
        style.setUpdatedAt(LocalDateTime.now());
        return toDto(styleRepository.save(style));
    }

    public SpaceStyleDto defaultStyle(User user) {
        SpaceStyleDto dto = new SpaceStyleDto();
        dto.setBackgroundColor("#f8fafc");
        dto.setThemeColor("#2563eb");
        dto.setFontFamily(FONT_OPTIONS.get("system"));
        dto.setProfile(user == null ? "" : nullToEmpty(user.getBio()));
        dto.setSignature("");
        dto.setTags(new ArrayList<>());
        dto.setTemplateId("clean");
        return dto;
    }

    private SpaceStyle createStyle(User user) {
        SpaceStyle style = new SpaceStyle();
        style.setUser(user);
        style.setCreatedAt(LocalDateTime.now());
        return style;
    }

    private void apply(SpaceStyle style, SpaceStyleDto dto) {
        style.setBackgroundImage(dto.getBackgroundImage());
        style.setBackgroundColor(dto.getBackgroundColor());
        style.setThemeColor(dto.getThemeColor());
        style.setFontFamily(dto.getFontFamily());
        style.setProfile(dto.getProfile());
        style.setSignature(dto.getSignature());
        style.setTagsJson(writeTags(dto.getTags()));
        style.setTemplateId(dto.getTemplateId());
    }

    private SpaceStyleDto sanitize(SpaceStyleDto request, User user) {
        SpaceStyleDto fallback = defaultStyle(user);
        SpaceStyleDto dto = new SpaceStyleDto();
        dto.setBackgroundImage(sanitizeImageUrl(request == null ? null : request.getBackgroundImage()));
        dto.setBackgroundColor(sanitizeColor(request == null ? null : request.getBackgroundColor(), fallback.getBackgroundColor()));
        dto.setThemeColor(sanitizeColor(request == null ? null : request.getThemeColor(), fallback.getThemeColor()));
        dto.setFontFamily(sanitizeFont(request == null ? null : request.getFontFamily()));
        dto.setProfile(limitText(request == null ? null : request.getProfile(), MAX_PROFILE_LENGTH));
        dto.setSignature(limitText(request == null ? null : request.getSignature(), MAX_SIGNATURE_LENGTH));
        dto.setTags(sanitizeTags(request == null ? null : request.getTags()));
        dto.setTemplateId(sanitizeTemplate(request == null ? null : request.getTemplateId()));
        return dto;
    }

    private SpaceStyleDto toDto(SpaceStyle style) {
        SpaceStyleDto dto = new SpaceStyleDto();
        dto.setBackgroundImage(style.getBackgroundImage());
        dto.setBackgroundColor(StringUtils.hasText(style.getBackgroundColor()) ? style.getBackgroundColor() : "#f8fafc");
        dto.setThemeColor(StringUtils.hasText(style.getThemeColor()) ? style.getThemeColor() : "#2563eb");
        dto.setFontFamily(StringUtils.hasText(style.getFontFamily()) ? style.getFontFamily() : FONT_OPTIONS.get("system"));
        dto.setProfile(nullToEmpty(style.getProfile()));
        dto.setSignature(nullToEmpty(style.getSignature()));
        dto.setTags(readTags(style.getTagsJson()));
        dto.setTemplateId(StringUtils.hasText(style.getTemplateId()) ? style.getTemplateId() : "clean");
        return dto;
    }

    private void saveVersion(User user, String versionName, SpaceStyleDto snapshot) {
        SpaceStyleVersion version = new SpaceStyleVersion();
        version.setUser(user);
        version.setVersionName(versionName);
        version.setStyleSnapshot(writeSnapshot(snapshot));
        version.setCreatedAt(LocalDateTime.now());
        versionRepository.save(version);
    }

    private String writeSnapshot(SpaceStyleDto snapshot) {
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unable to serialize style snapshot", e);
        }
    }

    private SpaceStyleDto readSnapshot(String snapshot, User user) {
        if (!StringUtils.hasText(snapshot)) {
            return defaultStyle(user);
        }
        try {
            return sanitize(objectMapper.readValue(snapshot, SpaceStyleDto.class), user);
        } catch (JsonProcessingException e) {
            return defaultStyle(user);
        }
    }

    private String writeTags(List<String> tags) {
        try {
            return objectMapper.writeValueAsString(tags == null ? List.of() : tags);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    private List<String> readTags(String tagsJson) {
        if (!StringUtils.hasText(tagsJson)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(tagsJson, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }

    private String sanitizeColor(String color, String fallback) {
        if (!StringUtils.hasText(color)) {
            return fallback;
        }
        String value = color.trim();
        return COLOR_PATTERN.matcher(value).matches() ? value.toLowerCase(Locale.ROOT) : fallback;
    }

    private String sanitizeFont(String fontFamily) {
        if (!StringUtils.hasText(fontFamily)) {
            return FONT_OPTIONS.get("system");
        }
        String value = fontFamily.trim();
        if (FONT_OPTIONS.containsKey(value)) {
            return FONT_OPTIONS.get(value);
        }
        return FONT_OPTIONS.containsValue(value) ? value : FONT_OPTIONS.get("system");
    }

    private String sanitizeImageUrl(String imageUrl) {
        if (!StringUtils.hasText(imageUrl)) {
            return null;
        }
        String value = imageUrl.trim();
        if (value.startsWith("/uploads/space-backgrounds/")
                || value.startsWith("/images/default-covers/")
                || value.startsWith("/images/default-cover.svg")) {
            return value;
        }
        return null;
    }

    private String sanitizeTemplate(String templateId) {
        if (!StringUtils.hasText(templateId)) {
            return "clean";
        }
        String value = templateId.trim().toLowerCase(Locale.ROOT);
        return TEMPLATE_IDS.contains(value) ? value : "clean";
    }

    private String limitText(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String trimmed = value.trim();
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }

    private List<String> sanitizeTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String tag : tags) {
            if (!StringUtils.hasText(tag)) {
                continue;
            }
            String value = tag.trim();
            if (value.length() > MAX_TAG_LENGTH) {
                value = value.substring(0, MAX_TAG_LENGTH);
            }
            normalized.add(value);
            if (normalized.size() >= MAX_TAG_COUNT) {
                break;
            }
        }
        return new ArrayList<>(normalized);
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
