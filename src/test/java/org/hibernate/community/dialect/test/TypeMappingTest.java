package org.hibernate.community.dialect.test;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.Types;
import org.hibernate.community.dialect.FileMakerDialect;
import org.hibernate.community.dialect.identity.FileMakerIdentityColumnSupport;
import org.hibernate.community.dialect.pagination.FileMakerLimitHandler;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;
import org.hibernate.type.spi.TypeConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Story;
import io.qameta.allure.Description;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;

@Epic("FileMaker Hibernate Integration")
@Feature("Data Type Mapping")
@Owner("FileMaker Team")
@DisplayName("FileMaker Type Mapping Tests")
public class TypeMappingTest {
    
    private FileMakerDialect dialect;
    private JdbcTypeRegistry jdbcTypeRegistry;
    
    @BeforeEach
    void setUp() {
        dialect = new FileMakerDialect(DatabaseVersion.make(21, 0));
        jdbcTypeRegistry = new TypeConfiguration().getJdbcTypeRegistry();
    }
    
    @Test
    @Story("Numeric Type Mapping")
    @Description("Verify that numeric types are correctly mapped between FileMaker and Java")
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("Test Numeric Type Mappings")
    void testNumericTypeMappings() {
        assertEquals(
            Types.NUMERIC,
            dialect.resolveSqlTypeDescriptor("NUMBER", Types.NUMERIC, 10, 2, jdbcTypeRegistry)
                .getJdbcTypeCode()
        );
        assertEquals(
            Types.NUMERIC,
            dialect.resolveSqlTypeDescriptor("DECIMAL", Types.DECIMAL, 10, 2, jdbcTypeRegistry)
                .getJdbcTypeCode()
        );
        assertEquals(
            Types.NUMERIC,
            dialect.resolveSqlTypeDescriptor("INTEGER", Types.INTEGER, 10, 0, jdbcTypeRegistry)
                .getJdbcTypeCode()
        );
    }
    
    @Test
    @Story("String Type Mapping")
    @Description("Verify that string types are correctly mapped between FileMaker and Java")
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("Test String Type Mappings")
    void testStringTypeMappings() {
        assertEquals(
            Types.VARCHAR,
            dialect.resolveSqlTypeDescriptor("VARCHAR", Types.VARCHAR, 255, 0, jdbcTypeRegistry)
                .getJdbcTypeCode()
        );
        assertEquals(
            Types.VARCHAR,
            dialect.resolveSqlTypeDescriptor("LONGVARCHAR", Types.LONGVARCHAR, 4000, 0, jdbcTypeRegistry)
                .getJdbcTypeCode()
        );
    }
    
    @Test
    @Story("Date/Time Type Mapping")
    @Description("Verify that date and time types are correctly mapped between FileMaker and Java")
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("Test Date/Time Type Mappings")
    void testDateTimeTypeMappings() {
        assertEquals(
            Types.DATE,
            dialect.resolveSqlTypeDescriptor("DATE", Types.DATE, 0, 0, jdbcTypeRegistry)
                .getJdbcTypeCode()
        );
        assertEquals(
            Types.TIME,
            dialect.resolveSqlTypeDescriptor("TIME", Types.TIME, 0, 0, jdbcTypeRegistry)
                .getJdbcTypeCode()
        );
        assertEquals(
            Types.TIMESTAMP,
            dialect.resolveSqlTypeDescriptor("TIMESTAMP", Types.TIMESTAMP, 0, 0, jdbcTypeRegistry)
                .getJdbcTypeCode()
        );
    }
    
    @Test
    @Story("Binary Type Mapping")
    @Description("Verify that binary types are correctly mapped between FileMaker and Java")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Test Binary Type Mappings")
    void testBinaryTypeMappings() {
        assertEquals(
            Types.BINARY,
            dialect.resolveSqlTypeDescriptor("BLOB", Types.BLOB, 0, 0, jdbcTypeRegistry)
                .getJdbcTypeCode()
        );
        assertEquals(
            Types.BINARY,
            dialect.resolveSqlTypeDescriptor("VARBINARY", Types.VARBINARY, 255, 0, jdbcTypeRegistry)
                .getJdbcTypeCode()
        );
    }
    
    @Test
    @Story("Default Type Mapping")
    @Description("Verify that default type mapping is correctly handled")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Test Default Type Mapping")
    void testDefaultTypeMapping() {
        assertEquals(
            Types.VARCHAR,
            dialect.resolveSqlTypeDescriptor("UNKNOWN_TYPE", 9999, 0, 0, jdbcTypeRegistry)
                .getJdbcTypeCode()
        );
    }
    
    @Test
    @Story("Limit Handler")
    @Description("Verify that limit handler is correctly configured")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Test Limit Handler")
    void testLimitHandler() {
        assertNotNull(dialect.getLimitHandler());
        assertTrue(dialect.getLimitHandler() instanceof FileMakerLimitHandler);
    }
    
    @Test
    @Story("Identity Column Support")
    @Description("Verify that identity column support is correctly configured")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Test Identity Column Support")
    void testIdentityColumnSupport() {
        assertNotNull(dialect.getIdentityColumnSupport());
        assertTrue(dialect.getIdentityColumnSupport() instanceof FileMakerIdentityColumnSupport);
    }
}