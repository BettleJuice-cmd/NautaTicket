package ni.edu.uam.nautaticket.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import ni.edu.uam.nautaticket.model.Usuario;
import ni.edu.uam.nautaticket.service.AuthService;

public class LoginController {

    @FXML
    private TextField txtCorreo;

    @FXML
    private PasswordField txtPassword;

    @FXML
    private Label lblMensaje;

    private final AuthService authService = new AuthService();

    @FXML
    private void iniciarSesion() {

        String correo = txtCorreo.getText();
        String password = txtPassword.getText();

        if (correo.isBlank() || password.isBlank()) {
            lblMensaje.setText(
                    "Completa todos los campos"
            );
            return;
        }

        try {
            Usuario usuario = authService.iniciarSesion(
                    correo, password
            );

            if (usuario != null) {

                String nombre = usuario.getNombre();
                String rol = usuario.getRol().getNombre();

                lblMensaje.setText(
                        "Bienvenido, " + nombre +
                                " | Rol: " + rol
                );

                txtPassword.clear();

            } else {
                lblMensaje.setText(
                        "Correo o contraseña incorrectos"
                );
                txtPassword.clear();
            }

        } catch (Exception e) {
            lblMensaje.setText(
                    "No se pudo conectar con el sistema"
            );
            e.printStackTrace();
        }
    }
}
