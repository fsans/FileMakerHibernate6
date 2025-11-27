# FileMaker Hibernate Dialect Implementation Notes

This document describes the limitations, driver bugs, documentation discrepancies, and Hibernate-specific constraints that affect the FileMaker Dialect implementation for Hibernate 6.5+.

---

## Tested Environment

This implementation has been developed and tested with:

| Component | Version |
|-----------|---------|
| **FileMaker Server** | 2025 (22.0.3) |
| **FileMaker JDBC Driver** | fmjdbc 21.0.2 |
| **Hibernate ORM** | 6.5.x / 6.6.x |
| **Java** | 17+ |

> **New Driver Available**: FileMaker Server 2025 (22.x) ships with a newer JDBC driver that has not yet been explored or tested with this dialect. The driver is provided with the FileMaker Server installer package. Future versions of this dialect may need updates to support any changes in the new driver.

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
8. [Known Workarounds](#known-workarounds)

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

**Workaround**: The dialect uses `SELECT MAX(id)` strategy to retrieve the last inserted ID. This has concurrency implications in high-load scenarios.

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

The dialect uses `SelectGenerator` with `SELECT MAX(id)`:

```java
@Override
public String getSelectSequenceNextValString(String sequenceName) {
    return "select max(" + sequenceName + ") from " + sequenceName;
}
```

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

The `SELECT MAX(id)` approach has race condition potential in high-concurrency scenarios. For production systems with heavy concurrent inserts, consider:

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
| FileMaker JDBC Driver | 19.x, 20.x, 21.x |
| FileMaker Server | 19, 20, 21, 2023, 2024 |
| Java | 17+ |
| Apache DBCP2 | 2.9.x, 2.11.x |

---

## Reporting Issues

When reporting issues with this dialect:

1. Include FileMaker Server version
2. Include JDBC driver version (from JAR manifest)
3. Include Hibernate version
4. Provide the exact SQL being generated (enable `hibernate.show_sql=true`)
5. Include the full stack trace

For FileMaker JDBC driver bugs, report to Claris through their official support channels.
