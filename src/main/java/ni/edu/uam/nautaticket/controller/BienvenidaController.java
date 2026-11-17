package ni.edu.uam.nautaticket.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import ni.edu.uam.nautaticket.NautaTicketApp;

import java.io.IOException;

public class BienvenidaController {

    @FXML private VBox raiz;
    @FXML private Label lblError;

    @FXML
    private void elegirAdministrador() {
        abrirLogin(PerfilAcceso.ADMINISTRADOR);
    }

    @FXML
    private void elegirEmpleado() {
        abrirLogin(PerfilAcceso.EMPLEADO);
    }

    @FXML
    private void elegirPersonalTI() {
        abrirLogin(PerfilAcceso.PERSONAL_TI);
    }

    private void abrirLogin(PerfilAcceso perfil) {
        try {
            Stage stage = (Stage) raiz.getScene().getWindow();
            NautaTicketApp.mostrarLogin(stage, perfil);
        } catch (IOException e) {
            lblError.setText("No se pudo abrir el inicio de sesión.");
            System.getLogger(getClass().getName()).log(
                    System.Logger.Level.ERROR, "Error al abrir el login", e);
        }
    }
}
