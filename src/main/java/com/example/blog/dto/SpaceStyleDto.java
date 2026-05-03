package com.example.blog.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.ArrayList;
import java.util.List;

public class SpaceStyleDto {
    private String backgroundImage;
    private String backgroundColor;
    private String themeColor;
    private String fontFamily;
    private String profile;
    private String signature;
    private List<String> tags = new ArrayList<>();
    private String templateId;

    public String getBackgroundImage() {
        return backgroundImage;
    }

    public void setBackgroundImage(String backgroundImage) {
        this.backgroundImage = backgroundImage;
    }

    public String getBackgroundColor() {
        return backgroundColor;
    }

    public void setBackgroundColor(String backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public String getThemeColor() {
        return themeColor;
    }

    public void setThemeColor(String themeColor) {
        this.themeColor = themeColor;
    }

    public String getFontFamily() {
        return fontFamily;
    }

    public void setFontFamily(String fontFamily) {
        this.fontFamily = fontFamily;
    }

    public String getProfile() {
        return profile;
    }

    public void setProfile(String profile) {
        this.profile = profile;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags == null ? new ArrayList<>() : tags;
    }

    public String getTemplateId() {
        return templateId;
    }

    public void setTemplateId(String templateId) {
        this.templateId = templateId;
    }

    @JsonIgnore
    public String getPageStyle() {
        StringBuilder style = new StringBuilder();
        style.append("--space-theme: ").append(cssValue(themeColor, "#2563eb")).append(';');
        style.append("--space-bg: ").append(cssValue(backgroundColor, "#f8fafc")).append(';');
        style.append("--space-font: ").append(fontFamily == null || fontFamily.isBlank()
                ? "Inter, system-ui, sans-serif"
                : fontFamily).append(';');
        style.append("background-color: var(--space-bg);");
        style.append("font-family: var(--space-font);");
        if (backgroundImage != null && !backgroundImage.isBlank()) {
            style.append("background-image: linear-gradient(120deg, rgba(255,255,255,.82), rgba(255,255,255,.58)), url('")
                    .append(backgroundImage.replace("'", "%27"))
                    .append("');");
            style.append("background-size: cover;");
            style.append("background-position: center;");
            style.append("background-attachment: fixed;");
        }
        return style.toString();
    }

    @JsonIgnore
    public boolean hasCustomProfile() {
        return profile != null && !profile.isBlank();
    }

    @JsonIgnore
    public boolean hasSignature() {
        return signature != null && !signature.isBlank();
    }

    @JsonIgnore
    public boolean hasTags() {
        return tags != null && !tags.isEmpty();
    }

    private String cssValue(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
