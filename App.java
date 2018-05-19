import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.io.*;

import org.postgresql.PGConnection;
import org.postgresql.largeobject.LargeObject;
import org.postgresql.largeobject.LargeObjectManager;
/**
 *
 * @author postgresqltutorial.com
 */
public class App{

    /**
     * Connect to the PostgreSQL database
     *
     * @return a Connection object
     */
    public Connection connect() {
        Connection conn = null;

        try {
            Class.forName("org.postgresql.Driver");
            conn = DriverManager.getConnection(url, user, password);
            System.out.println("Connected to the PostgreSQL server successfully.");
        } catch (SQLException e) {
            System.out.println("SQLException");
            System.out.println(e.getMessage());
        } catch (ClassNotFoundException e) {
            System.out.println(e.getMessage());
        }
 
        return conn;
    }

    public void truncateTable(Connection conn) {
        try {
            PreparedStatement ps = conn.prepareStatement("TRUNCATE TABLE world_files;");
            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }


    public void createTable(Connection conn) {
        try {
            PreparedStatement ps = conn.prepareStatement("CREATE TABLE world_files (file_name text, file_oid oid);");
            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public void persistFile(Connection conn) {
        try {   
            // All LargeObject API calls must be within a transaction block
            conn.setAutoCommit(false);

            // Get the Large Object Manager to perform operations with
            LargeObjectManager lobj = ((PGConnection)conn).getLargeObjectAPI();

            // Create a new large object
            int oid = lobj.create(LargeObjectManager.READ | LargeObjectManager.WRITE);

            // Open the large object for writing
            LargeObject obj = lobj.open(oid, LargeObjectManager.WRITE);

            // Now open the file
            File file = new File("eula.txt");
            FileInputStream fis = new FileInputStream(file);

            // Copy the data from the file to the large object
            byte buf[] = new byte[2048];
            int s, tl = 0;
            while ((s = fis.read(buf, 0, 2048)) > 0) {
                obj.write(buf, 0, s);
                tl += s;
            }

            // Close the large object
            obj.close();

            // Now insert the row into imageslo
            PreparedStatement ps = conn.prepareStatement("INSERT INTO world_files VALUES (?, ?)");
            ps.setString(1, file.getName());
            ps.setInt(2, oid);
            ps.executeUpdate();
            ps.close();
            fis.close();

            // Finally, commit the transaction.
            conn.commit();
        } catch (FileNotFoundException e) {
            System.out.println(e.getMessage());
        } catch (IOException e) {
            System.out.println(e.getMessage());
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public void restoreFile(Connection conn) {
        try {
            // All LargeObject API calls must be within a transaction block
            conn.setAutoCommit(false);

            // Get the Large Object Manager to perform operations with
            LargeObjectManager lobj = ((org.postgresql.PGConnection)conn).getLargeObjectAPI();

            PreparedStatement ps = conn.prepareStatement("SELECT file_name, file_oid FROM world_files WHERE file_name = ?");
            ps.setString(1, "eula.txt");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                // Open the large object for reading
                String fileName = rs.getString(1);
                int oid = rs.getInt(2);
                LargeObject obj = lobj.open(oid, LargeObjectManager.READ);

                // Read the data
                byte buf[] = new byte[obj.size()];
                obj.read(buf, 0, obj.size());
                
                // Do something with the data read here
                System.out.println("File: " + fileName + ", has Object ID: " + oid);

                // Close the object
                obj.close();
            }
            rs.close();
            ps.close();

            // Finally, commit the transaction.
            conn.commit();
        } catch (SQLException e) {
            System.out.println("SQLException");
            System.out.println(e.getMessage());
        }

    }
 
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        App app = new App();
        Connection conn = app.connect();
        System.out.println(args[0]);
        switch (args[0]) {
            case "reset":
                System.out.println("============== Truncating =============");
                app.truncateTable(conn);
                System.out.println("============== Truncated =============");
                break;
            case "persist":
                System.out.println("============== Persisting =============");
                app.createTable(conn);
                app.persistFile(conn);
                System.out.println("============== Persisted =============");
                break;
            case "restore":
                System.out.println("============== Restoring =============");
                app.restoreFile(conn);
                System.out.println("============== Restored ==============");  
                break;
            default:
                System.out.println("Specify persist or restore");
                break;
        }
    }
}

