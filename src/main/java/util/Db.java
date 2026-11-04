package util;

import java.sql.Connection;
import java.sql.DriverManager;

/**
 * Opens connections to the Entropy database.
 *
 * The defaults are for the local MariaDB/MySQL on this machine. To point the app somewhere else
 * without editing code, set ENTROPY_DB_URL, ENTROPY_DB_USER and ENTROPY_DB_PASSWORD.
 */
public final class Db {

    private static final String URL = env("ENTROPY_DB_URL", "jdbc:mysql://localhost:3306/entropy?useSSL=false");
    private static final String USER = env("ENTROPY_DB_USER", "alias");
    private static final String PASSWORD = env("ENTROPY_DB_PASSWORD", "770077");

    private Db() {}

    public static Connection connect() throws Exception {
        Class.forName("com.mysql.cj.jdbc.Driver");
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }
}
