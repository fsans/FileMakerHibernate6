package org.hibernate.community.dialect.test;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.Query;
import org.junit.jupiter.api.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("FileMaker Dialect Integration Tests")
public class FileMakerDialectIntegrationTest {
    
    private static SessionFactory sessionFactory;
    private Session session;
    private Transaction transaction;
    
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
    }
    
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
    @DisplayName("Test Contact Creation")
    void testCreateContact() {
        Contact contact = new Contact("John Doe", "john@example.com");
        session.persist(contact);
        transaction.commit();
        
        assertNotNull(contact.getId(), "Contact ID should not be null after persist");
    }
    
    @Test
    @Order(2)
    @DisplayName("Test Contact Retrieval")
    void testRetrieveContact() {
        // First create a contact
        Contact newContact = new Contact("Jane Doe", "jane@example.com");
        session.persist(newContact);
        transaction.commit();
        
        // Clear session to force database retrieval
        session.clear();
        
        // Retrieve the contact
        Contact retrievedContact = session.get(Contact.class, newContact.getId());
        assertNotNull(retrievedContact, "Retrieved contact should not be null");
        assertEquals("Jane Doe", retrievedContact.getName());
        assertEquals("jane@example.com", retrievedContact.getEmail());
    }
    
    @Test
    @Order(3)
    @DisplayName("Test Contact Update")
    void testUpdateContact() {
        // Create a contact
        Contact contact = new Contact("Bob Smith", "bob@example.com");
        session.persist(contact);
        transaction.commit();
        
        // Start new transaction
        transaction = session.beginTransaction();
        
        // Update contact
        contact.setEmail("bob.smith@example.com");
        session.merge(contact);
        transaction.commit();
        
        // Clear session and retrieve
        session.clear();
        Contact updatedContact = session.get(Contact.class, contact.getId());
        assertEquals("bob.smith@example.com", updatedContact.getEmail());
    }
    
    @Test
    @Order(4)
    @DisplayName("Test Contact Deletion")
    void testDeleteContact() {
        // Create a contact
        Contact contact = new Contact("Alice Brown", "alice@example.com");
        session.persist(contact);
        transaction.commit();
        
        Long contactId = contact.getId();
        
        // Start new transaction
        transaction = session.beginTransaction();
        
        // Delete contact
        session.remove(contact);
        transaction.commit();
        
        // Try to retrieve deleted contact
        Contact deletedContact = session.get(Contact.class, contactId);
        assertNull(deletedContact, "Contact should be null after deletion");
    }
    
    @Test
    @Order(5)
    @DisplayName("Test Contact Query")
    void testQueryContacts() {
        // Create multiple contacts
        session.persist(new Contact("User1", "user1@example.com"));
        session.persist(new Contact("User2", "user2@example.com"));
        session.persist(new Contact("User3", "user3@example.com"));
        transaction.commit();
        
        // Start new transaction
        transaction = session.beginTransaction();
        
        // Query using HQL
        Query<Contact> query = session.createQuery(
            "FROM Contact c WHERE c.email LIKE :pattern", Contact.class);
        query.setParameter("pattern", "%@example.com");
        
        List<Contact> contacts = query.getResultList();
        assertFalse(contacts.isEmpty(), "Should find contacts");
        assertTrue(contacts.size() >= 3, "Should find at least 3 contacts");
        
        // Verify all emails end with @example.com
        contacts.forEach(c -> 
            assertTrue(c.getEmail().endsWith("@example.com"), 
                "All emails should end with @example.com"));
    }
    
    @Test
    @Order(6)
    @DisplayName("Test Transaction Rollback")
    void testTransactionRollback() {
        // Create a contact
        Contact contact = new Contact("Test User", "test@example.com");
        session.persist(contact);
        transaction.commit();
        
        Long contactId = contact.getId();
        
        // Start new transaction
        transaction = session.beginTransaction();
        
        // Update contact
        contact.setEmail("invalid");
        session.merge(contact);
        
        // Rollback transaction
        transaction.rollback();
        
        // Verify the email wasn't updated
        session.clear();
        Contact unchangedContact = session.get(Contact.class, contactId);
        assertEquals("test@example.com", unchangedContact.getEmail(),
            "Email should remain unchanged after rollback");
    }
    
    @Test
    @Order(7)
    @DisplayName("Test Batch Operations")
    void testBatchOperations() {
        // Create multiple contacts in batch
        for (int i = 1; i <= 5; i++) {
            Contact contact = new Contact(
                "Batch User " + i, 
                "batch" + i + "@example.com"
            );
            session.persist(contact);
            
            if (i % 3 == 0) { // Flush every 3 records
                session.flush();
                session.clear();
            }
        }
        
        transaction.commit();
        
        // Query to verify batch insert
        Query<Long> countQuery = session.createQuery(
            "SELECT COUNT(c) FROM Contact c WHERE c.name LIKE :pattern",
            Long.class
        );
        countQuery.setParameter("pattern", "Batch User%");
        
        Long count = countQuery.getSingleResult();
        assertEquals(5L, count, "Should have inserted 5 batch contacts");
    }
}