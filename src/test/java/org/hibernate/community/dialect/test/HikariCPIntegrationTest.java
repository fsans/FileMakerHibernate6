package org.hibernate.community.dialect.test;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

public class HikariCPIntegrationTest {

    private static HikariDataSource dataSource;
    private static SessionFactory sessionFactory;

    @BeforeAll
    static void setup() {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl("jdbc:filemaker://192.168.0.24:2399/Contacts");
        hikariConfig.setUsername("admin");
        hikariConfig.setPassword("wakawaka");
        hikariConfig.setMaximumPoolSize(10);
        hikariConfig.setMinimumIdle(5);
        hikariConfig.setIdleTimeout(30000);
        hikariConfig.setConnectionTimeout(30000);

        hikariConfig.setConnectionTestQuery("SELECT p.* FROM FileMaker_Tables p");

        dataSource = new HikariDataSource(hikariConfig);

        Properties hibernateProperties = new Properties();
        hibernateProperties.put("hibernate.connection.driver_class", "com.filemaker.jdbc.Driver");
        hibernateProperties.put("hibernate.dialect", "org.hibernate.community.dialect.FileMakerDialect");
        hibernateProperties.put("hibernate.hbm2ddl.auto", "none");
        hibernateProperties.put("hibernate.show_sql", "true");

        sessionFactory = new Configuration()
                .addProperties(hibernateProperties)
                .buildSessionFactory();
    }

    @AfterAll
    static void tearDown() {
        if (sessionFactory != null) {
            sessionFactory.close();
        }
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    @Test
    void testConnectionPool() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            assertNotNull(connection, "HikariCP should provide a valid connection");
            assertFalse(connection.isClosed(), "Connection should be open");
        }
    }

    @Test
    void testHibernateIntegration() {
        try (Session session = sessionFactory.openSession()) {
            assertNotNull(session, "Session should be opened successfully");
            assertTrue(session.isConnected(), "Session should be connected");
        }
    }
}
