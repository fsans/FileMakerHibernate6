

| FileMaker field type | Converts to ODBC data type | About the data type |
|-----------|---------|---------|
|text |SQL_VARCHAR |The maximum column length of text is 1 million characters, unless you specify a smaller Maximum number of characters for the text field in FileMaker. FileMaker returns empty strings as NULL|
|number |SQL_DOUBLE |   The FileMaker number field type can contain positive or negatives values as small as 10-308, and as large as 10+308, with up to 15 significant digits|
|date |SQL_DATE |The FileMaker date field type can contain a date or a date interval. A date interval is returned as a date, unless it is less than 0 or greater than 24 hours (both return a value of 0)|
|time |SQL_TIME |The FileMaker time field type can contain the time of day or a time interval. A time interval is returned as a time of day, unless it is less than 0 or greater than 24 hours (both return a value of 0)|
|timestamp |SQL_TIMESTAMP |The FileMaker timestamp field type can contain a date and time or a date and time interval. A date and time interval is returned as a date and time, unless it is less than 0 or greater than 24 hours (both return a value of 0)|
|container (BLOB) |QL_LONGVARBINARY |You can retrieve binary data, file reference information, or data of a specific file type from a container field. Within a SELECT statement, use the CAST function to retrieve file reference information, and use the GetAs function to retrieve data of a specific file type|
|calculation |The result is mapped to the corresponding ODBC data type|String length is optional in table declarations. All strings are stored and retrieved in Unicode|

Notes: 

- You can SELECT up to 170 fields at one time from a FileMaker database file; - You can UPDATE up to 100 fields at one time.
- FileMaker supports repeating fields (array data types), but ODBC does not. - - FileMaker exports repetitions to tab-delimited or comma-delimited files and separates each repetition with a group separator (Unicode decimal value 29). Text columns separated with the group separator are concatenated. All other data types return only the first repetition. 
NOTE: In FileMaker Pro 11, ODBC and JDBC drivers support repeating fields for Text, Number, Time, Date, Timestamp types.