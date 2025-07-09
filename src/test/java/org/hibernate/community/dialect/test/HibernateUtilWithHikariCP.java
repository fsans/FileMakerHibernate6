package org.hibernate.community.dialect.test;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.community.dialect.test.Contact; // Ensure import of the Contact class
import java.util.Properties;

public class HibernateUtilWithHikariCP {

    private static final SessionFactory sessionFactory;
    private static final HikariDataSource dataSource;

    static {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl("jdbc:filemaker://192.168.0.24:2399/Contacts");
        hikariConfig.setUsername("admin");
        hikariConfig.setPassword("wakawaka");
        hikariConfig.setMaximumPoolSize(10);
        hikariConfig.setMinimumIdle(5);
        hikariConfig.setIdleTimeout(30000);
        hikariConfig.setConnectionTimeout(30000);

        // Correctly configure the connection test query
        hikariConfig.setConnectionTestQuery("SELECT p.* FROM FileMaker_Tables p");

        dataSource = new HikariDataSource(hikariConfig);

        Properties hibernateProperties = new Properties();

        // Ensure correct Hibernate properties are set
        hibernateProperties.put("hibernate.connection.datasource", dataSource);
        hibernateProperties.put("hibernate.dialect", "org.hibernate.community.dialect.FileMakerDialect");
        hibernateProperties.put("hibernate.hbm2ddl.auto", "none");
        hibernateProperties.put("hibernate.show_sql", "true");

        sessionFactory = new Configuration()
                .addAnnotatedClass(Contact.class) // Add the Contact entity to session factory
                .addProperties(hibernateProperties)
                .buildSessionFactory();
    }

    public static SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    public static void close() {
        if (sessionFactory != null) {
            sessionFactory.close();
        }
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
}