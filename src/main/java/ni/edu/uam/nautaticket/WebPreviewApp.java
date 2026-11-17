package ni.edu.uam.nautaticket;

import javafx.application.Application;
import javafx.concurrent.Worker;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import netscape.javascript.JSObject;

import ni.edu.uam.nautaticket.bridge.WebAdminBridge;
import ni.edu.uam.nautaticket.bridge.WebAtencionBridge;
import ni.edu.uam.nautaticket.bridge.WebDashboardBridge;
import ni.edu.uam.nautaticket.bridge.WebEstadisticasBridge;
import ni.edu.uam.nautaticket.bridge.WebHistorialBridge;
import ni.edu.uam.nautaticket.bridge.WebLoginBridge;
import ni.edu.uam.nautaticket.bridge.WebReporteBridge;
import ni.edu.uam.nautaticket.bridge.WebTicketBridge;

import ni.edu.uam.nautaticket.model.Usuario;
import ni.edu.uam.nautaticket.util.HibernateUtil;

import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

public class WebPreviewApp extends Application {

    private static final Logger LOGGER =
            Logger.getLogger(WebPreviewApp.class.getName());

    private WebEngine engine;
    private Stage stage;

    private String paginaLogin;
    private String paginaDashboard;

    private WebLoginBridge loginBridge;
    private WebDashboardBridge dashboardBridge;
    private WebAdminBridge adminBridge;
    private WebTicketBridge ticketBridge;
    private WebAtencionBridge atencionBridge;
    private WebHistorialBridge historialBridge;
    private WebReporteBridge reporteBridge;
    private WebEstadisticasBridge estadisticasBridge;

    private Usuario usuarioAutenticado;
    private ni.edu.uam.nautaticket.service.SesionAutenticada sesionAutenticada;
    private String perfilAutenticado;

    @Override
    public void start(Stage stage) {

        this.stage = stage;

        URL recursoLogin =
                getClass().getResource("/web/index.html");

        URL recursoDashboard =
                getClass().getResource("/web/dashboard.html");

        if (recursoLogin == null || recursoDashboard == null) {
            throw new IllegalStateException(
                    "No se encontraron las páginas HTML."
            );
        }

        paginaLogin = recursoLogin.toExternalForm();
        paginaDashboard = recursoDashboard.toExternalForm();

        WebView webView = new WebView();
        webView.setContextMenuEnabled(false);

        engine = webView.getEngine();
        engine.setJavaScriptEnabled(true);

        engine.setOnAlert(event ->
                LOGGER.info("[WEB] " + event.getData())
        );

        engine.getLoadWorker()
                .stateProperty()
                .addListener((observable, anterior, nuevo) -> {

                    if (nuevo == Worker.State.SUCCEEDED) {
                        paginaCargada();
                    }

                    if (nuevo == Worker.State.FAILED) {
                        LOGGER.log(
                                Level.SEVERE,
                                "Error cargando la interfaz HTML",
                                engine.getLoadWorker().getException()
                        );
                    }
                });

        engine.locationProperty().addListener(
                (observable, anterior, nueva) -> {

                    if (nueva == null ||
                            nueva.isBlank() ||
                            "about:blank".equals(nueva)) {
                        return;
                    }

                    boolean login =
                            esMismaPagina(paginaLogin, nueva);

                    boolean dashboard =
                            esMismaPagina(paginaDashboard, nueva);

                    if (!login && !dashboard) {

                        LOGGER.warning(
                                "Navegación no permitida: " + nueva
                        );

                        limpiarSesion();
                        engine.load(paginaLogin);
                    }
                }
        );

        StackPane root = new StackPane(webView);
        root.setStyle("-fx-background-color: #000000;");

        Scene scene = new Scene(
                root,
                1200,
                760,
                Color.BLACK
        );

        stage.setTitle("NautaTicket - Acceso");
        stage.setMinWidth(850);
        stage.setMinHeight(600);
        stage.setScene(scene);
        stage.centerOnScreen();
        stage.show();

        abrirLogin();
    }

    private void paginaCargada() {

        String direccion = engine.getLocation();

        if (esMismaPagina(paginaLogin, direccion)) {

            limpiarSesion();
            registrarLogin(direccion);
            return;
        }

        if (esMismaPagina(paginaDashboard, direccion)) {

            if (!haySesion()) {
                abrirLogin();
                return;
            }

            registrarDashboard(direccion);
            return;
        }

        limpiarSesion();
        abrirLogin();
    }

    private void registrarLogin(String direccion) {

        try {

            loginBridge = new WebLoginBridge(
                    engine,
                    direccion,
                    this::autenticacionCorrecta
            );

            JSObject ventana = (JSObject)
                    engine.executeScript("window");

            ventana.setMember(
                    "nautaBridge",
                    loginBridge
            );

            LOGGER.info("Puente de login registrado.");

        } catch (Exception e) {

            LOGGER.log(
                    Level.SEVERE,
                    "No se pudo registrar el login",
                    e
            );
        }
    }

    private void autenticacionCorrecta(
            ni.edu.uam.nautaticket.service.AuthService.AccesoAutenticado acceso,
            String perfil
    ) {
        Usuario usuario = acceso == null ? null : acceso.usuario();

        if (usuario == null || usuario.getId() == null) {
            return;
        }

        usuarioAutenticado = usuario;
        sesionAutenticada = acceso.sesion();
        perfilAutenticado = perfil;

        LOGGER.info(
                "Usuario autenticado correctamente."
        );

        abrirDashboard();
    }

    private void registrarDashboard(String direccion) {

        try {

            desactivarPuentes();

            JSObject ventana = (JSObject)
                    engine.executeScript("window");

            dashboardBridge = new WebDashboardBridge(
                    engine,
                    direccion,
                    usuarioAutenticado.getCorreo(),
                    perfilAutenticado,
                    this::cerrarSesion
            );

            ventana.setMember(
                    "nautaDashboardBridge",
                    dashboardBridge
            );

            if (esAdministrador()) {

                adminBridge = new WebAdminBridge(
                        engine,
                        direccion,
                        usuarioAutenticado
                );

                ventana.setMember(
                        "nautaAdminBridge",
                        adminBridge
                );

                estadisticasBridge =
                        new WebEstadisticasBridge(
                                engine,
                                stage,
                                direccion,
                                usuarioAutenticado
                        );

                ventana.setMember(
                        "nautaEstadisticasBridge",
                        estadisticasBridge
                );
            }

            if (esAdministrador() || "EMPLEADO".equalsIgnoreCase(
                    usuarioAutenticado.getRol().getNombre())) {
                ticketBridge = new WebTicketBridge(
                        engine, direccion, usuarioAutenticado,
                        HibernateUtil.getSessionFactory());
                ventana.setMember("nautaTicketBridge", ticketBridge);
            }

            if (esTecnico()) {

                atencionBridge = new WebAtencionBridge(
                        engine,
                        direccion,
                        usuarioAutenticado,
                        sesionAutenticada
                );

                ventana.setMember(
                        "nautaAtencionBridge",
                        atencionBridge
                );
            }

            if (esAdministrador() || esTecnico()) {

                historialBridge = new WebHistorialBridge(
                        engine,
                        direccion,
                        usuarioAutenticado
                );

                ventana.setMember(
                        "nautaHistorialBridge",
                        historialBridge
                );

                reporteBridge = new WebReporteBridge(
                        engine,
                        stage,
                        direccion,
                        usuarioAutenticado
                );

                ventana.setMember(
                        "nautaReporteBridge",
                        reporteBridge
                );
            }

            // El backend permite reportes a los
            // perfiles administrativo y técnico.
            engine.executeScript(
                    "document.querySelectorAll(" +
                            "'[data-section=\"reportes\"], " +
                            "#reportes').forEach(function(elemento) {" +
                            "elemento.setAttribute(" +
                            "'data-roles', " +
                            "'Administrador,Soporte técnico');" +
                            "});"
            );

            engine.executeScript(
                    "if (typeof window.nautaDashboardReady " +
                            "=== 'function') {" +
                            "window.nautaDashboardReady();" +
                            "}"
            );

            cargarScript(
                    "/web/js/tickets.js",
                    "js/tickets.js"
            );

            if (esTecnico()) {
                cargarScript(
                        "/web/js/atencion.js",
                        "js/atencion.js"
                );
            }

            if (esAdministrador() || esTecnico()) {

                cargarScript(
                        "/web/js/historial.js",
                        "js/historial.js"
                );

                cargarScript(
                        "/web/js/reportes.js",
                        "js/reportes.js"
                );
            }

            if (esAdministrador()) {

                cargarScript(
                        "/web/js/estadisticas.js",
                        "js/estadisticas.js"
                );
            }

            LOGGER.info(
                    "Dashboard HTML registrado correctamente."
            );

        } catch (Exception e) {

            LOGGER.log(
                    Level.SEVERE,
                    "No se pudo registrar el dashboard",
                    e
            );
        }
    }

    private void cargarScript(
            String rutaRecurso,
            String rutaHtml
    ) {

        URL recurso = getClass().getResource(rutaRecurso);

        if (recurso == null) {
            throw new IllegalStateException(
                    "No se encontró el archivo: " + rutaRecurso
            );
        }

        String codigo =
                "(function() {" +
                        "var script = document.createElement('script');" +
                        "script.src = '" + rutaHtml + "';" +
                        "document.body.appendChild(script);" +
                        "})();";

        engine.executeScript(codigo);
    }

    private boolean esAdministrador() {

        return haySesion()
                && usuarioAutenticado.getRol() != null
                && "ADMIN".equalsIgnoreCase(
                usuarioAutenticado.getRol().getNombre()
        );
    }

    private boolean esTecnico() {

        return haySesion()
                && usuarioAutenticado.getRol() != null
                && "TECNICO".equalsIgnoreCase(
                usuarioAutenticado.getRol().getNombre()
        );
    }

    private void abrirLogin() {

        stage.setTitle("NautaTicket - Acceso");
        engine.load(paginaLogin);
    }

    private void abrirDashboard() {

        if (!haySesion()) {
            abrirLogin();
            return;
        }

        stage.setTitle(
                "NautaTicket - Panel principal"
        );

        engine.load(paginaDashboard);
    }

    private void cerrarSesion() {

        limpiarSesion();
        LOGGER.info("Sesión cerrada.");
        abrirLogin();
    }

    private void desactivarPuentes() {

        if (adminBridge != null) {
            adminBridge.desactivar();
        }

        if (ticketBridge != null) {
            ticketBridge.desactivar();
        }

        if (atencionBridge != null) {
            atencionBridge.desactivar();
        }

        if (historialBridge != null) {
            historialBridge.desactivar();
        }

        if (reporteBridge != null) {
            reporteBridge.desactivar();
        }

        if (estadisticasBridge != null) {
            estadisticasBridge.desactivar();
        }

        adminBridge = null;
        ticketBridge = null;
        atencionBridge = null;
        historialBridge = null;
        reporteBridge = null;
        estadisticasBridge = null;
    }

    private void limpiarSesion() {

        desactivarPuentes();
        if (sesionAutenticada != null) sesionAutenticada.cerrar();
        sesionAutenticada = null;

        usuarioAutenticado = null;
        perfilAutenticado = null;

        loginBridge = null;
        dashboardBridge = null;
    }

    private boolean haySesion() {

        return usuarioAutenticado != null
                && usuarioAutenticado.getId() != null
                && perfilAutenticado != null
                && !perfilAutenticado.isBlank();
    }

    private boolean esMismaPagina(
            String esperada,
            String actual
    ) {

        try {

            URI uriEsperada = URI.create(esperada);
            URI uriActual = URI.create(actual);

            if (!"file".equalsIgnoreCase(
                    uriEsperada.getScheme()
            )) {
                return false;
            }

            if (!"file".equalsIgnoreCase(
                    uriActual.getScheme()
            )) {
                return false;
            }

            Path rutaEsperada =
                    Path.of(uriEsperada).toRealPath();

            Path rutaActual =
                    Path.of(uriActual).toRealPath();

            return rutaEsperada.equals(rutaActual);

        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void stop() {
        limpiarSesion();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
