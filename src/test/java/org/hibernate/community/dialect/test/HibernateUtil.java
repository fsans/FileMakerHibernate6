package org.hibernate.community.dialect.test;

import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;

public class HibernateUtil {
    private static SessionFactory sessionFactory;
    
    public static SessionFactory getSessionFactory() {
        if (sessionFactory == null || sessionFactory.isClosed()) {
            try {
                final StandardServiceRegistry registry = new StandardServiceRegistryBuilder()
                        .configure("/hibernate.cfg.xml") // configures settings from src/main/resources/hibernate.cfg.xml
                        .build();
                 
                sessionFactory = new MetadataSources(registry)
                        .addAnnotatedClass(Contact.class)
                        .buildMetadata()
                        .buildSessionFactory();
            } catch (Exception e) {
                throw new RuntimeException("Failed to initialize Hibernate SessionFactory", e);
            }
        }
        return sessionFactory;
    }
    
    public static void shutdown() {
        if (sessionFactory != null && !sessionFactory.isClosed()) {
            sessionFactory.close();
            sessionFactory = null;
        }
    }
}
