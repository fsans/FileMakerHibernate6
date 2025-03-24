package org.hibernate.community.dialect.test;

import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
 
public class HibernateUtil {
    public static SessionFactory getSessionFactory() {
        final StandardServiceRegistry registry = new StandardServiceRegistryBuilder()
                .configure("hibernate.cfg.xml") // configures settings from /src/main/resources/hibernate.cfg.xml
                .build();
         
        SessionFactory factory = new MetadataSources(registry)
                .buildMetadata().buildSessionFactory();
         
        return factory;
    }
}
