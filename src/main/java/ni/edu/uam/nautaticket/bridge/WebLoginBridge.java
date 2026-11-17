
package ni.edu.uam.nautaticket.bridge;

import javafx.concurrent.Task;
import javafx.scene.web.WebEngine;

import ni.edu.uam.nautaticket.controller.PerfilAcceso;
import ni.edu.uam.nautaticket.service.AuthService;
import ni.edu.uam.nautaticket.service.AuthService.AccesoAutenticado;

import java.util.function.BiConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class WebLoginBridge {

    private static final Logger LOGGER =
            Logger.getLogger(WebLoginBridge.class.getName());

    private final WebEngine engine;
    private final String paginaPermitida;
    private final BiConsumer<AccesoAutenticado, String> alAutenticar;

    private boolean procesando;

    public WebLoginBridge(
            WebEngine engine,
            String paginaPermitida,
            BiConsumer<AccesoAutenticado, String> alAutenticar
    ) {
        this.engine = engine;
        this.paginaPermitida = paginaPermitida;
        this.alAutenticar = alAutenticar;
    }

    public void iniciarSesion(
            String correo,
            String password,
            String perfil
    ) {
        if (procesando) {
            return;
        }

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

        String correoNormalizado = correo.trim();

        procesando = true;

        Task<AccesoAutenticado> tarea = new Task<>() {

            @Override
            protected AccesoAutenticado call() {
                AuthService servicio = new AuthService();

                return servicio.iniciarSesionConContexto(
                        correoNormalizado,
                        password
                );
            }
        };

        tarea.setOnSucceeded(event -> {

            procesando = false;

            try {
                AccesoAutenticado acceso = tarea.getValue();
                var usuario = acceso == null ? null : acceso.usuario();

                if (usuario == null
                        || usuario.getRol() == null
                        || !perfilSeleccionado.coincideConRol(
                        usuario.getRol().getNombre()
                )) {

                    if (acceso != null) acceso.sesion().cerrar();
                    mostrarError(
                            "Correo, contraseña o perfil incorrectos."
                    );
                    return;
                }

                alAutenticar.accept(
                        acceso,
                        nombrePerfil(perfil)
                );

            } catch (Exception e) {
                LOGGER.log(
                        Level.SEVERE,
                        "Error completando el acceso",
                        e
                );

                mostrarError(
                        "No se pudo completar el inicio de sesión."
                );
            }
        });

        tarea.setOnFailed(event -> {

            procesando = false;

            LOGGER.log(
                    Level.SEVERE,
                    "Error de autenticación",
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

    private String nombrePerfil(String perfil) {

        return switch (perfil) {
            case "ADMIN" -> "Administrador";
            case "TECNICO" -> "Soporte técnico";
            case "EMPLEADO" -> "Empleado";
            default -> "Usuario";
        };
    }

    private void mostrarError(String mensaje) {

        if (!paginaPermitida.equals(engine.getLocation())) {
            return;
        }

        String mensajeSeguro = switch (mensaje) {

            case "Completa los campos y selecciona un perfil." ->
                    "Completa los campos y selecciona un perfil.";

            case "Correo, contraseña o perfil incorrectos." ->
                    "Correo, contraseña o perfil incorrectos.";

            default ->
                    "No se pudo completar el inicio de sesión.";
        };

        engine.executeScript(
                "if (typeof window.nautaLoginError === 'function') {" +
                        "window.nautaLoginError('" + mensajeSeguro + "');" +
                        "}"
        );
    }
}
