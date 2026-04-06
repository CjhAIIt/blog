package com.example.blog.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
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
}
