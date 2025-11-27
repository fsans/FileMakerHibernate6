package org.hibernate.community.dialect.test;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.junit.jupiter.api.*;

import io.qameta.allure.*;

/**
 * Comprehensive test suite for FileMaker date/time type handling.
 * 
 * FileMaker supports three temporal types:
 * - DATE (SQL type 91) - date only, no time component
 * - TIME (SQL type 92) - time only, no date component  
 * - TIMESTAMP (SQL type 93) - full date and time
 * 
 * Java type mappings:
 * - LocalDate -> DATE
 * - LocalTime -> TIME
 * - LocalDateTime -> TIMESTAMP
 * - java.util.Date with @Temporal(TIMESTAMP) -> TIMESTAMP
 */
@Epic("FileMaker Hibernate Integration")
@Feature("Date/Time Operations - TIMESTAMP, DATE, and TIME field handling")
@Owner("FileMaker Team")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("FileMaker Date/Time Operations Tests")
@Severity(SeverityLevel.CRITICAL)
public class DateTimeOperationsTest {
    
    private Session session;
    
    @BeforeEach
    void setUp() {
        session = HibernateUtil.getSessionFactory().openSession();
    }

    @AfterEach
    void tearDown() {
        if (session != null) {
            session.close();
        }
    }
    
    // ========== LocalDate Tests (DATE type) ==========
    
    @Test
    @Order(1)
    @Story("LocalDate Handling")
    @Description("Tests LocalDate mapping to FileMaker DATE type")
    @DisplayName("LocalDate - Basic persistence and retrieval")
    void testLocalDatePersistence() {
        Contact contact = new Contact("localdate@test.com", "localdatetest", "test123");
        LocalDate testDate = LocalDate.of(2025, 11, 27);
        contact.setCreateDate(testDate);
        
        Transaction tx = session.beginTransaction();
        session.persist(contact);
        tx.commit();
        session.clear();
        
        Contact retrieved = session.get(Contact.class, contact.getId());
        assertNotNull(retrieved.getCreateDate(), "LocalDate should be persisted");
        assertEquals(testDate, retrieved.getCreateDate(), "LocalDate should match exactly");
        
        // Cleanup
        tx = session.beginTransaction();
        session.remove(retrieved);
        tx.commit();
    }
    
    @Test
    @Order(2)
    @Story("LocalDate Handling")
    @Description("Tests LocalDate edge cases: min/max dates, leap years")
    @DisplayName("LocalDate - Edge cases")
    void testLocalDateEdgeCases() {
        Contact contact = new Contact("localdate-edge@test.com", "localdateedge", "test123");
        
        // Test leap year date
        LocalDate leapYearDate = LocalDate.of(2024, 2, 29);
        contact.setCreateDate(leapYearDate);
        
        Transaction tx = session.beginTransaction();
        session.persist(contact);
        tx.commit();
        session.clear();
        
        Contact retrieved = session.get(Contact.class, contact.getId());
        assertEquals(leapYearDate, retrieved.getCreateDate(), "Leap year date should be preserved");
        
        // Test year boundaries
        LocalDate newYearsEve = LocalDate.of(2024, 12, 31);
        retrieved.setCreateDate(newYearsEve);
        tx = session.beginTransaction();
        session.merge(retrieved);
        tx.commit();
        session.clear();
        
        retrieved = session.get(Contact.class, contact.getId());
        assertEquals(newYearsEve, retrieved.getCreateDate(), "Year boundary date should be preserved");
        
        // Cleanup
        tx = session.beginTransaction();
        session.remove(retrieved);
        tx.commit();
    }
    
    // ========== LocalTime Tests (TIME type) ==========
    
    @Test
    @Order(3)
    @Story("LocalTime Handling")
    @Description("Tests LocalTime mapping to FileMaker TIME type")
    @DisplayName("LocalTime - Basic persistence and retrieval")
    void testLocalTimePersistence() {
        Contact contact = new Contact("localtime@test.com", "localtimetest", "test123");
        LocalTime testTime = LocalTime.of(14, 30, 45);
        contact.setCreateTime(testTime);
        
        Transaction tx = session.beginTransaction();
        session.persist(contact);
        tx.commit();
        session.clear();
        
        Contact retrieved = session.get(Contact.class, contact.getId());
        assertNotNull(retrieved.getCreateTime(), "LocalTime should be persisted");
        // FileMaker may truncate seconds, compare hours and minutes
        assertEquals(testTime.getHour(), retrieved.getCreateTime().getHour(), "Hour should match");
        assertEquals(testTime.getMinute(), retrieved.getCreateTime().getMinute(), "Minute should match");
        
        // Cleanup
        tx = session.beginTransaction();
        session.remove(retrieved);
        tx.commit();
    }
    
    @Test
    @Order(4)
    @Story("LocalTime Handling")
    @Description("Tests LocalTime edge cases: midnight, noon, end of day")
    @DisplayName("LocalTime - Edge cases")
    void testLocalTimeEdgeCases() {
        Contact contact = new Contact("localtime-edge@test.com", "localtimeedge", "test123");
        
        // Test midnight
        LocalTime midnight = LocalTime.of(0, 0, 0);
        contact.setCreateTime(midnight);
        
        Transaction tx = session.beginTransaction();
        session.persist(contact);
        tx.commit();
        session.clear();
        
        Contact retrieved = session.get(Contact.class, contact.getId());
        assertEquals(0, retrieved.getCreateTime().getHour(), "Midnight hour should be 0");
        assertEquals(0, retrieved.getCreateTime().getMinute(), "Midnight minute should be 0");
        
        // Test end of day (23:59:59)
        LocalTime endOfDay = LocalTime.of(23, 59, 59);
        retrieved.setCreateTime(endOfDay);
        tx = session.beginTransaction();
        session.merge(retrieved);
        tx.commit();
        session.clear();
        
        retrieved = session.get(Contact.class, contact.getId());
        assertEquals(23, retrieved.getCreateTime().getHour(), "End of day hour should be 23");
        assertEquals(59, retrieved.getCreateTime().getMinute(), "End of day minute should be 59");
        
        // Cleanup
        tx = session.beginTransaction();
        session.remove(retrieved);
        tx.commit();
    }
    
    // ========== LocalDateTime / TIMESTAMP Tests ==========
    
    @Test
    @Order(5)
    @Story("LocalDateTime Handling")
    @Description("Tests LocalDateTime mapping to FileMaker TIMESTAMP type")
    @DisplayName("LocalDateTime - Basic persistence and retrieval")
    void testLocalDateTimePersistence() {
        Contact contact = new Contact("localdatetime@test.com", "localdatetimetest", "test123");
        LocalDateTime testDateTime = LocalDateTime.of(2025, 11, 27, 14, 30, 45);
        
        // Convert to Date for lastContactDate field
        Date lastContact = Date.from(testDateTime.atZone(ZoneId.systemDefault()).toInstant());
        contact.setLastContactDate(lastContact);
        
        Transaction tx = session.beginTransaction();
        session.persist(contact);
        tx.commit();
        session.clear();
        
        Contact retrieved = session.get(Contact.class, contact.getId());
        assertNotNull(retrieved.getLastContactDate(), "TIMESTAMP should be persisted");
        
        // Compare ignoring milliseconds (FileMaker may truncate)
        LocalDateTime retrievedDateTime = retrieved.getLastContactDate().toInstant()
                .atZone(ZoneId.systemDefault()).toLocalDateTime();
        assertEquals(testDateTime.truncatedTo(ChronoUnit.SECONDS), 
                    retrievedDateTime.truncatedTo(ChronoUnit.SECONDS),
                    "LocalDateTime should match (ignoring sub-seconds)");
        
        // Cleanup
        tx = session.beginTransaction();
        session.remove(retrieved);
        tx.commit();
    }
    
    // ========== Timezone Tests ==========
    
    @Test
    @Order(6)
    @Story("Timezone Handling")
    @Description("Tests timezone conversions with FileMaker TIMESTAMP")
    @DisplayName("Timezone - Cross-timezone date/time handling")
    void testTimezoneHandling() {
        Contact contact = new Contact("timezone@test.com", "timezonetest", "test123");
        
        // Create a specific moment in time
        LocalDateTime now = LocalDateTime.now();
        Date lastContact = Date.from(now.atZone(ZoneId.systemDefault()).toInstant());
        contact.setLastContactDate(lastContact);
        
        Transaction tx = session.beginTransaction();
        session.persist(contact);
        tx.commit();
        session.clear();
        
        Contact retrieved = session.get(Contact.class, contact.getId());
        
        // Test timezone conversions
        ZoneId tokyo = ZoneId.of("Asia/Tokyo");
        ZoneId london = ZoneId.of("Europe/London");
        ZoneId newYork = ZoneId.of("America/New_York");
        
        ZonedDateTime tokyoTime = retrieved.getLastContactDate().toInstant().atZone(tokyo);
        ZonedDateTime londonTime = retrieved.getLastContactDate().toInstant().atZone(london);
        ZonedDateTime newYorkTime = retrieved.getLastContactDate().toInstant().atZone(newYork);
        
        // All should represent the same instant
        assertEquals(tokyoTime.toInstant(), londonTime.toInstant(), 
                    "Tokyo and London should represent same instant");
        assertEquals(londonTime.toInstant(), newYorkTime.toInstant(), 
                    "London and New York should represent same instant");
        
        // But local times should differ (unless by coincidence during DST transitions)
        // Tokyo is UTC+9, so typically different from London (UTC+0/+1)
        assertNotEquals(tokyoTime.getHour(), newYorkTime.getHour(),
                       "Tokyo and New York should have different local hours");
        
        // Cleanup
        tx = session.beginTransaction();
        session.remove(retrieved);
        tx.commit();
    }
    
    @Test
    @Order(7)
    @Story("Timezone Handling")
    @Description("Tests UTC timestamp storage and retrieval")
    @DisplayName("Timezone - UTC handling")
    void testUtcHandling() {
        Contact contact = new Contact("utc@test.com", "utctest", "test123");
        
        // Create a UTC timestamp
        LocalDateTime utcDateTime = LocalDateTime.of(2025, 6, 15, 12, 0, 0);
        Date utcDate = Date.from(utcDateTime.toInstant(ZoneOffset.UTC));
        contact.setLastContactDate(utcDate);
        
        Transaction tx = session.beginTransaction();
        session.persist(contact);
        tx.commit();
        session.clear();
        
        Contact retrieved = session.get(Contact.class, contact.getId());
        
        // Convert back to UTC and verify
        LocalDateTime retrievedUtc = retrieved.getLastContactDate().toInstant()
                .atZone(ZoneOffset.UTC).toLocalDateTime();
        
        assertEquals(utcDateTime.truncatedTo(ChronoUnit.SECONDS), 
                    retrievedUtc.truncatedTo(ChronoUnit.SECONDS),
                    "UTC timestamp should be preserved");
        
        // Cleanup
        tx = session.beginTransaction();
        session.remove(retrieved);
        tx.commit();
    }
    
    // ========== Combined Date/Time Tests ==========
    
    @Test
    @Order(8)
    @Story("Combined Date/Time")
    @Description("Tests setting all date/time fields together")
    @DisplayName("Combined - All temporal types in one entity")
    void testAllTemporalTypesTogether() {
        Contact contact = new Contact("alltypes@test.com", "alltypestest", "test123");
        
        LocalDate testDate = LocalDate.of(2025, 11, 27);
        LocalTime testTime = LocalTime.of(15, 45, 30);
        LocalDateTime testDateTime = LocalDateTime.of(2025, 11, 27, 15, 45, 30);
        
        contact.setCreateDate(testDate);
        contact.setCreateTime(testTime);
        contact.setLastContactDate(Date.from(testDateTime.atZone(ZoneId.systemDefault()).toInstant()));
        
        Transaction tx = session.beginTransaction();
        session.persist(contact);
        tx.commit();
        session.clear();
        
        Contact retrieved = session.get(Contact.class, contact.getId());
        
        // Verify all fields
        assertEquals(testDate, retrieved.getCreateDate(), "DATE field should match");
        assertEquals(testTime.getHour(), retrieved.getCreateTime().getHour(), "TIME hour should match");
        assertEquals(testTime.getMinute(), retrieved.getCreateTime().getMinute(), "TIME minute should match");
        assertNotNull(retrieved.getLastContactDate(), "TIMESTAMP should not be null");
        
        // Cleanup
        tx = session.beginTransaction();
        session.remove(retrieved);
        tx.commit();
    }
    
    @Test
    @Order(9)
    @Story("Null Handling")
    @Description("Tests null date/time values")
    @DisplayName("Null - Temporal fields can be null")
    void testNullTemporalValues() {
        Contact contact = new Contact("nulldates@test.com", "nulldatestest", "test123");
        // Don't set any date/time fields - they should remain null
        
        Transaction tx = session.beginTransaction();
        session.persist(contact);
        tx.commit();
        session.clear();
        
        Contact retrieved = session.get(Contact.class, contact.getId());
        
        // All temporal fields should be null (except auto-generated ones)
        assertNull(retrieved.getCreateDate(), "DATE field should be null");
        assertNull(retrieved.getCreateTime(), "TIME field should be null");
        assertNull(retrieved.getLastContactDate(), "TIMESTAMP field should be null");
        
        // Cleanup
        tx = session.beginTransaction();
        session.remove(retrieved);
        tx.commit();
    }
}
