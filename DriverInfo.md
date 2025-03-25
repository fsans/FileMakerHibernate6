# FileMaker JDBC Driver

The driver class and main entry point for the driver is
**com.filemaker.jdbc.Driver**

It is a Type 4 driver, not full compliant

## Connection url

```sql
com.filemaker.jdbc://host:port/database?params
```

port is not required, defaults to 2399

### Parameters

Connection URL parametres used by fmjdbc driver

|  Param  |    |  default  |  Options  |
|----|----|----|----|
| "User" | required |||
| "Password" | optional|||
| "ConnectTimeout" | optional| 0 ||
| "SocketTimeout" |optional| 0 ||
| "ServerName" | required|||
| "ServerDataSource" | required |||
| "LocalSSL" | optional | empty |(values Yes\|**empty**, any value not "Yes" is interpreted as false) |
| "CertificateVerificationLevel" | optional | "None" | (values **None**\|Certificate\|Name\|Full ) |
| "CertificateFailureType" | optional | "Warning" | (values None\|**Warning**\|Error) |
| "loglevel" | optional | empty | **empty** or 0\|>=1 is INFO\|>=2 is DEBUG, the log output is fixed to /tmp/fmjdbc.log and overwriten by session |

## type ids

2, 3, 4, 12, 12, -2, -2, -2, -2, 91, 92, 93

## JDBC2 types

"numeric", "decimal", "int", "varchar", "character varying", "blob", "varbinary", "longvarbinary", "binary varying", "date", "time", "timestamp"

## JDBC3 types

"numeric", "decimal", "int", "varchar", "character varying", "blob", "varbinary", "longvarbinary", "binary varying", "date", "time", "timestamp"

## set log level

change the log level of the driver with:

```java
setLogLevel(int param);
// param >= 1 set to INFO
// param >= 2 set to DEBUG
```

## Data types

Type conversion from FileMaker field types to java.sql types (JDBC SQL type) is as follows

|  FM type  |  Java Type  |
|----|----|
|text  |java.sql.Types.VARCHAR|
|number |java.sql.Types.DOUBLE|
|date  |java.sql.Types.DATE|
|time  |java.sql.Types.TIME|
|timestamp |java.sql.Types.TIMESTAMP|
|container  |java.sql.Types.BLOB|
|calculation  |specified by the data type of the calculation’s result|

The driver can accepts the following datatypes:

"numeric", "decimal", "int", "varchar", "character varying", "blob", "varbinary", "longvarbinary", "binary varying", "date", "time", "timestamp"

## Pagination

Supports the standar ANSI OFFSET/FETCH clause in the following form:

```sql
OFFSET x ROW|ROWS 
FETCH FIRST y ROW|ROWS ONLY
```

The complete fetch, with optional locking and sorting,  clause can be:

```sql
[FOR UPDATE (OF]) ]...
[ORDER BY ... ]
OFFSET x ROW|ROWS 
FETCH FIRST y ROW|ROWS ONLY
[WITH TIES]
```

## FileMaker system columns

FileMaker software adds system columns (fields) to all of the rows (records) in all of the tables that are defined in the FileMaker Pro file. For ODBC applications, these columns are included in the information returned by the catalog function SQLSpecialColumns. For JDBC applications, these columns are included in the information returned by the DatabaseMetaData method getVersionColumns. The columns can also be used in ExecuteSQL functions.

### ROWID column

The ROWID system column contains the unique ID number of the record. This is the same value that the FileMaker Pro Get(RecordID) function returns.

### ROWMODID column

The ROWMODID system column contains the total number of times changes to the current record have been committed. This is the same value that the FileMaker Pro Get(RecordModificationCount) function returns.

## Known limitations

This driver does not support the following features:

- SAVEPOINT statements
- retrieval of auto-generated keys
- passing parameters to a callable statement object by name
- holdable cursors
- retrieving and updating the object referenced by a Ref object
- updating of columns containing CLOB, ARRAY, and REF data types
- Boolean data type
- DATALINK data type
- transform groups and type mapping
- relationships between the JDBC SPI and the Connector architecture

## reserved words

## Binary data

The driver supports the following binary data types:

- BLOB
- VARBINARY
- LONGVARBINARY
- BINARY VARYING

If you use SELECT with binary data, you must use the **GetAs()** function to specify the stream to return.

### retrieving binary data

Retrieving the contents of a container field: **CAST()** function and **GetAs()** function

You can retrieve file reference information, binary data, or data of a specific file type from a container field.

- To retrieve file reference information from a container field, such as the file path to a file, picture, or QuickTime movie, use the **CAST()** function with a SELECT statement.
- If file data or JPEG binary data exists, the SELECT statement with **GetAs()** function retrieves the data in binary form; otherwise, the SELECT statement with field name returns NULL.

Example
Use the CAST() function with a SELECT statement to retrieve file reference information.

```sql
SELECT CAST(Company_Brochures AS VARCHAR) FROM Sales_Data
```

In this example, if you:

- inserted a file into the container field using FileMaker Pro but stored only a reference to the
file, the SELECT statement retrieves the file reference information as type SQL_VARCHAR.

- inserted the contents of a file into the container field using FileMaker Pro, the SELECT statement retrieves the name of the file.

- imported a file into the container field from another application, the SELECT statement displays '?' (the file displays as Untitled.dat in FileMaker Pro).

You can use the SELECT statement with the **GetAs()** function to retrieve the data in binary form in the following ways:

- When you use the **GetAs()** function with the DEFAULT option, you retrieve the default stream for the container without the need to explicitly define the stream type.

- To retrieve an individual stream type from a container, use the **GetAs()** function with the file’s type based on how the data was inserted into the container field in FileMaker Pro.

Example

```sql
 SELECT GetAs(Company_Brochures, DEFAULT) FROM Sales_Data
```

If the data was inserted using the Insert > File command, specify 'FILE' in the GetAs() function.

```sql
SELECT GetAs(Company_Brochures, 'FILE') FROM Sales_Data
```

If the data was inserted using the Insert > Picture command, drag and drop, or paste from the clipboard, specify one of the file types listed in the following table, for example, 'JPEG'.

File types
'GIFf', 'JPEG', 'TIFF','PDF ' and 'PNGf' (bitmap image format)

```sql
SELECT GetAs(Company_Logo, 'JPEG') FROM Company_Icons
```

### updating binary data

In container fields, you can UPDATE with text only, unless you prepare a parameterized statement and stream the data from your application. To use binary data, you may simply assign the filename by enclosing it in single quotation marks or use the **PutAs()** function. When specifying the filename, the file type is deduced from the file extension:

```sql
UPDATE table_name SET (container_name) = ? AS 'filename.file extension'
```

Unsupported file types will be inserted as type FILE.

When using the **PutAs()** function, specify the type: ```PutAs(col, 'type')```, where the type value is a supported file type

### Indexing binary fields

Indexes are not allowed on columns that correspond to container (binary) field types.

## Functions that return dates

**CURDATE** and **CURRENT_DATE** Returns today’s date

**CURTIME** and **CURRENT_TIME** Returns the current time

**CURTIMESTAMP** and **CURRENT_TIMESTAMP** Returns the current timestamp value

**TIMESTAMPVAL** Converts a character string to a date, example: TIMESTAMPVAL('2019-01-30 14:00:00')
returns its timestamp value

**DATE** and **TODAY** Returns today’s date, example: If today is 11/21/2019, DATE() returns 2019-11-21

**DATEVAL** Converts a character string to a date example:  DATEVAL('2019-01-30') returns 2019-01-30

Note The **DATE()** function is deprecated. Use the SQL standard **CURRENT_DATE** instead
