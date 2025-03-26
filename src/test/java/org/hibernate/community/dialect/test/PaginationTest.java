package org.hibernate.community.dialect.test;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.Query;
import org.junit.jupiter.api.*;
import io.qameta.allure.*;

/**
 * Test suite for validating FileMaker's SQL pagination capabilities through Hibernate.
 * FileMaker implements standard ANSI SQL pagination using OFFSET and FETCH FIRST clauses.
 * 
 * Key Features Tested:
 * 1. Basic pagination using FETCH FIRST n ROWS
 * 2. Ordering with ORDER BY clause
 * 3. Transaction management for data setup and cleanup
 * 
 * FileMaker SQL Pagination Rules:
 * - FETCH FIRST n ROWS ONLY for limiting results
 * - ORDER BY required for deterministic results
 * - Cannot use pagination in subqueries
 * 
 * Test Data:
 * - Uses Contact entity with basic fields
 * - Creates 10 test records with predictable data
 * - Each test manages its own transaction
 */
@Epic("FileMaker Hibernate Integration")
@Feature("SQL Pagination - Implementation of standard SQL pagination features:\n" +
         "1. FETCH FIRST n ROWS\n" +
         "2. Deterministic ordering\n" +
         "3. Transaction safety")
@Owner("FileMaker Team")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("FileMaker Pagination Tests")
@Severity(SeverityLevel.CRITICAL)
@Issue("HIBERNATE-006")
@TmsLink("TC-006")
public class PaginationTest {
    
    private static SessionFactory sessionFactory;
    private Session session;
    private Transaction transaction;
    private static final int TOTAL_CONTACTS = 10; // Reduced test data size
    
    @BeforeAll
    static void setupClass() {
        sessionFactory = HibernateUtil.getSessionFactory();
    }
    
    @AfterAll
    static void tearDownClass() {
        if (sessionFactory != null) {
            sessionFactory.close();
        }
    }
    
    @BeforeEach
    void setUp() {
        session = sessionFactory.openSession();
        transaction = session.beginTransaction();
        createTestData();
        transaction.commit(); // Commit the test data
    }
    
    @AfterEach
    void tearDown() {
        try {
            transaction = session.beginTransaction(); // Start new transaction for cleanup
            cleanupTestData();
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }
            throw e; // Re-throw to fail the test
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }
    
    /**
     * Tests basic pagination using FETCH FIRST with ordered results.
     * 
     * Verifies:
     * 1. FETCH FIRST n ROWS syntax is correctly handled
     * 2. Results are properly ordered by ID
     * 3. Correct number of records is returned
     * 4. Result content matches expected values
     * 
     * Expected Behavior:
     * - Returns exactly 5 records
     * - Records are ordered by ID
     * - Email addresses match the pattern user[1-5]@test.com
     */
    @Test
    @Order(1)
    @DisplayName("Simple FETCH FIRST Test")
    @Description("Validates basic pagination using FETCH FIRST clause with ordered results")
    @Severity(SeverityLevel.BLOCKER)
    void testSimplePagination() {
        transaction = session.beginTransaction(); // Start new transaction for test
        try {
            // Get first 5 emails using FETCH FIRST
            Query<String> query = session.createQuery(
                "SELECT c.email FROM Contact c ORDER BY c.id FETCH FIRST 5 ROWS ONLY", String.class);
            
            List<String> emails = query.getResultList();
            assertEquals(5, emails.size(), "Should return exactly 5 emails");
            
            // Verify content
            for (int i = 0; i < emails.size(); i++) {
                assertEquals("user" + (i + 1) + "@test.com", emails.get(i),
                    "Email should match");
            }
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }
            throw e;
        }
    }
    
    /**
     * Creates test data for pagination tests.
     * Generates TOTAL_CONTACTS number of Contact entities with predictable data.
     * Each contact has:
     * - Sequential email (user1@test.com, user2@test.com, etc.)
     * - Matching login (user1, user2, etc.)
     * - Matching password (pass1, pass2, etc.)
     * - Generic first/last names
     */
    private void createTestData() {
        for (int i = 1; i <= TOTAL_CONTACTS; i++) {
            Contact contact = new Contact();
            contact.setEmail("user" + i + "@test.com");
            contact.setFirstName("User");
            contact.setLastName("Test" + i);
            contact.setLogin("user" + i);
            contact.setPassword("pass" + i);
            session.persist(contact);
        }
        session.flush();
    }
    
    /**
     * Cleans up all test data.
     * Executes a bulk delete operation to remove all Contact entities.
     * Must be called within an active transaction.
     */
    private void cleanupTestData() {
        session.createQuery("DELETE FROM Contact").executeUpdate();
        session.flush();
    }
}
