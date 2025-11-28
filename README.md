# FileMaker Hibernate Dialect

[![GitHub tag](https://img.shields.io/github/v/tag/fsans/FileMakerHibernate6)](https://github.com/fsans/FileMakerHibernate6/tags)
[![Hibernate](https://img.shields.io/badge/Hibernate-6.5+-green)](https://hibernate.org/)
[![Java](https://img.shields.io/badge/Java-17+-orange)](https://openjdk.org/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

Hibernate 6.5+ dialect for FileMaker databases.

## Overview

This project provides a custom Hibernate dialect for Claris FileMaker JDBC, enabling Java applications to use Hibernate ORM seamlessly via the FileMaker JDBC driver. This replaces previous implementations for Hibernate 5 but solves compatibility issues with Hibernate 6 and provides improved functionality.

## Tested Environment

| Component | Version |
|-----------|---------|
| FileMaker Server | 2025 (22.0.3) |
| FileMaker JDBC Driver | fmjdbc 22.0.1 |
| Hibernate ORM | 6.5.x / 6.6.x |
| Java | 17+ |

## Features

- ANSI SQL pagination with `OFFSET` / `FETCH FIRST` clauses
- FileMaker-specific type mappings (VARCHAR, DOUBLE, DATE, TIME, TIMESTAMP, BLOB)
- Custom identity support using ROWID-based strategy combined with custom id/uuid generation for better compatibility
- Container field (binary data) support via native JDBC
- Extensive reserved keyword handling (~100+ FileMaker-specific keywords)
- Compatible with FileMaker Server 19, 20, 21, 2023, 2024, 2025

## Known Limitations

- **No HikariCP support** - FileMaker JDBC driver does not implement `Connection.isValid()`. Use Apache DBCP2 instead.
- **No `getGeneratedKeys()`** - Driver does not support auto-generated key retrieval
- **No `SELECT 1`** - Use `SELECT * FROM FileMaker_Tables` for connection validation
- **No DDL generation** - Schema must be managed in FileMaker Pro

See [docs/IMPLEMENTATION.md](docs/IMPLEMENTATION.md) for detailed documentation.

---

## Installation

### 1. Install the JDBC Driver

The FileMaker JDBC driver is not publicly distributed. Obtain it from your FileMaker Server installation and install to your local Maven repository:

```bash
# Place the driver JAR in src/main/resources/
cp /path/to/fmjdbc.jar ./src/main/resources/fmjdbc_22.0.1.jar

# Install to local Maven repository
./maven_deploy_driver.sh 22.0.1
```

### 2. Build and Install the Dialect

```bash
# Build the dialect
mvn package

# Install to local Maven repository
./maven_deploy_dialect.sh 21.0.2
```

### 3. Add Dependencies to Your Project

```xml
<!-- FileMaker Hibernate Dialect -->
<dependency>
    <groupId>com.filemaker.hibernate.dialect</groupId>
    <artifactId>FileMakerDialect</artifactId>
    <version>21.0.2</version>
</dependency>

<!-- FileMaker JDBC Driver -->
<dependency>
    <groupId>com.filemaker.jdbc.Driver</groupId>
    <artifactId>fmjdbc</artifactId>
    <version>21.0.2</version>
</dependency>

<!-- Apache DBCP2 (required - HikariCP does not work) -->
<dependency>
    <groupId>org.apache.commons</groupId>
    <artifactId>commons-dbcp2</artifactId>
    <version>2.11.0</version>
</dependency>
```

---

## Configuration

### Spring Boot (application.yml)

```yaml
spring:
  datasource:
    url: jdbc:filemaker://your-filemaker-host/DatabaseName
    username: your_username
    password: your_password
    driver-class-name: com.filemaker.jdbc.Driver
    # IMPORTANT: Use DBCP2, not HikariCP
    type: org.apache.commons.dbcp2.BasicDataSource
    dbcp2:
      initial-size: 1
      min-idle: 1
      max-idle: 5
      max-total: 10
      # FileMaker does not support SELECT 1
      validation-query: SELECT * FROM FileMaker_Tables FETCH FIRST 1 ROWS ONLY
      test-on-borrow: true
      test-while-idle: true
      duration-between-eviction-runs: 30s

  jpa:
    database-platform: org.hibernate.community.dialect.FileMakerDialect
    hibernate:
      ddl-auto: none  # FileMaker does not support DDL
    properties:
      hibernate:
        dialect: org.hibernate.community.dialect.FileMakerDialect
        show_sql: true
        format_sql: true
        jdbc:
          use_get_generated_keys: false
          use_scrollable_resultset: false
```

### Standalone Hibernate (hibernate.cfg.xml)

```xml
<hibernate-configuration>
  <session-factory>
    <property name="connection.url">jdbc:filemaker://your-host/DatabaseName</property>
    <property name="connection.username">admin</property>
    <property name="connection.password">password</property>
    <property name="connection.driver_class">com.filemaker.jdbc.Driver</property>
    <property name="dialect">org.hibernate.community.dialect.FileMakerDialect</property>
    <property name="hibernate.hbm2ddl.auto">none</property>
    <property name="hibernate.jdbc.use_get_generated_keys">false</property>
    <property name="hibernate.jdbc.use_scrollable_resultset">false</property>
  </session-factory>
</hibernate-configuration>
```

---

## Entity Example

### Using FileMakerBaseEntity (Recommended)

Extend `FileMakerBaseEntity` to automatically get correct ID configuration and FileMaker best practices:

```java
import org.hibernate.community.dialect.entity.FileMakerBaseEntity;

@Entity
@Table(name = "contact")
public class Contact extends FileMakerBaseEntity {
    
    @Column(name = "email")
    private String email;
    
    @Column(name = "first_name")
    private String firstName;
    
    // FileMaker stores all numbers as DOUBLE
    @Column(name = "salary")
    private Double salary;
    
    // FileMaker uses unified TIMESTAMP type
    @Column(name = "created_date")
    private LocalDateTime createdDate;
    
    // Getters and setters...
}
```

The base class provides:

- Pre-configured ID field (`insertable=false, updatable=false`)
- Optional `rowId` and `modId` for FileMaker system fields
- Proper `equals()` / `hashCode()` implementation
- `isPersisted()` utility method

### Manual Configuration

If you prefer not to use the base class:

```java
@Entity
@Table(name = "contact")
public class Contact {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", insertable = false, updatable = false)
    private Long id;
    
    // ... fields
}
```

---

## Container Fields (Binary Data)

FileMaker container fields require special SQL syntax and **cannot** use standard JPA `@Lob` mapping.

### Supported Format Codes

| Code | Format |
|------|--------|
| `GIFf` | Graphics Interchange Format |
| `JPEG` | Photographic images |
| `TIFF` | Raster file format |
| `PDF ` | Portable Document Format (trailing space required!) |
| `PNGf` | Bitmap image format (PNG) |

### SQL Syntax

```sql
-- Upload binary data
UPDATE table SET container_field = ? AS 'filename.ext' WHERE id = ?

-- Download binary data (use correct type code!)
SELECT GetAs(container_field, 'PNGf') FROM table WHERE id = ?

-- Get file reference
SELECT CAST(container_field AS VARCHAR) FROM table WHERE id = ?

-- Clear container
UPDATE table SET container_field = NULL WHERE id = ?
```

### Usage

Use native JDBC with `PreparedStatement.setBytes()` for uploads and `ResultSet.getBytes()` for downloads. See `ContainerFieldTest.java` for examples.

### Content Type Detection

When downloading, the API needs to know the format code. Detection strategy (fast → slow):

1. **`photo_content_type` field** - No extra query (fastest)
2. **File reference** - Single query via `CAST(container AS VARCHAR)`
3. **Format probing** - Tries each format until data is returned (slowest)

**Recommended:** Set up FileMaker auto-enter calculation on the content type field:

```
GetContainerAttribute ( photo_content ; "MIMEType" )
```

This ensures optimal performance by avoiding extra queries or probing.

---

## Demo Project

A complete working example using this dialect is available at:

**[filemaker-demo-api](https://github.com/fsans/filemaker-demo-api)** - Spring Boot REST API demonstrating FileMaker Hibernate integration with:

- Contact entity with CRUD operations
- Container field (photo) upload/download
- Pagination and filtering examples
- Complete configuration reference

---

## Documentation

- [IMPLEMENTATION.md](docs/IMPLEMENTATION.md) - Detailed implementation notes, limitations, and workarounds
- [DriverInfo.md](docs/DriverInfo.md) - FileMaker JDBC driver reference
- [FileMakerSQL-pagination.md](docs/FileMakerSQL-pagination.md) - Pagination syntax reference
- [FileMakerSQL-binaryData.md](docs/FileMakerSQL-binaryData.md) - Container field syntax reference

---

## License

This project is provided as-is for the FileMaker developer community