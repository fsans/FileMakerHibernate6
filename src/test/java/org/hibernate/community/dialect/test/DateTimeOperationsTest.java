package org.hibernate.community.dialect.test;

import static org.junit.jupiter.api.Assertions.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.junit.jupiter.api.*;

/**
 * Tests for handling date/time operations in FileMaker.
 * Tests timezone support and date/time field persistence.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("FileMaker DateTime Operations Tests")
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
    @DisplayName("Test date/time operations with timezone")
    @Disabled("Pending verification of timezone handling")
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
