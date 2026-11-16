package ni.edu.uam.nautaticket;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import ni.edu.uam.nautaticket.controller.LoginController;
import ni.edu.uam.nautaticket.controller.PerfilAcceso;
import ni.edu.uam.nautaticket.util.HibernateUtil;

import java.io.IOException;
import java.util.Objects;

public class NautaTicketApp extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        mostrarBienvenida(stage);
        stage.show();
    }

    public static void mostrarBienvenida(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(
                NautaTicketApp.class.getResource("bienvenida.fxml")
        );

        Scene scene = new Scene(loader.load());
        stage.setTitle("NautaTicket - Bienvenida");
        stage.setScene(scene);
        stage.sizeToScene();
        stage.centerOnScreen();
    }

    public static void mostrarLogin(Stage stage, PerfilAcceso perfil) throws IOException {
        Objects.requireNonNull(perfil, "Selecciona un perfil antes de iniciar sesión");

        FXMLLoader loader = new FXMLLoader(
                NautaTicketApp.class.getResource("login-view.fxml")
        );
        Scene scene = new Scene(loader.load());
        LoginController controller = loader.getController();
        controller.seleccionarPerfil(perfil);

        stage.setTitle("NautaTicket - " + perfil.getNombreVisible());
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
