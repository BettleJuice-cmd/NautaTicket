package ni.edu.uam.nautaticket.bridge;

import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.stage.Stage;

import ni.edu.uam.nautaticket.NautaTicketApp;
import ni.edu.uam.nautaticket.controller.PanelController;
import ni.edu.uam.nautaticket.controller.PerfilAcceso;
import ni.edu.uam.nautaticket.model.Usuario;
import ni.edu.uam.nautaticket.service.AuthService;

import java.util.logging.Level;
import java.util.logging.Logger;

public class WebLoginBridge {

    private static final Logger LOGGER =
            Logger.getLogger(WebLoginBridge.class.getName());

    private final WebEngine engine;
    private final Stage stage;
    private final String paginaPermitida;

    private boolean procesando = false;

    public WebLoginBridge(
            WebEngine engine,
            Stage stage,
            String paginaPermitida
    ) {
        this.engine = engine;
        this.stage = stage;
        this.paginaPermitida = paginaPermitida;
    }

    public void iniciarSesion(
            String correo,
            String password,
            String perfil
    ) {

        if (procesando) {
            return;
        }

        // El puente solamente acepta solicitudes
        // desde la pantalla local de acceso.
        if (!paginaPermitida.equals(engine.getLocation())) {
            return;
        }

        PerfilAcceso perfilSeleccionado =
                obtenerPerfil(perfil);

        if (perfilSeleccionado == null
                || correo == null
                || correo.isBlank()
                || password == null
                || password.isBlank()) {

            mostrarError(
                    "Completa los campos y selecciona un perfil."
            );
            return;
        }

        procesando = true;

        Task<Usuario> tarea = new Task<>() {

            @Override
            protected Usuario call() {
                AuthService servicio = new AuthService();

                return servicio.iniciarSesion(
                        correo,
                        password
                );
            }
        };

        tarea.setOnSucceeded(event -> {

            procesando = false;

            Usuario usuario = tarea.getValue();

            if (usuario == null
                    || usuario.getRol() == null
                    || !perfilSeleccionado.coincideConRol(
                    usuario.getRol().getNombre()
            )) {

                mostrarError(
                        "Correo, contraseña o perfil incorrectos."
                );
                return;
            }

            abrirPanel(usuario);
        });

        tarea.setOnFailed(event -> {

            procesando = false;

            LOGGER.log(
                    Level.SEVERE,
                    "Error durante la autenticación",
                    tarea.getException()
            );

            mostrarError(
                    "No se pudo completar el inicio de sesión."
            );
        });

        Thread hilo = new Thread(
                tarea,
                "nautaticket-autenticacion"
        );

        hilo.setDaemon(true);
        hilo.start();
    }

    private PerfilAcceso obtenerPerfil(String perfil) {

        if (perfil == null) {
            return null;
        }

        return switch (perfil) {
            case "ADMIN" ->
                    PerfilAcceso.ADMINISTRADOR;

            case "TECNICO" ->
                    PerfilAcceso.PERSONAL_TI;

            case "EMPLEADO" ->
                    PerfilAcceso.EMPLEADO;

            default -> null;
        };
    }

    private void abrirPanel(Usuario usuario) {

        try {
            FXMLLoader loader = new FXMLLoader(
                    NautaTicketApp.class.getResource(
                            "panel.fxml"
                    )
            );

            Parent root = loader.load();

            PanelController controller =
                    loader.getController();

            controller.iniciar(usuario);

            Scene scene = new Scene(root);

            stage.setScene(scene);
            stage.setTitle("NautaTicket");
            stage.sizeToScene();
            stage.centerOnScreen();

        } catch (Exception e) {

            LOGGER.log(
                    Level.SEVERE,
                    "No se pudo abrir el panel principal",
                    e
            );

            mostrarError(
                    "El acceso fue validado, pero no se pudo " +
                            "abrir el panel principal."
            );
        }
    }

    private void mostrarError(String mensaje) {

        if (engine.getLoadWorker().isRunning()) {
            return;
        }

        // Solo se envían mensajes fijos a JavaScript.
        // Nunca se devuelven contraseñas ni hashes.
        String mensajeSeguro = switch (mensaje) {
            case "Completa los campos y selecciona un perfil." ->
                    "Completa los campos y selecciona un perfil.";

            case "Correo, contraseña o perfil incorrectos." ->
                    "Correo, contraseña o perfil incorrectos.";

            case "El acceso fue validado, pero no se pudo abrir el panel principal." ->
                    "El acceso fue validado, pero no se pudo abrir el panel principal.";

            default ->
                    "No se pudo completar el inicio de sesión.";
        };

        engine.executeScript(
                "window.nautaLoginError(" +
                        "'" + mensajeSeguro + "'" +
                        ");"
        );
    }
}
