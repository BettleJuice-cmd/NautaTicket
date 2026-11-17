
package ni.edu.uam.nautaticket.bridge;

import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.concurrent.Task;
import javafx.scene.web.WebEngine;
import ni.edu.uam.nautaticket.dao.AsignacionTicketDao.TicketFila;
import ni.edu.uam.nautaticket.model.Usuario;
import ni.edu.uam.nautaticket.service.AtencionService;
import ni.edu.uam.nautaticket.service.SesionAutenticada;
import ni.edu.uam.nautaticket.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

public class WebAtencionBridge {

    private static final Logger LOGGER =
            Logger.getLogger(WebAtencionBridge.class.getName());

    private static final DateTimeFormatter FORMATO =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final WebEngine engine;
    private final String paginaPermitida;
    private final int tecnicoId;
    private final SesionAutenticada sesionAutenticada;
    private final SessionFactory sessionFactory;
    private final ObjectMapper mapper = new ObjectMapper();

    private final AtomicBoolean activo = new AtomicBoolean(true);

    private boolean procesando = false;

    public WebAtencionBridge(
            WebEngine engine,
            String paginaPermitida,
            Usuario usuario,
            SesionAutenticada sesionAutenticada
    ) {
        if (usuario == null || usuario.getId() == null) {
            throw new SecurityException(
                    "No existe una sesión válida."
            );
        }

        this.engine = engine;
        this.paginaPermitida = paginaPermitida;
        this.tecnicoId = usuario.getId();
        if (sesionAutenticada == null || sesionAutenticada.usuarioId() != tecnicoId) {
            throw new SecurityException("La identidad autenticada no corresponde al técnico.");
        }
        this.sesionAutenticada = sesionAutenticada;
        this.sessionFactory = HibernateUtil.getSessionFactory();
    }

    public void desactivar() {
        activo.set(false);
    }

    public void listarDatos() {

        if (!paginaAutorizada() || procesando) {
            return;
        }

        procesando = true;

        Task<Map<String, Object>> tarea = new Task<>() {

            @Override
            protected Map<String, Object> call() {

                verificarTecnico();

                AtencionService servicio =
                        new AtencionService(sessionFactory, sesionAutenticada);

                return construirDatos(servicio);
            }
        };

        tarea.setOnSucceeded(evento -> {

            procesando = false;

            enviarRespuesta(
                    "nautaAtencionDataReady",
                    tarea.getValue()
            );
        });

        tarea.setOnFailed(evento -> {

            procesando = false;

            LOGGER.log(
                    Level.SEVERE,
                    "No se pudo consultar la atención",
                    tarea.getException()
            );

            String mensaje = mensajeError(
                    tarea.getException(),
                    "No se pudieron cargar los tickets."
            );

            enviarRespuesta(
                    "nautaAtencionDataError",
                    mensaje
            );
        });

        iniciarTarea(tarea, "nautaticket-atencion-consulta");
    }

    public void ejecutarAccion(
            String accion,
            int ticketId,
            String solucion
    ) {

        if (!paginaAutorizada()) {
            return;
        }

        if (procesando) {
            enviarResultado(
                    false,
                    "Hay otra operación en proceso."
            );
            return;
        }

        if (accion == null) {
            enviarResultado(false, "Operación inválida.");
            return;
        }

        if (solucion != null && solucion.length() > 10000) {
            enviarResultado(
                    false,
                    "La solución es demasiado extensa."
            );
            return;
        }

        procesando = true;

        Task<String> tarea = new Task<>() {

            @Override
            protected String call() {

                verificarTecnico();

                AtencionService servicio =
                        new AtencionService(sessionFactory, sesionAutenticada);

                return switch (accion) {

                    case "tomar" -> {

                        int asignado =
                                servicio.tomarSiguienteTicket(tecnicoId);

                        yield "Ticket #" + asignado
                                + " asignado correctamente.";
                    }

                    case "iniciar" -> {

                        validarTicket(ticketId);

                        servicio.iniciarAtencion(
                                ticketId,
                                tecnicoId
                        );

                        yield "Atención iniciada correctamente.";
                    }

                    case "resolver" -> {

                        validarTicket(ticketId);

                        if (solucion == null || solucion.isBlank()) {
                            throw new IllegalArgumentException(
                                    "Escribe la solución del ticket."
                            );
                        }

                        servicio.resolverTicket(
                                ticketId,
                                tecnicoId,
                                solucion
                        );

                        yield "Ticket resuelto correctamente.";
                    }

                    default -> throw new IllegalArgumentException(
                            "Operación no permitida."
                    );
                };
            }
        };

        tarea.setOnSucceeded(evento -> {

            procesando = false;

            enviarResultado(
                    true,
                    tarea.getValue()
            );
        });

        tarea.setOnFailed(evento -> {

            procesando = false;

            Throwable error = tarea.getException();

            LOGGER.log(
                    Level.WARNING,
                    "No se completó la operación de atención",
                    error
            );

            enviarResultado(
                    false,
                    mensajeError(
                            error,
                            "No se pudo completar la operación."
                    )
            );
        });

        iniciarTarea(tarea, "nautaticket-atencion-operacion");
    }

    private Map<String, Object> construirDatos(
            AtencionService servicio
    ) {

        List<Map<String, Object>> pendientes =
                convertirFilas(servicio.listarPendientes());

        List<Map<String, Object>> asignados =
                convertirFilas(
                        servicio.listarMisTickets(tecnicoId)
                );

        // Nuevo: consultar la capacidad desde el
        // servicio que aplica las reglas reales FIFO.
        int activos =
                servicio.contarTicketsActivos(tecnicoId);

        int maximo =
                AtencionService.MAX_TICKETS_ACTIVOS;

        int disponibles =
                Math.max(0, maximo - activos);

        Map<String, Object> datos =
                new LinkedHashMap<>();

        datos.put("pendientes", pendientes);
        datos.put("asignados", asignados);

        datos.put("totalPendientes", pendientes.size());
        datos.put("totalAsignados", asignados.size());

        // Información nueva para el frontend.
        datos.put("ticketsActivos", activos);
        datos.put("capacidadMaxima", maximo);
        datos.put("espaciosDisponibles", disponibles);
        datos.put("capacidadLlena", activos >= maximo);

        return datos;
    }

    private List<Map<String, Object>> convertirFilas(
            List<TicketFila> filas
    ) {

        List<Map<String, Object>> resultado =
                new ArrayList<>();

        for (TicketFila ticket : filas) {

            Map<String, Object> fila =
                    new LinkedHashMap<>();

            fila.put("id", ticket.idTicket());
            fila.put("titulo", ticket.titulo());
            fila.put("estado", ticket.estado());

            fila.put(
                    "fecha",
                    ticket.fechaCreacion() == null
                            ? ""
                            : ticket.fechaCreacion().format(FORMATO)
            );

            resultado.add(fila);
        }

        return resultado;
    }

    private void verificarTecnico() {

        if (!activo.get()) {
            throw new SecurityException(
                    "La sesión ya no está disponible."
            );
        }

        try (Session session =
                     sessionFactory.openSession()) {

            Usuario usuario =
                    session.get(Usuario.class, tecnicoId);

            if (usuario == null
                    || !usuario.isActivo()
                    || usuario.getRol() == null
                    || !"TECNICO".equalsIgnoreCase(
                    usuario.getRol().getNombre())) {

                throw new SecurityException(
                        "Solo los técnicos TI activos "
                                + "pueden atender tickets."
                );
            }
        }
    }

    private void validarTicket(int id) {

        if (id <= 0) {
            throw new IllegalArgumentException(
                    "Selecciona un ticket válido."
            );
        }
    }

    private String mensajeError(
            Throwable error,
            String predeterminado
    ) {

        if ((error instanceof IllegalArgumentException
                || error instanceof IllegalStateException
                || error instanceof SecurityException)
                && error.getMessage() != null
                && !error.getMessage().isBlank()) {

            return error.getMessage();
        }

        return predeterminado;
    }

    private boolean paginaAutorizada() {

        return activo.get()
                && paginaPermitida.equals(engine.getLocation());
    }

    private void enviarResultado(
            boolean correcto,
            String mensaje
    ) {

        Map<String, Object> resultado =
                new LinkedHashMap<>();

        resultado.put("correcto", correcto);
        resultado.put("mensaje", mensaje);

        enviarRespuesta(
                "nautaAtencionActionResult",
                resultado
        );
    }

    private void enviarRespuesta(
            String funcion,
            Object datos
    ) {

        if (!paginaAutorizada()) {
            return;
        }

        try {

            String json =
                    mapper.writeValueAsString(datos);

            String argumento =
                    mapper.writeValueAsString(json);

            engine.executeScript(
                    "if (typeof window." + funcion
                            + " === 'function') {"
                            + "window." + funcion
                            + "(JSON.parse(" + argumento + "));"
                            + "}"
            );

        } catch (Exception e) {

            LOGGER.log(
                    Level.SEVERE,
                    "Error comunicando con WebView",
                    e
            );
        }
    }

    private void iniciarTarea(
            Task<?> tarea,
            String nombre
    ) {

        Thread hilo = new Thread(tarea, nombre);
        hilo.setDaemon(true);
        hilo.start();
    }
}
