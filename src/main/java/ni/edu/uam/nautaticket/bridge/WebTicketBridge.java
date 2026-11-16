package ni.edu.uam.nautaticket.bridge;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.concurrent.Task;
import javafx.scene.web.WebEngine;
import ni.edu.uam.nautaticket.dao.CategoriaDAO;
import ni.edu.uam.nautaticket.dao.TicketDAO;
import ni.edu.uam.nautaticket.dao.UsuarioDao;
import ni.edu.uam.nautaticket.model.Categoria;
import ni.edu.uam.nautaticket.model.Ticket;
import ni.edu.uam.nautaticket.model.Usuario;
import ni.edu.uam.nautaticket.service.TicketService;
import org.hibernate.SessionFactory;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

public class WebTicketBridge {

    private static final Logger LOGGER =
            Logger.getLogger(WebTicketBridge.class.getName());

    private static final DateTimeFormatter FORMATO_FECHA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final WebEngine engine;
    private final String paginaPermitida;
    private final Integer usuarioId;
    private final SessionFactory sessionFactory;
    private final ObjectMapper mapper = new ObjectMapper();

    private final AtomicBoolean activo =
            new AtomicBoolean(true);

    private boolean procesando;

    public WebTicketBridge(
            WebEngine engine,
            String paginaPermitida,
            Usuario usuario,
            SessionFactory sessionFactory
    ) {
        if (usuario == null || usuario.getId() == null) {
            throw new IllegalArgumentException(
                    "Se necesita un usuario autenticado."
            );
        }

        if (sessionFactory == null) {
            throw new IllegalArgumentException(
                    "Se necesita una conexión Hibernate."
            );
        }

        this.engine = engine;
        this.paginaPermitida = paginaPermitida;
        this.usuarioId = usuario.getId();
        this.sessionFactory = sessionFactory;
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
                verificarActivo();
                Usuario actual = obtenerUsuarioActual();

                TicketService servicio = crearServicio(actual);
                CategoriaDAO categoriaDAO =
                        new CategoriaDAO(sessionFactory);

                List<Map<String, Object>> tickets =
                        new ArrayList<>();

                List<Map<String, Object>> categorias =
                        new ArrayList<>();

                for (Categoria categoria :
                        categoriaDAO.listarActivas()) {

                    Map<String, Object> fila =
                            new LinkedHashMap<>();

                    fila.put("id", categoria.getIdCategoria());
                    fila.put("nombre", categoria.getNombre());

                    categorias.add(fila);
                }

                for (Ticket ticket : servicio.listarTickets()) {

                    Map<String, Object> fila =
                            new LinkedHashMap<>();

                    fila.put("id", ticket.getIdTicket());
                    fila.put("titulo", ticket.getTitulo());
                    fila.put(
                            "descripcion",
                            ticket.getDescripcion()
                    );

                    fila.put(
                            "categoria",
                            ticket.getCategoria() == null
                                    ? "Sin categoría"
                                    : ticket.getCategoria().getNombre()
                    );

                    fila.put(
                            "categoriaId",
                            ticket.getCategoria() == null
                                    ? null
                                    : ticket.getCategoria().getIdCategoria()
                    );

                    fila.put(
                            "estado",
                            ticket.getEstado() == null
                                    ? "Sin estado"
                                    : ticket.getEstado().getNombre()
                    );

                    fila.put(
                            "fecha",
                            ticket.getFechaCreacion() == null
                                    ? ""
                                    : ticket.getFechaCreacion()
                                    .format(FORMATO_FECHA)
                    );

                    tickets.add(fila);
                }

                Map<String, Object> resultado =
                        new LinkedHashMap<>();

                resultado.put("tickets", tickets);
                resultado.put("categorias", categorias);

                return resultado;
            }
        };

        tarea.setOnSucceeded(event -> {

            procesando = false;

            if (paginaAutorizada()) {
                enviarRespuesta(
                        "nautaTicketsDataReady",
                        tarea.getValue()
                );
            }
        });

        tarea.setOnFailed(event -> {

            procesando = false;

            LOGGER.log(
                    Level.SEVERE,
                    "Error consultando tickets",
                    tarea.getException()
            );

            enviarRespuesta(
                    "nautaTicketsDataError",
                    "No se pudieron consultar los tickets."
            );
        });

        iniciarTarea(tarea, "nautaticket-tickets-consulta");
    }

    public void ejecutarAccion(
            String accion,
            String datosJson
    ) {

        if (!paginaAutorizada()) {
            return;
        }

        if (procesando) {
            enviarResultado(
                    false,
                    "Hay una operación en proceso."
            );
            return;
        }

        if (accion == null ||
                datosJson == null ||
                datosJson.length() > 20000) {

            enviarResultado(false, "Solicitud inválida.");
            return;
        }

        procesando = true;

        Task<String> tarea = new Task<>() {

            @Override
            protected String call() throws Exception {

                verificarActivo();

                Usuario actual = obtenerUsuarioActual();

                TicketService servicio = crearServicio(actual);

                JsonNode datos = mapper.readTree(datosJson);

                if (datos == null || !datos.isObject()) {
                    throw new IllegalArgumentException(
                            "Datos inválidos."
                    );
                }

                return ejecutarOperacion(
                        servicio,
                        accion,
                        datos
                );
            }
        };

        tarea.setOnSucceeded(event -> {

            procesando = false;

            if (paginaAutorizada()) {
                enviarResultado(true, tarea.getValue());
            }
        });

        tarea.setOnFailed(event -> {

            procesando = false;

            Throwable error = tarea.getException();

            LOGGER.log(
                    Level.SEVERE,
                    "Error administrando tickets",
                    error
            );

            String mensaje = error instanceof
                    IllegalArgumentException
                    ? error.getMessage()
                    : "No se pudo completar la operación.";

            enviarResultado(false, mensaje);
        });

        iniciarTarea(tarea, "nautaticket-tickets-operacion");
    }

    private String ejecutarOperacion(
            TicketService servicio,
            String accion,
            JsonNode datos
    ) {

        return switch (accion) {

            case "crear" -> {

                Categoria categoria = buscarCategoria(
                        entero(datos, "categoriaId")
                );

                servicio.crearTicket(
                        texto(datos, "titulo"),
                        texto(datos, "descripcion"),
                        categoria,
                        usuarioId
                );

                yield "Ticket registrado correctamente.";
            }

            case "actualizar" -> {

                Categoria categoria = buscarCategoria(
                        entero(datos, "categoriaId")
                );

                boolean actualizado =
                        servicio.actualizarTicket(
                                entero(datos, "id"),
                                texto(datos, "titulo"),
                                texto(datos, "descripcion"),
                                categoria
                        );

                if (!actualizado) {
                    throw new IllegalArgumentException(
                            "El ticket no puede modificarse. " +
                                    "Debe estar pendiente y ser accesible " +
                                    "para tu cuenta."
                    );
                }

                yield "Ticket actualizado correctamente.";
            }

            case "eliminar" -> {

                boolean eliminado =
                        servicio.eliminarTicket(
                                entero(datos, "id")
                        );

                if (!eliminado) {
                    throw new IllegalArgumentException(
                            "No se puede eliminar el ticket. " +
                                    "Debe estar pendiente y no tener historial."
                    );
                }

                yield "Ticket eliminado correctamente.";
            }

            default -> throw new IllegalArgumentException(
                    "Operación no permitida."
            );
        };
    }

    private Categoria buscarCategoria(Integer id) {

        if (id == null) {
            throw new IllegalArgumentException(
                    "Selecciona una categoría."
            );
        }

        return new CategoriaDAO(sessionFactory)
                .listarActivas()
                .stream()
                .filter(categoria ->
                        id.equals(categoria.getIdCategoria())
                )
                .findFirst()
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "La categoría no existe o está inactiva."
                        )
                );
    }

    private Usuario obtenerUsuarioActual() {

        Usuario actual = new UsuarioDao()
                .buscarPorId(usuarioId);

        if (actual == null || !actual.isActivo()) {
            throw new SecurityException(
                    "La sesión ya no está autorizada."
            );
        }

        return actual;
    }

    private TicketService crearServicio(Usuario actual) {

        boolean administrador =
                actual.getRol() != null &&
                        "ADMIN".equalsIgnoreCase(
                                actual.getRol().getNombre()
                        );

        // La cuenta normal solo accede a sus tickets.
        // El administrador utiliza el alcance global
        // que ya contempla TicketDAO.
        Integer filtroSolicitante =
                administrador ? null : usuarioId;

        TicketDAO dao = new TicketDAO(
                sessionFactory,
                filtroSolicitante
        );

        return new TicketService(dao);
    }

    private String texto(JsonNode datos, String campo) {

        JsonNode valor = datos.get(campo);

        if (valor == null || !valor.isTextual()) {
            return "";
        }

        return valor.asText();
    }

    private Integer entero(JsonNode datos, String campo) {

        JsonNode valor = datos.get(campo);

        if (valor == null ||
                !valor.isIntegralNumber() ||
                !valor.canConvertToInt()) {
            throw new IllegalArgumentException(
                    "Identificador inválido: " + campo
            );
        }

        int numero = valor.intValue();

        if (numero <= 0) {
            throw new IllegalArgumentException(
                    "Identificador inválido: " + campo
            );
        }

        return numero;
    }

    private boolean paginaAutorizada() {

        return activo.get()
                && paginaPermitida.equals(engine.getLocation());
    }

    private void verificarActivo() {

        if (!activo.get()) {
            throw new SecurityException(
                    "La sesión ya no está disponible."
            );
        }
    }

    private void enviarResultado(
            boolean correcto,
            String mensaje
    ) {

        Map<String, Object> respuesta =
                new LinkedHashMap<>();

        respuesta.put("correcto", correcto);
        respuesta.put("mensaje", mensaje);

        enviarRespuesta(
                "nautaTicketsActionResult",
                respuesta
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

            LOGGER.log(
                    Level.SEVERE,
                    "Error enviando datos a WebView",
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
