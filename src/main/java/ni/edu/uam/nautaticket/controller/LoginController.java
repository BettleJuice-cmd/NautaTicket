package ni.edu.uam.nautaticket.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import ni.edu.uam.nautaticket.NautaTicketApp;
import ni.edu.uam.nautaticket.model.Usuario;
import ni.edu.uam.nautaticket.service.AuthService;
import ni.edu.uam.nautaticket.service.AuthService.AccesoAutenticado;

import java.io.IOException;

public class LoginController {

    @FXML private TextField txtCorreo;
    @FXML private PasswordField txtPassword;
    @FXML private Label lblPerfil;
    @FXML private Label lblMensaje;

    private final AuthService authService = new AuthService();
    private PerfilAcceso perfilSeleccionado;

    public void seleccionarPerfil(PerfilAcceso perfil) {
        this.perfilSeleccionado = perfil;
        lblPerfil.setText("Perfil seleccionado: " + perfil.getNombreVisible());
    }

    @FXML
    private void volver() {
        try {
            txtPassword.clear();
            NautaTicketApp.mostrarBienvenida(
                    (Stage) txtCorreo.getScene().getWindow());
        } catch (IOException e) {
            lblMensaje.setText("No se pudo volver a la bienvenida.");
            System.getLogger(getClass().getName()).log(
                    System.Logger.Level.ERROR, "Error al volver a la bienvenida", e);
        }
    }

    @FXML
    private void iniciarSesion() {
        if (perfilSeleccionado == null) {
            lblMensaje.setText("Primero selecciona un perfil en la bienvenida.");
            return;
        }

        String correo = txtCorreo.getText();
        String password = txtPassword.getText();
        if (correo.isBlank() || password.isBlank()) {
            lblMensaje.setText("Completa todos los campos");
            return;
        }

        try {
            // Se conserva la autenticación BCrypt existente.
            AccesoAutenticado acceso = authService.iniciarSesionConContexto(correo, password);
            Usuario usuario = acceso == null ? null : acceso.usuario();
            txtPassword.clear();

            if (usuario == null) {
                lblMensaje.setText("Correo o contraseña incorrectos");
                return;
            }

            if (usuario.getRol() == null ||
                    !perfilSeleccionado.coincideConRol(usuario.getRol().getNombre())) {
                acceso.sesion().cerrar();
                lblMensaje.setText("Tu cuenta no corresponde al perfil "
                        + perfilSeleccionado.getNombreVisible() + ".");
                return;
            }

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/ni/edu/uam/nautaticket/panel.fxml"));
            Parent root = loader.load();
            PanelController controller = loader.getController();
            controller.iniciar(usuario, acceso.sesion   ());

            Stage stage = (Stage) txtCorreo.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("NautaTicket - " + perfilSeleccionado.getNombreVisible());
            stage.sizeToScene();
            stage.centerOnScreen();
        } catch (Exception e) {
            lblMensaje.setText("No se pudo completar el inicio de sesión");
            System.getLogger(getClass().getName()).log(
                    System.Logger.Level.ERROR, "Error durante el inicio de sesión", e);
        }
    }
}
