package com.example.blog.service;

import com.example.blog.model.User;
import com.example.blog.repository.UserRepository;
import jakarta.annotation.Resource;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;

@Service
public class UserService implements UserDetailsService {
    @Resource
    private UserRepository userRepository;

    @Resource
    private PasswordEncoder passwordEncoder;

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public User save(User user) {
        return userRepository.save(user);
    }

    public User getByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("用户不存在：" + username));
    }

    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    public User registerUser(String username, String email, String password) {
        String normalizedUsername = normalizeUsername(username);
        if (userRepository.existsByUsername(normalizedUsername)) {
            throw new RuntimeException("用户名已存在");
        }
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("邮箱已被注册");
        }

        User user = new User();
        user.setUsername(normalizedUsername);
        user.setEmail(email.trim().toLowerCase());
        user.setPassword(passwordEncoder.encode(password));
        user.setEmailVerified(true);
        return userRepository.save(user);
    }

    public User createUser(String username, String email, String rawPassword) {
        User user = new User();
        user.setUsername(normalizeUsername(username));
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setEmailVerified(true);
        return userRepository.save(user);
    }

    public void updateProfile(User user, String bio, String qq, String githubUrl, String personalBlogUrl) {
        user.setBio(StringUtils.hasText(bio) ? bio.trim() : null);
        user.setQq(normalizeQq(qq));
        user.setGithubUrl(normalizeGithubUrl(githubUrl));
        user.setPersonalBlogUrl(normalizePersonalBlogUrl(personalBlogUrl));
        userRepository.save(user);
    }

    public User updateAccountProfile(User user,
                                     String username,
                                     String newPassword,
                                     String bio,
                                     String qq,
                                     String githubUrl,
                                     String personalBlogUrl,
                                     String avatarImageUrl) {
        String normalizedUsername = normalizeUsername(username);
        if (!normalizedUsername.equals(user.getUsername()) && userRepository.existsByUsername(normalizedUsername)) {
            throw new IllegalArgumentException("用户名已存在");
        }

        user.setUsername(normalizedUsername);
        if (StringUtils.hasText(newPassword)) {
            user.setPassword(passwordEncoder.encode(newPassword));
        }
        user.setBio(StringUtils.hasText(bio) ? bio.trim() : null);
        user.setQq(normalizeQq(qq));
        user.setGithubUrl(normalizeGithubUrl(githubUrl));
        user.setPersonalBlogUrl(normalizePersonalBlogUrl(personalBlogUrl));
        user.setAvatarImageUrl(avatarImageUrl);
        return userRepository.save(user);
    }

    public boolean matchesPassword(String rawPassword, String encodedPassword) {
        return StringUtils.hasText(rawPassword) && passwordEncoder.matches(rawPassword, encodedPassword);
    }

    public String normalizeUsername(String username) {
        return StringUtils.hasText(username) ? username.trim() : "";
    }

    public String normalizeGithubUrl(String githubUrl) {
        if (!StringUtils.hasText(githubUrl)) {
            return null;
        }
        String value = githubUrl.trim();
        if (!value.startsWith("http://") && !value.startsWith("https://")) {
            if (value.startsWith("github.com/")) {
                return "https://" + value;
            }
            return "https://github.com/" + value.replaceFirst("^/+", "");
        }
        return value;
    }

    public String normalizeQq(String qq) {
        if (!StringUtils.hasText(qq)) {
            return null;
        }
        return qq.trim();
    }

    public String normalizePersonalBlogUrl(String personalBlogUrl) {
        if (!StringUtils.hasText(personalBlogUrl)) {
            return null;
        }
        String value = personalBlogUrl.trim();
        if (!value.startsWith("http://") && !value.startsWith("https://")) {
            return "https://" + value.replaceFirst("^/+", "");
        }
        return value;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("用户不存在：" + username));
    }
}
