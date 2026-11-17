package ni.edu.uam.nautaticket.bridge;

import com.fasterxml.jackson.databind.ObjectMapper;

import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.web.WebEngine;
import javafx.stage.Stage;

import ni.edu.uam.nautaticket.dto.EstadisticaApiDTO;
import ni.edu.uam.nautaticket.model.Usuario;
import ni.edu.uam.nautaticket.service.EstadisticasApiClient;
import ni.edu.uam.nautaticket.util.HibernateUtil;

import org.hibernate.Session;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

public class WebEstadisticasBridge {

    private static final Logger LOG =
            Logger.getLogger(
                    WebEstadisticasBridge.class.getName()
            );

    private final WebEngine engine;
    private final Stage ventana;
    private final String paginaPermitida;
    private final int usuarioId;

    private final ObjectMapper mapper =
            new ObjectMapper();

    private final AtomicBoolean activo =
            new AtomicBoolean(true);

    private boolean procesando = false;

    public WebEstadisticasBridge(
            WebEngine engine,
            Stage ventana,
            String paginaPermitida,
            Usuario usuario
    ) {

        if (usuario == null ||
                usuario.getId() == null) {

            throw new SecurityException(
                    "Se requiere una sesión autenticada."
            );
        }

        this.engine = engine;
        this.ventana = ventana;
        this.paginaPermitida = paginaPermitida;
        this.usuarioId = usuario.getId();
    }

    public void desactivar() {
        activo.set(false);
    }

    public void consultar() {

        if (!paginaAutorizada() || procesando) {
            return;
        }

        try {
            verificarAdministrador();

        } catch (RuntimeException error) {

            LOG.log(
                    Level.WARNING,
                    "No se pudo validar al administrador",
                    error
            );

            enviarError(
                    "No se pudo validar la sesión administrativa."
            );

            return;
        }

        Dialog<Credenciales> dialogo =
                new Dialog<>();

        dialogo.initOwner(ventana);

        dialogo.setTitle(
                "NautaTicket - Estadísticas API"
        );

        dialogo.setHeaderText(
                "Autenticación de la API"
        );

        ButtonType botonConsultar =
                new ButtonType(
                        "Consultar",
                        ButtonBar.ButtonData.OK_DONE
                );

        dialogo.getDialogPane()
                .getButtonTypes()
                .addAll(
                        botonConsultar,
                        ButtonType.CANCEL
                );

        TextField correo = new TextField();

        correo.setPromptText(
                "Correo del administrador"
        );

        PasswordField password =
                new PasswordField();

        password.setPromptText(
                "Contraseña"
        );

        GridPane campos = new GridPane();

        campos.setHgap(12);
        campos.setVgap(12);
        campos.setPadding(new Insets(12));

        campos.add(
                new Label("Correo:"),
                0,
                0
        );

        campos.add(
                correo,
                1,
                0
        );

        campos.add(
                new Label("Contraseña:"),
                0,
                1
        );

        campos.add(
                password,
                1,
                1
        );

        dialogo.getDialogPane()
                .setContent(campos);

        dialogo.setResultConverter(boton -> {

            if (boton == botonConsultar) {

                return new Credenciales(
                        correo.getText().trim(),
                        password.getText()
                );
            }

            return null;
        });

        Optional<Credenciales> respuesta =
                dialogo.showAndWait();

        if (respuesta.isEmpty()) {
            return;
        }

        Credenciales credenciales =
                respuesta.get();

        if (credenciales.correo().isBlank() ||
                credenciales.password().isBlank()) {

            enviarError(
                    "Ingresa el correo y la contraseña de la API."
            );

            return;
        }

        procesando = true;

        enviarEvento(
                "nautaEstadisticasCargando",
                true
        );

        Task<List<Map<String, Object>>> tarea =
                new Task<>() {

                    @Override
                    protected List<Map<String, Object>> call()
                            throws Exception {

                        verificarActivo();

                        verificarAdministrador();

                        EstadisticasApiClient cliente =
                                new EstadisticasApiClient();

                        List<EstadisticaApiDTO> datos =
                                cliente.consultar(
                                        credenciales.correo(),
                                        credenciales.password()
                                );

                        verificarActivo();

                        List<Map<String, Object>> resultado =
                                new ArrayList<>();

                        for (EstadisticaApiDTO dato : datos) {

                            Map<String, Object> fila =
                                    new LinkedHashMap<>();

                            fila.put(
                                    "estado",
                                    dato.estado()
                            );

                            fila.put(
                                    "total",
                                    dato.total()
                            );

                            resultado.add(fila);
                        }

                        return resultado;
                    }
                };

        tarea.setOnSucceeded(evento -> {

            procesando = false;

            enviarEvento(
                    "nautaEstadisticasDataReady",
                    tarea.getValue()
            );
        });

        tarea.setOnFailed(evento -> {

            procesando = false;

            Throwable error =
                    tarea.getException();

            LOG.log(
                    Level.WARNING,
                    "No fue posible consultar la API",
                    error
            );

            String mensaje;

            if (error instanceof java.io.IOException
                    || error instanceof IllegalArgumentException
                    || error instanceof SecurityException) {

                mensaje = error.getMessage();

            } else if (error instanceof InterruptedException) {

                mensaje =
                        "La consulta fue interrumpida.";

            } else {

                mensaje =
                        "No se pudo conectar con la API.";
            }

            enviarError(mensaje);
        });

        Thread hilo = new Thread(
                tarea,
                "nautaticket-estadisticas-api"
        );

        hilo.setDaemon(true);
        hilo.start();
    }

    private void verificarAdministrador() {

        verificarActivo();

        try (Session session =
                     HibernateUtil
                             .getSessionFactory()
                             .openSession()) {

            // Consultamos directamente la entidad.
            // No necesitamos una consulta HQL.

            Usuario usuario =
                    session.get(
                            Usuario.class,
                            usuarioId
                    );

            if (usuario == null) {

                throw new SecurityException(
                        "El usuario no existe."
                );
            }

            if (!usuario.isActivo()) {

                throw new SecurityException(
                        "El usuario está inactivo."
                );
            }

            if (usuario.getRol() == null) {

                throw new SecurityException(
                        "El usuario no tiene un rol asignado."
                );
            }

            String nombreRol =
                    usuario.getRol().getNombre();

            if (!"ADMIN".equalsIgnoreCase(nombreRol)) {

                throw new SecurityException(
                        "Solo los administradores pueden consultar las estadísticas."
                );
            }
        }
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

    private void enviarError(String mensaje) {

        enviarEvento(
                "nautaEstadisticasDataError",
                mensaje
        );
    }

    private void enviarEvento(
            String funcion,
            Object valor
    ) {

        if (!paginaAutorizada()) {
            return;
        }

        try {

            String json =
                    mapper.writeValueAsString(valor);

            String argumento =
                    mapper.writeValueAsString(json);

            engine.executeScript(
                    "if (typeof window." + funcion +
                            " === 'function') {" +
                            "window." + funcion +
                            "(JSON.parse(" + argumento + "));" +
                            "}"
            );

        } catch (Exception error) {

            LOG.log(
                    Level.SEVERE,
                    "Error mostrando las estadísticas",
                    error
            );
        }
    }

    private record Credenciales(
            String correo,
            String password
    ) {
    }
}
