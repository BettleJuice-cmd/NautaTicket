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

        String url = System.getenv("NAUTATICKET_DB_URL");
        String user = System.getenv("NAUTATICKET_DB_USER");
        String password = System.getenv("NAUTATICKET_DB_PASSWORD");

        if (url == null || url.isBlank()) {
            url = DEFAULT_URL;
        }

        if (user == null || user.isBlank()
                || password == null || password.isBlank()) {
            throw new SQLException(
                    "Configura NAUTATICKET_DB_USER y NAUTATICKET_DB_PASSWORD."
            );
        }

        return DriverManager.getConnection(url, user, password);
    }
}
