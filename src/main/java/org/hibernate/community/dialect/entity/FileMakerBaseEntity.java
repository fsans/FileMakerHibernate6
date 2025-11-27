package org.hibernate.community.dialect.entity;

import jakarta.persistence.*;
import java.io.Serializable;

/**
 * Abstract base class for FileMaker entities.
 * 
 * <p>Extend this class to ensure your entities follow FileMaker-specific requirements
 * and best practices when using the FileMaker Hibernate Dialect.</p>
 * 
 * <h2>FileMaker Entity Requirements</h2>
 * <ul>
 *   <li><b>Primary Key:</b> FileMaker auto-generates serial IDs. The ID column must be
 *       marked as {@code insertable = false, updatable = false} to prevent Hibernate
 *       from trying to set it.</li>
 *   <li><b>Identity Strategy:</b> Use {@code GenerationType.AUTO} - the dialect uses
 *       {@code SELECT MAX(id)} as a workaround since FileMaker doesn't support
 *       {@code getGeneratedKeys()}.</li>
 *   <li><b>Numeric Types:</b> FileMaker stores all numbers as DOUBLE internally.
 *       Consider using {@code Double} for numeric fields to avoid precision issues.</li>
 *   <li><b>Date/Time:</b> FileMaker uses a unified TIMESTAMP type. Use
 *       {@code LocalDateTime} for date/time fields.</li>
 *   <li><b>Text Fields:</b> FileMaker VARCHAR has no practical length limit.
 *       Large text fields work without special configuration.</li>
 *   <li><b>Binary Data:</b> Use {@code byte[]} with {@code @Lob} for container fields.</li>
 * </ul>
 * 
 * <h2>Usage Example</h2>
 * <pre>{@code
 * @Entity
 * @Table(name = "contact")
 * public class Contact extends FileMakerBaseEntity {
 *     
 *     @Column(name = "email")
 *     private String email;
 *     
 *     @Column(name = "first_name")
 *     private String firstName;
 *     
 *     // FileMaker numbers - use Double for precision
 *     @Column(name = "salary")
 *     private Double salary;
 *     
 *     // Getters and setters...
 * }
 * }</pre>
 * 
 * <h2>FileMaker System Fields</h2>
 * <p>FileMaker provides read-only system fields that can be mapped:</p>
 * <ul>
 *   <li>{@code rowid} - Internal record ID (different from your serial ID)</li>
 *   <li>{@code modid} - Modification count</li>
 * </ul>
 * <p>These are exposed via {@link #getRowId()} and {@link #getModId()} if your
 * FileMaker table includes them as calculated fields.</p>
 * 
 * @author FileMaker Hibernate Dialect
 * @since 21.0.2
 * @see org.hibernate.community.dialect.FileMakerDialect
 */
@MappedSuperclass
public abstract class FileMakerBaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Primary key field.
     * 
     * <p>FileMaker auto-generates serial numbers. This field is configured as:</p>
     * <ul>
     *   <li>{@code insertable = false} - FileMaker assigns the ID on insert</li>
     *   <li>{@code updatable = false} - ID should never change</li>
     *   <li>{@code GenerationType.AUTO} - Uses dialect's identity support</li>
     * </ul>
     * 
     * <p><b>Important:</b> Your FileMaker table must have a serial number field
     * named "id" (or override {@link #getId()} with your column name).</p>
     */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", insertable = false, updatable = false)
    private Long id;

    /**
     * Optional: FileMaker internal row ID.
     * 
     * <p>Map this if you have a calculated field exposing {@code Get(RecordID)}.</p>
     * <p>This is FileMaker's internal record identifier, different from your serial ID.</p>
     */
    @Column(name = "rowid", insertable = false, updatable = false)
    private Long rowId;

    /**
     * Optional: FileMaker modification ID.
     * 
     * <p>Map this if you have a calculated field exposing {@code Get(RecordModificationCount)}.</p>
     * <p>Useful for optimistic locking scenarios.</p>
     */
    @Column(name = "modid", insertable = false, updatable = false)
    private Long modId;

    // ========== Getters ==========

    /**
     * Returns the entity's primary key.
     * 
     * @return the ID, or null if the entity hasn't been persisted yet
     */
    public Long getId() {
        return id;
    }

    /**
     * Returns FileMaker's internal record ID.
     * 
     * <p>Only available if your table has a calculated field for {@code Get(RecordID)}.</p>
     * 
     * @return the FileMaker row ID, or null if not mapped
     */
    public Long getRowId() {
        return rowId;
    }

    /**
     * Returns FileMaker's record modification count.
     * 
     * <p>Only available if your table has a calculated field for {@code Get(RecordModificationCount)}.</p>
     * 
     * @return the modification count, or null if not mapped
     */
    public Long getModId() {
        return modId;
    }

    // ========== Setters (protected - ID should not be set manually) ==========

    /**
     * Sets the ID. Protected to discourage manual ID assignment.
     * 
     * <p><b>Warning:</b> FileMaker manages IDs automatically. Only use this
     * for testing or special cases.</p>
     * 
     * @param id the ID to set
     */
    protected void setId(Long id) {
        this.id = id;
    }

    // ========== Object methods ==========

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FileMakerBaseEntity that = (FileMakerBaseEntity) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        // Use a constant for transient entities (id == null)
        // This ensures hashCode remains stable before and after persist
        return id != null ? id.hashCode() : 31;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{id=" + id + "}";
    }

    // ========== Utility methods ==========

    /**
     * Checks if this entity has been persisted (has an ID).
     * 
     * @return true if the entity has an ID, false otherwise
     */
    public boolean isPersisted() {
        return id != null;
    }
}
