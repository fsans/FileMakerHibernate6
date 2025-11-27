## SQL Binary Data Handling in FileMaker

### Retrieving Binary Data

FileMaker SQL allows retrieval of binary data stored in container fields using the `SELECT` statement. The `GetAs` function can be used to specify the format of the retrieved data.

#### Syntax

```sql
SELECT GetAs(field_name, 'format') FROM table_name;
```

- `field_name`: The name of the container field.
- `'format'`: The 4-character type code (classic Mac OS style):
  - `'GIFf'` - Graphics Interchange Format
  - `'JPEG'` - Photographic images
  - `'TIFF'` - Raster file format for digital images
  - `'PDF '` - Portable Document Format (note trailing space)
  - `'PNGf'` - Bitmap image format (PNG)

#### Example

```sql
SELECT GetAs(Company_Logo, 'JPEG') FROM Company_Icons;
```

This retrieves binary image data in JPEG format.

#### Edge Cases

- **Unsupported format**: Returns `NULL` if the specified format does not match the stored data.
- **Empty container field**: Returns `NULL`.

### Storing Binary Data

Binary data can be inserted into container fields using `INSERT` or `UPDATE`. Direct text insertion is possible, but binary streaming requires prepared statements in ODBC/JDBC.

#### Syntax

```sql
INSERT INTO table_name (container_field) VALUES (? AS 'filename.extension');
```

- `?` represents a placeholder for binary streaming.
- `'filename.extension'`: Specifies the file name and inferred format.

#### Example

```sql
INSERT INTO Documents (File_Attachment) VALUES (? AS 'report.pdf');
```

This inserts a binary PDF file into the `File_Attachment` field.

#### Edge Cases

- **Unsupported file types**: Inserted as type `FILE`.
- **Text-based insertion**: Direct text can be inserted, but binary data requires streaming.

### Updating Binary Data

Binary data can be updated similarly to insertion.

#### Syntax

```sql
UPDATE table_name SET container_field = ? AS 'filename.extension' WHERE condition;
```

#### Example

```sql
UPDATE Documents SET File_Attachment = ? AS 'report_v2.pdf' WHERE Doc_ID = 101;
```

This replaces the binary content of `File_Attachment`.

#### Edge Cases

- **File type mismatch**: If the new data format differs, FileMaker may change the stored type.

### Retrieving File References

To retrieve file paths or reference information, the `CAST` function is used.

#### Syntax

```sql
SELECT CAST(container_field AS VARCHAR) FROM table_name;
```

#### Example

```sql
SELECT CAST(Company_Brochures AS VARCHAR) FROM Sales_Data;
```

This retrieves the file path of the stored document.

#### Edge Cases

- **Referenced files moved or deleted**: May return invalid paths or missing references.

### External Storage Handling

FileMaker supports external storage of binary data. The `EXTERNAL` keyword defines storage options.

#### Syntax in Table Definition

```sql
CREATE TABLE table_name (
    container_field BLOB EXTERNAL 'relative_path' {SECURE | OPEN 'subfolder'}
);
```

- `SECURE`: Encrypts and manages storage.
- `OPEN 'subfolder'`: Allows access to stored files in a specific subfolder.

#### Example

```sql
CREATE TABLE Images (
    Product_Image BLOB EXTERNAL 'Files/Product_Images/' SECURE
);
```

This stores `Product_Image` files externally in a secure location.

#### Edge Cases

- **Incorrect path definitions**: May lead to inaccessible stored data.

### Considerations for JDBC Implementation

- **Binary Streaming**: Requires prepared statements for `INSERT` and `UPDATE`.
- **Storage Location**: External storage paths must be correctly defined.
- **Data Retrieval**: Use `GetAs` for structured access.

### Unsupported Use Cases

- **Subqueries**: Binary fields cannot be used in subqueries.
- **Indexing**: Binary data cannot be indexed for searches.
- **Sorting**: `ORDER BY` does not work with binary fields.
