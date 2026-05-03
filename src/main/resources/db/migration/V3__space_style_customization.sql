CREATE TABLE IF NOT EXISTS user_space_styles (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    background_image VARCHAR(500),
    background_color VARCHAR(32) NOT NULL DEFAULT '#f6f7fb',
    theme_color VARCHAR(32) NOT NULL DEFAULT '#4f46e5',
    font_family VARCHAR(120) NOT NULL DEFAULT 'Inter, system-ui, sans-serif',
    profile TEXT,
    signature VARCHAR(255),
    tags_json TEXT,
    template_id VARCHAR(64),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_space_styles_user UNIQUE (user_id),
    CONSTRAINT fk_space_styles_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS user_space_style_versions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    version_name VARCHAR(100) NOT NULL,
    style_snapshot TEXT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_space_style_versions_user FOREIGN KEY (user_id) REFERENCES users(id)
);

SET @idx_space_style_versions_user_created := (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'user_space_style_versions'
      AND index_name = 'idx_space_style_versions_user_created'
);
SET @idx_space_style_versions_user_created_sql := IF(
    @idx_space_style_versions_user_created = 0,
    'CREATE INDEX idx_space_style_versions_user_created ON user_space_style_versions(user_id, created_at)',
    'SELECT 1'
);
PREPARE stmt FROM @idx_space_style_versions_user_created_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
