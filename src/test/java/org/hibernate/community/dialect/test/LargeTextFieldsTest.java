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
@Feature("Large Text Field Handling - Max 100KB VARCHAR fields with UTF-8 encoding")
@Owner("FileMaker Team")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("FileMaker Large Text Fields Tests")
@Severity(SeverityLevel.CRITICAL)
@Issue("HIBERNATE-001")
@TmsLink("TC-001")
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
    
    @Step("Initialize Hibernate session and transaction")
    @BeforeEach
    void setUp() {
        session = HibernateUtil.getSessionFactory().openSession();
        transaction = session.beginTransaction();
    }

    @Step("Cleanup session and transaction")
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
    @Description("Validates FileMaker's ability to store and retrieve large text fields:\n" +
                "1. Creates a contact with 80KB of text data\n" +
                "2. Persists the contact to FileMaker\n" +
                "3. Retrieves the contact and verifies data integrity\n" +
                "4. Cleans up test data\n\n" +
                "Expected: Text data should be stored and retrieved without truncation or corruption\n" +
                "Limitations:\n" +
                "- Maximum field size: 100KB\n" +
                "- Field type: VARCHAR\n" +
                "- Encoding: UTF-8")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Large Text Field Storage and Retrieval")
    @Issue("HIBERNATE-001")
    @TmsLink("TC-001")
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
