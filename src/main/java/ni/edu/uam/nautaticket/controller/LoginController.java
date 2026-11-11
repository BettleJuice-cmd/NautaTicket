package ni.edu.uam.nautaticket.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import ni.edu.uam.nautaticket.model.Usuario;
import ni.edu.uam.nautaticket.service.AuthService;

public class LoginController {

    @FXML private TextField txtCorreo;
    @FXML private PasswordField txtPassword;
    @FXML private Label lblMensaje;

    private final AuthService authService = new AuthService();

    @FXML
    private void iniciarSesion() {

        String correo = txtCorreo.getText();
        String password = txtPassword.getText();

        if (correo.isBlank() || password.isBlank()) {
            lblMensaje.setText("Completa todos los campos");
            return;
        }

        try {
            Usuario usuario = authService.iniciarSesion(
                    correo, password
            );

            txtPassword.clear();

            if (usuario == null) {
                lblMensaje.setText(
                        "Correo o contraseña incorrectos"
                );
                return;
            }

            if ("ADMIN".equalsIgnoreCase(
                    usuario.getRol().getNombre())) {

                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource(
                                "/ni/edu/uam/nautaticket/admin-view.fxml"
                        )
                );

                Parent root = loader.load();

                AdminController controller = loader.getController();
                controller.establecerAdministrador(usuario);

                Stage stage = (Stage) txtCorreo.getScene().getWindow();
                stage.setScene(new Scene(root));
                stage.setTitle("NautaTicket - Administración");
                stage.centerOnScreen();

            } else {
                lblMensaje.setText(
                        "Acceso correcto: " + usuario.getNombre() +
                                ". Módulo de tickets pendiente de integración."
                );
            }

        } catch (Exception e) {
            lblMensaje.setText(
                    "No se pudo completar el inicio de sesión"
            );
            e.printStackTrace();
        }
    }
}
