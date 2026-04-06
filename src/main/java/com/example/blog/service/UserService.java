package com.example.blog.service;

import com.example.blog.model.RealNameVerificationStatus;
import com.example.blog.model.User;
import com.example.blog.model.UserRole;
import com.example.blog.repository.UserRepository;
import jakarta.annotation.Resource;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class UserService implements UserDetailsService {
    private static final Pattern REAL_NAME_PATTERN = Pattern.compile("^[\\u4E00-\\u9FFF]{1,5}$");

    @Resource
    private UserRepository userRepository;

    @Resource
    private PasswordEncoder passwordEncoder;

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public List<User> findPendingRealNameVerifications() {
        return userRepository.findByRealNameVerificationStatusOrderByRealNameVerificationSubmittedAtAscCreatedAtAsc(
                RealNameVerificationStatus.PENDING
        );
    }

    public List<User> findReviewedRealNameVerifications() {
        return userRepository.findByRealNameVerificationStatusInOrderByRealNameVerificationReviewedAtDescCreatedAtDesc(
                List.of(RealNameVerificationStatus.APPROVED, RealNameVerificationStatus.REJECTED)
        );
    }

    public User save(User user) {
        return userRepository.save(user);
    }

    public User getById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("用户不存在，ID：" + id));
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

    public User registerUser(String username, String email, String password, String realName) {
        String normalizedUsername = normalizeUsername(username);
        String normalizedEmail = normalizeEmail(email);
        String normalizedRealName = normalizeRealName(realName);

        if (userRepository.existsByUsername(normalizedUsername)) {
            throw new IllegalArgumentException("用户名已存在");
        }
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new IllegalArgumentException("邮箱已被注册");
        }

        User user = new User();
        user.setUsername(normalizedUsername);
        user.setEmail(normalizedEmail);
        user.setPassword(passwordEncoder.encode(password));
        user.setEmailVerified(true);
        user.setRealName(normalizedRealName);
        markVerificationSubmitted(user);
        return userRepository.save(user);
    }

    public User createUser(String username, String email, String rawPassword) {
        return createUser(username, email, rawPassword, UserRole.USER, RealNameVerificationStatus.APPROVED);
    }

    public User createUser(String username, String email, String rawPassword, UserRole role) {
        return createUser(username, email, rawPassword, role, RealNameVerificationStatus.APPROVED);
    }

    public User createUser(String username,
                           String email,
                           String rawPassword,
                           UserRole role,
                           RealNameVerificationStatus verificationStatus) {
        User user = new User();
        user.setUsername(normalizeUsername(username));
        user.setEmail(normalizeEmail(email));
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setEmailVerified(true);
        user.setRole(role);
        user.setRealNameVerificationStatus(verificationStatus);
        if (verificationStatus == RealNameVerificationStatus.APPROVED) {
            markVerificationApproved(user);
        } else if (verificationStatus == RealNameVerificationStatus.PENDING) {
            markVerificationSubmitted(user);
        } else {
            user.setRealNameVerificationReviewedAt(LocalDateTime.now());
        }
        return userRepository.save(user);
    }

    public void updateProfile(User user, String realName, String bio, String qq, String githubUrl, String personalBlogUrl) {
        user.setRealName(normalizeRealName(realName));
        user.setBio(StringUtils.hasText(bio) ? bio.trim() : null);
        user.setQq(normalizeQq(qq));
        user.setGithubUrl(normalizeGithubUrl(githubUrl));
        user.setPersonalBlogUrl(normalizePersonalBlogUrl(personalBlogUrl));
        userRepository.save(user);
    }

    public User updateAccountProfile(User user,
                                     String username,
                                     String newPassword,
                                     String realName,
                                     String bio,
                                     String qq,
                                     String githubUrl,
                                     String personalBlogUrl,
                                     String avatarImageUrl) {
        String normalizedUsername = normalizeUsername(username);
        if (!normalizedUsername.equals(user.getUsername()) && userRepository.existsByUsername(normalizedUsername)) {
            throw new IllegalArgumentException("用户名已存在");
        }

        String normalizedRealName = normalizeRealName(realName);
        boolean realNameChanged = !Objects.equals(user.getRealName(), normalizedRealName);

        user.setUsername(normalizedUsername);
        if (StringUtils.hasText(newPassword)) {
            user.setPassword(passwordEncoder.encode(newPassword));
        }
        user.setRealName(normalizedRealName);
        user.setBio(StringUtils.hasText(bio) ? bio.trim() : null);
        user.setQq(normalizeQq(qq));
        user.setGithubUrl(normalizeGithubUrl(githubUrl));
        user.setPersonalBlogUrl(normalizePersonalBlogUrl(personalBlogUrl));
        user.setAvatarImageUrl(avatarImageUrl);
        refreshVerificationAfterProfileEdit(user, realNameChanged);
        return userRepository.save(user);
    }

    public User approveRealNameVerification(Long userId) {
        User user = getById(userId);
        markVerificationApproved(user);
        return userRepository.save(user);
    }

    public User rejectRealNameVerification(Long userId) {
        User user = getById(userId);
        user.setRealNameVerificationStatus(RealNameVerificationStatus.REJECTED);
        if (user.getRealNameVerificationSubmittedAt() == null) {
            user.setRealNameVerificationSubmittedAt(LocalDateTime.now());
        }
        user.setRealNameVerificationReviewedAt(LocalDateTime.now());
        return userRepository.save(user);
    }

    public boolean canWritePosts(User user) {
        return user != null && user.canPublishPosts();
    }

    public String getPostPermissionMessage(User user) {
        if (user == null) {
            return "请先登录后再写博客";
        }
        if (user.isAdmin()) {
            return "";
        }
        return switch (user.getRealNameVerificationStatus()) {
            case APPROVED -> "";
            case PENDING -> "实名认证资料已提交管理员审核，审核通过后才能写博客";
            case REJECTED -> "实名认证审核未通过，请到个人资料页修改实名信息后重新提交";
        };
    }

    public boolean matchesPassword(String rawPassword, String encodedPassword) {
        return StringUtils.hasText(rawPassword) && passwordEncoder.matches(rawPassword, encodedPassword);
    }

    public String normalizeUsername(String username) {
        return StringUtils.hasText(username) ? username.trim() : "";
    }

    public String normalizeEmail(String email) {
        return StringUtils.hasText(email) ? email.trim().toLowerCase(Locale.ROOT) : "";
    }

    public String normalizeRealName(String realName) {
        if (!StringUtils.hasText(realName)) {
            return null;
        }

        String normalized = realName.trim();
        if (!REAL_NAME_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("真实姓名需为 1 到 5 个中文字符");
        }
        return normalized;
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

    private void refreshVerificationAfterProfileEdit(User user, boolean realNameChanged) {
        if (user.isAdmin()) {
            markVerificationApproved(user);
            return;
        }

        RealNameVerificationStatus currentStatus = user.getRealNameVerificationStatus();
        if (realNameChanged
                || currentStatus == RealNameVerificationStatus.REJECTED
                || (currentStatus == RealNameVerificationStatus.PENDING
                && user.getRealNameVerificationSubmittedAt() == null)) {
            markVerificationSubmitted(user);
        }
    }

    private void markVerificationSubmitted(User user) {
        user.setRealNameVerificationStatus(RealNameVerificationStatus.PENDING);
        user.setRealNameVerificationSubmittedAt(LocalDateTime.now());
        user.setRealNameVerificationReviewedAt(null);
    }

    private void markVerificationApproved(User user) {
        LocalDateTime now = LocalDateTime.now();
        user.setRealNameVerificationStatus(RealNameVerificationStatus.APPROVED);
        if (user.getRealNameVerificationSubmittedAt() == null) {
            user.setRealNameVerificationSubmittedAt(now);
        }
        user.setRealNameVerificationReviewedAt(now);
    }
}
