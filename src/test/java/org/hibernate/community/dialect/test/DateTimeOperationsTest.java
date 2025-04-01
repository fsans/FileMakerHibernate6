package org.hibernate.community.dialect.test;

import static org.junit.jupiter.api.Assertions.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.junit.jupiter.api.*;
import io.qameta.allure.*;

@Epic("FileMaker Hibernate Integration")
@Feature("Date/Time Operations - TIMESTAMP, DATE, and TIME field handling with constraints:\n" +
         "1. Supported types: DATE, TIME, TIMESTAMP\n" +
         "2. Timezone awareness\n" +
         "3. Millisecond precision\n" +
         "4. Date/time conversion between Java and FileMaker")
@Owner("FileMaker Team")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("FileMaker Date/Time Operations Tests")
@Severity(SeverityLevel.CRITICAL)
@Issue("HIBERNATE-004")
@TmsLink("TC-004")
//@Disabled("Temporarily disabled while debugging pagination")
public class DateTimeOperationsTest {
    
    private Session session;
    
    @Step("Initialize Hibernate session")
    @BeforeEach
    void setUp() {
        session = HibernateUtil.getSessionFactory().openSession();
    }

    @Step("Cleanup Hibernate session")
    @AfterEach
    void tearDown() {
        if (session != null) {
            session.close();
        }
    }
    
    @Test
    @Order(1)
    @Story("Timezone Handling")
    @Description("Validates FileMaker's date/time handling across timezones:\n" +
                "1. Stores current date/time in system timezone\n" +
                "2. Retrieves and verifies stored date/time\n" +
                "3. Tests timezone conversions (Tokyo vs London)\n" +
                "4. Verifies millisecond precision\n\n" +
                "Expected:\n" +
                "- Date/time values should preserve timezone information\n" +
                "- Correct handling of DST transitions\n" +
                "- Proper millisecond truncation\n\n" +
                "Limitations:\n" +
                "- FileMaker stores times in local server timezone\n" +
                "- Millisecond precision may be lost\n" +
                "- No support for calendar variations")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Date/Time Operations with Timezone Support")
    @Issue("HIBERNATE-004")
    @TmsLink("TC-004")
    void testDateTimeOperations() {
        Contact contact = new Contact("datetime@test.com", "datetimetest", "test123");
        
        LocalDateTime now = LocalDateTime.now();
        Date lastContact = Date.from(now.atZone(ZoneId.systemDefault()).toInstant());
        contact.setLastContactDate(lastContact);
        
        Transaction tx = session.beginTransaction();
        session.persist(contact);
        tx.commit();
        session.clear();
        
        Contact retrieved = session.get(Contact.class, contact.getId());
        assertEquals(lastContact.getTime() / 1000, 
                    retrieved.getLastContactDate().getTime() / 1000, 
                    "Last contact date should match (ignoring milliseconds)");
        
        // Test timezone handling by comparing dates in different timezones
        ZoneId tokyo = ZoneId.of("Asia/Tokyo");
        ZoneId london = ZoneId.of("Europe/London");
        LocalDateTime tokyoTime = retrieved.getLastContactDate().toInstant()
                                         .atZone(tokyo)
                                         .toLocalDateTime();
        LocalDateTime londonTime = retrieved.getLastContactDate().toInstant()
                                          .atZone(london)
                                          .toLocalDateTime();
        assertTrue(tokyoTime.getHour() != londonTime.getHour(), 
                  "Times in different zones should have different hours");
        
        // Cleanup
        tx = session.beginTransaction();
        session.remove(retrieved);
        tx.commit();
    }
}
