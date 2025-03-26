# SQL Pagination in FileMaker

## OFFSET Clause

The `OFFSET` clause determines the starting position of the records to be retrieved.

### Syntax

```sql
OFFSET n {ROWS | ROW}
```

- `n` is an unsigned integer.
- If `n` is greater than the total number of rows available, no results are returned.
- `ROWS` and `ROW` are interchangeable.
- Cannot be used in subqueries.

### Example

```sql
SELECT emp_id, last_name, first_name 
FROM employees 
ORDER BY last_name, first_name 
OFFSET 25 ROWS;
```

This query skips the first 25 records and starts retrieval from the 26th record.

### Edge Cases

- **`OFFSET 0`**: Returns all rows without skipping.
- **`OFFSET n` where `n > total_rows`**: Returns an empty result set without an error.

## FETCH FIRST Clause

The `FETCH FIRST` clause limits the number of rows returned.

### FETCH FIRST Clause Syntax

```sql
FETCH FIRST [ n [ PERCENT ] ] { ROWS | ROW } {ONLY | WITH TIES }
```

- `n` defines the number of rows to return. Default is `1` if omitted.
- `PERCENT` fetches `n%` of the total result set.
- `WITH TIES` ensures that all rows with the same ordering values are included.
- Cannot be used in subqueries.

#### FETCH FIRST Clause Example

```sql
SELECT emp_id, last_name, first_name 
FROM employees 
ORDER BY last_name, first_name 
FETCH FIRST 10 ROWS ONLY;
```

This query returns the first 10 records from the result set.

### FETCH FIRST Clause Edge Cases

- **`FETCH FIRST 0 ROWS ONLY`**: Returns an empty result set without an error.
- **`FETCH FIRST 100 PERCENT ROWS ONLY`**: Returns all rows.
- **Use without `ORDER BY`**: Retrieves an arbitrary subset of rows due to undefined ordering.

### Combining OFFSET and FETCH FIRST

Both clauses can be combined to implement pagination.

#### Combining OFFSET and FETCH FIRST Example

```sql
SELECT emp_id, last_name, first_name 
FROM employees 
ORDER BY last_name, first_name 
OFFSET 25 ROWS FETCH FIRST 10 ROWS ONLY;
```

This query retrieves 10 rows starting from the 26th record.

### WITH TIES Usage

If `WITH TIES` is used, additional rows sharing the same values as the last retrieved row (based on `ORDER BY`) are included.

#### WITH TIES Example

```sql
SELECT emp_id, last_name, first_name 
FROM employees 
ORDER BY last_name, first_name 
FETCH FIRST 10 ROWS WITH TIES;
```

This ensures all employees with the same last name and first name as the 10th record are included.

### WITH TIES Edge Cases

- **Without `ORDER BY`**: `WITH TIES` has no effect.
- **More rows than `n` returned**: If multiple rows share the same value at the cutoff point, additional rows are returned.

### Performance Considerations

- **Sorting Requirement**: The `ORDER BY` clause is essential to ensure deterministic results.
- **Offset Handling**: Large `OFFSET` values cause inefficiency since all preceding rows are still processed internally before skipping.
- **Best Practices**: Use indexed fields in `ORDER BY` to optimize performance.

### Unsupported Use Cases

- **Subqueries**: `OFFSET` and `FETCH FIRST` cannot be used in subqueries.
- **Updates & Deletes**: `OFFSET` and `FETCH FIRST` are only valid in `SELECT` statements.
