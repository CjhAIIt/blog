CREATE TABLE IF NOT EXISTS users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    username VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    email_verified TINYINT(1) NOT NULL DEFAULT 1,
    real_name VARCHAR(16) NULL,
    bio TEXT NULL,
    qq_encrypted VARCHAR(512) NULL,
    github_url_encrypted VARCHAR(512) NULL,
    personal_blog_url VARCHAR(512) NULL,
    avatar_image_url VARCHAR(512) NULL,
    created_at DATETIME NULL,
    password_updated_at DATETIME NULL,
    role VARCHAR(16) NOT NULL DEFAULT 'USER',
    real_name_verification_status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    real_name_verification_submitted_at DATETIME NULL,
    real_name_verification_reviewed_at DATETIME NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_username (username),
    UNIQUE KEY uk_users_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS plans (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    description TEXT NULL,
    cover_image_url VARCHAR(512) NULL,
    is_public TINYINT(1) NOT NULL DEFAULT 1,
    access_type VARCHAR(32) NOT NULL DEFAULT 'PRIVATE',
    expected_count INT NOT NULL DEFAULT 0,
    status INT NOT NULL DEFAULT 0,
    author_id BIGINT NOT NULL,
    created_at DATETIME NULL,
    updated_at DATETIME NULL,
    PRIMARY KEY (id),
    KEY idx_plans_author_id (author_id),
    CONSTRAINT fk_plans_author FOREIGN KEY (author_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS posts (
    id BIGINT NOT NULL AUTO_INCREMENT,
    title VARCHAR(255) NOT NULL,
    category VARCHAR(32) NOT NULL DEFAULT 'PROJECT',
    content LONGTEXT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PUBLISHED',
    cover_image_url VARCHAR(512) NULL,
    font_key VARCHAR(64) NULL,
    author_id BIGINT NOT NULL,
    created_at DATETIME NULL,
    updated_at DATETIME NULL,
    like_count INT NULL DEFAULT 0,
    scheduled_publish_at DATETIME NULL,
    plan_id BIGINT NULL,
    plan_order INT NULL,
    PRIMARY KEY (id),
    KEY idx_posts_author_id (author_id),
    KEY idx_posts_plan_id (plan_id),
    CONSTRAINT fk_posts_author FOREIGN KEY (author_id) REFERENCES users (id),
    CONSTRAINT fk_posts_plan FOREIGN KEY (plan_id) REFERENCES plans (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS comments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    content TEXT NOT NULL,
    post_id BIGINT NOT NULL,
    author_id BIGINT NOT NULL,
    parent_comment_id BIGINT NULL,
    created_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    KEY idx_comments_post_id (post_id),
    KEY idx_comments_author_id (author_id),
    KEY idx_comments_parent_id (parent_comment_id),
    CONSTRAINT fk_comments_post FOREIGN KEY (post_id) REFERENCES posts (id),
    CONSTRAINT fk_comments_author FOREIGN KEY (author_id) REFERENCES users (id),
    CONSTRAINT fk_comments_parent FOREIGN KEY (parent_comment_id) REFERENCES comments (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS post_likes (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    post_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_post_like_user_post (user_id, post_id),
    KEY idx_post_likes_post_id (post_id),
    KEY idx_post_likes_user_id (user_id),
    CONSTRAINT fk_post_likes_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_post_likes_post FOREIGN KEY (post_id) REFERENCES posts (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS notifications (
    id BIGINT NOT NULL AUTO_INCREMENT,
    type VARCHAR(32) NOT NULL,
    recipient_id BIGINT NOT NULL,
    actor_id BIGINT NOT NULL,
    post_id BIGINT NOT NULL,
    comment_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL,
    read_at DATETIME NULL,
    PRIMARY KEY (id),
    KEY idx_notification_recipient_created (recipient_id, created_at),
    KEY idx_notification_recipient_read (recipient_id, read_at),
    CONSTRAINT fk_notifications_recipient FOREIGN KEY (recipient_id) REFERENCES users (id),
    CONSTRAINT fk_notifications_actor FOREIGN KEY (actor_id) REFERENCES users (id),
    CONSTRAINT fk_notifications_post FOREIGN KEY (post_id) REFERENCES posts (id),
    CONSTRAINT fk_notifications_comment FOREIGN KEY (comment_id) REFERENCES comments (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
