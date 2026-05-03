package com.example.blog.dto;

public class DefaultCoverOption {
    private final String filename;
    private final String url;
    private final String label;
    private final String color;

    public DefaultCoverOption(String filename, String url, String label, String color) {
        this.filename = filename;
        this.url = url;
        this.label = label;
        this.color = color;
    }

    public String getFilename() {
        return filename;
    }

    public String getUrl() {
        return url;
    }

    public String getLabel() {
        return label;
    }

    public String getColor() {
        return color;
    }
}
