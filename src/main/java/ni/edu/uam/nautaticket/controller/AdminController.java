package ni.edu.uam.nautaticket.controller;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import ni.edu.uam.nautaticket.model.*;
import ni.edu.uam.nautaticket.service.AdministracionService;

public class AdminController {

    @FXML private TextField txtNombre;
    @FXML private TextField txtApellido;
    @FXML private TextField txtCorreo;
    @FXML private PasswordField txtPassword;

    @FXML private ComboBox<Rol> cbRol;
    @FXML private ComboBox<Departamento> cbDepartamento;

    @FXML private TableView<Usuario> tablaUsuarios;
    @FXML private TableColumn<Usuario, Integer> colId;
    @FXML private TableColumn<Usuario, String> colNombre;
    @FXML private TableColumn<Usuario, String> colCorreo;
    @FXML private TableColumn<Usuario, String> colRol;
    @FXML private TableColumn<Usuario, String> colDepartamento;
    @FXML private TableColumn<Usuario, String> colEstado;

    @FXML private TextField txtNombreDepartamento;
    @FXML private TextArea txtDescripcionDepartamento;

    @FXML private Label lblSesion;
    @FXML private Label lblMensaje;

    private AdministracionService servicio;

    @FXML
    private void initialize() {

        colId.setCellValueFactory(c ->
                new ReadOnlyObjectWrapper<>(c.getValue().getId()));

        colNombre.setCellValueFactory(c ->
                new ReadOnlyStringWrapper(
                        c.getValue().getNombre() + " " +
                                c.getValue().getApellido()
                ));

        colCorreo.setCellValueFactory(c ->
                new ReadOnlyStringWrapper(c.getValue().getCorreo()));

        colRol.setCellValueFactory(c ->
                new ReadOnlyStringWrapper(
                        c.getValue().getRol().getNombre()
                ));

        colDepartamento.setCellValueFactory(c ->
                new ReadOnlyStringWrapper(
                        c.getValue().getDepartamento() == null
                                ? "Sin departamento"
                                : c.getValue().getDepartamento().getNombre()
                ));

        colEstado.setCellValueFactory(c ->
                new ReadOnlyStringWrapper(
                        c.getValue().isActivo() ? "Activo" : "Inactivo"
                ));

        tablaUsuarios.getSelectionModel()
                .selectedItemProperty()
                .addListener((obs, anterior, actual) -> {
                    if (actual != null) {
                        cargarFormulario(actual);
                    }
                });
    }

    public void establecerAdministrador(Usuario administrador) {
        servicio = new AdministracionService(administrador);
        lblSesion.setText("Administrador: " + administrador.getNombre());
        recargarDatos();
    }

    private void recargarDatos() {
        cbRol.getItems().setAll(servicio.listarRoles());
        cbDepartamento.getItems().setAll(servicio.listarDepartamentos());
        tablaUsuarios.getItems().setAll(servicio.listarUsuarios());
    }

    private void cargarFormulario(Usuario usuario) {
        txtNombre.setText(usuario.getNombre());
        txtApellido.setText(usuario.getApellido());
        txtCorreo.setText(usuario.getCorreo());
        txtPassword.clear();

        cbRol.getItems().stream()
                .filter(r -> r.getId().equals(usuario.getRol().getId()))
                .findFirst().ifPresent(cbRol::setValue);

        if (usuario.getDepartamento() != null) {
            cbDepartamento.getItems().stream()
                    .filter(d -> d.getId().equals(
                            usuario.getDepartamento().getId()))
                    .findFirst().ifPresent(cbDepartamento::setValue);
        } else {
            cbDepartamento.setValue(null);
        }
    }

    @FXML
    private void registrarUsuario() {
        try {
            servicio.registrarUsuario(
                    txtNombre.getText(),
                    txtApellido.getText(),
                    txtCorreo.getText(),
                    txtPassword.getText(),
                    cbRol.getValue(),
                    cbDepartamento.getValue()
            );

            recargarDatos();
            limpiarFormulario();
            lblMensaje.setText("Usuario registrado correctamente");

        } catch (Exception e) {
            mostrarError(e);
        }
    }

    @FXML
    private void actualizarUsuario() {
        Usuario seleccionado =
                tablaUsuarios.getSelectionModel().getSelectedItem();

        if (seleccionado == null) {
            lblMensaje.setText("Selecciona un usuario para actualizar");
            return;
        }

        try {
            servicio.actualizarUsuario(
                    seleccionado.getId(),
                    txtNombre.getText(),
                    txtApellido.getText(),
                    txtCorreo.getText(),
                    txtPassword.getText(),
                    cbRol.getValue(),
                    cbDepartamento.getValue()
            );

            recargarDatos();
            limpiarFormulario();
            lblMensaje.setText("Usuario actualizado correctamente");

        } catch (Exception e) {
            mostrarError(e);
        }
    }

    @FXML
    private void desactivarUsuario() {
        Usuario seleccionado =
                tablaUsuarios.getSelectionModel().getSelectedItem();

        if (seleccionado == null) {
            lblMensaje.setText("Selecciona un usuario");
            return;
        }

        Alert confirmacion = new Alert(
                Alert.AlertType.CONFIRMATION,
                "¿Desactivar a " + seleccionado.getNombre() + "?",
                ButtonType.YES,
                ButtonType.NO
        );

        if (confirmacion.showAndWait().orElse(ButtonType.NO)
                != ButtonType.YES) {
            return;
        }

        try {
            servicio.desactivarUsuario(seleccionado.getId());
            recargarDatos();
            limpiarFormulario();
            lblMensaje.setText("Usuario desactivado");

        } catch (Exception e) {
            mostrarError(e);
        }
    }

    @FXML
    private void crearDepartamento() {
        try {
            servicio.crearDepartamento(
                    txtNombreDepartamento.getText(),
                    txtDescripcionDepartamento.getText()
            );

            txtNombreDepartamento.clear();
            txtDescripcionDepartamento.clear();

            recargarDatos();
            lblMensaje.setText("Departamento creado correctamente");

        } catch (Exception e) {
            mostrarError(e);
        }
    }

    @FXML
    private void limpiarFormulario() {
        tablaUsuarios.getSelectionModel().clearSelection();

        txtNombre.clear();
        txtApellido.clear();
        txtCorreo.clear();
        txtPassword.clear();

        cbRol.setValue(null);
        cbDepartamento.setValue(null);
    }

    private void mostrarError(Exception e) {
        if (e instanceof IllegalArgumentException) {
            lblMensaje.setText(e.getMessage());
        } else {
            lblMensaje.setText("No se pudo completar la operación");
        }

        e.printStackTrace();
    }
}
