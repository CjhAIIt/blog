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

            String currentType = findColumnType(metaData, connection.getCatalog(), "posts", "content");
            if (!requiresLongTextUpgrade(currentType)) {
                return;
            }

            try (Statement statement = connection.createStatement()) {
                statement.execute("ALTER TABLE posts MODIFY COLUMN content LONGTEXT NULL");
            }
            log.info("Upgraded posts.content from {} to LONGTEXT", currentType);
        } catch (SQLException e) {
            log.warn("Failed to ensure posts.content uses LONGTEXT; large Markdown imports may still fail", e);
        }
    }

    static boolean isMySqlFamily(String databaseProductName) {
        if (databaseProductName == null) {
            return false;
        }
        String normalized = databaseProductName.trim().toLowerCase(Locale.ROOT);
        return normalized.contains("mysql") || normalized.contains("mariadb");
    }

    static boolean requiresLongTextUpgrade(String currentType) {
        if (currentType == null || currentType.isBlank()) {
            return false;
        }
        String normalized = currentType.trim().toUpperCase(Locale.ROOT);
        if (normalized.contains("LONGTEXT")) {
            return false;
        }
        return LEGACY_CONTENT_TYPES.contains(normalized) || normalized.startsWith("VARCHAR");
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
