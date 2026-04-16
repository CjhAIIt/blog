package com.example.blog.model;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;

public enum RankingPeriod {
    WEEK("week", "周榜", "按本周发布文章数量和点赞数实时统计"),
    MONTH("month", "月榜", "按本月发布文章数量和点赞数实时统计"),
    YEAR("year", "年榜", "按本年发布文章数量和点赞数实时统计");

    private final String slug;
    private final String label;
    private final String description;

    RankingPeriod(String slug, String label, String description) {
        this.slug = slug;
        this.label = label;
        this.description = description;
    }

    public String getSlug() {
        return slug;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }

    public LocalDateTime startAt() {
        LocalDate today = LocalDate.now();
        return switch (this) {
            case WEEK -> today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atStartOfDay();
            case MONTH -> today.withDayOfMonth(1).atStartOfDay();
            case YEAR -> today.withDayOfYear(1).atStartOfDay();
        };
    }

    public static RankingPeriod fromSlug(String slug) {
        if (slug != null) {
            for (RankingPeriod period : values()) {
                if (period.slug.equalsIgnoreCase(slug.trim())) {
                    return period;
                }
            }
        }
        return WEEK;
    }
}
