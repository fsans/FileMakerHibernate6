package org.hibernate.community.dialect.test;

import jakarta.persistence.Table;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.io.Serializable;

@Entity
@Table(name = "contact")
public class Contact implements Serializable {
  

    private static final long serialVersionUID = 1L;
    
    // use always GenerationType.IDENTITY ??
    // as a Long or Integer, An identity or autoincrement column
    // TODO: investigate use of GenerationType.UUID in FM
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String email;

    public Contact() {
    }

    public Contact(String name, String email) {
        this.name = name;
        this.email = email;
    }

    public Contact(Long id, String name, String email) {
        this.id = id;
        this.name = name;
        this.email = email;
    }

    // Getters and setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
