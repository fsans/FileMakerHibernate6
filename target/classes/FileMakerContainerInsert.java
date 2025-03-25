import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class FileMakerContainerInsert {
    public static void insertContainerData(String dbUrl, String username, String password, 
                                            String tableName, String containerColumnName, 
                                            File dataFile, String fileType) {
        // JDBC Connection string format
        // Typical format: jdbc:filemaker://host:port/database
        String connectionUrl = dbUrl;
        
        try {
            // Load the FMJDBC driver
            Class.forName("com.filemaker.jdbc.Driver");
            
            // Establish database connection
            try (Connection connection = DriverManager.getConnection(connectionUrl, username, password)) {
                // Prepare SQL insert statement using PutAs() for container field
                // Note: Adjust the column names and table name as per your specific database schema
                String insertSQL = "INSERT INTO " + tableName + 
                                   " (other_column1, other_column2, " + containerColumnName + ") " +
                                   "VALUES (?, ?, PutAs(?, '" + fileType + "'))";
                
                try (PreparedStatement pstmt = connection.prepareStatement(insertSQL)) {
                    // Set other columns (replace with actual column values)
                    pstmt.setString(1, "SomeStringValue");
                    pstmt.setInt(2, 42);
                    
                    // Read binary file
                    try (FileInputStream fis = new FileInputStream(dataFile)) {
                        // Set binary stream with PutAs for container field
                        pstmt.setBinaryStream(3, fis, (int) dataFile.length());
                        
                        // Execute the insert
                        int rowsAffected = pstmt.executeUpdate();
                        
                        // Confirm insertion
                        System.out.println("Rows inserted: " + rowsAffected);
                    }
                }
            }
        } catch (ClassNotFoundException e) {
            System.err.println("FMJDBC Driver not found: " + e.getMessage());
        } catch (java.sql.SQLException e) {
            System.err.println("Database error: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("File reading error: " + e.getMessage());
        }
    }
    
    public static void main(String[] args) {
        // Example usage
        String dbUrl = "jdbc:filemaker://localhost:2399/YourDatabase";
        String username = "your_username";
        String password = "your_password";
        String tableName = "YourTableName";
        String containerColumnName = "ContainerField";
        
        File dataFile = new File("/path/to/your/file");
        
        // Examples of file types:
        // 'JPEG', 'PNG', 'GIF', 'PDF', 'TIFF', etc.
        insertContainerData(dbUrl, username, password, tableName, containerColumnName, dataFile, "JPEG");
    }
}

/* 
 * Important Considerations:
 * 1. Ensure you have the FMJDBC driver JAR in your classpath
 * 2. Replace placeholders with your actual database connection details
 * 3. Modify SQL statement to match your specific database schema
 * 4. Use appropriate file type with PutAs() method
 * 5. Handle exceptions appropriately in production code
 * 6. Close resources properly (connection, statement, input stream)
 */