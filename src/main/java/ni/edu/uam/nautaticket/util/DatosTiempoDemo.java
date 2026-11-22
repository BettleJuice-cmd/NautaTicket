package ni.edu.uam.nautaticket.util;

import org.mindrot.jbcrypt.BCrypt;

import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Base64;

/** Carga reproducible de tickets resueltos para comprobar los gráficos locales. */
public final class DatosTiempoDemo {
    private static final String MARCA = "[DEMO TIEMPOS]";
    private static final String CORREO_NUEVO = "tecnico.tiempos.demo@nautaticket.local";

    private DatosTiempoDemo() { }

    public static void main(String[] args) throws Exception {
        try (Connection conexion = ConexionBD.getConnection()) {
            conexion.setAutoCommit(false);
            try {
                int existentes = escalar(conexion,
                        "SELECT COUNT(*) FROM tickets WHERE titulo LIKE '[DEMO TIEMPOS]%' ");
                if (existentes == 4) {
                    System.out.println("Los cuatro tickets de demostración ya existen.");
                    conexion.rollback();
                    return;
                }
                if (existentes != 0) {
                    throw new IllegalStateException(
                            "Hay una carga parcial de tiempos de demostración; no se agregaron duplicados.");
                }

                int tecnicoExistente = buscarId(conexion, """
                        SELECT u.id_usuario FROM usuarios u JOIN roles r ON r.id_rol=u.id_rol
                        WHERE u.correo='tecnico.demo@nautaticket.com'
                          AND u.activo AND r.nombre='TECNICO'
                        """);
                int solicitante = buscarId(conexion, """
                        SELECT u.id_usuario FROM usuarios u JOIN roles r ON r.id_rol=u.id_rol
                        WHERE u.correo='empleado.demo@nautaticket.com'
                          AND u.activo AND r.nombre='EMPLEADO'
                        """);
                int categoria = buscarId(conexion,
                        "SELECT id_categoria FROM categorias WHERE activa ORDER BY id_categoria LIMIT 1");
                int estadoResuelto = buscarId(conexion,
                        "SELECT id_estado FROM estados_ticket WHERE nombre='RESUELTO'");
                int tecnicoNuevo = crearTecnico(conexion);

                LocalDateTime ahora = LocalDateTime.now();
                crearTicket(conexion, solicitante, categoria, estadoResuelto, tecnicoExistente,
                        "Revisión de impresora", 18, ahora.minusMinutes(11));
                crearTicket(conexion, solicitante, categoria, estadoResuelto, tecnicoExistente,
                        "Ajuste de acceso a software", 47, ahora.minusMinutes(2));
                crearTicket(conexion, solicitante, categoria, estadoResuelto, tecnicoNuevo,
                        "Diagnóstico de red", 72, ahora.minusMinutes(12));
                crearTicket(conexion, solicitante, categoria, estadoResuelto, tecnicoNuevo,
                        "Recuperación de estación", 155, ahora.minusMinutes(1));

                conexion.commit();
                System.out.println("Carga completada: técnico existente #" + tecnicoExistente
                        + ", técnico nuevo #" + tecnicoNuevo + ", 4 tickets resueltos.");
            } catch (Exception error) {
                conexion.rollback();
                throw error;
            }
        }
    }

    private static int crearTecnico(Connection conexion) throws SQLException {
        try (PreparedStatement consulta = conexion.prepareStatement(
                "SELECT id_usuario FROM usuarios WHERE correo=?")) {
            consulta.setString(1, CORREO_NUEVO);
            try (ResultSet resultado = consulta.executeQuery()) {
                if (resultado.next()) {
                    int id = resultado.getInt(1);
                    if (resultado.next()) throw new IllegalStateException("Correo de demostración duplicado.");
                    return id;
                }
            }
        }
        byte[] aleatorio = new byte[24];
        new SecureRandom().nextBytes(aleatorio);
        String password = Base64.getUrlEncoder().withoutPadding().encodeToString(aleatorio);
        try (PreparedStatement insercion = conexion.prepareStatement("""
                INSERT INTO usuarios (nombre, apellido, correo, password_hash, activo, id_rol, id_departamento)
                SELECT 'Tecnico', 'Tiempos Demo', ?, ?, true, r.id_rol,
                       (SELECT id_departamento FROM departamentos ORDER BY id_departamento LIMIT 1)
                FROM roles r WHERE r.nombre='TECNICO'
                RETURNING id_usuario
                """)) {
            insercion.setString(1, CORREO_NUEVO);
            insercion.setString(2, BCrypt.hashpw(password, BCrypt.gensalt()));
            try (ResultSet resultado = insercion.executeQuery()) {
                if (!resultado.next()) throw new IllegalStateException("No existe el rol TECNICO.");
                return resultado.getInt(1);
            }
        }
    }

    private static void crearTicket(Connection conexion, int solicitante, int categoria,
            int estado, int tecnico, String asunto, int duracionMinutos,
            LocalDateTime fin) throws SQLException {
        LocalDateTime asignacion = fin.minusMinutes(duracionMinutos);
        int ticket;
        try (PreparedStatement insercion = conexion.prepareStatement("""
                INSERT INTO tickets (id_solicitante, id_categoria, id_tecnico, id_estado,
                                     titulo, descripcion, solucion, fecha_creacion,
                                     fecha_asignacion, fecha_resolucion)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                RETURNING id_ticket
                """)) {
            insercion.setInt(1, solicitante);
            insercion.setInt(2, categoria);
            insercion.setInt(3, tecnico);
            insercion.setInt(4, estado);
            insercion.setString(5, MARCA + " " + asunto);
            insercion.setString(6, "Caso de demostración para verificar tiempos de resolución.");
            insercion.setString(7, "Solución simulada para comprobar los gráficos.");
            insercion.setTimestamp(8, Timestamp.valueOf(asignacion.minusMinutes(5)));
            insercion.setTimestamp(9, Timestamp.valueOf(asignacion));
            insercion.setTimestamp(10, Timestamp.valueOf(fin));
            try (ResultSet resultado = insercion.executeQuery()) {
                resultado.next();
                ticket = resultado.getInt(1);
            }
        }
        try (PreparedStatement insercion = conexion.prepareStatement("""
                INSERT INTO asignaciones_ticket (id_ticket, id_tecnico, fecha_asignacion, fecha_fin)
                VALUES (?, ?, ?, ?)
                """)) {
            insercion.setInt(1, ticket);
            insercion.setInt(2, tecnico);
            insercion.setTimestamp(3, Timestamp.valueOf(asignacion));
            insercion.setTimestamp(4, Timestamp.valueOf(fin));
            insercion.executeUpdate();
        }
        try (PreparedStatement insercion = conexion.prepareStatement("""
                INSERT INTO historial_tickets (id_ticket, id_usuario, estado_anterior,
                                               estado_nuevo, comentario, fecha_cambio)
                VALUES (?, ?, 'EN_PROCESO', 'RESUELTO', ?, ?)
                """)) {
            insercion.setInt(1, ticket);
            insercion.setInt(2, tecnico);
            insercion.setString(3, "Tiempo de resolución: " + duracionMinutos
                    + " min. Ticket de demostración resuelto.");
            insercion.setTimestamp(4, Timestamp.valueOf(fin));
            insercion.executeUpdate();
        }
    }

    private static int escalar(Connection conexion, String sql) throws SQLException {
        try (PreparedStatement consulta = conexion.prepareStatement(sql);
             ResultSet resultado = consulta.executeQuery()) {
            resultado.next();
            return resultado.getInt(1);
        }
    }

    private static int buscarId(Connection conexion, String sql) throws SQLException {
        try (PreparedStatement consulta = conexion.prepareStatement(sql);
             ResultSet resultado = consulta.executeQuery()) {
            if (!resultado.next()) throw new IllegalStateException("Falta un dato base para la demostración.");
            return resultado.getInt(1);
        }
    }
}
