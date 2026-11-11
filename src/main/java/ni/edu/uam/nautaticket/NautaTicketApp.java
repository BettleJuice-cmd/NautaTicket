package ni.edu.uam.nautaticket;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import ni.edu.uam.nautaticket.util.HibernateUtil;

public class NautaTicketApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {

        FXMLLoader loader = new FXMLLoader(
                getClass().getResource(
                        "/ni/edu/uam/nautaticket/login-view.fxml"
                )
        );

        Scene scene = new Scene(loader.load());

        stage.setTitle("NautaTicket - Inicio de sesión");
        stage.setScene(scene);
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
