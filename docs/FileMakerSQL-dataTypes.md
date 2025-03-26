## SQL Data Types in FileMaker

### Supported Data Types

FileMaker SQL supports various data types that map to FileMaker field types. These include numeric, text, date/time, and binary types.

#### Numeric Types

| SQL Type      | FileMaker Equivalent   | JDBC 2.0 Type | JDBC 3.0 Type | Java Type Code |
|--------------|----------------------|--------------|--------------|--------------|
| `NUMERIC`   | Number                | `NUMERIC`    | `NUMERIC`    | `java.sql.Types.NUMERIC` |
| `DECIMAL`   | Number                | `DECIMAL`    | `DECIMAL`    | `java.sql.Types.DECIMAL` |
| `INT`       | Number (Integer)       | `INTEGER`    | `INTEGER`    | `java.sql.Types.INTEGER` |
| `FLOAT`     | Number                 | `FLOAT`      | `FLOAT`      | `java.sql.Types.FLOAT` |

#### Text Types

| SQL Type               | FileMaker Equivalent  | JDBC 2.0 Type | JDBC 3.0 Type | Java Type Code |
|------------------------|---------------------|--------------|--------------|--------------|
| `VARCHAR(n)`          | Text                | `VARCHAR`    | `VARCHAR`    | `java.sql.Types.VARCHAR` |
| `CHARACTER VARYING(n)` | Text                | `VARCHAR`    | `VARCHAR`    | `java.sql.Types.VARCHAR` |
| `BLOB`                | Container           | `BLOB`       | `BLOB`       | `java.sql.Types.BLOB` |

#### Date and Time Types

| SQL Type       | FileMaker Equivalent  | JDBC 2.0 Type | JDBC 3.0 Type | Java Type Code |
|--------------|----------------------|--------------|--------------|--------------|
| `DATE`       | Date                  | `DATE`       | `DATE`       | `java.sql.Types.DATE` |
| `TIME`       | Time                  | `TIME`       | `TIME`       | `java.sql.Types.TIME` |
| `TIMESTAMP`  | Timestamp             | `TIMESTAMP`  | `TIMESTAMP`  | `java.sql.Types.TIMESTAMP` |

##### SQL Syntax:

```sql
SELECT DATE '2024-03-26';
SELECT TIME '14:30:00';
SELECT TIMESTAMP '2024-03-26 14:30:00';
```

- FileMaker SQL follows SQL-92 standard date/time formats.
- `DATE`, `TIME`, and `TIMESTAMP` values should always be enclosed in single quotes.
- The `TIMESTAMP` type includes both date and time components.
- FileMaker SQL does not support time zone-aware `TIMESTAMP` values.

##### Edge Cases

- **Invalid Format**: Using non-standard date formats results in an error.
- **Omitting Leading Zeros**: `DATE '2024-3-2'` is invalid; use `DATE '2024-03-02'`.
- **Strict Mode**: Ensure `Strict Data Type: 4-Digit Year Date` is not selected in FileMaker Pro for compatibility.

#### Binary Types

| SQL Type        | FileMaker Equivalent | JDBC 2.0 Type | JDBC 3.0 Type | Java Type Code |
|---------------|--------------------|--------------|--------------|--------------|
| `BLOB`        | Container          | `BLOB`       | `BLOB`       | `java.sql.Types.BLOB` |
| `VARBINARY`   | Container          | `VARBINARY`  | `VARBINARY`  | `java.sql.Types.VARBINARY` |
| `LONGVARBINARY` | Container          | `LONGVARBINARY` | `LONGVARBINARY` | `java.sql.Types.LONGVARBINARY` |
| `BINARY VARYING` | Container        | `VARBINARY`  | `VARBINARY`  | `java.sql.Types.VARBINARY` |

### Data Type Mapping

FileMaker SQL maps its internal field types to SQL standard types when used in ODBC and JDBC.

#### Example Mappings:

| FileMaker Field Type | SQL Type    |
|---------------------|------------|
| Number (Integer)    | `INT`      |
| Number (Decimal)    | `DECIMAL`  |
| Text               | `VARCHAR`  |
| Date               | `DATE`     |
| Time               | `TIME`     |
| Timestamp         | `TIMESTAMP` |
| Container         | `BLOB`      |

### Handling Data Type Conversions

FileMaker SQL allows conversion between certain types using expressions and functions.

#### Type Conversion Functions:

| Function | Description |
|---------|-------------|
| `CAST(expr AS type)` | Converts `expr` to specified `type` |
| `STRVAL(expr)` | Converts `expr` to a string |
| `NUMVAL(expr)` | Converts `expr` to a number |
| `TIMESTAMPVAL(expr)` | Converts `expr` to a timestamp |

#### Example:

```sql
SELECT CAST(salary AS VARCHAR) FROM employees;
SELECT TIMESTAMPVAL('2024-03-26 14:30:00');
```
This converts the `salary` field to text format and ensures a timestamp value is properly interpreted.

### Data Type Constraints

Certain SQL constraints apply to FileMaker data types:
- **Text Length Limitation**: `VARCHAR(n)` cannot exceed FileMaker’s text storage limits.
- **Indexing Restrictions**: `BLOB` fields cannot be indexed.
- **Sorting Limitations**: `BLOB` fields cannot be used in `ORDER BY`.
- **NULL Handling**: `NULL` values are supported but must be explicitly checked with `IS NULL`.
- **Date Validation**: Ensure date values conform to SQL-92 standard to prevent parsing errors.

### Considerations for JDBC Implementation

- **Numeric Precision**: Ensure `DECIMAL` types align with JDBC-supported precision.
- **Date Formatting**: Use `YYYY-MM-DD` format for `DATE` fields.
- **Binary Streaming**: Containers require streaming for efficient handling.
- **Time Zone Handling**: Since FileMaker does not support time zone-aware timestamps, conversions may be needed in the application layer.

### Unsupported Use Cases

- **Multi-column Indexing**: Not supported in FileMaker SQL.
- **Right Outer Joins**: Not available in FileMaker SQL.
- **Complex Data Type Nesting**: No support for structured types or arrays.

