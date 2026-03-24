package com.example.blog.dto;

public class ProfileForm {
    private String username;
    private String currentPassword;
    private String newPassword;
    private String confirmPassword;
    private String bio;
    private String qq;
    private String githubUrl;
    private String personalBlogUrl;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getCurrentPassword() {
        return currentPassword;
    }

    public void setCurrentPassword(String currentPassword) {
        this.currentPassword = currentPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getQq() {
        return qq;
    }

    public void setQq(String qq) {
        this.qq = qq;
    }

    public String getGithubUrl() {
        return githubUrl;
    }

    public void setGithubUrl(String githubUrl) {
        this.githubUrl = githubUrl;
    }

    public String getPersonalBlogUrl() {
        return personalBlogUrl;
    }

    public void setPersonalBlogUrl(String personalBlogUrl) {
        this.personalBlogUrl = personalBlogUrl;
    }
}
