package to.pkgdo.list;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    public static Connection getConnection() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver"); // Load MySQL driver
            return DriverManager.getConnection("jdbc:mysql://localhost:3306/todo_db", "root", "");
        } catch (ClassNotFoundException | SQLException e) {
            System.out.println("❌ Error connecting to database: " + e.getMessage());
            return null;
        }
    }
}
