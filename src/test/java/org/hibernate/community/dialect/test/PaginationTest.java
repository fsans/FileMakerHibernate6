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
     * Most basic form of pagination that retrieves the first N rows.
     */
    @Test
    @Order(1)
    @DisplayName("Simple FETCH FIRST Test")
    @Description("Validates basic pagination using FETCH FIRST clause with ordered results")
    @Severity(SeverityLevel.BLOCKER)
    void testSimplePagination() {
        transaction = session.beginTransaction();
        try {
            Query<String> query = session.createQuery(
                "SELECT c.email FROM Contact c ORDER BY c.id FETCH FIRST 5 ROWS ONLY", String.class);
            
            List<String> emails = query.getResultList();
            assertEquals(5, emails.size(), "Should return exactly 5 emails");
            
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
     * Tests pagination using OFFSET to skip records.
     * Verifies that OFFSET properly skips the specified number of rows.
     */
    @Test
    @Order(2)
    @DisplayName("OFFSET with FETCH FIRST Test")
    @Description("Validates pagination using OFFSET to skip records and FETCH FIRST to limit results")
    @Severity(SeverityLevel.CRITICAL)
    void testOffsetPagination() {
        transaction = session.beginTransaction();
        try {
            // Get records 4-6 (3 records starting from position 3)
            Query<String> query = session.createQuery(
                "SELECT c.email FROM Contact c ORDER BY c.id OFFSET 3 ROWS FETCH FIRST 3 ROWS ONLY", 
                String.class);
            
            List<String> emails = query.getResultList();
            assertEquals(3, emails.size(), "Should return exactly 3 emails");
            
            // Should get emails 4, 5, and 6
            for (int i = 0; i < emails.size(); i++) {
                assertEquals("user" + (i + 4) + "@test.com", emails.get(i),
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
     * Tests pagination with different ORDER BY clauses.
     * Verifies that results are properly ordered and paginated.
     */
    @Test
    @Order(3)
    @DisplayName("ORDER BY with Pagination Test")
    @Description("Validates pagination with different ordering options")
    @Severity(SeverityLevel.CRITICAL)
    void testOrderedPagination() {
        transaction = session.beginTransaction();
        try {
            // Get first 5 records ordered by email descending
            Query<String> query = session.createQuery(
                "SELECT c.email FROM Contact c ORDER BY c.email DESC FETCH FIRST 5 ROWS ONLY", 
                String.class);
            
            List<String> emails = query.getResultList();
            assertEquals(5, emails.size(), "Should return exactly 5 emails");
            
            // Verify descending order
            for (int i = 0; i < emails.size() - 1; i++) {
                assertTrue(emails.get(i).compareTo(emails.get(i + 1)) > 0,
                    "Emails should be in descending order");
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
     * Tests edge cases of pagination:
     * 1. OFFSET beyond available records
     * 2. FETCH FIRST with zero rows
     * 3. OFFSET with zero rows
     * 4. Requesting more rows than available
     */
    @Test
    @Order(4)
    @DisplayName("Pagination Edge Cases Test")
    @Description("Validates pagination behavior in edge cases")
    @Severity(SeverityLevel.NORMAL)
    void testPaginationEdgeCases() {
        transaction = session.beginTransaction();
        try {
            // Test 1: OFFSET beyond available records
            Query<String> query1 = session.createQuery(
                "SELECT c.email FROM Contact c ORDER BY c.id OFFSET 20 ROWS FETCH FIRST 5 ROWS ONLY", 
                String.class);
            List<String> emails1 = query1.getResultList();
            assertTrue(emails1.isEmpty(), "Should return empty list when offset is beyond available records");

            // Test 2: FETCH FIRST with zero rows
            Query<String> query2 = session.createQuery(
                "SELECT c.email FROM Contact c ORDER BY c.id FETCH FIRST 0 ROWS ONLY", 
                String.class);
            List<String> emails2 = query2.getResultList();
            assertTrue(emails2.isEmpty(), "Should return empty list when fetching zero rows");

            // Test 3: Request more rows than available
            Query<String> query3 = session.createQuery(
                "SELECT c.email FROM Contact c ORDER BY c.id FETCH FIRST 20 ROWS ONLY", 
                String.class);
            List<String> emails3 = query3.getResultList();
            assertEquals(TOTAL_CONTACTS, emails3.size(), 
                "Should return all available records when requesting more than exist");

            transaction.commit();
        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }
            throw e;
        }
    }

    /**
     * Tests multiple sequential pages to ensure consistent results.
     * Verifies that combining multiple page requests retrieves all records exactly once.
     */
    @Test
    @Order(5)
    @DisplayName("Sequential Pagination Test")
    @Description("Validates consistency of results when paginating through all records")
    @Severity(SeverityLevel.CRITICAL)
    void testSequentialPagination() {
        transaction = session.beginTransaction();
        try {
            final int PAGE_SIZE = 3;
            int offset = 0;
            int totalRecords = 0;
            
            // Collect all emails through pagination
            while (true) {
                Query<String> query = session.createQuery(
                    "SELECT c.email FROM Contact c ORDER BY c.id OFFSET " + offset + 
                    " ROWS FETCH FIRST " + PAGE_SIZE + " ROWS ONLY", 
                    String.class);
                
                List<String> emails = query.getResultList();
                if (emails.isEmpty()) {
                    break;
                }
                
                // Verify each page
                for (int i = 0; i < emails.size(); i++) {
                    assertEquals("user" + (offset + i + 1) + "@test.com", emails.get(i),
                        "Email should match on page starting at offset " + offset);
                }
                
                totalRecords += emails.size();
                offset += PAGE_SIZE;
            }
            
            assertEquals(TOTAL_CONTACTS, totalRecords, 
                "Total records from all pages should match expected count");
            
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
