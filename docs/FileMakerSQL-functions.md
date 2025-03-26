## SQL Functions in FileMaker

FileMaker SQL supports a variety of functions, including standard ANSI SQL functions and FileMaker-specific extensions. These functions are categorized based on their functionality.

### Aggregate Functions

Aggregate functions return a single value derived from a set of records.

| Function | Description | Example |
|----------|-------------|---------|
| `SUM(expr)` | Returns the sum of a numeric column | `SELECT SUM(salary) FROM employees;` |
| `AVG(expr)` | Returns the average of a numeric column | `SELECT AVG(salary) FROM employees;` |
| `COUNT(expr)` | Returns the number of non-null values | `SELECT COUNT(emp_id) FROM employees;` |
| `MAX(expr)` | Returns the maximum value | `SELECT MAX(salary) FROM employees;` |
| `MIN(expr)` | Returns the minimum value | `SELECT MIN(salary) FROM employees;` |

### String Functions

String manipulation functions allow for modification and formatting of text values.

| Function | Description | Example |
|----------|-------------|---------|
| `UPPER(str)` | Converts a string to uppercase | `SELECT UPPER(name) FROM employees;` |
| `LOWER(str)` | Converts a string to lowercase | `SELECT LOWER(name) FROM employees;` |
| `LTRIM(str)` | Removes leading spaces | `SELECT LTRIM(name) FROM employees;` |
| `RTRIM(str)` | Removes trailing spaces | `SELECT RTRIM(name) FROM employees;` |
| `TRIM(str)` | Removes leading and trailing spaces | `SELECT TRIM(name) FROM employees;` |
| `SUBSTRING(str, start, length)` | Extracts a substring from a string | `SELECT SUBSTRING(name, 1, 3) FROM employees;` |
| `CONCAT(str1, str2)` | Concatenates two strings | `SELECT CONCAT(first_name, last_name) FROM employees;` |

### Numeric Functions

Functions that perform mathematical operations.

| Function | Description | Example |
|----------|-------------|---------|
| `ABS(num)` | Returns the absolute value | `SELECT ABS(-100) FROM dual;` |
| `ROUND(num, precision)` | Rounds a number to a specified decimal place | `SELECT ROUND(123.456, 2) FROM dual;` |
| `CEIL(num)` | Returns the smallest integer greater than or equal to `num` | `SELECT CEIL(3.2) FROM dual;` |
| `FLOOR(num)` | Returns the largest integer less than or equal to `num` | `SELECT FLOOR(3.8) FROM dual;` |
| `MOD(a, b)` | Returns the remainder of division | `SELECT MOD(10, 3) FROM dual;` |
| `POWER(a, b)` | Raises `a` to the power of `b` | `SELECT POWER(2, 3) FROM dual;` |

### Date and Time Functions

FileMaker SQL provides support for standard SQL-92 date/time functions.

| Function | Description | Example |
|----------|-------------|---------|
| `CURRENT_DATE` | Returns the current date | `SELECT CURRENT_DATE FROM dual;` |
| `CURRENT_TIME` | Returns the current time | `SELECT CURRENT_TIME FROM dual;` |
| `CURRENT_TIMESTAMP` | Returns the current timestamp | `SELECT CURRENT_TIMESTAMP FROM dual;` |
| `YEAR(date)` | Extracts the year from a date | `SELECT YEAR(hire_date) FROM employees;` |
| `MONTH(date)` | Extracts the month from a date | `SELECT MONTH(hire_date) FROM employees;` |
| `DAY(date)` | Extracts the day from a date | `SELECT DAY(hire_date) FROM employees;` |
| `HOUR(time)` | Extracts the hour from a time | `SELECT HOUR(login_time) FROM logs;` |
| `MINUTE(time)` | Extracts the minute from a time | `SELECT MINUTE(login_time) FROM logs;` |
| `SECOND(time)` | Extracts the second from a time | `SELECT SECOND(login_time) FROM logs;` |

### Conditional Functions

Conditional functions allow logic-based evaluations.

| Function | Description | Example |
|----------|-------------|---------|
| `CASE WHEN condition THEN result ELSE alternative END` | Evaluates conditions and returns values accordingly | `SELECT CASE WHEN salary > 50000 THEN 'High' ELSE 'Low' END FROM employees;` |
| `COALESCE(expr1, expr2, ...)` | Returns the first non-null value in the list | `SELECT COALESCE(department, 'Unknown') FROM employees;` |
| `NULLIF(expr1, expr2)` | Returns `NULL` if `expr1` equals `expr2`; otherwise, returns `expr1` | `SELECT NULLIF(salary, 0) FROM employees;` |

### FileMaker-Specific (Non-ANSI) Functions

FileMaker SQL includes some functions not found in standard ANSI SQL.

| Function | Description | Example |
|----------|-------------|---------|
| `GetAs(container_field, 'format')` | Extracts binary data from a container field in a specific format | `SELECT GetAs(Company_Logo, 'JPEG') FROM Company_Icons;` |
| `STRVAL(expr)` | Converts any value to a string | `SELECT STRVAL(123) FROM dual;` |
| `NUMVAL(str)` | Converts a string to a numeric value | `SELECT NUMVAL('123.45') FROM dual;` |
| `TIMESTAMPVAL(str)` | Converts a string to a timestamp | `SELECT TIMESTAMPVAL('2024-03-26 14:30:00') FROM dual;` |

These functions are specific to FileMaker SQL and are not part of ANSI SQL standards. They provide additional flexibility for handling binary data and type conversions.

### Considerations for JDBC Implementation

- **Function Support**: Ensure JDBC drivers fully support FileMaker-specific functions.
- **String Handling**: Use `STRVAL` and `NUMVAL` cautiously to avoid unexpected conversions.
- **Date and Time Compatibility**: Convert timestamps if working with external databases that require different formats.

### Unsupported Use Cases

- **User-Defined Functions (UDFs)**: FileMaker SQL does not support custom functions.
- **Window Functions**: Aggregations over partitions (e.g., `ROW_NUMBER()`, `RANK()`) are not available.
- **Regular Expressions**: No built-in support for regex-based string operations.

This document provides an overview of SQL functions available in FileMaker, including both ANSI-standard and proprietary extensions.
