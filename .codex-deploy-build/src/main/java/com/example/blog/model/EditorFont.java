package com.example.blog.model;

import org.springframework.util.StringUtils;

import java.util.Arrays;

public enum EditorFont {
    HARMONYOS("harmonyos", "鸿蒙黑体", "\"HarmonyOS Sans SC\", \"HarmonyOS Sans\", \"Microsoft YaHei UI\", sans-serif"),
    PINGFANG("pingfang", "苹方", "\"PingFang SC\", \"Hiragino Sans GB\", \"Microsoft YaHei UI\", sans-serif"),
    SOURCE_HAN_SANS("source-han-sans", "思源黑体", "\"Source Han Sans SC\", \"Noto Sans CJK SC\", \"Microsoft YaHei UI\", sans-serif"),
    SOURCE_HAN_SERIF("source-han-serif", "思源宋体", "\"Source Han Serif SC\", \"Noto Serif CJK SC\", \"Songti SC\", serif"),
    LXGW_WENKAI("lxgw-wenkai", "霞鹜文楷", "\"LXGW WenKai\", \"KaiTi\", \"STKaiti\", serif"),
    MI_SANS("mi-sans", "MiSans", "\"MiSans\", \"Microsoft YaHei UI\", sans-serif"),
    ALIBABA_PUHUITI("alibaba-puhuiti", "阿里巴巴普惠体", "\"Alibaba PuHuiTi 3.0\", \"PingFang SC\", \"Microsoft YaHei UI\", sans-serif"),
    OPPO_SANS("oppo-sans", "OPPO Sans", "\"OPPO Sans\", \"Microsoft YaHei UI\", sans-serif"),
    GEORGIA("georgia", "Georgia 衬线", "\"Georgia\", \"Source Han Serif SC\", \"Songti SC\", serif"),
    JETBRAINS_MONO("jetbrains-mono", "JetBrains Mono", "\"JetBrains Mono\", \"Cascadia Code\", Consolas, monospace");

    public static final EditorFont DEFAULT = HARMONYOS;

    private final String key;
    private final String label;
    private final String cssStack;

    EditorFont(String key, String label, String cssStack) {
        this.key = key;
        this.label = label;
        this.cssStack = cssStack;
    }

    public String getKey() {
        return key;
    }

    public String getLabel() {
        return label;
    }

    public String getCssStack() {
        return cssStack;
    }

    public static EditorFont fromKey(String key) {
        if (!StringUtils.hasText(key)) {
            return DEFAULT;
        }
        return Arrays.stream(values())
                .filter(font -> font.key.equalsIgnoreCase(key.trim()))
                .findFirst()
                .orElse(DEFAULT);
    }
}
