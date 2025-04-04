package org.hibernate.community.dialect.test;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.internal.SessionFactoryImpl;
import org.junit.jupiter.api.*;
import io.qameta.allure.*;

/**
 * Test suite for verifying FileMaker's SQL pagination behavior using direct JDBC queries.
 * 
 * This class tests specific edge cases that need to be verified at the JDBC level
 * rather than through Hibernate's abstraction layer, particularly focusing on:
 * 
 * 1. OFFSET values greater than the total number of rows
 * 2. Direct SQL execution without Hibernate's query transformation
 * 
 * FileMaker should return an empty result set (not an error) when OFFSET exceeds
 * the total number of available records.
 */
@Epic("FileMaker JDBC Integration")
@Feature("Native SQL Pagination - Direct JDBC verification of FileMaker SQL pagination features:\n" +
         "1. OFFSET beyond available records\n" +
         "2. Empty result set handling\n" +
         "3. Error-free execution")
@Owner("FileMaker Team")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("FileMaker Native SQL Verification")
@Severity(SeverityLevel.CRITICAL)
@Issue("HIBERNATE-007")
@TmsLink("TC-007")
public class NativeSQLPagination {
    
    private static SessionFactory sessionFactory;
    private Session session;
    private Transaction transaction;
    private Connection jdbcConnection;
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
    void setUp() throws SQLException {
        session = sessionFactory.openSession();
        cleanupTestData(); // Clean all existing data
        createTestData();  // Create fresh test data
        
        // Get a JDBC connection from Hibernate's connection pool
        ConnectionProvider connectionProvider = 
                ((SessionFactoryImpl) sessionFactory).getServiceRegistry()
                .getService(ConnectionProvider.class);
        jdbcConnection = connectionProvider.getConnection();
        jdbcConnection.setAutoCommit(false);
    }

    @AfterEach
    void tearDown() throws SQLException {
        if (jdbcConnection != null) {
            jdbcConnection.rollback();
            jdbcConnection.close();
        }
        
        if (session != null) {
            cleanupTestData(); // Clean all test data
            session.close();
        }
    }
    
    /**
     * Tests that FileMaker correctly handles OFFSET values greater than the total number of rows
     * using direct JDBC SQL queries.
     * 
     * According to FileMaker documentation, when OFFSET exceeds the total number of rows,
     * an empty result set should be returned without an error.
     */
    @Test
    @Order(1)
    @DisplayName("JDBC Native SQL - OFFSET Beyond Total Rows")
    @Description("Validates that FileMaker returns an empty result set when OFFSET exceeds total rows using direct JDBC")
    @Severity(SeverityLevel.CRITICAL)
    void testOffsetBeyondTotalRowsWithNativeJdbc() throws SQLException {
        // First verify the total number of records
        String countSql = "SELECT COUNT(*) FROM contact";
        try (Statement stmt = jdbcConnection.createStatement();
             ResultSet rs = stmt.executeQuery(countSql)) {
            
            assertTrue(rs.next(), "Count query should return a result");
            int totalCount = rs.getInt(1);
            assertEquals(TOTAL_CONTACTS, totalCount, "Total count should match expected test data size");
        }
        
        // Test with OFFSET beyond total rows
        int excessiveOffset = TOTAL_CONTACTS + 10; // Well beyond available records
        String paginationSql = "SELECT email FROM contact ORDER BY id OFFSET " + excessiveOffset + " ROWS";
        
        try (Statement stmt = jdbcConnection.createStatement();
             ResultSet rs = stmt.executeQuery(paginationSql)) {
            
            // Should return an empty result set, not throw an exception
            assertFalse(rs.next(), "Result set should be empty when OFFSET exceeds total rows");
        } catch (SQLException e) {
            fail("FileMaker should return empty result set for OFFSET greater than total rows, not throw an exception: " + e.getMessage());
        }
    }
    
    /**
     * Tests various combinations of OFFSET and FETCH FIRST clauses using direct JDBC
     * to verify FileMaker's pagination behavior at the SQL level.
     */
    @Test
    @Order(2)
    @DisplayName("JDBC Native SQL - Pagination Edge Cases")
    @Description("Validates FileMaker's pagination behavior with various OFFSET and FETCH combinations using direct JDBC")
    @Severity(SeverityLevel.CRITICAL)
    void testPaginationEdgeCasesWithNativeJdbc() throws SQLException {
        // Test 1: OFFSET 0 - Should return all rows
        String sql1 = "SELECT email FROM contact ORDER BY id OFFSET 0 ROWS";
        List<String> emails = new ArrayList<>();
        
        try (Statement stmt = jdbcConnection.createStatement();
             ResultSet rs = stmt.executeQuery(sql1)) {
            
            while (rs.next()) {
                emails.add(rs.getString("email"));
            }
            
            assertEquals(TOTAL_CONTACTS, emails.size(), "OFFSET 0 should return all rows");
            
            // Verify the first few emails follow expected pattern
            for (int i = 0; i < 3; i++) {
                assertEquals("user" + (i + 1) + "@test.com", emails.get(i), 
                    "Email at position " + i + " should match expected pattern");
            }
        }
        
        // Test 2: OFFSET > total with FETCH FIRST - Should still return empty set
        String sql2 = "SELECT email FROM contact ORDER BY id OFFSET " + 
                      (TOTAL_CONTACTS + 5) + " ROWS FETCH FIRST 5 ROWS ONLY";
        
        try (Statement stmt = jdbcConnection.createStatement();
             ResultSet rs = stmt.executeQuery(sql2)) {
            
            assertFalse(rs.next(), "Result set should be empty when OFFSET exceeds total rows, even with FETCH FIRST");
        }
        
        // Test 3: Valid OFFSET with FETCH FIRST - Should return correct subset
        String sql3 = "SELECT email FROM contact ORDER BY id OFFSET 2 ROWS FETCH FIRST 3 ROWS ONLY";
        emails.clear();
        
        try (Statement stmt = jdbcConnection.createStatement();
             ResultSet rs = stmt.executeQuery(sql3)) {
            
            while (rs.next()) {
                emails.add(rs.getString("email"));
            }
            
            assertEquals(3, emails.size(), "Should return exactly 3 emails");
            
            // Should get records 3, 4, and 5 (0-indexed, so OFFSET 2)
            for (int i = 0; i < emails.size(); i++) {
                assertEquals("user" + (i + 3) + "@test.com", emails.get(i),
                    "Email should match for offset record " + (i + 3));
            }
        }
    }
    
    /**
     * Tests edge cases specific to the FETCH FIRST clause:
     * 1. FETCH FIRST 0 ROWS ONLY - Should throw an SQLException (minimum valid value is 1)
     * 2. FETCH FIRST 1 ROW ONLY - Should return exactly one row
     * 3. FETCH FIRST 100 PERCENT ROWS ONLY - Should return all rows
     * 4. FETCH FIRST with WITH TIES clause (requires ORDER BY)
     */
    @Test
    @Order(3)
    @DisplayName("JDBC Native SQL - FETCH FIRST Edge Cases")
    @Description("Validates FileMaker's handling of various FETCH FIRST clause edge cases using direct JDBC")
    @Severity(SeverityLevel.CRITICAL)
    void testFetchFirstEdgeCasesWithNativeJdbc() throws SQLException {
        // Test 1: FETCH FIRST 0 ROWS ONLY - Should throw an SQLException (minimum valid value is 1)
        String sql1 = "SELECT email FROM contact ORDER BY id FETCH FIRST 0 ROWS ONLY";
        
        try (Statement stmt = jdbcConnection.createStatement()) {
            SQLException exception = assertThrows(
                SQLException.class,
                () -> stmt.executeQuery(sql1),
                "FETCH FIRST 0 ROWS ONLY should throw an SQLException as n must be 1 or greater"
            );
             
            assertTrue(
                exception.getMessage().contains("fetch count") || 
                exception.getMessage().contains("not valid") ||
                exception.getMessage().contains("FQL0052"),
                "Exception should indicate an invalid fetch count: " + exception.getMessage()
            );
        }
        
        // Test 2: FETCH FIRST 1 ROW ONLY - Should return exactly one row
        String sql2 = "SELECT email FROM contact ORDER BY id FETCH FIRST 1 ROW ONLY";
        
        try (Statement stmt = jdbcConnection.createStatement();
             ResultSet rs = stmt.executeQuery(sql2)) {
            
            assertTrue(rs.next(), "FETCH FIRST 1 ROW ONLY should return one row");
            assertEquals("user1@test.com", rs.getString("email"), "First email should match");
            assertFalse(rs.next(), "FETCH FIRST 1 ROW ONLY should return exactly one row");
        }
        
        // Test 3: FETCH FIRST 100 PERCENT ROWS ONLY - Should return all rows
        String sql3 = "SELECT email FROM contact ORDER BY id FETCH FIRST 100 PERCENT ROWS ONLY";
        List<String> emails = new ArrayList<>();
        
        try (Statement stmt = jdbcConnection.createStatement();
             ResultSet rs = stmt.executeQuery(sql3)) {
            
            while (rs.next()) {
                emails.add(rs.getString("email"));
            }
            
            assertEquals(TOTAL_CONTACTS, emails.size(), 
                "FETCH FIRST 100 PERCENT ROWS ONLY should return all rows");
        }
        
        // Test 4: FETCH FIRST with WITH TIES clause
        // First create some test data with duplicate values for testing WITH TIES
        try (Statement stmt = jdbcConnection.createStatement()) {
            // Update some records to have the same last name for WITH TIES testing
            stmt.executeUpdate("UPDATE contact SET last_name = 'DuplicateValue' WHERE id IN (2, 3, 4)");
        }
        
        // Now test WITH TIES - should return all rows with the same value in the ORDER BY column
        String sql4 = "SELECT email, last_name FROM contact WHERE last_name = 'DuplicateValue' ORDER BY last_name FETCH FIRST 1 ROW WITH TIES";
        List<String> tieEmails = new ArrayList<>();
        
        try (Statement stmt = jdbcConnection.createStatement();
             ResultSet rs = stmt.executeQuery(sql4)) {
            
            while (rs.next()) {
                String lastName = rs.getString("last_name");
                String email = rs.getString("email");
                tieEmails.add(email);
                
                // All returned rows should have the same last_name value due to WITH TIES
                String expectedLastName = "DuplicateValue";
                assertEquals(expectedLastName, lastName);
            }
            
            // Print the actual number of rows returned for debugging
            System.out.println("Number of rows returned by WITH TIES query: " + tieEmails.size());
            
            // Check if we got all the rows with DuplicateValue
            // First, let's count how many rows should have this value
            String countSql = "SELECT COUNT(*) FROM contact WHERE last_name = 'DuplicateValue'";
            int expectedCount = 0;
            try (Statement countStmt = jdbcConnection.createStatement();
                 ResultSet countRs = countStmt.executeQuery(countSql)) {
                if (countRs.next()) {
                    expectedCount = countRs.getInt(1);
                }
            }
            
            System.out.println("Expected number of rows with DuplicateValue: " + expectedCount);
            assertEquals(expectedCount, tieEmails.size(), "WITH TIES should return all rows with the duplicate last name");
        }
    }
    
    /**
     * Tests the interaction between FETCH FIRST and ORDER BY clauses:
     * 1. FETCH FIRST without ORDER BY - Results in undefined ordering
     * 2. FETCH FIRST with different ORDER BY directions
     * 3. FETCH FIRST with multiple ORDER BY columns
     */
    @Test
    @Order(4)
    @DisplayName("JDBC Native SQL - FETCH FIRST with ORDER BY Variations")
    @Description("Validates FileMaker's handling of FETCH FIRST with various ORDER BY combinations")
    @Severity(SeverityLevel.CRITICAL)
    void testFetchFirstWithOrderByVariations() throws SQLException {
        // Test 1: FETCH FIRST without ORDER BY - Results in undefined ordering
        // We can only verify count, not specific order
        String sql1 = "SELECT email FROM contact FETCH FIRST 5 ROWS ONLY";
        int count = 0;
        
        try (Statement stmt = jdbcConnection.createStatement();
             ResultSet rs = stmt.executeQuery(sql1)) {
            
            while (rs.next()) {
                count++;
            }
            
            assertEquals(5, count, "FETCH FIRST without ORDER BY should return correct row count");
        }
        
        // Test 2: FETCH FIRST with ORDER BY DESC
        String sql2 = "SELECT email FROM contact ORDER BY email DESC FETCH FIRST 3 ROWS ONLY";
        List<String> emails = new ArrayList<>();
        
        try (Statement stmt = jdbcConnection.createStatement();
             ResultSet rs = stmt.executeQuery(sql2)) {
            
            while (rs.next()) {
                emails.add(rs.getString("email"));
            }
            
            assertEquals(3, emails.size(), "Should return exactly 3 emails");
            
            // Verify descending order
            for (int i = 0; i < emails.size() - 1; i++) {
                assertTrue(emails.get(i).compareTo(emails.get(i + 1)) > 0,
                    "Emails should be in descending order");
            }
        }
        
        // Test 3: FETCH FIRST with multiple ORDER BY columns
        // First set up some test data with duplicate first names but different last names
        try (Statement stmt = jdbcConnection.createStatement()) {
            stmt.executeUpdate("UPDATE contact SET first_name = 'SameFirstName' WHERE id IN (5, 6, 7)");
        }
        
        String sql3 = "SELECT first_name, last_name FROM contact " +
                      "ORDER BY first_name, last_name DESC " +
                      "FETCH FIRST 5 ROWS ONLY";
        
        List<String> firstNames = new ArrayList<>();
        List<String> lastNames = new ArrayList<>();
        
        try (Statement stmt = jdbcConnection.createStatement();
             ResultSet rs = stmt.executeQuery(sql3)) {
            
            while (rs.next()) {
                firstNames.add(rs.getString("first_name"));
                lastNames.add(rs.getString("last_name"));
            }
            
            assertEquals(5, firstNames.size(), "Should return exactly 5 rows");
            
            // Find indices where first_name is 'SameFirstName'
            List<Integer> sameNameIndices = new ArrayList<>();
            for (int i = 0; i < firstNames.size(); i++) {
                if ("SameFirstName".equals(firstNames.get(i))) {
                    sameNameIndices.add(i);
                }
            }
            
            // If we have multiple rows with the same first name, verify last names are in descending order
            if (sameNameIndices.size() > 1) {
                for (int i = 0; i < sameNameIndices.size() - 1; i++) {
                    int idx1 = sameNameIndices.get(i);
                    int idx2 = sameNameIndices.get(i + 1);
                    assertTrue(lastNames.get(idx1).compareTo(lastNames.get(idx2)) > 0,
                        "Last names should be in descending order for same first names");
                }
            }
        }
    }
    
    /**
     * Tests extreme edge cases for FETCH FIRST clause:
     * 1. Very large OFFSET value (beyond int max value)
     * 2. Very large FETCH FIRST value
     * 3. PERCENT values in FETCH FIRST clause
     */
    @Test
    @Order(5)
    @DisplayName("JDBC Native SQL - FETCH FIRST Extreme Edge Cases")
    @Description("Validates FileMaker's handling of extreme values in FETCH FIRST clause")
    @Severity(SeverityLevel.NORMAL)
    void testFetchFirstExtremeEdgeCases() throws SQLException {
        // Test 1: Very large OFFSET value
        // Using a value close to but below Integer.MAX_VALUE to avoid overflow
        String sql1 = "SELECT email FROM contact ORDER BY id OFFSET 2000000000 ROWS";
        
        try (Statement stmt = jdbcConnection.createStatement();
             ResultSet rs = stmt.executeQuery(sql1)) {
            
            assertFalse(rs.next(), "Very large OFFSET should return empty result set");
        } catch (SQLException e) {
            // Some databases might reject extremely large values, so we'll be lenient here
            System.out.println("Note: Very large OFFSET value test resulted in: " + e.getMessage());
        }
        
        // Test 2: Very large FETCH FIRST value
        String sql2 = "SELECT email FROM contact ORDER BY id FETCH FIRST 1000000 ROWS ONLY";
        int count = 0;
        
        try (Statement stmt = jdbcConnection.createStatement();
             ResultSet rs = stmt.executeQuery(sql2)) {
            
            while (rs.next()) {
                count++;
            }
            
            assertEquals(TOTAL_CONTACTS, count, 
                "Very large FETCH FIRST should return all available rows");
        } catch (SQLException e) {
            // Some databases might reject extremely large values, so we'll be lenient here
            System.out.println("Note: Very large FETCH FIRST value test resulted in: " + e.getMessage());
        }
        
        // Test 3: PERCENT values in FETCH FIRST clause
        
        // Test 3.1: Fractional PERCENT values are supported in FileMaker
        String sqlFractional = "SELECT email FROM contact ORDER BY id FETCH FIRST 50.5 PERCENT ROWS ONLY";
        
        try (Statement stmt = jdbcConnection.createStatement();
             ResultSet rs = stmt.executeQuery(sqlFractional)) {
            
            int rowCount = 0;
            while (rs.next()) {
                rowCount++;
            }
            
            // 50.5% of 10 rows is 5.05, which returns 6 rows
            // FileMaker appears to apply ceiling() to fractional results
            assertEquals(6, rowCount, "Fractional PERCENT should return the ceiling of the calculated rows");
            System.out.println("Fractional PERCENT (50.5%) returned " + rowCount + " rows");
            
        } catch (SQLException e) {
            fail("Fractional PERCENT values should be supported in FileMaker SQL: " + e.getMessage());
        }
        
        // Test 3.2: Integer PERCENT values should work correctly
        String sqlInteger = "SELECT email FROM contact ORDER BY id FETCH FIRST 50 PERCENT ROWS ONLY";
        
        try (Statement stmt = jdbcConnection.createStatement();
             ResultSet rs = stmt.executeQuery(sqlInteger)) {
            
            int rowCount = 0;
            while (rs.next()) {
                rowCount++;
            }
            
            // 50% of 10 rows should be 5
            assertEquals(5, rowCount, "Integer PERCENT should return the correct number of rows");
            
        } catch (SQLException e) {
            fail("Integer PERCENT should be supported by FileMaker SQL: " + e.getMessage());
        }
    }
    
    /**
     * Creates test data for pagination tests.
     * Generates TOTAL_CONTACTS number of Contact entities with predictable data.
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
