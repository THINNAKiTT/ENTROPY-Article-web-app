package util;
import java.sql.*;

public class utilDB {
    public static Connection getConnection() throws Exception {
        Class.forName("com.mysql.cj.jdbc.Driver");
        return DriverManager.getConnection(
            "jdbc:mysql://localhost:3306/entropy?useSSL=false",
            "alias", "770077");
    }
}
