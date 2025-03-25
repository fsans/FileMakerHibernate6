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
@Feature("Date/Time Operations")
@DisplayName("FileMaker Date/Time Operations Tests")
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
    
    @Test
    @Order(1)
    @Story("Timezone Handling")
    @Description("Verify that FileMaker correctly handles date/time fields with different timezones")
    @Severity(SeverityLevel.CRITICAL)
    @Issue("HIBERNATE-123")
    @DisplayName("Test date/time operations with timezone")
    //@Disabled("Pending verification of timezone handling")
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
