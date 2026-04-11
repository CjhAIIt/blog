SET @content_type := (
    SELECT UPPER(DATA_TYPE)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'posts'
      AND column_name = 'content'
    LIMIT 1
);
SET @content_sql := IF(
    @content_type IS NULL OR @content_type = '' OR @content_type = 'LONGTEXT',
    'SELECT 1',
    'ALTER TABLE posts MODIFY COLUMN content LONGTEXT NULL'
);
PREPARE stmt FROM @content_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @status_type := (
    SELECT UPPER(DATA_TYPE)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'posts'
      AND column_name = 'status'
    LIMIT 1
);
SET @status_sql := IF(
    @status_type IS NULL OR @status_type = '' OR @status_type = 'VARCHAR',
    'SELECT 1',
    'ALTER TABLE posts MODIFY COLUMN status VARCHAR(32) NOT NULL DEFAULT ''PUBLISHED'''
);
PREPARE stmt FROM @status_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @category_type := (
    SELECT UPPER(DATA_TYPE)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'posts'
      AND column_name = 'category'
    LIMIT 1
);
SET @category_sql := IF(
    @category_type IS NULL OR @category_type = '' OR @category_type = 'VARCHAR',
    'SELECT 1',
    'ALTER TABLE posts MODIFY COLUMN category VARCHAR(32) NOT NULL DEFAULT ''PROJECT'''
);
PREPARE stmt FROM @category_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @role_exists := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'users'
      AND column_name = 'role'
);
SET @role_sql := IF(
    @role_exists = 0,
    'ALTER TABLE users ADD COLUMN role VARCHAR(16) NOT NULL DEFAULT ''USER''',
    'SELECT 1'
);
PREPARE stmt FROM @role_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @verification_status_exists := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'users'
      AND column_name = 'real_name_verification_status'
);
SET @verification_status_sql := IF(
    @verification_status_exists = 0,
    'ALTER TABLE users ADD COLUMN real_name_verification_status VARCHAR(16) NOT NULL DEFAULT ''APPROVED''',
    'SELECT 1'
);
PREPARE stmt FROM @verification_status_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @verification_submitted_exists := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'users'
      AND column_name = 'real_name_verification_submitted_at'
);
SET @verification_submitted_sql := IF(
    @verification_submitted_exists = 0,
    'ALTER TABLE users ADD COLUMN real_name_verification_submitted_at DATETIME NULL',
    'SELECT 1'
);
PREPARE stmt FROM @verification_submitted_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @verification_reviewed_exists := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'users'
      AND column_name = 'real_name_verification_reviewed_at'
);
SET @verification_reviewed_sql := IF(
    @verification_reviewed_exists = 0,
    'ALTER TABLE users ADD COLUMN real_name_verification_reviewed_at DATETIME NULL',
    'SELECT 1'
);
PREPARE stmt FROM @verification_reviewed_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_posts_status_created := (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'posts'
      AND index_name = 'idx_posts_status_created'
);
SET @idx_posts_status_created_sql := IF(
    @idx_posts_status_created = 0,
    'CREATE INDEX idx_posts_status_created ON posts(status, created_at)',
    'SELECT 1'
);
PREPARE stmt FROM @idx_posts_status_created_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_posts_category_status_created := (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'posts'
      AND index_name = 'idx_posts_category_status_created'
);
SET @idx_posts_category_status_created_sql := IF(
    @idx_posts_category_status_created = 0,
    'CREATE INDEX idx_posts_category_status_created ON posts(category, status, created_at)',
    'SELECT 1'
);
PREPARE stmt FROM @idx_posts_category_status_created_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_posts_author_status_created := (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'posts'
      AND index_name = 'idx_posts_author_status_created'
);
SET @idx_posts_author_status_created_sql := IF(
    @idx_posts_author_status_created = 0,
    'CREATE INDEX idx_posts_author_status_created ON posts(author_id, status, created_at)',
    'SELECT 1'
);
PREPARE stmt FROM @idx_posts_author_status_created_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_posts_status_scheduled := (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'posts'
      AND index_name = 'idx_posts_status_scheduled'
);
SET @idx_posts_status_scheduled_sql := IF(
    @idx_posts_status_scheduled = 0,
    'CREATE INDEX idx_posts_status_scheduled ON posts(status, scheduled_publish_at)',
    'SELECT 1'
);
PREPARE stmt FROM @idx_posts_status_scheduled_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_posts_plan_order := (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'posts'
      AND index_name = 'idx_posts_plan_order'
);
SET @idx_posts_plan_order_sql := IF(
    @idx_posts_plan_order = 0,
    'CREATE INDEX idx_posts_plan_order ON posts(plan_id, plan_order)',
    'SELECT 1'
);
PREPARE stmt FROM @idx_posts_plan_order_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_comments_post_created := (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'comments'
      AND index_name = 'idx_comments_post_created'
);
SET @idx_comments_post_created_sql := IF(
    @idx_comments_post_created = 0,
    'CREATE INDEX idx_comments_post_created ON comments(post_id, created_at)',
    'SELECT 1'
);
PREPARE stmt FROM @idx_comments_post_created_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_post_likes_post := (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'post_likes'
      AND index_name = 'idx_post_likes_post'
);
SET @idx_post_likes_post_sql := IF(
    @idx_post_likes_post = 0,
    'CREATE INDEX idx_post_likes_post ON post_likes(post_id)',
    'SELECT 1'
);
PREPARE stmt FROM @idx_post_likes_post_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_post_likes_user := (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'post_likes'
      AND index_name = 'idx_post_likes_user'
);
SET @idx_post_likes_user_sql := IF(
    @idx_post_likes_user = 0,
    'CREATE INDEX idx_post_likes_user ON post_likes(user_id)',
    'SELECT 1'
);
PREPARE stmt FROM @idx_post_likes_user_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ft_posts_title_content := (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'posts'
      AND index_name = 'ft_posts_title_content'
);
SET @ft_posts_title_content_sql := IF(
    @ft_posts_title_content = 0,
    'CREATE FULLTEXT INDEX ft_posts_title_content ON posts(title, content) WITH PARSER ngram',
    'SELECT 1'
);
PREPARE stmt FROM @ft_posts_title_content_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
