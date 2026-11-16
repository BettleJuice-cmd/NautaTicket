
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
import ni.edu.uam.nautaticket.bridge.WebDashboardBridge;
import ni.edu.uam.nautaticket.bridge.WebLoginBridge;
import ni.edu.uam.nautaticket.model.Usuario;

import java.net.URI;
import java.net.URL;
import java.nio.file.Path;

public class WebPreviewApp extends Application {

    private WebEngine engine;
    private Stage stage;

    private String paginaLogin;
    private String paginaDashboard;

    private WebLoginBridge loginBridge;
    private WebDashboardBridge dashboardBridge;
    private WebAdminBridge adminBridge;

    private Usuario usuarioAutenticado;
    private String perfilAutenticado;

    @Override
    public void start(Stage stage) {

        this.stage = stage;

        URL recursoLogin = getClass().getResource(
                "/web/index.html"
        );

        URL recursoDashboard = getClass().getResource(
                "/web/dashboard.html"
        );

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
                System.out.println(
                        "[WEB] " + event.getData()
                )
        );

        engine.getLoadWorker()
                .stateProperty()
                .addListener((observable, anterior, nuevo) -> {

                    if (nuevo == Worker.State.SUCCEEDED) {
                        paginaCargada();
                    }

                    if (nuevo == Worker.State.FAILED) {

                        System.err.println(
                                "[WEB] Error cargando la interfaz."
                        );

                        Throwable error = engine
                                .getLoadWorker()
                                .getException();

                        if (error != null) {
                            error.printStackTrace();
                        }
                    }
                });

        engine.locationProperty().addListener(
                (observable, anterior, nueva) -> {

                    if (nueva == null
                            || nueva.isBlank()
                            || "about:blank".equals(nueva)) {
                        return;
                    }

                    boolean login = esMismaPagina(
                            paginaLogin,
                            nueva
                    );

                    boolean dashboard = esMismaPagina(
                            paginaDashboard,
                            nueva
                    );

                    if (!login && !dashboard) {

                        System.err.println(
                                "[WEB] Navegación no permitida."
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

        String direccionActual = engine.getLocation();

        if (esMismaPagina(paginaLogin, direccionActual)) {

            limpiarSesion();
            registrarLogin(direccionActual);
            return;
        }

        if (esMismaPagina(
                paginaDashboard,
                direccionActual
        )) {

            if (!haySesion()) {

                System.err.println(
                        "[WEB] Dashboard sin autenticación."
                );

                abrirLogin();
                return;
            }

            registrarDashboard(direccionActual);
            return;
        }

        limpiarSesion();
        abrirLogin();
    }

    private void registrarLogin(String direccionActual) {

        try {

            loginBridge = new WebLoginBridge(
                    engine,
                    direccionActual,
                    this::autenticacionCorrecta
            );

            JSObject ventana = (JSObject)
                    engine.executeScript("window");

            ventana.setMember(
                    "nautaBridge",
                    loginBridge
            );

            System.out.println(
                    "[WEB] Puente de login registrado."
            );

        } catch (Exception e) {
            System.err.println(
                    "[WEB] Error registrando login."
            );
            e.printStackTrace();
        }
    }

    private void autenticacionCorrecta(
            Usuario usuario,
            String perfil
    ) {

        if (usuario == null || usuario.getId() == null) {
            return;
        }

        usuarioAutenticado = usuario;
        perfilAutenticado = perfil;

        System.out.println(
                "[WEB] Usuario autenticado correctamente."
        );

        abrirDashboard();
    }

    private void registrarDashboard(
            String direccionActual
    ) {

        try {

            dashboardBridge = new WebDashboardBridge(
                    engine,
                    direccionActual,
                    usuarioAutenticado.getCorreo(),
                    perfilAutenticado,
                    this::cerrarSesion
            );

            JSObject ventana = (JSObject)
                    engine.executeScript("window");

            ventana.setMember(
                    "nautaDashboardBridge",
                    dashboardBridge
            );

            if (esAdministrador()) {

                adminBridge = new WebAdminBridge(
                        engine,
                        direccionActual,
                        usuarioAutenticado
                );

                ventana.setMember(
                        "nautaAdminBridge",
                        adminBridge
                );

                System.out.println(
                        "[WEB] Administración disponible."
                );
            }

            engine.executeScript(
                    "if (typeof window.nautaDashboardReady " +
                            "=== 'function') {" +
                            "window.nautaDashboardReady();" +
                            "}"
            );

        } catch (Exception e) {

            System.err.println(
                    "[WEB] Error registrando dashboard."
            );

            e.printStackTrace();
        }
    }

    private boolean esAdministrador() {

        return haySesion()
                && usuarioAutenticado.getRol() != null
                && "ADMIN".equalsIgnoreCase(
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

        stage.setTitle("NautaTicket - Panel principal");
        engine.load(paginaDashboard);
    }

    private void cerrarSesion() {

        limpiarSesion();

        System.out.println(
                "[WEB] Sesión cerrada."
        );

        abrirLogin();
    }

    private void limpiarSesion() {

        if (adminBridge != null) {
            adminBridge.desactivar();
        }

        usuarioAutenticado = null;
        perfilAutenticado = null;

        loginBridge = null;
        dashboardBridge = null;
        adminBridge = null;
    }

    private boolean haySesion() {

        return usuarioAutenticado != null
                && usuarioAutenticado.getId() != null
                && perfilAutenticado != null
                && !perfilAutenticado.isBlank();
    }

    private boolean esMismaPagina(
            String direccionEsperada,
            String direccionActual
    ) {

        try {

            URI esperada = URI.create(direccionEsperada);
            URI actual = URI.create(direccionActual);

            if (!"file".equalsIgnoreCase(
                    esperada.getScheme()
            )) {
                return false;
            }

            if (!"file".equalsIgnoreCase(
                    actual.getScheme()
            )) {
                return false;
            }

            Path rutaEsperada =
                    Path.of(esperada).toRealPath();

            Path rutaActual =
                    Path.of(actual).toRealPath();

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
