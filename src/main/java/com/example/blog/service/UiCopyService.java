package com.example.blog.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class UiCopyService {
    private static final String COPY_RESOURCE = "static/data/copy.json";

    private final Map<String, Object> copy;

    public UiCopyService(ObjectMapper objectMapper) {
        this.copy = loadCopy(objectMapper);
    }

    public Map<String, Object> getCopy() {
        return copy;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> section(String name) {
        Object value = copy.get(name);
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Map.of();
    }

    private Map<String, Object> loadCopy(ObjectMapper objectMapper) {
        try (InputStream inputStream = new ClassPathResource(COPY_RESOURCE).getInputStream()) {
            Map<String, Object> loaded = objectMapper.readValue(inputStream, new TypeReference<>() {});
            return Map.copyOf(normalize(loaded));
        } catch (IOException e) {
            return defaultCopy();
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> normalize(Map<String, Object> source) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof Map<?, ?> nested) {
                result.put(entry.getKey(), normalize((Map<String, Object>) nested));
            } else {
                result.put(entry.getKey(), value);
            }
        }
        return result;
    }

    private Map<String, Object> defaultCopy() {
        Map<String, Object> mobile = new LinkedHashMap<>();
        mobile.put("latestArticles", "晨风拂柳，心语轻扬");
        mobile.put("popular", "锦绣文章，芳华独赏");
        mobile.put("empty", "风起云涌，暂无内容");
        mobile.put("hero", "落笔成文，字字珠玑");
        mobile.put("readButton", "细读芳华");

        Map<String, Object> web = new LinkedHashMap<>();
        web.put("topArticle", "云上高阁，文华先行");
        web.put("featured", "锦上添花，独领风骚");
        web.put("readButton", "细读芳华");
        web.put("empty", "文章未至，静候佳作");
        web.put("hero", "落笔成文，字字珠玑");
        web.put("latestArticles", "晨风拂柳，心语轻扬");

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("mobile", mobile);
        root.put("web", web);
        return Map.copyOf(root);
    }
}
