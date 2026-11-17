
package ni.edu.uam.nautaticket.controller;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import ni.edu.uam.nautaticket.model.*;
import ni.edu.uam.nautaticket.service.AdministracionService;

import java.text.Normalizer;
import java.util.Locale;

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

    private final ObservableList<Usuario> usuariosBase =
            FXCollections.observableArrayList();

    private FilteredList<Usuario> usuariosFiltrados;
    private SortedList<Usuario> usuariosOrdenados;

    private TextField buscarUsuarios;

    @FXML
    private void initialize() {

        configurarColumnas();
        configurarFiltro();

        tablaUsuarios.getSelectionModel()
                .selectedItemProperty()
                .addListener((obs, anterior, actual) -> {

                    if (actual != null) {
                        cargarFormulario(actual);
                    }
                });
    }

    private void configurarColumnas() {

        colId.setCellValueFactory(c ->
                new ReadOnlyObjectWrapper<>(
                        c.getValue().getId()
                )
        );

        colNombre.setCellValueFactory(c ->
                new ReadOnlyStringWrapper(
                        valor(c.getValue().getNombre()) + " " +
                                valor(c.getValue().getApellido())
                )
        );

        colCorreo.setCellValueFactory(c ->
                new ReadOnlyStringWrapper(
                        valor(c.getValue().getCorreo())
                )
        );

        colRol.setCellValueFactory(c ->
                new ReadOnlyStringWrapper(
                        c.getValue().getRol() == null
                                ? "Sin rol"
                                : c.getValue().getRol().getNombre()
                )
        );

        colDepartamento.setCellValueFactory(c ->
                new ReadOnlyStringWrapper(
                        c.getValue().getDepartamento() == null
                                ? "Sin departamento"
                                : c.getValue()
                                .getDepartamento()
                                .getNombre()
                )
        );

        colEstado.setCellValueFactory(c ->
                new ReadOnlyStringWrapper(
                        c.getValue().isActivo()
                                ? "Activo"
                                : "Inactivo"
                )
        );
    }

    private void configurarFiltro() {

        usuariosFiltrados = new FilteredList<>(
                usuariosBase,
                usuario -> true
        );

        usuariosOrdenados = new SortedList<>(
                usuariosFiltrados
        );

        usuariosOrdenados.comparatorProperty().bind(
                tablaUsuarios.comparatorProperty()
        );

        tablaUsuarios.setItems(usuariosOrdenados);

        buscarUsuarios = new TextField();

        buscarUsuarios.setPromptText("Filtrar usuarios");
        buscarUsuarios.setPrefWidth(185);
        buscarUsuarios.setMaxWidth(Double.MAX_VALUE);

        buscarUsuarios.setStyle(
                "-fx-font-size: 11px;" +
                        "-fx-background-radius: 5px;"
        );

        Label etiqueta = new Label("Usuario");

        etiqueta.setStyle(
                "-fx-font-weight: bold;" +
                        "-fx-font-size: 11px;"
        );

        VBox cabecera = new VBox(
                5,
                etiqueta,
                buscarUsuarios
        );

        colNombre.setText("");
        colNombre.setGraphic(cabecera);
        colNombre.setPrefWidth(
                Math.max(colNombre.getPrefWidth(), 210)
        );

        buscarUsuarios.textProperty().addListener(
                (observable, anterior, nuevo) ->
                        aplicarFiltro(nuevo)
        );
    }

    private void aplicarFiltro(String busqueda) {

        String texto = normalizar(busqueda);

        usuariosFiltrados.setPredicate(usuario -> {

            if (texto.isEmpty()) {
                return true;
            }

            String nombreCompleto =
                    valor(usuario.getNombre()) + " " +
                            valor(usuario.getApellido());

            String correo = valor(usuario.getCorreo());

            String rol = usuario.getRol() == null
                    ? ""
                    : valor(usuario.getRol().getNombre());

            String departamento =
                    usuario.getDepartamento() == null
                            ? ""
                            : valor(
                            usuario.getDepartamento()
                                    .getNombre()
                    );

            String estado = usuario.isActivo()
                    ? "Activo"
                    : "Inactivo";

            String contenido = String.join(
                    " ",
                    String.valueOf(usuario.getId()),
                    nombreCompleto,
                    correo,
                    rol,
                    departamento,
                    estado
            );

            return normalizar(contenido).contains(texto);
        });
    }

    private String normalizar(String texto) {

        if (texto == null) {
            return "";
        }

        String normalizado = Normalizer.normalize(
                texto,
                Normalizer.Form.NFD
        );

        return normalizado
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .trim();
    }

    private String valor(String texto) {
        return texto == null ? "" : texto;
    }

    public void establecerAdministrador(
            Usuario administrador
    ) {

        servicio = new AdministracionService(
                administrador
        );

        lblSesion.setText(
                "Administrador: " + administrador.getNombre()
        );

        recargarDatos();
    }

    private void recargarDatos() {

        if (servicio == null) {
            return;
        }

        cbRol.getItems().setAll(
                servicio.listarRoles()
        );

        cbDepartamento.getItems().setAll(
                servicio.listarDepartamentos()
        );

        usuariosBase.setAll(
                servicio.listarUsuarios()
        );

        aplicarFiltro(
                buscarUsuarios == null
                        ? ""
                        : buscarUsuarios.getText()
        );
    }

    private void cargarFormulario(Usuario usuario) {

        txtNombre.setText(
                valor(usuario.getNombre())
        );

        txtApellido.setText(
                valor(usuario.getApellido())
        );

        txtCorreo.setText(
                valor(usuario.getCorreo())
        );

        txtPassword.clear();

        cbRol.setValue(null);
        cbDepartamento.setValue(null);

        if (usuario.getRol() != null) {

            cbRol.getItems().stream()
                    .filter(rol ->
                            rol.getId().equals(
                                    usuario.getRol().getId()
                            )
                    )
                    .findFirst()
                    .ifPresent(cbRol::setValue);
        }

        if (usuario.getDepartamento() != null) {

            cbDepartamento.getItems().stream()
                    .filter(departamento ->
                            departamento.getId().equals(
                                    usuario.getDepartamento().getId()
                            )
                    )
                    .findFirst()
                    .ifPresent(cbDepartamento::setValue);
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

            lblMensaje.setText(
                    "Usuario registrado correctamente"
            );

        } catch (Exception e) {
            mostrarError(e);
        }
    }

    @FXML
    private void actualizarUsuario() {

        Usuario seleccionado =
                tablaUsuarios.getSelectionModel()
                        .getSelectedItem();

        if (seleccionado == null) {

            lblMensaje.setText(
                    "Selecciona un usuario para actualizar"
            );
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

            lblMensaje.setText(
                    "Usuario actualizado correctamente"
            );

        } catch (Exception e) {
            mostrarError(e);
        }
    }

    @FXML
    private void desactivarUsuario() {

        Usuario seleccionado =
                tablaUsuarios.getSelectionModel()
                        .getSelectedItem();

        if (seleccionado == null) {

            lblMensaje.setText(
                    "Selecciona un usuario"
            );
            return;
        }

        Alert confirmacion = new Alert(
                Alert.AlertType.CONFIRMATION,
                "¿Desactivar a " +
                        seleccionado.getNombre() + "?",
                ButtonType.YES,
                ButtonType.NO
        );

        if (confirmacion.showAndWait()
                .orElse(ButtonType.NO)
                != ButtonType.YES) {
            return;
        }

        try {

            servicio.desactivarUsuario(
                    seleccionado.getId()
            );

            recargarDatos();
            limpiarFormulario();

            lblMensaje.setText(
                    "Usuario desactivado"
            );

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

            lblMensaje.setText(
                    "Departamento creado correctamente"
            );

        } catch (Exception e) {
            mostrarError(e);
        }
    }

    @FXML
    private void limpiarFormulario() {

        tablaUsuarios.getSelectionModel()
                .clearSelection();

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
            lblMensaje.setText(
                    "No se pudo completar la operación"
            );
        }

        e.printStackTrace();
    }
}
