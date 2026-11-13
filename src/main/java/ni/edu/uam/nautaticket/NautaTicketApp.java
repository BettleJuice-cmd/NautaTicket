
package ni.edu.uam.nautaticket;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import ni.edu.uam.nautaticket.util.HibernateUtil;

import java.io.IOException;

public class NautaTicketApp extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        mostrarLogin(stage);
        stage.show();
    }

    public static void mostrarLogin(Stage stage) throws IOException {

        FXMLLoader loader = new FXMLLoader(
                NautaTicketApp.class.getResource("login-view.fxml")
        );

        Scene scene = new Scene(loader.load());

        stage.setTitle("NautaTicket - Inicio de sesión");
        stage.setScene(scene);
        stage.sizeToScene();
        stage.centerOnScreen();
    }

    @Override
    public void stop() {
        HibernateUtil.cerrar();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
