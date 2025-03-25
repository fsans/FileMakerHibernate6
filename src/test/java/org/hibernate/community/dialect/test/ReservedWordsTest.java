package org.hibernate.community.dialect.test;

import org.hibernate.community.dialect.FileMakerDialect;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for SQL keyword recognition in the FileMaker dialect.
 */
@Epic("FileMaker Hibernate Dialect")
@Feature("SQL Keyword Recognition")
@Owner("hibernate-team")
@Severity(SeverityLevel.CRITICAL)
public class ReservedWordsTest {

    private FileMakerDialect dialect;

    @BeforeEach
    void setUp() {
        dialect = new FileMakerDialect();
    }

    @DisplayName("Verify individual SQL keyword recognition")
    @Description("Validates that specific SQL keywords are recognized by the FileMaker dialect")
    @Severity(SeverityLevel.BLOCKER)
    @ParameterizedTest(name = "Test keyword: {0}")
    @ValueSource(strings = {
            // FileMaker-specific keywords
            "rowid", "modid", "getas", "putas", "recid",
            
            // Common SQL keywords
            "select", "insert", "update", "delete", "from", "where",
            
            // Data type keywords
            "varchar", "integer", "timestamp", "blob",
            
            // Function keywords
            "count", "max", "min", "avg", "sum",
            
            // FileMaker date/time functions
            "curdate", "curtime", "today", "now",
            
            // FileMaker type conversion
            "strval", "numval", "dateval", "timestampval"
    })
    void shouldRecognizeKeyword(String keyword) {
        assertThat(dialect.getKeywords().contains(keyword))
                .as("Dialect should recognize '%s' as a keyword", keyword)
                .isTrue();
    }

    @Test
    @DisplayName("Verify standard ANSI SQL keywords are recognized")
    @Description("Validates that basic ANSI SQL keywords are properly recognized")
    @Severity(SeverityLevel.BLOCKER)
    void shouldContainAllBaseKeywords() {
        // Verify that all ANSI SQL keywords are included
        assertThat(dialect.getKeywords())
                .as("Dialect should include all ANSI SQL keywords")
                .contains("select", "from", "where", "insert", "update", "delete");
    }

    @Test
    @DisplayName("Verify FileMaker-specific keywords are recognized")
    @Description("Validates that FileMaker-specific keywords like ROWID and MODID are recognized")
    @Severity(SeverityLevel.CRITICAL)
    void shouldContainFileMakerSpecificKeywords() {
        // Verify FileMaker-specific keywords
        assertThat(dialect.getKeywords())
                .as("Dialect should include FileMaker-specific keywords")
                .contains("rowid", "modid", "getas", "putas", "recid");
    }

    @Test
    @DisplayName("Verify SQL data type keywords are recognized")
    @Description("Validates that SQL data type keywords are properly recognized")
    @Severity(SeverityLevel.CRITICAL)
    void shouldContainDataTypeKeywords() {
        // Verify data type keywords
        assertThat(dialect.getKeywords())
                .as("Dialect should include data type keywords")
                .contains("varchar", "integer", "timestamp", "blob", "numeric");
    }

    @Test
    @DisplayName("Verify SQL function keywords are recognized")
    @Description("Validates that SQL function keywords and FileMaker-specific functions are recognized")
    @Severity(SeverityLevel.CRITICAL)
    void shouldContainFunctionKeywords() {
        // Verify function keywords
        assertThat(dialect.getKeywords())
                .as("Dialect should include function keywords")
                .contains("count", "max", "min", "avg", "sum", 
                        "curdate", "curtime", "today",
                        "strval", "numval", "dateval");
    }
}