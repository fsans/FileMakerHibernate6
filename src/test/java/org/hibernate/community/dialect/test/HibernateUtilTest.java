package org.hibernate.community.dialect.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.mapping.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import jakarta.persistence.TypedQuery;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class HibernateUtilTest {

    private static SessionFactory sessionFactory;
    private Session session;

 

    @BeforeAll
    public static void setup() {
        sessionFactory = HibernateUtil.getSessionFactory();
        System.out.println("SessionFactory created at BeforeAll");
    }

    @AfterAll
    public static void tearDown() {
        if (sessionFactory != null)
            sessionFactory.close();
        System.out.println("SessionFactory destroyed at AfterAll");
    }

    @Disabled
    @Test
    @Order(1)
    public void testJdbcDriverVersion() {
        System.out.println("Running testJdbcDriverVersion...");
        String driverVersion = null;
        
        try {
            driverVersion = java.sql.DriverManager.getDriver("jdbc:filemaker://").getMajorVersion() + "." +
                           java.sql.DriverManager.getDriver("jdbc:filemaker://").getMinorVersion();
            
        } catch (SQLException e) {
            System.err.println("Error retrieving JDBC driver version: " + e.getMessage());
        }
        System.out.println("FMJDBC Driver Version: " + driverVersion);

    }


    @Disabled
    @Test
    @Order(2)
    public void testCreateWithPreparedStatement() {
        System.out.println("test2 - Running testCreateWithPreparedStatement...");
         // Retrieve configuration properties from Hibernate
         java.util.Properties properties = new java.util.Properties();
         properties.putAll(sessionFactory.getProperties());
         String connectionUrl = properties.getProperty("hibernate.connection.url");
         String connectionUser = "admin"; //properties.getProperty("hibernate.connection.username");
         String connectionPassword = "wakawaka"; // properties.getProperty("hibernate.connection.password");

        String insertSQL = "INSERT INTO Contact (name, email) VALUES (?, ?)";

        try (Connection connection = DriverManager.getConnection(connectionUrl, connectionUser, connectionPassword);
             PreparedStatement preparedStatement = connection.prepareStatement(insertSQL)) {
             
            preparedStatement.setString(1, "john");
            preparedStatement.setString(2, "john@example.com");
            int rowsAffected = preparedStatement.executeUpdate();
            Assertions.assertEquals(1, rowsAffected, "One row should be inserted");
            System.out.println("test2 - done testCreateWithPreparedStatement...: " + rowsAffected);
        } catch (SQLException e) {
            System.err.println("test2 - Error executing prepared statement: " + e.getMessage());
        }
    }

    @Disabled
    @Test
    @Order(3)
    public void testSelectWithPreparedStatement() {
        System.out.println("test3 - Running testSelectWithPreparedStatement...");
         // Retrieve configuration properties from Hibernate
         java.util.Properties properties = new java.util.Properties();
         properties.putAll(sessionFactory.getProperties());
         String connectionUrl = properties.getProperty("hibernate.connection.url");
         String connectionUser = "admin"; 
         String connectionPassword = "wakawaka"; 

        String selectSQL = "SELECT * FROM Contact WHERE email=?";

        try (Connection connection = DriverManager.getConnection(connectionUrl, connectionUser, connectionPassword);
             PreparedStatement preparedStatement = connection.prepareStatement(selectSQL)) {
             
            preparedStatement.setString(1, "john@example.com");
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                List<Contact> rowsAffected = new ArrayList<>();
                while (resultSet.next()) {
                    Contact contact = new Contact(resultSet.getString("name"), resultSet.getString("email"));
                    rowsAffected.add(contact);
                }
                Assertions.assertNotEquals(0, rowsAffected.size(), "almost one row should be retrieved");
                System.out.println("test3 - done testSelectWithPreparedStatement...: " + rowsAffected.size());
            }
        } catch (SQLException e) {
            System.err.println("test3 - Error executing prepared statement: " + e.getMessage());
        }
    }




    @Disabled
    @Test
    @Order(4)
    public void testDirectCreate(){
        System.out.println("test4 - Running testDirectCreate...");
        Assertions.assertNotNull(session, "Session should not be null");
        Contact contact = new Contact("smithww", "smith@example.com");
        session.beginTransaction();
        try {
            session.persist(contact);
            System.out.println("test4 - done testDirectCreate...: " );
        } catch (Exception e) {
            System.err.println("test4 - Error persisting contact: " + e.getMessage());
        }
    }


    //@Disabled
    @Test
    @Order(5)
    public void testCreate() {
        System.out.println("test5 - testCreate");

        Assertions.assertNotNull(session, "Session should not be null");
        session.beginTransaction();
        Contact contact = new Contact("abc", "abc@example.com");

        try {
            session.persist(contact);
        } catch (Exception e) {
            System.err.println("test5 - Error persisting contact: " + e.getMessage());
        }

        session.getTransaction().commit();
        Long id = contact.getId();
        Assertions.assertTrue(id > 0);
        System.out.println("test5 - id:" + contact.getId());
    }

    //@Disabled
    @Test
    @Order(6)
    public void testUpdate() {
        System.out.println("test6 - testUpdate");

        Long id = 5L;
        Contact contact = new Contact();
        contact.setId(id);
        contact.setName("pepe");
        contact.setEmail("smith@example.com");
        
        session.beginTransaction();
        session.merge(contact);
        session.getTransaction().commit();
        Contact updatedContact = session.find(Contact.class, id);

        assertEquals("pepe", updatedContact.getName());

    }

    //@Disabled
    @Test
    @Order(7)
    public void testGet() {
        System.out.println("test7 - testGet by ID");
        Long id = 5L;
        Contact contact = session.find(Contact.class, id);
        assertEquals("pepe", contact.getName());
    }

    //@Disabled
    @Test
    @Order(8)
    public void testList() {
        System.out.println("test8 - testList");
        TypedQuery<Contact> query = session.createQuery("from Contact", Contact.class);
        List<Contact> resultList = query.getResultList();
        Assertions.assertFalse(resultList.isEmpty());
        System.out.println("test4 - got results: " + resultList.size() );
    }

    @Disabled
    @Test
    @Order(9)
    public void testDelete() {
        System.out.println("test9 - testDelete");

        Long id = 404L;
        Contact contact = session.find(Contact.class, id);
        session.beginTransaction();
        session.remove(contact);
        session.getTransaction().commit();
        Contact deletedContact = session.find(Contact.class, id);

        Assertions.assertNull(deletedContact);
    }

   

    @BeforeEach
    public void openSession() {
        session = sessionFactory.openSession();
        System.out.println("Session created");
    }

    @AfterEach
    public void closeSession() {
        if (session != null)
            session.close();
        System.out.println("Session closed\n");
    }

   
}
