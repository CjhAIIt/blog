package com.example.blog.config;

import com.example.blog.config.SchemaCompatibilityInitializer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SchemaCompatibilityInitializerTest {
    @Test
    void detectsMySqlFamilyDatabases() {
        assertTrue(SchemaCompatibilityInitializer.isMySqlFamily("MySQL"));
        assertTrue(SchemaCompatibilityInitializer.isMySqlFamily("MariaDB"));
        assertFalse(SchemaCompatibilityInitializer.isMySqlFamily("H2"));
    }

    @Test
    void recognizesLegacyContentColumnTypes() {
        assertTrue(SchemaCompatibilityInitializer.requiresLongTextUpgrade("TEXT"));
        assertTrue(SchemaCompatibilityInitializer.requiresLongTextUpgrade("MEDIUMTEXT"));
        assertTrue(SchemaCompatibilityInitializer.requiresLongTextUpgrade("VARCHAR"));
        assertFalse(SchemaCompatibilityInitializer.requiresLongTextUpgrade("LONGTEXT"));
    }

    @Test
    void recognizesLegacyPlanStatusColumnTypes() {
        assertTrue(SchemaCompatibilityInitializer.requiresPlanStatusUpgrade("INT"));
        assertTrue(SchemaCompatibilityInitializer.requiresPlanStatusUpgrade("TINYINT"));
        assertFalse(SchemaCompatibilityInitializer.requiresPlanStatusUpgrade("VARCHAR"));
    }
}
