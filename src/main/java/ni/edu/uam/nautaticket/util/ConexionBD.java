package ni.edu.uam.nautaticket.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConexionBD {
    private static final String DEFAULT_URL =
            "jdbc:postgresql://localhost:5432/nautaticket_db";

    private ConexionBD() {
    }

    public static Connection getConnection() throws SQLException {
        // Misma configuración que HibernateUtil: DB_URL, DB_USER y DB_PASSWORD.
        String url = System.getenv().getOrDefault("DB_URL", DEFAULT_URL);
        String user = System.getenv().getOrDefault("DB_USER", "postgres");
        String password = System.getenv("DB_PASSWORD");

        if (password == null || password.isBlank()) {
            throw new SQLException("Configura la variable DB_PASSWORD para PostgreSQL.");
        }

        return DriverManager.getConnection(url, user, password);
    }
}
