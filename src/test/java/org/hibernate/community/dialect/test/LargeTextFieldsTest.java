package org.hibernate.community.dialect.test;

import static org.junit.jupiter.api.Assertions.*;
import java.util.UUID;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.junit.jupiter.api.*;
import io.qameta.allure.*;

/**
 * Tests for handling large text fields in FileMaker.
 * Focuses on VARCHAR fields up to 100KB.
 */
@Epic("FileMaker Hibernate Integration")
@Feature("Large Text Field Handling")
@Owner("FileMaker Team")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("FileMaker Large Text Fields Tests")
public class LargeTextFieldsTest {
    
    private Session session;
    private Transaction transaction;
    private static final String LARGE_TEXT;
    
    static {
        // Generate 80KB of text data
        StringBuilder sb = new StringBuilder(82000);
        for (int i = 0; i < 1000; i++) {
            sb.append(UUID.randomUUID().toString())
              .append(" - Sample text for large field testing. Line ")
              .append(i + 1)
              .append("\n");
        }
        LARGE_TEXT = sb.toString();
    }
    
    @BeforeEach
    void setUp() {
        session = HibernateUtil.getSessionFactory().openSession();
        transaction = session.beginTransaction();
    }

    @AfterEach
    void tearDown() {
        if (transaction != null && transaction.isActive()) {
            transaction.rollback();
        }
        if (session != null && session.isOpen()) {
            session.close();
        }
    }
    
    @Test
    @Order(1)
    @Story("Large Text Storage")
    @Description("Verify that FileMaker correctly handles large text fields up to 100KB")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Test Large Text Fields")
    void testLargeTextFields() {
        Contact contact = new Contact("test@example.com", "tuser", "test123");
        contact.setNotes(LARGE_TEXT);
        
        session.persist(contact);
        transaction.commit();
        session.clear();
        
        // Start new transaction for reading
        transaction = session.beginTransaction();
        Contact savedContact = session.get(Contact.class, contact.getId());
        assertNotNull(savedContact, "Contact should be saved");
        assertEquals(LARGE_TEXT, savedContact.getNotes(), "Large text should be saved and retrieved correctly");
        
        // Cleanup
        session.remove(savedContact);
        transaction.commit();
    }
}
