package org.hibernate.community.dialect.test;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import io.qameta.allure.Description;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Step;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class GenerateRandomDataTest {
    private static final String[] FIRST_NAMES = {
        "James", "Mary", "John", "Patricia", "Robert", "Jennifer", "Michael", "Linda",
        "William", "Elizabeth", "David", "Barbara", "Richard", "Susan", "Joseph", "Jessica"
    };
    
    private static final String[] LAST_NAMES = {
        "Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia", "Miller", "Davis",
        "Rodriguez", "Martinez", "Hernandez", "Lopez", "Gonzalez", "Wilson", "Anderson"
    };
    
    private static final String[] COMPANIES = {
        "Tech Solutions", "Global Systems", "Data Corp", "Smart Software", "Cloud Nine",
        "Digital Dreams", "Cyber Systems", "Future Tech", "Innovation Labs", "Code Masters"
    };
    
    private static final String[] JOB_TITLES = {
        "Software Engineer", "Project Manager", "Data Analyst", "System Architect",
        "Product Owner", "DevOps Engineer", "QA Engineer", "Business Analyst",
        "Technical Lead", "Full Stack Developer"
    };
    
    private static final String[] DOMAINS = {
        "example.com", "testmail.com", "company.net", "enterprise.org", "techmail.io"
    };

    @Test
    @DisplayName("Generate Random Test Data")
    @Description("Generates 100 random contact records for testing")
    @Severity(SeverityLevel.NORMAL)
    void generateRandomData() {
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction transaction = null;
        
        try {
            transaction = session.beginTransaction();
            
            // Generate 100 random contacts
            for (int i = 0; i < 100; i++) {
                Contact contact = createRandomContact();
                session.persist(contact);
                
                // Flush every 20 records to avoid memory issues
                if (i % 20 == 0) {
                    session.flush();
                    session.clear();
                }
            }
            
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }
            throw e;
        } finally {
            session.close();
        }
    }
    
    @Step("Create random contact")
    private Contact createRandomContact() {
        Random random = new Random();
        String firstName = FIRST_NAMES[random.nextInt(FIRST_NAMES.length)];
        String lastName = LAST_NAMES[random.nextInt(LAST_NAMES.length)];
        String company = COMPANIES[random.nextInt(COMPANIES.length)];
        String jobTitle = JOB_TITLES[random.nextInt(JOB_TITLES.length)];
        String domain = DOMAINS[random.nextInt(DOMAINS.length)];
        
        Contact contact = new Contact();
        contact.setFirstName(firstName);
        contact.setLastName(lastName);
        contact.setEmail(generateEmail(firstName, lastName, domain));
        contact.setLogin(generateLogin(firstName, lastName));
        contact.setPassword(UUID.randomUUID().toString().substring(0, 12));
        contact.setCompany(company);
        contact.setJobTitle(jobTitle);
        contact.setTitle(random.nextBoolean() ? "Mr." : "Ms.");
        contact.setWebsite("https://www." + domain);
        contact.setNotes("Test contact generated on " + LocalDateTime.now());
        contact.setLastContactDate(generateRandomDate());
        contact.setPhotoUrl("https://example.com/photos/" + UUID.randomUUID() + ".jpg");
        contact.setPhotoContentType("image/jpeg");
        
        return contact;
    }
    
    private String generateEmail(String firstName, String lastName, String domain) {
        return (firstName.toLowerCase() + "." + lastName.toLowerCase() + "@" + domain)
            .replace(" ", "");
    }
    
    private String generateLogin(String firstName, String lastName) {
        return (firstName.substring(0, 1) + lastName).toLowerCase().replace(" ", "");
    }
    
    private Date generateRandomDate() {
        LocalDateTime start = LocalDateTime.now().minusYears(2);
        LocalDateTime end = LocalDateTime.now();
        long startEpochDay = start.toLocalDate().toEpochDay();
        long endEpochDay = end.toLocalDate().toEpochDay();
        long randomDay = ThreadLocalRandom.current().nextLong(startEpochDay, endEpochDay);
        
        return java.sql.Date.valueOf(java.time.LocalDate.ofEpochDay(randomDay));
    }
}
