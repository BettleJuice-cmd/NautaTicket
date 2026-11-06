package ni.edu.uam.nautaticket.util;
import java.sql.Connection;
import java.sql.SQLException;

public class TestConexionBD {
    public static void main(String[] args) {

        try (Connection conexion = ConexionBD.getConnection()) {

            System.out.println("Conexion exitosa con PostgreSQL.");
            System.out.println("Base de datos: " + conexion.getCatalog());

        } catch (SQLException e) {

            System.err.println("Error al conectar con PostgreSQL.");
            System.err.println("Detalle: " + e.getMessage());

        }
    }
}
