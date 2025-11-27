package org.hibernate.community.dialect.test;

import io.qameta.allure.*;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.io.InputStream;
import java.sql.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for FileMaker container field (binary data) handling.
 * <p>
 * FileMaker container fields require special SQL syntax:
 * <ul>
 *   <li><b>Read:</b> {@code SELECT GetAs(field, 'format') FROM table WHERE id = ?}</li>
 *   <li><b>Write:</b> {@code UPDATE table SET field = ? AS 'filename.ext' WHERE id = ?}</li>
 * </ul>
 * <p>
 * Standard JPA/Hibernate @Lob mapping does NOT work with FileMaker containers.
 * These tests verify the native JDBC approach.
 */
@Epic("FileMaker Hibernate Dialect")
@Feature("Container Field Support")
@DisplayName("Container Field (Binary Data) Tests")
public class ContainerFieldTest {

    private static SessionFactory sessionFactory;
    private static final String TEST_IMAGE_PATH = "/chemical.png";
    private static final String TEST_PDF_PATH = "/fm17_cert.pdf";

    @BeforeAll
    static void setUp() {
        Configuration configuration = new Configuration();
        configuration.configure("hibernate.cfg.xml");
        sessionFactory = configuration.buildSessionFactory();
    }

    @AfterAll
    static void tearDown() {
        if (sessionFactory != null) {
            sessionFactory.close();
        }
    }

    @Test
    @Story("Upload binary data to container field")
    @Description("Test uploading a PNG image to a FileMaker container field using native SQL")
    @Severity(SeverityLevel.CRITICAL)
    void testUploadToContainerField() throws IOException {
        // Load test image from resources
        byte[] imageData;
        try (InputStream is = getClass().getResourceAsStream(TEST_IMAGE_PATH)) {
            assertNotNull(is, "Test image not found: " + TEST_IMAGE_PATH);
            imageData = is.readAllBytes();
        }
        assertTrue(imageData.length > 0, "Test image should not be empty");

        try (Session session = sessionFactory.openSession()) {
            session.beginTransaction();

            // Create a test contact
            Contact contact = new Contact("container-test@test.com", "containertest", "test123");
            contact.setFirstName("Container");
            contact.setLastName("Test");
            contact.setPhotoContentType("image/png");
            session.persist(contact);
            session.getTransaction().commit();

            Long contactId = contact.getId();
            assertNotNull(contactId, "Contact ID should be assigned");

            // Upload image using native SQL with FileMaker syntax
            session.doWork(connection -> {
                String sql = "UPDATE contact SET photo_content = ? AS 'chemical.png' WHERE id = ?";
                try (PreparedStatement ps = connection.prepareStatement(sql)) {
                    ps.setBytes(1, imageData);
                    ps.setLong(2, contactId);
                    int updated = ps.executeUpdate();
                    assertEquals(1, updated, "Should update exactly one row");
                }
            });

            // Verify upload by downloading
            // FileMaker uses 4-character type codes: PNGf for PNG, JPEG for JPEG
            session.doWork(connection -> {
                String sql = "SELECT GetAs(photo_content, 'PNGf') FROM contact WHERE id = ?";
                try (PreparedStatement ps = connection.prepareStatement(sql)) {
                    ps.setLong(1, contactId);
                    try (ResultSet rs = ps.executeQuery()) {
                        assertTrue(rs.next(), "Should find the record");
                        byte[] downloaded = rs.getBytes(1);
                        assertNotNull(downloaded, "Downloaded data should not be null");
                        assertEquals(imageData.length, downloaded.length, 
                                "Downloaded size should match uploaded size");
                    }
                }
            });

            // Cleanup
            session.beginTransaction();
            session.remove(session.get(Contact.class, contactId));
            session.getTransaction().commit();
        }
    }

    @Test
    @Story("Download binary data from container field")
    @Description("Test downloading binary data using GetAs() function")
    @Severity(SeverityLevel.CRITICAL)
    void testDownloadFromContainerField() throws IOException {
        byte[] imageData;
        try (InputStream is = getClass().getResourceAsStream(TEST_IMAGE_PATH)) {
            assertNotNull(is, "Test image not found: " + TEST_IMAGE_PATH);
            imageData = is.readAllBytes();
        }

        try (Session session = sessionFactory.openSession()) {
            session.beginTransaction();

            // Create and upload
            Contact contact = new Contact("download-test@test.com", "downloadtest", "test123");
            contact.setFirstName("Download");
            contact.setLastName("Test");
            session.persist(contact);
            session.getTransaction().commit();

            Long contactId = contact.getId();

            // Upload
            session.doWork(connection -> {
                String sql = "UPDATE contact SET photo_content = ? AS 'chemical.png' WHERE id = ?";
                try (PreparedStatement ps = connection.prepareStatement(sql)) {
                    ps.setBytes(1, imageData);
                    ps.setLong(2, contactId);
                    ps.executeUpdate();
                }
            });

            // Test different GetAs formats
            // FileMaker uses 4-character type codes (classic Mac OS style):
            // - GIFf: Graphics Interchange Format
            // - JPEG: Photographic images
            // - TIFF: Raster file format for digital images
            // - PDF : Portable Document Format (note trailing space)
            // - PNGf: Bitmap image format (PNG)
            // Note: FILE returns NULL for typed content
            session.doWork(connection -> {
                // Test PNGf format (correct code for PNG)
                String sqlPng = "SELECT GetAs(photo_content, 'PNGf') FROM contact WHERE id = ?";
                try (PreparedStatement ps = connection.prepareStatement(sqlPng)) {
                    ps.setLong(1, contactId);
                    try (ResultSet rs = ps.executeQuery()) {
                        assertTrue(rs.next());
                        byte[] data = rs.getBytes(1);
                        assertNotNull(data, "PNGf format should return data");
                        assertTrue(data.length > 0, "PNGf data should not be empty");
                    }
                }
            });

            // Cleanup
            session.beginTransaction();
            session.remove(session.get(Contact.class, contactId));
            session.getTransaction().commit();
        }
    }

    @Test
    @Story("Clear container field")
    @Description("Test setting a container field to NULL")
    @Severity(SeverityLevel.NORMAL)
    void testClearContainerField() throws IOException {
        byte[] imageData;
        try (InputStream is = getClass().getResourceAsStream(TEST_IMAGE_PATH)) {
            assertNotNull(is);
            imageData = is.readAllBytes();
        }

        try (Session session = sessionFactory.openSession()) {
            session.beginTransaction();

            Contact contact = new Contact("clear-test@test.com", "cleartest", "test123");
            session.persist(contact);
            session.getTransaction().commit();

            Long contactId = contact.getId();

            // Upload first
            session.doWork(connection -> {
                String sql = "UPDATE contact SET photo_content = ? AS 'chemical.png' WHERE id = ?";
                try (PreparedStatement ps = connection.prepareStatement(sql)) {
                    ps.setBytes(1, imageData);
                    ps.setLong(2, contactId);
                    ps.executeUpdate();
                }
            });

            // Verify it's there using PNGf format
            session.doWork(connection -> {
                String sql = "SELECT GetAs(photo_content, 'PNGf') FROM contact WHERE id = ?";
                try (PreparedStatement ps = connection.prepareStatement(sql)) {
                    ps.setLong(1, contactId);
                    try (ResultSet rs = ps.executeQuery()) {
                        assertTrue(rs.next());
                        assertNotNull(rs.getBytes(1), "Data should exist before clear");
                    }
                }
            });

            // Clear the container
            session.doWork(connection -> {
                String sql = "UPDATE contact SET photo_content = NULL WHERE id = ?";
                try (PreparedStatement ps = connection.prepareStatement(sql)) {
                    ps.setLong(1, contactId);
                    int updated = ps.executeUpdate();
                    assertEquals(1, updated);
                }
            });

            // Verify it's cleared
            session.doWork(connection -> {
                String sql = "SELECT GetAs(photo_content, 'PNGf') FROM contact WHERE id = ?";
                try (PreparedStatement ps = connection.prepareStatement(sql)) {
                    ps.setLong(1, contactId);
                    try (ResultSet rs = ps.executeQuery()) {
                        assertTrue(rs.next());
                        assertNull(rs.getBytes(1), "Data should be null after clear");
                    }
                }
            });

            // Cleanup
            session.beginTransaction();
            session.remove(session.get(Contact.class, contactId));
            session.getTransaction().commit();
        }
    }

    @Test
    @Story("Get container reference")
    @Description("Test retrieving file reference using CAST to VARCHAR")
    @Severity(SeverityLevel.MINOR)
    void testGetContainerReference() throws IOException {
        byte[] imageData;
        try (InputStream is = getClass().getResourceAsStream(TEST_IMAGE_PATH)) {
            assertNotNull(is);
            imageData = is.readAllBytes();
        }

        try (Session session = sessionFactory.openSession()) {
            session.beginTransaction();

            Contact contact = new Contact("ref-test@test.com", "reftest", "test123");
            session.persist(contact);
            session.getTransaction().commit();

            Long contactId = contact.getId();

            // Upload
            session.doWork(connection -> {
                String sql = "UPDATE contact SET photo_content = ? AS 'chemical.png' WHERE id = ?";
                try (PreparedStatement ps = connection.prepareStatement(sql)) {
                    ps.setBytes(1, imageData);
                    ps.setLong(2, contactId);
                    ps.executeUpdate();
                }
            });

            // Get reference
            session.doWork(connection -> {
                String sql = "SELECT CAST(photo_content AS VARCHAR) FROM contact WHERE id = ?";
                try (PreparedStatement ps = connection.prepareStatement(sql)) {
                    ps.setLong(1, contactId);
                    try (ResultSet rs = ps.executeQuery()) {
                        assertTrue(rs.next());
                        String reference = rs.getString(1);
                        // Reference might contain filename or path info
                        assertNotNull(reference, "Reference should not be null");
                        System.out.println("Container reference: " + reference);
                    }
                }
            });

            // Cleanup
            session.beginTransaction();
            session.remove(session.get(Contact.class, contactId));
            session.getTransaction().commit();
        }
    }

    @Test
    @Story("Upload and download PDF document")
    @Description("Test uploading and downloading PDF files using 'PDF ' type code (note trailing space)")
    @Severity(SeverityLevel.CRITICAL)
    void testPdfContainerField() throws IOException {
        // Load test PDF from resources
        byte[] pdfData;
        try (InputStream is = getClass().getResourceAsStream(TEST_PDF_PATH)) {
            assertNotNull(is, "Test PDF not found: " + TEST_PDF_PATH);
            pdfData = is.readAllBytes();
        }
        assertTrue(pdfData.length > 0, "Test PDF should not be empty");

        try (Session session = sessionFactory.openSession()) {
            session.beginTransaction();

            // Create a test contact
            Contact contact = new Contact("pdf-test@test.com", "pdftest", "test123");
            contact.setFirstName("PDF");
            contact.setLastName("Test");
            contact.setPhotoContentType("application/pdf");
            session.persist(contact);
            session.getTransaction().commit();

            Long contactId = contact.getId();
            assertNotNull(contactId, "Contact ID should be assigned");

            // Upload PDF using native SQL with FileMaker syntax
            session.doWork(connection -> {
                String sql = "UPDATE contact SET photo_content = ? AS 'fm17_cert.pdf' WHERE id = ?";
                try (PreparedStatement ps = connection.prepareStatement(sql)) {
                    ps.setBytes(1, pdfData);
                    ps.setLong(2, contactId);
                    int updated = ps.executeUpdate();
                    assertEquals(1, updated, "Should update exactly one row");
                }
            });

            // Verify upload by downloading with 'PDF ' format (note trailing space!)
            session.doWork(connection -> {
                // FileMaker requires 'PDF ' with trailing space for PDF documents
                String sql = "SELECT GetAs(photo_content, 'PDF ') FROM contact WHERE id = ?";
                try (PreparedStatement ps = connection.prepareStatement(sql)) {
                    ps.setLong(1, contactId);
                    try (ResultSet rs = ps.executeQuery()) {
                        assertTrue(rs.next(), "Should find the record");
                        byte[] downloaded = rs.getBytes(1);
                        assertNotNull(downloaded, "Downloaded PDF should not be null");
                        assertEquals(pdfData.length, downloaded.length, 
                                "Downloaded size should match uploaded size");
                    }
                }
            });

            // Verify reference shows filename
            session.doWork(connection -> {
                String sql = "SELECT CAST(photo_content AS VARCHAR) FROM contact WHERE id = ?";
                try (PreparedStatement ps = connection.prepareStatement(sql)) {
                    ps.setLong(1, contactId);
                    try (ResultSet rs = ps.executeQuery()) {
                        assertTrue(rs.next());
                        String reference = rs.getString(1);
                        assertNotNull(reference, "Reference should not be null");
                        assertTrue(reference.contains("pdf"), "Reference should contain pdf filename");
                        System.out.println("PDF container reference: " + reference);
                    }
                }
            });

            // Cleanup
            session.beginTransaction();
            session.remove(session.get(Contact.class, contactId));
            session.getTransaction().commit();
        }
    }

    @Test
    @Story("Verify PDF magic bytes")
    @Description("Test that uploaded PDF files have correct magic bytes (%PDF)")
    @Severity(SeverityLevel.NORMAL)
    void testPdfMagicBytes() throws IOException {
        // Load test PDF from resources
        byte[] pdfData;
        try (InputStream is = getClass().getResourceAsStream(TEST_PDF_PATH)) {
            assertNotNull(is, "Test PDF not found: " + TEST_PDF_PATH);
            pdfData = is.readAllBytes();
        }

        try (Session session = sessionFactory.openSession()) {
            session.beginTransaction();

            // Create a test contact
            Contact contact = new Contact("magic-test@test.com", "magictest", "test123");
            session.persist(contact);
            session.getTransaction().commit();

            Long contactId = contact.getId();

            // Upload PDF
            session.doWork(connection -> {
                String sql = "UPDATE contact SET photo_content = ? AS 'test.pdf' WHERE id = ?";
                try (PreparedStatement ps = connection.prepareStatement(sql)) {
                    ps.setBytes(1, pdfData);
                    ps.setLong(2, contactId);
                    ps.executeUpdate();
                }
            });

            // Download and verify magic bytes
            session.doWork(connection -> {
                String sql = "SELECT GetAs(photo_content, 'PDF ') FROM contact WHERE id = ?";
                try (PreparedStatement ps = connection.prepareStatement(sql)) {
                    ps.setLong(1, contactId);
                    try (ResultSet rs = ps.executeQuery()) {
                        assertTrue(rs.next(), "Should find the record");
                        byte[] downloaded = rs.getBytes(1);
                        assertNotNull(downloaded, "PDF data should not be null");
                        assertTrue(downloaded.length > 4, "PDF should have content");
                        
                        // Verify PDF magic bytes: %PDF
                        assertEquals('%', (char) downloaded[0], "First byte should be %");
                        assertEquals('P', (char) downloaded[1], "Second byte should be P");
                        assertEquals('D', (char) downloaded[2], "Third byte should be D");
                        assertEquals('F', (char) downloaded[3], "Fourth byte should be F");
                        System.out.println("PDF magic bytes verified: %PDF");
                    }
                }
            });

            // Cleanup
            session.beginTransaction();
            session.remove(session.get(Contact.class, contactId));
            session.getTransaction().commit();
        }
    }
}
