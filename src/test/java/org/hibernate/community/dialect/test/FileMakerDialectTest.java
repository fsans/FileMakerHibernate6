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

@DisplayName("FileMaker Dialect Tests")
public class FileMakerDialectTest {
    
    private FileMakerDialect dialect;
    private JdbcTypeRegistry jdbcTypeRegistry;
    
    @BeforeEach
    void setUp() {
        dialect = new FileMakerDialect(DatabaseVersion.make(21, 0));
        jdbcTypeRegistry = new TypeConfiguration().getJdbcTypeRegistry();
    }
    
    @Test
    @DisplayName("Test numeric type mappings")
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
    @DisplayName("Test string type mappings")
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
    @DisplayName("Test binary type mappings")
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
    @DisplayName("Test date/time type mappings")
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
    @DisplayName("Test default type mapping")
    void testDefaultTypeMapping() {
        assertEquals(
            Types.VARCHAR,
            dialect.resolveSqlTypeDescriptor("UNKNOWN_TYPE", 9999, 0, 0, jdbcTypeRegistry)
                .getJdbcTypeCode()
        );
    }
    
    @Test
    @DisplayName("Test limit handler")
    void testLimitHandler() {
        assertNotNull(dialect.getLimitHandler());
        assertTrue(dialect.getLimitHandler() instanceof FileMakerLimitHandler);
    }
    
    @Test
    @DisplayName("Test identity column support")
    void testIdentityColumnSupport() {
        assertNotNull(dialect.getIdentityColumnSupport());
        assertTrue(dialect.getIdentityColumnSupport() instanceof FileMakerIdentityColumnSupport);
    }
}