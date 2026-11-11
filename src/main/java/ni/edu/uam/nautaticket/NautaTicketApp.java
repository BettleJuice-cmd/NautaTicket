
package ni.edu.uam.nautaticket;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import ni.edu.uam.nautaticket.controller.AtencionController;
import ni.edu.uam.nautaticket.service.AtencionService;

public class NautaTicketApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {

        FXMLLoader loader = new FXMLLoader(
                getClass().getResource(
                        "/ni/edu/uam/nautaticket/atencion.fxml"
                )
        );

        Scene scene = new Scene(loader.load());

        AtencionController controller = loader.getController();
        controller.setServicio(new AtencionService());

        stage.setTitle("NautaTicket - Atencion FIFO");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
