
package ni.edu.uam.nautaticket;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class NautaTicketApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {

        FXMLLoader loader = new FXMLLoader(
                getClass().getResource(
                        "/ni/edu/uam/nautaticket/hello-view.fxml"
                )
        );

        Scene scene = new Scene(loader.load());

        stage.setTitle("NautaTicket");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
