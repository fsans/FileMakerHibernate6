package org.hibernate.community.dialect.test;

import static org.junit.jupiter.api.Assertions.*;

import org.hibernate.community.dialect.FileMakerDialect;
import org.hibernate.community.dialect.identity.FileMakerIdentityColumnSupport;
import org.hibernate.dialect.DatabaseVersion;
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
import io.qameta.allure.Issue;
import io.qameta.allure.TmsLink;

@Epic("FileMaker Hibernate Integration")
@Feature("Identity Column Support - Validates FileMaker's identity column support capabilities")
@Owner("Hibernate Team")
public class IdentitySupportTest {

    private FileMakerDialect dialect;

    @BeforeEach
    void setUp() {
        dialect = new FileMakerDialect(DatabaseVersion.make(19, 6));
    }

    @Test
    @Story("Identity Column Support")
    @Description("Validates FileMaker's identity column support:\n" +
                "1. Proper IdentityColumnSupport implementation\n" +
                "2. No auto-generated keys support\n" +
                "3. Manual ID generation requirements\n\n" +
                "Expected: Should use FileMakerIdentityColumnSupport with manual ID generation")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Identity Column Configuration")
    @Issue("HIBERNATE-002")
    @TmsLink("TC-002")
    void testIdentityColumnSupport() {
        assertNotNull(dialect.getIdentityColumnSupport());
        assertTrue(dialect.getIdentityColumnSupport() instanceof FileMakerIdentityColumnSupport);
    }
    
    @Test
    @Story("Identity Column Support")
    @Description("Validates that FileMaker does not support identity columns")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("No Identity Column Support")
    void testNoIdentityColumnSupport() {
        assertFalse(dialect.getIdentityColumnSupport().supportsIdentityColumns());
    }
    
    @Test
    @Story("Identity Column Support")
    @Description("Validates that FileMaker requires manual ID generation")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Manual ID Generation")
    void testManualIdGeneration() {
        assertFalse(dialect.getIdentityColumnSupport().supportsInsertSelectIdentity());
    }
}