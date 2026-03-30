package com.example.blog.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UserServiceTest {
    private final UserService userService = new UserService();

    @Test
    void normalizeRealNameTrimsChineseName() {
        assertEquals("张三", userService.normalizeRealName(" 张三 "));
    }

    @Test
    void normalizeRealNameReturnsNullForBlankValue() {
        assertNull(userService.normalizeRealName(" "));
    }

    @Test
    void normalizeRealNameRejectsNonChineseValue() {
        assertThrows(IllegalArgumentException.class, () -> userService.normalizeRealName("Tom"));
    }

    @Test
    void normalizeRealNameRejectsTooLongValue() {
        assertThrows(IllegalArgumentException.class, () -> userService.normalizeRealName("张三李四王六"));
    }
}
