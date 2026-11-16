package ni.edu.uam.nautaticket.util;

import ni.edu.uam.nautaticket.model.HistorialTicket;
import ni.edu.uam.nautaticket.service.HistorialService;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.AvailableSettings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.postgresql.ds.PGSimpleDataSource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@Timeout(30)
class HistorialFiltrosPostgresTest {
    private static final LocalDate DIA = LocalDate.of(2026, 1, 1);
    private PGSimpleDataSource base;
    private PGSimpleDataSource datos;
    private SessionFactory sesiones;
    private String esquema;
    private int solicitanteA;
    private int solicitanteB;
    private int ticketA;
    private int ticketB;
    private int ticketC;
    private int inicioA;
    private int procesoA;
    private int cierreA;
    private int inicioB;
    private int resueltoB;
    private int inicioC;

    @BeforeEach
    void preparar() throws Exception {
        String url = System.getenv("NAUTATICKET_TEST_DB_URL");
        assumeTrue(url != null && !url.isBlank(), "Se requiere una base PostgreSQL desechable.");
        base = dataSource(url);
        assertEquals("nautaticket_test", base.getDatabaseName(), "La base de prueba debe llamarse nautaticket_test");
        esquema = "prueba_" + UUID.randomUUID().toString().replace("-", "");
        try (var conexion = base.getConnection(); var sentencia = conexion.createStatement()) {
            sentencia.execute("CREATE SCHEMA " + esquema);
        }
        datos = dataSource(url);
        datos.setCurrentSchema(esquema);
        aplicar("02_schema.sql");
        aplicar("03_seed.sql");
        aplicar("04_migracion_hibernate.sql");

        solicitanteA = usuario("a");
        solicitanteB = usuario("b");
        ticketA = ticket(solicitanteA);
        ticketB = ticket(solicitanteB);
        ticketC = ticket(solicitanteA);
        inicioA = movimiento(ticketA, solicitanteA, "PENDIENTE", "2026-01-01 00:00:00");
        procesoA = movimiento(ticketA, solicitanteA, "EN_PROCESO", "2026-01-01 12:00:00");
        cierreA = movimiento(ticketA, solicitanteA, "CERRADO", "2026-01-01 23:59:59.999999");
        inicioB = movimiento(ticketB, solicitanteB, "PENDIENTE", "2026-01-01 12:00:00");
        resueltoB = movimiento(ticketB, solicitanteB, "RESUELTO", "2026-01-02 00:00:00");
        inicioC = movimiento(ticketC, solicitanteA, "PENDIENTE", "2026-01-02 09:00:00");

        var configuracion = HibernateUtil.configuracionBase()
                .setProperty("hibernate.default_schema", esquema)
                .setProperty("hibernate.show_sql", "false");
        configuracion.getProperties().put(AvailableSettings.JAKARTA_NON_JTA_DATASOURCE, datos);
        sesiones = configuracion.buildSessionFactory();
    }

    @AfterEach
    void retirar() throws SQLException {
        try {
            if (sesiones != null) sesiones.close();
        } finally {
            if (esquema != null) {
                try (var conexion = base.getConnection(); var sentencia = conexion.createStatement()) {
                    sentencia.execute("DROP SCHEMA " + esquema + " CASCADE");
                }
            }
        }
    }

    @Test
    void filtrosIndividualesSinIdYOrdenEstable() {
        var servicio = new HistorialService(sesiones);
        assertEquals(List.of(inicioC, resueltoB, cierreA, inicioB, procesoA, inicioA),
                ids(servicio.buscar(null, null, null, null)));
        assertEquals(4, servicio.buscar(null, DIA, DIA, null).size());
        assertEquals(2, servicio.buscar(null, DIA.plusDays(1), null, null).size());
        assertEquals(4, servicio.buscar(null, null, DIA, null).size());
        assertEquals(3, servicio.buscar(null, null, null, "PENDIENTE").size());
        assertEquals(3, servicio.buscar(ticketA, null, null, null).size());
    }

    @Test
    void combinaFiltrosEIncluyeAmbosLimitesDelDia() {
        var servicio = new HistorialService(sesiones);
        assertEquals(List.of(cierreA), ids(servicio.buscar(ticketA, DIA, DIA, "CERRADO")));
        assertEquals(List.of(inicioA), ids(servicio.buscar(ticketA, DIA, DIA, "PENDIENTE")));
        assertEquals(List.of(inicioB, inicioA), ids(servicio.buscar(null, DIA, DIA, "PENDIENTE")));
        assertTrue(servicio.buscar(ticketB, DIA, DIA, "RESUELTO").isEmpty());
        assertEquals(1, servicio.buscar(ticketB, DIA.plusDays(1), DIA.plusDays(1), "RESUELTO").size());
    }

    @Test
    void empleadoSoloVeSusTicketsConYCualquierFiltro() {
        var empleado = new HistorialService(sesiones, solicitanteA);
        assertEquals(4, empleado.buscar(null, null, null, null).size());
        assertEquals(3, empleado.buscar(null, DIA, DIA, null).size());
        assertEquals(List.of(inicioA), ids(empleado.buscar(null, DIA, DIA, "PENDIENTE")));
        assertTrue(empleado.buscar(ticketB, null, null, null).isEmpty());
        assertTrue(empleado.buscar(ticketB, DIA, DIA, "PENDIENTE").isEmpty());
        assertEquals(3, empleado.historialPorTicket(ticketA, null, null, null).size());
    }

    @Test
    void rechazaIdNoPositivoRangoInvertidoYEstadoDesconocido() {
        var servicio = new HistorialService(sesiones);
        assertThrows(IllegalArgumentException.class, () -> servicio.buscar(0, null, null, null));
        assertThrows(IllegalArgumentException.class, () -> servicio.buscar(-1, null, null, null));
        assertThrows(IllegalArgumentException.class, () -> servicio.buscar(null, DIA.plusDays(1), DIA, null));
        assertThrows(IllegalArgumentException.class, () -> servicio.buscar(null, null, null, "DESCONOCIDO"));
    }

    private static List<Integer> ids(List<HistorialTicket> movimientos) {
        return movimientos.stream().map(HistorialTicket::getIdHistorial).toList();
    }

    private int usuario(String nombre) throws SQLException {
        return entero("""
                INSERT INTO usuarios (nombre, apellido, correo, password_hash, id_rol)
                VALUES ('%s', 'Prueba', '%s@nauta.invalid', 'fixture',
                        (SELECT id_rol FROM roles WHERE nombre = 'EMPLEADO'))
                RETURNING id_usuario
                """.formatted(nombre, nombre));
    }

    private int ticket(int solicitante) throws SQLException {
        return entero("""
                INSERT INTO tickets (id_solicitante, id_categoria, titulo, descripcion, id_estado)
                VALUES (%d, (SELECT MIN(id_categoria) FROM categorias), 'Prueba', 'Detalle',
                        (SELECT id_estado FROM estados_ticket WHERE nombre = 'PENDIENTE'))
                RETURNING id_ticket
                """.formatted(solicitante));
    }

    private int movimiento(int ticket, int actor, String nuevo, String fecha) throws SQLException {
        return entero("""
                INSERT INTO historial_tickets (id_ticket, id_usuario, estado_nuevo, fecha_cambio)
                VALUES (%d, %d, '%s', TIMESTAMP '%s') RETURNING id_historial
                """.formatted(ticket, actor, nuevo, fecha));
    }

    private int entero(String sql) throws SQLException {
        try (var conexion = datos.getConnection(); var sentencia = conexion.createStatement();
             var resultado = sentencia.executeQuery(sql)) {
            assertTrue(resultado.next());
            return resultado.getInt(1);
        }
    }

    private void aplicar(String archivo) throws Exception {
        try (var conexion = datos.getConnection(); var sentencia = conexion.createStatement()) {
            sentencia.execute(Files.readString(Path.of("database", archivo)));
        }
    }

    private static PGSimpleDataSource dataSource(String url) {
        var ds = new PGSimpleDataSource();
        ds.setURL(url);
        ds.setUser(System.getenv("NAUTATICKET_TEST_DB_USER"));
        ds.setPassword(System.getenv("NAUTATICKET_TEST_DB_PASSWORD"));
        return ds;
    }
}
