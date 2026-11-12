
package ni.edu.uam.nautaticket.controller;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import ni.edu.uam.nautaticket.dao.CategoriaDAO;
import ni.edu.uam.nautaticket.dao.TicketDAO;
import ni.edu.uam.nautaticket.model.Categoria;
import ni.edu.uam.nautaticket.model.Ticket;
import ni.edu.uam.nautaticket.service.TicketService;
import org.hibernate.SessionFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class TicketController {

    @FXML private TextField txtTitulo;
    @FXML private TextArea txtDescripcion;
    @FXML private ComboBox<Categoria> cmbCategoria;
    @FXML private ComboBox<Categoria> cmbFiltroCategoria;

    @FXML private TableView<Ticket> tablaTickets;
    @FXML private TableColumn<Ticket, Integer> colId;
    @FXML private TableColumn<Ticket, String> colTitulo;
    @FXML private TableColumn<Ticket, String> colCategoria;
    @FXML private TableColumn<Ticket, String> colEstado;
    @FXML private TableColumn<Ticket, String> colFecha;

    private final TicketService ticketService;
    private final CategoriaDAO categoriaDAO;
    private final Integer idSolicitante;

    private final DateTimeFormatter formatoFecha =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public TicketController(SessionFactory sessionFactory,
                            Integer idSolicitante) {
        this(sessionFactory, idSolicitante, false);
    }

    public TicketController(SessionFactory sessionFactory,
                            Integer idSolicitante,
                            boolean administrarTickets) {

        if (sessionFactory == null) {
            throw new IllegalArgumentException(
                    "La conexión Hibernate es obligatoria.");
        }

        if (idSolicitante == null || idSolicitante <= 0) {
            throw new IllegalArgumentException(
                    "Debe existir un usuario autenticado.");
        }

        this.ticketService =
                new TicketService(new TicketDAO(
                        sessionFactory, administrarTickets ? null : idSolicitante));

        this.categoriaDAO = new CategoriaDAO(sessionFactory);
        this.idSolicitante = idSolicitante;
    }

    @FXML
    private void initialize() {

        colId.setCellValueFactory(data ->
                new ReadOnlyObjectWrapper<>(data.getValue().getIdTicket()));

        colTitulo.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(data.getValue().getTitulo()));

        colCategoria.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(data.getValue().getCategoria().getNombre()));

        colEstado.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(data.getValue().getEstado().getNombre()));

        colFecha.setCellValueFactory(data -> {
            LocalDateTime fecha = data.getValue().getFechaCreacion();

            return new ReadOnlyStringWrapper(
                    fecha == null ? "" : fecha.format(formatoFecha)
            );
        });

        tablaTickets.getSelectionModel()
                .selectedItemProperty()
                .addListener((obs, anterior, seleccionado) -> {
                    if (seleccionado != null) {
                        txtTitulo.setText(seleccionado.getTitulo());
                        txtDescripcion.setText(
                                seleccionado.getDescripcion());
                        cmbCategoria.setValue(
                                seleccionado.getCategoria());
                    }
                });

        cargarCategorias();
        cargarTickets();
    }

    private void cargarCategorias() {
        try {
            var categorias = categoriaDAO.listarActivas();

            cmbCategoria.setItems(
                    FXCollections.observableArrayList(categorias));

            cmbFiltroCategoria.setItems(
                    FXCollections.observableArrayList(categorias));

        } catch (RuntimeException e) {
            mostrarError(e.getMessage());
        }
    }

    private void cargarTickets() {
        try {
            tablaTickets.setItems(
                    FXCollections.observableArrayList(
                            ticketService.listarTickets()
                    )
            );
        } catch (RuntimeException e) {
            mostrarError(e.getMessage());
        }
    }

    @FXML
    private void registrarTicket() {
        try {
            ticketService.crearTicket(
                    txtTitulo.getText(),
                    txtDescripcion.getText(),
                    cmbCategoria.getValue(),
                    idSolicitante
            );

            mostrarInformacion("Ticket registrado correctamente.");
            limpiarFormulario();
            cargarTickets();

        } catch (RuntimeException e) {
            mostrarError(e.getMessage());
        }
    }

    @FXML
    private void actualizarTicket() {
        Ticket seleccionado =
                tablaTickets.getSelectionModel().getSelectedItem();

        if (seleccionado == null) {
            mostrarError("Seleccione un ticket para actualizar.");
            return;
        }

        try {
            boolean actualizado = ticketService.actualizarTicket(
                    seleccionado.getIdTicket(),
                    txtTitulo.getText(),
                    txtDescripcion.getText(),
                    cmbCategoria.getValue()
            );

            if (actualizado) {
                mostrarInformacion("Ticket actualizado correctamente.");
                limpiarFormulario();
                cargarTickets();
            } else {
                mostrarError(
                        "No se puede actualizar este ticket. " +
                                "Verifique que esté pendiente.");
            }

        } catch (RuntimeException e) {
            mostrarError(e.getMessage());
        }
    }

    @FXML
    private void eliminarTicket() {
        Ticket seleccionado =
                tablaTickets.getSelectionModel().getSelectedItem();

        if (seleccionado == null) {
            mostrarError("Seleccione un ticket para eliminar.");
            return;
        }

        Alert confirmacion = new Alert(
                Alert.AlertType.CONFIRMATION,
                "¿Desea eliminar el ticket seleccionado?",
                ButtonType.YES,
                ButtonType.NO
        );

        confirmacion.setHeaderText("Confirmar eliminación");

        if (confirmacion.showAndWait().orElse(ButtonType.NO)
                != ButtonType.YES) {
            return;
        }

        try {
            boolean eliminado = ticketService.eliminarTicket(
                    seleccionado.getIdTicket()
            );

            if (eliminado) {
                mostrarInformacion("Ticket eliminado correctamente.");
                limpiarFormulario();
                cargarTickets();
            } else {
                mostrarError(
                        "No se puede eliminar este ticket. " +
                                "Debe estar pendiente y no tener historial.");
            }

        } catch (RuntimeException e) {
            mostrarError(e.getMessage());
        }
    }

    @FXML
    private void filtrarTickets() {
        Categoria categoria = cmbFiltroCategoria.getValue();

        if (categoria == null) {
            cargarTickets();
            return;
        }

        try {
            tablaTickets.setItems(
                    FXCollections.observableArrayList(
                            ticketService.buscarPorCategoria(
                                    categoria.getIdCategoria()
                            )
                    )
            );
        } catch (RuntimeException e) {
            mostrarError(e.getMessage());
        }
    }

    @FXML
    private void mostrarTodos() {
        cmbFiltroCategoria.setValue(null);
        cargarTickets();
    }

    @FXML
    private void limpiarFormulario() {
        tablaTickets.getSelectionModel().clearSelection();
        txtTitulo.clear();
        txtDescripcion.clear();
        cmbCategoria.setValue(null);
    }

    private void mostrarInformacion(String mensaje) {
        Alert alerta = new Alert(Alert.AlertType.INFORMATION);
        alerta.setTitle("NautaTicket");
        alerta.setHeaderText(null);
        alerta.setContentText(mensaje);
        alerta.showAndWait();
    }

    private void mostrarError(String mensaje) {
        Alert alerta = new Alert(Alert.AlertType.ERROR);
        alerta.setTitle("Error - NautaTicket");
        alerta.setHeaderText(null);
        alerta.setContentText(
                mensaje == null ? "Ocurrió un error." : mensaje);
        alerta.showAndWait();
    }
}
