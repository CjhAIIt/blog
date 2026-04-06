package com.example.blog.service;

import com.example.blog.model.RealNameVerificationStatus;
import com.example.blog.model.User;
import com.example.blog.model.UserRole;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        assertThrows(IllegalArgumentException.class, () -> userService.normalizeRealName("张三李四王五"));
    }

    @Test
    void approvedUserCanWritePosts() {
        User user = new User();
        user.setRealNameVerificationStatus(RealNameVerificationStatus.APPROVED);

        assertTrue(userService.canWritePosts(user));
    }

    @Test
    void pendingUserCannotWritePosts() {
        User user = new User();
        user.setRealNameVerificationStatus(RealNameVerificationStatus.PENDING);

        assertFalse(userService.canWritePosts(user));
        assertEquals("实名认证资料已提交管理员审核，审核通过后才能写博客", userService.getPostPermissionMessage(user));
    }

    @Test
    void adminCanWritePostsEvenIfVerificationNotApproved() {
        User user = new User();
        user.setRole(UserRole.ADMIN);
        user.setRealNameVerificationStatus(RealNameVerificationStatus.REJECTED);

        assertTrue(userService.canWritePosts(user));
    }
}
