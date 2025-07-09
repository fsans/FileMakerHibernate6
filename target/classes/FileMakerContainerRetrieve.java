import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.io.FileOutputStream;
import java.io.InputStream;

public class FileMakerContainerRetrieve {
    public static void retrieveContainerData(String dbUrl, String username, String password, 
                                              String tableName, String containerColumnName, 
                                              String whereClause, String outputPath) {
        Connection connection = null;
        Statement statement = null;
        ResultSet results = null;
        
        try {
            // Load the FMJDBC driver
            Class.forName("com.filemaker.jdbc.Driver");
            
            // Establish database connection
            connection = DriverManager.getConnection(dbUrl, username, password);
            
            // Create statement
            statement = connection.createStatement();
            
            // Construct query with GetAs to specify file type
            String query = String.format(
                "SELECT id, GetAs(%s, 'JPEG') AS container_data FROM %s WHERE %s", 
                containerColumnName, 
                tableName, 
                whereClause
            );
            
            // Execute query
            results = statement.executeQuery(query);
            
            // Process results
            if (results.next()) {
                // Get binary stream from container field
                try (InputStream imageData = results.getBinaryStream("container_data");
                     FileOutputStream outputStream = new FileOutputStream(
                         outputPath + "/image_" + results.getString("id") + ".jpg")) {
                    
                    // Simple byte-by-byte copy (Note: for large files, use buffered approach)
                    int c;
                    while ((c = imageData.read()) != -1) {
                        outputStream.write(c);
                    }
                    
                    System.out.println("Container data retrieved and saved to: " + outputPath);
                }
            } else {
                System.out.println("No matching record found.");
            }
        } catch (ClassNotFoundException e) {
            System.err.println("FMJDBC Driver not found: " + e.getMessage());
        } catch (java.sql.SQLException e) {
            System.err.println("Database error: " + e.getMessage());
        } catch (java.io.IOException e) {
            System.err.println("File writing error: " + e.getMessage());
        } finally {
            // Ensure all resources are closed
            try {
                if (results != null) results.close();
                if (statement != null) statement.close();
                if (connection != null) connection.close();
            } catch (java.sql.SQLException e) {
                System.err.println("Error closing resources: " + e.getMessage());
            }
        }
    }
    
    public static void main(String[] args) {
        // Example usage
        String dbUrl = "jdbc:filemaker://localhost:2399/YourDatabase";
        String username = "your_username";
        String password = "your_password";
        String tableName = "english_nature";
        String containerColumnName = "img";
        
        // Retrieve container data
        retrieveContainerData(
            dbUrl, 
            username, 
            password, 
            tableName, 
            containerColumnName, 
            "ID = 23", 
            "/Users/YourName/Desktop"
        );
    }
}

/* 
 * Important Considerations:
 * 1. Ensure FMJDBC driver JAR is in your classpath
 * 2. Replace placeholders with actual database connection details
 * 3. Modify WHERE clause and table/column names to match your schema
 * 4. Can use different file types: 'JPEG', 'PNG', 'PDF', etc. with GetAs()
 * 5. For large files, consider using buffered streams for more efficient reading
 */