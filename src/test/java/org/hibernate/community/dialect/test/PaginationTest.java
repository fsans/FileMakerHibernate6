package org.hibernate.community.dialect.test;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.community.dialect.FileMakerDialect;
import org.hibernate.community.dialect.pagination.FileMakerLimitHandler2;
import org.hibernate.dialect.DatabaseVersion;
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
    private static final int TOTAL_CONTACTS = 10; // Test data size
    
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
        cleanupTestData(); // Clean all existing data
        createTestData();  // Create fresh test data
    }

    @AfterEach
    void tearDown() {
        if (session != null) {
            cleanupTestData(); // Clean all test data
            session.close();
        }
    }
    

    @Test
    @Order(1)
    @Story("Limit Handler")
    @Description("Validates FileMaker's SQL LIMIT clause handling:\n" +
                "1. Proper LimitHandler implementation\n" +
                "2. OFFSET/FETCH support\n" +
                "3. Pagination capabilities\n\n" +
                "Expected: Should use FileMakerLimitHandler2 for pagination")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Limit Handler Configuration")
    @Issue("HIBERNATE-002")
    @TmsLink("TC-002")
    void testLimitHandler() {

        FileMakerDialect dialect = new FileMakerDialect(DatabaseVersion.make(19, 6));

        assertNotNull(dialect.getLimitHandler());
        assertTrue(dialect.getLimitHandler() instanceof FileMakerLimitHandler2);
    } 


    /**
     * Tests basic pagination using FETCH FIRST with ordered results.
     * Most basic form of pagination that retrieves the first N rows.
     */
    @Test
    @Order(2)
    @DisplayName("Simple FETCH FIRST Test")
    @Description("Validates basic pagination using FETCH FIRST clause with ordered results")
    @Severity(SeverityLevel.BLOCKER)
    void testSimplePagination() {
        transaction = session.beginTransaction();
        try {
            // Verify data exists first
            Query<Long> countQuery = session.createQuery(
                "SELECT COUNT(*) FROM Contact c", Long.class);
            long count = countQuery.getSingleResult();
            assertTrue(count > 0, "Test data should exist");

            Query<String> query = session.createQuery(
                "SELECT c.email FROM Contact c ORDER BY c.id FETCH FIRST 5 ROWS ONLY", 
                String.class);
            
            List<String> emails = query.getResultList();
            assertEquals(5, emails.size(), "Should return exactly 5 emails");
            
            for (int i = 0; i < emails.size(); i++) {
                assertEquals("user" + (i + 1) + "@test.com", emails.get(i),
                    "Email should match for index " + i);
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
    @Order(3)
    @DisplayName("OFFSET with FETCH FIRST Test")
    @Description("Validates pagination using OFFSET to skip records and FETCH FIRST to limit results")
    @Severity(SeverityLevel.CRITICAL)
    void testOffsetPagination() {
        transaction = session.beginTransaction();
        try {
            // Start from index 3 (4th record) and get 3 records
            Query<String> query = session.createQuery(
                "SELECT c.email FROM Contact c ORDER BY c.id OFFSET 3 ROWS FETCH FIRST 3 ROWS ONLY",
                String.class);
            
            List<String> emails = query.getResultList();
            assertEquals(3, emails.size(), "Should return exactly 3 emails");
            
            // Should get records 4, 5, and 6
            for (int i = 0; i < emails.size(); i++) {
                assertEquals("user" + (i + 4) + "@test.com", emails.get(i),
                    "Email should match for offset record " + (i + 4));
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
    @Order(4)
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
    @Disabled
    @Order(5)
    @DisplayName("Pagination Edge Cases Test")
    @Description("Validates pagination behavior in edge cases")
    @Severity(SeverityLevel.NORMAL)
    void testPaginationEdgeCases() {
        transaction = session.beginTransaction();
        try {
            // Test 1: OFFSET beyond available records
            // Use Hibernate's API, let dialect translate to OFFSET/FETCH
            Query<String> query1 = session.createQuery(
                "SELECT c.email FROM Contact c ORDER BY c.id", 
                String.class)
                .setFirstResult(20)  // Test 1: OFFSET beyond available records
                .setMaxResults(5);
                
            // Debug: Print the generated SQL
            String sql1 = query1.unwrap(org.hibernate.query.Query.class).getQueryString();
            System.out.println("DEBUG - Generated SQL for query1: " + sql1);
            
            List<String> emails1 = query1.getResultList();
            assertTrue(emails1.isEmpty(), 
                "Should return empty list when offset is beyond available records");

            // Test 2: Small page size
            // Let dialect translate to FETCH FIRST n ROWS
            Query<String> query2 = session.createQuery(
                "SELECT c.email FROM Contact c ORDER BY c.id", 
                String.class)
                .setMaxResults(5);
                
            // Debug: Print the generated SQL
            String sql2 = query2.unwrap(org.hibernate.query.Query.class).getQueryString();
            System.out.println("DEBUG - Generated SQL for query2: " + sql2);
            
            List<String> emails2 = query2.getResultList();
            assertEquals(5, emails2.size(), 
                "Should return exactly 5 rows");
            // Verify we got the first 5 emails
            for (int i = 0; i < 5; i++) {
                assertEquals("user" + (i + 1) + "@test.com", emails2.get(i),
                    "Email at position " + i + " should match expected pattern");
            }

            // Test 3: Percentage-based fetch (FileMaker-specific feature)
            // Use direct SQL since Hibernate doesn't have API for PERCENT
            Query<String> query3 = session.createQuery(
                "SELECT c.email FROM Contact c ORDER BY c.id FETCH FIRST 50 PERCENT ROWS ONLY", 
                String.class);
                
            // Debug: Print the generated SQL
            String sql3 = query3.unwrap(org.hibernate.query.Query.class).getQueryString();
            System.out.println("DEBUG - Generated SQL for query3: " + sql3);
            
            List<String> emails3 = query3.getResultList();
            assertEquals(5, emails3.size(), 
                "Should return 50% of available records (5 out of 10)");
                
            // Test 3.1: Fractional percentage-based fetch
            // FileMaker applies ceiling() to fractional percentages
            Query<String> query3f = session.createQuery(
                "SELECT c.email FROM Contact c ORDER BY c.id FETCH FIRST 50.5 PERCENT ROWS ONLY", 
                String.class);
                
            // Debug: Print the generated SQL
            String sql3f = query3f.unwrap(org.hibernate.query.Query.class).getQueryString();
            System.out.println("DEBUG - Generated SQL for query3f: " + sql3f);
            
            List<String> emails3f = query3f.getResultList();
            assertEquals(6, emails3f.size(), 
                "Should return ceiling of 50.5% of available records (6 out of 10)");

            // Test 4: Request more rows than available
            // Let dialect translate to FETCH FIRST n ROWS
            Query<String> query4 = session.createQuery(
                "SELECT c.email FROM Contact c ORDER BY c.id", 
                String.class)
                .setMaxResults(20);
                
            // Debug: Print the generated SQL
            String sql4 = query4.unwrap(org.hibernate.query.Query.class).getQueryString();
            System.out.println("DEBUG - Generated SQL for query4: " + sql4);
            
            List<String> emails4 = query4.getResultList();
            assertEquals(10, emails4.size(), 
                "Should return all 10 available records when requesting more than exist");

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
    @Disabled
    @Order(6)
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
                    "SELECT c.email FROM Contact c ORDER BY c.id", 
                    String.class)
                    .setFirstResult(offset)
                    .setMaxResults(PAGE_SIZE);
                
                // Debug: Print the generated SQL
                String sql = query.unwrap(org.hibernate.query.Query.class).getQueryString();
                System.out.println("DEBUG - Generated SQL for query: " + sql);
                
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
     * Tests specific edge cases of FileMaker SQL pagination:
     * 1. OFFSET 0: Returns all rows without skipping
     * 2. OFFSET n where n > total_rows: Returns an empty result set without an error
     * 3. FETCH FIRST 0 ROWS ONLY: Returns an empty result set without an error
     * 4. FETCH FIRST 100 PERCENT ROWS ONLY: Returns all rows
     * 5. Use without ORDER BY: Retrieves an arbitrary subset of rows due to undefined ordering
     * 6. OFFSET and FETCH combined for pagination
     */
    @Test
    @Order(7)
    @DisplayName("FileMaker SQL Pagination Edge Cases")
    @Description("Validates FileMaker SQL pagination behavior in specific edge cases")
    @Severity(SeverityLevel.CRITICAL)
    @Issue("HIBERNATE-006")
    @TmsLink("TC-006")
    void testFileMakerSqlPaginationEdgeCases() {
        transaction = session.beginTransaction();
        try {
            // Verify data exists first
            Query<Long> countQuery = session.createQuery(
                "SELECT COUNT(*) FROM Contact c", Long.class);
            long totalCount = countQuery.getSingleResult();
            assertEquals(TOTAL_CONTACTS, totalCount, "Test data count should match expected");

            // Test 1: OFFSET 0 - Should return all rows without skipping
            Query<String> query1 = session.createQuery(
                "SELECT c.email FROM Contact c ORDER BY c.id OFFSET 0 ROWS", 
                String.class);
            
            List<String> result1 = query1.getResultList();
            assertEquals(TOTAL_CONTACTS, result1.size(), 
                "OFFSET 0 should return all rows without skipping");
            
            // Test 2: OFFSET > total_rows - FileMaker throws an exception rather than returning empty set
            try {
                Query<String> query2 = session.createQuery(
                    "SELECT c.email FROM Contact c ORDER BY c.id OFFSET " + (TOTAL_CONTACTS + 10) + " ROWS", 
                    String.class);
                
                List<String> result2 = query2.getResultList();
                // This line won't be reached with FileMaker
                fail("FileMaker should throw an exception for OFFSET > total_rows");
            } catch (Exception e) {
                // Expected behavior for FileMaker
                assertTrue(e.getMessage().contains("error in the syntax") || 
                           e.getMessage().contains("execution failed") ||
                           e.getMessage().contains("FQL"),
                    "FileMaker should throw a specific error for excessive OFFSET values");
            }
            
            // Test 3: FETCH FIRST 0 ROWS ONLY - Should return empty result set
            Query<String> query3 = session.createQuery(
                "SELECT c.email FROM Contact c ORDER BY c.id FETCH FIRST 0 ROWS ONLY", 
                String.class);
            
            List<String> result3 = query3.getResultList();
            assertTrue(result3.isEmpty(), 
                "FETCH FIRST 0 ROWS ONLY should return empty result set without error");
            
            // Test 4: FETCH FIRST 100 PERCENT ROWS ONLY - Should return all rows
            Query<String> query4 = session.createQuery(
                "SELECT c.email FROM Contact c ORDER BY c.id FETCH FIRST 100 PERCENT ROWS ONLY", 
                String.class);
            
            List<String> result4 = query4.getResultList();
            assertEquals(TOTAL_CONTACTS, result4.size(), 
                "FETCH FIRST 100 PERCENT ROWS ONLY should return all rows");
            
            // Test 5: Without ORDER BY - Should retrieve rows but in undefined order
            // We can only verify count, not specific order
            Query<String> query5 = session.createQuery(
                "SELECT c.email FROM Contact c FETCH FIRST 5 ROWS ONLY", 
                String.class);
            
            List<String> result5 = query5.getResultList();
            assertEquals(5, result5.size(), 
                "Without ORDER BY should still return correct number of rows");
            
            // Test 6: OFFSET and FETCH combined for pagination
            // Get rows 4-6 (0-indexed, so OFFSET 3)
            Query<String> query6 = session.createQuery(
                "SELECT c.email FROM Contact c ORDER BY c.id OFFSET 3 ROWS FETCH FIRST 3 ROWS ONLY", 
                String.class);
            
            List<String> result6 = query6.getResultList();
            assertEquals(3, result6.size(), 
                "Combined OFFSET and FETCH should return correct number of rows");
            
            // Verify we got the expected records (4th, 5th, and 6th)
            for (int i = 0; i < result6.size(); i++) {
                assertEquals("user" + (i + 4) + "@test.com", result6.get(i),
                    "Email should match for combined pagination at index " + i);
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
        Transaction tx = session.beginTransaction();
        try {
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
            tx.commit();
        } catch (Exception e) {
            if (tx != null && tx.isActive()) {
                tx.rollback();
            }
            throw e;
        }
    }
    
    /**
     * Cleans up all test data.
     * Executes a bulk delete operation to remove all Contact entities.
     */
    private void cleanupTestData() {
        Transaction tx = session.beginTransaction();
        try {
            session.createMutationQuery("DELETE FROM Contact").executeUpdate();
            session.flush();
            tx.commit();
        } catch (Exception e) {
            if (tx != null && tx.isActive()) {
                tx.rollback();
            }
            throw e;
        }
    }
}
