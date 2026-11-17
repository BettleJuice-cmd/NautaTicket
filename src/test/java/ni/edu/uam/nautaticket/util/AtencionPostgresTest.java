package ni.edu.uam.nautaticket.util;

import ni.edu.uam.nautaticket.dao.TicketDAO;
import ni.edu.uam.nautaticket.dao.AsignacionTicketDao;
import ni.edu.uam.nautaticket.dao.ReporteDAO;
import ni.edu.uam.nautaticket.dto.ReporteTicketDTO;
import ni.edu.uam.nautaticket.model.AsignacionTicket;
import ni.edu.uam.nautaticket.service.AtencionService;
import ni.edu.uam.nautaticket.service.HistorialService;
import ni.edu.uam.nautaticket.service.ReporteService;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.AvailableSettings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import org.postgresql.ds.PGSimpleDataSource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@Timeout(30)
class AtencionPostgresTest {
    private static final String MIGRACION = "05_asignacion_activa_unica.sql";
    private static final String TEMPRANO = "2026-01-01 10:00:00";
    private static final String TARDE = "2026-01-01 11:00:00";

    private PGSimpleDataSource base;
    private PGSimpleDataSource datos;
    private String esquema;
    private SessionFactory sesiones;
    private int tecnicoA;
    private int tecnicoB;
    private int solicitante;

    @BeforeEach
    void prepararEsquema() throws Exception {
        String url = System.getenv("NAUTATICKET_TEST_DB_URL");
        assumeTrue(url != null && !url.isBlank(), "Configure una base PostgreSQL desechable.");
        base = dataSource(url);
        assertEquals("nautaticket_test", base.getDatabaseName(), "Se requiere la base desechable nautaticket_test");
        String nuevoEsquema = "prueba_" + UUID.randomUUID().toString().replace("-", "");
        try (var conexion = base.getConnection(); var sentencia = conexion.createStatement()) {
            sentencia.execute("CREATE SCHEMA " + nuevoEsquema);
            esquema = nuevoEsquema;
        }
        datos = dataSource(url);
        datos.setCurrentSchema(esquema);
        datos.setApplicationName(esquema);

        aplicar("02_schema.sql");
        aplicar("03_seed.sql");
        aplicar("04_migracion_hibernate.sql");
        tecnicoA = usuario("a", "TECNICO");
        tecnicoB = usuario("b", "TECNICO");
        solicitante = usuario("solicitante", "EMPLEADO");
    }

    @AfterEach
    void retirarEsquema() throws SQLException {
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
    void dosTecnicosEsperanYRespetanFechaEId() throws Exception {
        verificarTomaConcurrente(false);
    }

    @Test
    void muestraTicketsNuevosYLimitaTresAsignacionesHastaResolver() throws Exception {
        aplicar(MIGRACION);
        int primero = ticket("PENDIENTE", TEMPRANO);
        int segundo = ticket("PENDIENTE", TEMPRANO);
        int tercero = ticket("PENDIENTE", TEMPRANO);
        int cuarto = ticket("PENDIENTE", TEMPRANO);
        var atencion = iniciarHibernate();

        assertEquals(List.of(primero, segundo, tercero, cuarto),
                atencion.listarPendientes().stream().map(AsignacionTicketDao.TicketFila::idTicket).toList());
        assertTrue(atencion.listarMisTickets(tecnicoA).isEmpty());
        assertEquals(0, atencion.contarTicketsActivos(tecnicoA));

        assertEquals(primero, atencion.tomarSiguienteTicket(tecnicoA));
        assertEquals(segundo, atencion.tomarSiguienteTicket(tecnicoA));
        assertEquals(tercero, atencion.tomarSiguienteTicket(tecnicoA));
        assertEquals(3, atencion.contarTicketsActivos(tecnicoA));
        assertEquals(3, atencion.listarMisTickets(tecnicoA).size());
        assertEquals(cuarto, atencion.listarPendientes().getFirst().idTicket());
        assertThrows(IllegalStateException.class, () -> atencion.tomarSiguienteTicket(tecnicoA));
        assertEquals(3, entero("SELECT COUNT(*) FROM historial_tickets"));
        assertEquals(cuarto, atencion.listarPendientes().getFirst().idTicket());

        atencion.iniciarAtencion(primero, tecnicoA);
        assertEquals(3, atencion.contarTicketsActivos(tecnicoA));
        atencion.resolverTicket(primero, tecnicoA, "Resuelto");
        assertEquals(2, atencion.contarTicketsActivos(tecnicoA));
        assertEquals(cuarto, atencion.tomarSiguienteTicket(tecnicoA));
        assertEquals(3, atencion.contarTicketsActivos(tecnicoA));
    }

    @Test
    void dosTomasDelMismoTecnicoSoloPuedenOcuparUltimoEspacio() throws Exception {
        aplicar(MIGRACION);
        ticket("PENDIENTE", TEMPRANO);
        ticket("PENDIENTE", TEMPRANO);
        int tercero = ticket("PENDIENTE", TEMPRANO);
        int cuarto = ticket("PENDIENTE", TEMPRANO);
        var atencion = iniciarHibernate();
        atencion.tomarSiguienteTicket(tecnicoA);
        atencion.tomarSiguienteTicket(tecnicoA);

        try (var hilos = Executors.newFixedThreadPool(2)) {
            var tomaA = hilos.submit(() -> atencion.tomarSiguienteTicket(tecnicoA));
            var tomaB = hilos.submit(() -> atencion.tomarSiguienteTicket(tecnicoA));
            int exitos = 0;
            int rechazos = 0;
            for (var toma : List.of(tomaA, tomaB)) {
                try {
                    assertEquals(tercero, toma.get(10, TimeUnit.SECONDS));
                    exitos++;
                } catch (ExecutionException error) {
                    assertInstanceOf(IllegalStateException.class, error.getCause());
                    rechazos++;
                }
            }
            assertEquals(1, exitos);
            assertEquals(1, rechazos);
        }
        assertEquals(3, atencion.contarTicketsActivos(tecnicoA));
        assertEquals(cuarto, atencion.listarPendientes().getFirst().idTicket());
        assertEquals(3, entero("SELECT COUNT(*) FROM historial_tickets"));
    }

    @Test
    void siLaPrimeraTomaRevierteElSegundoTecnicoRecibeEseTicket() throws Exception {
        verificarTomaConcurrente(true);
    }

    @Test
    void postgresRechazaUnaSegundaAsignacionActivaYConservaLasCerradas() throws Exception {
        aplicar(MIGRACION);
        int ticket = ticket("PENDIENTE", TEMPRANO);
        asignacion(ticket, tecnicoA, "TIMESTAMP '" + TARDE + "'");
        asignacion(ticket, tecnicoB, "NULL");
        var error = assertThrows(SQLException.class, () -> asignacion(ticket, tecnicoA, "NULL"));
        assertEquals("23505", error.getSQLState());
        assertEquals(2, entero("SELECT COUNT(*) FROM asignaciones_ticket"));
        assertEquals(1, entero("SELECT COUNT(*) FROM asignaciones_ticket WHERE fecha_fin IS NULL"));
    }

    @Test
    void iniciarYResolverRegistraCambiosYCierraSoloLaAsignacionActiva() throws Exception {
        aplicar(MIGRACION);
        int ticket = ticket("PENDIENTE", TEMPRANO);
        asignacion(ticket, tecnicoB, "TIMESTAMP '" + TARDE + "'");
        var atencion = iniciarHibernate();
        assertThrows(IllegalArgumentException.class, () -> atencion.tomarSiguienteTicket(solicitante));
        assertEquals(ticket, atencion.tomarSiguienteTicket(tecnicoA));
        assertThrows(IllegalStateException.class, () -> atencion.iniciarAtencion(ticket, tecnicoB));
        atencion.iniciarAtencion(ticket, tecnicoA);
        assertThrows(IllegalStateException.class, () -> atencion.iniciarAtencion(ticket, tecnicoA));
        assertThrows(IllegalArgumentException.class, () -> atencion.resolverTicket(ticket, tecnicoA, " "));
        atencion.resolverTicket(ticket, tecnicoA, "  Solución de prueba  ");
        assertThrows(IllegalStateException.class, () -> atencion.resolverTicket(ticket, tecnicoA, "Repetición"));

        var resuelto = new TicketDAO(sesiones).buscarPorId(ticket);
        assertEquals("RESUELTO", resuelto.getEstado().getNombre());
        assertEquals("Solución de prueba", resuelto.getSolucion());
        assertNotNull(resuelto.getFechaResolucion());
        assertEquals(solicitante, resuelto.getSolicitante().getId());
        assertEquals(tecnicoA, resuelto.getTecnico().getId());
        var asignaciones = sesiones.fromSession(session -> session.createSelectionQuery("""
                SELECT a FROM AsignacionTicket a JOIN FETCH a.ticket JOIN FETCH a.tecnico
                WHERE a.ticket.idTicket = :ticket ORDER BY a.idAsignacion
                """, AsignacionTicket.class).setParameter("ticket", ticket).getResultList());
        assertEquals(2, asignaciones.size());
        assertEquals(ticket, asignaciones.getLast().getTicket().getIdTicket());
        assertEquals(tecnicoA, asignaciones.getLast().getTecnico().getId());
        assertEquals(LocalDateTime.parse("2026-01-01T11:00:00"), asignaciones.getFirst().getFechaFin());
        assertNotNull(asignaciones.getLast().getFechaFin());

        var historial = new HistorialService(sesiones);
        var eventos = historial.historialPorTicket(ticket, null, null, null);
        assertEquals(3, eventos.size());
        assertEquals("RESUELTO", eventos.getFirst().getEstadoNuevo());
        assertEquals("EN_PROCESO", eventos.get(1).getEstadoNuevo());
        assertNull(eventos.getLast().getEstadoNuevo());
        assertTrue(eventos.getLast().getComentario().contains("sin asignar"));
        assertTrue(eventos.stream().allMatch(evento -> evento.getUsuario().getId() == tecnicoA));
        assertEquals(1L, historial.totales(null, null, null).get("RESUELTO"));
    }

    @Test
    void resolverRevierteElTicketSiHayCeroOMasDeUnaAsignacionActiva() throws Exception {
        int sinAsignacion = ticket("EN_PROCESO", TEMPRANO);
        int duplicado = ticket("EN_PROCESO", TARDE);
        ejecutar("UPDATE tickets SET id_tecnico = " + tecnicoA);
        asignacion(duplicado, tecnicoA, "NULL");
        asignacion(duplicado, tecnicoA, "NULL");
        var atencion = iniciarHibernate();

        for (int ticket : List.of(sinAsignacion, duplicado)) {
            var error = assertThrows(IllegalStateException.class,
                    () -> atencion.resolverTicket(ticket, tecnicoA, "No debe persistirse"));
            assertTrue(error.getMessage().contains("exactamente una"));
            var original = new TicketDAO(sesiones).buscarPorId(ticket);
            assertEquals("EN_PROCESO", original.getEstado().getNombre());
            assertNull(original.getSolucion());
            assertNull(original.getFechaResolucion());
        }
        assertEquals(2, entero("SELECT COUNT(*) FROM asignaciones_ticket WHERE fecha_fin IS NULL"));
        assertEquals(0, entero("SELECT COUNT(*) FROM historial_tickets"));
    }

    @Test
    void migracionCierraAsignacionesFinalizadasConFechasComprobables() throws Exception {
        int resuelto = ticket("RESUELTO", TEMPRANO);
        int cerrado = ticket("CERRADO", TEMPRANO);
        ejecutar("UPDATE tickets SET id_tecnico = " + tecnicoA);
        ejecutar("UPDATE tickets SET fecha_resolucion = TIMESTAMP '" + TARDE + "' WHERE id_ticket = " + resuelto);
        ejecutar("UPDATE tickets SET fecha_cierre = TIMESTAMP '" + TARDE + "' WHERE id_ticket = " + cerrado);
        asignacion(resuelto, tecnicoA, "NULL");
        asignacion(cerrado, tecnicoA, "NULL");

        aplicar(MIGRACION);

        assertEquals(2, entero("SELECT COUNT(*) FROM asignaciones_ticket WHERE fecha_fin = TIMESTAMP '" + TARDE + "'"));
    }

    @Test
    void migracionRechazaDuplicadosSinCambiarDatos() throws Exception {
        int ticket = ticket("PENDIENTE", TEMPRANO);
        asignacion(ticket, tecnicoA, "NULL");
        asignacion(ticket, tecnicoB, "NULL");

        var error = assertThrows(SQLException.class, () -> aplicar(MIGRACION));

        assertTrue(error.getMessage().contains("más de una asignación activa"));
        assertEquals(2, entero("SELECT COUNT(*) FROM asignaciones_ticket WHERE fecha_fin IS NULL"));
        assertEquals(0, entero("SELECT COUNT(*) WHERE to_regclass('ux_asignaciones_ticket_activa') IS NOT NULL"));
    }

    @Test
    void migracionRevierteElRellenoSiQuedaUnCierreAmbiguo() throws Exception {
        int confiable = ticket("RESUELTO", TEMPRANO);
        int ambiguo = ticket("RESUELTO", TARDE);
        ejecutar("UPDATE tickets SET id_tecnico = " + tecnicoA);
        ejecutar("UPDATE tickets SET fecha_resolucion = TIMESTAMP '" + TARDE + "' WHERE id_ticket = " + confiable);
        asignacion(confiable, tecnicoA, "NULL");
        asignacion(ambiguo, tecnicoA, "NULL");

        var error = assertThrows(SQLException.class, () -> aplicar(MIGRACION));

        assertTrue(error.getMessage().contains("requieren revisión"));
        assertEquals(2, entero("SELECT COUNT(*) FROM asignaciones_ticket WHERE fecha_fin IS NULL"));
        assertEquals(0, entero("SELECT COUNT(*) WHERE to_regclass('ux_asignaciones_ticket_activa') IS NOT NULL"));
    }

    @Test
    void reporteFiltraPorInicioPersonalSinDuplicadosYRespetaFechaYEstado() throws Exception {
        aplicar(MIGRACION);
        int admin = usuario("adminReporte", "ADMIN");
        int resuelto = ticket("RESUELTO", TEMPRANO);
        int cerrado = ticket("CERRADO", "2026-01-02 10:00:00");
        int soloAsignado = ticket("PENDIENTE", "2026-01-02 11:00:00");
        int atendidoPorOtro = ticket("EN_PROCESO", "2026-01-02 12:00:00");
        asignacion(soloAsignado, tecnicoA, "NULL");
        registrarInicio(resuelto, tecnicoA);
        registrarInicio(resuelto, tecnicoA);
        registrarInicio(cerrado, tecnicoA);
        registrarInicio(atendidoPorOtro, tecnicoB);

        iniciarHibernate();
        var dao = new ReporteDAO(sesiones);

        assertEquals(List.of(resuelto, cerrado), idsReporte(dao.obtenerTickets(null, null, null, tecnicoA)));
        assertEquals(List.of(atendidoPorOtro), idsReporte(dao.obtenerTickets(null, null, null, tecnicoB)));
        assertEquals(4, dao.obtenerTickets(null, null, null, admin).size());
        assertEquals(List.of(cerrado), idsReporte(dao.obtenerTickets(
                LocalDate.of(2026, 1, 2), null, null, tecnicoA)));
        assertEquals(List.of(resuelto), idsReporte(dao.obtenerTickets(
                null, null, "RESUELTO", tecnicoA)));
        assertEquals(List.of(cerrado), idsReporte(dao.obtenerTickets(
                null, LocalDate.of(2026, 1, 3), "CERRADO", tecnicoA)));
    }

    @Test
    void reporteRechazaEmpleadoInactivoRolDesconocidoYUsuarioInexistente(@TempDir Path carpeta) throws Exception {
        aplicar(MIGRACION);
        ejecutar("INSERT INTO roles (nombre) VALUES ('INVITADO')");
        int desconocido = usuario("invitadoReporte", "INVITADO");
        ejecutar("UPDATE usuarios SET activo = FALSE WHERE id_usuario = " + tecnicoA);
        iniciarHibernate();
        var dao = new ReporteDAO(sesiones);

        assertThrows(SecurityException.class, () -> dao.obtenerTickets(null, null, null, solicitante));
        assertThrows(SecurityException.class, () -> dao.obtenerTickets(null, null, null, tecnicoA));
        assertThrows(SecurityException.class, () -> dao.obtenerTickets(null, null, null, desconocido));
        assertThrows(SecurityException.class, () -> dao.obtenerTickets(null, null, null, Integer.MAX_VALUE));
        assertThrows(IllegalArgumentException.class, () -> dao.obtenerTickets(null, null, null, 0));
        assertThrows(IllegalArgumentException.class, () -> new ReporteService(sesiones, 0));
        Path destino = carpeta.resolve("empleado.pdf");
        assertThrows(SecurityException.class, () ->
                new ReporteService(sesiones, solicitante).generarReporte(null, null, null, destino));
        assertFalse(Files.exists(destino));
    }

    @Test
    void servicioGeneraPdfAutorizado(@TempDir Path carpeta) throws Exception {
        aplicar(MIGRACION);
        int ticket = ticket("RESUELTO", TEMPRANO);
        registrarInicio(ticket, tecnicoA);
        iniciarHibernate();

        Path destino = carpeta.resolve("atencion-tecnico.pdf");
        new ReporteService(sesiones, tecnicoA).generarReporte(null, null, null, destino);

        assertTrue(Files.size(destino) > 100);
        try (var pdf = Files.newInputStream(destino)) {
            assertArrayEquals("%PDF".getBytes(java.nio.charset.StandardCharsets.US_ASCII),
                    pdf.readNBytes(4));
        }
    }

    private List<Integer> idsReporte(List<ReporteTicketDTO> tickets) {
        return tickets.stream().map(ReporteTicketDTO::getIdTicket).toList();
    }

    private void registrarInicio(int ticket, int tecnico) throws SQLException {
        ejecutar("""
                INSERT INTO historial_tickets
                    (id_ticket, id_usuario, estado_anterior, estado_nuevo)
                VALUES (%d, %d, 'PENDIENTE', 'EN_PROCESO')
                """.formatted(ticket, tecnico));
    }

    private void verificarTomaConcurrente(boolean revertirPrimera) throws Exception {
        aplicar(MIGRACION);
        int tardio = ticket("PENDIENTE", TARDE);
        int primero = ticket("PENDIENTE", TEMPRANO);
        int segundo = ticket("PENDIENTE", TEMPRANO);
        var atencion = iniciarHibernate();
        ejecutar("CREATE TABLE bloqueo_prueba (id INTEGER PRIMARY KEY); INSERT INTO bloqueo_prueba VALUES (1);");
        ejecutar("""
                CREATE FUNCTION bloquear_historial() RETURNS trigger LANGUAGE plpgsql AS $$
                BEGIN
                    IF NEW.id_usuario = %d THEN
                        PERFORM id FROM bloqueo_prueba WHERE id = 1 FOR UPDATE;
                        %s
                    END IF;
                    RETURN NEW;
                END;
                $$;
                CREATE TRIGGER esperar_historial BEFORE INSERT ON historial_tickets
                    FOR EACH ROW EXECUTE FUNCTION bloquear_historial();
                """.formatted(tecnicoA, revertirPrimera ? "RAISE EXCEPTION 'Fallo de historial de prueba';" : ""));

        try (var hilos = Executors.newFixedThreadPool(2);
             Connection control = datos.getConnection();
             var sentencia = control.createStatement()) {
            control.setAutoCommit(false);
            int pidControl;
            try (var resultado = sentencia.executeQuery("SELECT pg_backend_pid() FROM bloqueo_prueba FOR UPDATE")) {
                assertTrue(resultado.next());
                pidControl = resultado.getInt(1);
            }
            var tomaA = hilos.submit(() -> atencion.tomarSiguienteTicket(tecnicoA));
            int pidA = esperarBloqueadoPor(pidControl);
            var tomaB = hilos.submit(() -> atencion.tomarSiguienteTicket(tecnicoB));
            esperarBloqueadoPor(pidA);
            assertEquals(0, entero("SELECT COUNT(*) FROM asignaciones_ticket"));
            control.commit();

            if (revertirPrimera) {
                assertThrows(ExecutionException.class, () -> tomaA.get(5, TimeUnit.SECONDS));
                assertEquals(primero, tomaB.get(5, TimeUnit.SECONDS));
                assertEquals(1, entero("SELECT COUNT(*) FROM historial_tickets"));
                assertEquals(segundo, atencion.listarPendientes().getFirst().idTicket());
            } else {
                assertEquals(primero, tomaA.get(5, TimeUnit.SECONDS));
                assertEquals(segundo, tomaB.get(5, TimeUnit.SECONDS));
                assertEquals(2, entero("SELECT COUNT(*) FROM historial_tickets"));
                assertEquals(tardio, atencion.listarPendientes().getFirst().idTicket());
            }
        }
    }

    private int esperarBloqueadoPor(int bloqueador) throws Exception {
        try (var conexion = datos.getConnection(); var consulta = conexion.prepareStatement("""
                SELECT pid FROM pg_stat_activity
                WHERE application_name = ? AND ? = ANY(pg_blocking_pids(pid))
                """)) {
            consulta.setString(1, esquema);
            consulta.setInt(2, bloqueador);
            long limite = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            do {
                try (var resultado = consulta.executeQuery()) {
                    if (resultado.next()) return resultado.getInt(1);
                }
                Thread.sleep(25);
            } while (System.nanoTime() < limite);
        }
        throw new AssertionError("No se observó una transacción esperando al PID " + bloqueador);
    }

    private AtencionService iniciarHibernate() {
        var configuracion = HibernateUtil.configuracionBase()
                .setProperty("hibernate.default_schema", esquema)
                .setProperty("hibernate.show_sql", "false");
        configuracion.getProperties().put(AvailableSettings.JAKARTA_NON_JTA_DATASOURCE, datos);
        sesiones = configuracion.buildSessionFactory();
        return new AtencionService(sesiones);
    }

    private int usuario(String nombre, String rol) throws SQLException {
        return entero("""
                INSERT INTO usuarios (nombre, apellido, correo, password_hash, id_rol)
                VALUES ('%s', 'Prueba', '%s@nauta.invalid', 'fixture', (SELECT id_rol FROM roles WHERE nombre = '%s'))
                RETURNING id_usuario
                """.formatted(nombre, nombre, rol));
    }

    private int ticket(String estado, String fecha) throws SQLException {
        return entero("""
                INSERT INTO tickets (id_solicitante, id_categoria, titulo, descripcion, id_estado, fecha_creacion)
                VALUES (%d, (SELECT MIN(id_categoria) FROM categorias), 'Prueba', 'Detalle',
                        (SELECT id_estado FROM estados_ticket WHERE nombre = '%s'), TIMESTAMP '%s')
                RETURNING id_ticket
                """.formatted(solicitante, estado, fecha));
    }

    private void asignacion(int ticket, int tecnico, String fechaFin) throws SQLException {
        ejecutar("""
                INSERT INTO asignaciones_ticket (id_ticket, id_tecnico, fecha_asignacion, fecha_fin)
                VALUES (%d, %d, TIMESTAMP '%s', %s)
                """.formatted(ticket, tecnico, TEMPRANO, fechaFin));
    }

    private static PGSimpleDataSource dataSource(String url) {
        var ds = new PGSimpleDataSource();
        ds.setURL(url);
        ds.setUser(System.getenv("NAUTATICKET_TEST_DB_USER"));
        ds.setPassword(System.getenv("NAUTATICKET_TEST_DB_PASSWORD"));
        return ds;
    }

    private void aplicar(String archivo) throws Exception {
        ejecutar(Files.readString(Path.of("database", archivo)));
    }

    private void ejecutar(String sql) throws SQLException {
        try (var conexion = datos.getConnection(); var sentencia = conexion.createStatement()) {
            sentencia.execute(sql);
        }
    }

    private int entero(String sql) throws SQLException {
        try (var conexion = datos.getConnection(); var sentencia = conexion.createStatement();
             var resultado = sentencia.executeQuery(sql)) {
            assertTrue(resultado.next());
            return resultado.getInt(1);
        }
    }
}
