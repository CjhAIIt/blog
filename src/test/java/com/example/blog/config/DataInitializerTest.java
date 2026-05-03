package com.example.blog.config;

import com.example.blog.model.Post;
import com.example.blog.model.RealNameVerificationStatus;
import com.example.blog.model.User;
import com.example.blog.model.UserRole;
import com.example.blog.service.CommentService;
import com.example.blog.service.PostService;
import com.example.blog.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataInitializerTest {
    @Mock
    private UserService userService;

    @Mock
    private PostService postService;

    @Mock
    private CommentService commentService;

    @Test
    void reusesDefaultAdminEmailWhenUsernameIsMissing() {
        User defaultEmailUser = new User("legacy-admin", "password", "admin@example.com");
        defaultEmailUser.setBio("Existing account");
        defaultEmailUser.setRole(UserRole.USER);

        User existingUser = new User("member", "password", "member@example.com");
        Post existingPost = new Post();

        when(userService.findAll()).thenReturn(List.of(defaultEmailUser, existingUser));
        when(postService.findAllIncludingDrafts()).thenReturn(List.of(existingPost));
        when(userService.findByUsername("admin")).thenReturn(Optional.empty());
        when(userService.findByEmail("admin@example.com")).thenReturn(Optional.of(defaultEmailUser));
        when(userService.save(defaultEmailUser)).thenReturn(defaultEmailUser);

        new DataInitializer(userService, postService, commentService).run();

        assertTrue(defaultEmailUser.isAdmin());
        assertEquals(RealNameVerificationStatus.APPROVED, defaultEmailUser.getRealNameVerificationStatus());
        verify(userService, never()).createUser(
                eq("admin"),
                eq("admin@example.com"),
                anyString(),
                eq(UserRole.ADMIN),
                eq(RealNameVerificationStatus.APPROVED)
        );
    }
}
