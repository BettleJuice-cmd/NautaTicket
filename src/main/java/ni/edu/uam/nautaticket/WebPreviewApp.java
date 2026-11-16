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

import ni.edu.uam.nautaticket.bridge.WebLoginBridge;

import java.net.URI;
import java.net.URL;
import java.nio.file.Path;

public class WebPreviewApp extends Application {

    private WebLoginBridge loginBridge;

    @Override
    public void start(Stage stage) {

        URL recurso = WebPreviewApp.class.getResource(
                "/web/index.html"
        );

        if (recurso == null) {
            throw new IllegalStateException(
                    "No se encontró /web/index.html"
            );
        }

        String direccionPermitida = recurso.toExternalForm();

        WebView webView = new WebView();
        webView.setContextMenuEnabled(false);

        WebEngine engine = webView.getEngine();
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

                        String direccionActual =
                                engine.getLocation();

                        System.out.println(
                                "[WEB] Página cargada: "
                                        + direccionActual
                        );

                        if (!esPaginaPermitida(
                                direccionPermitida,
                                direccionActual
                        )) {

                            System.err.println(
                                    "[WEB] Página no autorizada."
                            );

                            return;
                        }

                        registrarPuente(
                                engine,
                                stage,
                                direccionActual
                        );
                    }

                    if (nuevo == Worker.State.FAILED) {

                        System.err.println(
                                "[WEB] Error al cargar la página."
                        );

                        Throwable error =
                                engine.getLoadWorker()
                                        .getException();

                        if (error != null) {
                            error.printStackTrace();
                        }
                    }
                });

        StackPane root = new StackPane(webView);

        root.setStyle(
                "-fx-background-color: #000000;"
        );

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

        engine.load(direccionPermitida);
    }

    private boolean esPaginaPermitida(
            String direccionEsperada,
            String direccionActual
    ) {

        try {

            URI uriEsperada =
                    URI.create(direccionEsperada);

            URI uriActual =
                    URI.create(direccionActual);

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
                    Path.of(uriEsperada)
                            .toRealPath();

            Path rutaActual =
                    Path.of(uriActual)
                            .toRealPath();

            return rutaEsperada.equals(rutaActual);

        } catch (Exception e) {

            System.err.println(
                    "[WEB] No se pudo verificar la página: "
                            + e.getMessage()
            );

            return false;
        }
    }

    private void registrarPuente(
            WebEngine engine,
            Stage stage,
            String direccionActual
    ) {

        try {

            loginBridge = new WebLoginBridge(
                    engine,
                    stage,
                    direccionActual
            );

            JSObject ventana = (JSObject)
                    engine.executeScript("window");

            ventana.setMember(
                    "nautaBridge",
                    loginBridge
            );

            Object tipo = engine.executeScript(
                    "typeof window.nautaBridge"
            );

            Object disponible = engine.executeScript(
                    "window.nautaBridge != null"
            );

            System.out.println(
                    "[WEB] Tipo de puente: " + tipo
            );

            System.out.println(
                    "[WEB] Puente registrado: " + disponible
            );

        } catch (Exception e) {

            System.err.println(
                    "[WEB] Error registrando el puente Java."
            );

            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
