# FileMaker Hibernate Dialect Implementation Notes

This document describes the limitations, driver bugs, documentation discrepancies, and Hibernate-specific constraints that affect the FileMaker Dialect implementation for Hibernate 6.5+.

---

## Tested Environment

This implementation has been developed and tested with:

| Component | Version |
|-----------|---------|
| **FileMaker Server** | 2025 (22.0.1,NOV 2025) |
| **FileMaker JDBC Driver** | fmjdbc 22.0.1 |
| **Hibernate ORM** | 6.5+ |
| **Java** | 17+ |

---

> **Note**: Some FileMaker SQL documentation referenced in this project may be unofficial or community-sourced. Always verify behavior against the actual FileMaker JDBC driver and Claris official documentation when available.

---

## Table of Contents

1. [FileMaker JDBC Driver Limitations](#filemaker-jdbc-driver-limitations)
2. [FileMaker SQL vs Standard SQL](#filemaker-sql-vs-standard-sql)
3. [Documentation Discrepancies](#documentation-discrepancies)
4. [Hibernate 6.5+ Constraints](#hibernate-65-constraints)
5. [Connection Pooling: Why Not HikariCP](#connection-pooling-why-not-hikaricp)
6. [ID Generation Strategy](#id-generation-strategy)
7. [Pagination Implementation](#pagination-implementation)
8. [Container Fields (Binary Data)](#7-container-fields-require-special-syntax)
9. [Known Workarounds](#known-workarounds)

---

## FileMaker JDBC Driver Limitations

### 1. No `Connection.isValid()` Support

The FileMaker JDBC driver does not implement the `Connection.isValid(int timeout)` method introduced in JDBC 4.0. This method is used by modern connection pools to validate connections efficiently.

```text
[FileMaker][FileMaker JDBC] This method is not yet implemented.
```

> **Note**: The driver does support `Connection.isClosed()` which correctly returns `false` for open connections and `true` after `close()` is called. However, `isClosed()` only checks the local connection state and does not verify the connection is still valid with the server.

**Impact**: Connection pools that rely on `isValid()` (like HikariCP) will fail or behave unpredictably.

**Workaround**: Use connection pools that support `validationQuery` instead (see [Connection Pooling](#connection-pooling-why-not-hikaricp)).

> **Important**: FileMaker does not support `SELECT 1` which is the standard validation query used by most databases. You must use a query against a system table instead:
>
> ```sql
> SELECT * FROM FileMaker_Tables FETCH FIRST 1 ROWS ONLY
> ```
>
> Or without pagination:
>
> ```sql
> SELECT * FROM FileMaker_Tables
> ```

### 2. No `getGeneratedKeys()` Support

The FileMaker JDBC driver does not support `Statement.getGeneratedKeys()` or `Statement.RETURN_GENERATED_KEYS`.

**Impact**: Hibernate cannot retrieve auto-generated primary keys after INSERT operations using the standard JDBC mechanism.

**Workaround**: The dialect uses a ROWID-based strategy to retrieve the last inserted ID:

```sql
SELECT id FROM table WHERE ROWID = (SELECT MAX(ROWID) FROM table)
```

This is safer than `SELECT MAX(id)` because FileMaker's ROWID:

- Is system-managed and cannot be altered by users
- Always increases and never resets (even after record deletion)
- Is guaranteed unique within the table

### 3. No Scrollable ResultSets

The driver does not support scrollable result sets (`ResultSet.TYPE_SCROLL_INSENSITIVE` or `ResultSet.TYPE_SCROLL_SENSITIVE`).

**Impact**: Certain Hibernate features that rely on scrollable cursors are unavailable.

**Configuration**:

```properties
hibernate.jdbc.use_scrollable_resultset=false
```

### 4. Limited Batch Operations

Batch INSERT/UPDATE operations have limited support and may not provide performance benefits.

**Configuration**:

```properties
hibernate.jdbc.batch_size=1
```

### 5. Index Out of Bounds for OFFSET Beyond Result Set

When using `OFFSET n ROWS` where `n` exceeds the total number of rows, the driver throws:

```text
[FileMaker][FileMaker JDBC] Index X out of bounds for length Y
```

**Expected behavior** (per documentation): Should return an empty result set.

**Status**: Driver bug. See [Documentation Discrepancies](#documentation-discrepancies).

### 6. FETCH FIRST 0 ROWS Rejected

Using `FETCH FIRST 0 ROWS ONLY` throws:

```text
FQL0052: The fetch count in FETCH clause is not valid.
```

**Expected behavior** (per documentation): Should return an empty result set.

**Status**: Driver bug. See [Documentation Discrepancies](#documentation-discrepancies).

### 7. Container Fields Require Special Syntax

FileMaker container fields (BLOB) cannot be accessed using standard JDBC blob methods. They require special SQL syntax with `GetAs()` for retrieval and `? AS 'filename'` for storage.

**Impact**: Standard JPA `@Lob` mapping does NOT work with FileMaker containers.

**Workaround**: Use native JDBC queries with the following syntax:

```sql
-- Upload: Use PreparedStatement.setBytes() with AS 'filename' syntax
UPDATE table SET container_field = ? AS 'filename.ext' WHERE id = ?

-- Download: Use GetAs() with 4-character type code
SELECT GetAs(container_field, 'PNGf') FROM table WHERE id = ?

-- Get file reference
SELECT CAST(container_field AS VARCHAR) FROM table WHERE id = ?
```

**Supported Format Codes** (classic Mac OS style, case-sensitive):

| Code | Format |
|------|--------|
| `GIFf` | Graphics Interchange Format |
| `JPEG` | Photographic images |
| `TIFF` | Raster file format for digital images |
| `PDF ` | Portable Document Format (trailing space required!) |
| `PNGf` | Bitmap image format (PNG) |

> **Important**: The `FILE` format returns NULL for typed content. Always use the specific type code matching the stored data format.

**Content Type Detection Strategy:**

When the format is unknown, the API uses a fallback strategy (fast → slow):

1. **Check stored content type** - Read from a separate field (e.g., `photo_content_type`)
2. **Check file reference** - Parse filename from `CAST(container AS VARCHAR)`
3. **Probe formats** - Try each format code until data is returned

**Recommended:** Configure FileMaker auto-enter calculation on the content type field:

```
GetContainerAttribute ( container_field ; "MIMEType" )
```

This returns MIME types like `image/png`, `application/pdf`, etc., enabling fast detection without extra queries.

---

## FileMaker SQL vs Standard SQL

FileMaker uses FQL (FileMaker Query Language) which is SQL-like but has significant differences:

### Supported Features

- Basic SELECT, INSERT, UPDATE, DELETE
- OFFSET and FETCH FIRST for pagination
- ORDER BY, GROUP BY, HAVING
- Aggregate functions (COUNT, SUM, AVG, MIN, MAX)
- LIKE with wildcards
- DISTINCT

### Unsupported or Limited Features

- **Subqueries in OFFSET/FETCH**: Not supported
- **OFFSET/FETCH in UPDATE/DELETE**: Only valid in SELECT
- **Complex JOINs**: Limited support
- **Window functions**: Not supported
- **CTEs (WITH clause)**: Not supported
- **UNION/INTERSECT/EXCEPT**: Limited support

---

## Documentation Discrepancies

The following behaviors differ between FileMaker SQL documentation and actual JDBC driver behavior:

| Feature | Documentation Says | Driver Does |
|---------|-------------------|-------------|
| `OFFSET n` where n > total rows | Returns empty result set | Throws "Index out of bounds" |
| `FETCH FIRST 0 ROWS ONLY` | Returns empty result set | Throws "FQL0052: fetch count not valid" |
| `FETCH FIRST n ROWS` without ORDER BY | Retrieves arbitrary subset | Works, but Hibernate HQL rejects it |

**Reference**: See `docs/FileMakerSQL-pagination.md` for detailed pagination documentation.

> **Warning**: The pagination documentation may be from unofficial sources. These discrepancies should be reported to Claris for clarification.

---

## Hibernate 6.5+ Constraints

### 1. HQL Requires ORDER BY Before FETCH

Hibernate's HQL parser requires `ORDER BY` before `FETCH FIRST` clauses, even though FileMaker SQL allows `FETCH FIRST` without ordering.

**Example that fails in HQL**:

```java
// Hibernate rejects this - "mismatched input 'FETCH'"
session.createQuery("SELECT c FROM Contact c FETCH FIRST 5 ROWS ONLY");
```

**Workaround**: Always include `ORDER BY` when using pagination in HQL:

```java
session.createQuery("SELECT c FROM Contact c ORDER BY c.id FETCH FIRST 5 ROWS ONLY");
```

### 2. Dialect Registration

In Hibernate 6.5+, dialects are registered via `DialectResolver` SPI. The FileMaker dialect is registered in:

```text
META-INF/services/org.hibernate.engine.jdbc.dialect.spi.DialectResolver
```

### 3. Type Mappings

FileMaker has limited type support. The dialect maps types as follows:

| Java Type | FileMaker Type | Notes |
|-----------|---------------|-------|
| String | VARCHAR | No distinction between VARCHAR and TEXT |
| Integer/Long | DOUBLE | FileMaker stores all numbers as floating point |
| Date/LocalDate | TIMESTAMP | FileMaker uses unified timestamp type |
| byte[] | BLOB | Container fields |
| Boolean | INTEGER | 0/1 representation |

### 4. DDL Generation

DDL generation (`hibernate.hbm2ddl.auto`) should be set to `none` for FileMaker:
```properties
hibernate.hbm2ddl.auto=none
```

FileMaker schema is managed through FileMaker Pro, not SQL DDL statements.

---

## Connection Pooling: Why Not HikariCP

### The Problem

HikariCP is the default and recommended connection pool for Hibernate 6.x. However, it **does not work** with the FileMaker JDBC driver because:

1. **HikariCP uses `Connection.isValid()`** for connection validation
2. **FileMaker JDBC driver does not implement `isValid()`**
3. HikariCP has no fallback to `validationQuery`

When HikariCP attempts to validate a FileMaker connection:

```text
java.sql.SQLFeatureNotSupportedException: isValid() not supported
```

### The Solution: Apache DBCP2

Apache Commons DBCP2 supports the legacy `validationQuery` mechanism:

```yaml
spring:
  datasource:
    type: org.apache.commons.dbcp2.BasicDataSource
    dbcp2:
      validation-query: SELECT * FROM FileMaker_Tables FETCH FIRST 1 ROWS ONLY
      test-on-borrow: true
      test-while-idle: true
```

### Alternative: C3P0

C3P0 also supports `validationQuery` and can be used as an alternative:
```properties
hibernate.c3p0.preferredTestQuery=SELECT * FROM FileMaker_Tables FETCH FIRST 1 ROWS ONLY
```

### Validation Query for FileMaker

FileMaker does not support `SELECT 1`. Use system tables instead:
```sql
SELECT * FROM FileMaker_Tables FETCH FIRST 1 ROWS ONLY
```

---

## ID Generation Strategy

### The Challenge

FileMaker typically uses auto-increment serial numbers or UUIDs generated by FileMaker itself. The JDBC driver does not support retrieving these via `getGeneratedKeys()`.

### Current Implementation

The dialect uses FileMaker's ROWID system column to reliably identify the last inserted record:

```java
@Override
public String getIdentitySelectString(String table, String column, int type) {
    return "select " + column + " from " + table + 
           " where ROWID = (select max(ROWID) from " + table + ")";
}
```

This approach uses ROWID (which is immutable and always increasing) to find the last inserted record, then returns the user-defined ID column value.

### Entity Configuration

```java
@Entity
@Table(name = "contact")
public class Contact {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", insertable = false, updatable = false)
    private Long id;
}
```

### Concurrency Warning

While the ROWID-based approach is more reliable than `SELECT MAX(id)`, there is still a theoretical race condition in high-concurrency scenarios where multiple inserts happen simultaneously. For mission-critical applications, consider:

1. Using FileMaker-generated UUIDs as the primary identifier
2. Implementing application-level ID generation
3. Using optimistic locking to detect conflicts

---

## Pagination Implementation

### Hibernate 6.5+ LimitHandler

The dialect implements `LimitHandler` for pagination:

```java
@Override
public LimitHandler getLimitHandler() {
    return new FileMakerLimitHandler();
}
```

### SQL Generation

| HQL | Generated SQL |
|-----|---------------|
| `setFirstResult(10)` | `OFFSET 10 ROWS` |
| `setMaxResults(5)` | `FETCH FIRST 5 ROWS ONLY` |
| Both | `OFFSET 10 ROWS FETCH FIRST 5 ROWS ONLY` |

### Tested Edge Cases

| Scenario | Status | Notes |
|----------|--------|-------|
| OFFSET 0 | ✅ Works | Returns all rows |
| OFFSET > total rows | ❌ Driver bug | Throws exception instead of empty result |
| FETCH FIRST 0 | ❌ Driver bug | Throws FQL0052 error |
| FETCH FIRST 100 PERCENT | ✅ Works | Returns all rows |
| OFFSET + FETCH combined | ✅ Works | Standard pagination |
| FETCH without ORDER BY | ❌ Hibernate limitation | HQL parser rejects |

---

## Known Workarounds

### 1. Connection Validation

Use DBCP2 or C3P0 instead of HikariCP.

### 2. ID Retrieval

Configure `insertable = false, updatable = false` on ID columns and let FileMaker manage them.

### 3. Pagination Edge Cases

Avoid `OFFSET` values that exceed result set size. Always check count before paginating.

### 4. Large Text Fields

FileMaker handles large text transparently. No special CLOB handling needed.

### 5. Date/Time Handling

Use `java.time` classes (LocalDate, LocalDateTime) for best compatibility.

---

## Version Compatibility

| Component | Tested Version |
|-----------|---------------|
| Hibernate ORM | 6.5.x, 6.6.x |
| FileMaker JDBC Driver | 19.x, 20.x, 21.x, 22.0 |
| FileMaker Server | 19, 20, 21, 2023, 2024, 2025 |
| Java | 17+ |
| Apache DBCP2 | 2.9.x, 2.11.x |

> **Last tested**: November 27, 2025 with FileMaker Server 2025 (22.0.1) and JDBC driver 22.0

---

## Reporting Issues

When reporting issues with this dialect:

1. Include FileMaker Server version
2. Include JDBC driver version 
3. Include Hibernate version
4. Provide the exact SQL being generated (enable `hibernate.show_sql=true`)
5. Include the full stack trace

For FileMaker JDBC driver bugs, report to Claris through their official support channels.
