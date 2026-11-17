package ni.edu.uam.nautaticket.bridge;

import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.concurrent.Task;
import javafx.scene.web.WebEngine;
import ni.edu.uam.nautaticket.model.HistorialTicket;
import ni.edu.uam.nautaticket.model.Usuario;
import ni.edu.uam.nautaticket.service.HistorialService;
import ni.edu.uam.nautaticket.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

public class WebHistorialBridge {

    private static final Logger LOG =
            Logger.getLogger(WebHistorialBridge.class.getName());

    private static final Set<String> ESTADOS = Set.of(
            "PENDIENTE",
            "EN_PROCESO",
            "RESUELTO",
            "CERRADO"
    );

    private static final DateTimeFormatter FORMATO =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final WebEngine engine;
    private final String paginaPermitida;
    private final Integer usuarioId;
    private final SessionFactory sessionFactory;
    private final ObjectMapper mapper = new ObjectMapper();

    private final AtomicBoolean activo =
            new AtomicBoolean(true);

    private boolean procesando = false;

    public WebHistorialBridge(
            WebEngine engine,
            String paginaPermitida,
            Usuario usuario
    ) {
        if (usuario == null || usuario.getId() == null) {
            throw new SecurityException(
                    "Se necesita una sesión autenticada."
            );
        }

        this.engine = engine;
        this.paginaPermitida = paginaPermitida;
        this.usuarioId = usuario.getId();
        this.sessionFactory =
                HibernateUtil.getSessionFactory();
    }

    public void desactivar() {
        activo.set(false);
    }

    public void buscar(
            String ticket,
            String desde,
            String hasta,
            String estado
    ) {
        if (!paginaAutorizada()) {
            return;
        }

        if (procesando) {
            return;
        }

        procesando = true;

        Task<Map<String, Object>> tarea = new Task<>() {

            @Override
            protected Map<String, Object> call() {

                verificarActivo();

                Integer idTicket = leerTicket(ticket);

                LocalDate fechaDesde = leerFecha(
                        desde,
                        "inicial"
                );

                LocalDate fechaHasta = leerFecha(
                        hasta,
                        "final"
                );

                String estadoNormalizado =
                        leerEstado(estado);

                if (fechaDesde != null
                        && fechaHasta != null
                        && fechaDesde.isAfter(fechaHasta)) {

                    throw new IllegalArgumentException(
                            "La fecha inicial no puede ser posterior a la final."
                    );
                }

                obtenerAlcanceAutorizado();

                HistorialService servicio =
                        new HistorialService(sessionFactory, usuarioId);

                List<HistorialTicket> movimientos =
                        servicio.buscar(
                                idTicket,
                                fechaDesde,
                                fechaHasta,
                                estadoNormalizado
                        );

                List<Map<String, Object>> filas =
                        new ArrayList<>();

                for (HistorialTicket movimiento : movimientos) {

                    Map<String, Object> fila =
                            new LinkedHashMap<>();

                    fila.put(
                            "id",
                            movimiento.getIdHistorial()
                    );

                    fila.put(
                            "ticketId",
                            movimiento.getTicket() == null
                                    ? null
                                    : movimiento.getTicket()
                                    .getIdTicket()
                    );

                    fila.put(
                            "fecha",
                            movimiento.getFechaCambio() == null
                                    ? ""
                                    : movimiento.getFechaCambio()
                                    .format(FORMATO)
                    );

                    Usuario actor = movimiento.getUsuario();

                    fila.put(
                            "usuario",
                            actor == null
                                    ? "Usuario desconocido"
                                    : (
                                    valor(actor.getNombre()) + " " +
                                            valor(actor.getApellido())
                            ).trim()
                    );

                    fila.put(
                            "estadoAnterior",
                            valor(movimiento.getEstadoAnterior())
                    );

                    fila.put(
                            "estadoNuevo",
                            valor(movimiento.getEstadoNuevo())
                    );

                    fila.put(
                            "comentario",
                            valor(movimiento.getComentario())
                    );

                    filas.add(fila);
                }

                Map<String, Object> datos =
                        new LinkedHashMap<>();

                datos.put("movimientos", filas);
                datos.put("total", filas.size());

                return datos;
            }
        };

        tarea.setOnSucceeded(evento -> {
            procesando = false;

            enviarRespuesta(
                    "nautaHistorialDataReady",
                    tarea.getValue()
            );
        });

        tarea.setOnFailed(evento -> {
            procesando = false;

            Throwable error = tarea.getException();

            LOG.log(
                    Level.WARNING,
                    "Error consultando historial",
                    error
            );

            String mensaje =
                    error instanceof IllegalArgumentException
                            || error instanceof SecurityException
                            ? error.getMessage()
                            : "No se pudo consultar el historial.";

            enviarRespuesta(
                    "nautaHistorialDataError",
                    mensaje
            );
        });

        Thread hilo = new Thread(
                tarea,
                "nautaticket-historial-consulta"
        );

        hilo.setDaemon(true);
        hilo.start();
    }

    private Integer obtenerAlcanceAutorizado() {

        try (Session session =
                     sessionFactory.openSession()) {

            Usuario actual = session.createSelectionQuery("""
                    SELECT u FROM Usuario u
                    JOIN FETCH u.rol
                    WHERE u.id = :id
                      AND u.activo = true
                    """, Usuario.class)
                    .setParameter("id", usuarioId)
                    .uniqueResult();

            if (actual == null || actual.getRol() == null) {
                throw new SecurityException(
                        "La sesión ya no está autorizada."
                );
            }

            String rol = actual.getRol().getNombre();

            if ("ADMIN".equalsIgnoreCase(rol)
                    || "TECNICO".equalsIgnoreCase(rol)) {

                return null;
            }

            throw new SecurityException(
                    "No tienes permiso para consultar el historial general."
            );
        }
    }

    private Integer leerTicket(String texto) {

        if (texto == null || texto.isBlank()) {
            return null;
        }

        try {
            int id = Integer.parseInt(texto.trim());

            if (id <= 0) {
                throw new NumberFormatException();
            }

            return id;

        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "El ID del ticket debe ser un entero positivo."
            );
        }
    }

    private LocalDate leerFecha(
            String texto,
            String nombre
    ) {

        if (texto == null || texto.isBlank()) {
            return null;
        }

        try {
            return LocalDate.parse(texto.trim());

        } catch (RuntimeException e) {
            throw new IllegalArgumentException(
                    "La fecha " + nombre + " no es válida."
            );
        }
    }

    private String leerEstado(String estado) {

        if (estado == null
                || estado.isBlank()
                || "TODOS".equalsIgnoreCase(estado)) {
            return null;
        }

        String normalizado = estado.trim().toUpperCase();

        if (!ESTADOS.contains(normalizado)) {
            throw new IllegalArgumentException(
                    "Estado no válido."
            );
        }

        return normalizado;
    }

    private String valor(String texto) {
        return texto == null ? "" : texto;
    }

    private void verificarActivo() {

        if (!activo.get()) {
            throw new SecurityException(
                    "La sesión ya no está disponible."
            );
        }
    }

    private boolean paginaAutorizada() {

        return activo.get()
                && paginaPermitida.equals(
                engine.getLocation()
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

            String json = mapper.writeValueAsString(datos);
            String argumento =
                    mapper.writeValueAsString(json);

            engine.executeScript(
                    "if (typeof window." + funcion +
                            " === 'function') {" +
                            "window." + funcion +
                            "(JSON.parse(" + argumento + "));" +
                            "}"
            );

        } catch (Exception e) {

            LOG.log(
                    Level.SEVERE,
                    "Error enviando historial a WebView",
                    e
            );
        }
    }
}
