package com.example.blog.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;
import java.util.Set;

@Component
@Order(0)
@ConditionalOnProperty(name = "app.schema.compatibility.enabled", havingValue = "true")
public class SchemaCompatibilityInitializer implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(SchemaCompatibilityInitializer.class);
    private static final Set<String> LEGACY_CONTENT_TYPES = Set.of("TINYTEXT", "TEXT", "MEDIUMTEXT", "LONGVARCHAR");

    private final DataSource dataSource;

    public SchemaCompatibilityInitializer(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(String... args) {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            if (!isMySqlFamily(metaData.getDatabaseProductName())) {
                return;
            }

            ensurePostsContentLongText(connection, metaData);
            ensurePostsStatusSupportsScheduled(connection, metaData);
            ensurePostsCategorySupportsNewValues(connection, metaData);
            ensureUsersRoleColumn(connection, metaData);
            ensureUsersVerificationColumns(connection, metaData);
            ensurePlansStatusSupportsNamedValues(connection, metaData);
            ensureCommonIndexes(connection, metaData);
        } catch (SQLException e) {
            log.warn("Failed to apply schema compatibility upgrades", e);
        }
    }

    private void ensurePostsContentLongText(Connection connection, DatabaseMetaData metaData) throws SQLException {
        String currentType = findColumnType(metaData, connection.getCatalog(), "posts", "content");
        if (!requiresLongTextUpgrade(currentType)) {
            return;
        }

        try (Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE posts MODIFY COLUMN content LONGTEXT NULL");
        }
        log.info("Upgraded posts.content from {} to LONGTEXT", currentType);
    }

    private void ensurePostsStatusSupportsScheduled(Connection connection, DatabaseMetaData metaData) throws SQLException {
        String currentType = findColumnType(metaData, connection.getCatalog(), "posts", "status");
        if (currentType == null || currentType.isBlank()) {
            return;
        }

        String normalized = currentType.trim().toUpperCase(Locale.ROOT);
        if (normalized.startsWith("VARCHAR")) {
            return;
        }

        try (Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE posts MODIFY COLUMN status VARCHAR(32) NULL");
        }
        log.info("Upgraded posts.status from {} to VARCHAR(32) to support scheduled publishing", currentType);
    }

    private void ensurePostsCategorySupportsNewValues(Connection connection, DatabaseMetaData metaData) throws SQLException {
        String currentType = findColumnType(metaData, connection.getCatalog(), "posts", "category");
        if (currentType == null || currentType.isBlank()) {
            return;
        }

        String normalized = currentType.trim().toUpperCase(Locale.ROOT);
        if (normalized.startsWith("VARCHAR")) {
            return;
        }

        try (Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE posts MODIFY COLUMN category VARCHAR(32) NOT NULL DEFAULT 'PROJECT'");
        }
        log.info("Upgraded posts.category from {} to VARCHAR(32) to support new categories", currentType);
    }

    private void ensureUsersRoleColumn(Connection connection, DatabaseMetaData metaData) throws SQLException {
        if (findColumnType(metaData, connection.getCatalog(), "users", "role") != null) {
            return;
        }

        try (Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE users ADD COLUMN role VARCHAR(16) NOT NULL DEFAULT 'USER'");
        }
        log.info("Added users.role column with default USER");
    }

    private void ensureUsersVerificationColumns(Connection connection, DatabaseMetaData metaData) throws SQLException {
        String verificationStatusType = findColumnType(metaData, connection.getCatalog(), "users", "real_name_verification_status");
        if (verificationStatusType == null) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("""
                        ALTER TABLE users
                        ADD COLUMN real_name_verification_status VARCHAR(16) NOT NULL DEFAULT 'APPROVED'
                        """);
            }
            log.info("Added users.real_name_verification_status column with default APPROVED");
        } else if (!verificationStatusType.trim().toUpperCase(Locale.ROOT).startsWith("VARCHAR")) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("""
                        ALTER TABLE users
                        MODIFY COLUMN real_name_verification_status VARCHAR(16) NOT NULL DEFAULT 'APPROVED'
                        """);
            }
            log.info("Upgraded users.real_name_verification_status from {} to VARCHAR(16)", verificationStatusType);
        }

        if (findColumnType(metaData, connection.getCatalog(), "users", "real_name_verification_submitted_at") == null) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("""
                        ALTER TABLE users
                        ADD COLUMN real_name_verification_submitted_at DATETIME NULL
                        """);
            }
            log.info("Added users.real_name_verification_submitted_at column");
        }

        if (findColumnType(metaData, connection.getCatalog(), "users", "real_name_verification_reviewed_at") == null) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("""
                        ALTER TABLE users
                        ADD COLUMN real_name_verification_reviewed_at DATETIME NULL
                        """);
            }
            log.info("Added users.real_name_verification_reviewed_at column");
        }
    }

    private void ensurePlansStatusSupportsNamedValues(Connection connection, DatabaseMetaData metaData) throws SQLException {
        String statusType = findColumnType(metaData, connection.getCatalog(), "plans", "status");
        if (statusType == null) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("""
                        ALTER TABLE plans
                        ADD COLUMN status VARCHAR(16) NOT NULL DEFAULT 'IN_PROGRESS'
                        """);
            }
            log.info("Added plans.status column with default IN_PROGRESS");
            return;
        }

        String normalized = statusType.trim().toUpperCase(Locale.ROOT);
        if (!normalized.startsWith("VARCHAR")) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("""
                        ALTER TABLE plans
                        MODIFY COLUMN status VARCHAR(16) NOT NULL DEFAULT 'IN_PROGRESS'
                        """);
            }
            log.info("Upgraded plans.status from {} to VARCHAR(16)", statusType);
        }

        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                    UPDATE plans
                    SET status = CASE UPPER(TRIM(status))
                        WHEN '1' THEN 'COMPLETED'
                        WHEN 'COMPLETED' THEN 'COMPLETED'
                        WHEN '2' THEN 'SHELVED'
                        WHEN 'SHELVED' THEN 'SHELVED'
                        ELSE 'IN_PROGRESS'
                    END
                    """);
        }
    }

    private void ensureCommonIndexes(Connection connection, DatabaseMetaData metaData) throws SQLException {
        ensureNamedIndex(connection, metaData, "users", "idx_users_verification_queue",
                "CREATE INDEX idx_users_verification_queue ON users (real_name_verification_status, real_name_verification_submitted_at, created_at)");
        ensureNamedIndex(connection, metaData, "users", "idx_users_role_created",
                "CREATE INDEX idx_users_role_created ON users (role, created_at)");
        ensureNamedIndex(connection, metaData, "posts", "idx_posts_status_created",
                "CREATE INDEX idx_posts_status_created ON posts (status, created_at)");
        ensureNamedIndex(connection, metaData, "posts", "idx_posts_category_status_created",
                "CREATE INDEX idx_posts_category_status_created ON posts (category, status, created_at)");
        ensureNamedIndex(connection, metaData, "posts", "idx_posts_author_status_created",
                "CREATE INDEX idx_posts_author_status_created ON posts (author_id, status, created_at)");
        ensureNamedIndex(connection, metaData, "posts", "idx_posts_schedule_queue",
                "CREATE INDEX idx_posts_schedule_queue ON posts (status, scheduled_publish_at)");
        ensureNamedIndex(connection, metaData, "posts", "idx_posts_plan_order",
                "CREATE INDEX idx_posts_plan_order ON posts (plan_id, plan_order)");
        ensureNamedIndex(connection, metaData, "plans", "idx_plans_public_updated",
                "CREATE INDEX idx_plans_public_updated ON plans (is_public, updated_at)");
        ensureNamedIndex(connection, metaData, "plans", "idx_plans_access_public_updated",
                "CREATE INDEX idx_plans_access_public_updated ON plans (access_type, is_public, updated_at)");
        ensureNamedIndex(connection, metaData, "plans", "idx_plans_author_updated",
                "CREATE INDEX idx_plans_author_updated ON plans (author_id, updated_at)");
        ensureNamedIndex(connection, metaData, "comments", "idx_comments_post_created",
                "CREATE INDEX idx_comments_post_created ON comments (post_id, created_at)");
        ensureNamedIndex(connection, metaData, "comments", "idx_comments_parent_created",
                "CREATE INDEX idx_comments_parent_created ON comments (parent_comment_id, created_at)");
    }

    static boolean isMySqlFamily(String productName) {
        if (productName == null || productName.isBlank()) {
            return false;
        }
        String normalized = productName.trim().toUpperCase(Locale.ROOT);
        return normalized.contains("MYSQL") || normalized.contains("MARIADB");
    }

    static boolean requiresLongTextUpgrade(String typeName) {
        if (typeName == null || typeName.isBlank()) {
            return false;
        }
        String normalized = typeName.trim().toUpperCase(Locale.ROOT);
        return normalized.startsWith("VARCHAR") || LEGACY_CONTENT_TYPES.contains(normalized);
    }

    static boolean requiresPlanStatusUpgrade(String typeName) {
        if (typeName == null || typeName.isBlank()) {
            return false;
        }
        return !typeName.trim().toUpperCase(Locale.ROOT).startsWith("VARCHAR");
    }

    private String findColumnType(DatabaseMetaData metaData, String catalog, String tableName, String columnName) throws SQLException {
        String type = readColumnType(metaData, catalog, tableName, columnName);
        if (type != null) {
            return type;
        }
        return readColumnType(metaData, catalog, tableName.toUpperCase(Locale.ROOT), columnName.toUpperCase(Locale.ROOT));
    }

    private String readColumnType(DatabaseMetaData metaData, String catalog, String tableName, String columnName) throws SQLException {
        try (ResultSet columns = metaData.getColumns(catalog, null, tableName, columnName)) {
            if (columns.next()) {
                return columns.getString("TYPE_NAME");
            }
            return null;
        }
    }

    private void ensureNamedIndex(Connection connection,
                                  DatabaseMetaData metaData,
                                  String tableName,
                                  String indexName,
                                  String createSql) throws SQLException {
        if (hasIndex(metaData, connection.getCatalog(), tableName, indexName)) {
            return;
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute(createSql);
        }
        log.info("Created index {} on {}", indexName, tableName);
    }

    private boolean hasIndex(DatabaseMetaData metaData, String catalog, String tableName, String indexName) throws SQLException {
        if (matchesIndex(metaData, catalog, tableName, indexName)) {
            return true;
        }
        return matchesIndex(metaData, catalog, tableName.toUpperCase(Locale.ROOT), indexName.toUpperCase(Locale.ROOT));
    }

    private boolean matchesIndex(DatabaseMetaData metaData, String catalog, String tableName, String indexName) throws SQLException {
        try (ResultSet indexes = metaData.getIndexInfo(catalog, null, tableName, false, false)) {
            while (indexes.next()) {
                String currentIndexName = indexes.getString("INDEX_NAME");
                if (currentIndexName != null && currentIndexName.equalsIgnoreCase(indexName)) {
                    return true;
                }
            }
            return false;
        }
    }
}
