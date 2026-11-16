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

import ni.edu.uam.nautaticket.util.HibernateUtil;

import java.net.URL;

public class WebPreviewApp extends Application {

    private ni.edu.uam.nautaticket.WebLoginBridge loginBridge;

    @Override
    public void start(Stage stage) {

        URL pagina = WebPreviewApp.class.getResource(
                "/web/index.html"
        );

        if (pagina == null) {
            throw new IllegalStateException(
                    "No se encontró /web/index.html"
            );
        }

        WebView webView = new WebView();
        webView.setContextMenuEnabled(false);

        WebEngine engine = webView.getEngine();
        engine.setJavaScriptEnabled(true);

        String direccion = pagina.toExternalForm();

        loginBridge = new ni.edu.uam.nautaticket.WebLoginBridge(
                engine,
                stage,
                direccion
        );

        engine.getLoadWorker()
                .stateProperty()
                .addListener((observable, anterior, nuevo) -> {

                    if (nuevo == Worker.State.SUCCEEDED) {

                        if (!direccion.equals(engine.getLocation())) {
                            return;
                        }

                        JSObject ventana = (JSObject)
                                engine.executeScript("window");

                        ventana.setMember(
                                "nautaBridge",
                                loginBridge
                        );

                        System.out.println(
                                "Interfaz Web de NautaTicket lista."
                        );
                    }

                    if (nuevo == Worker.State.FAILED) {

                        System.err.println(
                                "No se pudo cargar la interfaz."
                        );

                        if (engine.getLoadWorker()
                                .getException() != null) {

                            engine.getLoadWorker()
                                    .getException()
                                    .printStackTrace();
                        }
                    }
                });

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

        engine.load(direccion);

        stage.show();
    }

    @Override
    public void stop() {
        HibernateUtil.cerrar();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
