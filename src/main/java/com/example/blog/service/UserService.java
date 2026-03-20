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
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("用户名已存在");
        }
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("邮箱已被注册");
        }

        User user = new User();
        user.setUsername(username.trim());
        user.setEmail(email.trim().toLowerCase());
        user.setPassword(passwordEncoder.encode(password));
        user.setEmailVerified(true);
        return userRepository.save(user);
    }

    public User createUser(String username, String email, String rawPassword) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setEmailVerified(true);
        return userRepository.save(user);
    }

    public void updateProfile(User user, String bio, String qq, String githubUrl) {
        user.setBio(StringUtils.hasText(bio) ? bio.trim() : null);
        user.setQq(normalizeQq(qq));
        user.setGithubUrl(normalizeGithubUrl(githubUrl));
        userRepository.save(user);
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

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("用户不存在：" + username));
    }
}
